package com.raindropcentral.core.database.entity.player;

import com.raindropcentral.core.database.entity.statistic.RPlayerStatistic;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a persisted player profile stored in the {@code r_player} table.
 * <p>
 * The entity is mapped by unique {@link UUID} identifiers and maintains a mandatory
 * single row per Mojang account. A bidirectional one-to-one mapping links the profile
 * to its {@link RPlayerStatistic} aggregate for statistic persistence. All updates
 * should be routed through {@link com.raindropcentral.core.database.repository.RPlayerRepository}
 * to guarantee executor-aligned state changes.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Entity
@Table(name = "r_player")
public class RPlayer extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Minimum permissible player name length after trimming whitespace. Mirrors the
     * lower bound enforced by Mojang and guards against persisting empty values.
     */
    private static final int MIN_NAME_LENGTH = 3;

    /**
     * Maximum permissible player name length after trimming whitespace. Aligns with
     * Mojang username rules to keep the column constraint and validation consistent.
     */
    private static final int MAX_NAME_LENGTH = 16;
    
    /**
     * Column mapping for {@code unique_id}. This value remains immutable after
     * creation and enforces table-level uniqueness for Mojang player UUIDs.
     */
    @Column(name = "unique_id", unique = true, nullable = false)
    private UUID uniqueId;

    /**
     * Column mapping for {@code player_name}. Updated when the player joins with a
     * different username while respecting the configured length constraints.
     */
    @Column(name = "player_name", nullable = false, length = MAX_NAME_LENGTH)
    private String playerName;

    /**
     * Bidirectional one-to-one relation to the player's statistics aggregate.
     * Hibernate owns the association on this side to ensure cascade persistence
     * for statistic snapshots whenever the player entity is persisted.
     */
    @OneToOne(
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private RPlayerStatistic playerStatistic;

    /**
     * Hibernate-required constructor for proxy instantiation. Should not be used directly.
     */
    protected RPlayer() {}

    /**
     * Creates a new persistent player using a Mojang UUID and last known username.
     *
     * @param uniqueId   globally unique identifier for the player; must not be {@code null}
     * @param playerName current username for the player; must satisfy validation constraints
     */
    public RPlayer(final @NotNull UUID uniqueId, final @NotNull String playerName) {
        this.uniqueId = Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        this.playerName = validatePlayerName(playerName);
    }

    /**
     * Convenience constructor that builds the entity from a live Bukkit {@link Player} snapshot.
     *
     * @param bukkitPlayer source player; used to populate UUID and username fields
     */
    public RPlayer(final @NotNull Player bukkitPlayer) {
        this(bukkitPlayer.getUniqueId(), bukkitPlayer.getName());
    }

    /**
     * Retrieves the immutable Mojang UUID backing the {@code unique_id} column.
     *
     * @return unique identifier for this player row
     */
    public @NotNull UUID getUniqueId() {
        return this.uniqueId;
    }

    /**
     * Provides the latest stored username synchronized with the {@code player_name} column.
     *
     * @return last persisted username for the player
     */
    public @NotNull String getPlayerName() {
        return this.playerName;
    }

    /**
     * Updates the persisted username. Validation ensures the database length
     * constraints are respected prior to persistence operations.
     *
     * @param newName updated username to store
     */
    public void updatePlayerName(final @NotNull String newName) {
        this.playerName = validatePlayerName(newName);
    }

    /**
     * Exposes the owning statistic aggregate when loaded. The association may be
     * {@code null} for new players until statistics are captured.
     *
     * @return linked statistic aggregate or {@code null} if none exists yet
     */
    public @Nullable RPlayerStatistic getPlayerStatistic() {
        return this.playerStatistic;
    }

    /**
     * Assigns the statistic aggregate and synchronizes the back-reference to maintain
     * Hibernate-managed ownership for the one-to-one relationship.
     *
     * @param playerStatistic statistics aggregate to associate; must not be {@code null}
     */
    public void setPlayerStatistic(final @NotNull RPlayerStatistic playerStatistic) {
        Objects.requireNonNull(playerStatistic, "playerStatistic cannot be null");
        this.playerStatistic = playerStatistic;
        playerStatistic.setPlayer(this);
    }

    /**
     * Indicates whether the associated statistics aggregate is populated with any rows.
     * Callers typically use this to skip unnecessary loads when displaying summaries.
     *
     * @return {@code true} when statistics exist, {@code false} otherwise
     */
    public boolean hasStatistics() {
        return this.playerStatistic != null && !this.playerStatistic.getStatistics().isEmpty();
    }

    /**
     * Normalizes and validates a player name before persistence. Names are trimmed
     * and their length verified against the Mojang-aligned bounds.
     *
     * @param name user-provided player name to validate; must not be {@code null}
     * @return trimmed player name suitable for persistence
     * @throws IllegalArgumentException when the normalized name violates length constraints
     */
    private static String validatePlayerName(final @NotNull String name) {
        Objects.requireNonNull(name, "playerName cannot be null");

        final String trimmed = name.trim();
        if (trimmed.length() < MIN_NAME_LENGTH || trimmed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                "Player name must be between %d and %d characters, got: %d"
                    .formatted(MIN_NAME_LENGTH, MAX_NAME_LENGTH, trimmed.length())
            );
        }

        return trimmed;
    }

    /**
     * Generates a concise debug description containing persistence identifiers and
     * current state flags. Used in logs to rapidly assess entity synchronization.
     *
     * @return formatted string summarizing the entity state
     */
    @Override
    public String toString() {
        return "RPlayer[id=%d, uuid=%s, name=%s, hasStats=%b]"
            .formatted(getId(), uniqueId, playerName, hasStatistics());
    }
}
