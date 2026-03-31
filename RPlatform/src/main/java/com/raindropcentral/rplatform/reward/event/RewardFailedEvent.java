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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the RewardFailedEvent API type.
 */
public final class RewardFailedEvent extends RewardEvent {

    private final Throwable cause;
    private final String reason;

    /**
     * Executes RewardFailedEvent.
     */
    public RewardFailedEvent(@NotNull Player player, @NotNull AbstractReward reward, @Nullable Throwable cause) {
        super(player, reward);
        this.cause = cause;
        this.reason = cause != null ? cause.getMessage() : "Unknown error";
    }

    /**
     * Executes RewardFailedEvent.
     */
    public RewardFailedEvent(@NotNull Player player, @NotNull AbstractReward reward, @NotNull String reason) {
        super(player, reward);
        this.cause = null;
        this.reason = reason;
    }

    /**
     * Gets cause.
     */
    public @Nullable Throwable getCause() {
        return cause;
    }

    /**
     * Gets reason.
     */
    public @NotNull String getReason() {
        return reason;
    }
}
