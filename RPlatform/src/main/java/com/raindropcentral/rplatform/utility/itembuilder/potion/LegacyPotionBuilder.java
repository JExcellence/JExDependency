package com.raindropcentral.rplatform.utility.itembuilder.potion;

import com.raindropcentral.rplatform.utility.itembuilder.AItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("deprecation")
public class LegacyPotionBuilder extends AItemBuilder<PotionMeta, LegacyPotionBuilder> implements IPotionBuilder<LegacyPotionBuilder> {
	
	public LegacyPotionBuilder() {
		super(Material.valueOf("POTION"));
	}
	
	@Override
	public LegacyPotionBuilder setBasePotionType(@NotNull PotionType type) {
		try {
			meta.setMainEffect(type.getEffectType());
		} catch (Exception ignored) {
		}
		return this;
	}
	
	public LegacyPotionBuilder addCustomEffect(
		@NotNull PotionEffect effect,
		boolean overwrite
	) {
		meta.addCustomEffect(effect, overwrite);
		return this;
	}
	
}