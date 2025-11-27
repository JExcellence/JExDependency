package com.raindropcentral.rdq.requirement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rcore.utility.RStatisticFactory;
import com.raindropcentral.rdq.RDQImpl;
import com.raindropcentral.rplatform.enumeration.EStatisticType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A requirement based on RCoreImpl statistics system.
 * <p>
 * This requirement can handle various statistic-based conditions such as:
 * - Kill requirements (kill X zombies)
 * - Collection requirements (mine X diamonds)
 * - Achievement requirements (reach level X)
 * - Time-based requirements (play for X hours)
 * </p>
 * <p>
 * Supports two modes:
 * - ABSOLUTE: Player must have at least X total (e.g., "have killed 100 zombies")
 * - RELATIVE: Player must gain X more from when requirement started (e.g., "kill 5 more zombies")
 * </p>
 *
 * Uses RCoreService async APIs under the hood to fetch stats by (identifier, plugin).
 *
 * @author ItsRainingHP
 * @version 1.0.0
 * @since TBD
 */
public class RStatisticRequirement extends AbstractRequirement {

    private static final Logger LOGGER = Logger.getLogger(RStatisticRequirement.class.getName());

    /**
     * Cached RDQImpl reference to avoid repeated lookups.
     */
    private static volatile @Nullable RDQImpl CACHED_RDQ;

    /**
     * The plugin that owns this statistic.
     */
    @JsonProperty("plugin")
    private final String plugin;

    /**
     * The unique identifier of the statistic to check.
     */
    @JsonProperty("identifier")
    private final String identifier;

    /**
     * The required amount/value for this statistic.
     */
    @JsonProperty("required_amount")
    private final double requiredAmount;

    /**
     * The mode determining how the requirement is evaluated.
     */
    @JsonProperty("mode")
    private final RequirementMode mode;

    /**
     * The starting value for RELATIVE mode requirements.
     * This is set when the requirement is first initialized for a player.
     */
    @JsonProperty("starting_value")
    private Double startingValue;

    /**
     * Optional qualifier for more specific statistic matching.
     * For example, for kill statistics, this could specify the entity type.
     */
    @JsonProperty("qualifier")
    private final String qualifier;

    /**
     * The expected statistic type for validation.
     */
    @JsonProperty("statistic_type")
    private final RStatisticFactory.StatisticType statisticType;

    /**
     * Creates a new RStatisticRequirement.
     *
     * @param plugin         The plugin that owns the statistic
     * @param identifier     The statistic identifier
     * @param requiredAmount The required amount/value
     * @param mode           The requirement mode (ABSOLUTE or RELATIVE)
     * @param qualifier      Optional qualifier for specific matching
     * @param statisticType  The expected statistic type
     */
    @JsonCreator
    public RStatisticRequirement(
            @JsonProperty("plugin") @NotNull final String plugin,
            @JsonProperty("identifier") @NotNull final String identifier,
            @JsonProperty("required_amount") final double requiredAmount,
            @JsonProperty("mode") @NotNull final RequirementMode mode,
            @JsonProperty("qualifier") @Nullable final String qualifier,
            @JsonProperty("statistic_type") @NotNull final RStatisticFactory.StatisticType statisticType,
            @JsonProperty("starting_value") @Nullable final Double startingValue
    ) {
        super(Type.CUSTOM);
        this.plugin = plugin;
        this.identifier = identifier;
        this.requiredAmount = requiredAmount;
        this.mode = mode;
        this.qualifier = qualifier;
        this.statisticType = statisticType;
        this.startingValue = startingValue;
    }

    /**
     * Convenience constructor for ABSOLUTE mode requirements.
     */
    public RStatisticRequirement(
            @NotNull final String plugin,
            @NotNull final String identifier,
            final double requiredAmount,
            @NotNull final RStatisticFactory.StatisticType statisticType
    ) {
        this(plugin, identifier, requiredAmount, RequirementMode.ABSOLUTE, null, statisticType, null);
    }

    /**
     * Convenience constructor for RELATIVE mode requirements.
     */
    public RStatisticRequirement(
            @NotNull final String plugin,
            @NotNull final String identifier,
            final double requiredAmount,
            @NotNull final RStatisticFactory.StatisticType statisticType,
            final double startingValue
    ) {
        this(plugin, identifier, requiredAmount, RequirementMode.RELATIVE, null, statisticType, startingValue);
    }

    @Override
    public boolean isMet(@NotNull final Player player) {
        try {
            final double currentValue = getCurrentStatisticValue(player);
            final double targetValue = getTargetValue(player);

            final boolean met = currentValue >= targetValue;

            LOGGER.log(Level.FINER, String.format(
                    "Checking statistic requirement for player %s: current=%.4f, target=%.4f, met=%s [id=%s, plugin=%s, mode=%s]",
                    player.getName(), currentValue, targetValue, met, identifier, plugin, mode
            ));

            return met;
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to check statistic requirement for player " + player.getName(), exception);
            return false;
        }
    }

