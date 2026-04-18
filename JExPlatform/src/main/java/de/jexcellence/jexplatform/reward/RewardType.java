package de.jexcellence.jexplatform.reward;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Descriptor for a dynamically registered reward type.
 *
 * @param id                  unique identifier
 * @param pluginId            the providing plugin
 * @param implementationClass the concrete class
 * @author JExcellence
 * @since 1.0.0
 */
public record RewardType(
        @NotNull String id,
        @NotNull String pluginId,
        @NotNull Class<? extends AbstractReward> implementationClass
) {
    /** Compact constructor with null checks. */
    public RewardType {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(pluginId, "pluginId cannot be null");
        Objects.requireNonNull(implementationClass, "implementationClass cannot be null");
    }

    /** Creates a core platform reward type. */
    public static @NotNull RewardType core(@NotNull String id,
                                           @NotNull Class<? extends AbstractReward> clazz) {
        return new RewardType(id, "jexplatform", clazz);
    }

    /** Creates a plugin-provided reward type. */
    public static @NotNull RewardType plugin(@NotNull String id, @NotNull String pluginId,
                                             @NotNull Class<? extends AbstractReward> clazz) {
        return new RewardType(id, pluginId, clazz);
    }

    /** Returns the fully qualified type name. */
    public @NotNull String qualifiedName() {
        return pluginId + ":" + id;
    }
}
