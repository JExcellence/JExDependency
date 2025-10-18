package com.raindropcentral.rplatform.utility.unified;

import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Unified builder interface for all item types (items, heads, potions).
 * Provides fluent API for common item meta operations.
 */
public interface IUnifiedItemBuilder<T extends ItemMeta, B extends IUnifiedItemBuilder<T, B>> {
	
	B setName(@NotNull Component name);
	
	B setLore(@NotNull List<Component> lore);
	
	B addLoreLine(@NotNull Component line);
	
	B addLoreLines(@NotNull List<Component> lore);
	
	B addLoreLines(@NotNull Component... lore);
	
	B setAmount(int amount);
	
	B setCustomModelData(int data);
	
	B addEnchantment(
		@NotNull Enchantment enchantment,
		int level
	);
	
	B addItemFlags(@NotNull ItemFlag... flags);
	
	B setGlowing(boolean glowing);
	
	ItemStack build();
	
}
