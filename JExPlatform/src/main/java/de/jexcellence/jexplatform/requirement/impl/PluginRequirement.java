package de.jexcellence.jexplatform.requirement.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.jexplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Delegates requirement checking to a third-party plugin via
 * {@link de.jexcellence.jexplatform.requirement.plugin.PluginIntegrationBridge}.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class PluginRequirement extends AbstractRequirement {

    @JsonProperty("pluginId")
    private final String pluginId;

    @JsonProperty("requirementId")
    private final String requirementId;

    @JsonProperty("parameters")
    private final Map<String, Object> parameters;

    /**
     * Creates a plugin requirement.
     *
     * @param pluginId      the target plugin
     * @param requirementId the requirement identifier within that plugin
     * @param parameters    additional parameters
     */
    public PluginRequirement(@JsonProperty("pluginId") @NotNull String pluginId,
                             @JsonProperty("requirementId") @NotNull String requirementId,
                             @JsonProperty("parameters") Map<String, Object> parameters) {
        super("PLUGIN");
        this.pluginId = pluginId;
        this.requirementId = requirementId;
        this.parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
    }

    @Override
    public boolean isMet(@NotNull Player player) {
        // Resolved at runtime via ServiceRegistry → PluginIntegrationBridge
        return false;
    }

    @Override
    public double calculateProgress(@NotNull Player player) {
        return 0.0;
    }

    @Override
    public void consume(@NotNull Player player) {
        // Delegated to plugin bridge
    }

    @Override
    public @NotNull String descriptionKey() {
        return "requirement.plugin." + pluginId;
    }

    /**
     * Returns the target plugin ID.
     *
     * @return the plugin ID
     */
    public @NotNull String getPluginId() {
        return pluginId;
    }

    /**
     * Returns the requirement identifier.
     *
     * @return the requirement ID
     */
    public @NotNull String getRequirementId() {
        return requirementId;
    }

    /**
     * Returns the parameters map.
     *
     * @return the parameters
     */
    public @NotNull Map<String, Object> getParameters() {
        return parameters;
    }
}
