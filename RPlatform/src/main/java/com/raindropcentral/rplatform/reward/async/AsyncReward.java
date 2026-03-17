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

package com.raindropcentral.rplatform.reward.async;

import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the AsyncReward API type.
 */
public abstract class AsyncReward extends AbstractReward {

    /**
     * Executes grant.
     */
    @Override
    public final @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        return grantAsync(player);
    }

    /**
     * Executes grantAsync.
     */
    public abstract @NotNull CompletableFuture<Boolean> grantAsync(@NotNull Player player);
}
