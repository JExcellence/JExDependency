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

package com.raindropcentral.rdq.requirement;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.lifecycle.RequirementLifecycleHook;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lifecycle hook for RDQ requirement operations.
 *
 * <p>Provides logging and extension points for RDQ-specific logic.
 */
public class RDQRequirementLifecycleHook implements RequirementLifecycleHook {

    private static final Logger LOGGER = Logger.getLogger(RDQRequirementLifecycleHook.class.getName());

    /**
     * Executes beforeCheck.
     */
    @Override
    public boolean beforeCheck(@NotNull Player player, @NotNull AbstractRequirement requirement) {
        LOGGER.log(Level.FINE, "Checking requirement {0} for player {1}", 
            new Object[]{requirement.getTypeId(), player.getName()});
        return true;
    }

    /**
     * Executes afterCheck.
     */
    @Override
    public void afterCheck(@NotNull Player player, @NotNull AbstractRequirement requirement, boolean met, double progress) {
        if (met) {
            LOGGER.log(Level.FINE, "Requirement {0} met for {1}", 
                new Object[]{requirement.getTypeId(), player.getName()});
        }
    }

    /**
     * Executes beforeConsume.
     */
    @Override
    public boolean beforeConsume(@NotNull Player player, @NotNull AbstractRequirement requirement) {
        LOGGER.log(Level.FINE, "Consuming requirement {0} for {1}", 
            new Object[]{requirement.getTypeId(), player.getName()});
        return true;
    }

    /**
     * Executes afterConsume.
     */
    @Override
    public void afterConsume(@NotNull Player player, @NotNull AbstractRequirement requirement) {
        LOGGER.log(Level.FINE, "Consumed requirement {0} for {1}", 
            new Object[]{requirement.getTypeId(), player.getName()});
    }

    /**
     * Executes onError.
     */
    @Override
    public void onError(@NotNull Player player, @NotNull AbstractRequirement requirement, @NotNull Throwable error) {
        LOGGER.log(Level.SEVERE, "Error processing requirement " + requirement.getTypeId() + 
                    " for player " + player.getName(), error);
    }
}
