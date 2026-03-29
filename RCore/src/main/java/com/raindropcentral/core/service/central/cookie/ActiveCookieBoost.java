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

package com.raindropcentral.core.service.central.cookie;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.raindropcentral.rplatform.cookie.CookieBoostType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Persisted active timed boost from a droplet cookie.
 */
public record ActiveCookieBoost(
        @NotNull UUID playerId,
        @NotNull CookieBoostType boostType,
        @Nullable String integrationId,
        @Nullable String targetId,
        @NotNull String itemCode,
        double rateBonus,
        long expiresAtEpochMs
) {

    private static final String IDENTIFIER_PREFIX = "droplet_cookie.boost.";

    public ActiveCookieBoost {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(boostType, "boostType cannot be null");
        Objects.requireNonNull(itemCode, "itemCode cannot be null");
    }

    /**
     * Returns whether the boost has reached or passed its expiry time.
     *
     * @param nowEpochMs current wall-clock time in epoch milliseconds
     * @return {@code true} when the boost is no longer active
     */
    public boolean isExpired(final long nowEpochMs) {
        return this.expiresAtEpochMs <= nowEpochMs;
    }

    /**
     * Returns the effective multiplier represented by this boost.
     *
     * @return {@code 1.0 + rateBonus}
     */
    public double multiplier() {
        return 1.0D + this.rateBonus;
    }

    /**
     * Returns the in-memory lookup key used by {@link ActiveCookieBoostService}.
     *
     * @return normalized cache key combining boost type, integration, and target identifiers
     */
    public @NotNull String cacheKey() {
        return this.boostType.name() + "|" + normalize(this.integrationId) + "|" + normalize(this.targetId);
    }

    /**
     * Returns the statistic identifier used to persist the boost in player statistics.
     *
     * @return statistic identifier prefixed for droplet cookie boosts
     */
    public @NotNull String statisticIdentifier() {
        final StringBuilder builder = new StringBuilder(IDENTIFIER_PREFIX)
                .append(toStatisticKey(this.boostType));
        if (this.integrationId != null && !this.integrationId.isBlank()) {
            builder.append('.').append(normalize(this.integrationId));
        }
        if (this.targetId != null && !this.targetId.isBlank()) {
            builder.append('.').append(normalize(this.targetId));
        }
        return builder.toString();
    }

    /**
     * Serializes the boost into the JSON payload stored inside a string statistic.
     *
     * @return JSON representation of the boost state
     */
    public @NotNull String serialize() {
        final JsonObject payload = new JsonObject();
        payload.addProperty("boostType", this.boostType.name());
        payload.addProperty("itemCode", this.itemCode);
        payload.addProperty("rateBonus", this.rateBonus);
        payload.addProperty("expiresAtEpochMs", this.expiresAtEpochMs);
        if (this.integrationId != null && !this.integrationId.isBlank()) {
            payload.addProperty("integrationId", this.integrationId);
        }
        if (this.targetId != null && !this.targetId.isBlank()) {
            payload.addProperty("targetId", this.targetId);
        }
        return payload.toString();
    }

    /**
     * Attempts to restore a persisted boost from a statistic entry.
     *
     * @param playerId owning player identifier
     * @param identifier persisted statistic identifier
     * @param rawValue serialized statistic payload
     * @return decoded boost when the identifier and payload match the expected format
     */
    public static @NotNull Optional<ActiveCookieBoost> deserialize(
            final @NotNull UUID playerId,
            final @NotNull String identifier,
            final @NotNull String rawValue
    ) {
        if (!identifier.startsWith(IDENTIFIER_PREFIX) || rawValue.isBlank()) {
            return Optional.empty();
        }

        try {
            final JsonObject payload = JsonParser.parseString(rawValue).getAsJsonObject();
            final CookieBoostType boostType = CookieBoostType.valueOf(payload.get("boostType").getAsString());
            final String itemCode = payload.get("itemCode").getAsString();
            final double rateBonus = payload.get("rateBonus").getAsDouble();
            final long expiresAt = payload.get("expiresAtEpochMs").getAsLong();
            final String integrationId = payload.has("integrationId") ? payload.get("integrationId").getAsString() : null;
            final String targetId = payload.has("targetId") ? payload.get("targetId").getAsString() : null;
            return Optional.of(new ActiveCookieBoost(playerId, boostType, integrationId, targetId, itemCode, rateBonus, expiresAt));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static @NotNull String normalize(final @Nullable String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace(" ", "_");
    }

    private static @NotNull String toStatisticKey(final @NotNull CookieBoostType boostType) {
        return switch (boostType) {
            case SKILL_XP -> "skill_xp";
            case JOB_XP -> "job_xp";
            case JOB_VAULT -> "job_vault";
            case DOUBLE_DROP -> "double_drop";
        };
    }
}
