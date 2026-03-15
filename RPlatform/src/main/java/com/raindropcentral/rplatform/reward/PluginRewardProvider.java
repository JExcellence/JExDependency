package com.raindropcentral.rplatform.reward;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Represents the PluginRewardProvider API type.
 */
public interface PluginRewardProvider {

    /**
     * Gets pluginId.
     */
    @NotNull String getPluginId();

    /**
     * Gets rewardTypes.
     */
    @NotNull Map<String, RewardType> getRewardTypes();

    /**
     * Executes onRegister.
     */
    default void onRegister() {}

    /**
     * Executes onUnregister.
     */
    default void onUnregister() {}

    /**
     * Executes register.
     */
    default void register() {
        RewardRegistry registry = RewardRegistry.getInstance();
        for (RewardType type : getRewardTypes().values()) {
            registry.registerType(type);
        }
        onRegister();
    }

    /**
     * Executes unregister.
     */
    default void unregister() {
        RewardRegistry registry = RewardRegistry.getInstance();
        for (String typeName : getRewardTypes().keySet()) {
            registry.unregisterType(typeName);
        }
        onUnregister();
    }
}
