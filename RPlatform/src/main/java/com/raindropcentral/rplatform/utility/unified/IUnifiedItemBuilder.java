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

package com.raindropcentral.rplatform.utility.unified;

import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Unified builder interface for item-like constructs including inventory items, heads, and potions.
 *
 * <p>The interface mirrors fluent operations exposed by {@link com.raindropcentral.rplatform.utility.itembuilder.AItemBuilder}
 * and specialised builders so callers can operate on version specific implementations without
 * branching. Each method returns the generic builder type to allow method chaining.</p>
 *
 * @param <T> concrete {@link ItemMeta} type manipulated by the builder
 * @param <B> concrete builder type returned from fluent calls
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public interface IUnifiedItemBuilder<T extends ItemMeta, B extends IUnifiedItemBuilder<T, B>> {

        /**
         * Applies the translated display name component.
         *
         * @param name resolved display name
         * @return fluent builder reference for chaining
         */
        B setName(@NotNull Component name);

        /**
         * Replaces the lore with the provided ordered components.
         *
         * @param lore lore components to render
         * @return fluent builder reference for chaining
         */
        B setLore(@NotNull List<Component> lore);

        /**
         * Appends a single lore line.
         *
         * @param line lore component to append
         * @return fluent builder reference for chaining
         */
        B addLoreLine(@NotNull Component line);

        /**
         * Appends multiple lore lines supplied as a list.
         *
         * @param lore lore components to append
         * @return fluent builder reference for chaining
         */
        B addLoreLines(@NotNull List<Component> lore);

        /**
         * Appends multiple lore lines supplied as varargs.
         *
         * @param lore lore components to append
         * @return fluent builder reference for chaining
         */
        B addLoreLines(@NotNull Component... lore);

        /**
         * Updates the stack size.
         *
         * @param amount new stack size
         * @return fluent builder reference for chaining
         */
        B setAmount(int amount);

        /**
         * Sets the custom model data for resource-pack overrides.
         *
         * @param data custom model value
         * @return fluent builder reference for chaining
         */
        B setCustomModelData(int data);

        /**
         * Adds an enchantment level, forcing unsafe levels when requested.
         *
         * @param enchantment enchantment to apply
         * @param level level to set
         * @return fluent builder reference for chaining
         */
        B addEnchantment(
                @NotNull Enchantment enchantment,
                int level
        );

        /**
         * Adds one or more {@link ItemFlag ItemFlags} to the item metadata.
         *
         * @param flags flags to add
         * @return fluent builder reference for chaining
         */
        B addItemFlags(@NotNull ItemFlag... flags);

        /**
         * Toggles the glowing appearance.
         *
         * @param glowing whether to enable the glow effect
         * @return fluent builder reference for chaining
         */
        B setGlowing(boolean glowing);

        /**
         * Finalises metadata changes and returns the resulting item stack.
         *
         * @return built {@link ItemStack}
         */
        ItemStack build();

}
