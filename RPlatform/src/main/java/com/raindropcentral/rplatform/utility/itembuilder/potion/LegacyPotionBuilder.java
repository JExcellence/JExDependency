package com.raindropcentral.rplatform.utility.itembuilder.potion;

import com.raindropcentral.rplatform.utility.itembuilder.AItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

/**
 * Bukkit legacy implementation of {@link IPotionBuilder} that adapts pre-1.13 potion metadata APIs.
 *
 * <p>Constructed by {@link com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory#potion()} when
 * running on non-Paper platforms.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@SuppressWarnings("deprecation")
public class LegacyPotionBuilder extends AItemBuilder<PotionMeta, LegacyPotionBuilder> implements IPotionBuilder<LegacyPotionBuilder> {

        /**
         * Creates a legacy potion builder targeting the base potion material.
         */
        public LegacyPotionBuilder() {
                super(Material.valueOf("POTION"));
        }

        /**
         * Applies the requested base potion type using the legacy main effect bridge.
         *
         * @param type Bukkit potion type describing the base effects
         * @return fluent builder reference for chaining
         */
        @Override
        public LegacyPotionBuilder setBasePotionType(@NotNull PotionType type) {
                try {
                        meta.setMainEffect(type.getEffectType());
                } catch (Exception ignored) {
                }
                return this;
        }

        /**
         * Adds a custom potion effect using legacy metadata APIs.
         *
         * @param effect potion effect to append
         * @param overwrite whether to replace an existing effect of the same type
         * @return fluent builder reference for chaining
         */
        @Override
        public LegacyPotionBuilder addCustomEffect(
                @NotNull PotionEffect effect,
                boolean overwrite
        ) {
                meta.addCustomEffect(effect, overwrite);
                return this;
        }

}