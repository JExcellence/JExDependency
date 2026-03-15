package com.raindropcentral.rplatform.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Comprehensive enumeration of statistic types tracked by the platform.
 *
 * <p>This catalogue centralises the keys, primitive data contracts, and descriptive metadata
 * mirrored by database schemas, analytics pipelines, and gameplay services. By codifying each
 * statistic in a single enum the platform guarantees that downstream modules—such as quest
 * requirements or perk unlock conditions—reference a consistent taxonomy with well-defined
 * defaults.</p>
 *
 * <p><strong>Categories</strong>
 * <ul>
 *     <li><strong>Core:</strong> Basic identity information and login history seeded on first join.</li>
 *     <li><strong>Gameplay:</strong> Sandbox counters summarising moment-to-moment player actions.</li>
 *     <li><strong>Social:</strong> Messaging and command usage that measure community engagement.</li>
 *     <li><strong>Economy:</strong> Ledger-style entries powering marketplace and economy modules.</li>
 *     <li><strong>Progression:</strong> Levels, experience, and achievements tied to character growth.</li>
 *     <li><strong>PvP:</strong> Competitive combat statistics informing matchmaking and rewards.</li>
 *     <li><strong>Building:</strong> Creative and construction telemetry surfaced to city builders.</li>
 *     <li><strong>System:</strong> Feature flags and operational telemetry coordinating onboarding.</li>
 *     <li><strong>Perks:</strong> Unlock and usage counters leveraged by perk services.</li>
 * </ul>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public enum EStatisticType {

    // ========== CORE STATISTICS ==========
    /** Timestamp marking when the player first connected to the network. */
    JOIN_DATE("join_date", StatisticDataType.TIMESTAMP, 0L, StatisticCategory.CORE, "The timestamp when the player first joined the server"),
    /** Timestamp of the player's most recent login event. */
    LAST_SEEN("last_seen", StatisticDataType.TIMESTAMP, 0L, StatisticCategory.CORE, "The timestamp when the player was last seen online"),
    /** Server identifier representing the shard or realm of the initial join. */
    FIRST_JOIN_SERVER("first_join_server", StatisticDataType.STRING, "", StatisticCategory.CORE, "The name of the server where the player first joined"),
    /** Running total of how many times the player has authenticated successfully. */
    LOGIN_COUNT("login_count", StatisticDataType.NUMBER, 0.0, StatisticCategory.CORE, "Total number of times the player has logged in"),
    /** Accumulated playtime in milliseconds across all recorded sessions. */
    TOTAL_TIME_PLAYED("total_time_played", StatisticDataType.NUMBER, 0.0, StatisticCategory.CORE, "Total time played in milliseconds"),
    /** Semantic version used for coordinating player data migrations. */
    PLAYER_VERSION("player_version", StatisticDataType.STRING, "1.0.0", StatisticCategory.CORE, "Player data version for migration purposes"),

    // ========== GAMEPLAY STATISTICS ==========
    /** Total death count across all worlds. */
    TOTAL_DEATHS("total_deaths", StatisticDataType.NUMBER, 0.0, StatisticCategory.GAMEPLAY, "Total number of player deaths"),
    /** Combined mob and PvP eliminations recorded for the player. */
    TOTAL_KILLS("total_kills", StatisticDataType.NUMBER, 0.0, StatisticCategory.GAMEPLAY, "Total number of mob/player kills"),
    /** Blocks the player has broken in any dimension. */
    BLOCKS_BROKEN("blocks_broken", StatisticDataType.NUMBER, 0.0, StatisticCategory.GAMEPLAY, "Total number of blocks broken"),
    /** Blocks the player has placed, useful for build quotas. */
    BLOCKS_PLACED("blocks_placed", StatisticDataType.NUMBER, 0.0, StatisticCategory.GAMEPLAY, "Total number of blocks placed"),
    /** Distance walked expressed in block units, aggregated server-wide. */
    DISTANCE_WALKED("distance_walked", StatisticDataType.NUMBER, 0.0, StatisticCategory.GAMEPLAY, "Total distance walked in blocks"),
    /** Cumulative flight distance used for Elytra or creative travel analytics. */
    DISTANCE_FLOWN("distance_flown", StatisticDataType.NUMBER, 0.0, StatisticCategory.GAMEPLAY, "Total distance flown in blocks"),
    /** Quantity of crafting actions completed. */
    ITEMS_CRAFTED("items_crafted", StatisticDataType.NUMBER, 0.0, StatisticCategory.GAMEPLAY, "Total number of items crafted"),
    /** Total nutrition consumed to gauge hunger mechanics. */
    FOOD_EATEN("food_eaten", StatisticDataType.NUMBER, 0.0, StatisticCategory.GAMEPLAY, "Total amount of food consumed"),

    // ========== SOCIAL STATISTICS ==========
    /** Chat messages authored by the player. */
    MESSAGES_SENT("messages_sent", StatisticDataType.NUMBER, 0.0, StatisticCategory.SOCIAL, "Total number of chat messages sent"),
    /** Commands executed, covering administrative and gameplay usage. */
    COMMANDS_USED("commands_used", StatisticDataType.NUMBER, 0.0, StatisticCategory.SOCIAL, "Total number of commands executed"),

    // ========== ECONOMY STATISTICS ==========
    /** Gross revenue acquired from quests, sales, and gameplay. */
    TOTAL_MONEY_EARNED("total_money_earned", StatisticDataType.NUMBER, 0.0, StatisticCategory.ECONOMY, "Total money earned throughout gameplay"),
    /** Aggregate spending used to audit marketplace sinks. */
    TOTAL_MONEY_SPENT("total_money_spent", StatisticDataType.NUMBER, 0.0, StatisticCategory.ECONOMY, "Total money spent on purchases"),
    /** Current balance snapshot for balance-dependent mechanics. */
    CURRENT_BALANCE("current_balance", StatisticDataType.NUMBER, 0.0, StatisticCategory.ECONOMY, "Current money balance"),
    /** Items sold through auction houses or merchants. */
    ITEMS_SOLD("items_sold", StatisticDataType.NUMBER, 0.0, StatisticCategory.ECONOMY, "Total number of items sold"),
    /** Items purchased across all shops and trades. */
    ITEMS_BOUGHT("items_bought", StatisticDataType.NUMBER, 0.0, StatisticCategory.ECONOMY, "Total number of items purchased"),

    // ========== PROGRESSION STATISTICS ==========
    /** Player level powering unlock requirements and perks. */
    CURRENT_LEVEL("current_level", StatisticDataType.NUMBER, 1.0, StatisticCategory.PROGRESSION, "Current player level"),
    /** Total experience accumulated for ranking purposes. */
    TOTAL_EXPERIENCE("total_experience", StatisticDataType.NUMBER, 0.0, StatisticCategory.PROGRESSION, "Total experience points earned"),
    /** Number of advancements or achievements obtained. */
    ACHIEVEMENTS_UNLOCKED("achievements_unlocked", StatisticDataType.NUMBER, 0.0, StatisticCategory.PROGRESSION, "Total number of achievements unlocked"),
    /** Completed quest count supporting questline gating. */
    QUESTS_COMPLETED("quests_completed", StatisticDataType.NUMBER, 0.0, StatisticCategory.PROGRESSION, "Total number of quests completed"),
    /** Unspent skill points available for allocation. */
    SKILL_POINTS("skill_points", StatisticDataType.NUMBER, 0.0, StatisticCategory.PROGRESSION, "Available skill points to spend"),
    /** Prestige tier achieved after rebirth or seasonal reset. */
    PRESTIGE_LEVEL("prestige_level", StatisticDataType.NUMBER, 0.0, StatisticCategory.PROGRESSION, "Current prestige level"),

    // ========== PVP STATISTICS ==========
    /** Player-versus-player eliminations earned in combat arenas. */
    PVP_KILLS("pvp_kills", StatisticDataType.NUMBER, 0.0, StatisticCategory.PVP, "Total player vs player kills"),
    /** Losses sustained during PvP encounters. */
    PVP_DEATHS("pvp_deaths", StatisticDataType.NUMBER, 0.0, StatisticCategory.PVP, "Total deaths in player vs player combat"),
    /** Assist credit awarded for collaborative PvP victories. */
    PVP_ASSISTS("pvp_assists", StatisticDataType.NUMBER, 0.0, StatisticCategory.PVP, "Total PvP kill assists"),

    // ========== BUILDING STATISTICS ==========
    /** Count of major builds completed by the player. */
    STRUCTURES_BUILT("structures_built", StatisticDataType.NUMBER, 0.0, StatisticCategory.BUILDING, "Total number of structures completed"),
    /** Time invested in creative mode sessions. */
    CREATIVE_TIME("creative_time", StatisticDataType.NUMBER, 0.0, StatisticCategory.BUILDING, "Total time spent in creative mode"),
    /** Total owned plot claims tracked by land management systems. */
    PLOTS_OWNED("plots_owned", StatisticDataType.NUMBER, 0.0, StatisticCategory.BUILDING, "Current number of plots owned"),

    // ========== SYSTEM STATISTICS ==========
    /** Flag indicating tutorial completion for onboarding flows. */
    TUTORIAL_COMPLETED("tutorial_completed", StatisticDataType.BOOLEAN, false, StatisticCategory.SYSTEM, "Whether the player has completed the tutorial"),
    /** Tracks whether the welcome message still needs to be displayed. */
    WELCOME_MESSAGE_SHOWN("welcome_message_shown", StatisticDataType.BOOLEAN, false, StatisticCategory.SYSTEM, "Whether the welcome message has been displayed"),
    /** Notes when players finalise their initial configuration. */
    SETTINGS_CONFIGURED("settings_configured", StatisticDataType.BOOLEAN, false, StatisticCategory.SYSTEM, "Whether the player has configured their settings"),
    /** Schema version applied to the player's stored data. */
    DATA_VERSION("data_version", StatisticDataType.STRING, "1.0", StatisticCategory.SYSTEM, "Version of the player data structure"),
    /** Last known IP captured for account security checks. */
    LAST_IP_ADDRESS("last_ip_address", StatisticDataType.STRING, "", StatisticCategory.SYSTEM, "Last known IP address (for security)"),
    /** Minecraft client version reported at login. */
    CLIENT_VERSION("client_version", StatisticDataType.STRING, "", StatisticCategory.SYSTEM, "Minecraft client version used"),

    // ========== PERK STATISTICS ==========
    /** Total number of perk activations, regardless of perk type. */
    TOTAL_PERKS_ACTIVATED("total_perks_activated", StatisticDataType.NUMBER, 0.0, StatisticCategory.PERKS, "Total number of times any perk has been activated"),
    /** Count of perks currently owned by the player. */
    TOTAL_PERKS_OWNED("total_perks_owned", StatisticDataType.NUMBER, 0.0, StatisticCategory.PERKS, "Total number of perks currently owned by the player"),
    /** Lifetime purchases used for monetisation analytics. */
    TOTAL_PERKS_PURCHASED("total_perks_purchased", StatisticDataType.NUMBER, 0.0, StatisticCategory.PERKS, "Total number of perks purchased throughout gameplay"),
    /** Number of perks simultaneously active. */
    ACTIVE_PERKS_COUNT("active_perks_count", StatisticDataType.NUMBER, 0.0, StatisticCategory.PERKS, "Current number of active perks"),
    /** Violations triggered when attempting to activate a perk on cooldown. */
    PERK_COOLDOWN_VIOLATIONS("perk_cooldown_violations", StatisticDataType.NUMBER, 0.0, StatisticCategory.PERKS, "Number of times player tried to activate perk during cooldown"),
    /** Identifier of the perk used most frequently. */
    MOST_USED_PERK("most_used_perk", StatisticDataType.STRING, "", StatisticCategory.PERKS, "Identifier of the most frequently used perk"),
    /** Total time spent while any perk is active, measured in milliseconds. */
    PERK_USAGE_TIME("perk_usage_time", StatisticDataType.NUMBER, 0.0, StatisticCategory.PERKS, "Total time spent with perks active (in milliseconds)"),
    /** Timestamp of the most recent perk activation. */
    LAST_PERK_ACTIVATION("last_perk_activation", StatisticDataType.TIMESTAMP, 0L, StatisticCategory.PERKS, "Timestamp of the last perk activation"),
    /** Currency amount invested into perk purchases and activations. */
    PERK_MONEY_SPENT("perk_money_spent", StatisticDataType.NUMBER, 0.0, StatisticCategory.PERKS, "Total money spent on perk purchases and activations"),
    /** Favourite perk category derived from usage frequency. */
    FAVORITE_PERK_CATEGORY("favorite_perk_category", StatisticDataType.STRING, "", StatisticCategory.PERKS, "Most frequently used perk category"),
    
    ;
    
    /**
     * Unique identifier persisted in storage layers and payload documents.
     */
    private final String key;

    /**
     * Primitive contract enforced for values assigned to this statistic.
     */
    private final StatisticDataType dataType;

    /**
     * Baseline payload written when a statistic has not been populated yet.
     */
    private final Object defaultValue;

    /**
     * Functional grouping used for filtering, analytics, and dashboards.
     */
    private final StatisticCategory category;

    /**
     * Human-readable explanation surfaced in GUIs and documentation.
     */
    private final String description;
    
    /**
     * Constructs a new {@code EStatisticType} with the specified properties.
     *
     * @param key          the unique key string for this statistic type
     * @param dataType     the data type of this statistic
     * @param defaultValue the default value for this statistic
     * @param category     the category this statistic belongs to
     * @param description  a human-readable description
     */
    EStatisticType(
        final @NotNull String key,
        final @NotNull StatisticDataType dataType,
        final @NotNull Object defaultValue,
        final @NotNull StatisticCategory category,
        final @NotNull String description
    ) {
        this.key = key;
        this.dataType = dataType;
        this.defaultValue = defaultValue;
        this.category = category;
        this.description = description;
    }
    
    /**
     * Returns the unique key string associated with this statistic type.
     *
     * @return the key string for this statistic type
     */
    public String getKey() {
        return this.key;
    }

    /**
     * Returns the data type of this statistic.
     *
     * @return the data type
     */
    public StatisticDataType getDataType() {
        return this.dataType;
    }

    /**
     * Returns the default value for this statistic.
     *
     * @return the default value
     */
    public Object getDefaultValue() {
        return this.defaultValue;
    }

    /**
     * Returns the category this statistic belongs to.
     *
     * @return the statistic category
     */
    public StatisticCategory getCategory() {
        return this.category;
    }

    /**
     * Returns the human-readable description of this statistic.
     *
     * @return the description
     */
    public String getDescription() {
        return this.description;
    }
    
    /**
     * Checks if this statistic is of the specified data type.
     *
     * @param dataType the data type to check
     * @return true if this statistic matches the data type
     */
    public boolean isOfType(
        final @NotNull StatisticDataType dataType
    ) {
        return this.dataType == dataType;
    }
    
    /**
     * Checks if this statistic belongs to the specified category.
     *
     * @param category the category to check
     * @return true if this statistic belongs to the category
     */
    public boolean isInCategory(
        final @NotNull StatisticCategory category
    ) {
        return this.category == category;
    }
    
    /**
     * Gets all statistics of a specific data type.
     *
     * @param dataType the data type to filter by
     * @return a list of statistics matching the data type
     */
    public static List<EStatisticType> getByDataType(
        final @NotNull StatisticDataType dataType
    ) {
        return
            Arrays
                .stream(values())
                .filter(stat -> stat.isOfType(dataType))
                .toList();
    }
    
    /**
     * Gets all statistics in a specific category.
     *
     * @param category the category to filter by
     * @return a list of statistics in the category
     */
    public static List<EStatisticType> getByCategory(
        final @NotNull StatisticCategory category
    ) {
        return
            Arrays
                .stream(values())
                .filter(stat -> stat.isInCategory(category))
                .toList();
    }
    
    /**
     * Finds a statistic type by its key.
     *
     * @param key the key to search for
     * @return the statistic type, or null if not found
     */
    public static @Nullable EStatisticType getByKey(
        final @NotNull String key
    ) {
        return
            Arrays
                .stream(values())
                .filter(stat -> stat.getKey().equals(key))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Creates a map of default values for all statistics in a category.
     *
     * @param category the category to get defaults for
     * @return a map of statistic keys to their default values
     */
    public static Map<String, Object> getDefaultValuesForCategory(
        final @NotNull StatisticCategory category
    ) {
        return
            getByCategory(category)
                .stream()
                .collect(Collectors.toMap(
                    EStatisticType::getKey,
                    EStatisticType::getDefaultValue
                ));
    }
    
    
    /**
     * Creates a map of default values for all core statistics.
     *
     * @return a map of core statistic keys to their default values
     */
    public static Map<String, Object> getCoreDefaults() {
        return getDefaultValuesForCategory(StatisticCategory.CORE);
    }

    /**
     * Creates a map of default values for all gameplay statistics.
     *
     * @return a map of gameplay statistic keys to their default values
     */
    public static Map<String, Object> getGameplayDefaults() {
        return getDefaultValuesForCategory(StatisticCategory.GAMEPLAY);
    }

    /**
     * Creates a map of default values for all perk statistics.
     *
     * @return a map of perk statistic keys to their default values
     */
    public static Map<String, Object> getPerkDefaults() {
        return getDefaultValuesForCategory(StatisticCategory.PERKS);
    }

    /**
     * Gets all perk-related statistics.
     *
     * @return a list of all perk statistics
     */
    public static List<EStatisticType> getPerkStatistics() {
        return getByCategory(StatisticCategory.PERKS);
    }

    /**
     * Creates a dynamic perk activation count key for a specific perk.
     *
     * @param perkIdentifier the perk identifier
     * @return the dynamic statistic key for perk activation count
     */
    public static String getPerkActivationCountKey(final @NotNull String perkIdentifier) {
        return "perk_activation_count_" + perkIdentifier.toLowerCase();
    }

    /**
     * Creates a dynamic perk last used key for a specific perk.
     *
     * @param perkIdentifier the perk identifier
     * @return the dynamic statistic key for perk last used timestamp
     */
    public static String getPerkLastUsedKey(final @NotNull String perkIdentifier) {
        return "perk_last_used_" + perkIdentifier.toLowerCase();
    }

    /**
     * Creates a dynamic perk total usage time key for a specific perk.
     *
     * @param perkIdentifier the perk identifier
     * @return the dynamic statistic key for perk total usage time
     */
    public static String getPerkUsageTimeKey(final @NotNull String perkIdentifier) {
        return "perk_usage_time_" + perkIdentifier.toLowerCase();
    }

    /**
     * Enumeration of statistic data types.
     *
     * @since 1.0.0
     * @version 1.0.1
     */
    public enum StatisticDataType {
        /**
         * Boolean value (true/false).
         */
        BOOLEAN,
        /**
         * Numeric value (Double).
         */
        NUMBER,
        /**
         * String value.
         */
        STRING,
        /**
         * Timestamp value (Long).
         */
        TIMESTAMP
    }
    
    /**
     * Enumeration of statistic categories for organization.
     *
     * @since 1.0.0
     * @version 1.0.1
     */
    public enum StatisticCategory {
        /**
         * Core player information and basic statistics.
         */
        CORE,
        /**
         * RDQ player information and basic statistics.
         */
        RDQ,
        /**
         * General gameplay statistics.
         */
        GAMEPLAY,
        /**
         * Social interaction statistics.
         */
        SOCIAL,
        /**
         * Economic and transaction statistics.
         */
        ECONOMY,
        /**
         * Progression, levels, and achievements.
         */
        PROGRESSION,
        /**
         * Player vs Player combat statistics.
         */
        PVP,
        /**
         * Building and creative statistics.
         */
        BUILDING,
        /**
         * System and technical statistics.
         */
        SYSTEM,
        /**
         * Minigame-related statistics.
         */
        MINIGAMES,
        /**
         * Perk activation, usage, and tracking statistics.
         */
        PERKS
    }
}