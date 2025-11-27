package com.raindropcentral.rdq.database.entity.rank;

import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a rank unlocked by a player.
 *
 * @author JExcellence
 * @since 6.0.0
 */
@Entity
@Table(
    name = "rdq_player_ranks",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"player_id", "rank_id"})
    },
    indexes = {
        @Index(name = "idx_player_rank_player", columnList = "player_id"),
        @Index(name = "idx_player_rank_rank", columnList = "rank_id"),
        @Index(name = "idx_player_rank_tree", columnList = "tree_id")
    }
)
public class PlayerRank extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "rank_id", nullable = false, length = 64)
    private String rankId;

    @Column(name = "tree_id", nullable = false, length = 64)
    private String treeId;

    @Column(name = "unlocked_at", nullable = false, updatable = false)
    private Instant unlockedAt;

    protected PlayerRank() {
    }

    public PlayerRank(
        @NotNull UUID playerId,
        @NotNull String rankId,
        @NotNull String treeId,
        @NotNull Instant unlockedAt
    ) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.rankId = Objects.requireNonNull(rankId, "rankId");
        this.treeId = Objects.requireNonNull(treeId, "treeId");
        this.unlockedAt = Objects.requireNonNull(unlockedAt, "unlockedAt");
    }

    @NotNull
    public static PlayerRank create(
        @NotNull UUID playerId,
        @NotNull String rankId,
        @NotNull String treeId
    ) {
        return new PlayerRank(playerId, rankId, treeId, Instant.now());
    }

    @NotNull
    public UUID playerId() {
        return playerId;
    }

    @NotNull
    public String rankId() {
        return rankId;
    }

    @NotNull
    public String treeId() {
        return treeId;
    }

    @NotNull
    public Instant unlockedAt() {
        return unlockedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerRank that)) return false;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "PlayerRank[playerId=" + playerId + ", rankId=" + rankId + ", treeId=" + treeId + "]";
    }
}