    @Override
    public double calculateProgress(@NotNull final Player player) {
        try {
            final double currentValue = getCurrentStatisticValue(player);
            final double targetValue = getTargetValue(player);

            if (targetValue <= 0) {
                return 1.0;
            }

            final double progress;
            if (mode == RequirementMode.ABSOLUTE) {
                progress = currentValue / targetValue;
            } else {
                final double startValue = getStartingValue(player);
                final double gained = currentValue - startValue;
                progress = gained / requiredAmount;
            }

            return Math.max(0.0, Math.min(1.0, progress));
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to calculate progress for statistic requirement", exception);
            return 0.0;
        }
    }

    @Override
    public void consume(@NotNull final Player player) {
        try {
            if (mode == RequirementMode.RELATIVE) {
                final double currentValue = getCurrentStatisticValue(player);
                this.startingValue = currentValue;

                LOGGER.log(Level.FINER, String.format(
                        "Updated starting value for relative statistic requirement to %.4f for player %s",
                        currentValue, player.getName()
                ));
            }
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to consume statistic requirement", exception);
        }
    }

    @Override
    public @NotNull String getDescriptionKey() {
        final StringBuilder keyBuilder = new StringBuilder("requirement.statistic.");

        keyBuilder.append(mode.name().toLowerCase()).append(".");
        keyBuilder.append(statisticType.name().toLowerCase()).append(".");
        keyBuilder.append(identifier.toLowerCase().replaceAll("[^a-z0-9_]", "_"));

        if (qualifier != null && !qualifier.isEmpty()) {
            keyBuilder.append(".").append(qualifier.toLowerCase().replaceAll("[^a-z0-9_]", "_"));
        }

        return keyBuilder.toString();
    }

    /**
     * Gets the current value of the statistic for the given player.
     * Uses RCoreService async API with a short timeout to avoid long main-thread blocking.
     *
     * @param player The player to check
     * @return The current statistic value (0.0 if unavailable)
     */
    private double getCurrentStatisticValue(@NotNull final Player player) {
        try {
            final RDQImpl rdq = getRDQ();
            if (rdq == null) {
                LOGGER.log(Level.INFO, "RDQImpl instance not available; cannot fetch statistics.");
                return 0.0;
            }

            final Object value = rdq.getRCoreService()
                    .getStatisticValueAsync(player.getUniqueId(), this.identifier, this.plugin)
                    .completeOnTimeout(null, 75, TimeUnit.MILLISECONDS)
                    .join();

            return convertToDouble(value);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to get current statistic value", exception);
            return 0.0;
        }
    }

    /**
     * Gets the target value that needs to be reached.
     *
     * @param player The player to check
     * @return The target value
     */
    private double getTargetValue(@NotNull final Player player) {
        if (mode == RequirementMode.ABSOLUTE) {
            return requiredAmount;
        } else {
            final double startValue = getStartingValue(player);
            return startValue + requiredAmount;
        }
    }

    /**
     * Gets the starting value for relative requirements.
     *
     * @param player The player to check
     * @return The starting value
     */
    private double getStartingValue(@NotNull final Player player) {
        if (startingValue != null) {
            return startingValue;
        }

        final double currentValue = getCurrentStatisticValue(player);
        this.startingValue = currentValue;

        LOGGER.log(Level.FINER, String.format(
                "Initialized starting value for relative requirement to %.4f for player %s",
                currentValue, player.getName()
        ));

        return currentValue;
    }

    /**
     * Converts a statistic value to double for calculations.
     *
     * @param value The statistic value
     * @return The value as double
     */
    private double convertToDouble(@Nullable final Object value) {
        if (value == null) {
            return 0.0;
        }

        try {
            switch (value) {
                case Number number -> {
                    return number.doubleValue();
                }
                case Boolean bool -> {
                    return bool ? 1.0 : 0.0;
                }
                case String str -> {
                    try {
                        return Double.parseDouble(str);
                    } catch (NumberFormatException e) {
                        return str.isEmpty() ? 0.0 : 1.0;
                    }
                }
                default -> {
                    // For non-primitive types, treat presence as truthy (1.0)
                    return 1.0;
                }
            }
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to convert statistic value to double: " + value, exception);
            return 0.0;
        }
    }

    /**
     * Initializes this requirement for a player (sets starting value for relative mode).
     *
     * @param player The player to initialize for
     */
    public void initializeFoRDQPlayer(@NotNull final Player player) {
        if (mode == RequirementMode.RELATIVE && startingValue == null) {
            getStartingValue(player);
        }
    }

