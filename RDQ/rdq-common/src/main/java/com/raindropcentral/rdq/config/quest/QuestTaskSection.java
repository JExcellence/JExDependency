package com.raindropcentral.rdq.config.quest;

import com.raindropcentral.rdq.config.requirement.BaseRequirementSection;
import com.raindropcentral.rdq.config.utility.IconSection;
import com.raindropcentral.rdq.config.utility.RewardSection;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.configmapper.sections.CSIgnore;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Configuration section for a quest task.
 * <p>
 * Represents a single task within a quest, including its type, target amount,
 * requirements, rewards, and display properties. Tasks are the smallest unit
 * of quest progression.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class QuestTaskSection extends AConfigSection {
    
    /** The localization key for the display name of the task. */
    private String displayNameKey;
    
    /** The localization key for the description of the task. */
    private String descriptionKey;
    
    /** The icon representing this task. */
    private IconSection icon;
    
    /** The type of task (KILL, COLLECT, INTERACT, LOCATION, etc.). */
    private String taskType;
    
    /** The target amount required to complete this task. */
    private Integer targetAmount;
    
    /** Whether this task is optional for quest completion. */
    private Boolean isOptional;
    
    /** Map of requirement keys to their configuration sections. */
    private Map<String, BaseRequirementSection> requirements;
    
    /** Map of reward keys to their configuration sections. */
    private Map<String, RewardSection> rewards;
    
    /** The category ID this task belongs to (set by parent). */
    @CSIgnore
    private String categoryId;
    
    /** The quest ID this task belongs to (set by parent). */
    @CSIgnore
    private String questId;
    
    /** The task ID (set by parent). */
    @CSIgnore
    private String taskId;
    
    /**
     * Constructs a new QuestTaskSection with the given evaluation environment builder.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder
     */
    public QuestTaskSection(
        final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
    ) {
        super(evaluationEnvironmentBuilder);
    }
    
    /**
     * Called after parsing the configuration fields. Sets default localization keys if not provided.
     *
     * @param fields the list of fields parsed
     * @throws Exception if an error occurs during post-processing
     */
    @Override
    public void afterParsing(final List<Field> fields) throws Exception {
        super.afterParsing(fields);
        
        if (categoryId != null && questId != null && taskId != null) {
            if (displayNameKey == null) {
                displayNameKey = "quest." + categoryId + "." + questId + ".task." + taskId + ".name";
            }
            if (descriptionKey == null) {
                descriptionKey = "quest." + categoryId + "." + questId + ".task." + taskId + ".description";
            }
        }
    }
    
    /**
     * Gets the display name key for this task.
     *
     * @return the display name key, or "not_defined" if not set
     */
    public String getDisplayNameKey() {
        return displayNameKey == null ? "not_defined" : displayNameKey;
    }
    
    /**
     * Gets the description key for this task.
     *
     * @return the description key, or "not_defined" if not set
     */
    public String getDescriptionKey() {
        return descriptionKey == null ? "not_defined" : descriptionKey;
    }
    
    /**
     * Gets the icon section for this task.
     *
     * @return the icon section, or a new default IconSection if not set
     */
    public IconSection getIcon() {
        return icon == null ? new IconSection(new EvaluationEnvironmentBuilder()) : icon;
    }
    
    /**
     * Gets the task type.
     *
     * @return the task type, or "CUSTOM" if not set
     */
    public String getTaskType() {
        return taskType == null ? "CUSTOM" : taskType;
    }
    
    /**
     * Gets the target amount for this task.
     *
     * @return the target amount, or 1 if not set
     */
    public Integer getTargetAmount() {
        return targetAmount == null ? 1 : targetAmount;
    }
    
    /**
     * Checks if this task is optional.
     *
     * @return true if optional, false otherwise
     */
    public Boolean getOptional() {
        return isOptional != null && isOptional;
    }
    
    /**
     * Gets the requirements for this task.
     *
     * @return the map of requirement keys to sections, or an empty map if not set
     */
    public Map<String, BaseRequirementSection> getRequirements() {
        return requirements == null ? new HashMap<>() : requirements;
    }
    
    /**
     * Gets the rewards for this task.
     *
     * @return the map of reward keys to sections, or an empty map if not set
     */
    public Map<String, RewardSection> getRewards() {
        return rewards == null ? new HashMap<>() : rewards;
    }
    
    /**
     * Gets the category ID this task belongs to.
     *
     * @return the category ID, or a generated one if not set
     */
    public String getCategoryId() {
        return categoryId == null ? "not_defined_" + UUID.randomUUID() : categoryId;
    }
    
    /**
     * Sets the category ID this task belongs to.
     *
     * @param categoryId the category ID
     */
    public void setCategoryId(final String categoryId) {
        this.categoryId = categoryId;
    }
    
    /**
     * Gets the quest ID this task belongs to.
     *
     * @return the quest ID, or a generated one if not set
     */
    public String getQuestId() {
        return questId == null ? "not_defined_" + UUID.randomUUID() : questId;
    }
    
    /**
     * Sets the quest ID this task belongs to.
     *
     * @param questId the quest ID
     */
    public void setQuestId(final String questId) {
        this.questId = questId;
    }
    
    /**
     * Gets the task ID.
     *
     * @return the task ID, or a generated one if not set
     */
    public String getTaskId() {
        return taskId == null ? "not_defined_" + UUID.randomUUID() : taskId;
    }
    
    /**
     * Sets the task ID.
     *
     * @param taskId the task ID
     */
    public void setTaskId(final String taskId) {
        this.taskId = taskId;
    }
}
