package de.jexcellence.quests.command.argument;

import com.raindropcentral.commands.v2.argument.ArgumentType;
import de.jexcellence.quests.machine.MachineRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * JExCommand 2.0 argument type {@code machine-type}. Registry is
 * in-memory so the completer consults it directly — no cache needed.
 */
public final class MachineTypeArgumentType {

    private MachineTypeArgumentType() {
    }

    public static @NotNull ArgumentType<String> of(@NotNull MachineRegistry registry) {
        return ArgumentType.custom(
                "machine-type",
                String.class,
                (sender, raw) -> registry.get(raw)
                        .map(type -> ArgumentType.ParseResult.ok(type.identifier()))
                        .orElseGet(() -> ArgumentType.ParseResult.err(
                                "machine.not-found",
                                Map.of("type", raw))),
                (sender, partial) -> {
                    final String lower = partial.toLowerCase(java.util.Locale.ROOT);
                    return registry.identifiers().stream()
                            .filter(id -> id.toLowerCase(java.util.Locale.ROOT).startsWith(lower))
                            .sorted()
                            .toList();
                }
        );
    }
}
