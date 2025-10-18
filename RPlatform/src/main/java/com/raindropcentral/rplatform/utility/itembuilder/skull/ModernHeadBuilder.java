package com.raindropcentral.rplatform.utility.itembuilder.skull;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.raindropcentral.rplatform.utility.itembuilder.AItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ModernHeadBuilder extends AItemBuilder<SkullMeta, ModernHeadBuilder> implements IHeadBuilder<ModernHeadBuilder> {
	
	public ModernHeadBuilder() {
		super(Material.PLAYER_HEAD);
	}
	
	@Override
	public ModernHeadBuilder setPlayerHead(Player player) {
		if (
			player == null
		) {
			return this;
		}
		
		meta.setPlayerProfile(player.getPlayerProfile());
		return this;
	}
	
	@Override
	public ModernHeadBuilder setPlayerHead(OfflinePlayer offlinePlayer) {
		if (
			offlinePlayer == null
		) {
			return this;
		}
		
		meta.setPlayerProfile(offlinePlayer.getPlayerProfile());
		meta.setOwningPlayer(offlinePlayer);
		return this;
	}
	
	@Override
	public ModernHeadBuilder setCustomTexture(
		@NotNull UUID uuid,
		@NotNull String textures
	) {
		PlayerProfile profile = Bukkit.createProfile(uuid, null);
		profile.setProperty(new ProfileProperty("textures", textures, null));
		meta.setPlayerProfile(profile);
		return this;
	}
	
	@Override
	public ItemStack build() {
		item.setItemMeta(meta);
		return item;
	}
	
}