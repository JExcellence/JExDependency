package de.jexcellence.jexplatform.statistic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Unified statistic type registry covering core platform metrics, gameplay
 * tracking, social interactions, economy, progression, PvP, building, system
 * diagnostics, and perk usage.
 *
 * <p>Each constant carries a unique key, data type, default value, category,
 * and human-readable description. Static query methods support filtering by
 * category, data type, and key lookup.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public enum StatisticType {

    // ── Core ──────────────────────────────────────────────────────────────────

    FIRST_JOIN("first_join", DataType.TIMESTAMP, "0", Category.CORE,
            "Timestamp of first server join"),
    LAST_JOIN("last_join", DataType.TIMESTAMP, "0", Category.CORE,
            "Timestamp of most recent join"),
    LAST_QUIT("last_quit", DataType.TIMESTAMP, "0", Category.CORE,
            "Timestamp of most recent quit"),
    TOTAL_PLAYTIME("total_playtime", DataType.NUMBER, "0", Category.CORE,
            "Cumulative playtime in seconds"),
    SESSION_PLAYTIME("session_playtime", DataType.NUMBER, "0", Category.CORE,
            "Current session playtime in seconds"),
    JOIN_COUNT("join_count", DataType.NUMBER, "0", Category.CORE,
            "Total number of server joins"),

    // ── Gameplay ──────────────────────────────────────────────────────────────

    BLOCKS_BROKEN("blocks_broken", DataType.NUMBER, "0", Category.GAMEPLAY,
            "Total blocks broken"),
    BLOCKS_PLACED("blocks_placed", DataType.NUMBER, "0", Category.GAMEPLAY,
            "Total blocks placed"),
    ITEMS_CRAFTED("items_crafted", DataType.NUMBER, "0", Category.GAMEPLAY,
            "Total items crafted"),
    ITEMS_CONSUMED("items_consumed", DataType.NUMBER, "0", Category.GAMEPLAY,
            "Total items consumed"),
    DISTANCE_WALKED("distance_walked", DataType.NUMBER, "0", Category.GAMEPLAY,
            "Total distance walked in blocks"),
    DISTANCE_SPRINTED("distance_sprinted", DataType.NUMBER, "0", Category.GAMEPLAY,
            "Total distance sprinted in blocks"),
    JUMPS("jumps", DataType.NUMBER, "0", Category.GAMEPLAY,
            "Total number of jumps"),
    MOBS_KILLED("mobs_killed", DataType.NUMBER, "0", Category.GAMEPLAY,
            "Total hostile mobs killed"),

    // ── Social ────────────────────────────────────────────────────────────────

    MESSAGES_SENT("messages_sent", DataType.NUMBER, "0", Category.SOCIAL,
            "Total chat messages sent"),
    COMMANDS_EXECUTED("commands_executed", DataType.NUMBER, "0", Category.SOCIAL,
            "Total commands executed"),

    // ── Economy ───────────────────────────────────────────────────────────────

    TOTAL_EARNED("total_earned", DataType.NUMBER, "0", Category.ECONOMY,
            "Cumulative currency earned"),
    TOTAL_SPENT("total_spent", DataType.NUMBER, "0", Category.ECONOMY,
            "Cumulative currency spent"),
    CURRENT_BALANCE("current_balance", DataType.NUMBER, "0", Category.ECONOMY,
            "Current account balance"),
    TRANSACTIONS("transactions", DataType.NUMBER, "0", Category.ECONOMY,
            "Total number of transactions"),
    ITEMS_SOLD("items_sold", DataType.NUMBER, "0", Category.ECONOMY,
            "Total items sold to shops"),

    // ── Progression ───────────────────────────────────────────────────────────

    LEVEL("level", DataType.NUMBER, "1", Category.PROGRESSION,
            "Current player level"),
    EXPERIENCE("experience", DataType.NUMBER, "0", Category.PROGRESSION,
            "Current experience points"),
    TOTAL_EXPERIENCE("total_experience", DataType.NUMBER, "0", Category.PROGRESSION,
            "Cumulative experience earned"),
    QUESTS_COMPLETED("quests_completed", DataType.NUMBER, "0", Category.PROGRESSION,
            "Total quests completed"),
    ACHIEVEMENTS_UNLOCKED("achievements_unlocked", DataType.NUMBER, "0", Category.PROGRESSION,
            "Total achievements unlocked"),
    SKILLS_LEVELED("skills_leveled", DataType.NUMBER, "0", Category.PROGRESSION,
            "Total skill level-ups"),

    // ── PvP ───────────────────────────────────────────────────────────────────

    PLAYERS_KILLED("players_killed", DataType.NUMBER, "0", Category.PVP,
            "Total players killed"),
    DEATHS("deaths", DataType.NUMBER, "0", Category.PVP,
            "Total deaths"),
    KD_RATIO("kd_ratio", DataType.NUMBER, "0", Category.PVP,
            "Kill/death ratio"),

    // ── Building ──────────────────────────────────────────────────────────────

    STRUCTURES_BUILT("structures_built", DataType.NUMBER, "0", Category.BUILDING,
            "Total structures completed"),
    PLOTS_CLAIMED("plots_claimed", DataType.NUMBER, "0", Category.BUILDING,
            "Total plots or claims created"),
    REDSTONE_COMPONENTS("redstone_components", DataType.NUMBER, "0", Category.BUILDING,
            "Total redstone components placed"),

    // ── System ────────────────────────────────────────────────────────────────

    CLIENT_BRAND("client_brand", DataType.STRING, "", Category.SYSTEM,
            "Client brand identifier"),
    CLIENT_VERSION("client_version", DataType.STRING, "", Category.SYSTEM,
            "Client protocol version"),
    LANGUAGE("language", DataType.STRING, "en", Category.SYSTEM,
            "Client language setting"),
    IS_BEDROCK("is_bedrock", DataType.BOOLEAN, "false", Category.SYSTEM,
            "Whether the player connects via Bedrock"),
    LAST_IP("last_ip", DataType.STRING, "", Category.SYSTEM,
            "Last known IP address"),
    LAST_SERVER("last_server", DataType.STRING, "", Category.SYSTEM,
            "Last server name in a network"),

    // ── Perks ─────────────────────────────────────────────────────────────────

    PERK_TOTAL_ACTIVATIONS("perk_total_activations", DataType.NUMBER, "0", Category.PERKS,
            "Total perk activations across all perks"),
    PERK_TOTAL_USAGE_TIME("perk_total_usage_time", DataType.NUMBER, "0", Category.PERKS,
            "Total perk usage time in seconds"),
    PERK_UNIQUE_USED("perk_unique_used", DataType.NUMBER, "0", Category.PERKS,
            "Number of unique perks activated"),
    PERK_FAVORITE("perk_favorite", DataType.STRING, "", Category.PERKS,
            "Most frequently activated perk identifier"),
    PERK_LAST_ACTIVATED("perk_last_activated", DataType.STRING, "", Category.PERKS,
            "Identifier of the last activated perk"),
    PERK_LAST_ACTIVATED_TIME("perk_last_activated_time", DataType.TIMESTAMP, "0", Category.PERKS,
            "Timestamp of the last perk activation"),
    PERK_LONGEST_SESSION("perk_longest_session", DataType.NUMBER, "0", Category.PERKS,
            "Longest single perk session in seconds"),
    PERK_DAILY_ACTIVATIONS("perk_daily_activations", DataType.NUMBER, "0", Category.PERKS,
            "Perk activations today"),
    PERK_WEEKLY_ACTIVATIONS("perk_weekly_activations", DataType.NUMBER, "0", Category.PERKS,
            "Perk activations this week"),
    PERK_CURRENCY_SPENT("perk_currency_spent", DataType.NUMBER, "0", Category.PERKS,
            "Total currency spent on perk activations");

    // ── Fields ────────────────────────────────────────────────────────────────

    private final String key;
    private final DataType dataType;
    private final String defaultValue;
    private final Category category;
    private final String description;

    StatisticType(@NotNull String key, @NotNull DataType dataType,
                  @NotNull String defaultValue, @NotNull Category category,
                  @NotNull String description) {
        this.key = key;
        this.dataType = dataType;
        this.defaultValue = defaultValue;
        this.category = category;
        this.description = description;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /**
     * Returns the unique key used for storage and lookups.
     *
     * @return the statistic key
     */
    public @NotNull String key() {
        return key;
    }

    /**
     * Returns the data type of this statistic.
     *
     * @return the data type
     */
    public @NotNull DataType dataType() {
        return dataType;
    }

    /**
     * Returns the default value as a string.
     *
     * @return the default value
     */
    public @NotNull String defaultValue() {
        return defaultValue;
    }

    /**
     * Returns the category this statistic belongs to.
     *
     * @return the category
     */
    public @NotNull Category category() {
        return category;
    }

    /**
     * Returns a human-readable description.
     *
     * @return the description
     */
    public @NotNull String description() {
        return description;
    }

    // ── Static queries ────────────────────────────────────────────────────────

    /**
     * Finds a statistic type by its key.
     *
     * @param key the key to look up
     * @return the matching type, or empty when not found
     */
    public static @NotNull Optional<StatisticType> getByKey(@Nullable String key) {
        if (key == null || key.isEmpty()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(type -> type.key.equals(key))
                .findFirst();
    }

    /**
     * Returns all statistic types belonging to the given category.
     *
     * @param category the category to filter by
     * @return an unmodifiable set of matching types
     */
    public static @NotNull Set<StatisticType> getByCategory(@NotNull Category category) {
        return Arrays.stream(values())
                .filter(type -> type.category == category)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns all statistic types with the given data type.
     *
     * @param dataType the data type to filter by
     * @return an unmodifiable set of matching types
     */
    public static @NotNull Set<StatisticType> getByDataType(@NotNull DataType dataType) {
        return Arrays.stream(values())
                .filter(type -> type.dataType == dataType)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns default values for all types in the given category.
     *
     * @param category the category
     * @return key-to-default-value map
     */
    public static @NotNull Map<String, String> getDefaultValuesForCategory(
            @NotNull Category category) {
        return Arrays.stream(values())
                .filter(type -> type.category == category)
                .collect(Collectors.toUnmodifiableMap(
                        StatisticType::key, StatisticType::defaultValue));
    }

    /**
     * Returns default values for all {@link Category#CORE} statistics.
     *
     * @return core defaults
     */
    public static @NotNull Map<String, String> getCoreDefaults() {
        return getDefaultValuesForCategory(Category.CORE);
    }

    /**
     * Returns default values for all {@link Category#GAMEPLAY} statistics.
     *
     * @return gameplay defaults
     */
    public static @NotNull Map<String, String> getGameplayDefaults() {
        return getDefaultValuesForCategory(Category.GAMEPLAY);
    }

    /**
     * Returns default values for all {@link Category#PERKS} statistics.
     *
     * @return perk defaults
     */
    public static @NotNull Map<String, String> getPerkDefaults() {
        return getDefaultValuesForCategory(Category.PERKS);
    }

    /**
     * Returns all perk-related statistic types.
     *
     * @return perk statistics
     */
    public static @NotNull Set<StatisticType> getPerkStatistics() {
        return getByCategory(Category.PERKS);
    }

    /**
     * Returns all categories as an immutable set.
     *
     * @return all categories
     */
    public static @NotNull Set<Category> allCategories() {
        return EnumSet.allOf(Category.class);
    }

    // ── Dynamic perk keys ─────────────────────────────────────────────────────

    /**
     * Generates a dynamic key for a specific perk's activation count.
     *
     * @param perkId the perk identifier
     * @return the dynamic key
     */
    public static @NotNull String getPerkActivationCountKey(@NotNull String perkId) {
        return "perk_activation_count_" + perkId;
    }

    /**
     * Generates a dynamic key for a specific perk's last-used timestamp.
     *
     * @param perkId the perk identifier
     * @return the dynamic key
     */
    public static @NotNull String getPerkLastUsedKey(@NotNull String perkId) {
        return "perk_last_used_" + perkId;
    }

    /**
     * Generates a dynamic key for a specific perk's cumulative usage time.
     *
     * @param perkId the perk identifier
     * @return the dynamic key
     */
    public static @NotNull String getPerkUsageTimeKey(@NotNull String perkId) {
        return "perk_usage_time_" + perkId;
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    /**
     * The data type of a statistic value.
     */
    public enum DataType {
        /** Boolean flag ({@code "true"} / {@code "false"}). */
        BOOLEAN,
        /** Numeric value (integer or decimal). */
        NUMBER,
        /** Free-form string. */
        STRING,
        /** Epoch-millis timestamp. */
        TIMESTAMP
    }

    /**
     * Logical grouping for statistic types.
     */
    public enum Category {
        /** Core platform metrics (joins, playtime). */
        CORE,
        /** In-game activity (blocks, items, distance). */
        GAMEPLAY,
        /** Social interactions (chat, commands). */
        SOCIAL,
        /** Economy transactions and balances. */
        ECONOMY,
        /** Levels, experience, quests, achievements. */
        PROGRESSION,
        /** Player-vs-player combat stats. */
        PVP,
        /** Building and claiming. */
        BUILDING,
        /** System and client metadata. */
        SYSTEM,
        /** Perk activation and usage tracking. */
        PERKS
    }
}
