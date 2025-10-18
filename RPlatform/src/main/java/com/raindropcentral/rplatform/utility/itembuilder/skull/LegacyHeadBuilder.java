package com.raindropcentral.rplatform.utility.itembuilder.skull;

import com.raindropcentral.rplatform.utility.itembuilder.AItemBuilder;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@SuppressWarnings("deprecation")
public class LegacyHeadBuilder extends AItemBuilder<SkullMeta, LegacyHeadBuilder> implements IHeadBuilder<LegacyHeadBuilder> {
	
	public LegacyHeadBuilder() {
		super(new ItemStack(Material.PLAYER_HEAD, 1, (short) 3));
	}
	
	public LegacyHeadBuilder setPlayerHead(Player player) {
		if (
			player == null
		) {
			return this;
		}
		
		meta.setOwner(player.getName());
		return this;
	}
	
	public LegacyHeadBuilder setPlayerHead(OfflinePlayer offlinePlayer) {
		if (
			offlinePlayer == null
		) {
			return this;
		}
		
		meta.setOwner(offlinePlayer.getName());
		meta.setOwningPlayer(offlinePlayer);
		return this;
	}
	
	public LegacyHeadBuilder setCustomTexture(
		@NotNull UUID uuid,
		@NotNull String textures
	) {
		try {
			String profileName = meta.getOwner();
			if (profileName == null || profileName.trim().isEmpty()) {
				profileName = "CustomHead";
			}
			
			Object profile = createGameProfile(uuid, profileName, textures);
			
			// Try method-based approach first (older versions)
			try {
				meta.getClass()
					.getMethod("setProfile", profile.getClass())
					.invoke(meta, profile);
			} catch (NoSuchMethodException e) {
				// Fallback to field-based approach (newer versions like 1.21.8)
				setProfileViaField(profile);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to set custom texture on legacy head", e);
		}
		return this;
	}
	
	/**
	 * Sets the profile using direct field access (for newer Spigot versions).
	 */
	private void setProfileViaField(Object profile) throws Exception {
		java.lang.reflect.Field profileField = meta.getClass().getDeclaredField("profile");
		profileField.setAccessible(true);
		
		// Check if the field expects ResolvableProfile (1.21.8+) or GameProfile (older)
		Class<?> fieldType = profileField.getType();
		
		if (fieldType.getSimpleName().equals("ResolvableProfile")) {
			// Convert GameProfile to ResolvableProfile for 1.21.8+
			Object resolvableProfile = createResolvableProfile(profile);
			profileField.set(meta, resolvableProfile);
		} else {
			// Use GameProfile directly for older versions
			profileField.set(meta, profile);
		}
	}
	
	/**
	 * Creates a ResolvableProfile from a GameProfile for Minecraft 1.21.8+.
	 */
	private Object createResolvableProfile(Object gameProfile) throws Exception {
		// Try to create ResolvableProfile using reflection
		try {
			Class<?> resolvableProfileClass = Class.forName("net.minecraft.world.item.component.ResolvableProfile");
			
			// Try constructor that takes GameProfile
			try {
				return resolvableProfileClass.getConstructor(gameProfile.getClass()).newInstance(gameProfile);
			} catch (NoSuchMethodException e) {
				// Try static factory method if constructor doesn't exist
				try {
					return resolvableProfileClass.getMethod("of", gameProfile.getClass()).invoke(null, gameProfile);
				} catch (NoSuchMethodException e2) {
					// Try other possible factory methods
					return resolvableProfileClass.getMethod("create", gameProfile.getClass()).invoke(null, gameProfile);
				}
			}
		} catch (Exception e) {
			// If all else fails, try to use the GameProfile directly (might work in some cases)
			throw new RuntimeException("Failed to create ResolvableProfile from GameProfile", e);
		}
	}
	
	private Object createGameProfile(
		@NotNull UUID uuid,
		@NotNull String name,
		@NotNull String texture
	) throws Exception {
		if (name.trim().isEmpty()) {
			name = "CustomHead";
		}
		
		Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
		Class<?> propertyClass    = Class.forName("com.mojang.authlib.properties.Property");
		Object   profile          = gameProfileClass
			                            .getConstructor(UUID.class, String.class)
			                            .newInstance(uuid, name);
		Object   property         = propertyClass
			                            .getConstructor(String.class, String.class)
			                            .newInstance("textures", texture);
		Object   properties       = gameProfileClass
			                            .getMethod("getProperties")
			                            .invoke(profile);
		properties
			.getClass()
			.getMethod("put", Object.class, Object.class)
			.invoke(properties, "textures", property)
		;
		return profile;
	}
	
	@Override
	public ItemStack build() {
		item.setItemMeta(meta);
		return item;
	}
	
}
