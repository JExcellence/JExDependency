package com.raindropcentral.rplatform.api;

import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface PlatformAPI {

    @NotNull PlatformType getType();

    boolean supportsAdventure();

    boolean supportsFolia();

    void close();

    void sendMessage(@NotNull Player player, @NotNull Component message);

    void sendMessages(@NotNull Player player, @NotNull List<Component> messages);

    void sendActionBar(@NotNull Player player, @NotNull Component message);

    void sendTitle(@NotNull Player player,
                   @NotNull Component title,
                   @Nullable Component subtitle,
                   int fadeInTicks,
                   int stayTicks,
                   int fadeOutTicks);

    @NotNull Component getDisplayName(@NotNull Player player);

    void setDisplayName(@NotNull Player player, @NotNull Component displayName);

    @Nullable Component getItemDisplayName(@NotNull ItemStack itemStack);

    @NotNull ItemStack setItemDisplayName(@NotNull ItemStack itemStack, @Nullable Component displayName);

    @NotNull List<Component> getItemLore(@NotNull ItemStack itemStack);

    @NotNull ItemStack setItemLore(@NotNull ItemStack itemStack, @NotNull List<Component> lore);

    @NotNull ItemStack createPlayerHead(@Nullable Player player);

    @NotNull ItemStack createPlayerHead(@Nullable OfflinePlayer offlinePlayer);

    @NotNull ItemStack createCustomHead(@NotNull UUID uuid, @NotNull String textureData);

    @NotNull ItemStack createCustomHead(@NotNull UUID uuid, @NotNull String textureData, @Nullable Component displayName);

    @NotNull ItemStack applyCustomTexture(@NotNull ItemStack skull, @NotNull UUID uuid, @NotNull String textureData);

    @NotNull String getServerVersion();

    @NotNull ISchedulerAdapter scheduler();
}