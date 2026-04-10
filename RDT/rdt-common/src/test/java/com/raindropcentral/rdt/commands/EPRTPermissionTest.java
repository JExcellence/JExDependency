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

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link EPRTPermission} permission-node metadata.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class EPRTPermissionTest {

    @Test
    void exposesConfiguredInternalNamesAndFallbackNodes() {
        assertEquals("command", EPRTPermission.COMMAND.getInternalName());
        assertEquals("raindroptowns.command", EPRTPermission.COMMAND.getFallbackNode());
        assertEquals("mainCommand", EPRTPermission.MAIN.getInternalName());
        assertEquals("raindroptowns.command.main", EPRTPermission.MAIN.getFallbackNode());
        assertEquals("spawnCommand", EPRTPermission.SPAWN.getInternalName());
        assertEquals("raindroptowns.command.spawn", EPRTPermission.SPAWN.getFallbackNode());
        assertEquals("fobCommand", EPRTPermission.FOB.getInternalName());
        assertEquals("raindroptowns.command.fob", EPRTPermission.FOB.getFallbackNode());
        assertEquals("bankCommand", EPRTPermission.BANK.getInternalName());
        assertEquals("raindroptowns.command.bank", EPRTPermission.BANK.getFallbackNode());
    }

    @Test
    void keepsInternalNamesAndFallbackNodesUnique() {
        final Set<String> internalNames = EnumSet.allOf(EPRTPermission.class).stream()
            .map(EPRTPermission::getInternalName)
            .collect(Collectors.toSet());
        final Set<String> fallbackNodes = EnumSet.allOf(EPRTPermission.class).stream()
            .map(EPRTPermission::getFallbackNode)
            .collect(Collectors.toSet());

        assertEquals(EPRTPermission.values().length, internalNames.size());
        assertEquals(EPRTPermission.values().length, fallbackNodes.size());
        assertTrue(fallbackNodes.stream().allMatch(node -> node.startsWith("raindroptowns.command")));
    }
}
