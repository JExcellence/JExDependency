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

package com.raindropcentral.rdr.commands;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link EPRRPermission} permission-node metadata.
 */
class EPRRPermissionTest {

    @Test
    void exposesConfiguredInternalNamesAndFallbackNodes() {
        assertEquals("command", EPRRPermission.COMMAND.getInternalName());
        assertEquals("raindroprdr.command", EPRRPermission.COMMAND.getFallbackNode());
        assertEquals("commandAdmin", EPRRPermission.ADMIN.getInternalName());
        assertEquals("raindroprdr.command.admin", EPRRPermission.ADMIN.getFallbackNode());
        assertEquals("commandAdminBackup", EPRRPermission.ADMIN_BACKUP.getInternalName());
        assertEquals("raindroprdr.command.admin.backup", EPRRPermission.ADMIN_BACKUP.getFallbackNode());
        assertEquals("commandAdminRollback", EPRRPermission.ADMIN_ROLLBACK.getInternalName());
        assertEquals("raindroprdr.command.admin.rollback", EPRRPermission.ADMIN_ROLLBACK.getFallbackNode());
        assertEquals("commandInfo", EPRRPermission.INFO.getInternalName());
        assertEquals("raindroprdr.command.info", EPRRPermission.INFO.getFallbackNode());
        assertEquals("commandScoreboard", EPRRPermission.SCOREBOARD.getInternalName());
        assertEquals("raindroprdr.command.scoreboard", EPRRPermission.SCOREBOARD.getFallbackNode());
        assertEquals("commandStorage", EPRRPermission.STORAGE.getInternalName());
        assertEquals("raindroprdr.command.storage", EPRRPermission.STORAGE.getFallbackNode());
        assertEquals("commandTrade", EPRRPermission.TRADE.getInternalName());
        assertEquals("raindroprdr.command.trade", EPRRPermission.TRADE.getFallbackNode());
    }

    @Test
    void keepsInternalNamesAndFallbackNodesUnique() {
        final Set<String> internalNames = EnumSet.allOf(EPRRPermission.class).stream()
            .map(EPRRPermission::getInternalName)
            .collect(java.util.stream.Collectors.toSet());
        final Set<String> fallbackNodes = EnumSet.allOf(EPRRPermission.class).stream()
            .map(EPRRPermission::getFallbackNode)
            .collect(java.util.stream.Collectors.toSet());

        assertEquals(EPRRPermission.values().length, internalNames.size());
        assertEquals(EPRRPermission.values().length, fallbackNodes.size());
        assertTrue(fallbackNodes.stream().allMatch(node -> node.startsWith("raindroprdr.command")));
    }
}
