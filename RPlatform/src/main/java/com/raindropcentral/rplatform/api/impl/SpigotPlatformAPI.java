package com.raindropcentral.rplatform.api.impl;

import com.raindropcentral.rplatform.api.PlatformAPI;
import com.raindropcentral.rplatform.api.PlatformType;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Spigot/Bukkit PlatformAPI implementation.
 * - Uses legacy serializers to bridge Adventure components.
 * - Applies skull textures via reflection (no direct authlib compile dependency).
 * - Delegates scheduling to ISchedulerAdapter (Bukkit scheduler underneath).
 */
public final class SpigotPlatformAPI implements PlatformAPI {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final JavaPlugin plugin;
    private final ISchedulerAdapter scheduler;

    public SpigotPlatformAPI(final @NotNull JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.scheduler = ISchedulerAdapter.create(plugin, PlatformType.SPIGOT);
    }

    @Override
    public @NotNull PlatformType getType() {
        return PlatformType.SPIGOT;
    }

    @Override
    public boolean supportsAdventure() {
        return false;
    }

    @Override
    public boolean supportsFolia() {
        return false;
    }

    @Override
    public void close() {
        // No resources to close for Spigot adapter
    }

    // ===== Messaging / Titles =====

    @Override
    public void sendMessage(final @NotNull Player player, final @NotNull Component message) {
        player.sendMessage(LEGACY.serialize(message));
    }

    @Override
    public void sendMessages(final @NotNull Player player, final @NotNull List<Component> messages) {
        for (final Component m : messages) {
            player.sendMessage(LEGACY.serialize(m));
        }
    }

    @Override
    public void sendActionBar(final @NotNull Player player, final @NotNull Component message) {
        // Spigot exposes String-based action bar
        player.sendActionBar(LEGACY.serialize(message));
    }

    @Override
    public void sendTitle(final @NotNull Player player,
                          final @NotNull Component title,
                          final @Nullable Component subtitle,
                          final int fadeInTicks,
                          final int stayTicks,
                          final int fadeOutTicks) {
        player.sendTitle(
                LEGACY.serialize(title),
                subtitle != null ? LEGACY.serialize(subtitle) : "",
                fadeInTicks,
                stayTicks,
                fadeOutTicks
        );
    }

    @Override
    public @NotNull Component getDisplayName(final @NotNull Player player) {
        final String dn = player.getDisplayName();
        return dn != null ? LEGACY.deserialize(dn) : Component.empty();
    }

    @Override
    public void setDisplayName(final @NotNull Player player, final @NotNull Component displayName) {
        player.setDisplayName(LEGACY.serialize(displayName));
    }

    // ===== Item meta helpers =====

    @Override
    public @Nullable Component getItemDisplayName(final @NotNull ItemStack itemStack) {
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return null;
        final String dn = meta.getDisplayName();
        return dn != null ? LEGACY.deserialize(dn) : null;
    }

    @Override
    public @NotNull ItemStack setItemDisplayName(final @NotNull ItemStack itemStack, final @Nullable Component displayName) {
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName != null ? LEGACY.serialize(displayName) : null);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    @Override
    public @NotNull List<Component> getItemLore(final @NotNull ItemStack itemStack) {
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta == null || !meta.hasLore()) return List.of();
        final List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return List.of();
        final List<Component> out = new ArrayList<>(lore.size());
        for (final String line : lore) {
            out.add(LEGACY.deserialize(line));
        }
        return out;
    }

    @Override
    public @NotNull ItemStack setItemLore(final @NotNull ItemStack itemStack, final @NotNull List<Component> lore) {
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            final List<String> out = new ArrayList<>(lore.size());
            for (final Component c : lore) {
                out.add(LEGACY.serialize(c));
            }
            meta.setLore(out);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    // ===== Heads / Skulls =====

    @Override
    public @NotNull ItemStack createPlayerHead(final @Nullable Player player) {
        final ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (player == null) return head;
        final SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
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
    public @NotNull ItemStack createCustomHead(final @NotNull UUID uuid,
                                               final @NotNull String textureData,
                                               final @Nullable Component displayName) {
        final ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        final SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            if (displayName != null) {
                meta.setDisplayName(LEGACY.serialize(displayName));
            }
            applyCustomTextureReflective(meta, uuid, textureData);
            head.setItemMeta(meta);
        }
        return head;
    }

    @Override
    public @NotNull ItemStack applyCustomTexture(final @NotNull ItemStack skull,
                                                 final @NotNull UUID uuid,
                                                 final @NotNull String textureData) {
        if (skull.getType() != Material.PLAYER_HEAD) return skull;
        final SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            applyCustomTextureReflective(meta, uuid, textureData);
            skull.setItemMeta(meta);
        }
        return skull;
    }

    /**
     * Apply custom textures to a skull using reflection so we don't depend on authlib at compile time.
     * textureData should be the base64 "textures" value (same as Paper's ProfileProperty usage).
     */
    private static void applyCustomTextureReflective(final @NotNull SkullMeta meta,
                                                     final @NotNull UUID uuid,
                                                     final @NotNull String textureData) {
        try {
            // Class lookup
            final Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            final Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");

            // new GameProfile(UUID, String)
            final Constructor<?> gpCtor = gameProfileClass.getConstructor(UUID.class, String.class);
            final Object profile = gpCtor.newInstance(uuid, null);

            // new Property("textures", textureData)
            final Constructor<?> propCtor = propertyClass.getConstructor(String.class, String.class);
            final Object texturesProperty = propCtor.newInstance("textures", textureData);

            // profile.getProperties().put("textures", property)
            final Method getProps = gameProfileClass.getMethod("getProperties");
            final Object props = getProps.invoke(profile);
            final Method putMethod = props.getClass().getMethod("put", Object.class, Object.class);
            putMethod.invoke(props, "textures", texturesProperty);

            // Set private field "profile" on SkullMeta
            final Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (Throwable ignored) {
            // Silently ignore; skull remains without custom texture
        }
    }

    // ===== Server info / Scheduler =====

    @Override
    public @NotNull String getServerVersion() {
        return Bukkit.getVersion();
    }

    @Override
    public @NotNull ISchedulerAdapter scheduler() {
        return this.scheduler;
    }
}