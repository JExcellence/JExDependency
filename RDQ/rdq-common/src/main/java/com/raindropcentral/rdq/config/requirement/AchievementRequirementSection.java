package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration section for achievement-based requirements.
 * <p>
 * This section handles all configuration options specific to {@code AchievementRequirement},
 * including required achievements, achievement checking modes, and plugin integration hints
 * for evaluating completion state.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
@CSAlways
public class AchievementRequirementSection extends AConfigSection {

    // ~~~ ACHIEVEMENT-SPECIFIC PROPERTIES ~~~

    /**
     * Single required achievement.
     * YAML key: "requiredAchievement"
     */
    private String requiredAchievement;

    /**
     * Alternative achievement field name.
     * YAML key: "achievement"
     */
    private String achievement;

    /**
     * List of required achievements.
     * YAML key: "requiredAchievements"
     */
    private List<String> requiredAchievements;

    /**
     * Alternative achievements list field name.
     * YAML key: "achievements"
     */
    private List<String> achievements;

    /**
     * Whether all achievements must be completed (AND) or just one (OR).
     * YAML key: "requireAll"
     */
    private Boolean requireAll;

    /**
     * Achievement plugin identifier (e.g., "advancedachievements", "achievements").
     * YAML key: "achievementPlugin"
     */
    private String achievementPlugin;

    /**
     * Constructs a new AchievementRequirementSection.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder
     */    /**
     * Default no-arg constructor for Jackson deserialization.
     */
    protected AchievementRequirementSection() {
        super(new EvaluationEnvironmentBuilder());
    }

    public AchievementRequirementSection(EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }

    // ~~~ GETTERS ~~~

    /**
     * Gets the single required achievement, trying multiple field names.
     *
     * @return the required achievement, or an empty string when none is defined
     */
    public String getRequiredAchievement() {
        if (this.requiredAchievement != null) {
            return this.requiredAchievement;
        }
        if (this.achievement != null) {
            return this.achievement;
        }
        return "";
    }

    /**
     * Gets the complete list of required achievements from all sources.
     *
     * @return combined list of all required achievements while avoiding duplicate single-entry
     *         aliases
     */
    public List<String> getRequiredAchievements() {
        List<String> achievementList = new ArrayList<>();

        // Add achievements from requiredAchievements list
        if (this.requiredAchievements != null) {
            achievementList.addAll(this.requiredAchievements);
        }

        // Add achievements from alternative achievements list
        if (this.achievements != null) {
            achievementList.addAll(this.achievements);
        }

        // Add single achievement if specified
        String singleAchievement = getRequiredAchievement();
        if (!singleAchievement.isEmpty() && !achievementList.contains(singleAchievement)) {
            achievementList.add(singleAchievement);
        }

        return achievementList;
    }

    /**
     * Gets whether all achievements must be completed.
     *
     * @return {@code true} if all achievements are required, or {@code true} by default when
     *         unspecified
     */
    public Boolean getRequireAll() {
        return this.requireAll != null ? this.requireAll : true;
    }

    /**
     * Gets the achievement plugin identifier.
     *
     * @return the achievement plugin identifier, defaulting to {@code "advancedachievements"}
     */
    public String getAchievementPlugin() {
        return this.achievementPlugin != null ? this.achievementPlugin : "advancedachievements";
    }
}
