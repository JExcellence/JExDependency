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

package com.raindropcentral.rds.listeners;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.configs.ConfigSection;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests town-placement bypass permission behavior for shop placement.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class BlockListenerTest {

    @Test
    void bypassPermissionAllowsPlacementWhenTownRestrictionIsEnabled(
            final @TempDir Path tempDir
    ) throws IOException {
        final ConfigSection config = this.loadConfig(tempDir, true);
        final Player bypassPlayer = this.createPlayerWithPermission(true);
        final BlockListener listener = new BlockListener((RDS) null);

        assertTrue(this.invokeCanPlacePlayerShop(listener, bypassPlayer, config));
    }

    @Test
    void bypassPermissionSupportMatchesPlayerPermissionNode() {
        final Player bypassPlayer = this.createPlayerWithPermission(true);
        final Player regularPlayer = this.createPlayerWithPermission(false);

        assertTrue(BlockListener.hasTownPlacementBypassPermission(bypassPlayer));
        assertFalse(BlockListener.hasTownPlacementBypassPermission(regularPlayer));
    }

    private boolean invokeCanPlacePlayerShop(
            final BlockListener listener,
            final Player player,
            final ConfigSection config
    ) {
        try {
            final Method method = BlockListener.class.getDeclaredMethod("canPlacePlayerShop", Player.class, ConfigSection.class);
            method.setAccessible(true);
            return (boolean) method.invoke(listener, player, config);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to invoke canPlacePlayerShop for test", exception);
        }
    }

    private ConfigSection loadConfig(
            final Path tempDir,
            final boolean onlyPlayerShops
    ) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
                protection:
                  only_player_shops: %s
                """.formatted(onlyPlayerShops));
        return ConfigSection.fromFile(configFile.toFile());
    }

    private Player createPlayerWithPermission(
            final boolean hasBypassPermission
    ) {
        final InvocationHandler handler = (proxy, method, args) -> {
            if ("hasPermission".equals(method.getName())) {
                return hasBypassPermission;
            }
            return this.getDefaultValue(proxy, method, args);
        };
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                handler
        );
    }

    private Object getDefaultValue(
            final Object proxy,
            final Method method,
            final Object[] args
    ) {
        if ("equals".equals(method.getName())) {
            return args != null && args.length == 1 && proxy == args[0];
        }
        if ("hashCode".equals(method.getName())) {
            return System.identityHashCode(proxy);
        }
        if ("toString".equals(method.getName())) {
            return "BlockListenerTestPlayer";
        }

        final Class<?> returnType = method.getReturnType();
        if (returnType == Boolean.TYPE) {
            return false;
        }
        if (returnType == Byte.TYPE) {
            return (byte) 0;
        }
        if (returnType == Short.TYPE) {
            return (short) 0;
        }
        if (returnType == Integer.TYPE) {
            return 0;
        }
        if (returnType == Long.TYPE) {
            return 0L;
        }
        if (returnType == Float.TYPE) {
            return 0F;
        }
        if (returnType == Double.TYPE) {
            return 0D;
        }
        if (returnType == Character.TYPE) {
            return '\0';
        }
        return null;
    }
}
