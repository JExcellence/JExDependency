package com.raindropcentral.rdq.database.entity.quest;

import com.raindropcentral.rdq.config.utility.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import com.raindropcentral.rdq.quest.model.QuestDifficulty;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entity representing a quest.
 *
 * <p>A quest is a collection of tasks that players must complete to earn rewards.
 * Quests can be repeatable, have cooldowns, time limits, and difficulty levels.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
@Getter
@Setter
@Entity
@Table(
        name = "rdq_quest",
        uniqueConstraints = @UniqueConstraint(columnNames = "identifier"),
        indexes = {
                @Index(name = "idx_quest_identifier", columnList = "identifier"),
                @Index(name = "idx_quest_category", columnList = "category_id"),
                @Index(name = "idx_quest_enabled", columnList = "enabled"),
                @Index(name = "idx_quest_difficulty", columnList = "difficulty")
        }
)
/**
 * Represents the Quest API type.
 */
public class Quest extends BaseEntity {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * Unique identifier for this quest (e.g., "zombie_slayer").
     */
    @Column(name = "identifier", nullable = false, unique = true, length = 64)
    private String identifier;
    
    /**
     * The category this quest belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private QuestCategory category;
    
    /**
     * The icon representing this quest in the UI.
     * Contains material, display name key, description key, and visual properties.
     */
    @Convert(converter = IconSectionConverter.class)
    @Column(name = "icon", nullable = false, columnDefinition = "LONGTEXT")
    private IconSection icon;
    
    /**
     * The difficulty level of this quest.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 32)
    private QuestDifficulty difficulty = QuestDifficulty.NORMAL;
    
    /**
     * Whether this quest can be completed multiple times.
     */
    @Column(name = "repeatable", nullable = false)
    private boolean repeatable = false;
    
    /**
     * Maximum number of times this quest can be completed.
     * 0 means unlimited.
     */
    @Column(name = "max_completions", nullable = false)
    private int maxCompletions = 0;
    
    /**
     * Cooldown period in seconds between quest completions.
     * 0 means no cooldown.
     */
    @Column(name = "cooldown_seconds", nullable = false)
    private long cooldownSeconds = 0;
    
    /**
     * Time limit in seconds for completing the quest after starting.
     * 0 means no time limit.
     */
    @Column(name = "time_limit_seconds", nullable = false)
    private long timeLimitSeconds = 0;
    
    /**
     * Whether this quest is enabled and available to players.
     */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;
    
    /**
     * Tasks that must be completed for this quest.
     */
    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<QuestTask> tasks = new ArrayList<>();
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected Quest() {
    }
    
    /**
     * Constructs a new quest.
     *
     * @param identifier the unique identifier
     * @param category   the quest category
     * @param icon       the icon section with display information
     * @param difficulty the quest difficulty
     */
    public Quest(
            @NotNull final String identifier,
            @NotNull final QuestCategory category,
            @NotNull final IconSection icon,
            @NotNull final QuestDifficulty difficulty
    ) {
        this.identifier = identifier;
        this.category = category;
        this.icon = icon;
        this.difficulty = difficulty;
    }
    
    /**
     * Checks if this quest is repeatable.
     *
     * @return true if repeatable, false otherwise
     */
    public boolean isRepeatable() {
        return repeatable;
    }
    
    /**
     * Checks if this quest has a time limit.
     *
     * @return true if time-limited, false otherwise
     */
    public boolean hasTimeLimit() {
        return timeLimitSeconds > 0;
    }
    
    /**
     * Gets the time limit as a Duration.
     *
     * @return the time limit duration, or Duration.ZERO if no limit
     */
    public Duration getTimeLimit() {
        return Duration.ofSeconds(timeLimitSeconds);
    }
    
    /**
     * Gets the cooldown period as a Duration.
     *
     * @return the cooldown duration, or Duration.ZERO if no cooldown
     */
    public Duration getCooldown() {
        return Duration.ofSeconds(cooldownSeconds);
    }
    
    /**
     * Adds a task to this quest.
     *
     * @param task the task to add
     */
    public void addTask(@NotNull final QuestTask task) {
        tasks.add(task);
        task.setQuest(this);
    }
    
    /**
     * Removes a task from this quest.
     *
     * @param task the task to remove
     */
    public void removeTask(@NotNull final QuestTask task) {
        tasks.remove(task);
        task.setQuest(null);
    }
    
    /**
     * Executes equals.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Quest quest)) return false;
        
        if (this.getId() != null && quest.getId() != null) {
            return this.getId().equals(quest.getId());
        }
        
        return identifier != null && identifier.equals(quest.identifier);
    }
    
    /**
     * Returns whether hCode.
     */
    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return this.getId().hashCode();
        }
        
        return Objects.hash(identifier);
    }
    
    /**
     * Executes toString.
     */
    @Override
    public String toString() {
        return "Quest{" +
                "id=" + getId() +
                ", identifier='" + identifier + '\'' +
                ", difficulty=" + difficulty +
                ", repeatable=" + repeatable +
                ", enabled=" + enabled +
                '}';
    }
}
