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
import java.util.Objects;
import java.util.UUID;

/**
 * Paper {@link PlatformAPI} implementation leveraging the platform's Adventure and profile APIs.
 * while delegating scheduling to the shared adapter.
 *
 * <p><strong>Threading:</strong> Public callers must follow the {@link PlatformAPI} guidance by
 * executing on the main thread unless explicitly re-entering via {@link #scheduler()}.</p>
 *
 * <p><strong>Lifecycle:</strong> Instantiated during platform initialization and retained until
 * plugin disable when {@link #close()} is invoked (currently a no-op).</p>
 *
 * <p><strong>Integration:</strong> Coordinates Paper-specific Adventure operations and profile
 * handling so RCore, RDQ, and other modules can target a unified API without bespoke branches.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public final class PaperPlatformAPI implements PlatformAPI {

    /**
     * Owning plugin context captured for scheduler creation and resource lookups.
     *
     * <p><strong>Lifecycle:</strong> Captured during construction and retained for the adapter's
     * lifetime.</p>
     *
     * <p><strong>Nullability:</strong> Always non-null; the constructor enforces this contract.</p>
     */
    private final JavaPlugin plugin;

    /**
     * Scheduler adapter configured for Paper servers.
     *
     * <p><strong>Lifecycle:</strong> Created eagerly during construction and reused for all
     * scheduling requests.</p>
     *
     * <p><strong>Visibility:</strong> Private field exposed through {@link #scheduler()} and never
     * {@code null} once the adapter is constructed.</p>
     */
    private final ISchedulerAdapter scheduler;

    /**
     * Creates a Paper adapter tied to the provided plugin.
     *
     * <p><strong>Usage:</strong> Typically instantiated via the platform factory to ensure the
     * correct implementation is selected.</p>
     *
     * <p><strong>Error handling:</strong> Throws {@link NullPointerException} when {@code plugin} is
     * {@code null}; scheduler creation issues propagate so deployment problems surface early.</p>
     *
     * @param plugin the owning plugin
     */
    public PaperPlatformAPI(final @NotNull JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.scheduler = ISchedulerAdapter.create(plugin, PlatformType.PAPER);
    }

    /**
     * {@inheritDoc}.
     *
     * <p><strong>Usage:</strong> Identifies this adapter as {@link PlatformType#PAPER}.</p>
     *
     * @return the Paper platform type
     */
    @Override
    public @NotNull PlatformType getType() {
        return PlatformType.PAPER;
    }

    /**
     * {@inheritDoc}.
     *
     * <p><strong>Usage:</strong> Returns {@code true} because Paper fully supports Adventure.</p>
     *
     * @return {@code true}
     */
    @Override
    public boolean supportsAdventure() {
        return true;
    }

    /**
     * {@inheritDoc}.
     *
     * <p><strong>Usage:</strong> Paper lacks Folia's regionized scheduler so this remains
     * {@code false}.</p>
     *
     * @return {@code false}
     */
    @Override
    public boolean supportsFolia() {
        return false;
    }

    /**
     * {@inheritDoc}.
     *
     * <p><strong>Usage:</strong> Currently a no-op since Paper requires no explicit shutdown.</p>
     */
    @Override
    public void close() {
        // No-op for Paper
    }

    /**
     * {@inheritDoc}.
     *
     * @param player  the message recipient
     * @param message the Adventure component to deliver
     */
    @Override
    public void sendMessage(final @NotNull Player player, final @NotNull Component message) {
        player.sendMessage(message);
    }

    /**
     * {@inheritDoc}.
     *
     * @param player   the message recipient
     * @param messages ordered list of components to send
     */
    @Override
    public void sendMessages(final @NotNull Player player, final @NotNull List<Component> messages) {
        for (final Component message : messages) {
            player.sendMessage(message);
        }
    }

    /**
     * {@inheritDoc}.
     *
     * @param player  the action bar recipient
     * @param message the component to display
     */
    @Override
    public void sendActionBar(final @NotNull Player player, final @NotNull Component message) {
        player.sendActionBar(message);
    }

    /**
     * {@inheritDoc}.
     *
     * @param player       the target player
     * @param title        title component
     * @param subtitle     optional subtitle component
     * @param fadeInTicks  fade-in ticks
     * @param stayTicks    stay duration in ticks
     * @param fadeOutTicks fade-out ticks
     */
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

    /**
     * {@inheritDoc}.
     *
     * @param player the player whose name is requested
     * @return the Adventure display name
     */
    @Override
    public @NotNull Component getDisplayName(final @NotNull Player player) {
        return player.displayName();
    }

    /**
     * {@inheritDoc}.
     *
     * @param player      the player to mutate
     * @param displayName the new display name component
     */
    @Override
    public void setDisplayName(final @NotNull Player player, final @NotNull Component displayName) {
        player.displayName(displayName);
    }

    /**
     * {@inheritDoc}.
     *
     * @param itemStack the item to inspect
     * @return optional display name component
     */
    @Override
    public @Nullable Component getItemDisplayName(final @NotNull ItemStack itemStack) {
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;
        return meta.displayName();
    }

    /**
     * {@inheritDoc}.
     *
     * @param itemStack   the item to modify
     * @param displayName the component to apply or {@code null}
     * @return the supplied item stack
     */
    @Override
    public @NotNull ItemStack setItemDisplayName(final @NotNull ItemStack itemStack, final @Nullable Component displayName) {
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.displayName(displayName);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    /**
     * {@inheritDoc}.
     *
     * @param itemStack the item whose lore is read
     * @return immutable list of lore components
     */
    @Override
    public @NotNull List<Component> getItemLore(final @NotNull ItemStack itemStack) {
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return List.of();
        final List<Component> lore = meta.lore();
        return lore != null ? lore : List.of();
    }

    /**
     * {@inheritDoc}.
     *
     * @param itemStack the item to mutate
     * @param lore      lore components to apply
     * @return the supplied item stack
     */
    @Override
    public @NotNull ItemStack setItemLore(final @NotNull ItemStack itemStack, final @NotNull List<Component> lore) {
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.lore(lore);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    /**
     * {@inheritDoc}.
     *
     * @param player the source player or {@code null}
     * @return a new player head item stack
     */
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

    /**
     * {@inheritDoc}.
     *
     * @param offlinePlayer the offline profile or {@code null}
     * @return a new player head item stack
     */
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

    /**
     * {@inheritDoc}.
     *
     * @param uuid        profile UUID used for the synthetic head
     * @param textureData base64 texture payload
     * @return a player head with the texture applied
     */
    @Override
    public @NotNull ItemStack createCustomHead(final @NotNull UUID uuid, final @NotNull String textureData) {
        return createCustomHead(uuid, textureData, null);
    }

    /**
     * {@inheritDoc}.
     *
     * @param uuid         profile UUID used for the synthetic head
     * @param textureData  base64 texture payload
     * @param displayName  optional Adventure display name
     * @return a player head with the texture applied
     */
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

    /**
     * {@inheritDoc}.
     *
     * @param skull       the skull to mutate
     * @param uuid        profile UUID used for the texture
     * @param textureData base64 texture payload
     * @return the supplied item stack
     */
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

    /**
     * {@inheritDoc}.
     *
     * @return Bukkit-provided version string
     */
    @Override
    public @NotNull String getServerVersion() {
        return Bukkit.getVersion();
    }

    /**
     * {@inheritDoc}.
     *
     * @return Paper scheduler adapter
     */
    @Override
    public @NotNull ISchedulerAdapter scheduler() {
        return this.scheduler;
    }
}
