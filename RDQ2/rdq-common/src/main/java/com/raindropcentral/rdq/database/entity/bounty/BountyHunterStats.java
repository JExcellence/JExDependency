package com.raindropcentral.rdq.database.entity.bounty;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Entity tracking bounty hunting statistics for individual players.
 * <p>
 * This entity maintains cumulative metrics for bounty hunters including total
 * bounties claimed, rewards earned, and overall ranking data. Statistics are
 * updated whenever a player successfully claims a bounty and can be queried
 * for leaderboard displays.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 2.0.0
 */
@Entity
@Table(
    name = "r_bounty_hunter_stats",
    uniqueConstraints = @UniqueConstraint(columnNames = {"player_id"}),
    indexes = {
        @Index(name = "idx_bounties_claimed", columnList = "bounties_claimed DESC"),
        @Index(name = "idx_total_reward_value", columnList = "total_reward_value DESC")
    }
)
public final class BountyHunterStats extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id", nullable = false, unique = true)
    private RDQPlayer player;

    @Column(name = "bounties_claimed", nullable = false)
    private int bountiesClaimed = 0;

    @Column(name = "total_reward_value", nullable = false)
    private double totalRewardValue = 0.0;

    @Column(name = "highest_bounty_value", nullable = false)
    private double highestBountyValue = 0.0;

    @Column(name = "last_claim_timestamp")
    private Long lastClaimTimestamp;

    /**
     * Protected no-arg constructor for JPA.
     */
    protected BountyHunterStats() {}

    /**
     * Creates a new bounty hunter statistics tracker for the specified player.
     *
     * @param player the player whose statistics are being tracked
     */
    public BountyHunterStats(final @NotNull RDQPlayer player) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    /**
     * Gets the player associated with these statistics.
     *
     * @return the player whose bounty hunting stats are tracked
     */
    public @NotNull RDQPlayer getPlayer() {
        return this.player;
    }

    /**
     * Sets the player for these statistics.
     *
     * @param player the player to associate with these stats
     */
    public void setPlayer(final @NotNull RDQPlayer player) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    /**
     * Gets the total number of bounties claimed by this player.
     *
     * @return the count of successfully claimed bounties
     */
    public int getBountiesClaimed() {
        return this.bountiesClaimed;
    }

    /**
     * Sets the total number of bounties claimed.
     *
     * @param bountiesClaimed the new claimed bounty count
     */
    public void setBountiesClaimed(final int bountiesClaimed) {
        this.bountiesClaimed = Math.max(0, bountiesClaimed);
    }

    /**
     * Increments the bounty claim counter by one.
     */
    public void incrementBountiesClaimed() {
        this.bountiesClaimed++;
    }

    /**
     * Gets the cumulative value of all rewards earned from bounty claims.
     *
     * @return the total reward value accumulated
     */
    public double getTotalRewardValue() {
        return this.totalRewardValue;
    }

    /**
     * Sets the total reward value earned from bounties.
     *
     * @param totalRewardValue the new total reward value
     */
    public void setTotalRewardValue(final double totalRewardValue) {
        this.totalRewardValue = Math.max(0.0, totalRewardValue);
    }

    /**
     * Adds the specified reward value to the cumulative total.
     *
     * @param value the reward value to add
     */
    public void addRewardValue(final double value) {
        if (value > 0) {
            this.totalRewardValue += value;
            if (value > this.highestBountyValue) {
                this.highestBountyValue = value;
            }
        }
    }

    /**
     * Gets the highest single bounty value claimed by this player.
     *
     * @return the maximum bounty reward value
     */
    public double getHighestBountyValue() {
        return this.highestBountyValue;
    }

    /**
     * Sets the highest bounty value claimed.
     *
     * @param highestBountyValue the new highest bounty value
     */
    public void setHighestBountyValue(final double highestBountyValue) {
        this.highestBountyValue = Math.max(0.0, highestBountyValue);
    }

    /**
     * Gets the timestamp of the last successful bounty claim.
     *
     * @return an Optional containing the epoch millisecond timestamp, or empty if no claims yet
     */
    public @NotNull Optional<Long> getLastClaimTimestamp() {
        return Optional.ofNullable(this.lastClaimTimestamp);
    }

    /**
     * Sets the timestamp of the last bounty claim.
     *
     * @param lastClaimTimestamp the epoch millisecond timestamp
     */
    public void setLastClaimTimestamp(final Long lastClaimTimestamp) {
        this.lastClaimTimestamp = lastClaimTimestamp;
    }

    /**
     * Updates the last claim timestamp to the current system time.
     */
    public void updateLastClaimTimestamp() {
        this.lastClaimTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Gets the UUID of the player associated with these statistics.
     * This is a convenience method to avoid navigating through the player entity.
     *
     * @return the player's unique identifier
     */
    public @NotNull UUID getPlayerUniqueId() {
        return this.player.getUniqueId();
    }
    
    /**
     * Gets the name of the player associated with these statistics.
     * This is a convenience method to avoid navigating through the player entity.
     *
     * @return the player's name
     */
    public @NotNull String getPlayerName() {
        return this.player.getPlayerName();
    }

    /**
     * Records a successful bounty claim by incrementing counters and updating values.
     *
     * @param rewardValue the value of the bounty reward claimed
     */
    public void recordClaim(final double rewardValue) {
        incrementBountiesClaimed();
        addRewardValue(rewardValue);
        updateLastClaimTimestamp();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final BountyHunterStats other)) return false;
        return Objects.equals(this.player, other.player);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.player);
    }

    @Override
    public String toString() {
        return "BountyHunterStats[id=%d, player=%s, claimed=%d, totalValue=%.2f, highest=%.2f]"
                .formatted(
                    getId(),
                    player != null ? player.getPlayerName() : "null",
                    bountiesClaimed,
                    totalRewardValue,
                    highestBountyValue
                );
    }
}
