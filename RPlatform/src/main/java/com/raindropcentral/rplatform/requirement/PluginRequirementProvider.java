package com.raindropcentral.rplatform.requirement;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Interface for plugins to register their custom requirement types.
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * public class OneBlockRequirementProvider implements PluginRequirementProvider {
 *     @Override
 *     public String getPluginId() {
 *         return "jexoneblock";
 *     }
 *
 *     @Override
 *     public Map<String, RequirementType> getRequirementTypes() {
 *         return Map.of(
 *             "EVOLUTION_LEVEL", RequirementType.plugin("EVOLUTION_LEVEL", "jexoneblock", EvolutionLevelRequirement.class),
 *             "BLOCKS_BROKEN", RequirementType.plugin("BLOCKS_BROKEN", "jexoneblock", BlocksBrokenRequirement.class)
 *         );
 *     }
 * }
 * }</pre>
 */
public interface PluginRequirementProvider {

    @NotNull
    String getPluginId();

    @NotNull
    Map<String, RequirementType> getRequirementTypes();

    default void onRegister() {}

    default void onUnregister() {}

    default void register() {
        RequirementRegistry registry = RequirementRegistry.getInstance();
        for (RequirementType type : getRequirementTypes().values()) {
            registry.registerType(type);
        }
        onRegister();
    }

    default void unregister() {
        RequirementRegistry registry = RequirementRegistry.getInstance();
        for (String typeName : getRequirementTypes().keySet()) {
            registry.unregisterType(typeName);
        }
        onUnregister();
    }
}
