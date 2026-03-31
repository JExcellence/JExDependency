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

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link EPRSPermission} permission-node metadata.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class EPRSPermissionTest {

    @Test
    void exposesConfiguredInternalNamesAndFallbackNodes() {
        assertEquals("command", EPRSPermission.COMMAND.getInternalName());
        assertEquals("raindropshops.command", EPRSPermission.COMMAND.getFallbackNode());
        assertEquals("commandAdmin", EPRSPermission.ADMIN.getInternalName());
        assertEquals("raindropshops.command.admin", EPRSPermission.ADMIN.getFallbackNode());
        assertEquals("commandBar", EPRSPermission.BAR.getInternalName());
        assertEquals("raindropshops.command.bar", EPRSPermission.BAR.getFallbackNode());
        assertEquals("commandInfo", EPRSPermission.INFO.getInternalName());
        assertEquals("raindropshops.command.info", EPRSPermission.INFO.getFallbackNode());
        assertEquals("commandGive", EPRSPermission.GIVE.getInternalName());
        assertEquals("raindropshops.command.give", EPRSPermission.GIVE.getFallbackNode());
        assertEquals("commandScoreboard", EPRSPermission.SCOREBOARD.getInternalName());
        assertEquals("raindropshops.command.scoreboard", EPRSPermission.SCOREBOARD.getFallbackNode());
        assertEquals("commandSearch", EPRSPermission.SEARCH.getInternalName());
        assertEquals("raindropshops.command.search", EPRSPermission.SEARCH.getFallbackNode());
        assertEquals("commandStore", EPRSPermission.STORE.getInternalName());
        assertEquals("raindropshops.command.store", EPRSPermission.STORE.getFallbackNode());
        assertEquals("commandTaxes", EPRSPermission.TAXES.getInternalName());
        assertEquals("raindropshops.command.taxes", EPRSPermission.TAXES.getFallbackNode());
    }

    @Test
    void keepsInternalNamesAndFallbackNodesUnique() {
        final Set<String> internalNames = EnumSet.allOf(EPRSPermission.class).stream()
            .map(EPRSPermission::getInternalName)
            .collect(Collectors.toSet());
        final Set<String> fallbackNodes = EnumSet.allOf(EPRSPermission.class).stream()
            .map(EPRSPermission::getFallbackNode)
            .collect(Collectors.toSet());

        assertEquals(EPRSPermission.values().length, internalNames.size());
        assertEquals(EPRSPermission.values().length, fallbackNodes.size());
        assertTrue(fallbackNodes.stream().allMatch(node -> node.startsWith("raindropshops.command")));
    }
}
