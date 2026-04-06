/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdt.requirement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * Requirement backed by one town's aggregate in-town playtime.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownPlaytimeRequirement extends AbstractRequirement {

    private static volatile RDT runtime;

    @JsonProperty("requiredTownPlaytimeTicks")
    private final long requiredTownPlaytimeTicks;

    @JsonProperty("description")
    private final String description;

    /**
     * Creates a new town-playtime requirement.
     *
     * @param requiredTownPlaytimeTicks required in-town playtime in ticks
     * @param description optional description
     */
    public TownPlaytimeRequirement(final long requiredTownPlaytimeTicks, final @Nullable String description) {
        super("TOWN_PLAYTIME");
        this.requiredTownPlaytimeTicks = Math.max(0L, requiredTownPlaytimeTicks);
        this.description = description;
    }

    /**
     * Creates a new town-playtime requirement from JSON.
     *
     * @param requiredTownPlaytimeTicks required in-town playtime in ticks
     * @param requiredTownPlaytimeSeconds optional required playtime in seconds
     * @param description optional description
     */
    @JsonCreator
    public TownPlaytimeRequirement(
        @JsonProperty("requiredTownPlaytimeTicks") final @Nullable Long requiredTownPlaytimeTicks,
        @JsonProperty("requiredTownPlaytimeSeconds") final @Nullable Long requiredTownPlaytimeSeconds,
        @JsonProperty("description") final @Nullable String description
    ) {
        this(resolveRequiredTicks(requiredTownPlaytimeTicks, requiredTownPlaytimeSeconds), description);
    }

    /**
     * Binds the active RDT runtime for requirement evaluation.
     *
     * @param runtime active RDT runtime
     */
    public static void bindRuntime(final @NotNull RDT runtime) {
        TownPlaytimeRequirement.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    /**
     * Clears the currently bound runtime.
     */
    public static void clearRuntime() {
        TownPlaytimeRequirement.runtime = null;
    }

    /**
     * Creates a requirement from config-factory input.
     *
     * @param config config map
     * @return created requirement
     */
    public static @NotNull TownPlaytimeRequirement fromConfig(final @NotNull Map<String, Object> config) {
        final long requiredTicks = resolveRequiredTicks(
            asLong(config.get("requiredTownPlaytimeTicks")),
            asLong(config.get("requiredTownPlaytimeSeconds"))
        );
        final String description = config.get("description") == null ? null : String.valueOf(config.get("description"));
        return new TownPlaytimeRequirement(requiredTicks, description);
    }

    /**
     * Returns whether the current player's town has met the aggregate playtime threshold.
     *
     * @param player player performing the requirement check
     * @return {@code true} when the player's town has enough aggregate in-town playtime
     */
    @Override
    public boolean isMet(final @NotNull Player player) {
        final RTown town = this.resolveTown(player);
        return town != null && town.getAggregateTownPlaytimeTicks() >= this.requiredTownPlaytimeTicks;
    }

    /**
     * Returns normalized progress for the player's town aggregate playtime.
     *
     * @param player player performing the requirement check
     * @return normalized progress from {@code 0.0} to {@code 1.0}
     */
    @Override
    public double calculateProgress(final @NotNull Player player) {
        if (this.requiredTownPlaytimeTicks <= 0L) {
            return 1.0D;
        }

        final RTown town = this.resolveTown(player);
        if (town == null) {
            return 0.0D;
        }
        return Math.min(1.0D, (double) town.getAggregateTownPlaytimeTicks() / (double) this.requiredTownPlaytimeTicks);
    }

    /**
     * Consuming town playtime has no effect.
     *
     * @param player player consuming the requirement
     */
    @Override
    public void consume(final @NotNull Player player) {
    }

    /**
     * Returns the translation key for this requirement type.
     *
     * @return translation key
     */
    @Override
    public @NotNull String getDescriptionKey() {
        return "requirement.town_playtime";
    }

    /**
     * Returns the required in-town playtime in ticks.
     *
     * @return required in-town playtime in ticks
     */
    public long getRequiredTownPlaytimeTicks() {
        return this.requiredTownPlaytimeTicks;
    }

    /**
     * Returns the required in-town playtime in whole seconds.
     *
     * @return required in-town playtime in seconds
     */
    @JsonIgnore
    public long getRequiredTownPlaytimeSeconds() {
        return this.requiredTownPlaytimeTicks / 20L;
    }

    /**
     * Returns the optional description.
     *
     * @return optional description
     */
    public @Nullable String getDescription() {
        return this.description;
    }

    private @Nullable RTown resolveTown(final @NotNull Player player) {
        return runtime == null || runtime.getTownRuntimeService() == null
            ? null
            : runtime.getTownRuntimeService().getTownFor(player.getUniqueId());
    }

    private static long resolveRequiredTicks(
        final @Nullable Long ticks,
        final @Nullable Long seconds
    ) {
        if (ticks != null && ticks > 0L) {
            return ticks;
        }
        return seconds == null || seconds <= 0L ? 0L : seconds * 20L;
    }

    private static @Nullable Long asLong(final @Nullable Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String textValue) {
            try {
                return Long.parseLong(textValue);
            } catch (final NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
