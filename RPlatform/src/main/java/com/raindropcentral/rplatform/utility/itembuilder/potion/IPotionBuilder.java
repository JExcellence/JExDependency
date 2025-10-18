package com.raindropcentral.rplatform.utility.itembuilder.potion;

import com.raindropcentral.rplatform.utility.unified.IUnifiedItemBuilder;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

/**
 * Version-agnostic interface for potion-specific builder methods.
 * Supports both legacy and modern implementations.
 */
public interface IPotionBuilder<B extends IPotionBuilder<B>> extends IUnifiedItemBuilder<PotionMeta, B> {
	
	/**
	 * Sets the base potion type.
	 *
	 * @param type The Bukkit PotionType.
	 *
	 * @return this builder for chaining.
	 */
	B setBasePotionType(@NotNull PotionType type);
	
	/**
	 * Adds a custom potion effect.
	 *
	 * @param effect    The PotionEffect to add.
	 * @param overwrite Whether to overwrite an existing effect of the same type.
	 *
	 * @return this builder for chaining.
	 */
	B addCustomEffect(
		@NotNull PotionEffect effect,
		boolean overwrite
	);
	
}