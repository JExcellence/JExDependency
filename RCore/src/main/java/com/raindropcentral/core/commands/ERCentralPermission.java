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

package com.raindropcentral.core.commands;

import de.jexcellence.evaluable.section.IPermissionNode;
import org.jetbrains.annotations.NotNull;

/**
 * Permission nodes for RaindropCentral integration commands.
 */
public enum ERCentralPermission implements IPermissionNode {

    BOSS_BAR("bossBar", "rcore.command.bossbar"),
    MAIN("main", "rcore.command.main"),
    CONNECT("connect", "rcore.central.connect"),
    DISCONNECT("disconnect", "rcore.central.disconnect"),
    CLAIM_DROPLETS("claimDroplets", "rcore.central.claim.droplets"),
    STORE_UPDATE("storeUpdate", "rcore.central.store.update");

    private final String permissionInternalIdentifier;
    private final String fallbackPermissionNode;

    ERCentralPermission(
            final @NotNull String permissionInternalIdentifier,
            final @NotNull String fallbackPermissionNode
    ) {
        this.permissionInternalIdentifier = permissionInternalIdentifier;
        this.fallbackPermissionNode = fallbackPermissionNode;
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull String getInternalName() {
        return this.permissionInternalIdentifier;
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull String getFallbackNode() {
        return this.fallbackPermissionNode;
    }
}
