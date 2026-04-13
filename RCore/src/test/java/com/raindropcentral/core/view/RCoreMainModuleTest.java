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

package com.raindropcentral.core.view;

import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RCoreMainModuleTest {

    @Test
    void exposesExpectedRoutingForKnownModules() {
        assertEquals("RDA", RCoreMainModule.RDA.moduleId());
        assertEquals("RDA", RCoreMainModule.RDA.pluginName());
        assertEquals("pra", RCoreMainModule.RDA.commandLabel());
        assertEquals("main", RCoreMainModule.RDA.commandArguments());
        assertEquals("pra main", RCoreMainModule.RDA.commandLine());

        assertEquals("RDQ", RCoreMainModule.RDQ.pluginName());
        assertEquals("prq main", RCoreMainModule.RDQ.commandLine());

        assertEquals("RDR", RCoreMainModule.RDR.pluginName());
        assertEquals("prr storage", RCoreMainModule.RDR.commandLine());

        assertEquals("RDS", RCoreMainModule.RDS.pluginName());
        assertEquals("prs search", RCoreMainModule.RDS.commandLine());

        assertEquals("RDT", RCoreMainModule.RDT.pluginName());
        assertEquals("prt main", RCoreMainModule.RDT.commandLine());
    }

    @Test
    void isUnavailableWhenPluginIsMissing() {
        final Server server = mockServer(null, false);

        assertFalse(RCoreMainModule.RDA.isAvailable(server));
    }

    @Test
    void isUnavailableWhenPluginIsDisabled() {
        final Server server = mockServer("RDA", false);

        assertFalse(RCoreMainModule.RDA.isAvailable(server));
    }

    @Test
    void isAvailableWhenPluginIsEnabled() {
        final Server server = mockServer("RDA", true);

        assertTrue(RCoreMainModule.RDA.isAvailable(server));
    }

    private static Server mockServer(
        final String pluginName,
        final boolean pluginEnabled
    ) {
        final Server server = mock(Server.class);
        final PluginManager pluginManager = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pluginManager);

        if (pluginName == null) {
            when(pluginManager.getPlugin("RDA")).thenReturn(null);
        } else {
            final Plugin plugin = mock(Plugin.class);
            when(plugin.isEnabled()).thenReturn(pluginEnabled);
            when(pluginManager.getPlugin(pluginName)).thenReturn(plugin);
        }
        return server;
    }
}
