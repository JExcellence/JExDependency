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

package com.raindropcentral.rplatform.protection.impl;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link TownyProtectionBridge#getPlayerTownLevel(org.bukkit.entity.Player)}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class TownyProtectionBridgeTest {

    private static Field bukkitServerField;
    private static Server originalServer;

    private PluginManager pluginManager;

    @BeforeAll
    static void captureBukkitServer() throws ReflectiveOperationException {
        bukkitServerField = Bukkit.class.getDeclaredField("server");
        bukkitServerField.setAccessible(true);
        originalServer = (Server) bukkitServerField.get(null);
    }

    @AfterAll
    static void restoreBukkitServer() throws ReflectiveOperationException {
        bukkitServerField.set(null, originalServer);
    }

    @BeforeEach
    void setUpBukkitServer() throws ReflectiveOperationException {
        this.pluginManager = mock(PluginManager.class);

        final Server server = mock(Server.class);
        when(server.getPluginManager()).thenReturn(this.pluginManager);
        when(server.getLogger()).thenReturn(Logger.getLogger(TownyProtectionBridgeTest.class.getName()));

        bukkitServerField.set(null, server);
    }

    @Test
    void getPlayerTownLevelUsesTownLevelNumber() {
        final Player player = mock(Player.class);
        final TestTownyPlugin plugin = mock(TestTownyPlugin.class);
        final FakeTownyTown town = new FakeTownyTown(3);
        final FakeTownyApi api = new FakeTownyApi(town);
        final TownyProtectionBridge bridge = new TownyProtectionBridge();
        when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getTownyAPI()).thenReturn(api);
        when(this.pluginManager.getPlugin("Towny")).thenReturn(plugin);

        assertEquals(3.0D, bridge.getPlayerTownLevel(player));
    }

    private static final class FakeTownyApi {

        private final FakeTownyTown town;

        private FakeTownyApi(final @NotNull FakeTownyTown town) {
            this.town = town;
        }

        public FakeTownyTown getTown(final Player player) {
            return this.town;
        }
    }

    private static final class FakeTownyTown {

        private final int levelNumber;

        private FakeTownyTown(final int levelNumber) {
            this.levelNumber = levelNumber;
        }

        public int getLevelNumber() {
            return this.levelNumber;
        }
    }
}
