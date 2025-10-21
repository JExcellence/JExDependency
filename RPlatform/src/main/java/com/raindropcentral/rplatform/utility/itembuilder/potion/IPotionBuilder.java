package com.raindropcentral.rplatform.utility.itembuilder.potion;

import com.raindropcentral.rplatform.utility.unified.IUnifiedItemBuilder;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

/**
 * Version-agnostic interface for potion-specific builder methods covering base potion types and
 * supplemental effects.
 *
 * <p>Implementations extend the unified item builder contract and expose fluent APIs that work
 * across Paper and legacy Bukkit runtimes.</p>
 *
 * @param <B> concrete builder type returned from fluent calls
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public interface IPotionBuilder<B extends IPotionBuilder<B>> extends IUnifiedItemBuilder<PotionMeta, B> {

        /**
         * Sets the base potion type.
         *
         * @param type Bukkit {@link PotionType} to apply
         * @return fluent builder reference for chaining
         */
        B setBasePotionType(@NotNull PotionType type);

        /**
         * Adds a custom potion effect.
         *
         * @param effect potion effect to add
         * @param overwrite whether to replace an existing effect of the same type
         * @return fluent builder reference for chaining
         */
        B addCustomEffect(
                @NotNull PotionEffect effect,
                boolean overwrite
        );

}