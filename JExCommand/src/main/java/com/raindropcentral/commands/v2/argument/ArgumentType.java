package com.raindropcentral.commands.v2.argument;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Typed argument parser for JExCommand 2.0.
 *
 * <p>Each {@code ArgumentType<T>} converts a raw string token into a typed value.
 * Types are identified by a short id (e.g. {@code "online_player"}) used in the
 * YAML {@code argumentSchema[].type} field.
 *
 * <p>Parsers should never throw — unparseable input must return
 * {@link ParseResult#err(String, Map)} with an i18n key and optional placeholders.
 * The dispatcher feeds that key into the plugin's {@code CommandMessages} SPI.
 *
 * @param <T> the produced Java type
 * @author JExcellence
 * @since 2.0.0
 */
public interface ArgumentType<T> {

    /** Short id used in YAML ({@code string}, {@code positive_double}, etc.). */
    @NotNull String id();

    /** The Java type produced by a successful parse. */
    @NotNull Class<T> javaType();

    /**
     * Parses a raw string token in the context of the invoking sender.
     *
     * @param sender the command sender (for player-relative lookups)
     * @param raw    the raw token; never {@code null} or empty
     * @return the parse result — success carries the value, failure carries an i18n key
     */
    @NotNull ParseResult<T> parse(@NotNull CommandSender sender, @NotNull String raw);

    /**
     * Tab-completion suggestions for a partial token.
     *
     * @param sender  the requesting sender
     * @param partial the partial token (may be empty)
     * @return ordered list of suggestions
     */
    default @NotNull List<String> complete(@NotNull CommandSender sender, @NotNull String partial) {
        return List.of();
    }

    // ── Results ──────────────────────────────────────────────────────────────

    /**
     * Outcome of a parse attempt. Use {@link #ok(Object)} for success and
     * {@link #err(String)} / {@link #err(String, Map)} for typed failures.
     *
     * @param <T> the produced value type
     */
    final class ParseResult<T> {

        private final T value;
        private final String errorKey;
        private final Map<String, String> placeholders;

        private ParseResult(@Nullable T value, @Nullable String errorKey,
                            @Nullable Map<String, String> placeholders) {
            this.value = value;
            this.errorKey = errorKey;
            this.placeholders = placeholders == null ? Map.of() : Map.copyOf(placeholders);
        }

        /** Successful parse carrying the typed value. */
        public static <T> @NotNull ParseResult<T> ok(@NotNull T value) {
            return new ParseResult<>(value, null, null);
        }

        /** Failed parse with an i18n key and no placeholders. */
        public static <T> @NotNull ParseResult<T> err(@NotNull String key) {
            return new ParseResult<>(null, key, null);
        }

        /** Failed parse with an i18n key and placeholder map. */
        public static <T> @NotNull ParseResult<T> err(@NotNull String key,
                                                      @NotNull Map<String, String> placeholders) {
            return new ParseResult<>(null, key, placeholders);
        }

        public boolean isOk()                                  { return errorKey == null; }
        public boolean isErr()                                 { return errorKey != null; }
        public @Nullable T value()                             { return value; }
        public @Nullable String errorKey()                     { return errorKey; }
        public @NotNull Map<String, String> placeholders()     { return placeholders; }
    }

    // ── Custom factory ───────────────────────────────────────────────────────

    /**
     * Builds a custom argument type without subclassing. Useful for plugin-specific
     * domain types (e.g. a currency identifier resolved via a service).
     *
     * @param id          the type id used in YAML
     * @param javaType    the produced Java type
     * @param parser      parse function: {@code (sender, raw) -> ParseResult<T>}
     * @param completer   completion function: {@code (sender, partial) -> List<String>}
     * @return a new argument type
     */
    static <T> @NotNull ArgumentType<T> custom(
            @NotNull String id,
            @NotNull Class<T> javaType,
            @NotNull BiFunction<CommandSender, String, ParseResult<T>> parser,
            @NotNull BiFunction<CommandSender, String, List<String>> completer) {
        return new ArgumentType<>() {
            @Override public @NotNull String id() { return id; }
            @Override public @NotNull Class<T> javaType() { return javaType; }
            @Override public @NotNull ParseResult<T> parse(@NotNull CommandSender sender, @NotNull String raw) {
                return parser.apply(sender, raw);
            }
            @Override public @NotNull List<String> complete(@NotNull CommandSender sender, @NotNull String partial) {
                return completer.apply(sender, partial);
            }
        };
    }

    /**
     * Convenience {@link #custom} variant where completions don't depend on the sender.
     */
    static <T> @NotNull ArgumentType<T> custom(
            @NotNull String id,
            @NotNull Class<T> javaType,
            @NotNull BiFunction<CommandSender, String, ParseResult<T>> parser,
            @NotNull Function<String, List<String>> completer) {
        return custom(id, javaType, parser, (sender, partial) -> completer.apply(partial));
    }
}
