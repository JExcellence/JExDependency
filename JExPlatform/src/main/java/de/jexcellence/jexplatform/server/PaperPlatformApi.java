package de.jexcellence.jexplatform.server;

import com.destroystokyo.paper.profile.ProfileProperty;
import de.jexcellence.jexplatform.logging.JExLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Paper implementation using native Adventure component APIs.
 *
 * <p>On Paper, {@link Player} implements {@link net.kyori.adventure.audience.Audience}
 * directly, and {@link org.bukkit.inventory.meta.ItemMeta} supports Adventure components
 * for display names and lore without serialization.
 *
 * @author JExcellence
 * @since 1.0.0
 */
@SuppressWarnings("UnstableApiUsage")
public non-sealed class PaperPlatformApi implements PlatformApi {

    private static final JExLogger LOG = JExLogger.of("PlatformApi");

    /** The owning plugin, accessible to subclasses. */
    protected final JavaPlugin plugin;

    /**
     * Creates a Paper API bound to the given plugin.
     *
     * @param plugin owning plugin
     */
    PaperPlatformApi(@NotNull JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public void sendMessage(@NotNull Player player, @NotNull Component message) {
        player.sendMessage(message);
    }

    @Override
    public void sendActionBar(@NotNull Player player, @NotNull Component message) {
        player.sendActionBar(message);
    }

    @Override
    public void sendTitle(@NotNull Player player, @NotNull Component title,
                          @NotNull Component subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        var times = Title.Times.times(
                Duration.ofMillis(fadeInTicks * 50L),
                Duration.ofMillis(stayTicks * 50L),
                Duration.ofMillis(fadeOutTicks * 50L)
        );
        player.showTitle(Title.title(title, subtitle, times));
    }

    @Override
    public @NotNull Component playerDisplayName(@NotNull Player player) {
        return player.displayName();
    }

    @Override
    public @NotNull Component itemDisplayName(@NotNull ItemStack item) {
        var meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.displayName();
        }
        return Component.translatable(item.translationKey());
    }

    @Override
    public void setItemDisplayName(@NotNull ItemStack item, @NotNull Component name) {
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            item.setItemMeta(meta);
        }
    }

    @Override
    public @NotNull List<Component> itemLore(@NotNull ItemStack item) {
        var meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return List.of();
        var lore = meta.lore();
        return lore != null ? lore : List.of();
    }

    @Override
    public void setItemLore(@NotNull ItemStack item, @NotNull List<Component> lore) {
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.lore(lore);
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
            var profile = Bukkit.createProfile(owner, "");
            profile.getProperties().add(new ProfileProperty("textures", base64Texture));
            meta.setPlayerProfile(profile);
        } catch (Throwable t) {
            LOG.warn("Failed to apply head texture: {}", t.getMessage());
        }
        head.setItemMeta(meta);
        return head;
    }
}
