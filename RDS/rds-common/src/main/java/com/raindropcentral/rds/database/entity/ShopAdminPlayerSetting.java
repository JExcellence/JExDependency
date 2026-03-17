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

package com.raindropcentral.rds.database.entity;

import java.util.Objects;
import java.util.UUID;

import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persists one admin override definition for an individual player.
 *
 * <p>Each row may define a max-shops override, a discount override, or both.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@Entity
@Table(name = "shop_admin_player_settings")
@SuppressWarnings({
        "FieldCanBeLocal",
        "unused",
        "JpaDataSourceORMInspection"
})
/**
 * Represents the ShopAdminPlayerSetting API type.
 */
public class ShopAdminPlayerSetting extends BaseEntity {

    @Column(name = "player_uuid", nullable = false, unique = true)
    @Convert(converter = UUIDConverter.class)
    private UUID player_uuid;

    @Column(name = "player_name")
    private String player_name;

    @Column(name = "maximum_shops")
    private Integer maximum_shops;

    @Column(name = "discount_percent")
    private Double discount_percent;

    /**
     * Creates a persisted player override.
     *
     * @param playerId target player UUID
     * @param playerName optional cached player name
     * @param maximumShops optional maximum shops override ({@code -1} means unlimited)
     * @param discountPercent optional discount override in range {@code 0.0} to {@code 100.0}
     */
    public ShopAdminPlayerSetting(
            final @NotNull UUID playerId,
            final @Nullable String playerName,
            final @Nullable Integer maximumShops,
            final @Nullable Double discountPercent
    ) {
        this.player_uuid = Objects.requireNonNull(playerId, "playerId");
        this.player_name = normalizeOptionalName(playerName);
        this.maximum_shops = maximumShops;
        this.discount_percent = discountPercent;
    }

    /**
     * Constructor reserved for JPA hydration.
     */
    protected ShopAdminPlayerSetting() {
    }

    /**
     * Returns the target player UUID.
     *
     * @return player UUID
     */
    public @NotNull UUID getPlayerId() {
        return this.player_uuid;
    }

    /**
     * Returns the optional cached player name.
     *
     * @return cached player name, or {@code null} when unset
     */
    public @Nullable String getPlayerName() {
        return this.player_name;
    }

    /**
     * Returns the optional maximum shops override.
     *
     * @return maximum shops override, or {@code null} when unset
     */
    public @Nullable Integer getMaximumShops() {
        return this.maximum_shops;
    }

    /**
     * Returns the optional discount override.
     *
     * @return discount percent override, or {@code null} when unset
     */
    public @Nullable Double getDiscountPercent() {
        return this.discount_percent;
    }

    /**
     * Replaces the target player UUID.
     *
     * @param playerId replacement player UUID
     */
    public void setPlayerId(
            final @NotNull UUID playerId
    ) {
        this.player_uuid = Objects.requireNonNull(playerId, "playerId");
    }

    /**
     * Replaces the optional cached player name.
     *
     * @param playerName replacement cached player name
     */
    public void setPlayerName(
            final @Nullable String playerName
    ) {
        this.player_name = normalizeOptionalName(playerName);
    }

    /**
     * Replaces the optional maximum shops override.
     *
     * @param maximumShops replacement maximum shops override
     */
    public void setMaximumShops(
            final @Nullable Integer maximumShops
    ) {
        this.maximum_shops = maximumShops;
    }

    /**
     * Replaces the optional discount override.
     *
     * @param discountPercent replacement discount percent override
     */
    public void setDiscountPercent(
            final @Nullable Double discountPercent
    ) {
        this.discount_percent = discountPercent;
    }

    private static @Nullable String normalizeOptionalName(
            final @Nullable String rawValue
    ) {
        if (rawValue == null) {
            return null;
        }

        final String normalized = rawValue.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
