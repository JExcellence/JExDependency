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

public class AItemBuilder<T extends ItemMeta, B extends IUnifiedItemBuilder<T, B>> {
	
	protected ItemStack item;
	protected T         meta;
	
	protected AItemBuilder(@NotNull Material material) {
		this(new ItemStack(material));
	}
	
	protected AItemBuilder(@NotNull ItemStack item) {
		this.item = item;
		
		//noinspection unchecked
		this.meta = (T) item.getItemMeta();
	}
	
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
	
	@SuppressWarnings("unchecked")
	public B addLoreLine(@NotNull Component line) {
		List<Component> currentLore = getCurrentLore();
		currentLore.add(line);
		setLore(currentLore);
		return (B) this;
	}
	
	public B addLoreLines(@NotNull List<Component> lore) {
		List<Component> currentLore = getCurrentLore();
		currentLore.addAll(lore);
		setLore(currentLore);
		return (B) this;
	}
	
	public B addLoreLines(@NotNull Component... lore) {
		List<Component> currentLore = getCurrentLore();
		currentLore.addAll(Arrays.asList(lore));
		setLore(currentLore);
		return (B) this;
	}
	
	/**
	 * Sets the display name using legacy methods for Spigot/Bukkit compatibility.
	 */
	private void setNameLegacy(@NotNull Component name) {
		String legacyName = LegacyComponentSerializer.legacySection().serialize(name);
		meta.setDisplayName(legacyName);
	}
	
	/**
	 * Sets the lore using legacy methods for Spigot/Bukkit compatibility.
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
	
	@SuppressWarnings("unchecked")
	public B setAmount(int amount) {
		item.setAmount(amount);
		return (B) this;
	}
	
	@SuppressWarnings("unchecked")
	public B setCustomModelData(int data) {
		meta.setCustomModelData(data);
		return (B) this;
	}
	
	@SuppressWarnings("unchecked")
	public B addEnchantment(
		@NotNull Enchantment enchantment,
		int level
	) {
		meta.addEnchant(enchantment, level, true);
		return (B) this;
	}
	
	@SuppressWarnings("unchecked")
	public B addItemFlags(@NotNull ItemFlag... flags) {
		meta.addItemFlags(flags);
		return (B) this;
	}
	
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
	
	public ItemStack build() {
		item.setItemMeta(meta);
		return item;
	}
	
}