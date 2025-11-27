package com.raindropcentral.rdq.requirement.plugins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.api.eco.EcoSkillsService;
import com.raindropcentral.rplatform.api.mcmmo.McMMOService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unified requirement implementation for multiple skill plugins.
 * <p>
 * This requirement checks whether a player has reached a specified level in particular skills,
 * or a total combined level across all skills, as defined by various skill plugins.
 * It supports both individual skill requirements, multiple skill requirements, and a "total" 
 * (aggregate) level requirement across different skill plugin systems.
 * </p>
 * <p>
 * <b>Supported Skill Plugins:</b>
 * <ul>
 *   <li><b>EcoSkills:</b> Modern skill system with custom skills</li>
 *   <li><b>McMMO:</b> Popular RPG skill system with various abilities</li>
 *   <li><b>Future plugins:</b> Extensible architecture for additional skill systems</li>
 * </ul>
 * </p>
 * <ul>
 *   <li>If {@code skill} is {@code null} or equals "total" (case-insensitive), the requirement checks the player's total combined skill level.</li>
 *   <li>For single skill requirements, it checks the player's level in the specified skill.</li>
 *   <li>For multiple skill requirements, all specified skills must meet their required levels.</li>
 *   <li>Supports configuration via RequirementSection with flexible field names.</li>
 * </ul>
 *
 * Used in the RaindropQuests system to gate progression, upgrades, or features behind
 * skill achievements across different skill plugin systems.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class SkillsRequirement extends AbstractRequirement {

    private static final Logger LOGGER = Logger.getLogger(SkillsRequirement.class.getName());

    /**
     * Enumeration of supported skill plugins.
     */
    public enum SkillPlugin {
        /**
         * EcoSkills plugin.
         */
        ECO_SKILLS("EcoSkills", "ecoskills"),
        
        AURA_SKILLS("AuraSkills", "auraskills"),
        
        /**
         * McMMO plugin.
         */
        MCMMO("mcMMO", "mcmmo"),
        
        /**
         * Auto-detect the available skill plugin.
         */
        AUTO("Auto", "auto");

        private final String pluginName;
        private final String identifier;

        SkillPlugin(final String pluginName, final String identifier) {
            this.pluginName = pluginName;
            this.identifier = identifier;
        }

        /**
         * Gets the plugin name.
         *
         * @return The plugin name.
         */
        public String getPluginName() {
            return this.pluginName;
        }

        /**
         * Gets the identifier.
         *
         * @return The identifier.
         */
        public String getIdentifier() {
            return this.identifier;
        }

        /**
         * Gets a SkillPlugin by identifier.
         *
         * @param identifier The identifier.
         * @return The SkillPlugin, or AUTO if not found.
         */
        public static SkillPlugin fromIdentifier(final String identifier) {
            if (identifier == null) {
                return AUTO;
            }
            
            for (final SkillPlugin plugin : values()) {
                if (plugin.identifier.equalsIgnoreCase(identifier)) {
                    return plugin;
                }
            }
            return AUTO;
        }
    }

    /**
     * The skill plugin to use for this requirement.
     */
    @JsonProperty("skillPlugin")
    private final SkillPlugin skillPlugin;

    /**
     * The name of the skill to check, or "total" for aggregate level.
     * Used for single skill requirements.
     */
    @JsonProperty("skill")
    private final String skill;

    /**
     * The required level for the skill or total combined level.
     * Used for single skill requirements.
     */
    @JsonProperty("level")
    private final int level;

    /**
     * Map of multiple skill requirements.
     * Maps skill names to required levels for multiple skill requirements.
     */
    @JsonProperty("skills")
    private final Map<String, Integer> skills;

    /**
     * Optional description for this skills requirement.
     */
    @JsonProperty("description")
    private final String description;

    /**
     * Cached skill service for the specified plugin.
     * This is resolved at runtime and not serialized.
     */
    @JsonIgnore
    private transient Object skillService;

    /**
     * Constructs a new SkillsRequirement for a single skill with auto-detection.
     *
     * @param skill The name of the skill to check, or {@code null}/"total" for total combined level.
     * @param level The required level for the skill or total combined level.
     */
    public SkillsRequirement(
            @Nullable final String skill, 
            final int level
    ) {
        this(SkillPlugin.AUTO, skill, level, null, null);
    }

    /**
     * Constructs a new SkillsRequirement for a single skill with specific plugin.
     *
     * @param skillPlugin The skill plugin to use.
     * @param skill The name of the skill to check, or {@code null}/"total" for total combined level.
     * @param level The required level for the skill or total combined level.
     */
    public SkillsRequirement(
            @NotNull final SkillPlugin skillPlugin,
            @Nullable final String skill, 
            final int level
    ) {
        this(skillPlugin, skill, level, null, null);
    }

    /**
     * Constructs a new SkillsRequirement for multiple skills.
     *
     * @param skillPlugin The skill plugin to use.
     * @param skills Map of skill names to required levels.
     */
    public SkillsRequirement(
            @NotNull final SkillPlugin skillPlugin,
            @NotNull final Map<String, Integer> skills
    ) {
        this(skillPlugin, null, 0, skills, null);
    }

    /**
     * Constructs a new SkillsRequirement with full configuration options.
     *
     * @param skillPlugin The skill plugin to use (can be null for auto-detection).
     * @param skill Single skill name (can be null if using skills map).
     * @param level Single skill level (ignored if using skills map).
     * @param skills Map of multiple skills and levels (can be null if using single skill).
     * @param description Optional description for this requirement.
     */
    @JsonCreator
    public SkillsRequirement(
            @JsonProperty("skillPlugin") @Nullable final SkillPlugin skillPlugin,
            @JsonProperty("skill") @Nullable final String skill,
            @JsonProperty("level") final int level,
            @JsonProperty("skills") @Nullable final Map<String, Integer> skills,
            @JsonProperty("description") @Nullable final String description
    ) {
        super(Type.SKILLS);

        // Validate that we have either a single skill or multiple skills
        final boolean hasSingleSkill = skill != null && level > 0;
        final boolean hasMultipleSkills = skills != null && !skills.isEmpty();

        if (!hasSingleSkill && !hasMultipleSkills) {
            throw new IllegalArgumentException("Either a single skill with level or multiple skills must be specified.");
        }
	    
	    if (hasMultipleSkills) {
            for (final Map.Entry<String, Integer> entry : skills.entrySet()) {
                if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                    throw new IllegalArgumentException("Skill name cannot be null or empty.");
                }
                if (entry.getValue() == null || entry.getValue() <= 0) {
                    throw new IllegalArgumentException("Skill level must be positive for skill: " + entry.getKey());
                }
            }
        }

        this.skillPlugin = skillPlugin != null ? skillPlugin : SkillPlugin.AUTO;
        this.skill = skill;
        this.level = level;
        this.skills = skills != null ? new HashMap<>(skills) : new HashMap<>();
        this.description = description;
    }

    /**
     * Factory method to create SkillsRequirement from string plugin identifier.
     *
     * @param pluginIdentifier The plugin identifier string.
     * @param skill The skill name.
     * @param level The required level.
     * @return A new SkillsRequirement instance.
     */
    @JsonIgnore
    @NotNull
    public static SkillsRequirement fromPluginString(
            @Nullable final String pluginIdentifier,
            @Nullable final String skill,
            final int level
    ) {
        final SkillPlugin plugin = SkillPlugin.fromIdentifier(pluginIdentifier);
        return new SkillsRequirement(plugin, skill, level, null, null);
    }

    /**
     * Checks if the player meets the skill or total combined level requirement.
     *
     * @param player The player to check.
     * @return {@code true} if the player meets or exceeds the required level(s), {@code false} otherwise.
     */
    @Override
    public boolean isMet(
            @NotNull final Player player
    ) {
        final Object service = this.getSkillService();
        if (service == null) {
            LOGGER.log(Level.WARNING, "Skill service not available for plugin: " + this.skillPlugin);
            return false;
        }

        // Check multiple skills if specified
        if (!this.skills.isEmpty()) {
            return this.skills.entrySet().stream()
                    .allMatch(entry -> this.checkSingleSkill(player, service, entry.getKey(), entry.getValue()));
        }

        // Check single skill
        if (this.skill != null) {
            return this.checkSingleSkill(player, service, this.skill, this.level);
        }

        return false;
    }

    /**
     * Calculates the progress toward fulfilling this skills requirement for the specified player.
     * <p>
     * For single skill requirements, progress is calculated as {@code currentLevel / requiredLevel}.
     * For multiple skill requirements, progress is the average progress across all skills.
     * Progress is clamped to 1.0 if the requirement is met.
     * </p>
     *
     * @param player The player whose progress is being calculated.
     * @return A double between 0.0 and 1.0 representing completion progress.
     */
    @Override
    public double calculateProgress(
            @NotNull final Player player
    ) {
        final Object service = this.getSkillService();
        if (service == null) {
            LOGGER.log(Level.WARNING, "Skill service not available for plugin: " + this.skillPlugin);
            return 0.0;
        }

        // Calculate progress for multiple skills
        if (!this.skills.isEmpty()) {
            double totalProgress = 0.0;
            for (final Map.Entry<String, Integer> entry : this.skills.entrySet()) {
                final double skillProgress = this.calculateSingleSkillProgress(player, service, entry.getKey(), entry.getValue());
                totalProgress += skillProgress;
            }
            return Math.min(1.0, totalProgress / this.skills.size());
        }

        // Calculate progress for single skill
        if (this.skill != null) {
            return this.calculateSingleSkillProgress(player, service, this.skill, this.level);
        }

        return 0.0;
    }

    /**
     * Consumes resources from the player to fulfill this requirement.
     * <p>
     * Not applicable for skills requirements; this method is a no-op.
     * </p>
     *
     * @param player The player from whom resources would be consumed.
     */
    @Override
    public void consume(
            @NotNull final Player player
    ) {
        // Skill levels are not consumed
    }

    /**
     * Gets the translation key for the requirement's description.
     * <p>
     * Used for localization and display in the UI.
     * </p>
     *
     * @return The language key for this requirement's description.
     */
    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.skills." + this.skillPlugin.getIdentifier();
    }

    /**
     * Gets the skill plugin used by this requirement.
     *
     * @return The skill plugin.
     */
    @NotNull
    public SkillPlugin getSkillPlugin() {
        return this.skillPlugin;
    }

    /**
     * Gets the required level for this requirement (single skill mode).
     *
     * @return The required skill or total combined level.
     */
    public int getLevel() {
        return this.level;
    }

    /**
     * Gets the skill name for this requirement (single skill mode).
     *
     * @return The skill name, or "total" for combined level.
     */
    @Nullable
    public String getSkill() {
        return this.skill;
    }

    /**
     * Gets the map of required skills and their levels (multiple skills mode).
     *
     * @return Unmodifiable map of skill names to required levels.
     */
    @NotNull
    public Map<String, Integer> getSkills() {
        return Collections.unmodifiableMap(this.skills);
    }

    /**
     * Gets the optional description for this skills requirement.
     *
     * @return The description, or null if not provided.
     */
    @Nullable
    public String getDescription() {
        return this.description;
    }

    /**
     * Gets detailed progress information for each skill for the specified player.
     *
     * @param player The player whose progress will be calculated.
     * @return A map of skill names to their current levels.
     */
    @JsonIgnore
    @NotNull
    public Map<String, Integer> getCurrentSkillLevels(
            @NotNull final Player player
    ) {
        final Object service = this.getSkillService();
        if (service == null) {
            return new HashMap<>();
        }

        final Map<String, Integer> currentLevels = new HashMap<>();

        // Get levels for multiple skills
        if (!this.skills.isEmpty()) {
            for (final String skillName : this.skills.keySet()) {
                final int currentLevel = this.getCurrentSkillLevel(player, service, skillName);
                currentLevels.put(skillName, currentLevel);
            }
        }

        // Get level for single skill
        if (this.skill != null) {
            final int currentLevel = this.getCurrentSkillLevel(player, service, this.skill);
            currentLevels.put(this.skill, currentLevel);
        }

        return currentLevels;
    }

    /**
     * Checks if this requirement uses multiple skills.
     *
     * @return True if multiple skills are required, false for single skill.
     */
    @JsonIgnore
    public boolean isMultipleSkills() {
        return !this.skills.isEmpty();
    }

    /**
     * Checks if this requirement uses the total skill level.
     *
     * @return True if checking total skill level, false otherwise.
     */
    @JsonIgnore
    public boolean isTotalSkillLevel() {
        return this.skill != null && this.skill.equalsIgnoreCase("total");
    }

    /**
     * Gets the detected skill plugin being used.
     *
     * @return The detected skill plugin, or null if none available.
     */
    @JsonIgnore
    @Nullable
    public SkillPlugin getDetectedSkillPlugin() {
        if (this.skillPlugin != SkillPlugin.AUTO) {
            return this.skillPlugin;
        }

        // Auto-detect available plugin
        if (Bukkit.getPluginManager().getPlugin("EcoSkills") != null) {
            return SkillPlugin.ECO_SKILLS;
        }
        if (Bukkit.getPluginManager().getPlugin("mcMMO") != null) {
            return SkillPlugin.MCMMO;
        }

        return null;
    }

    /**
     * Validates the internal state of this skills requirement.
     *
     * @throws IllegalStateException If the requirement is in an invalid state.
     */
    @JsonIgnore
    public void validate() {
        final boolean hasSingleSkill = this.skill != null && this.level > 0;
        final boolean hasMultipleSkills = !this.skills.isEmpty();

        if (!hasSingleSkill && !hasMultipleSkills) {
            throw new IllegalStateException("Either a single skill or multiple skills must be specified.");
        }
	    
	    if (hasMultipleSkills) {
            for (final Map.Entry<String, Integer> entry : this.skills.entrySet()) {
                if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                    throw new IllegalStateException("Skill name cannot be null or empty.");
                }
                if (entry.getValue() == null || entry.getValue() <= 0) {
                    throw new IllegalStateException("Skill level must be positive for skill: " + entry.getKey());
                }
            }
        }

        // Validate that the skill plugin is available
        final SkillPlugin detectedPlugin = this.getDetectedSkillPlugin();
        if (detectedPlugin == null) {
            throw new IllegalStateException("No supported skill plugin found. Available plugins: EcoSkills, mcMMO");
        }
    }

    /**
     * Gets the skill service for the configured plugin, resolving it at runtime if necessary.
     *
     * @return The skill service, or null if not available.
     */
    @Nullable
    private Object getSkillService() {
        if (this.skillService == null) {
            final SkillPlugin targetPlugin = this.skillPlugin == SkillPlugin.AUTO ? this.getDetectedSkillPlugin() : this.skillPlugin;
            
            if (targetPlugin == null) {
                return null;
            }

            switch (targetPlugin) {
                case ECO_SKILLS -> {
                    final Plugin ecoSkillsPlugin = Bukkit.getPluginManager().getPlugin("EcoSkills");
                    if (ecoSkillsPlugin != null && ecoSkillsPlugin.isEnabled()) {
                        this.skillService = new EcoSkillsService();
                    }
                }
                case MCMMO -> {
                    final Plugin mcmmoPlugin = Bukkit.getPluginManager().getPlugin("mcMMO");
                    if (mcmmoPlugin != null && mcmmoPlugin.isEnabled()) {
                        this.skillService = new McMMOService();
                    }
                }
            }
        }
        return this.skillService;
    }

    /**
     * Checks if a player meets a single skill requirement.
     *
     * @param player The player to check.
     * @param service The skill service.
     * @param skillName The skill name.
     * @param requiredLevel The required level.
     * @return True if the requirement is met.
     */
    private boolean checkSingleSkill(
            @NotNull final Player player,
            @NotNull final Object service,
            @NotNull final String skillName,
            final int requiredLevel
    ) {
        if (skillName.equalsIgnoreCase("total")) {
            if (service instanceof final EcoSkillsService ecoService) {
                return requiredLevel <= ecoService.getTotalSkillLevel(player);
            } else if (service instanceof final McMMOService mcmmoService) {
                return requiredLevel <= mcmmoService.getTotalSkillLevel(player);
            }
        } else {
            if (service instanceof final EcoSkillsService ecoService) {
                return requiredLevel <= ecoService.getSkillLevel(player, skillName);
            } else if (service instanceof final McMMOService mcmmoService) {
                return requiredLevel <= mcmmoService.getSkillLevel(player, skillName);
            }
        }
        return false;
    }

    /**
     * Calculates progress for a single skill.
     *
     * @param player The player.
     * @param service The skill service.
     * @param skillName The skill name.
     * @param requiredLevel The required level.
     * @return Progress value between 0.0 and 1.0.
     */
    private double calculateSingleSkillProgress(
            @NotNull final Player player,
            @NotNull final Object service,
            @NotNull final String skillName,
            final int requiredLevel
    ) {
        if (requiredLevel <= 0) {
            return 1.0;
        }

        final int currentLevel = this.getCurrentSkillLevel(player, service, skillName);
        return Math.min(1.0, (double) currentLevel / requiredLevel);
    }

    /**
     * Gets the current level for a specific skill.
     *
     * @param player The player.
     * @param service The skill service.
     * @param skillName The skill name.
     * @return The current level.
     */
    private int getCurrentSkillLevel(
            @NotNull final Player player,
            @NotNull final Object service,
            @NotNull final String skillName
    ) {
        if (skillName.equalsIgnoreCase("total")) {
            if (service instanceof final EcoSkillsService ecoService) {
                return ecoService.getTotalSkillLevel(player);
            } else if (service instanceof final McMMOService mcmmoService) {
                return mcmmoService.getTotalSkillLevel(player);
            }
        } else {
            if (service instanceof final EcoSkillsService ecoService) {
                return ecoService.getSkillLevel(player, skillName);
            } else if (service instanceof final McMMOService mcmmoService) {
                return mcmmoService.getSkillLevel(player, skillName);
            }
        }
        return 0;
    }
}