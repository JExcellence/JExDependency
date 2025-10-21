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
 * <p><strong>Implementation requirements.</strong> Builder implementations must mutate the
 * underlying {@link PotionMeta} so changes propagate to the final {@code ItemStack}. In
 * particular, {@link #setBasePotionType(PotionType)} must update the metadata using the
 * platform-appropriate setter (for example {@link PotionMeta#setBasePotionData(org.bukkit.potion.PotionData)} or
 * {@link PotionMeta#setMainEffect(org.bukkit.potion.PotionEffectType)} on legacy servers) and
 * {@link #addCustomEffect(PotionEffect, boolean)} must honour the {@code overwrite} flag when
 * delegating to {@link PotionMeta#addCustomEffect(PotionEffect, boolean)}. All fluent methods must
 * return the concrete builder type so chaining remains consistent with
 * {@link IUnifiedItemBuilder}.</p>
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