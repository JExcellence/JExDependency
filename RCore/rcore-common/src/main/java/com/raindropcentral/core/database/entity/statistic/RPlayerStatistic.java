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

@Entity
@Table(name = "r_player_statistic")
public class RPlayerStatistic extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @OneToOne(mappedBy = "playerStatistic")
    private RPlayer player;

    @OneToMany(
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JoinColumn(name = "player_statistic_id")
    private Set<RAbstractStatistic> statistics = new HashSet<>();

    protected RPlayerStatistic() {}

    public RPlayerStatistic(final @NotNull RPlayer player) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    public @NotNull RPlayer getPlayer() {
        return this.player;
    }

    public void setPlayer(final @NotNull RPlayer player) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    public @NotNull Set<RAbstractStatistic> getStatistics() {
        return Set.copyOf(this.statistics);
    }

    /**
     * Internal method to access the mutable statistics collection.
     * WARNING: This exposes the internal Hibernate-managed collection.
     * Only use this when you need to modify entities in-place for proper persistence tracking.
     */
    public @NotNull Set<RAbstractStatistic> getStatisticsInternal() {
        return this.statistics;
    }

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
     * Add or replace a statistic ensuring uniqueness by identifier.
     * Important: The DB unique constraint is on (identifier, player_statistic_id),
     * so we must not allow multiple stats with the same identifier regardless of plugin.
     * 
     * This method updates existing statistics in-place when possible to avoid
     * Hibernate flush ordering issues that can cause unique constraint violations.
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
     * Check if a statistic with the given identifier exists, regardless of plugin.
     * This matches the database unique constraint which is on (identifier, player_statistic_id).
     */
    public boolean hasStatisticByIdentifier(final @NotNull String identifier) {
        Objects.requireNonNull(identifier, "identifier cannot be null");
        return this.statistics.stream()
                .anyMatch(stat -> stat.getIdentifier().equals(identifier));
    }

    public boolean removeStatistic(
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        Objects.requireNonNull(identifier, "identifier cannot be null");
        Objects.requireNonNull(plugin, "plugin cannot be null");

        return this.statistics.removeIf(stat -> stat.matches(identifier, plugin));
    }

    /**
     * Removes any statistic with the given identifier, ignoring plugin.
     * This matches the DB uniqueness rule and is safe to use when replacing stats.
     */
    public boolean removeStatisticByIdentifier(final @NotNull String identifier) {
        Objects.requireNonNull(identifier, "identifier cannot be null");
        return this.statistics.removeIf(stat -> stat.getIdentifier().equals(identifier));
    }

    public long getStatisticCountForPlugin(final @NotNull String plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");

        return this.statistics.stream()
                .filter(stat -> stat.getPlugin().equals(plugin))
                .count();
    }

    public int getTotalStatisticCount() {
        return this.statistics.size();
    }

    @Override
    public String toString() {
        return "RPlayerStatistic[id=%d, player=%s, statisticsCount=%d]"
                .formatted(getId(), player != null ? player.getPlayerName() : "null", statistics.size());
    }
}