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
