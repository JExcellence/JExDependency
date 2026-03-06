package com.raindropcentral.rds.commands;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@code /rs} tab completion behavior.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class PRSTest {

    @Test
    void tabCompletionSuggestsTaxesAction() {
        final PRS command = new PRS(new PRSSection(new EvaluationEnvironmentBuilder()), null);
        final Player player = this.createPlayer();

        final List<String> suggestions = command.onPlayerTabCompletion(player, "prs", new String[]{"ta"});

        assertEquals(List.of("TAXES"), suggestions);
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
