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

package com.raindropcentral.rds.commands;

import de.jexcellence.evaluable.section.IPermissionNode;

/**
 * Permission nodes used by the primary player command ({@code /prt}).
 *
 * <p>Each enum value provides an {@code internalName} used by the configuration system
 * and a {@code fallbackNode} string that represents the default Bukkit permission
 * when no explicit mapping is provided.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */

public enum EPRSPermission implements IPermissionNode{
    COMMAND("command","raindropshops.command"),
    ADMIN("commandAdmin","raindropshops.command.admin"),
    BAR("commandBar","raindropshops.command.bar"),
    INFO("commandInfo","raindropshops.command.info"),
    GIVE("commandGive","raindropshops.command.give"),
    SCOREBOARD("commandScoreboard","raindropshops.command.scoreboard"),
    SEARCH("commandSearch","raindropshops.command.search"),
    STORE("commandStore","raindropshops.command.store"),
    TAXES("commandTaxes","raindropshops.command.taxes")
    ;

    /** Internal key used by the configuration system to resolve permissions. */
    private final String internalName;

    /** Default Bukkit permission node used when no explicit mapping is provided. */
    private final String fallbackNode;

    /**
     * Construct a permission node descriptor.
     *
     * @param internalName internal configuration key
     * @param fallbackNode default Bukkit permission node
     */
    EPRSPermission(String internalName, String fallbackNode){
        this.internalName = internalName;
        this.fallbackNode = fallbackNode;
    }

    /**
     * Returns the internal name.
     *
     * @return the internal name
     */
    @Override
    public String getInternalName() {
        return this.internalName;
    }

    /**
     * Returns the fallback node.
     *
     * @return the fallback node
     */
    @Override
    public String getFallbackNode() {
        return this.fallbackNode;
    }
}
