package de.jexcellence.quests.database.entity;

import de.jexcellence.jehibernate.entity.base.LongIdEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A quest definition. Stored in the database to support hot-swap via
 * admin tools; the canonical source is still the YAML file under
 * {@code resources/quests/definitions/**}. Icon, reward, and
 * requirement configurations are kept as JSON blobs — the service
 * layer parses them into sealed-interface domain types.
 */
@Entity
@Table(
        name = "jexquests_quest",
        uniqueConstraints = @UniqueConstraint(columnNames = "identifier"),
        indexes = {
                @Index(name = "idx_jexquests_quest_identifier", columnList = "identifier"),
                @Index(name = "idx_jexquests_quest_category", columnList = "category"),
                @Index(name = "idx_jexquests_quest_enabled", columnList = "enabled")
        }
)
public class Quest extends LongIdEntity {

    @Column(name = "identifier", nullable = false, unique = true, length = 64)
    private String identifier;

    @Column(name = "category", nullable = false, length = 32)
    private String category;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Lob
    @Column(name = "icon_data")
    private String iconData;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 16)
    private QuestDifficulty difficulty = QuestDifficulty.MEDIUM;

    @Column(name = "repeatable", nullable = false)
    private boolean repeatable;

    @Column(name = "max_completions", nullable = false)
    private int maxCompletions;

    @Column(name = "cooldown_seconds", nullable = false)
    private long cooldownSeconds;

    @Column(name = "time_limit_seconds", nullable = false)
    private long timeLimitSeconds;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Lob
    @Column(name = "requirement_data")
    private String requirementData;

    @Lob
    @Column(name = "reward_data")
    private String rewardData;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "jexquests_quest_prerequisites",
            joinColumns = @JoinColumn(name = "quest_id")
    )
    @Column(name = "prerequisite_identifier", length = 64)
    private List<String> prerequisites = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "jexquests_quest_dependents",
            joinColumns = @JoinColumn(name = "quest_id")
    )
    @Column(name = "dependent_identifier", length = 64)
    private List<String> dependents = new ArrayList<>();

    protected Quest() {
    }

    public Quest(
            @NotNull String identifier,
            @NotNull String category,
            @NotNull String displayName,
            @NotNull QuestDifficulty difficulty
    ) {
        this.identifier = identifier;
        this.category = category;
        this.displayName = displayName;
        this.difficulty = difficulty;
    }

    public @NotNull String getIdentifier() { return this.identifier; }
    public @NotNull String getCategory() { return this.category; }
    public void setCategory(@NotNull String category) { this.category = category; }
    public @NotNull String getDisplayName() { return this.displayName; }
    public void setDisplayName(@NotNull String displayName) { this.displayName = displayName; }
    public @Nullable String getIconData() { return this.iconData; }
    public void setIconData(@Nullable String iconData) { this.iconData = iconData; }
    public @NotNull QuestDifficulty getDifficulty() { return this.difficulty; }
    public void setDifficulty(@NotNull QuestDifficulty difficulty) { this.difficulty = difficulty; }
    public boolean isRepeatable() { return this.repeatable; }
    public void setRepeatable(boolean repeatable) { this.repeatable = repeatable; }
    public int getMaxCompletions() { return this.maxCompletions; }
    public void setMaxCompletions(int maxCompletions) { this.maxCompletions = maxCompletions; }
    public long getCooldownSeconds() { return this.cooldownSeconds; }
    public void setCooldownSeconds(long cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }
    public long getTimeLimitSeconds() { return this.timeLimitSeconds; }
    public void setTimeLimitSeconds(long timeLimitSeconds) { this.timeLimitSeconds = timeLimitSeconds; }
    public boolean isEnabled() { return this.enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public @Nullable String getRequirementData() { return this.requirementData; }
    public void setRequirementData(@Nullable String requirementData) { this.requirementData = requirementData; }
    public @Nullable String getRewardData() { return this.rewardData; }
    public void setRewardData(@Nullable String rewardData) { this.rewardData = rewardData; }
    public @NotNull List<String> getPrerequisites() { return this.prerequisites; }
    public @NotNull List<String> getDependents() { return this.dependents; }

    @Override
    public String toString() {
        return "Quest[" + this.identifier + "/" + this.category + "/" + this.difficulty + "]";
    }
}
