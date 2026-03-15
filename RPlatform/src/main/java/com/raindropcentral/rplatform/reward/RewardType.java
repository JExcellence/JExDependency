package com.raindropcentral.rplatform.reward;

import org.jetbrains.annotations.NotNull;

/**
 * Represents the RewardType API type.
 */
public record RewardType(
    @NotNull String id,
    @NotNull String pluginId,
    @NotNull Class<? extends AbstractReward> implementationClass
) {
    
    /**
     * Executes core.
     */
    public static RewardType core(
        @NotNull String id,
        @NotNull Class<? extends AbstractReward> clazz
    ) {
        return new RewardType(id, "core", clazz);
    }

    /**
     * Executes plugin.
     */
    public static RewardType plugin(
        @NotNull String id,
        @NotNull String pluginId,
        @NotNull Class<? extends AbstractReward> clazz
    ) {
        return new RewardType(id, pluginId, clazz);
    }

    /**
     * Gets qualifiedName.
     */
    public @NotNull String getQualifiedName() {
        return pluginId + ":" + id;
    }
}
