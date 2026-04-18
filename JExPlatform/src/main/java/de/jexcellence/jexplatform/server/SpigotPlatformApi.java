package de.jexcellence.jexplatform.server;

import de.jexcellence.jexplatform.logging.JExLogger;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Spigot implementation bridging Adventure through {@link BukkitAudiences}.
 *
 * <p>Spigot does not natively support Adventure components, so messaging
 * goes through the adventure-platform-bukkit bridge and item metadata uses
 * {@link LegacyComponentSerializer} for conversion.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class SpigotPlatformApi implements PlatformApi {

    private static final JExLogger LOG = JExLogger.of("PlatformApi");
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final JavaPlugin plugin;
    private final BukkitAudiences audiences;

    /**
     * Creates a Spigot API with a BukkitAudiences bridge for Adventure support.
     *
     * @param plugin owning plugin
     */
    SpigotPlatformApi(@NotNull JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.audiences = BukkitAudiences.create(plugin);
    }

    @Override
    public void sendMessage(@NotNull Player player, @NotNull Component message) {
        audiences.player(player).sendMessage(message);
    }

    @Override
    public void sendActionBar(@NotNull Player player, @NotNull Component message) {
        audiences.player(player).sendActionBar(message);
    }

    @Override
    public void sendTitle(@NotNull Player player, @NotNull Component title,
                          @NotNull Component subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        var times = Title.Times.times(
                Duration.ofMillis(fadeInTicks * 50L),
                Duration.ofMillis(stayTicks * 50L),
                Duration.ofMillis(fadeOutTicks * 50L)
        );
        audiences.player(player).showTitle(Title.title(title, subtitle, times));
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull Component playerDisplayName(@NotNull Player player) {
        return LEGACY.deserialize(player.getDisplayName());
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull Component itemDisplayName(@NotNull ItemStack item) {
        var meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return LEGACY.deserialize(meta.getDisplayName());
        }
        return Component.text(formatMaterial(item.getType()));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setItemDisplayName(@NotNull ItemStack item, @NotNull Component name) {
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(LEGACY.serialize(name));
            item.setItemMeta(meta);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull List<Component> itemLore(@NotNull ItemStack item) {
        var meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return List.of();
        var lore = meta.getLore();
        if (lore == null) return List.of();
        return lore.stream().<Component>map(LEGACY::deserialize).toList();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setItemLore(@NotNull ItemStack item, @NotNull List<Component> lore) {
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(lore.stream().map(LEGACY::serialize).toList());
            item.setItemMeta(meta);
        }
    }

    @Override
    public @NotNull ItemStack createPlayerHead(@NotNull OfflinePlayer player) {
        var head = new ItemStack(Material.PLAYER_HEAD);
        var meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        head.setItemMeta(meta);
        return head;
    }

    @Override
    public @NotNull ItemStack createTexturedHead(@NotNull UUID owner, @NotNull String base64Texture) {
        var head = new ItemStack(Material.PLAYER_HEAD);
        var meta = (SkullMeta) head.getItemMeta();
        try {
            var profileClass = Class.forName("com.mojang.authlib.GameProfile");
            var propertyClass = Class.forName("com.mojang.authlib.properties.Property");

            var profile = profileClass.getConstructor(UUID.class, String.class)
                    .newInstance(owner, "");
            var properties = profileClass.getMethod("getProperties").invoke(profile);
            var putMethod = properties.getClass().getMethod("put", Object.class, Object.class);
            var property = propertyClass.getConstructor(String.class, String.class)
                    .newInstance("textures", base64Texture);
            putMethod.invoke(properties, "textures", property);

            var profileField = findField(meta.getClass(), "profile");
            if (profileField != null) {
                profileField.setAccessible(true);
                profileField.set(meta, profile);
            }
        } catch (ReflectiveOperationException e) {
            LOG.warn("Failed to apply head texture via reflection: {}", e.getMessage());
        }
        head.setItemMeta(meta);
        return head;
    }

    /**
     * Closes the BukkitAudiences bridge. Call during plugin shutdown.
     */
    void close() {
        audiences.close();
    }

    // ── Internal ────────────────────────────────────────────────────────────────

    private static String formatMaterial(@NotNull Material material) {
        return material.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
    }

    private static Field findField(Class<?> clazz, String name) {
        for (var current = clazz; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // walk up hierarchy
            }
        }
        return null;
    }
}
