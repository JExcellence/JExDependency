package com.raindropcentral.rdq.config.quest;

import com.raindropcentral.rdq.config.requirement.BaseRequirementSection;
import com.raindropcentral.rdq.config.utility.IconSection;
import com.raindropcentral.rdq.config.utility.RewardSection;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.configmapper.sections.CSIgnore;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Configuration section for a quest.
 * <p>
 * Represents a single quest within a category, including its difficulty, icon,
 * prerequisites, unlocks, requirements, rewards, and nested tasks. Quests are
 * the primary unit of player progression within the quest system.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class QuestSection extends AConfigSection {
    
    /** The localization key for the display name of the quest. */
    private String displayNameKey;
    
    /** The localization key for the description of the quest. */
    private String descriptionKey;
    
    /** The icon representing this quest. */
    private IconSection icon;
    
    /** The difficulty level of the quest (EASY, MEDIUM, HARD, EXPERT, MASTER). */
    private String difficulty;
    
    /** Whether this quest is enabled. */
    private Boolean isEnabled;
    
    /** Maximum number of times this quest can be completed (null = unlimited). */
    private Integer maxCompletions;
    
    /** Cooldown in seconds before the quest can be repeated (null = no cooldown). */
    private Integer cooldownSeconds;
    
    /** Time limit in seconds to complete the quest (null = no time limit). */
    private Integer timeLimitSeconds;
    
    /** List of prerequisite quest IDs that must be completed before this quest. */
    private List<String> prerequisites;
    
    /** List of quest IDs that are unlocked when this quest is completed. */
    private List<String> unlocks;
    
    /** Map of requirement keys to their configuration sections. */
    private Map<String, BaseRequirementSection> requirements;
    
    /** Map of reward keys to their configuration sections. */
    private Map<String, RewardSection> rewards;
    
    /** Map of task keys to their configuration sections. */
    private Map<String, QuestTaskSection> tasks;
    
    /** The category ID this quest belongs to (set by parent). */
    @CSIgnore
    private String categoryId;
    
    /** The quest ID (set by parent). */
    @CSIgnore
    private String questId;
    
    /**
     * Constructs a new QuestSection with the given evaluation environment builder.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder
     */
    public QuestSection(
        final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
    ) {
        super(evaluationEnvironmentBuilder);
    }
    
    /**
     * Called after parsing the configuration fields. Sets default localization keys
     * and processes nested tasks.
     *
     * @param fields the list of fields parsed
     * @throws Exception if an error occurs during post-processing
     */
    @Override
    public void afterParsing(final List<Field> fields) throws Exception {
        super.afterParsing(fields);
        
        if (categoryId != null && questId != null) {
            if (displayNameKey == null) {
                displayNameKey = "quest." + categoryId + "." + questId + ".name";
            }
            if (descriptionKey == null) {
                descriptionKey = "quest." + categoryId + "." + questId + ".description";
            }
            
            // Process nested tasks
            if (tasks != null) {
                for (Map.Entry<String, QuestTaskSection> entry : tasks.entrySet()) {
                    QuestTaskSection task = entry.getValue();
                    task.setCategoryId(categoryId);
                    task.setQuestId(questId);
                    task.setTaskId(entry.getKey());
                    task.afterParsing(new ArrayList<>());
                }
            }
        }
    }
    
    /**
     * Gets the display name key for this quest.
     *
     * @return the display name key, or "not_defined" if not set
     */
    public String getDisplayNameKey() {
        return displayNameKey == null ? "not_defined" : displayNameKey;
    }
    
    /**
     * Gets the description key for this quest.
     *
     * @return the description key, or "not_defined" if not set
     */
    public String getDescriptionKey() {
        return descriptionKey == null ? "not_defined" : descriptionKey;
    }
    
    /**
     * Gets the icon section for this quest.
     *
     * @return the icon section, or a new default IconSection if not set
     */
    public IconSection getIcon() {
        return icon == null ? new IconSection(new EvaluationEnvironmentBuilder()) : icon;
    }
    
    /**
     * Gets the difficulty level of this quest.
     *
     * @return the difficulty, or "MEDIUM" if not set
     */
    public String getDifficulty() {
        return difficulty == null ? "MEDIUM" : difficulty;
    }
    
    /**
     * Checks if this quest is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public Boolean getEnabled() {
        return isEnabled != null && isEnabled;
    }
    
    /**
     * Gets the maximum number of completions for this quest.
     *
     * @return the max completions, or null if unlimited
     */
    public Integer getMaxCompletions() {
        return maxCompletions;
    }
    
    /**
     * Gets the cooldown in seconds before the quest can be repeated.
     *
     * @return the cooldown in seconds, or null if no cooldown
     */
    public Integer getCooldownSeconds() {
        return cooldownSeconds;
    }
    
    /**
     * Gets the time limit in seconds to complete the quest.
     *
     * @return the time limit in seconds, or null if no time limit
     */
    public Integer getTimeLimitSeconds() {
        return timeLimitSeconds;
    }
    
    /**
     * Gets the list of prerequisite quest IDs.
     *
     * @return the list of prerequisite quest IDs, or an empty list if not set
     */
    public List<String> getPrerequisites() {
        return prerequisites == null ? new ArrayList<>() : prerequisites;
    }
    
    /**
     * Gets the list of quest IDs that are unlocked when this quest is completed.
     *
     * @return the list of unlocked quest IDs, or an empty list if not set
     */
    public List<String> getUnlocks() {
        return unlocks == null ? new ArrayList<>() : unlocks;
    }
    
    /**
     * Gets the requirements for this quest.
     *
     * @return the map of requirement keys to sections, or an empty map if not set
     */
    public Map<String, BaseRequirementSection> getRequirements() {
        return requirements == null ? new HashMap<>() : requirements;
    }
    
    /**
     * Gets the rewards for this quest.
     *
     * @return the map of reward keys to sections, or an empty map if not set
     */
    public Map<String, RewardSection> getRewards() {
        return rewards == null ? new HashMap<>() : rewards;
    }
    
    /**
     * Gets the tasks for this quest.
     *
     * @return the map of task keys to sections, or an empty map if not set
     */
    public Map<String, QuestTaskSection> getTasks() {
        return tasks == null ? new HashMap<>() : tasks;
    }
    
    /**
     * Gets the category ID this quest belongs to.
     *
     * @return the category ID, or a generated one if not set
     */
    public String getCategoryId() {
        return categoryId == null ? "not_defined_" + UUID.randomUUID() : categoryId;
    }
    
    /**
     * Sets the category ID this quest belongs to.
     *
     * @param categoryId the category ID
     */
    public void setCategoryId(final String categoryId) {
        this.categoryId = categoryId;
    }
    
    /**
     * Gets the quest ID.
     *
     * @return the quest ID, or a generated one if not set
     */
    public String getQuestId() {
        return questId == null ? "not_defined_" + UUID.randomUUID() : questId;
    }
    
    /**
     * Sets the quest ID.
     *
     * @param questId the quest ID
     */
    public void setQuestId(final String questId) {
        this.questId = questId;
    }
}
