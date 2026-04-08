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

package com.raindropcentral.rdt.service;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Immutable snapshot of one rendered level requirement.
 *
 * @param entryKey unique entry key for contribution actions
 * @param definitionKey owning configured requirement key
 * @param kind requirement kind
 * @param title rendered display name
 * @param description rendered description
 * @param requiredAmount required total amount or {@code 1.0} for generic progress checks
 * @param currentAmount saved or live progress amount
 * @param availableAmount currently available player contribution amount
 * @param completed whether the entry is complete
 * @param contributable whether the entry accepts saved turn-ins
 * @param exactMatch whether item matching requires exact similarity
 * @param currencyId currency identifier for currency requirements
 * @param displayItem display item for GUI rendering
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public record TownLevelRequirementSnapshot(
    @NotNull String entryKey,
    @NotNull String definitionKey,
    @NotNull RequirementKind kind,
    @NotNull String title,
    @NotNull String description,
    double requiredAmount,
    double currentAmount,
    double availableAmount,
    boolean completed,
    boolean contributable,
    boolean exactMatch,
    @Nullable String currencyId,
    @Nullable ItemStack displayItem
) {

    /**
     * Creates an immutable requirement snapshot.
     *
     * @param entryKey unique entry key for contribution actions
     * @param definitionKey owning configured requirement key
     * @param kind requirement kind
     * @param title rendered display name
     * @param description rendered description
     * @param requiredAmount required total amount or {@code 1.0} for generic progress checks
     * @param currentAmount saved or live progress amount
     * @param availableAmount currently available player contribution amount
     * @param completed whether the entry is complete
     * @param contributable whether the entry accepts saved turn-ins
     * @param exactMatch whether item matching requires exact similarity
     * @param currencyId currency identifier for currency requirements
     * @param displayItem display item for GUI rendering
     */
    public TownLevelRequirementSnapshot {
        entryKey = Objects.requireNonNull(entryKey, "entryKey");
        definitionKey = Objects.requireNonNull(definitionKey, "definitionKey");
        kind = Objects.requireNonNull(kind, "kind");
        title = Objects.requireNonNull(title, "title");
        description = Objects.requireNonNull(description, "description");
        requiredAmount = Math.max(0.0D, requiredAmount);
        currentAmount = Math.max(0.0D, currentAmount);
        availableAmount = Math.max(0.0D, availableAmount);
        displayItem = displayItem == null ? null : displayItem.clone();
    }

    /**
     * Returns a cloned display item for GUI rendering.
     *
     * @return cloned display item, or {@code null} when not applicable
     */
    @Override
    public @Nullable ItemStack displayItem() {
        return this.displayItem == null ? null : this.displayItem.clone();
    }

    /**
     * Returns normalized progress between {@code 0.0} and {@code 1.0}.
     *
     * @return normalized progress ratio
     */
    public double progress() {
        if (this.requiredAmount <= 0.0D) {
            return this.completed ? 1.0D : 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, this.currentAmount / this.requiredAmount));
    }
}
