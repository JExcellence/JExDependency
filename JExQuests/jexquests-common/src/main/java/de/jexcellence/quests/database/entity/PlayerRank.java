package de.jexcellence.quests.database.entity;

import de.jexcellence.jehibernate.entity.base.LongIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One player's position in a {@link RankTree}. Unique per (player, tree).
 *
 * <p>Carries enough metadata for RDQ-parity features: {@code selectedAt}
 * marks when the player first opted into the tree,
 * {@code lastSwitchedAt} drives the cooldown gate for
 * {@link de.jexcellence.quests.service.RankPathService#switchRankPath},
 * and {@code active} is the "currently selected" flag (multiple trees
 * can have rows but only one can be active per player).
 */
@Entity
@Table(
        name = "jexquests_player_rank",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_jexquests_player_rank_tree",
                columnNames = {"player_uuid", "tree_identifier"}
        ),
        indexes = {
                @Index(name = "idx_jexquests_player_rank_player", columnList = "player_uuid"),
                @Index(name = "idx_jexquests_player_rank_tree", columnList = "tree_identifier")
        }
)
public class PlayerRank extends LongIdEntity {

    @Column(name = "player_uuid", nullable = false)
    private UUID playerUuid;

    @Column(name = "tree_identifier", nullable = false, length = 64)
    private String treeIdentifier;

    @Column(name = "current_rank_identifier", nullable = false, length = 64)
    private String currentRankIdentifier;

    @Column(name = "promoted_at", nullable = false)
    private LocalDateTime promotedAt;

    @Column(name = "progression_percent", nullable = false)
    private int progressionPercent;

    @Column(name = "tree_completed", nullable = false)
    private boolean treeCompleted;

    @Column(name = "tree_completed_at")
    private LocalDateTime treeCompletedAt;

    @Column(name = "selected_at", nullable = false)
    private LocalDateTime selectedAt;

    @Column(name = "last_switched_at")
    private LocalDateTime lastSwitchedAt;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected PlayerRank() {
    }

    public PlayerRank(
            @NotNull UUID playerUuid,
            @NotNull String treeIdentifier,
            @NotNull String initialRankIdentifier
    ) {
        this.playerUuid = playerUuid;
        this.treeIdentifier = treeIdentifier;
        this.currentRankIdentifier = initialRankIdentifier;
        final var now = LocalDateTime.now();
        this.promotedAt = now;
        this.selectedAt = now;
    }

    public @NotNull UUID getPlayerUuid() { return this.playerUuid; }
    public @NotNull String getTreeIdentifier() { return this.treeIdentifier; }
    public @NotNull String getCurrentRankIdentifier() { return this.currentRankIdentifier; }
    public void setCurrentRankIdentifier(@NotNull String currentRankIdentifier) { this.currentRankIdentifier = currentRankIdentifier; }
    public @NotNull LocalDateTime getPromotedAt() { return this.promotedAt; }
    public void setPromotedAt(@NotNull LocalDateTime promotedAt) { this.promotedAt = promotedAt; }
    public int getProgressionPercent() { return this.progressionPercent; }
    public void setProgressionPercent(int progressionPercent) { this.progressionPercent = progressionPercent; }
    public boolean isTreeCompleted() { return this.treeCompleted; }
    public void setTreeCompleted(boolean treeCompleted) { this.treeCompleted = treeCompleted; }
    public @Nullable LocalDateTime getTreeCompletedAt() { return this.treeCompletedAt; }
    public void setTreeCompletedAt(@Nullable LocalDateTime treeCompletedAt) { this.treeCompletedAt = treeCompletedAt; }
    public @NotNull LocalDateTime getSelectedAt() { return this.selectedAt; }
    public void setSelectedAt(@NotNull LocalDateTime selectedAt) { this.selectedAt = selectedAt; }
    public @Nullable LocalDateTime getLastSwitchedAt() { return this.lastSwitchedAt; }
    public void setLastSwitchedAt(@Nullable LocalDateTime lastSwitchedAt) { this.lastSwitchedAt = lastSwitchedAt; }
    public boolean isActive() { return this.active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return "PlayerRank[" + this.playerUuid + "/" + this.treeIdentifier + "/" + this.currentRankIdentifier + "]";
    }
}
