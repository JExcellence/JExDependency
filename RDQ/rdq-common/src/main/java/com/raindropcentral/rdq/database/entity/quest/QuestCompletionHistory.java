package com.raindropcentral.rdq.database.entity.quest;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Entity representing a historical record of a player's quest completion in the RaindropQuests system.
 * <p>
 * This entity tracks when a player completed a quest, how many times they've completed it,
 * and how long it took. It's used for repeatability tracking, cooldown management, and
 * statistics display.
 * </p>
 *
 * <p>
 * Each completion creates a new record, allowing for full history tracking. The most recent
 * completion record is used for cooldown calculations.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since TBD
 */
@Entity
@Table(
    name = "rdq_quest_completion_history",
    indexes = {
        @Index(name = "idx_quest_completion_player_quest", columnList = "player_id, quest_id"),
        @Index(name = "idx_quest_completion_completed_at", columnList = "completed_at")
    }
)
public class QuestCompletionHistory extends BaseEntity {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * The player who completed the quest.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id", nullable = false)
    private RDQPlayer player;
    
    /**
     * The quest that was completed.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;
    
    /**
     * When the quest was completed.
     */
    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;
    
    /**
     * How many times this player has completed this quest (at the time of this completion).
     */
    @Column(name = "completion_count", nullable = false)
    private int completionCount;
    
    /**
     * How long it took to complete the quest in seconds.
     */
    @Column(name = "time_taken_seconds", nullable = false)
    private long timeTakenSeconds;
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected QuestCompletionHistory() {}
    
    /**
     * Constructs a new {@code QuestCompletionHistory} record.
     *
     * @param player            the player who completed the quest
     * @param quest             the quest that was completed
     * @param completedAt       when the quest was completed
     * @param completionCount   how many times the player has completed this quest
     * @param timeTakenSeconds  how long it took to complete in seconds
     */
    public QuestCompletionHistory(
        @NotNull final RDQPlayer player,
        @NotNull final Quest quest,
        @NotNull final LocalDateTime completedAt,
        final int completionCount,
        final long timeTakenSeconds
    ) {
        this.player = player;
        this.quest = quest;
        this.completedAt = completedAt;
        this.completionCount = completionCount;
        this.timeTakenSeconds = timeTakenSeconds;
    }

    /**
     * Returns the player who completed the quest.
     *
     * @return the player
     */
    @NotNull
    public RDQPlayer getPlayer() {
        return this.player;
    }

    /**
     * Gets the player ID who completed the quest.
     * Convenience method for accessing the player's UUID.
     *
     * @return the player's UUID
     */
    @NotNull
    public java.util.UUID getPlayerId() {
        return this.player.getUniqueId();
    }

    /**
     * Gets the quest identifier.
     * Convenience method for accessing the quest's identifier.
     *
     * @return the quest identifier
     */
    @NotNull
    public String getQuestIdentifier() {
        return this.quest.getIdentifier();
    }

    /**
     * Sets the player for this completion record.
     *
     * @param player the player
     */
    public void setPlayer(@NotNull final RDQPlayer player) {
        this.player = player;
    }

    /**
     * Returns the quest that was completed.
     *
     * @return the quest
     */
    @NotNull
    public Quest getQuest() {
        return this.quest;
    }

    /**
     * Sets the quest for this completion record.
     *
     * @param quest the quest
     */
    public void setQuest(@NotNull final Quest quest) {
        this.quest = quest;
    }

    /**
     * Returns when the quest was completed.
     *
     * @return the completion time
     */
    @NotNull
    public LocalDateTime getCompletedAt() {
        return this.completedAt;
    }

    /**
     * Sets when the quest was completed.
     *
     * @param completedAt the completion time
     */
    public void setCompletedAt(@NotNull final LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    /**
     * Returns how many times the player has completed this quest.
     *
     * @return the completion count
     */
    public int getCompletionCount() {
        return this.completionCount;
    }

    /**
     * Sets the completion count.
     *
     * @param completionCount the completion count
     */
    public void setCompletionCount(final int completionCount) {
        this.completionCount = completionCount;
    }
    
    /**
     * Returns how long it took to complete the quest in seconds.
     *
     * @return the time taken in seconds
     */
    public long getTimeTakenSeconds() {
        return this.timeTakenSeconds;
    }
    
    /**
     * Sets the time taken to complete the quest.
     *
     * @param timeTakenSeconds the time taken in seconds
     */
    public void setTimeTakenSeconds(final long timeTakenSeconds) {
        this.timeTakenSeconds = timeTakenSeconds;
    }

    /**
     * Checks if the player can repeat this quest based on max completions.
     *
     * @return true if the player can repeat the quest, false otherwise
     */
    public boolean canRepeat() {
        if (!this.quest.isRepeatable()) {
            return false;
        }

        int maxCompletions = this.quest.getMaxCompletions();
        if (maxCompletions <= 0) {
            return true; // Unlimited repeats
        }

        return this.completionCount < maxCompletions;
    }
    
    /**
     * Gets the remaining cooldown time in seconds.
     *
     * @return remaining cooldown seconds, or 0 if no cooldown or cooldown expired
     */
    public long getCooldownRemainingSeconds() {
        long cooldownSeconds = this.quest.getCooldownSeconds();
        if (cooldownSeconds <= 0) {
            return 0;
        }

        LocalDateTime cooldownExpires = this.completedAt.plusSeconds(cooldownSeconds);
        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(cooldownExpires)) {
            return 0;
        }

        return ChronoUnit.SECONDS.between(now, cooldownExpires);
    }
    
    /**
     * Checks if the cooldown period has expired.
     *
     * @return true if cooldown has expired or no cooldown exists, false otherwise
     */
    public boolean isCooldownExpired() {
        return getCooldownRemainingSeconds() == 0;
    }

    /**
     * Gets when the cooldown will expire.
     *
     * @return the cooldown expiration time, or null if no cooldown
     */
    public LocalDateTime getCooldownExpiresAt() {
        long cooldownSeconds = this.quest.getCooldownSeconds();
        if (cooldownSeconds <= 0) {
            return null;
        }

        return this.completedAt.plusSeconds(cooldownSeconds);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuestCompletionHistory that)) return false;
        
        if (this.getId() != null && that.getId() != null) {
            return this.getId().equals(that.getId());
        }
        
        if (this.player != null && that.player != null &&
                this.quest != null && that.quest != null &&
                this.completedAt != null && that.completedAt != null) {
            return this.player.equals(that.player) &&
                   this.quest.equals(that.quest) &&
                   this.completedAt.equals(that.completedAt);
        }

        return false;
    }
    
    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return this.getId().hashCode();
        }
        
        if (this.player != null && this.quest != null && this.completedAt != null) {
            return Objects.hash(this.player, this.quest, this.completedAt);
        }

        return System.identityHashCode(this);
    }
    
    @Override
    public String toString() {
        return "QuestCompletionHistory{" +
                "id=" + getId() +
                ", player=" + (player != null ? player.getName() : "null") +
                ", quest=" + (quest != null ? quest.getIdentifier() : "null") +
                ", completedAt=" + completedAt +
                ", completionCount=" + completionCount +
                ", timeTakenSeconds=" + timeTakenSeconds +
                '}';
    }
}
