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

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests p r r behavior.
 */
class PRRTest {

    @Test
    void tabCompletionSuggestsStorageAction() {
        final PRR command = new PRR(new PRRSection(new EvaluationEnvironmentBuilder()), null);
        final Player player = this.createPlayer();

        final List<String> suggestions = command.onPlayerTabCompletion(player, "prr", new String[]{"st"});

        assertEquals(List.of("storage"), suggestions);
    }

    @Test
    void tabCompletionSuggestsAdminAction() {
        final PRR command = new PRR(new PRRSection(new EvaluationEnvironmentBuilder()), null);
        final Player player = this.createPlayer();

        final List<String> suggestions = command.onPlayerTabCompletion(player, "prr", new String[]{"ad"});

        assertEquals(List.of("admin"), suggestions);
    }

    @Test
    void tabCompletionSuggestsTaxesAction() {
        final PRR command = new PRR(new PRRSection(new EvaluationEnvironmentBuilder()), null);
        final Player player = this.createPlayer();

        final List<String> suggestions = command.onPlayerTabCompletion(player, "prr", new String[]{"ta"});

        assertEquals(List.of("taxes"), suggestions);
    }

    @Test
    void tabCompletionSuggestsTradeAction() {
        final PRR command = new PRR(new PRRSection(new EvaluationEnvironmentBuilder()), null);
        final Player player = this.createPlayer();

        final List<String> suggestions = command.onPlayerTabCompletion(player, "prr", new String[]{"tr"});

        assertEquals(List.of("trade"), suggestions);
    }

    @Test
    void tabCompletionReturnsEmptyListBeyondFirstArgument() {
        final PRR command = new PRR(new PRRSection(new EvaluationEnvironmentBuilder()), null);
        final Player player = this.createPlayer();

        final List<String> suggestions = command.onPlayerTabCompletion(player, "prr", new String[]{"storage", "extra"});

        assertEquals(List.of(), suggestions);
    }

    @Test
    void detectsNumericHotkeyArguments() {
        assertTrue(PRR.isNumericArgument("12"));
        assertFalse(PRR.isNumericArgument("storage"));
    }

    @Test
    void parsesNumericHotkeyArguments() {
        assertEquals(7, PRR.parseHotkeyArgument("7"));
        assertNull(PRR.parseHotkeyArgument("storage"));
    }

    private Player createPlayer() {
        final UUID uniqueId = UUID.randomUUID();
        return (Player) Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[]{Player.class},
            (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> uniqueId.hashCode();
                        case "toString" -> "PlayerProxy[" + uniqueId + "]";
                        default -> null;
                    };
                }

                return switch (method.getName()) {
                    case "hasPermission" -> true;
                    case "getUniqueId" -> uniqueId;
                    case "getName" -> "TabTester";
                    default -> this.defaultValue(method.getReturnType());
                };
            }
        );
    }

    private Object defaultValue(final Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0.0F;
        }
        if (returnType == double.class) {
            return 0.0D;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }
}
