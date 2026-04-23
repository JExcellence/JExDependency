package de.jexcellence.quests.database.entity;

import de.jexcellence.jehibernate.entity.base.LongIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One player's progress on a specific task within a quest. Uses a
 * numeric progress/target pair — interpretation depends on the task's
 * requirement type (kills, blocks broken, distance travelled, etc.).
 */
@Entity
@Table(
        name = "jexquests_player_task_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_jexquests_ptp_player_quest_task",
                columnNames = {"player_uuid", "quest_identifier", "task_identifier"}
        ),
        indexes = {
                @Index(name = "idx_jexquests_ptp_player", columnList = "player_uuid"),
                @Index(name = "idx_jexquests_ptp_quest", columnList = "quest_identifier")
        }
)
public class PlayerTaskProgress extends LongIdEntity {

    @Column(name = "player_uuid", nullable = false)
    private UUID playerUuid;

    @Column(name = "quest_identifier", nullable = false, length = 64)
    private String questIdentifier;

    @Column(name = "task_identifier", nullable = false, length = 64)
    private String taskIdentifier;

    @Column(name = "progress", nullable = false)
    private long progress;

    @Column(name = "target", nullable = false)
    private long target;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt;

    protected PlayerTaskProgress() {
    }

    public PlayerTaskProgress(
            @NotNull UUID playerUuid,
            @NotNull String questIdentifier,
            @NotNull String taskIdentifier,
            long target
    ) {
        this.playerUuid = playerUuid;
        this.questIdentifier = questIdentifier;
        this.taskIdentifier = taskIdentifier;
        this.target = target;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public @NotNull UUID getPlayerUuid() { return this.playerUuid; }
    public @NotNull String getQuestIdentifier() { return this.questIdentifier; }
    public @NotNull String getTaskIdentifier() { return this.taskIdentifier; }
    public long getProgress() { return this.progress; }
    public void setProgress(long progress) { this.progress = progress; }
    public long getTarget() { return this.target; }
    public void setTarget(long target) { this.target = target; }
    public boolean isCompleted() { return this.completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public @NotNull LocalDateTime getLastUpdatedAt() { return this.lastUpdatedAt; }
    public void setLastUpdatedAt(@NotNull LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

    public int percent() {
        if (this.target <= 0L) return this.completed ? 100 : 0;
        return (int) Math.min(100L, (this.progress * 100L) / this.target);
    }

    @Override
    public String toString() {
        return "PlayerTaskProgress[" + this.playerUuid + "/" + this.questIdentifier + "/"
                + this.taskIdentifier + " " + this.progress + "/" + this.target + "]";
    }
}
