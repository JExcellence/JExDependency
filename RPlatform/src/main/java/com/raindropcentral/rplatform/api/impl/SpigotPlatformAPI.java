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
 * Spigot/Bukkit implementation of {@link PlatformAPI} that bridges Adventure components via legacy
 * serialization and applies skull textures through reflection to avoid hard dependencies on
 * Mojang's authlib.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public final class SpigotPlatformAPI implements PlatformAPI {

    /**
     * Legacy serializer used to convert Adventure components to Spigot-compatible strings.
     *
     * <p><strong>Lifecycle:</strong> Cached statically to avoid repeated serializer allocation.</p>
     */
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    /**
     * Owning plugin required for scheduler integration and resource access.
     *
     * <p><strong>Lifecycle:</strong> Captured during construction and retained for the adapter
     * lifetime.</p>
     */
    private final JavaPlugin plugin;

    /**
     * Scheduler adapter wrapping Bukkit's scheduling facilities.
     *
     * <p><strong>Lifecycle:</strong> Created when the adapter is constructed and reused thereafter.</p>
     */
    private final ISchedulerAdapter scheduler;

    /**
     * Creates a Spigot adapter for the provided plugin context.
     *
     * <p><strong>Usage:</strong> Prefer instantiation through the platform factory to ensure the
     * correct implementation is selected for the running server.</p>
     *
     * @param plugin the plugin creating the adapter
     */
    public SpigotPlatformAPI(final @NotNull JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.scheduler = ISchedulerAdapter.create(plugin, PlatformType.SPIGOT);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link PlatformType#SPIGOT}
     */
    @Override
    public @NotNull PlatformType getType() {
        return PlatformType.SPIGOT;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code false} because Spigot requires legacy serialization
     */
    @Override
    public boolean supportsAdventure() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code false} because Folia scheduling is unavailable on Spigot
     */
    @Override
    public boolean supportsFolia() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Currently no resources need explicit cleanup for Spigot.</p>
     */
    @Override
    public void close() {
        // No resources to close for Spigot adapter
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Components are serialized to the legacy format before sending.</p>
     *
     * @param player  the message recipient
     * @param message the component message to serialize
     */
    @Override
    public void sendMessage(final @NotNull Player player, final @NotNull Component message) {
        player.sendMessage(LEGACY.serialize(message));
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Each component is serialized individually to preserve formatting.</p>
     *
     * @param player   the message recipient
     * @param messages list of components to dispatch
     */
    @Override
    public void sendMessages(final @NotNull Player player, final @NotNull List<Component> messages) {
        for (final Component m : messages) {
            player.sendMessage(LEGACY.serialize(m));
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Converts the component into the string expected by Spigot's action
     * bar API.</p>
     *
     * @param player  the action bar recipient
     * @param message the component to display
     */
    @Override
    public void sendActionBar(final @NotNull Player player, final @NotNull Component message) {
        player.sendActionBar(LEGACY.serialize(message));
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Serializes title and subtitle components before invoking Spigot's
     * string-based API.</p>
     *
     * @param player       the target player
     * @param title        the title component
     * @param subtitle     optional subtitle component
     * @param fadeInTicks  fade-in duration
     * @param stayTicks    stay duration
     * @param fadeOutTicks fade-out duration
     */
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

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Converts the player's legacy display name to an Adventure
     * component.</p>
     *
     * @param player the player whose display name is requested
     * @return the Adventure display name
     */
    @Override
    public @NotNull Component getDisplayName(final @NotNull Player player) {
        final String dn = player.getDisplayName();
        return dn != null ? LEGACY.deserialize(dn) : Component.empty();
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Serializes the component before delegating to Spigot's setter.</p>
     *
     * @param player      the player to mutate
     * @param displayName the Adventure component to apply
     */
    @Override
    public void setDisplayName(final @NotNull Player player, final @NotNull Component displayName) {
        player.setDisplayName(LEGACY.serialize(displayName));
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Reads the legacy display name from item meta and converts it back
     * into an Adventure component.</p>
     *
     * @param itemStack the item whose display name is inspected
     * @return optional Adventure display name
     */
    @Override
    public @Nullable Component getItemDisplayName(final @NotNull ItemStack itemStack) {
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return null;
        final String dn = meta.getDisplayName();
        return dn != null ? LEGACY.deserialize(dn) : null;
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Serializes the Adventure component to the string-based Spigot
     * representation.</p>
     *
     * @param itemStack   the item to modify
     * @param displayName the Adventure component to apply or {@code null}
     * @return the supplied item stack
     */
    @Override
    public @NotNull ItemStack setItemDisplayName(final @NotNull ItemStack itemStack, final @Nullable Component displayName) {
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName != null ? LEGACY.serialize(displayName) : null);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Converts each lore line from legacy strings to Adventure
     * components.</p>
     *
     * @param itemStack the item whose lore is read
     * @return immutable list of Adventure lore components
     */
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

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Serializes each Adventure component to maintain compatibility with
     * Spigot's lore storage.</p>
     *
     * @param itemStack the item to mutate
     * @param lore      lore components to apply
     * @return the supplied item stack
     */
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

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Populates a player head using Spigot's owning player field.</p>
     *
     * @param player the source player or {@code null}
     * @return a player head item stack
     */
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

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Associates the skull with the offline player's cached profile.</p>
     *
     * @param offlinePlayer the offline player or {@code null}
     * @return a player head item stack
     */
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

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Convenience overload delegating to the variant with a display
     * name.</p>
     *
     * @param uuid        synthetic profile UUID
     * @param textureData base64 texture payload
     * @return a skull with the texture applied
     */
    @Override
    public @NotNull ItemStack createCustomHead(final @NotNull UUID uuid, final @NotNull String textureData) {
        return createCustomHead(uuid, textureData, null);
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Applies the custom texture via reflection and optionally assigns a
     * legacy display name.</p>
     *
     * @param uuid         synthetic profile UUID
     * @param textureData  base64 texture payload
     * @param displayName  optional Adventure display name
     * @return a skull with the texture applied
     */
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

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Uses reflection to inject the synthetic profile into an existing
     * skull item.</p>
     *
     * @param skull       the skull item to mutate
     * @param uuid        synthetic profile UUID
     * @param textureData base64 texture payload
     * @return the supplied item stack
     */
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
     * Applies a custom texture to a skull using reflection so that authlib remains an optional
     * dependency.
     *
     * <p><strong>Usage:</strong> Internal helper for the public skull methods; silently fails when
     * the reflective calls are unavailable.</p>
     *
     * @param meta        the skull meta to mutate
     * @param uuid        synthetic profile UUID
     * @param textureData base64 texture payload
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

    /**
     * {@inheritDoc}
     *
     * @return Bukkit version string
     */
    @Override
    public @NotNull String getServerVersion() {
        return Bukkit.getVersion();
    }

    /**
     * {@inheritDoc}
     *
     * @return the scheduler adapter leveraging Bukkit's scheduler
     */
    @Override
    public @NotNull ISchedulerAdapter scheduler() {
        return this.scheduler;
    }
}