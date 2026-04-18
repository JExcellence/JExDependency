package com.raindropcentral.commands.v2;

import com.raindropcentral.commands.v2.argument.ArgumentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compiled, runtime form of a {@link CommandSectionV2}.
 *
 * <p>The loader resolves every {@link com.raindropcentral.commands.v2.argument.ArgumentSpec}
 * into a concrete {@link ArgumentType} and pairs each node with its dot-separated path
 * (e.g. {@code "economy.give"}). Definitions are immutable once built.
 *
 * <p>A definition is a tree — {@link #subcommands()} holds the children in insertion
 * order and {@link #findSubcommand(String)} provides a name/alias lookup that matches
 * the loader's lowercasing convention.
 *
 * @author JExcellence
 * @since 2.0.0
 */
public final class CommandDefinition {

    private final String name;
    private final String path;
    private final @Nullable String description;
    private final @Nullable String permission;
    private final List<String> aliases;
    private final List<String> senders;
    private final List<ResolvedArgument> arguments;
    private final List<CommandDefinition> subcommands;
    private final Map<String, CommandDefinition> byName;

    public CommandDefinition(@NotNull String name,
                             @NotNull String path,
                             @Nullable String description,
                             @Nullable String permission,
                             @NotNull List<String> aliases,
                             @NotNull List<String> senders,
                             @NotNull List<ResolvedArgument> arguments,
                             @NotNull List<CommandDefinition> subcommands) {
        this.name = name;
        this.path = path;
        this.description = description;
        this.permission = permission;
        this.aliases = List.copyOf(aliases);
        this.senders = List.copyOf(senders);
        this.arguments = List.copyOf(arguments);
        this.subcommands = List.copyOf(subcommands);
        var idx = new ConcurrentHashMap<String, CommandDefinition>();
        for (var sub : this.subcommands) {
            idx.put(sub.name.toLowerCase(Locale.ROOT), sub);
            for (var alias : sub.aliases) {
                idx.put(alias.toLowerCase(Locale.ROOT), sub);
            }
        }
        this.byName = Map.copyOf(idx);
    }

    /** Local name (without parent path). */
    public @NotNull String name() {
        return name;
    }

    /** Dot-separated path used by handler lookups, e.g. {@code "economy.give"}. */
    public @NotNull String path() {
        return path;
    }

    /** Optional human-readable description for this node, or {@code null}. */
    public @Nullable String description() {
        return description;
    }

    /** Permission node required to invoke this branch, or {@code null}/blank if none. */
    public @Nullable String permission() {
        return permission;
    }

    /** Alternate names for this node (meaningful primarily on the root). */
    public @NotNull List<String> aliases() {
        return aliases;
    }

    /** Allowed sender kinds: any of {@code "player"}, {@code "console"}; empty = both. */
    public @NotNull List<String> senders() {
        return senders;
    }

    /** Positional arguments declared for this node, in schema order. */
    public @NotNull List<ResolvedArgument> arguments() {
        return arguments;
    }

    /** Direct children of this node, in YAML declaration order. */
    public @NotNull List<CommandDefinition> subcommands() {
        return subcommands;
    }

    /** Returns {@code true} if a non-blank permission is declared on this node. */
    public boolean hasPermission() {
        return permission != null && !permission.isBlank();
    }

    /** Returns {@code true} if this node permits player execution. */
    public boolean allowsPlayer() {
        return senders.isEmpty() || senders.contains("player");
    }

    /** Returns {@code true} if this node permits console execution. */
    public boolean allowsConsole() {
        return senders.isEmpty() || senders.contains("console");
    }

    /** Look up a direct child by name or alias (case insensitive). */
    public @NotNull Optional<CommandDefinition> findSubcommand(@NotNull String nameOrAlias) {
        return Optional.ofNullable(byName.get(nameOrAlias.toLowerCase(Locale.ROOT)));
    }

    /**
     * Builds a single-line usage string like {@code "/economy give <target> <currency> <amount>"}.
     *
     * @param rootLabel the root alias to prefix with a slash (e.g. {@code "economy"})
     */
    public @NotNull String usage(@NotNull String rootLabel) {
        var segments = new ArrayList<String>();
        segments.add(rootLabel);
        // path minus the root: economy.give → [give]
        var parts = path.split("\\.");
        for (int i = 1; i < parts.length; i++) segments.add(parts[i]);
        var line = new StringBuilder("/").append(String.join(" ", segments));
        for (var arg : arguments) {
            line.append(' ');
            line.append(arg.required() ? '<' : '[');
            line.append(arg.name());
            line.append(arg.required() ? '>' : ']');
        }
        return line.toString();
    }

    /**
     * A single resolved positional argument — the compiled counterpart of
     * {@link com.raindropcentral.commands.v2.argument.ArgumentSpec}.
     */
    public static final class ResolvedArgument {
        private final String name;
        private final ArgumentType<?> type;
        private final boolean required;
        private final @Nullable String defaultValue;
        private final @Nullable String description;

        /**
         * Creates a resolved argument.
         *
         * @param name         placeholder id referenced from the handler
         * @param type         concrete argument type (already resolved via the registry)
         * @param required     whether the argument must be present
         * @param defaultValue raw default when not required and no token given
         * @param description  optional hint shown in help output
         */
        public ResolvedArgument(@NotNull String name,
                                @NotNull ArgumentType<?> type,
                                boolean required,
                                @Nullable String defaultValue,
                                @Nullable String description) {
            this.name = name;
            this.type = type;
            this.required = required;
            this.defaultValue = defaultValue;
            this.description = description;
        }

        /** Argument name as declared in the YAML schema. */
        public @NotNull String name() { return name; }

        /** Concrete argument type used to parse and complete this slot. */
        public @NotNull ArgumentType<?> type() { return type; }

        /** {@code true} when the argument must be present on the command line. */
        public boolean required() { return required; }

        /** Raw default value applied when the argument is optional and missing. */
        public @Nullable String defaultValue() { return defaultValue; }

        /** Optional human-readable description shown in help output. */
        public @Nullable String description() { return description; }
    }
}
