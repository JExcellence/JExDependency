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

package com.raindropcentral.rplatform.reward.event;

import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the RewardGrantEvent API type.
 */
public final class RewardGrantEvent extends RewardEvent implements Cancellable {

    private boolean cancelled = false;

    /**
     * Executes RewardGrantEvent.
     */
    public RewardGrantEvent(@NotNull Player player, @NotNull AbstractReward reward) {
        super(player, reward);
    }

    /**
     * Executes RewardGrantEvent.
     */
    public RewardGrantEvent(@NotNull Player player, @NotNull AbstractReward reward, boolean async) {
        super(player, reward, async);
    }

    /**
     * Returns whether cancelled.
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets cancelled.
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
