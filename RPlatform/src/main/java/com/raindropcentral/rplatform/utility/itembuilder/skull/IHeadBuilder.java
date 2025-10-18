package com.raindropcentral.rplatform.utility.itembuilder.skull;

import com.raindropcentral.rplatform.utility.unified.IUnifiedItemBuilder;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface IHeadBuilder<B extends IHeadBuilder<B>> extends IUnifiedItemBuilder<SkullMeta, B> {
	
	B setPlayerHead(@Nullable Player player);
	
	B setPlayerHead(@Nullable OfflinePlayer offlinePlayer);
	
	B setCustomTexture(
		@NotNull UUID uuid,
		@NotNull String textures
	);
	
}