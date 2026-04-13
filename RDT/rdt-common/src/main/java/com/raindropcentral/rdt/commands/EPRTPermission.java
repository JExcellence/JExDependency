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

package com.raindropcentral.rdt.commands;

import de.jexcellence.evaluable.section.IPermissionNode;

/**
 * Permission nodes used by the primary RDT player command.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public enum EPRTPermission implements IPermissionNode {
    COMMAND("command", "raindroptowns.command"),
    MAIN("mainCommand", "raindroptowns.command.main"),
    SPAWN("spawnCommand", "raindroptowns.command.spawn"),
    FOB("fobCommand", "raindroptowns.command.fob"),
    BANK("bankCommand", "raindroptowns.command.bank"),
    SERVERBANK("serverBankCommand", "raindroptowns.command.serverbank"),
    TAX("taxCommand", "raindroptowns.command.tax");

    private final String internalName;
    private final String fallbackNode;

    EPRTPermission(final String internalName, final String fallbackNode) {
        this.internalName = internalName;
        this.fallbackNode = fallbackNode;
    }

    /**
     * Returns the internal config key for this permission.
     *
     * @return internal permission key
     */
    @Override
    public String getInternalName() {
        return this.internalName;
    }

    /**
     * Returns the fallback Bukkit permission node.
     *
     * @return fallback permission node
     */
    @Override
    public String getFallbackNode() {
        return this.fallbackNode;
    }
}
