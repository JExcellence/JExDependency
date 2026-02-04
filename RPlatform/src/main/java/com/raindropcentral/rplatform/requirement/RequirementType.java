package com.raindropcentral.rplatform.requirement;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents a requirement type that can be dynamically registered.
 *
 * @param id the unique identifier for this requirement type (e.g., "EVOLUTION_LEVEL")
 * @param pluginId the plugin that provides this requirement type
 * @param implementationClass the class that implements this requirement
 */
public record RequirementType(
    @NotNull String id,
    @NotNull String pluginId,
    @NotNull Class<? extends AbstractRequirement> implementationClass
) {
    public RequirementType {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(pluginId, "pluginId cannot be null");
        Objects.requireNonNull(implementationClass, "implementationClass cannot be null");
    }
    
    /**
     * Creates a core RPlatform requirement type.
     */
    @NotNull
    public static RequirementType core(
        @NotNull String id,
        @NotNull Class<? extends AbstractRequirement> clazz
    ) {
        return new RequirementType(id, "rplatform", clazz);
    }
    
    /**
     * Creates a plugin requirement type.
     */
    @NotNull
    public static RequirementType plugin(
        @NotNull String id,
        @NotNull String pluginId,
        @NotNull Class<? extends AbstractRequirement> clazz
    ) {
        return new RequirementType(id, pluginId, clazz);
    }
    
    /**
     * Gets the full qualified type name (pluginId:id).
     */
    @NotNull
    public String getQualifiedName() {
        return pluginId + ":" + id;
    }
}
