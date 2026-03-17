/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

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
