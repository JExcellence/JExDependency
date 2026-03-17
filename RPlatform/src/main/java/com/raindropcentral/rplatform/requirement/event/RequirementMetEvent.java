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
import org.jetbrains.annotations.NotNull;

/**
 * Called when a requirement is met.
 */
public final class RequirementMetEvent extends RequirementEvent {

    private final double progress;

    /**
     * Executes RequirementMetEvent.
     */
    public RequirementMetEvent(@NotNull Player player, @NotNull AbstractRequirement requirement, double progress) {
        super(player, requirement);
        this.progress = progress;
    }

    /**
     * Executes RequirementMetEvent.
     */
    public RequirementMetEvent(@NotNull Player player, @NotNull AbstractRequirement requirement, double progress, boolean async) {
        super(player, requirement, async);
        this.progress = progress;
    }

    /**
     * Gets progress.
     */
    public double getProgress() {
        return progress;
    }
}
