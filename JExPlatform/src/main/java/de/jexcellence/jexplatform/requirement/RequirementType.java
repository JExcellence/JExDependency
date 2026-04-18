package de.jexcellence.jexplatform.requirement;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Descriptor for a dynamically registered requirement type.
 *
 * @param id                  unique identifier (e.g., {@code "ITEM"})
 * @param pluginId            the providing plugin (e.g., {@code "jexplatform"})
 * @param implementationClass the concrete class implementing this type
 * @author JExcellence
 * @since 1.0.0
 */
public record RequirementType(
        @NotNull String id,
        @NotNull String pluginId,
        @NotNull Class<? extends AbstractRequirement> implementationClass
) {
    /**
     * Compact constructor with null checks.
     */
    public RequirementType {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(pluginId, "pluginId cannot be null");
        Objects.requireNonNull(implementationClass, "implementationClass cannot be null");
    }

    /**
     * Creates a core platform requirement type.
     *
     * @param id    the type identifier
     * @param clazz the implementation class
     * @return a new requirement type
     */
    public static @NotNull RequirementType core(
            @NotNull String id,
            @NotNull Class<? extends AbstractRequirement> clazz) {
        return new RequirementType(id, "jexplatform", clazz);
    }

    /**
     * Creates a plugin-provided requirement type.
     *
     * @param id       the type identifier
     * @param pluginId the plugin identifier
     * @param clazz    the implementation class
     * @return a new requirement type
     */
    public static @NotNull RequirementType plugin(
            @NotNull String id,
            @NotNull String pluginId,
            @NotNull Class<? extends AbstractRequirement> clazz) {
        return new RequirementType(id, pluginId, clazz);
    }

    /**
     * Returns the fully qualified type name ({@code pluginId:id}).
     *
     * @return the qualified name
     */
    public @NotNull String qualifiedName() {
        return pluginId + ":" + id;
    }
}
