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

package com.raindropcentral.rda.command.player.ra;

import de.jexcellence.evaluable.section.IPermissionNode;
import org.jetbrains.annotations.NotNull;

enum EPRAPermission implements IPermissionNode {
    MAIN("commandMain", "raindropabilities.command.main"),
    CAST("commandCast", "raindropabilities.command.cast"),
    PARTY("commandParty", "raindropabilities.command.party");

    private final String internalName;
    private final String fallbackNode;

    EPRAPermission(final String internalName, final String fallbackNode) {
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
