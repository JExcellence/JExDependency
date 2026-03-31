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

package com.raindropcentral.rplatform.utility.itembuilder.potion;

import com.raindropcentral.rplatform.utility.itembuilder.AItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

/**
 * Paper-focused implementation of {@link IPotionBuilder} that leverages the modern potion data API.
 *
 * <p>Constructed when {@link com.raindropcentral.rplatform.version.ServerEnvironment#isPaper()} is
 * true, this builder exposes fluent metadata operations consistent with legacy behaviour.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class ModernPotionBuilder extends AItemBuilder<PotionMeta, ModernPotionBuilder> implements IPotionBuilder<ModernPotionBuilder> {

        /**
         * Creates a modern potion builder for the standard potion material.
         */
        public ModernPotionBuilder() {
                super(Material.POTION);
        }

        /**
         * Applies the base potion data supported by modern Bukkit builds.
         *
         * @param type Bukkit potion type describing the base effects
         * @return fluent builder reference for chaining
         */
        @Override
        public ModernPotionBuilder setBasePotionType(@NotNull PotionType type) {
                meta.setBasePotionData(new PotionData(type));
                return this;
        }

        /**
         * Adds a custom potion effect while preserving the modern metadata semantics.
         *
         * @param effect potion effect to append
         * @param overwrite whether to replace an existing effect of the same type
         * @return fluent builder reference for chaining
         */
        @Override
        public ModernPotionBuilder addCustomEffect(
                @NotNull PotionEffect effect,
                boolean overwrite
        ) {
                meta.addCustomEffect(effect, overwrite);
                return this;
        }

}