package com.raindropcentral.rdq.database.entity.quest;

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a player's quest completion history.
 * <p>
 * Tracks how many times a player has completed a quest and when they can
 * repeat it again (for repeatable quests with cooldowns).
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
@Getter
@Setter
@Entity
@Table(
        name = "rdq_quest_completion_history",
        indexes = {
                @Index(name = "idx_quest_completion_player", columnList = "player_id"),
                @Index(name = "idx_quest_completion_quest", columnList = "quest_identifier"),
                @Index(name = "idx_quest_completion_player_quest", columnList = "player_id, quest_identifier"),
                @Index(name = "idx_quest_completion_next_available", columnList = "next_available_at")
        }
)
public class QuestCompletionHistory extends BaseEntity {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * The player's unique identifier.
     */
    @Column(name = "player_id", nullable = false)
    private UUID playerId;
    
    /**
     * The quest identifier.
     */
    @Column(name = "quest_identifier", nullable = false, length = 64)
    private String questIdentifier;
    
    /**
     * Timestamp of the most recent completion.
     */
    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;
    
    /**
     * Total number of times this quest has been completed.
     */
    @Column(name = "completion_count", nullable = false)
    private int completionCount = 1;
    
    /**
     * Timestamp when this quest becomes available again.
     * Null if no cooldown or quest is not repeatable.
     */
    @Column(name = "next_available_at")
    private Instant nextAvailableAt;
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected QuestCompletionHistory() {
    }
    
    /**
     * Constructs a new quest completion history entry.
     *
     * @param playerId         the player's unique identifier
     * @param questIdentifier  the quest identifier
     * @param completedAt      when the quest was completed
     * @param completionCount  the total completion count
     * @param nextAvailableAt  when the quest becomes available again
     */
    public QuestCompletionHistory(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier,
            @NotNull final Instant completedAt,
            final int completionCount,
            final Instant nextAvailableAt
    ) {
        this.playerId = playerId;
        this.questIdentifier = questIdentifier;
        this.completedAt = completedAt;
        this.completionCount = completionCount;
        this.nextAvailableAt = nextAvailableAt;
    }
    
    /**
     * Factory method to create a new completion history entry.
     *
     * @param playerId        the player's unique identifier
     * @param questIdentifier the quest identifier
     * @param cooldown        the cooldown duration before the quest can be repeated
     * @return a new quest completion history instance
     */
    public static QuestCompletionHistory create(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier,
            @NotNull final Duration cooldown
    ) {
        final Instant now = Instant.now();
        final Instant nextAvailable = cooldown.isZero() ? null : now.plus(cooldown);
        return new QuestCompletionHistory(playerId, questIdentifier, now, 1, nextAvailable);
    }
    
    /**
     * Checks if this quest can be repeated now.
     *
     * @return true if the cooldown has expired or there is no cooldown, false otherwise
     */
    public boolean canRepeat() {
        return nextAvailableAt == null || Instant.now().isAfter(nextAvailableAt);
    }
    
    /**
     * Gets the remaining cooldown duration.
     *
     * @return the remaining duration, or Duration.ZERO if no cooldown or cooldown expired
     */
    public Duration getRemainingCooldown() {
        if (canRepeat()) {
            return Duration.ZERO;
        }
        final Duration remaining = Duration.between(Instant.now(), nextAvailableAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
    
    /**
     * Records a new completion of this quest.
     *
     * @param cooldown the cooldown duration before the quest can be repeated again
     */
    public void recordCompletion(@NotNull final Duration cooldown) {
        this.completedAt = Instant.now();
        this.completionCount++;
        this.nextAvailableAt = cooldown.isZero() ? null : Instant.now().plus(cooldown);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuestCompletionHistory that)) return false;
        
        if (this.getId() != null && that.getId() != null) {
            return this.getId().equals(that.getId());
        }
        
        return playerId != null && playerId.equals(that.playerId) &&
                questIdentifier != null && questIdentifier.equals(that.questIdentifier);
    }
    
    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return this.getId().hashCode();
        }
        
        return Objects.hash(playerId, questIdentifier);
    }
    
    @Override
    public String toString() {
        return "QuestCompletionHistory{" +
                "id=" + getId() +
                ", playerId=" + playerId +
                ", questIdentifier='" + questIdentifier + '\'' +
                ", completionCount=" + completionCount +
                ", completedAt=" + completedAt +
                ", nextAvailableAt=" + nextAvailableAt +
                '}';
    }
}
