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
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link HuskTownProtectionBridge#getPlayerTownLevel(org.bukkit.entity.Player)}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class HuskTownProtectionBridgeTest {

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
        when(server.getLogger()).thenReturn(Logger.getLogger(HuskTownProtectionBridgeTest.class.getName()));

        bukkitServerField.set(null, server);
    }

    @Test
    void getPlayerTownLevelUsesTownLevel() {
        final Player player = mock(Player.class);
        final TestHuskTownsPlugin plugin = mock(TestHuskTownsPlugin.class);
        final FakeHuskTown town = new FakeHuskTown(6);
        final FakeHuskTownsApi api = new FakeHuskTownsApi(new FakeOnlineUser(), town);
        final HuskTownProtectionBridge bridge = new HuskTownProtectionBridge();
        when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getAPI()).thenReturn(api);
        when(this.pluginManager.getPlugin("HuskTowns")).thenReturn(plugin);

        assertEquals(6.0D, bridge.getPlayerTownLevel(player));
    }

    private static final class FakeHuskTownsApi {

        private final FakeOnlineUser onlineUser;
        private final FakeHuskTown town;

        private FakeHuskTownsApi(final @NotNull FakeOnlineUser onlineUser, final @NotNull FakeHuskTown town) {
            this.onlineUser = onlineUser;
            this.town = town;
        }

        public FakeOnlineUser getOnlineUser(final Player player) {
            return this.onlineUser;
        }

        public Optional<FakeMember> getUserTown(final FakeOnlineUser user) {
            return Optional.of(new FakeMember(this.town));
        }
    }

    private static final class FakeOnlineUser {
    }

    private static final class FakeMember {

        private final FakeHuskTown town;

        private FakeMember(final @NotNull FakeHuskTown town) {
            this.town = town;
        }

        public FakeHuskTown town() {
            return this.town;
        }
    }

    private static final class FakeHuskTown {

        private final int level;

        private FakeHuskTown(final int level) {
            this.level = level;
        }

        public int getLevel() {
            return this.level;
        }
    }
}
