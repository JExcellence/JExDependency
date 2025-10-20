package com.raindropcentral.rdq.database.entity.player;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.perk.RPlayerPerk;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRank;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRankPath;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.*;

/**
 * Represents a persisted RDQ player record with ownership links to bounties, ranks, rank paths,
 * and perks. Each association mirrors the player-facing state that is synchronized with the Bukkit
 * runtime, allowing repository queries to fetch the aggregate in a single operation.
 * <p>
 * The entity maintains unmodifiable views over collection associations to prevent accidental
 * external mutation. Callers must rely on the provided mutator methods to keep the Hibernate
 * lifecycle synchronized.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Entity
@Table(name = "rdq_player")
public final class RDQPlayer extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "unique_id", unique = true, nullable = false)
    private UUID uniqueId;

    @Column(name = "player_name", nullable = false, length = 16)
    private String playerName;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "player")
    private RBounty bounty;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "player")
    private List<RPlayerRank> playerRanks = new ArrayList<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "player")
    private List<RPlayerRankPath> playerRankPaths = new ArrayList<>();

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<RPlayerPerk> playerPerks = new HashSet<>();

    /**
     * No-args constructor required by Hibernate for reflective instantiation.
     */
    protected RDQPlayer() {}

    /**
     * Creates a new player aggregate using the provided identifier and username.
     *
     * @param uniqueId   the unique identifier that matches the player's Mojang UUID
     * @param playerName the player's current username
     */
    public RDQPlayer(final @NotNull UUID uniqueId, final @NotNull String playerName) {
        this.uniqueId = Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        this.playerName = Objects.requireNonNull(playerName, "playerName cannot be null");
    }

    /**
     * Convenience constructor that adapts an online Bukkit player into the persistence aggregate.
     *
     * @param player the online Bukkit player supplying the identifier and username
     */
    public RDQPlayer(final @NotNull Player player) {
        this(player.getUniqueId(), player.getName());
    }

    /**
     * Retrieves the Mojang UUID assigned to the player.
     *
     * @return the player's unique identifier
     */
    public @NotNull UUID getUniqueId() {
        return this.uniqueId;
    }

    /**
     * Updates the player's UUID while enforcing the non-null invariant of the entity.
     *
     * @param uniqueId the new unique identifier for the player
     */
    public void setUniqueId(final @NotNull UUID uniqueId) {
        this.uniqueId = Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
    }

    /**
     * Obtains the last known username synchronized with the database.
     *
     * @return the stored player name
     */
    public @NotNull String getPlayerName() {
        return this.playerName;
    }

    /**
     * Replaces the stored player name, ensuring the value remains non-null.
     *
     * @param playerName the updated player name to store
     */
    public void setPlayerName(final @NotNull String playerName) {
        this.playerName = Objects.requireNonNull(playerName, "playerName cannot be null");
    }

    /**
     * Gets the bounty currently linked to the player, if any.
     *
     * @return the associated bounty or {@code null} when none is configured
     */
    public @Nullable RBounty getBounty() {
        return this.bounty;
    }

    /**
     * Assigns or clears the bounty that belongs to the player while synchronizing the reverse
     * association.
     *
     * @param bounty the bounty to attach, or {@code null} to detach an existing bounty
     */
    public void setBounty(final @Nullable RBounty bounty) {
        this.bounty = bounty;
        if (bounty != null) {
            bounty.setPlayer(this);
        }
    }

    /**
     * Supplies an unmodifiable snapshot of the ranks linked to the player.
     *
     * @return an immutable view of the player's ranks
     */
    public @NotNull List<RPlayerRank> getPlayerRanks() {
        return Collections.unmodifiableList(this.playerRanks);
    }

    /**
     * Replaces the current rank list with the supplied collection. The internal storage is cleared
     * and repopulated rather than re-assigned to keep Hibernate change tracking intact.
     *
     * @param playerRanks the ranks to persist for the player
     */
    public void setPlayerRanks(final @NotNull List<RPlayerRank> playerRanks) {
        this.playerRanks.clear();
        if (playerRanks != null) {
            this.playerRanks.addAll(playerRanks);
        }
    }

    /**
     * Provides the active rank path definitions associated with the player as an immutable view.
     *
     * @return the player's rank paths
     */
    public @NotNull List<RPlayerRankPath> getPlayerRankPaths() {
        return Collections.unmodifiableList(this.playerRankPaths);
    }

    /**
     * Reassigns the player's rank paths while preserving Hibernate-managed collections.
     *
     * @param playerRankPaths the new set of rank paths that should belong to the player
     */
    public void setPlayerRankPaths(final @NotNull List<RPlayerRankPath> playerRankPaths) {
        this.playerRankPaths.clear();
        if (playerRankPaths != null) {
            this.playerRankPaths.addAll(playerRankPaths);
        }
    }

    /**
     * Returns the set of perks unlocked by the player as an immutable view to prevent external
     * mutation.
     *
     * @return the player's configured perks
     */
    public @NotNull Set<RPlayerPerk> getPlayerPerks() {
        return Collections.unmodifiableSet(this.playerPerks);
    }

    /**
     * Adds a rank to the player if it is not already associated and synchronizes the inverse side
     * of the relationship.
     *
     * @param playerRank the rank to associate with the player
     */
    public void addPlayerRank(final @NotNull RPlayerRank playerRank) {
        Objects.requireNonNull(playerRank, "playerRank cannot be null");
        if (!this.playerRanks.contains(playerRank)) {
            this.playerRanks.add(playerRank);
            playerRank.setRdqPlayer(this);
        }
    }

    /**
     * Removes a rank association from the player and clears the back-reference when the rank was
     * part of the collection.
     *
     * @param playerRank the rank to remove from the player
     */
    public void removePlayerRank(final @NotNull RPlayerRank playerRank) {
        Objects.requireNonNull(playerRank, "playerRank cannot be null");
        if (this.playerRanks.remove(playerRank)) {
            playerRank.setRdqPlayer(null);
        }
    }

    /**
     * Looks up the player's rank inside the specified rank tree.
     *
     * @param rankTreeIdentifier the tree identifier to inspect
     * @return an {@link Optional} containing the matching rank when present
     */
    public @NotNull Optional<RPlayerRank> getPlayerRankForTree(final @NotNull String rankTreeIdentifier) {
        Objects.requireNonNull(rankTreeIdentifier, "rankTreeIdentifier cannot be null");
        return this.playerRanks.stream()
                .filter(rank -> rank.belongsToRankTree(rankTreeIdentifier))
                .findFirst();
    }

    /**
     * Retrieves the currently active rank for the player, if any.
     *
     * @return an {@link Optional} describing the active rank
     */
    public @NotNull Optional<RPlayerRank> getActivePlayerRank() {
        return this.playerRanks.stream()
                .filter(RPlayerRank::isActive)
                .findFirst();
    }

    /**
     * Indicates whether the player has any rank associations recorded.
     *
     * @return {@code true} when at least one rank is stored, otherwise {@code false}
     */
    public boolean hasAnyRanks() {
        return !this.playerRanks.isEmpty();
    }

    /**
     * Determines whether the player owns a rank inside the specified tree.
     *
     * @param rankTreeIdentifier the identifier of the rank tree to check
     * @return {@code true} if a rank exists in the tree, otherwise {@code false}
     */
    public boolean hasRankInTree(final @NotNull String rankTreeIdentifier) {
        Objects.requireNonNull(rankTreeIdentifier, "rankTreeIdentifier cannot be null");
        return getPlayerRankForTree(rankTreeIdentifier).isPresent();
    }

    /**
     * Two players are considered equal when they share the same unique identifier.
     *
     * @param obj the object to compare
     * @return {@code true} when the provided object represents the same player
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RDQPlayer other)) return false;
        return this.uniqueId.equals(other.uniqueId);
    }

    /**
     * Computes the hash code using the player's unique identifier.
     *
     * @return the hash value derived from the UUID
     */
    @Override
    public int hashCode() {
        return this.uniqueId.hashCode();
    }

    /**
     * Provides a concise summary of the player's identifier, name, and rank count for logging and
     * debugging scenarios.
     *
     * @return a formatted string describing the player
     */
    @Override
    public String toString() {
        return "RDQPlayer[id=%d, uuid=%s, name=%s, ranks=%d]"
                .formatted(getId(), uniqueId, playerName, playerRanks.size());
    }
}