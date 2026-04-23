package de.jexcellence.quests.database.entity;

import de.jexcellence.jehibernate.entity.base.LongIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One player's progress on a specific quest. Lifecycle tracked via
 * {@link QuestStatus}. Repeatable quests keep one row per attempt if
 * {@code completionCount} > 1; non-repeatable quests update the single
 * row in place.
 */
@Entity
@Table(
        name = "jexquests_player_quest_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_jexquests_pqp_player_quest",
                columnNames = {"player_uuid", "quest_identifier"}
        ),
        indexes = {
                @Index(name = "idx_jexquests_pqp_player", columnList = "player_uuid"),
                @Index(name = "idx_jexquests_pqp_status", columnList = "status")
        }
)
public class PlayerQuestProgress extends LongIdEntity {

    @Column(name = "player_uuid", nullable = false)
    private UUID playerUuid;

    @Column(name = "quest_identifier", nullable = false, length = 64)
    private String questIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private QuestStatus status = QuestStatus.AVAILABLE;

    @Column(name = "current_task_index", nullable = false)
    private int currentTaskIndex;

    @Column(name = "completion_count", nullable = false)
    private int completionCount;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt;

    protected PlayerQuestProgress() {
    }

    public PlayerQuestProgress(@NotNull UUID playerUuid, @NotNull String questIdentifier) {
        this.playerUuid = playerUuid;
        this.questIdentifier = questIdentifier;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public @NotNull UUID getPlayerUuid() { return this.playerUuid; }
    public @NotNull String getQuestIdentifier() { return this.questIdentifier; }
    public @NotNull QuestStatus getStatus() { return this.status; }
    public void setStatus(@NotNull QuestStatus status) { this.status = status; }
    public int getCurrentTaskIndex() { return this.currentTaskIndex; }
    public void setCurrentTaskIndex(int currentTaskIndex) { this.currentTaskIndex = currentTaskIndex; }
    public int getCompletionCount() { return this.completionCount; }
    public void setCompletionCount(int completionCount) { this.completionCount = completionCount; }
    public @Nullable LocalDateTime getStartedAt() { return this.startedAt; }
    public void setStartedAt(@Nullable LocalDateTime startedAt) { this.startedAt = startedAt; }
    public @Nullable LocalDateTime getCompletedAt() { return this.completedAt; }
    public void setCompletedAt(@Nullable LocalDateTime completedAt) { this.completedAt = completedAt; }
    public @Nullable LocalDateTime getExpiresAt() { return this.expiresAt; }
    public void setExpiresAt(@Nullable LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public @NotNull LocalDateTime getLastUpdatedAt() { return this.lastUpdatedAt; }
    public void setLastUpdatedAt(@NotNull LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

    @Override
    public String toString() {
        return "PlayerQuestProgress[" + this.playerUuid + "/" + this.questIdentifier + "/" + this.status + "]";
    }
}
