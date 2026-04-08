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
 * Immutable preview of one level reward entry.
 *
 * @param definitionKey owning configured reward key
 * @param typeId reward type identifier
 * @param title rendered display name
 * @param description rendered description
 * @param displayItem display item for GUI rendering
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public record TownLevelRewardSnapshot(
    @NotNull String definitionKey,
    @NotNull String typeId,
    @NotNull String title,
    @NotNull String description,
    @Nullable ItemStack displayItem
) {

    /**
     * Creates an immutable reward preview entry.
     *
     * @param definitionKey owning configured reward key
     * @param typeId reward type identifier
     * @param title rendered display name
     * @param description rendered description
     * @param displayItem display item for GUI rendering
     */
    public TownLevelRewardSnapshot {
        definitionKey = Objects.requireNonNull(definitionKey, "definitionKey");
        typeId = Objects.requireNonNull(typeId, "typeId");
        title = Objects.requireNonNull(title, "title");
        description = Objects.requireNonNull(description, "description");
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
}
