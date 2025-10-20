package com.raindropcentral.rplatform.statistic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Canonical catalogue of player statistics shared by RPlatform storage, analytics, and downstream gameplay modules.
 *
 * <p>Each enum constant defines the identifier, schema type, default payload, functional category, and a concise
 * description that surfaces in dashboards or developer tooling. The metadata mirrors the columns provisioned within
 * statistics database tables as well as JSON payloads cached by {@code StatisticService} implementations; updating a
 * key or {@link DataType} therefore requires a schema migration and client coordination to avoid orphaned data.</p>
 *
 * <p>Categories align with those exposed by {@link com.raindropcentral.rplatform.type.EStatisticType} so both free and
 * premium plugins evaluate quests, perks, and requirements against a consistent taxonomy. Consumers should extend the
 * enum with new entries when evolving gameplay rather than reusing existing keys, which preserves compatibility with
 * {@link #getDefaultValuesForCategory(Category)} bootstrapping routines and external dashboards.</p>
 *
 * <table border="1" summary="Statistic categories">
 *     <caption>Statistic categories and their usage scope</caption>
 *     <thead>
 *     <tr><th>Category</th><th>Usage</th><th>Example Keys</th></tr>
 *     </thead>
 *     <tbody>
 *     <tr><td>{@link Category#CORE CORE}</td><td>Identity and login metadata persisted across seasons.</td><td>{@link #JOIN_DATE}, {@link #LOGIN_COUNT}</td></tr>
 *     <tr><td>{@link Category#GAMEPLAY GAMEPLAY}</td><td>General survival and sandbox progress counters.</td><td>{@link #BLOCKS_BROKEN}, {@link #TOTAL_KILLS}</td></tr>
 *     <tr><td>{@link Category#ECONOMY ECONOMY}</td><td>Balance tracking used by economy and marketplace modules.</td><td>{@link #CURRENT_BALANCE}, {@link #TOTAL_MONEY_EARNED}</td></tr>
 *     <tr><td>{@link Category#PROGRESSION PROGRESSION}</td><td>Leveling, questing, and achievement metrics.</td><td>{@link #CURRENT_LEVEL}, {@link #QUESTS_COMPLETED}</td></tr>
 *     <tr><td>{@link Category#PERKS PERKS}</td><td>Perk unlock and usage telemetry powering perk services.</td><td>{@link #TOTAL_PERKS_OWNED}, {@link #LAST_PERK_ACTIVATION}</td></tr>
 *     </tbody>
 * </table>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
public enum StatisticType {

    /** Core account metadata keyed by join history and login cadence. */
    JOIN_DATE("join_date", DataType.TIMESTAMP, 0L, Category.CORE, "First join timestamp"),
    LAST_SEEN("last_seen", DataType.TIMESTAMP, 0L, Category.CORE, "Last seen timestamp"),
    FIRST_JOIN_SERVER("first_join_server", DataType.STRING, "", Category.CORE, "First join server name"),
    LOGIN_COUNT("login_count", DataType.NUMBER, 0.0, Category.CORE, "Total login count"),
    TOTAL_TIME_PLAYED("total_time_played", DataType.NUMBER, 0.0, Category.CORE, "Total playtime in milliseconds"),
    PLAYER_VERSION("player_version", DataType.STRING, "1.0.0", Category.CORE, "Player data version"),

    /** Sandbox and survival counters reflecting gameplay behaviour. */
    TOTAL_DEATHS("total_deaths", DataType.NUMBER, 0.0, Category.GAMEPLAY, "Total deaths"),
    TOTAL_KILLS("total_kills", DataType.NUMBER, 0.0, Category.GAMEPLAY, "Total kills"),
    BLOCKS_BROKEN("blocks_broken", DataType.NUMBER, 0.0, Category.GAMEPLAY, "Blocks broken"),
    BLOCKS_PLACED("blocks_placed", DataType.NUMBER, 0.0, Category.GAMEPLAY, "Blocks placed"),
    DISTANCE_WALKED("distance_walked", DataType.NUMBER, 0.0, Category.GAMEPLAY, "Distance walked"),
    DISTANCE_FLOWN("distance_flown", DataType.NUMBER, 0.0, Category.GAMEPLAY, "Distance flown"),
    ITEMS_CRAFTED("items_crafted", DataType.NUMBER, 0.0, Category.GAMEPLAY, "Items crafted"),
    FOOD_EATEN("food_eaten", DataType.NUMBER, 0.0, Category.GAMEPLAY, "Food consumed"),

    /** Social interactions and communication usage metrics. */
    MESSAGES_SENT("messages_sent", DataType.NUMBER, 0.0, Category.SOCIAL, "Messages sent"),
    COMMANDS_USED("commands_used", DataType.NUMBER, 0.0, Category.SOCIAL, "Commands executed"),

    /** Economic balances and trade telemetry. */
    TOTAL_MONEY_EARNED("total_money_earned", DataType.NUMBER, 0.0, Category.ECONOMY, "Total money earned"),
    TOTAL_MONEY_SPENT("total_money_spent", DataType.NUMBER, 0.0, Category.ECONOMY, "Total money spent"),
    CURRENT_BALANCE("current_balance", DataType.NUMBER, 0.0, Category.ECONOMY, "Current balance"),
    ITEMS_SOLD("items_sold", DataType.NUMBER, 0.0, Category.ECONOMY, "Items sold"),
    ITEMS_BOUGHT("items_bought", DataType.NUMBER, 0.0, Category.ECONOMY, "Items bought"),

    /** Character growth and achievement progression. */
    CURRENT_LEVEL("current_level", DataType.NUMBER, 1.0, Category.PROGRESSION, "Current level"),
    TOTAL_EXPERIENCE("total_experience", DataType.NUMBER, 0.0, Category.PROGRESSION, "Total experience"),
    ACHIEVEMENTS_UNLOCKED("achievements_unlocked", DataType.NUMBER, 0.0, Category.PROGRESSION, "Achievements unlocked"),
    QUESTS_COMPLETED("quests_completed", DataType.NUMBER, 0.0, Category.PROGRESSION, "Quests completed"),
    SKILL_POINTS("skill_points", DataType.NUMBER, 0.0, Category.PROGRESSION, "Available skill points"),
    PRESTIGE_LEVEL("prestige_level", DataType.NUMBER, 0.0, Category.PROGRESSION, "Prestige level"),

    /** Competitive combat outcomes. */
    PVP_KILLS("pvp_kills", DataType.NUMBER, 0.0, Category.PVP, "PvP kills"),
    PVP_DEATHS("pvp_deaths", DataType.NUMBER, 0.0, Category.PVP, "PvP deaths"),
    PVP_ASSISTS("pvp_assists", DataType.NUMBER, 0.0, Category.PVP, "PvP assists"),

    /** Building and plot development counters. */
    STRUCTURES_BUILT("structures_built", DataType.NUMBER, 0.0, Category.BUILDING, "Structures built"),
    CREATIVE_TIME("creative_time", DataType.NUMBER, 0.0, Category.BUILDING, "Creative mode time"),
    PLOTS_OWNED("plots_owned", DataType.NUMBER, 0.0, Category.BUILDING, "Plots owned"),

    /** System flags and environmental markers for onboarding flows. */
    TUTORIAL_COMPLETED("tutorial_completed", DataType.BOOLEAN, false, Category.SYSTEM, "Tutorial completed"),
    WELCOME_MESSAGE_SHOWN("welcome_message_shown", DataType.BOOLEAN, false, Category.SYSTEM, "Welcome message shown"),
    SETTINGS_CONFIGURED("settings_configured", DataType.BOOLEAN, false, Category.SYSTEM, "Settings configured"),
    DATA_VERSION("data_version", DataType.STRING, "1.0", Category.SYSTEM, "Data version"),
    LAST_IP_ADDRESS("last_ip_address", DataType.STRING, "", Category.SYSTEM, "Last IP address"),
    CLIENT_VERSION("client_version", DataType.STRING, "", Category.SYSTEM, "Client version"),

    /** Perk unlock telemetry and monetisation data. */
    TOTAL_PERKS_ACTIVATED("total_perks_activated", DataType.NUMBER, 0.0, Category.PERKS, "Total perk activations"),
    TOTAL_PERKS_OWNED("total_perks_owned", DataType.NUMBER, 0.0, Category.PERKS, "Total perks owned"),
    TOTAL_PERKS_PURCHASED("total_perks_purchased", DataType.NUMBER, 0.0, Category.PERKS, "Total perks purchased"),
    ACTIVE_PERKS_COUNT("active_perks_count", DataType.NUMBER, 0.0, Category.PERKS, "Active perks count"),
    PERK_COOLDOWN_VIOLATIONS("perk_cooldown_violations", DataType.NUMBER, 0.0, Category.PERKS, "Cooldown violations"),
    MOST_USED_PERK("most_used_perk", DataType.STRING, "", Category.PERKS, "Most used perk"),
    PERK_USAGE_TIME("perk_usage_time", DataType.NUMBER, 0.0, Category.PERKS, "Perk usage time"),
    LAST_PERK_ACTIVATION("last_perk_activation", DataType.TIMESTAMP, 0L, Category.PERKS, "Last perk activation"),
    PERK_MONEY_SPENT("perk_money_spent", DataType.NUMBER, 0.0, Category.PERKS, "Money spent on perks"),
    FAVORITE_PERK_CATEGORY("favorite_perk_category", DataType.STRING, "", Category.PERKS, "Favorite perk category");

    /** Persistent identifier used by storage engines. */
    private final String key;
    /** Primitive type contract enforced at persistence boundaries. */
    private final DataType dataType;
    /** Default payload seeded when a statistic lacks stored data. */
    private final Object defaultValue;
    /** Functional grouping leveraged by analytics and gameplay filters. */
    private final Category category;
    /** Human-readable summary surfaced to dashboards and tooling. */
    private final String description;

    /**
     * Constructs a statistic definition.
     *
     * @param key          unique identifier persisted in the statistics schema
     * @param dataType     the primitive type enforced by storage adapters
     * @param defaultValue baseline value applied when creating a new player record
     * @param category     functional grouping that powers aggregate queries and filtering
     * @param description  human-readable summary surfaced in dashboards
     */
    StatisticType(
            final @NotNull String key,
            final @NotNull DataType dataType,
            final @NotNull Object defaultValue,
            final @NotNull Category category,
            final @NotNull String description
    ) {
        this.key = key;
        this.dataType = dataType;
        this.defaultValue = defaultValue;
        this.category = category;
        this.description = description;
    }

    /**
     * Obtains the storage key for this statistic.
     *
     * @return lower-case identifier used in database columns and JSON documents
     */
    public @NotNull String getKey() {
        return key;
    }

    /**
     * Resolves the primitive type required for values stored under this key.
     *
     * @return the {@link DataType} expected by persistence and analytics pipelines
     */
    public @NotNull DataType getDataType() {
        return dataType;
    }

    /**
     * Supplies the default value assigned to newly provisioned player entries.
     *
     * @return immutable baseline value suitable for seeding statistics rows
     */
    public @NotNull Object getDefaultValue() {
        return defaultValue;
    }

    /**
     * Retrieves the logical category this statistic belongs to.
     *
     * @return functional grouping used for filtering and dashboard segmentation
     */
    public @NotNull Category getCategory() {
        return category;
    }

    /**
     * Provides the human-readable explanation for the statistic.
     *
     * @return descriptive label designed for dashboards and documentation
     */
    public @NotNull String getDescription() {
        return description;
    }

    /**
     * Verifies whether this statistic enforces the provided data type.
     *
     * @param dataType type to compare against this definition
     * @return {@code true} when the supplied type matches {@link #getDataType()}, otherwise {@code false}
     */
    public boolean isOfType(final @NotNull DataType dataType) {
        return this.dataType == dataType;
    }

    /**
     * Checks if this statistic is part of the provided category.
     *
     * @param category grouping to compare
     * @return {@code true} when {@link #getCategory()} equals the given category
     */
    public boolean isInCategory(final @NotNull Category category) {
        return this.category == category;
    }

    /**
     * Collects statistics constrained to the provided data type.
     *
     * @param dataType primitive type to filter by
     * @return immutable list of statistics whose {@link #getDataType()} equals {@code dataType}
     */
    public static @NotNull List<StatisticType> getByDataType(final @NotNull DataType dataType) {
        return Arrays.stream(values())
                .filter(stat -> stat.isOfType(dataType))
                .toList();
    }

    /**
     * Retrieves statistics belonging to a specific category.
     *
     * @param category category filter applied to {@link #values()}
     * @return immutable list of statistics scoped to {@code category}
     */
    public static @NotNull List<StatisticType> getByCategory(final @NotNull Category category) {
        return Arrays.stream(values())
                .filter(stat -> stat.isInCategory(category))
                .toList();
    }

    /**
     * Resolves a statistic definition by its persistent key.
     *
     * @param key identifier to look up
     * @return matching statistic, or {@code null} when no constant exposes the supplied key
     */
    public static @Nullable StatisticType getByKey(final @NotNull String key) {
        return Arrays.stream(values())
                .filter(stat -> stat.getKey().equals(key))
                .findFirst()
                .orElse(null);
    }

    /**
     * Generates a default-value map limited to the provided category.
     *
     * @param category category whose statistics should populate the defaults
     * @return map keyed by {@link #getKey()} containing {@link #getDefaultValue()} entries
     */
    public static @NotNull Map<String, Object> getDefaultValuesForCategory(final @NotNull Category category) {
        return getByCategory(category).stream()
                .collect(Collectors.toMap(StatisticType::getKey, StatisticType::getDefaultValue));
    }

    /**
     * Produces the storage key used for counting individual perk activations.
     *
     * @param perkIdentifier canonical perk identifier (e.g. config key)
     * @return namespaced statistic key for the perk activation counter
     */
    public static @NotNull String getPerkActivationCountKey(final @NotNull String perkIdentifier) {
        return "perk_activation_count_" + perkIdentifier.toLowerCase();
    }

    /**
     * Produces the storage key recording the last activation timestamp for a perk.
     *
     * @param perkIdentifier canonical perk identifier (e.g. config key)
     * @return namespaced statistic key for the perk "last used" timestamp
     */
    public static @NotNull String getPerkLastUsedKey(final @NotNull String perkIdentifier) {
        return "perk_last_used_" + perkIdentifier.toLowerCase();
    }

    /**
     * Produces the storage key aggregating time spent with a perk active.
     *
     * @param perkIdentifier canonical perk identifier (e.g. config key)
     * @return namespaced statistic key for the perk usage time counter
     */
    public static @NotNull String getPerkUsageTimeKey(final @NotNull String perkIdentifier) {
        return "perk_usage_time_" + perkIdentifier.toLowerCase();
    }

    /**
     * Enumerates the primitive data types enforced by statistics storage.
     */
    public enum DataType {
        /** Boolean flag stored as a binary state. */
        BOOLEAN,
        /** Numeric value backed by a {@code double}. */
        NUMBER,
        /** UTF-8 encoded text payload. */
        STRING,
        /** Epoch timestamp stored as milliseconds. */
        TIMESTAMP
    }

    /**
     * Functional groupings that drive filtering, leaderboards, and reporting slices.
     */
    public enum Category {
        /** Identity, join history, and lifecycle metadata. */
        CORE,
        /** Reserved RDQ specific metrics not yet surfaced platform-wide. */
        RDQ,
        /** General gameplay counters for sandbox progression. */
        GAMEPLAY,
        /** Social engagement and command usage metrics. */
        SOCIAL,
        /** Economy balances and transactional aggregates. */
        ECONOMY,
        /** Player leveling and quest advancement statistics. */
        PROGRESSION,
        /** Competitive combat metrics. */
        PVP,
        /** Building and creative play statistics. */
        BUILDING,
        /** System flags for onboarding, migrations, and environment markers. */
        SYSTEM,
        /** Minigame specific counters. */
        MINIGAMES,
        /** Perk unlock and activity metrics. */
        PERKS
    }
}
