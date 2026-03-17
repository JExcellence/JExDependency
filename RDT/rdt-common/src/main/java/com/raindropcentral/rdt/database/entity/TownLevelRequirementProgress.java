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

package com.raindropcentral.rdt.database.entity;

import com.raindropcentral.rplatform.database.converter.ItemStackConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Persistent banked progress for one town-level requirement token.
 *
 * <p>Each row belongs to exactly one {@link RTown} and stores either banked currency amount,
 * banked item stack progress, or both for a single level-requirement progress key.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@Entity
@Table(
        name = "town_level_requirement_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_town_level_requirement_progress_town_key",
                columnNames = {"town_id", "progress_key"}
        )
)
@SuppressWarnings({
        "FieldCanBeLocal",
        "unused",
        "JpaDataSourceORMInspection"
})
/**
 * Represents the TownLevelRequirementProgress API type.
 */
public class TownLevelRequirementProgress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "town_id", nullable = false)
    private RTown town;

    @Column(name = "progress_key", nullable = false, length = 255)
    private String progressKey;

    @Column(name = "currency_amount", nullable = false)
    private double currencyAmount;

    @Convert(converter = ItemStackConverter.class)
    @Column(name = "item_stack", columnDefinition = "LONGTEXT")
    private ItemStack itemStack;

    /**
     * Creates a new progress row for the provided town and requirement token.
     *
     * @param town owning town entity
     * @param progressKey stable requirement progress token
     */
    public TownLevelRequirementProgress(
            final @NotNull RTown town,
            final @NotNull String progressKey
    ) {
        this.progressKey = normalizeProgressKey(progressKey);
        Objects.requireNonNull(town, "town cannot be null").addLevelRequirementProgress(this);
    }

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected TownLevelRequirementProgress() {
    }

    /**
     * Returns the owning town.
     *
     * @return owning town, or {@code null} only when detached in memory
     */
    public @Nullable RTown getTown() {
        return this.town;
    }

    /**
     * Returns the stable requirement progress token for this row.
     *
     * @return normalized progress token
     */
    public @NotNull String getProgressKey() {
        return this.progressKey;
    }

    /**
     * Returns the currently banked currency amount.
     *
     * @return non-negative banked currency amount
     */
    public double getCurrencyAmount() {
        return this.currencyAmount;
    }

    /**
     * Updates the banked currency amount.
     *
     * @param currencyAmount replacement amount; negative values are clamped to {@code 0.0}
     */
    public void setCurrencyAmount(final double currencyAmount) {
        this.currencyAmount = Math.max(0.0D, currencyAmount);
    }

    /**
     * Returns the currently banked item stack.
     *
     * @return cloned banked stack, or {@code null} when none exists
     */
    public @Nullable ItemStack getItemStack() {
        return cloneItem(this.itemStack);
    }

    /**
     * Updates the banked item stack.
     *
     * @param itemStack replacement stack, or {@code null} to clear item progress
     */
    public void setItemStack(final @Nullable ItemStack itemStack) {
        this.itemStack = cloneItem(itemStack);
    }

    /**
     * Returns whether this row stores banked currency progress.
     *
     * @return {@code true} when the currency amount is positive
     */
    public boolean hasCurrencyProgress() {
        return this.currencyAmount > 0.0D;
    }

    /**
     * Returns whether this row stores banked item progress.
     *
     * @return {@code true} when the item stack is non-empty
     */
    public boolean hasItemProgress() {
        return this.itemStack != null && !this.itemStack.isEmpty();
    }

    /**
     * Returns whether this row no longer stores any progress payload.
     *
     * @return {@code true} when the row is effectively empty
     */
    public boolean isEmpty() {
        return !this.hasCurrencyProgress() && !this.hasItemProgress();
    }

    void setTownInternal(final @Nullable RTown town) {
        this.town = town;
    }

    private static @NotNull String normalizeProgressKey(final @NotNull String progressKey) {
        final String normalizedProgressKey = Objects.requireNonNull(progressKey, "progressKey cannot be null").trim();
        if (normalizedProgressKey.isEmpty()) {
            throw new IllegalArgumentException("progressKey cannot be blank");
        }
        return normalizedProgressKey;
    }

    private static @Nullable ItemStack cloneItem(final @Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return null;
        }
        return itemStack.clone();
    }
}
