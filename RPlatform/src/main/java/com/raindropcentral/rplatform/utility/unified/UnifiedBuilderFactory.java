package com.raindropcentral.rplatform.utility.unified;

import com.raindropcentral.rplatform.utility.itembuilder.LegacyItemBuilder;
import com.raindropcentral.rplatform.utility.itembuilder.ModernItemBuilder;
import com.raindropcentral.rplatform.utility.itembuilder.potion.IPotionBuilder;
import com.raindropcentral.rplatform.utility.itembuilder.potion.LegacyPotionBuilder;
import com.raindropcentral.rplatform.utility.itembuilder.potion.ModernPotionBuilder;
import com.raindropcentral.rplatform.utility.itembuilder.skull.*;
import com.raindropcentral.rplatform.version.ServerEnvironment;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Factory for unified item builders.
 * Provides automatic version detection for head builders.
 */
public class UnifiedBuilderFactory {
	
	/**
	 * Returns the appropriate head builder (modern or legacy) based on server platform.
	 * 
	 * @deprecated Use {@link #unifiedHead()} or {@link #safeHead()} for better cross-platform compatibility
	 */
	@Deprecated
	public static IHeadBuilder<?> head() {
		if (ServerEnvironment.getInstance().isPaper()) {
			return new ModernHeadBuilder();
		} else {
			return new LegacyHeadBuilder();
		}
	}
	
	/**
	 * Returns a safe head builder that automatically detects platform capabilities.
	 * This builder prevents NoSuchMethodError by using reflection and fallbacks.
	 *
	 * @return a new SafeHeadBuilder
	 */
	@NotNull
	public static SafeHeadBuilder safeHead() {
		return new SafeHeadBuilder();
	}
	
	/**
	 * Returns a unified head builder that works across all platforms.
	 * This is the recommended way to create player heads.
	 *
	 * @return a new UnifiedHeadBuilder
	 */
	@NotNull
	public static UnifiedHeadBuilder unifiedHead() {
		return new UnifiedHeadBuilder();
	}
	
	/**
	 * Returns a unified head builder for a specific player.
	 *
	 * @param player the player whose head to create
	 * @return a new UnifiedHeadBuilder with the player's head
	 */
	@NotNull
	public static UnifiedHeadBuilder unifiedHead(@Nullable Player player) {
		return new UnifiedHeadBuilder(player);
	}
	
	/**
	 * Returns a unified head builder for a specific offline player.
	 *
	 * @param offlinePlayer the offline player whose head to create
	 * @return a new UnifiedHeadBuilder with the offline player's head
	 */
	@NotNull
	public static UnifiedHeadBuilder unifiedHead(@Nullable OfflinePlayer offlinePlayer) {
		return new UnifiedHeadBuilder(offlinePlayer);
	}
	
	/**
	 * Returns a unified head builder for a custom textured head.
	 *
	 * @param uuid the UUID to associate with the head
	 * @param textureData the base64 encoded texture data
	 * @return a new UnifiedHeadBuilder with the custom texture
	 */
	@NotNull
	public static UnifiedHeadBuilder unifiedHead(@NotNull UUID uuid, @NotNull String textureData) {
		return new UnifiedHeadBuilder(uuid, textureData);
	}
	
	/**
	 * Returns a unified head builder for a custom textured head with a random UUID.
	 *
	 * @param textureData the base64 encoded texture data
	 * @return a new UnifiedHeadBuilder with the custom texture
	 */
	@NotNull
	public static UnifiedHeadBuilder unifiedHead(@NotNull String textureData) {
		return new UnifiedHeadBuilder(UUID.randomUUID(), textureData);
	}
	
	/**
	 * Returns the appropriate potion builder (modern or legacy) based on server version.
	 */
	public static IPotionBuilder<?> potion() {
        if (ServerEnvironment.getInstance().isPaper()) {
			return new ModernPotionBuilder();
		} else {
			return new LegacyPotionBuilder();
		}
	}
	
	/**
	 * Returns the appropriate item builder (modern or legacy) based on server version.
	 */
	public static IUnifiedItemBuilder<?, ?> item(
		final @NotNull Material material
	) {
        if (ServerEnvironment.getInstance().isPaper()) {
			return new ModernItemBuilder(material);
		} else {
			return new LegacyItemBuilder(material);
		}
	}
	
	public static IUnifiedItemBuilder<?, ?> item(
		final @NotNull ItemStack item
	) {
        if (ServerEnvironment.getInstance().isPaper()) {
			return new ModernItemBuilder(item);
		} else {
			return new LegacyItemBuilder(item);
		}
	}
	
}