    /**
     * Attempts to resolve the RDQImpl instance.
     * Tries common plugin ids and reflective accessors to obtain the delegate implementation.
     */
    private @Nullable RDQImpl getRDQ() {
        final RDQImpl cached = CACHED_RDQ;
        if (cached != null) {
            return cached;
        }

        // Try to resolve via the JavaPlugin instance using common names and reflective accessors
        final String[] pluginNames = new String[] { "RaindropQuests", "RDQ", "RDQImpl" };
        for (final String name : pluginNames) {
            try {
                final org.bukkit.plugin.Plugin p = Bukkit.getPluginManager().getPlugin(name);
                if (p == null) {
                    continue;
                }

                // Try common accessor method names to get the delegate implementation
                final String[] accessors = new String[] { "getImplementation", "getImpl", "getDelegate", "getRDQImpl" };
                for (final String accessor : accessors) {
                    try {
                        final var method = p.getClass().getMethod(accessor);
                        method.setAccessible(true);
                        final Object impl = method.invoke(p);
                        if (impl instanceof RDQImpl rdqImpl) {
                            CACHED_RDQ = rdqImpl;
                            return rdqImpl;
                        }
                    } catch (NoSuchMethodException ignored) {
                        // Try next accessor
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Failed to resolve RDQImpl via plugin name: " + name, e);
            }
        }

        // As a last resort, try accessing a known static field if present (optional)
        try {
            final Class<?> rdqClass = Class.forName("com.raindropcentral.rdq.RDQImpl");
            final var field = rdqClass.getDeclaredField("INSTANCE");
            field.setAccessible(true);
            final Object value = field.get(null);
            if (value instanceof RDQImpl rdq) {
                CACHED_RDQ = rdq;
                return rdq;
            }
        } catch (Exception ignored) {
            // No static instance available
        }

        return null;
    }

    /**
     * Creates a statistic requirement for kill counts.
     *
     * @param entityType     The entity type to count (e.g., "ZOMBIE", "SKELETON")
     * @param requiredKills  The number of kills required
     * @param mode           The requirement mode
     * @return A new RStatisticRequirement for kills
     */
    public static RStatisticRequirement createKillRequirement(
            @NotNull final String entityType,
            final int requiredKills,
            @NotNull final RequirementMode mode
    ) {
        return new RStatisticRequirement(
                EStatisticType.StatisticCategory.RDQ.name(),
                "kills_" + entityType.toLowerCase(),
                requiredKills,
                mode,
                entityType,
                RStatisticFactory.StatisticType.NUMBER,
                null
        );
    }

    /**
     * Creates a statistic requirement for block breaking.
     *
     * @param blockType      The block type to count (e.g., "DIAMOND_ORE", "STONE")
     * @param requiredBlocks The number of blocks required
     * @param mode           The requirement mode
     * @return A new RStatisticRequirement for block breaking
     */
    public static RStatisticRequirement createBlockBreakRequirement(
            @NotNull final String blockType,
            final int requiredBlocks,
            @NotNull final RequirementMode mode
    ) {
        return new RStatisticRequirement(
                EStatisticType.StatisticCategory.RDQ.name(),
                "blocks_broken_" + blockType.toLowerCase(),
                requiredBlocks,
                mode,
                blockType,
                RStatisticFactory.StatisticType.NUMBER,
                null
        );
    }

    /**
     * Creates a statistic requirement for playtime.
     *
     * @param requiredMinutes The required playtime in minutes
     * @param mode            The requirement mode
     * @return A new RStatisticRequirement for playtime
     */
    public static RStatisticRequirement createPlaytimeRequirement(
            final int requiredMinutes,
            @NotNull final RequirementMode mode
    ) {
        return new RStatisticRequirement(
                EStatisticType.StatisticCategory.RDQ.name(),
                "playtime_minutes",
                requiredMinutes,
                mode,
                null,
                RStatisticFactory.StatisticType.NUMBER,
                null
        );
    }

    public String getPlugin() {
        return plugin;
    }

    public String getIdentifier() {
        return identifier;
    }

    public double getRequiredAmount() {
        return requiredAmount;
    }

    public RequirementMode getMode() {
        return mode;
    }

    public String getQualifier() {
        return qualifier;
    }

    public RStatisticFactory.StatisticType getStatisticType() {
        return statisticType;
    }

    public Double getStartingValue() {
        return startingValue;
    }

    /**
     * Enumeration of requirement modes for statistic requirements.
     */
    public enum RequirementMode {
        /**
         * Player must have at least X total of the statistic.
         * Example: "Have killed at least 100 zombies total"
         */
        ABSOLUTE,

        /**
         * Player must gain X more of the statistic from when the requirement started.
         * Example: "Kill 5 more zombies from when you started this quest"
         */
        RELATIVE
    }
}