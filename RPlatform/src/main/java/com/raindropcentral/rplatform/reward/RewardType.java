package com.raindropcentral.rplatform.reward;

import org.jetbrains.annotations.NotNull;

public record RewardType(
    @NotNull String id,
    @NotNull String pluginId,
    @NotNull Class<? extends AbstractReward> implementationClass
) {
    
    public static RewardType core(
        @NotNull String id,
        @NotNull Class<? extends AbstractReward> clazz
    ) {
        return new RewardType(id, "core", clazz);
    }

    public static RewardType plugin(
        @NotNull String id,
        @NotNull String pluginId,
        @NotNull Class<? extends AbstractReward> clazz
    ) {
        return new RewardType(id, pluginId, clazz);
    }

    public @NotNull String getQualifiedName() {
        return pluginId + ":" + id;
    }
}
