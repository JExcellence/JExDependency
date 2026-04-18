package com.raindropcentral.commands.v2;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Typed argument bundle handed to a {@link CommandHandler} after a successful parse.
 *
 * <p>The context exposes:
 * <ul>
 *   <li>{@link #sender()} — the invoking {@code CommandSender}.</li>
 *   <li>{@link #alias()} — the alias used to invoke the command.</li>
 *   <li>{@link #path()} — the fully qualified command path
 *       (e.g. {@code "economy.give"}).</li>
 *   <li>{@link #args()} — a name-indexed map of parsed typed values.</li>
 * </ul>
 *
 * <p>Use {@link #get(String, Class)} for typed access or
 * {@link #require(String, Class)} when the argument is known to be present.
 *
 * @author JExcellence
 * @since 2.0.0
 */
public final class CommandContext {

    private final CommandSender sender;
    private final String alias;
    private final String path;
    private final Map<String, Object> args;
    private final String[] rawArgs;

    public CommandContext(@NotNull CommandSender sender,
                          @NotNull String alias,
                          @NotNull String path,
                          @NotNull Map<String, Object> args,
                          @NotNull String[] rawArgs) {
        this.sender = sender;
        this.alias = alias;
        this.path = path;
        this.args = Map.copyOf(args);
        this.rawArgs = rawArgs.clone();
    }

    /** The invoking sender. */
    public @NotNull CommandSender sender() {
        return sender;
    }

    /** The alias the user typed (may match the root command name). */
    public @NotNull String alias() {
        return alias;
    }

    /**
     * The dot-separated command path, e.g. {@code "economy"} for the root or
     * {@code "economy.give"} for a subcommand.
     */
    public @NotNull String path() {
        return path;
    }

    /** Raw tokens that came after the matched subcommand chain. */
    public @NotNull String[] rawArgs() {
        return rawArgs.clone();
    }

    /** All parsed arguments, keyed by their YAML {@code name}. */
    public @NotNull Map<String, Object> args() {
        return args;
    }

    /**
     * Typed lookup. Returns {@link Optional#empty()} when the key is absent or the
     * stored value is not assignable to the requested type.
     */
    @SuppressWarnings("unchecked")
    public <T> @NotNull Optional<T> get(@NotNull String name, @NotNull Class<T> type) {
        var value = args.get(name);
        if (value == null) return Optional.empty();
        if (!type.isInstance(value)) return Optional.empty();
        return Optional.of((T) value);
    }

    /**
     * Typed lookup that throws if absent or mis-typed. Use only when the schema
     * guarantees the argument (e.g. {@code required: true}).
     */
    public <T> @NotNull T require(@NotNull String name, @NotNull Class<T> type) {
        return get(name, type).orElseThrow(() -> new IllegalStateException(
                "Argument '" + name + "' of type " + type.getSimpleName()
                        + " not found in context for path '" + path + "'"));
    }

    /**
     * Returns the raw value associated with {@code name} (may be {@code null}). The
     * stored value preserves the producing argument type's Java type.
     */
    public @Nullable Object raw(@NotNull String name) {
        return args.get(name);
    }

    /** Convenience — returns the sender as a {@link Player} if applicable. */
    public @NotNull Optional<Player> asPlayer() {
        return sender instanceof Player p ? Optional.of(p) : Optional.empty();
    }
}
