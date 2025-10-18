package com.raindropcentral.rdq.requirement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rdq.service.RCoreBridge;
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

public final class RStatisticRequirement extends AbstractRequirement {

    private static final Logger LOGGER = CentralLogger.getLogger(RStatisticRequirement.class);
    private static final long DEFAULT_TIMEOUT_MS = 75L;

    private static volatile @Nullable Supplier<@Nullable RCoreBridge> BRIDGE_SUPPLIER;

    public static void setBridgeSupplier(@NotNull Supplier<@Nullable RCoreBridge> supplier) {
        BRIDGE_SUPPLIER = supplier;
    }

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

    public RStatisticRequirement(
            final @NotNull String plugin,
            final @NotNull String identifier,
            final double requiredAmount,
            final @NotNull StatisticType.DataType statisticType
    ) {
        this(plugin, identifier, requiredAmount, RequirementMode.ABSOLUTE, null, statisticType, null, DEFAULT_TIMEOUT_MS);
    }

    public RStatisticRequirement(
            final @NotNull String plugin,
            final @NotNull String identifier,
            final double requiredAmount,
            final @NotNull StatisticType.DataType statisticType,
            final double startingValue
    ) {
        this(plugin, identifier, requiredAmount, RequirementMode.RELATIVE, null, statisticType, startingValue, DEFAULT_TIMEOUT_MS);
    }

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

    @NotNull
    public String getPlugin() {
        return this.plugin;
    }

    @NotNull
    public String getIdentifier() {
        return this.identifier;
    }

    public double getRequiredAmount() {
        return this.requiredAmount;
    }

    @NotNull
    public RequirementMode getMode() {
        return this.mode;
    }

    @Nullable
    public String getQualifier() {
        return this.qualifier;
    }

    @NotNull
    public StatisticType.DataType getStatisticType() {
        return this.statisticType;
    }

    @Nullable
    public Double getStartingValue() {
        return this.startingValue;
    }

    public long getTimeoutMillis() {
        return this.timeoutMillis;
    }

    public void initializeForPlayer(final @NotNull Player player) {
        if (mode == RequirementMode.RELATIVE && startingValue == null) {
            getStartingValue(player);
        }
    }

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