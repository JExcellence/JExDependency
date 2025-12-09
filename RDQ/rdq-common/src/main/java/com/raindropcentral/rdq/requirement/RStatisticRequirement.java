package com.raindropcentral.rdq.requirement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rdq2.service.RCoreBridge;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.statistic.StatisticType;
import com.raindropcentral.rplatform.type.EStatisticType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Requirement that validates player progress against statistics provided by {@link RCoreBridge}.
 *
 * <p>The requirement supports both absolute and relative comparisons depending on the configured
 * {@link RequirementMode} and can be initialized through a number of convenience factory methods.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public final class RStatisticRequirement extends AbstractRequirement {

    private static final Logger LOGGER = CentralLogger.getLogger(RStatisticRequirement.class);
    private static final long DEFAULT_TIMEOUT_MS = 75L;

    private static volatile @Nullable Supplier<@Nullable RCoreBridge> BRIDGE_SUPPLIER;

    /**
     * Registers the supplier that delivers {@link RCoreBridge} instances used to fetch statistics.
     *
     * @param supplier supplier returning the shared bridge instance or {@code null}
     */
    public static void setBridgeSupplier(@NotNull Supplier<@Nullable RCoreBridge> supplier) {
        BRIDGE_SUPPLIER = supplier;
    }

    /**
     * Clears any previously registered bridge supplier.
     */
    public static void clearBridgeSupplier() {
        BRIDGE_SUPPLIER = null;
    }

    public enum RequirementMode {
        ABSOLUTE,
        RELATIVE
    }

    @JsonProperty("plugin")
    private final String plugin;

    @JsonProperty("identifier")
    private final String identifier;

    @JsonProperty("required_amount")
    private final double requiredAmount;

    @JsonProperty("mode")
    private final RequirementMode mode;

    @JsonProperty("starting_value")
    private volatile Double startingValue;

    @JsonProperty("qualifier")
    private final String qualifier;

    @JsonProperty("statistic_type")
    private final StatisticType.DataType statisticType;

    @JsonProperty("timeoutMillis")
    private final long timeoutMillis;

    /**
     * Creates a new statistic requirement instance.
     *
     * @param plugin         plugin namespace that owns the statistic
     * @param identifier     statistic identifier inside the namespace
     * @param requiredAmount amount to reach or exceed
     * @param mode           comparison mode to evaluate progress
     * @param qualifier      optional qualifier (for example entity or block type)
     * @param statisticType  type of statistic data being tracked
     * @param startingValue  pre-defined starting value used for relative requirements
     * @param timeoutMillis  optional timeout in milliseconds for statistic fetches
     */
    @JsonCreator
    public RStatisticRequirement(
            @JsonProperty("plugin") final @NotNull String plugin,
            @JsonProperty("identifier") final @NotNull String identifier,
            @JsonProperty("required_amount") final double requiredAmount,
            @JsonProperty("mode") final @NotNull RequirementMode mode,
            @JsonProperty("qualifier") final @Nullable String qualifier,
            @JsonProperty("statistic_type") final @NotNull StatisticType.DataType statisticType,
            @JsonProperty("starting_value") final @Nullable Double startingValue,
            @JsonProperty("timeoutMillis") final @Nullable Long timeoutMillis
    ) {
        super(Type.CUSTOM);
        this.plugin = plugin;
        this.identifier = identifier;
        this.requiredAmount = requiredAmount;
        this.mode = mode;
        this.qualifier = qualifier;
        this.statisticType = statisticType;
        this.startingValue = startingValue;
        this.timeoutMillis = timeoutMillis != null && timeoutMillis > 0 ? timeoutMillis : DEFAULT_TIMEOUT_MS;
    }

    /**
     * Creates an absolute statistic requirement.
     *
     * @param plugin         plugin namespace that owns the statistic
     * @param identifier     statistic identifier inside the namespace
     * @param requiredAmount amount to reach or exceed
     * @param statisticType  type of statistic data being tracked
     */
    public RStatisticRequirement(
            final @NotNull String plugin,
            final @NotNull String identifier,
            final double requiredAmount,
            final @NotNull StatisticType.DataType statisticType
    ) {
        this(plugin, identifier, requiredAmount, RequirementMode.ABSOLUTE, null, statisticType, null, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Creates a relative statistic requirement with a predefined starting value.
     *
     * @param plugin         plugin namespace that owns the statistic
     * @param identifier     statistic identifier inside the namespace
     * @param requiredAmount amount to gain beyond the starting value
     * @param statisticType  type of statistic data being tracked
     * @param startingValue  initial value representing the player's baseline statistic
     */
    public RStatisticRequirement(
            final @NotNull String plugin,
            final @NotNull String identifier,
            final double requiredAmount,
            final @NotNull StatisticType.DataType statisticType,
            final double startingValue
    ) {
        this(plugin, identifier, requiredAmount, RequirementMode.RELATIVE, null, statisticType, startingValue, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Determines whether the requirement is currently satisfied by the supplied player.
     *
     * @param player player to evaluate
     * @return {@code true} when the player's statistic meets or exceeds the requirement
     */
    @Override
    public boolean isMet(final @NotNull Player player) {
        try {
            final double currentValue = getCurrentStatisticValue(player);
            final double targetValue = getTargetValue(player);
            final boolean met = currentValue >= targetValue;
            LOGGER.log(Level.FINER, String.format(
                    "Checking statistic requirement for player %s: current=%.4f, target=%.4f, met=%s [id=%s, plugin=%s, mode=%s]",
                    player.getName(), currentValue, targetValue, met, identifier, plugin, mode
            ));
            return met;
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to check statistic requirement for player " + player.getName(), exception);
            return false;
        }
    }

    /**
     * Calculates the normalized progress for the supplied player.
     *
     * @param player player to evaluate
     * @return progress value between {@code 0.0} and {@code 1.0}
     */
    @Override
    public double calculateProgress(final @NotNull Player player) {
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
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to calculate progress for statistic requirement", exception);
            return 0.0;
        }
    }

    /**
     * Updates the cached starting value when the requirement is relative.
     *
     * @param player player being consumed
     */
    @Override
    public void consume(final @NotNull Player player) {
        try {
            if (mode == RequirementMode.RELATIVE) {
                final double currentValue = getCurrentStatisticValue(player);
                this.startingValue = currentValue;
                LOGGER.log(Level.FINER, String.format(
                        "Updated starting value for relative statistic requirement to %.4f for player %s",
                        currentValue, player.getName()
                ));
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to consume statistic requirement", exception);
        }
    }

    /**
     * Builds the translation key for representing this requirement in messages or GUIs.
     *
     * @return translation key based on the requirement configuration
     */
    @Override
    @NotNull
    public String getDescriptionKey() {
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
     * Retrieves the plugin namespace owning the statistic.
     *
     * @return plugin namespace
     */
    @NotNull
    public String getPlugin() {
        return this.plugin;
    }

    /**
     * Retrieves the statistic identifier.
     *
     * @return statistic identifier
     */
    @NotNull
    public String getIdentifier() {
        return this.identifier;
    }

    /**
     * Amount to reach or exceed for the requirement to succeed.
     *
     * @return target amount
     */
    public double getRequiredAmount() {
        return this.requiredAmount;
    }

    /**
     * Provides the configured comparison mode.
     *
     * @return requirement mode
     */
    @NotNull
    public RequirementMode getMode() {
        return this.mode;
    }

    /**
     * Retrieves the optional qualifier (such as an entity or block identifier).
     *
     * @return qualifier or {@code null} if not configured
     */
    @Nullable
    public String getQualifier() {
        return this.qualifier;
    }

    /**
     * Provides the statistic data type used to interpret values returned by the bridge.
     *
     * @return statistic data type
     */
    @NotNull
    public StatisticType.DataType getStatisticType() {
        return this.statisticType;
    }

    /**
     * Returns the cached starting value used for relative comparisons.
     *
     * @return starting value or {@code null} if not yet initialized
     */
    @Nullable
    public Double getStartingValue() {
        return this.startingValue;
    }

    /**
     * Fetch timeout applied when retrieving statistic values.
     *
     * @return timeout in milliseconds
     */
    public long getTimeoutMillis() {
        return this.timeoutMillis;
    }

    /**
     * Initializes the starting value for the supplied player when the requirement is relative.
     *
     * @param player player being prepared for evaluation
     */
    public void initializeForPlayer(final @NotNull Player player) {
        if (mode == RequirementMode.RELATIVE && startingValue == null) {
            getStartingValue(player);
        }
    }

    /**
     * Creates a statistic requirement tracking kills for a particular entity type.
     *
     * @param entityType    entity identifier to evaluate
     * @param requiredKills kills required to satisfy the requirement
     * @param mode          comparison mode to evaluate progress
     * @return kill requirement instance
     */
    @NotNull
    public static RStatisticRequirement createKillRequirement(
            final @NotNull String entityType,
            final int requiredKills,
            final @NotNull RequirementMode mode
    ) {
        return new RStatisticRequirement(
                EStatisticType.StatisticCategory.RDQ.name(),
                "kills_" + entityType.toLowerCase(),
                requiredKills,
                mode,
                entityType,
                StatisticType.DataType.NUMBER,
                null,
                DEFAULT_TIMEOUT_MS
        );
    }

    /**
     * Creates a statistic requirement tracking block breaks for the provided block type.
     *
     * @param blockType      block identifier to evaluate
     * @param requiredBlocks blocks required to satisfy the requirement
     * @param mode           comparison mode to evaluate progress
     * @return block break requirement instance
     */
    @NotNull
    public static RStatisticRequirement createBlockBreakRequirement(
            final @NotNull String blockType,
            final int requiredBlocks,
            final @NotNull RequirementMode mode
    ) {
        return new RStatisticRequirement(
                EStatisticType.StatisticCategory.RDQ.name(),
                "blocks_broken_" + blockType.toLowerCase(),
                requiredBlocks,
                mode,
                blockType,
                StatisticType.DataType.NUMBER,
                null,
                DEFAULT_TIMEOUT_MS
        );
    }

    /**
     * Creates a statistic requirement tracking playtime in minutes.
     *
     * @param requiredMinutes minutes required to satisfy the requirement
     * @param mode            comparison mode to evaluate progress
     * @return playtime requirement instance
     */
    @NotNull
    public static RStatisticRequirement createPlaytimeRequirement(
            final int requiredMinutes,
            final @NotNull RequirementMode mode
    ) {
        return new RStatisticRequirement(
                EStatisticType.StatisticCategory.RDQ.name(),
                "playtime_minutes",
                requiredMinutes,
                mode,
                null,
                StatisticType.DataType.NUMBER,
                null,
                DEFAULT_TIMEOUT_MS
        );
    }

    private double getCurrentStatisticValue(final @NotNull Player player) {
        try {
            final RCoreBridge bridge = resolveBridge();
            if (bridge == null) {
                LOGGER.log(Level.FINE, "RCoreBridge not available");
                return 0.0;
            }
            final CompletableFuture<Optional<Object>> future =
                    bridge.findStatisticValueAsync(player.getUniqueId(), this.identifier, this.plugin);
            final Optional<Object> opt = future
                    .completeOnTimeout(Optional.empty(), this.timeoutMillis, TimeUnit.MILLISECONDS)
                    .join();
            final Object value = opt.orElse(null);
            return convertToDouble(value);
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to get current statistic value", exception);
            return 0.0;
        }
    }

    private double getTargetValue(final @NotNull Player player) {
        if (mode == RequirementMode.ABSOLUTE) {
            return requiredAmount;
        } else {
            final double startValue = getStartingValue(player);
            return startValue + requiredAmount;
        }
    }

    private double getStartingValue(final @NotNull Player player) {
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

    private double convertToDouble(final @Nullable Object value) {
        if (value == null) {
            return 0.0;
        }
        try {
            return switch (value) {
                case Number number -> number.doubleValue();
                case Boolean bool -> bool ? 1.0 : 0.0;
                case String str -> {
                    try {
                        yield Double.parseDouble(str);
                    } catch (final NumberFormatException e) {
                        yield str.isEmpty() ? 0.0 : 1.0;
                    }
                }
                default -> 1.0;
            };
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to convert statistic value to double: " + value, exception);
            return 0.0;
        }
    }

    private @Nullable RCoreBridge resolveBridge() {
        final Supplier<RCoreBridge> s = BRIDGE_SUPPLIER;
        return s != null ? s.get() : null;
    }
}