package de.jexcellence.jexplatform.requirement.plugin;

import de.jexcellence.jexplatform.requirement.RequirementRegistry;
import de.jexcellence.jexplatform.requirement.RequirementType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * SPI for plugins that provide custom requirement types.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public interface PluginRequirementProvider {

    /**
     * Returns the plugin identifier.
     *
     * @return the plugin ID
     */
    @NotNull String pluginId();

    /**
     * Returns the requirement types this provider registers.
     *
     * @return the types
     */
    @NotNull Collection<RequirementType> types();

    /**
     * Registers all types with the given registry.
     *
     * @param registry the requirement registry
     */
    void register(@NotNull RequirementRegistry registry);

    /**
     * Unregisters all types from the given registry.
     *
     * @param registry the requirement registry
     */
    void unregister(@NotNull RequirementRegistry registry);
}
