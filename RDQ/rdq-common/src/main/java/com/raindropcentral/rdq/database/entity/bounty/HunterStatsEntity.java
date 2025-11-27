package com.raindropcentral.rdq.database.entity.bounty;

import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity tracking hunter statistics for the bounty system.
 *
 * @author JExcellence
 * @since 6.0.0
 */
@Entity
@Table(
    name = "rdq_hunter_stats",
    indexes = {
        @Index(name = "idx_hunter_stats_player", columnList = "player_id"),
        @Index(name = "idx_hunter_stats_claimed", columnList = "bounties_claimed"),
        @Index(name = "idx_hunter_stats_earned", columnList = "total_earned")
    }
)
public class HunterStatsEntity extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "player_id", unique = true, nullable = false)
    private UUID playerId;

    @Column(name = "player_name", nullable = false, length = 16)
    private String playerName;

    @Column(name = "bounties_placed", nullable = false)
    private int bountiesPlaced;

    @Column(name = "bounties_claimed", nullable = false)
    private int bountiesClaimed;

    @Column(name = "deaths", nullable = false)
    private int deaths;

    @Column(name = "total_earned", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalEarned;

    @Column(name = "total_spent", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalSpent;

    @Column(name = "highest_bounty", nullable = false, precision = 19, scale = 4)
    private BigDecimal highestBounty;

    protected HunterStatsEntity() {
    }

    public HunterStatsEntity(
        @NotNull UUID playerId,
        @NotNull String playerName,
        int bountiesPlaced,
        int bountiesClaimed,
        int deaths,
        @NotNull BigDecimal totalEarned,
        @NotNull BigDecimal totalSpent,
        @NotNull BigDecimal highestBounty
    ) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.playerName = Objects.requireNonNull(playerName, "playerName");
        this.bountiesPlaced = bountiesPlaced;
        this.bountiesClaimed = bountiesClaimed;
        this.deaths = deaths;
        this.totalEarned = Objects.requireNonNull(totalEarned, "totalEarned");
        this.totalSpent = Objects.requireNonNull(totalSpent, "totalSpent");
        this.highestBounty = Objects.requireNonNull(highestBounty, "highestBounty");
    }

    public static HunterStatsEntity create(@NotNull UUID playerId, @NotNull String playerName) {
        return new HunterStatsEntity(playerId, playerName, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public com.raindropcentral.rdq.bounty.HunterStats toRecord() {
        return new com.raindropcentral.rdq.bounty.HunterStats(playerId, playerName, bountiesPlaced, bountiesClaimed, deaths, totalEarned, totalSpent);
    }

    public void incrementBountiesPlaced(@NotNull BigDecimal amount) {
        this.bountiesPlaced++;
        this.totalSpent = this.totalSpent.add(amount);
    }

    public void incrementBountiesClaimed(@NotNull BigDecimal amount) {
        this.bountiesClaimed++;
        this.totalEarned = this.totalEarned.add(amount);
        if (amount.compareTo(this.highestBounty) > 0) {
            this.highestBounty = amount;
        }
    }

    public void incrementDeaths() {
        this.deaths++;
    }

    public void updatePlayerName(@NotNull String name) {
        this.playerName = Objects.requireNonNull(name, "name");
    }

    @NotNull
    public UUID playerId() {
        return playerId;
    }

    @NotNull
    public String playerName() {
        return playerName;
    }

    public int bountiesPlaced() {
        return bountiesPlaced;
    }

    public int bountiesClaimed() {
        return bountiesClaimed;
    }

    public int deaths() {
        return deaths;
    }

    @NotNull
    public BigDecimal totalEarned() {
        return totalEarned;
    }

    @NotNull
    public BigDecimal totalSpent() {
        return totalSpent;
    }

    @NotNull
    public BigDecimal highestBounty() {
        return highestBounty;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HunterStatsEntity that)) return false;
        return Objects.equals(playerId, that.playerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId);
    }

    @Override
    public String toString() {
        return "HunterStatsEntity[playerId=" + playerId + ", claimed=" + bountiesClaimed + ", earned=" + totalEarned + "]";
    }
}
