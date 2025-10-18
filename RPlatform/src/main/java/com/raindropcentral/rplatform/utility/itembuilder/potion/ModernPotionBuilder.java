package com.raindropcentral.rplatform.utility.itembuilder.potion;

import com.raindropcentral.rplatform.utility.itembuilder.AItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

public class ModernPotionBuilder extends AItemBuilder<PotionMeta, ModernPotionBuilder> implements IPotionBuilder<ModernPotionBuilder> {
	
	public ModernPotionBuilder() {
		super(Material.POTION);
	}
	
	public ModernPotionBuilder setBasePotionType(@NotNull PotionType type) {
		meta.setBasePotionData(new PotionData(type));
		return this;
	}
	
	public ModernPotionBuilder addCustomEffect(
		@NotNull PotionEffect effect,
		boolean overwrite
	) {
		meta.addCustomEffect(effect, overwrite);
		return this;
	}
	
}