package com.raindropcentral.core.database.entity.statistic;

import com.raindropcentral.core.database.entity.player.RPlayer;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Root aggregate for player statistic values persisted in {@code r_player_statistic}.
 * <p>
 * Each instance maintains a one-to-one relationship with {@link RPlayer} and owns a
 * collection of {@link RAbstractStatistic} rows stored in the {@code r_statistic}
 * table via a join column. Repository operations execute on dedicated executors,
 * so callers should avoid mutating the entity from asynchronous contexts without
 * proper synchronization.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Entity
@Table(name = "r_player_statistic")
public class RPlayerStatistic extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Back-reference to the owning {@link RPlayer}. Hibernate manages the mapped-by side
     * so the foreign key resides on the {@code r_player} table.
     */
    @OneToOne(mappedBy = "playerStatistic")
    private RPlayer player;

    /**
     * Aggregated statistic values. Stored in {@code r_statistic} with a non-null
     * {@code player_statistic_id} foreign key and constrained by (identifier, player_statistic_id).
     */
    @OneToMany(
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JoinColumn(name = "player_statistic_id")
    private Set<RAbstractStatistic> statistics = new HashSet<>();

    /**
     * Protected constructor for JPA. Aggregates should be instantiated via the player-bound constructor.
     */
    protected RPlayerStatistic() {}

    /**
     * Creates the aggregate bound to a particular player profile.
     *
     * @param player owning player; must be present before persistence
     */
    public RPlayerStatistic(final @NotNull RPlayer player) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    /**
     * Provides the owning player reference.
     *
     * @return player that owns this aggregate
     */
    public @NotNull RPlayer getPlayer() {
        return this.player;
    }

    /**
     * Updates the owning player reference, typically during cascading assignments.
     *
     * @param player player to associate with this aggregate
     */
    public void setPlayer(final @NotNull RPlayer player) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    /**
     * Returns an immutable view of all persisted statistics to prevent accidental
     * mutation outside managed transactional scopes.
     *
     * @return read-only set of statistics
     */
    public @NotNull Set<RAbstractStatistic> getStatistics() {
        return Set.copyOf(this.statistics);
    }

    /**
     * Internal accessor exposing the mutable Hibernate-managed statistics set. Use with
     * caution inside transactional boundaries to allow dirty tracking.
     *
     * @return mutable statistic set managed by Hibernate
     */
    public @NotNull Set<RAbstractStatistic> getStatisticsInternal() {
        return this.statistics;
    }

    /**
     * Locates the first statistic value matching the given identifier and plugin pair.
     *
     * @param identifier logical statistic identifier
     * @param plugin     plugin namespace claiming the statistic
     * @return optional resolved value, empty when no statistic is present
     */
    public @NotNull Optional<Object> getStatisticValue(
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        Objects.requireNonNull(identifier, "identifier cannot be null");
        Objects.requireNonNull(plugin, "plugin cannot be null");

        return this.statistics.stream()
                .filter(stat -> stat.matches(identifier, plugin))
                .map(RAbstractStatistic::getValue)
                .findFirst();
    }

    /**
     * Adds a new statistic or replaces an existing one that matches the identifier.
     * Maintains the database unique constraint on (identifier, player_statistic_id)
     * and ensures back-references are synchronized for cascading updates.
     *
     * @param statistic statistic instance to attach to the aggregate
     */
    public void addOrReplaceStatistic(final @NotNull RAbstractStatistic statistic) {
        Objects.requireNonNull(statistic, "statistic cannot be null");

        // Find existing statistic with the same identifier
        RAbstractStatistic existingToRemove = null;
        for (RAbstractStatistic existing : this.statistics) {
            if (existing.getIdentifier().equals(statistic.getIdentifier())) {
                existingToRemove = existing;
                break;
            }
        }

        // Remove the existing statistic if found
        if (existingToRemove != null) {
            this.statistics.remove(existingToRemove);
            existingToRemove.setPlayerStatistic(null);
        }

        // Add the new/updated statistic
        this.statistics.add(statistic);
        statistic.setPlayerStatistic(this);
    }

    /**
     * Determines whether a statistic exists for the provided identifier and plugin pair.
     *
     * @param identifier logical statistic identifier
     * @param plugin     plugin namespace to match
     * @return {@code true} when a matching statistic is present
     */
    public boolean hasStatistic(
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        Objects.requireNonNull(identifier, "identifier cannot be null");
        Objects.requireNonNull(plugin, "plugin cannot be null");

        return this.statistics.stream()
                .anyMatch(stat -> stat.matches(identifier, plugin));
    }

    /**
     * Checks for a statistic with the given identifier regardless of plugin namespace,
     * mirroring the unique constraint enforced by the database schema.
     *
     * @param identifier statistic identifier to probe
     * @return {@code true} when any statistic shares the identifier
     */
    public boolean hasStatisticByIdentifier(final @NotNull String identifier) {
        Objects.requireNonNull(identifier, "identifier cannot be null");
        return this.statistics.stream()
                .anyMatch(stat -> stat.getIdentifier().equals(identifier));
    }

    /**
     * Removes statistics matching both identifier and plugin namespace.
     *
     * @param identifier logical statistic identifier
     * @param plugin     plugin namespace associated with the statistic
     * @return {@code true} when any entries were removed
     */
    public boolean removeStatistic(
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        Objects.requireNonNull(identifier, "identifier cannot be null");
        Objects.requireNonNull(plugin, "plugin cannot be null");

        return this.statistics.removeIf(stat -> stat.matches(identifier, plugin));
    }

    /**
     * Removes any statistic with the given identifier, ignoring plugin namespace. Useful
     * when re-seeding values where identifier uniqueness must be preserved. Operates on the
     * Hibernate-managed collection and therefore must execute on the thread that owns the
     * surrounding transaction to avoid concurrent modification hazards.
     *
     * @param identifier statistic identifier targeted for removal
     * @return {@code true} when any matching statistic was removed and the association
     * is orphaned, prompting Hibernate to cascade delete the underlying row
     */
    public boolean removeStatisticByIdentifier(final @NotNull String identifier) {
        Objects.requireNonNull(identifier, "identifier cannot be null");
        return this.statistics.removeIf(stat -> stat.getIdentifier().equals(identifier));
    }

    /**
     * Counts statistics owned by the specified plugin. This method performs a snapshot read
     * over the managed collection and should be invoked only while holding the persistence
     * context lock (typically within a single-threaded transaction) to guarantee deterministic
     * results.
     *
     * @param plugin plugin namespace to filter by
     * @return number of statistics belonging to the plugin at invocation time
     */
    public long getStatisticCountForPlugin(final @NotNull String plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");

        return this.statistics.stream()
                .filter(stat -> stat.getPlugin().equals(plugin))
                .count();
    }

    /**
     * Provides the current size of the managed statistic set. Callers should avoid invoking
     * this from parallel threads because the underlying {@link HashSet} is not thread-safe
     * and Hibernate tracks changes on the owning session thread.
     *
     * @return total number of statistics tracked by this aggregate
     */
    public int getTotalStatisticCount() {
        return this.statistics.size();
    }

    /**
     * Presents a concise textual representation for logging and debugging. The method performs
     * only read access on entity state and therefore should be invoked from threads that already
     * own the entity to prevent visibility issues on lazily loaded associations.
     *
     * @return formatted string describing the aggregate
     */
    @Override
    public String toString() {
        return "RPlayerStatistic[id=%d, player=%s, statisticsCount=%d]"
                .formatted(getId(), player != null ? player.getPlayerName() : "null", statistics.size());
    }
}
