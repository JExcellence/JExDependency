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

package com.raindropcentral.rdq.command.player.rq.machine;

import de.jexcellence.evaluable.section.IPermissionNode;
import org.jetbrains.annotations.NotNull;

/**
 * Enumeration of machine command permissions.
 *
 * <p>Defines permission nodes required for executing various machine command actions.
 * Each permission corresponds to a specific machine management operation and integrates
 * with the plugin's permission system.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.0
 */
public enum EMachinePermission implements IPermissionNode {
    /**
     * Permission to use the base machine command.
     */
    COMMAND("command", "rdq.admin.machine"),
    
    /**
     * Permission to give machine items to players.
     */
    GIVE("give", "rdq.admin.machine.give"),
    
    /**
     * Permission to list machines.
     */
    LIST("list", "rdq.admin.machine.list"),
    
    /**
     * Permission to remove machines.
     */
    REMOVE("remove", "rdq.admin.machine.remove"),
    
    /**
     * Permission to reload machine configurations.
     */
    RELOAD("reload", "rdq.admin.machine.reload"),
    
    /**
     * Permission to view machine information.
     */
    INFO("info", "rdq.admin.machine.info"),
    
    /**
     * Permission to teleport to machines.
     */
    TELEPORT("teleport", "rdq.admin.machine.teleport");
    
    private final String internalName;
    private final String fallbackNode;
    
    /**
     * Constructs a machine permission with the specified internal name and fallback node.
     *
     * @param internalName the internal permission identifier
     * @param fallbackNode the fallback permission node string
     */
    EMachinePermission(
        final @NotNull String internalName,
        final @NotNull String fallbackNode
    ) {
        this.internalName = internalName;
        this.fallbackNode = fallbackNode;
    }
    
    @Override
    public @NotNull String getInternalName() {
        return this.internalName;
    }
    
    @Override
    public @NotNull String getFallbackNode() {
        return this.fallbackNode;
    }
}
