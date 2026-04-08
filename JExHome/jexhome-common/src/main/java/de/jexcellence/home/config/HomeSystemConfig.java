package de.jexcellence.home.config;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration section for the home system following RDQ BountySection patterns.
 * <p>
 * Contains settings for home limits, teleport settings, and color scheme.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
@CSAlways
public class HomeSystemConfig extends AConfigSection {

    /** Map of permission nodes to home limits. */
    private Map<String, Integer> homeLimits;

    /** Teleport configuration section. */
    private TeleportSection teleport;

    /** Color scheme configuration section. */
    private ColorSchemeSection colors;

    /** Bedrock Edition support configuration section. */
    private BedrockSection bedrock;

    /** Default home name when none is specified. */
    private String defaultHomeName;

    /**
     * Constructs a new HomeSystemConfig with the given evaluation environment builder.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder
     */
    public HomeSystemConfig(final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }

    /**
     * Gets the home limits map.
     *
     * @return the home limits map
     */
    public @NotNull Map<String, Integer> getHomeLimits() {
        if (this.homeLimits == null) {
            return new HashMap<>(Map.of(
                "jexhome.limit.basic", 3,
                "jexhome.limit.vip", 10,
                "jexhome.limit.unlimited", -1
            ));
        }
        return this.homeLimits;
    }

    /**
     * Gets the teleport configuration section.
     *
     * @return the teleport section
     */
    public @NotNull TeleportSection getTeleport() {
        if (this.teleport == null) {
            return new TeleportSection(getBaseEnvironment());
        }
        return this.teleport;
    }

    /**
     * Gets the color scheme configuration section.
     *
     * @return the color scheme section
     */
    public @NotNull ColorSchemeSection getColors() {
        if (this.colors == null) {
            return new ColorSchemeSection(getBaseEnvironment());
        }
        return this.colors;
    }

    /**
     * Gets the default home name.
     *
     * @return the default home name, or "home" if not set
     */
    public @NotNull String getDefaultHomeName() {
        return this.defaultHomeName != null ? this.defaultHomeName : "home";
    }

    /**
     * Gets the Bedrock Edition support configuration section.
     *
     * @return the bedrock section
     */
    public @NotNull BedrockSection getBedrock() {
        if (this.bedrock == null) {
            return new BedrockSection(getBaseEnvironment());
        }
        return this.bedrock;
    }

    /**
     * Checks if Bedrock form support is enabled.
     *
     * @return true if Bedrock forms are enabled
     */
    public boolean isBedrockEnabled() {
        return getBedrock().isEnabled();
    }

    /**
     * Checks if chest GUI should be forced for all players.
     *
     * @return true if chest GUI is forced for all players
     */
    public boolean isForceChestGui() {
        return getBedrock().isForceChestGui();
    }

    // Delegate methods for backward compatibility

    public int getTeleportDelay() {
        return getTeleport().getDelay();
    }

    public boolean isCancelOnMove() {
        return getTeleport().isCancelOnMove();
    }

    public boolean isCancelOnDamage() {
        return getTeleport().isCancelOnDamage();
    }

    public boolean isShowCountdown() {
        return getTeleport().isShowCountdown();
    }

    public boolean isPlaySounds() {
        return getTeleport().isPlaySounds();
    }

    public boolean isShowParticles() {
        return getTeleport().isShowParticles();
    }

    /**
     * Gets the maximum number of homes allowed for a player based on their permissions.
     *
     * @param player the player to check
     * @return the maximum number of homes, or 1 if no permission matches
     */
    public int getMaxHomesForPlayer(final @NotNull Player player) {
        return this.getHomeLimits().entrySet().stream()
            .filter(entry -> player.hasPermission(entry.getKey()))
            .map(Map.Entry::getValue)
            .max(Integer::compare)
            .orElse(1);
    }
}
