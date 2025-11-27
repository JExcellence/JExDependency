package com.raindropcentral.rdq.database.entity.rank;

import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a player's active path in a rank tree.
 *
 * @author JExcellence
 * @since 6.0.0
 */
@Entity
@Table(
    name = "rdq_player_rank_paths",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"player_id", "tree_id"})
    },
    indexes = {
        @Index(name = "idx_player_path_player", columnList = "player_id"),
        @Index(name = "idx_player_path_tree", columnList = "tree_id"),
        @Index(name = "idx_player_path_active", columnList = "active")
    }
)
public class PlayerRankPath extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "tree_id", nullable = false, length = 64)
    private String treeId;

    @Column(name = "current_rank_id", nullable = false, length = 64)
    private String currentRankId;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "last_rank_change")
    private Instant lastRankChange;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected PlayerRankPath() {
    }

    public PlayerRankPath(
        @NotNull UUID playerId,
        @NotNull String treeId,
        @NotNull String currentRankId,
        @NotNull Instant startedAt,
        @Nullable Instant lastRankChange,
        boolean active
    ) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.treeId = Objects.requireNonNull(treeId, "treeId");
        this.currentRankId = Objects.requireNonNull(currentRankId, "currentRankId");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        this.lastRankChange = lastRankChange;
        this.active = active;
    }

    @NotNull
    public static PlayerRankPath create(
        @NotNull UUID playerId,
        @NotNull String treeId,
        @NotNull String initialRankId
    ) {
        var now = Instant.now();
        return new PlayerRankPath(playerId, treeId, initialRankId, now, now, true);
    }

    public void advanceToRank(@NotNull String rankId) {
        this.currentRankId = Objects.requireNonNull(rankId, "rankId");
        this.lastRankChange = Instant.now();
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @NotNull
    public UUID playerId() {
        return playerId;
    }

    @NotNull
    public String treeId() {
        return treeId;
    }

    @NotNull
    public String currentRankId() {
        return currentRankId;
    }

    @NotNull
    public Instant startedAt() {
        return startedAt;
    }

    @Nullable
    public Instant lastRankChange() {
        return lastRankChange;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerRankPath that)) return false;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "PlayerRankPath[playerId=" + playerId + ", treeId=" + treeId + ", currentRankId=" + currentRankId + "]";
    }
}
