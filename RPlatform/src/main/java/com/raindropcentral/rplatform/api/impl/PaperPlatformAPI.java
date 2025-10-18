package com.raindropcentral.rplatform.api.impl;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.raindropcentral.rplatform.api.PlatformAPI;
import com.raindropcentral.rplatform.api.PlatformType;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public final class PaperPlatformAPI implements PlatformAPI {

    private final JavaPlugin plugin;
    private final ISchedulerAdapter scheduler;

    public PaperPlatformAPI(final @NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = ISchedulerAdapter.create(plugin, PlatformType.PAPER);
    }

    @Override
    public @NotNull PlatformType getType() {
        return PlatformType.PAPER;
    }

    @Override
    public boolean supportsAdventure() {
        return true;
    }

    @Override
    public boolean supportsFolia() {
        return false;
    }

    @Override
    public void close() {
        // No-op for Paper
    }

    @Override
    public void sendMessage(final @NotNull Player player, final @NotNull Component message) {
        player.sendMessage(message);
    }

    @Override
    public void sendMessages(final @NotNull Player player, final @NotNull List<Component> messages) {
        for (final Component message : messages) {
            player.sendMessage(message);
        }
    }

    @Override
    public void sendActionBar(final @NotNull Player player, final @NotNull Component message) {
        player.sendActionBar(message);
    }

    @Override
    public void sendTitle(final @NotNull Player player,
                          final @NotNull Component title,
                          final @Nullable Component subtitle,
                          final int fadeInTicks,
                          final int stayTicks,
                          final int fadeOutTicks) {
        final Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeInTicks * 50L),
                Duration.ofMillis(stayTicks * 50L),
                Duration.ofMillis(fadeOutTicks * 50L)
        );
        player.showTitle(Title.title(title, subtitle != null ? subtitle : Component.empty(), times));
    }

    @Override
    public @NotNull Component getDisplayName(final @NotNull Player player) {
        return player.displayName();
    }

    @Override
    public void setDisplayName(final @NotNull Player player, final @NotNull Component displayName) {
        player.displayName(displayName);
    }

    @Override
    public @Nullable Component getItemDisplayName(final @NotNull ItemStack itemStack) {
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;
        return meta.displayName();
    }

    @Override
    public @NotNull ItemStack setItemDisplayName(final @NotNull ItemStack itemStack, final @Nullable Component displayName) {
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.displayName(displayName);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    @Override
    public @NotNull List<Component> getItemLore(final @NotNull ItemStack itemStack) {
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return List.of();
        final List<Component> lore = meta.lore();
        return lore != null ? lore : List.of();
    }

    @Override
    public @NotNull ItemStack setItemLore(final @NotNull ItemStack itemStack, final @NotNull List<Component> lore) {
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.lore(lore);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    @Override
    public @NotNull ItemStack createPlayerHead(final @Nullable Player player) {
        final ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (player == null) return head;
        final SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setPlayerProfile(player.getPlayerProfile());
            head.setItemMeta(meta);
        }
        return head;
    }

    @Override
    public @NotNull ItemStack createPlayerHead(final @Nullable OfflinePlayer offlinePlayer) {
        final ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (offlinePlayer == null) return head;
        final SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setPlayerProfile(offlinePlayer.getPlayerProfile());
            meta.setOwningPlayer(offlinePlayer);
            head.setItemMeta(meta);
        }
        return head;
    }

    @Override
    public @NotNull ItemStack createCustomHead(final @NotNull UUID uuid, final @NotNull String textureData) {
        return createCustomHead(uuid, textureData, null);
    }

    @Override
    public @NotNull ItemStack createCustomHead(final @NotNull UUID uuid, final @NotNull String textureData, final @Nullable Component displayName) {
        final ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        final SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            final PlayerProfile profile = Bukkit.createProfile(uuid, null);
            profile.setProperty(new ProfileProperty("textures", textureData, null));
            meta.setPlayerProfile(profile);
            if (displayName != null) {
                meta.displayName(displayName);
            }
            head.setItemMeta(meta);
        }
        return head;
    }

    @Override
    public @NotNull ItemStack applyCustomTexture(final @NotNull ItemStack skull, final @NotNull UUID uuid, final @NotNull String textureData) {
        if (skull.getType() != Material.PLAYER_HEAD) {
            return skull;
        }
        final SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            final PlayerProfile profile = Bukkit.createProfile(uuid, null);
            profile.setProperty(new ProfileProperty("textures", textureData, null));
            meta.setPlayerProfile(profile);
            skull.setItemMeta(meta);
        }
        return skull;
    }

    @Override
    public @NotNull String getServerVersion() {
        return Bukkit.getVersion();
    }

    @Override
    public @NotNull ISchedulerAdapter scheduler() {
        return this.scheduler;
    }
}