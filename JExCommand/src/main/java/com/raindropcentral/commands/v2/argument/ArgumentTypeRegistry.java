package com.raindropcentral.commands.v2.argument;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Registry of {@link ArgumentType}s keyed by their YAML id.
 *
 * <p>Callers typically start from {@link #defaults()} (which contains every built-in
 * type) and register plugin-specific types on top:
 *
 * <pre>
 * var registry = ArgumentTypeRegistry.defaults()
 *     .register("currency", ArgumentType.custom("currency", Currency.class,
 *         (sender, raw) -> { ... },
 *         partial -> List.of(...)));
 * </pre>
 *
 * <p>The {@code enum(...)} type is not pre-registered by id because the qualifier
 * is embedded in the id itself ({@code enum(org.bukkit.Material)}). The registry
 * {@link #resolve(String)} method recognises the {@code enum(...)} syntax and builds
 * an argument type on demand.
 *
 * <p><b>Built-in error keys.</b> Parsers in this registry emit the following i18n
 * keys via {@link ArgumentType.ParseResult#err(String, Map)}. Plugins provide
 * translations for each via their {@code CommandMessages} implementation.
 *
 * <table>
 *   <caption>Built-in error keys</caption>
 *   <tr><th>Key</th><th>Placeholders</th></tr>
 *   <tr><td>{@code jexcommand.error.invalid-long}</td><td>{@code value}</td></tr>
 *   <tr><td>{@code jexcommand.error.invalid-double}</td><td>{@code value}</td></tr>
 *   <tr><td>{@code jexcommand.error.not-positive-long}</td><td>{@code value}</td></tr>
 *   <tr><td>{@code jexcommand.error.not-positive-double}</td><td>{@code value}</td></tr>
 *   <tr><td>{@code jexcommand.error.player-not-online}</td><td>{@code value}</td></tr>
 *   <tr><td>{@code jexcommand.error.player-unknown}</td><td>{@code value}</td></tr>
 *   <tr><td>{@code jexcommand.error.invalid-enum}</td><td>{@code value}, {@code options}</td></tr>
 * </table>
 *
 * @author JExcellence
 * @since 2.0.0
 */
public final class ArgumentTypeRegistry {

    private static final Pattern ENUM_SYNTAX = Pattern.compile("^enum\\((.+)\\)$");

    private final Map<String, ArgumentType<?>> byId = new ConcurrentHashMap<>();

    private ArgumentTypeRegistry() {}

    /** Empty registry — callers register every type explicitly. Rare. */
    public static @NotNull ArgumentTypeRegistry empty() {
        return new ArgumentTypeRegistry();
    }

    /** Registry pre-populated with every built-in type. */
    public static @NotNull ArgumentTypeRegistry defaults() {
        var r = new ArgumentTypeRegistry();
        for (var type : builtIns()) r.register(type);
        return r;
    }

    // ── Registration ─────────────────────────────────────────────────────────

    /** Registers a type. Returns this registry for chaining. */
    public @NotNull ArgumentTypeRegistry register(@NotNull ArgumentType<?> type) {
        byId.put(type.id().toLowerCase(Locale.ROOT), type);
        return this;
    }

    /** Convenience wrapper that builds a custom type and registers it. */
    public @NotNull ArgumentTypeRegistry register(@NotNull String id,
                                                   @NotNull ArgumentType<?> type) {
        byId.put(id.toLowerCase(Locale.ROOT), type);
        return this;
    }

    // ── Resolution ───────────────────────────────────────────────────────────

    /**
     * Resolves a type id to an {@link ArgumentType}. Handles the {@code enum(FQN)}
     * syntax by building an on-the-fly type for the referenced enum class.
     *
     * @param id the type id from YAML
     * @return the resolved type, or {@code null} if unknown
     */
    public @Nullable ArgumentType<?> resolve(@NotNull String id) {
        var normalized = id.trim();
        var direct = byId.get(normalized.toLowerCase(Locale.ROOT));
        if (direct != null) return direct;

        var m = ENUM_SYNTAX.matcher(normalized);
        if (m.matches()) {
            try {
                var clazz = Class.forName(m.group(1));
                if (clazz.isEnum()) {
                    @SuppressWarnings({"rawtypes", "unchecked"})
                    var enumType = buildEnumType((Class) clazz);
                    return enumType;
                }
            } catch (ClassNotFoundException ignored) {
                return null;
            }
        }
        return null;
    }

    // ── Built-ins ────────────────────────────────────────────────────────────

    private static List<ArgumentType<?>> builtIns() {
        return List.of(
                stringType(),
                quotedStringType(),
                longType("long", false),
                longType("positive_long", true),
                doubleType("double", false),
                doubleType("positive_double", true),
                onlinePlayerType(),
                offlinePlayerType(),
                uuidType()
        );
    }

    private static ArgumentType<String> stringType() {
        return ArgumentType.custom("string", String.class,
                (s, raw) -> raw.contains(" ")
                        ? ArgumentType.ParseResult.err("jexcommand.error.expected-single-word",
                                Map.of("value", raw))
                        : ArgumentType.ParseResult.ok(raw),
                partial -> List.of());
    }

    private static ArgumentType<String> quotedStringType() {
        // Quoted tokens are reassembled at split-time; by the time we see a single
        // token here it's already the full string. Accept as-is.
        return ArgumentType.custom("quoted_string", String.class,
                (s, raw) -> ArgumentType.ParseResult.ok(raw),
                partial -> List.of());
    }

    private static ArgumentType<Long> longType(String id, boolean positive) {
        return ArgumentType.custom(id, Long.class,
                (s, raw) -> {
                    try {
                        var v = Long.parseLong(raw);
                        if (positive && v < 1) {
                            return ArgumentType.ParseResult.err(
                                    "jexcommand.error.not-positive-long", Map.of("value", raw));
                        }
                        return ArgumentType.ParseResult.ok(v);
                    } catch (NumberFormatException e) {
                        return ArgumentType.ParseResult.err(
                                "jexcommand.error.invalid-long", Map.of("value", raw));
                    }
                },
                partial -> List.of("1", "10", "100", "1000"));
    }

    private static ArgumentType<Double> doubleType(String id, boolean positive) {
        return ArgumentType.custom(id, Double.class,
                (s, raw) -> {
                    try {
                        var v = Double.parseDouble(raw);
                        if (positive && v <= 0) {
                            return ArgumentType.ParseResult.err(
                                    "jexcommand.error.not-positive-double", Map.of("value", raw));
                        }
                        return ArgumentType.ParseResult.ok(v);
                    } catch (NumberFormatException e) {
                        return ArgumentType.ParseResult.err(
                                "jexcommand.error.invalid-double", Map.of("value", raw));
                    }
                },
                partial -> List.of("1", "10", "100", "1000"));
    }

    private static ArgumentType<Player> onlinePlayerType() {
        return ArgumentType.custom("online_player", Player.class,
                (s, raw) -> {
                    var p = Bukkit.getPlayerExact(raw);
                    return p == null
                            ? ArgumentType.ParseResult.<Player>err(
                                    "jexcommand.error.player-not-online", Map.of("value", raw))
                            : ArgumentType.ParseResult.ok(p);
                },
                (sender, partial) -> {
                    var lower = partial.toLowerCase(Locale.ROOT);
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(lower))
                            .toList();
                });
    }

    private static ArgumentType<OfflinePlayer> offlinePlayerType() {
        return ArgumentType.custom("offline_player", OfflinePlayer.class,
                (s, raw) -> {
                    // Accept UUID or exact/known name.
                    try {
                        var uuid = UUID.fromString(raw);
                        return ArgumentType.ParseResult.ok(Bukkit.getOfflinePlayer(uuid));
                    } catch (IllegalArgumentException ignored) {
                        // Not a UUID — fall through to name path.
                    }
                    @SuppressWarnings("deprecation")
                    var op = Bukkit.getOfflinePlayer(raw);
                    if (!op.hasPlayedBefore() && !op.isOnline()) {
                        return ArgumentType.ParseResult.err(
                                "jexcommand.error.player-unknown", Map.of("value", raw));
                    }
                    return ArgumentType.ParseResult.ok(op);
                },
                (sender, partial) -> {
                    var lower = partial.toLowerCase(Locale.ROOT);
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(lower))
                            .toList();
                });
    }

    private static ArgumentType<UUID> uuidType() {
        return ArgumentType.custom("uuid", UUID.class,
                (s, raw) -> {
                    try {
                        return ArgumentType.ParseResult.ok(UUID.fromString(raw));
                    } catch (IllegalArgumentException e) {
                        return ArgumentType.ParseResult.err(
                                "jexcommand.error.invalid-uuid", Map.of("value", raw));
                    }
                },
                partial -> List.of());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <E extends Enum<E>> ArgumentType<E> buildEnumType(Class<E> enumClass) {
        return ArgumentType.custom(
                "enum(" + enumClass.getName() + ")",
                enumClass,
                (s, raw) -> {
                    for (var constant : enumClass.getEnumConstants()) {
                        if (constant.name().equalsIgnoreCase(raw)) {
                            return ArgumentType.ParseResult.ok(constant);
                        }
                    }
                    var options = String.join(", ",
                            Arrays.stream(enumClass.getEnumConstants())
                                    .map(c -> c.name().toLowerCase(Locale.ROOT))
                                    .toArray(String[]::new));
                    return ArgumentType.ParseResult.err(
                            "jexcommand.error.invalid-enum",
                            Map.of("value", raw, "options", options));
                },
                (sender, partial) -> {
                    var lower = partial.toLowerCase(Locale.ROOT);
                    var list = new ArrayList<String>();
                    for (var constant : enumClass.getEnumConstants()) {
                        var name = constant.name().toLowerCase(Locale.ROOT);
                        if (name.startsWith(lower)) list.add(name);
                    }
                    return list;
                });
    }

    // ── Introspection ────────────────────────────────────────────────────────

    /** Snapshot of registered type ids (not thread-safe against concurrent register). */
    public @NotNull List<String> registeredIds() {
        return new ArrayList<>(byId.keySet());
    }
}
