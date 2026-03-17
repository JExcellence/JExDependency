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

package com.raindropcentral.rplatform.utility.itembuilder;

import com.raindropcentral.rplatform.utility.unified.IUnifiedItemBuilder;
import com.raindropcentral.rplatform.version.ServerEnvironment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Base builder that adapts Paper and legacy Bukkit metadata APIs into a unified fluent surface.
 *
 * <p>The builder caches the mutable {@link ItemStack} and {@link ItemMeta} references while
 * performing version-sensitive operations. Subclasses expose domain specific entry points while
 * still returning the generic builder type.</p>
 *
 * @param <T> concrete {@link ItemMeta} subtype handled by the builder
 * @param <B> fluent builder type returned from chained calls
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class AItemBuilder<T extends ItemMeta, B extends IUnifiedItemBuilder<T, B>> {

        /**
         * Mutable item instance accumulating metadata changes prior to {@link #build()}.
         */
        protected ItemStack item;

        /**
         * Cached metadata handle used to apply version-aware updates.
         */
        protected T meta;

        /**
         * Creates a new builder for the provided material using a freshly constructed item.
         *
         * @param material material backing the item stack
         */
        protected AItemBuilder(@NotNull Material material) {
                this(new ItemStack(material));
        }

        /**
         * Creates a builder around an existing {@link ItemStack} instance.
         *
         * @param item base item stack to mutate
         */
        protected AItemBuilder(@NotNull ItemStack item) {
                this.item = item;

                //noinspection unchecked
                this.meta = (T) item.getItemMeta();
        }

        /**
         * Applies a display name using Adventure components, falling back to legacy serializers.
         * when Paper-specific APIs are unavailable.
         *
         * @param name translated display name component
         * @return fluent builder reference for chaining
         */
        @SuppressWarnings("unchecked")
        public B setName(@NotNull Component name) {
                if (ServerEnvironment.getInstance().isPaper()) {
                        try {
                                meta.displayName(name);
                        } catch (NoSuchMethodError e) {
                                // Fallback to legacy method
                                setNameLegacy(name);
                        }
                } else {
                        // Use legacy method for Spigot/Bukkit
                        setNameLegacy(name);
                }
                return (B) this;
        }

        /**
         * Applies lore components, handling Paper native APIs and falling back to serialized.
         * legacy strings when necessary.
         *
         * @param lore ordered list of lore lines to render
         * @return fluent builder reference for chaining
         */
        @SuppressWarnings("unchecked")
        public B setLore(@NotNull List<Component> lore) {
                if (ServerEnvironment.getInstance().isPaper()) {
                        // Use Paper's native Adventure API
                        try {
                                meta.lore(lore);
                        } catch (NoSuchMethodError e) {
                                // Fallback to legacy method
                                setLoreLegacy(lore);
                        }
                } else {
                        // Use legacy method for Spigot/Bukkit
                        setLoreLegacy(lore);
                }
                return (B) this;
        }

        /**
         * Adds a single lore line while preserving existing content, re-reading from metadata to.
         * ensure compatibility with both Paper and legacy APIs.
         *
         * @param line lore component to append
         * @return fluent builder reference for chaining
         */
        @SuppressWarnings("unchecked")
        public B addLoreLine(@NotNull Component line) {
                List<Component> currentLore = getCurrentLore();
                currentLore.add(line);
                setLore(currentLore);
                return (B) this;
        }

        /**
         * Adds multiple lore lines from a pre-built list.
         *
         * @param lore lines to append in order
         * @return fluent builder reference for chaining
         */
        public B addLoreLines(@NotNull List<Component> lore) {
                List<Component> currentLore = getCurrentLore();
                currentLore.addAll(lore);
                setLore(currentLore);
                return (B) this;
        }

        /**
         * Adds multiple lore lines from a varargs component sequence.
         *
         * @param lore lore components to append
         * @return fluent builder reference for chaining
         */
        public B addLoreLines(@NotNull Component... lore) {
                List<Component> currentLore = getCurrentLore();
                currentLore.addAll(Arrays.asList(lore));
                setLore(currentLore);
                return (B) this;
        }

        /**
         * Sets the display name using legacy methods for Spigot/Bukkit compatibility.
         *
         * @param name translated display name component
         */
        private void setNameLegacy(@NotNull Component name) {
                String legacyName = LegacyComponentSerializer.legacySection().serialize(name);
                meta.setDisplayName(legacyName);
        }

        /**
         * Sets the lore using legacy methods for Spigot/Bukkit compatibility.
         *
         * @param lore lore components to serialize through the legacy serializer
         */
        private void setLoreLegacy(@NotNull List<Component> lore) {
                List<String> legacyLore = new ArrayList<>();
                for (Component component : lore) {
                        legacyLore.add(LegacyComponentSerializer.legacySection().serialize(component));
                }
                meta.setLore(legacyLore);
        }

        /**
         * Gets the current lore in a platform-compatible way.
         *
         * @return mutable copy of the current lore components
         */
        private List<Component> getCurrentLore() {
                if (ServerEnvironment.getInstance().isPaper()) {
                        try {
                                List<Component> lore = meta.lore();
                                return lore != null ? new ArrayList<>(lore) : new ArrayList<>();
                        } catch (NoSuchMethodError e) {
                                // Fallback to legacy method
                                return getCurrentLoreLegacy();
                        }
                } else {
                        return getCurrentLoreLegacy();
                }
        }

        /**
         * Gets the current lore using legacy methods.
         *
         * @return mutable copy of the current lore components
         */
        private List<Component> getCurrentLoreLegacy() {
                List<String> legacyLore = meta.getLore();
                if (legacyLore == null) {
                        return new ArrayList<>();
                }

                List<Component> componentLore = new ArrayList<>();
                for (String line : legacyLore) {
                        componentLore.add(LegacyComponentSerializer.legacySection().deserialize(line));
                }
                return componentLore;
        }

        /**
         * Sets the resulting item stack amount.
         *
         * @param amount new stack size
         * @return fluent builder reference for chaining
         */
        @SuppressWarnings("unchecked")
        public B setAmount(int amount) {
                item.setAmount(amount);
                return (B) this;
        }

        /**
         * Applies custom model data values for resource-pack driven overrides.
         *
         * @param data custom model data value
         * @return fluent builder reference for chaining
         */
        @SuppressWarnings("unchecked")
        public B setCustomModelData(int data) {
                meta.setCustomModelData(data);
                return (B) this;
        }

        /**
         * Adds an enchantment while forcing higher-than-default levels when requested.
         *
         * @param enchantment enchantment to add
         * @param level level to apply
         * @return fluent builder reference for chaining
         */
        @SuppressWarnings("unchecked")
        public B addEnchantment(
                @NotNull Enchantment enchantment,
                int level
        ) {
                meta.addEnchant(enchantment, level, true);
                return (B) this;
        }
	
        /**
         * Appends one or more {@link ItemFlag} instances to the metadata.
         *
         * @param flags flags to add
         * @return fluent builder reference for chaining
         */
        @SuppressWarnings("unchecked")
        public B addItemFlags(@NotNull ItemFlag... flags) {
                meta.addItemFlags(flags);
                return (B) this;
        }

        /**
         * Toggles the glowing enchantment trick by adding or removing a harmless enchant and.
         * hiding it from tooltips.
         *
         * @param glowing whether the item should glow
         * @return fluent builder reference for chaining
         */
        @SuppressWarnings("unchecked")
        public B setGlowing(boolean glowing) {
                if (glowing) {
                        meta.addEnchant(Enchantment.LURE, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } else {
                        meta.removeEnchant(Enchantment.LURE);
                        meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                return (B) this;
        }

        /**
         * Commits metadata changes back to the underlying {@link ItemStack} and returns it for use.
         * in inventories or menu renderers.
         *
         * @return fully built item stack
         */
        public ItemStack build() {
                item.setItemMeta(meta);
                return item;
        }

}
