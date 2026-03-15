package com.raindropcentral.rdr.commands;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

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
