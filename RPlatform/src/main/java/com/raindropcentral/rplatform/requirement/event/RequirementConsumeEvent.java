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

package com.raindropcentral.rplatform.requirement.event;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

/**
 * Called before a requirement is consumed.
 *
 * <p>Can be cancelled to prevent consumption.
 */
public final class RequirementConsumeEvent extends RequirementEvent implements Cancellable {

    private boolean cancelled = false;

    /**
     * Executes RequirementConsumeEvent.
     */
    public RequirementConsumeEvent(@NotNull Player player, @NotNull AbstractRequirement requirement) {
        super(player, requirement);
    }

    /**
     * Executes RequirementConsumeEvent.
     */
    public RequirementConsumeEvent(@NotNull Player player, @NotNull AbstractRequirement requirement, boolean async) {
        super(player, requirement, async);
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
