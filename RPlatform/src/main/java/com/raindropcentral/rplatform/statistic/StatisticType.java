package com.raindropcentral.rplatform.statistic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum StatisticType {

    JOIN_DATE("join_date", DataType.TIMESTAMP, 0L, Category.CORE, "First join timestamp"),
    LAST_SEEN("last_seen", DataType.TIMESTAMP, 0L, Category.CORE, "Last seen timestamp"),
    FIRST_JOIN_SERVER("first_join_server", DataType.STRING, "", Category.CORE, "First join server name"),
    LOGIN_COUNT("login_count", DataType.NUMBER, 0.0, Category.CORE, "Total login count"),
    TOTAL_TIME_PLAYED("total_time_played", DataType.NUMBER, 0.0, Category.CORE, "Total playtime in milliseconds"),
    PLAYER_VERSION("player_version", DataType.STRING, "1.0.0", Category.CORE, "Player data version"),

    TOTAL_DEATHS("total_deaths", DataType.NUMBER, 0.0, Category.GAMEPLAY, "Total deaths"),
    TOTAL_KILLS("total_kills", DataType.NUMBER, 0.0, Category.GAMEPLAY, "Total kills"),
    BLOCKS_BROKEN("blocks_broken", DataType.NUMBER, 0.0, Category.GAMEPLAY, "Blocks broken"),
    BLOCKS_PLACED("blocks_placed", DataType.NUMBER, 0.0, Category.GAMEPLAY, "Blocks placed"),
    DISTANCE_WALKED("distance_walked", DataType.NUMBER, 0.0, Category.GAMEPLAY, "Distance walked"),
    DISTANCE_FLOWN("distance_flown", DataType.NUMBER, 0.0, Category.GAMEPLAY, "Distance flown"),
    ITEMS_CRAFTED("items_crafted", DataType.NUMBER, 0.0, Category.GAMEPLAY, "Items crafted"),
    FOOD_EATEN("food_eaten", DataType.NUMBER, 0.0, Category.GAMEPLAY, "Food consumed"),

    MESSAGES_SENT("messages_sent", DataType.NUMBER, 0.0, Category.SOCIAL, "Messages sent"),
    COMMANDS_USED("commands_used", DataType.NUMBER, 0.0, Category.SOCIAL, "Commands executed"),

    TOTAL_MONEY_EARNED("total_money_earned", DataType.NUMBER, 0.0, Category.ECONOMY, "Total money earned"),
    TOTAL_MONEY_SPENT("total_money_spent", DataType.NUMBER, 0.0, Category.ECONOMY, "Total money spent"),
    CURRENT_BALANCE("current_balance", DataType.NUMBER, 0.0, Category.ECONOMY, "Current balance"),
    ITEMS_SOLD("items_sold", DataType.NUMBER, 0.0, Category.ECONOMY, "Items sold"),
    ITEMS_BOUGHT("items_bought", DataType.NUMBER, 0.0, Category.ECONOMY, "Items bought"),

    CURRENT_LEVEL("current_level", DataType.NUMBER, 1.0, Category.PROGRESSION, "Current level"),
    TOTAL_EXPERIENCE("total_experience", DataType.NUMBER, 0.0, Category.PROGRESSION, "Total experience"),
    ACHIEVEMENTS_UNLOCKED("achievements_unlocked", DataType.NUMBER, 0.0, Category.PROGRESSION, "Achievements unlocked"),
    QUESTS_COMPLETED("quests_completed", DataType.NUMBER, 0.0, Category.PROGRESSION, "Quests completed"),
    SKILL_POINTS("skill_points", DataType.NUMBER, 0.0, Category.PROGRESSION, "Available skill points"),
    PRESTIGE_LEVEL("prestige_level", DataType.NUMBER, 0.0, Category.PROGRESSION, "Prestige level"),

    PVP_KILLS("pvp_kills", DataType.NUMBER, 0.0, Category.PVP, "PvP kills"),
    PVP_DEATHS("pvp_deaths", DataType.NUMBER, 0.0, Category.PVP, "PvP deaths"),
    PVP_ASSISTS("pvp_assists", DataType.NUMBER, 0.0, Category.PVP, "PvP assists"),

    STRUCTURES_BUILT("structures_built", DataType.NUMBER, 0.0, Category.BUILDING, "Structures built"),
    CREATIVE_TIME("creative_time", DataType.NUMBER, 0.0, Category.BUILDING, "Creative mode time"),
    PLOTS_OWNED("plots_owned", DataType.NUMBER, 0.0, Category.BUILDING, "Plots owned"),

    TUTORIAL_COMPLETED("tutorial_completed", DataType.BOOLEAN, false, Category.SYSTEM, "Tutorial completed"),
    WELCOME_MESSAGE_SHOWN("welcome_message_shown", DataType.BOOLEAN, false, Category.SYSTEM, "Welcome message shown"),
    SETTINGS_CONFIGURED("settings_configured", DataType.BOOLEAN, false, Category.SYSTEM, "Settings configured"),
    DATA_VERSION("data_version", DataType.STRING, "1.0", Category.SYSTEM, "Data version"),
    LAST_IP_ADDRESS("last_ip_address", DataType.STRING, "", Category.SYSTEM, "Last IP address"),
    CLIENT_VERSION("client_version", DataType.STRING, "", Category.SYSTEM, "Client version"),

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

    private final String key;
    private final DataType dataType;
    private final Object defaultValue;
    private final Category category;
    private final String description;

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

    public @NotNull String getKey() {
        return key;
    }

    public @NotNull DataType getDataType() {
        return dataType;
    }

    public @NotNull Object getDefaultValue() {
        return defaultValue;
    }

    public @NotNull Category getCategory() {
        return category;
    }

    public @NotNull String getDescription() {
        return description;
    }

    public boolean isOfType(final @NotNull DataType dataType) {
        return this.dataType == dataType;
    }

    public boolean isInCategory(final @NotNull Category category) {
        return this.category == category;
    }

    public static @NotNull List<StatisticType> getByDataType(final @NotNull DataType dataType) {
        return Arrays.stream(values())
                .filter(stat -> stat.isOfType(dataType))
                .toList();
    }

    public static @NotNull List<StatisticType> getByCategory(final @NotNull Category category) {
        return Arrays.stream(values())
                .filter(stat -> stat.isInCategory(category))
                .toList();
    }

    public static @Nullable StatisticType getByKey(final @NotNull String key) {
        return Arrays.stream(values())
                .filter(stat -> stat.getKey().equals(key))
                .findFirst()
                .orElse(null);
    }

    public static @NotNull Map<String, Object> getDefaultValuesForCategory(final @NotNull Category category) {
        return getByCategory(category).stream()
                .collect(Collectors.toMap(StatisticType::getKey, StatisticType::getDefaultValue));
    }

    public static @NotNull String getPerkActivationCountKey(final @NotNull String perkIdentifier) {
        return "perk_activation_count_" + perkIdentifier.toLowerCase();
    }

    public static @NotNull String getPerkLastUsedKey(final @NotNull String perkIdentifier) {
        return "perk_last_used_" + perkIdentifier.toLowerCase();
    }

    public static @NotNull String getPerkUsageTimeKey(final @NotNull String perkIdentifier) {
        return "perk_usage_time_" + perkIdentifier.toLowerCase();
    }

    public enum DataType {
        BOOLEAN,
        NUMBER,
        STRING,
        TIMESTAMP
    }

    public enum Category {
        CORE,
        RDQ,
        GAMEPLAY,
        SOCIAL,
        ECONOMY,
        PROGRESSION,
        PVP,
        BUILDING,
        SYSTEM,
        MINIGAMES,
        PERKS
    }
}
