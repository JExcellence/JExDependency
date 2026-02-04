package com.raindropcentral.rplatform.reward;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface PluginRewardProvider {

    @NotNull String getPluginId();

    @NotNull Map<String, RewardType> getRewardTypes();

    default void onRegister() {}

    default void onUnregister() {}

    default void register() {
        RewardRegistry registry = RewardRegistry.getInstance();
        for (RewardType type : getRewardTypes().values()) {
            registry.registerType(type);
        }
        onRegister();
    }

    default void unregister() {
        RewardRegistry registry = RewardRegistry.getInstance();
        for (String typeName : getRewardTypes().keySet()) {
            registry.unregisterType(typeName);
        }
        onUnregister();
    }
}
