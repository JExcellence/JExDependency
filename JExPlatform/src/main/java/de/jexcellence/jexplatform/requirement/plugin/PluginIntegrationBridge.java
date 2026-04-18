package de.jexcellence.jexplatform.requirement.plugin;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Bridge interface for delegating requirement checks to third-party plugins.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public interface PluginIntegrationBridge {

    /**
     * Returns the plugin identifier this bridge delegates to.
     *
     * @return the plugin ID
     */
    @NotNull String pluginId();

    /**
     * Checks whether the plugin is available and loaded.
     *
     * @return {@code true} when the plugin is active
     */
    boolean isAvailable();

    /**
     * Checks the requirement via the plugin.
     *
     * @param player       the player
     * @param requirementId the requirement identifier within the plugin
     * @param parameters    additional parameters
     * @return {@code true} when the requirement is met
     */
    boolean checkRequirement(@NotNull Player player,
                             @NotNull String requirementId,
                             @NotNull java.util.Map<String, Object> parameters);

    /**
     * Calculates progress via the plugin.
     *
     * @param player       the player
     * @param requirementId the requirement identifier
     * @param parameters    additional parameters
     * @return progress between {@code 0.0} and {@code 1.0}
     */
    double calculateProgress(@NotNull Player player,
                             @NotNull String requirementId,
                             @NotNull java.util.Map<String, Object> parameters);
}
