package com.raindropcentral.rdq.database.entity.player;

import com.raindropcentral.rdq.database.entity.rank.RPlayerRank;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRankPath;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Entity representing a RaindropQuests player extension.
 * <p>
 * This entity is mapped to the {@code rdq_player} table and extends the core player
 * with quest-specific features like bounties, ranks, rank paths, and perks.
 * Uses UUID reference to RCore's RPlayer instead of direct entity relationship.
 * </p>
 *
 * @author JExcellence
 */
@Entity
@Table(name = "rdq_player")
public class RDQPlayer extends BaseEntity {

    /**
     * The unique identifier (UUID) of the player.
     */
    @Column(name = "unique_id", unique = true, nullable = false)
    private UUID uniqueId;

    /**
     * The name of the player.
     */
    @Column(name = "player_name", nullable = false)
    private String playerName;

    /**
     * The player's rank associations across multiple rank trees.
     * <p>
     * This field establishes a one-to-many relationship with the {@link com.raindropcentral.rdq.database.entity.rank.RPlayerRank} entity,
     * representing the player's current ranks across different rank trees. The association is eagerly fetched,
     * cascades all operations, and removes orphans when the relationship is broken.
     * </p>
     * <p>
     * A player can have multiple rank records - one for each rank tree they are progressing in.
     * Mapped by the {@code rdqPlayer} property in {@link com.raindropcentral.rdq.database.entity.rank.RPlayerRank}.
     * </p>
     */
    @OneToMany(
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            mappedBy = "player"
    )
    private List<RPlayerRank> playerRanks = new ArrayList<>();

    @OneToMany(
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            mappedBy = "player"
    )
    private List<RPlayerRankPath> playerRankPaths = new ArrayList<>();

    /**
     * Protected no-argument constructor for JPA/Hibernate.
     */
    protected RDQPlayer() {
    }

    /**
     * Constructs a new {@code RDQPlayer} entity with the specified UUID and player name.
     *
     * @param uniqueId   the unique identifier of the player
     * @param playerName the name of the player
     */
    public RDQPlayer(
            final @NotNull UUID uniqueId,
            final @NotNull String playerName
    ) {
        this.uniqueId = uniqueId;
        this.playerName = playerName;
    }

    /**
     * Constructs a new {@code RDQPlayer} entity from a Bukkit {@link Player} instance.
     *
     * @param player the Bukkit player
     */
    public RDQPlayer(final @NotNull Player player) {
        this(player.getUniqueId(), player.getName());
    }

    /**
     * Gets the unique identifier (UUID) of the player.
     * Alias for getPlayerUuid() for backward compatibility.
     *
     * @return the player's UUID
     */
    public UUID getUniqueId() {
        return this.uniqueId;
    }

    /**
     * Gets all rank associations for this player across different rank trees.
     *
     * @return a list of {@link RPlayerRank} entities representing the player's ranks in various trees
     */
    public List<RPlayerRank> getPlayerRanks() {
        return this.playerRanks;
    }

    public List<RPlayerRankPath> getPlayerRankPaths() {
        return this.playerRankPaths;
    }

    public void setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    /**
     * Sets the player's rank associations.
     *
     * @param playerRanks the list of {@link RPlayerRank} entities
     */
    public void setPlayerRanks(final List<RPlayerRank> playerRanks) {
        this.playerRanks = playerRanks != null ? playerRanks : new ArrayList<>();
    }

    /**
     * Adds a new rank association for this player.
     *
     * @param playerRank the {@link RPlayerRank} entity to add
     */
    public void addPlayerRank(final @NotNull RPlayerRank playerRank) {
        if (this.playerRanks == null) {
            this.playerRanks = new ArrayList<>();
        }
        this.playerRanks.add(playerRank);
        playerRank.setRdqPlayer(this);
    }

    /**
     * Gets the player's rank for a specific rank tree.
     *
     * @param rankTreeIdentifier the identifier of the rank tree
     * @return an Optional containing the player's rank in the specified tree, or empty if not found
     */
    public Optional<RPlayerRank> getPlayerRankForTree(final @NotNull String rankTreeIdentifier) {
        if (this.playerRanks == null) {
            return Optional.empty();
        }

        return this.playerRanks.stream()
                .filter(rank -> rank.belongsToRankTree(rankTreeIdentifier))
                .findFirst();
    }

    /**
     * Gets the player's currently active rank (if any).
     *
     * @return an Optional containing the active rank, or empty if no rank is active
     */
    public Optional<RPlayerRank> getActivePlayerRank() {
        if (this.playerRanks == null) {
            return Optional.empty();
        }

        return this.playerRanks.stream()
                .filter(RPlayerRank::isActive)
                .findFirst();
    }

    /**
     * Checks if the player has any rank associations.
     *
     * @return true if the player has at least one rank, false otherwise
     */
    public boolean hasAnyRanks() {
        return this.playerRanks != null && !this.playerRanks.isEmpty();
    }

    /**
     * Checks if the player has a rank in the specified rank tree.
     *
     * @param rankTreeIdentifier the identifier of the rank tree to check
     * @return true if the player has a rank in the specified tree, false otherwise
     */
    public boolean hasRankInTree(final @NotNull String rankTreeIdentifier) {
        return getPlayerRankForTree(rankTreeIdentifier).isPresent();
    }

    /**
     * Legacy method for backward compatibility.
     * Returns the first rank found, or null if no ranks exist.
     *
     * @return the first {@link RPlayerRank} entity, or null
     * @deprecated Use {@link #getPlayerRanks()}, {@link #getActivePlayerRank()}, or {@link #getPlayerRankForTree(String)} instead
     */
    @Deprecated
    public RPlayerRank getPlayerRank() {
        if (this.playerRanks == null || this.playerRanks.isEmpty()) {
            return null;
        }
        return this.playerRanks.getFirst();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof final RDQPlayer rdqPlayer)) return false;
        return uniqueId.equals(rdqPlayer.uniqueId);
    }

    @Override
    public int hashCode() {
        return uniqueId.hashCode();
    }

}