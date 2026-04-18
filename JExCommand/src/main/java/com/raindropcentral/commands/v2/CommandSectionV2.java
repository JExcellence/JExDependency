package com.raindropcentral.commands.v2;

import com.raindropcentral.commands.v2.argument.ArgumentSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Declarative node of a JExCommand 2.0 command tree, as loaded from YAML.
 *
 * <p>A section represents either the root command or any subcommand, and carries
 * its own metadata, argument schema, and nested subcommands. The structure is
 * recursive — every subcommand is itself a {@code CommandSectionV2}.
 *
 * <p><b>YAML example.</b>
 * <pre>
 * name: economy
 * description: "Administrative economy operations"
 * aliases: [eco, econ]
 * permission: "economy.command"
 * senders: [console, player]
 * argumentSchema: []
 * subcommands:
 *   - name: give
 *     permission: "economy.command.give"
 *     argumentSchema:
 *       - { name: target,   type: offline_player,   required: true }
 *       - { name: currency, type: currency,         required: true }
 *       - { name: amount,   type: positive_double,  required: true }
 *   - name: take
 *     permission: "economy.command.take"
 *     argumentSchema:
 *       - { name: target,   type: offline_player,   required: true }
 *       - { name: currency, type: currency,         required: true }
 *       - { name: amount,   type: positive_double,  required: true }
 * </pre>
 *
 * <p>Sections are pure value objects — compilation against an
 * {@link com.raindropcentral.commands.v2.argument.ArgumentTypeRegistry} yields
 * a {@link CommandDefinition}, which is the runtime form.
 *
 * @param name            command or subcommand id (lowercase, no spaces)
 * @param description     optional human-readable description
 * @param permission      optional permission node (empty string or {@code null} = none)
 * @param aliases         alternate names; only meaningful on the root section
 * @param senders         allowed sender kinds (any of {@code "player"}, {@code "console"});
 *                        empty list defaults to both
 * @param argumentSchema  positional arguments for this path
 * @param subcommands     nested subcommands (order preserved)
 * @author JExcellence
 * @since 2.0.0
 */
public record CommandSectionV2(
        @NotNull String name,
        @Nullable String description,
        @Nullable String permission,
        @NotNull List<String> aliases,
        @NotNull List<String> senders,
        @NotNull List<ArgumentSpec> argumentSchema,
        @NotNull List<CommandSectionV2> subcommands
) {

    public CommandSectionV2 {
        aliases = List.copyOf(aliases);
        senders = List.copyOf(senders);
        argumentSchema = List.copyOf(argumentSchema);
        subcommands = List.copyOf(subcommands);
    }

    /** Returns {@code true} if this section permits player execution. */
    public boolean allowsPlayer() {
        return senders.isEmpty() || senders.contains("player");
    }

    /** Returns {@code true} if this section permits console execution. */
    public boolean allowsConsole() {
        return senders.isEmpty() || senders.contains("console");
    }

    /** Returns {@code true} if a non-empty permission is declared. */
    public boolean hasPermission() {
        return permission != null && !permission.isBlank();
    }

    /** Finds a direct child subcommand by its (lowercased) name or alias. */
    public @NotNull java.util.Optional<CommandSectionV2> findSubcommand(@NotNull String nameOrAlias) {
        var needle = nameOrAlias.toLowerCase(java.util.Locale.ROOT);
        for (var sub : subcommands) {
            if (sub.name.equalsIgnoreCase(needle)) return java.util.Optional.of(sub);
            for (var alias : sub.aliases) {
                if (alias.equalsIgnoreCase(needle)) return java.util.Optional.of(sub);
            }
        }
        return java.util.Optional.empty();
    }
}
