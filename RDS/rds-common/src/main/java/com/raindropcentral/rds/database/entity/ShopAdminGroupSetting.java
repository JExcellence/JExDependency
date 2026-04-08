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

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;

/**
 * Persists one admin override definition for a LuckPerms group.
 *
 * <p>Each row may define a max-shops override, a discount override, or both.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@Entity
@Table(name = "shop_admin_group_settings")
@SuppressWarnings({
        "FieldCanBeLocal",
        "unused",
        "JpaDataSourceORMInspection"
})
/**
 * Represents the ShopAdminGroupSetting API type.
 */
public class ShopAdminGroupSetting extends BaseEntity {

    @Column(name = "group_name", nullable = false, unique = true)
    private String group_name;

    @Column(name = "maximum_shops")
    private Integer maximum_shops;

    @Column(name = "discount_percent")
    private Double discount_percent;

    /**
     * Creates a persisted group override.
     *
     * @param groupName normalized LuckPerms group identifier
     * @param maximumShops optional maximum shops override ({@code -1} means unlimited)
     * @param discountPercent optional discount override in range {@code 0.0} to {@code 100.0}
     */
    public ShopAdminGroupSetting(
            final @NotNull String groupName,
            final @Nullable Integer maximumShops,
            final @Nullable Double discountPercent
    ) {
        this.group_name = normalizeGroupName(groupName);
        this.maximum_shops = maximumShops;
        this.discount_percent = discountPercent;
    }

    /**
     * Constructor reserved for JPA hydration.
     */
    protected ShopAdminGroupSetting() {
    }

    /**
     * Returns the normalized group identifier.
     *
     * @return group identifier
     */
    public @NotNull String getGroupName() {
        return this.group_name;
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
     * Replaces the normalized group identifier.
     *
     * @param groupName replacement group identifier
     */
    public void setGroupName(
            final @NotNull String groupName
    ) {
        this.group_name = normalizeGroupName(groupName);
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

    private static @NotNull String normalizeGroupName(
            final @NotNull String groupName
    ) {
        final String normalized = Objects.requireNonNull(groupName, "groupName")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be blank");
        }
        return normalized;
    }
}
