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
 * Folia-specific {@link PlatformAPI} implementation that takes advantage of the Adventure-native
 * APIs and regionized scheduler semantics available on Folia servers.
 *
 * <p><strong>Threading:</strong> Folia enforces regionized execution; callers should schedule work
 * through {@link #scheduler()} when operating outside the player or global regions required by the
 * Folia API. All public methods follow the {@link PlatformAPI} expectation of main-thread or
 * region-thread invocation.</p>
 *
 * <p><strong>Lifecycle:</strong> Instantiated during platform initialization after Folia detection
 * succeeds. Remains valid until plugin disable, at which point {@link #close()} is called (currently
 * a no-op).</p>
 *
 * <p><strong>Integration:</strong> Provides the Folia-specific messaging and skull handling bridge
 * consumed by RCore, RDQ, and any Folia-aware modules.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public final class FoliaPlatformAPI implements PlatformAPI {

    /**
     * Owning plugin reference used for scheduler creation and context-aware services.
     *
     * <p><strong>Lifecycle:</strong> Captured during construction and retained for the lifetime of
     * the API instance.</p>
     *
     * <p><strong>Nullability:</strong> Guaranteed non-null by constructor validation.</p>
     */
    private final JavaPlugin plugin;

    /**
     * Scheduler adapter bound to Folia's regionized execution model.
     *
     * <p><strong>Lifecycle:</strong> Created eagerly during construction and reused for all
     * scheduling operations.</p>
     *
     * <p><strong>Visibility:</strong> Accessed through {@link #scheduler()} and always initialised.</p>
     */
    private final ISchedulerAdapter scheduler;

    /**
     * Creates a new Folia platform adapter for the supplied plugin.
     *
     * <p><strong>Usage:</strong> Instantiate via {@link com.raindropcentral.rplatform.api.PlatformAPIFactory}
     * to ensure detection logic selects this implementation only when Folia classes exist.</p>
     *
     * <p><strong>Error handling:</strong> Throws {@link NullPointerException} when {@code plugin} is
     * {@code null}; scheduler creation issues propagate so operators can identify configuration
     * problems.</p>
     *
     * @param plugin the plugin creating the API instance
     */
    public FoliaPlatformAPI(final @NotNull JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.scheduler = ISchedulerAdapter.create(plugin, PlatformType.FOLIA);
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Always returns {@link PlatformType#FOLIA} for Folia deployments.</p>
     *
     * @return the Folia platform type
     */
    @Override
    public @NotNull PlatformType getType() {
        return PlatformType.FOLIA;
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Folia ships the Adventure bridge, so callers may send Adventure
     * components without legacy fallbacks.</p>
     *
     * @return {@code true} because Folia exposes Adventure natively
     */
    @Override
    public boolean supportsAdventure() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Always {@code true}; use to toggle Folia-specific scheduling
     * strategies.</p>
     *
     * @return {@code true}
     */
    @Override
    public boolean supportsFolia() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Folia implementation does not allocate additional resources, so
     * the method currently performs no work.</p>
     */
    @Override
    public void close() {
        // No-op
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Delegates directly to {@link Player#sendMessage(Component)} with no
     * conversion.</p>
     *
     * @param player  the recipient player
     * @param message the component message to send
     */
    @Override
    public void sendMessage(final @NotNull Player player, final @NotNull Component message) {
        player.sendMessage(message);
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Iterates through the provided components and sends each via the
     * Adventure-native API.</p>
     *
     * @param player   the recipient player
     * @param messages ordered list of messages to dispatch
     */
    @Override
    public void sendMessages(final @NotNull Player player, final @NotNull List<Component> messages) {
        for (final Component message : messages) {
            player.sendMessage(message);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Uses {@link Player#sendActionBar(Component)} which is fully
     * supported on Folia.</p>
     *
     * @param player  the action bar recipient
     * @param message the message to display on the action bar
     */
    @Override
    public void sendActionBar(final @NotNull Player player, final @NotNull Component message) {
        player.sendActionBar(message);
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Converts tick timings to milliseconds for Folia's title API before
     * issuing the call.</p>
     *
     * @param player       the player to show the title to
     * @param title        the title component
     * @param subtitle     the optional subtitle component
     * @param fadeInTicks  the fade-in duration in ticks
     * @param stayTicks    the stay duration in ticks
     * @param fadeOutTicks the fade-out duration in ticks
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
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Returns the Adventure display name exposed by Folia players.</p>
     *
     * @param player the player whose name is queried
     * @return the Adventure display name component
     */
    @Override
    public @NotNull Component getDisplayName(final @NotNull Player player) {
        return player.displayName();
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Delegates to {@link Player#displayName(Component)} to ensure the
     * new name propagates through Folia's Adventure-aware APIs.</p>
     *
     * @param player      the player to mutate
     * @param displayName the new display name
     */
    @Override
    public void setDisplayName(final @NotNull Player player, final @NotNull Component displayName) {
        player.displayName(displayName);
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Reads the component-backed name stored within Folia item meta.</p>
     *
     * @param itemStack the item being inspected
     * @return the display name component or {@code null}
     */
    @Override
    public @Nullable Component getItemDisplayName(final @NotNull ItemStack itemStack) {
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;
        return meta.displayName();
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Applies the Adventure component directly since Folia item meta
     * natively supports it.</p>
     *
     * @param itemStack   the item to modify
     * @param displayName the component to apply or {@code null}
     * @return the provided item stack for chaining
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
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Provides immutable Adventure lore or an empty list when the item
     * lacks lore.</p>
     *
     * @param itemStack the item whose lore should be retrieved
     * @return list of lore components
     */
    @Override
    public @NotNull List<Component> getItemLore(final @NotNull ItemStack itemStack) {
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return List.of();
        final List<Component> lore = meta.lore();
        return lore != null ? lore : List.of();
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Writes Adventure components directly into the Folia item meta.</p>
     *
     * @param itemStack the item to update
     * @param lore      lore components to persist
     * @return the provided item stack for chaining
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
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Generates a head bound to the current player's profile, retaining
     * Adventure metadata.</p>
     *
     * @param player the source player, may be {@code null}
     * @return a player head item stack
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
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Uses Bukkit's offline profile support to populate the head without
     * requiring the player to be online.</p>
     *
     * @param offlinePlayer the offline player profile or {@code null}
     * @return a player head item stack
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
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Delegates to the overload that accepts an optional display name for
     * convenience.</p>
     *
     * @param uuid        the generated profile UUID
     * @param textureData the base64 texture payload
     * @return the created head item stack
     */
    @Override
    public @NotNull ItemStack createCustomHead(final @NotNull UUID uuid, final @NotNull String textureData) {
        return createCustomHead(uuid, textureData, null);
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Builds a synthetic profile with the provided texture data and
     * optionally applies a display name.</p>
     *
     * @param uuid         the generated profile UUID
     * @param textureData  the base64 texture payload
     * @param displayName  optional Adventure display name
     * @return the generated head item stack
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
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Applies the supplied texture onto an existing skull, replacing the
     * underlying profile metadata.</p>
     *
     * @param skull       the skull item to modify
     * @param uuid        the generated profile UUID
     * @param textureData the base64 texture payload
     * @return the mutated skull item stack
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
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Returns the Folia server version as reported by Bukkit.</p>
     *
     * @return server version string
     */
    @Override
    public @NotNull String getServerVersion() {
        return Bukkit.getVersion();
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Usage:</strong> Provides the Folia-aware scheduler for plugin task orchestration.</p>
     *
     * @return the scheduler adapter instance
     */
    @Override
    public @NotNull ISchedulerAdapter scheduler() {
        return this.scheduler;
    }
}