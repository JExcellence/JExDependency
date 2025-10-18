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

/**
 * Shared abstraction that exposes messaging, item utilities, and scheduling primitives across
 * Spigot, Paper, and Folia server implementations.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public interface PlatformAPI {

    /**
     * Retrieves the type of server implementation the API instance represents.
     *
     * <p><strong>Usage:</strong> Query during plugin start-up to branch into platform-specific
     * configuration or behaviour.</p>
     *
     * @return the detected platform type backing this API instance
     */
    @NotNull PlatformType getType();

    /**
     * Determines whether the underlying server natively supports the Adventure API bridge.
     *
     * <p><strong>Usage:</strong> Use to decide if components can be sent directly without legacy
     * serialization fallbacks.</p>
     *
     * @return {@code true} when Adventure calls are fully supported
     */
    boolean supportsAdventure();

    /**
     * Determines whether the running platform supports Folia-style regionized execution.
     *
     * <p><strong>Usage:</strong> Guard Folia-specific concurrency decisions or scheduler
     * selections.</p>
     *
     * @return {@code true} when Folia features are available
     */
    boolean supportsFolia();

    /**
     * Releases any resources held by the platform implementation.
     *
     * <p><strong>Usage:</strong> Invoke during plugin disable to ensure adapters dispose of
     * listeners or schedulers they own.</p>
     */
    void close();

    /**
     * Sends a single Adventure component to the target player.
     *
     * <p><strong>Usage:</strong> Prefer for ad-hoc messaging where formatting should respect the
     * platform's native capabilities.</p>
     *
     * @param player   the player receiving the message
     * @param message  the component to deliver
     */
    void sendMessage(@NotNull Player player, @NotNull Component message);

    /**
     * Sends multiple Adventure components to the target player in order.
     *
     * <p><strong>Usage:</strong> Use for batched notifications where ordering and platform-specific
     * serialization must be preserved.</p>
     *
     * @param player   the recipient player
     * @param messages the ordered components to dispatch
     */
    void sendMessages(@NotNull Player player, @NotNull List<Component> messages);

    /**
     * Displays an action bar message to the player.
     *
     * <p><strong>Usage:</strong> Call for short-lived status updates; implementations translate to
     * the platform's action bar API.</p>
     *
     * @param player  the recipient player
     * @param message the action bar component to show
     */
    void sendActionBar(@NotNull Player player, @NotNull Component message);

    /**
     * Shows a title and optional subtitle to the player with custom timing.
     *
     * <p><strong>Usage:</strong> Invoke for cinematic notifications; timing arguments map to the
     * platform's expected tick values.</p>
     *
     * @param player       the player to display the title to
     * @param title        the main title component
     * @param subtitle     the optional subtitle component, {@code null} to omit
     * @param fadeInTicks  the fade-in duration in ticks
     * @param stayTicks    the stay duration in ticks
     * @param fadeOutTicks the fade-out duration in ticks
     */
    void sendTitle(@NotNull Player player,
                   @NotNull Component title,
                   @Nullable Component subtitle,
                   int fadeInTicks,
                   int stayTicks,
                   int fadeOutTicks);

    /**
     * Retrieves the Adventure display name for the supplied player.
     *
     * <p><strong>Usage:</strong> Call when synchronizing UI elements to ensure display names honour
     * platform-specific storage.</p>
     *
     * @param player the player whose display name is queried
     * @return the Adventure component representing the player's display name
     */
    @NotNull Component getDisplayName(@NotNull Player player);

    /**
     * Applies the provided Adventure component as the player's display name.
     *
     * <p><strong>Usage:</strong> Invoke when updating UI or scoreboard state so platform-specific
     * mutators are respected.</p>
     *
     * @param player      the player to update
     * @param displayName the new display name component
     */
    void setDisplayName(@NotNull Player player, @NotNull Component displayName);

    /**
     * Reads an item's display name as an Adventure component, if present.
     *
     * <p><strong>Usage:</strong> Use to normalize item metadata handling across platforms with
     * differing component support.</p>
     *
     * @param itemStack the item to inspect
     * @return the display name component or {@code null} when absent
     */
    @Nullable Component getItemDisplayName(@NotNull ItemStack itemStack);

    /**
     * Updates an item's display name using Adventure components.
     *
     * <p><strong>Usage:</strong> Invoke before persisting or giving items so the correct
     * serialization path is chosen per platform.</p>
     *
     * @param itemStack   the item to mutate
     * @param displayName the new display name component, or {@code null} to clear
     * @return the same item stack instance for fluent transformations
     */
    @NotNull ItemStack setItemDisplayName(@NotNull ItemStack itemStack, @Nullable Component displayName);

    /**
     * Retrieves an item's lore as Adventure components.
     *
     * <p><strong>Usage:</strong> Call prior to editing lore lines to work with a platform-neutral
     * representation.</p>
     *
     * @param itemStack the item whose lore should be read
     * @return immutable list of lore components, or an empty list when none are present
     */
    @NotNull List<Component> getItemLore(@NotNull ItemStack itemStack);

    /**
     * Sets an item's lore using Adventure components.
     *
     * <p><strong>Usage:</strong> Apply before distributing items so each platform serializes the
     * lore appropriately.</p>
     *
     * @param itemStack the item to modify
     * @param lore      the lore components to apply
     * @return the same item stack instance for chaining
     */
    @NotNull ItemStack setItemLore(@NotNull ItemStack itemStack, @NotNull List<Component> lore);

    /**
     * Creates a new player head item populated with the provided online player profile.
     *
     * <p><strong>Usage:</strong> Use for runtime-generated heads tied to currently connected
     * players.</p>
     *
     * @param player the source player, or {@code null} to return an unbound head
     * @return a player head item stack
     */
    @NotNull ItemStack createPlayerHead(@Nullable Player player);

    /**
     * Creates a new player head item populated with the provided offline player profile.
     *
     * <p><strong>Usage:</strong> Invoke when the owner may not be online; implementations resolve
     * profile data appropriately.</p>
     *
     * @param offlinePlayer the offline player to associate, or {@code null} for an empty head
     * @return a player head item stack
     */
    @NotNull ItemStack createPlayerHead(@Nullable OfflinePlayer offlinePlayer);

    /**
     * Creates a custom textured player head using the supplied texture data.
     *
     * <p><strong>Usage:</strong> Provide the base64-encoded texture payload when no display name is
     * required.</p>
     *
     * @param uuid        the stable UUID for the generated profile
     * @param textureData the base64 texture payload for the skin
     * @return the generated player head item stack
     */
    @NotNull ItemStack createCustomHead(@NotNull UUID uuid, @NotNull String textureData);

    /**
     * Creates a custom textured player head and assigns an Adventure display name.
     *
     * <p><strong>Usage:</strong> Prefer when generating heads for menus that require labelled
     * entries.</p>
     *
     * @param uuid         the stable UUID for the generated profile
     * @param textureData  the base64 texture payload for the skin
     * @param displayName  optional Adventure component applied as the head's display name
     * @return the generated player head item stack
     */
    @NotNull ItemStack createCustomHead(@NotNull UUID uuid, @NotNull String textureData, @Nullable Component displayName);

    /**
     * Applies a custom texture payload to an existing skull item.
     *
     * <p><strong>Usage:</strong> Use when cloning or mutating an existing item stack in-place while
     * preserving other metadata.</p>
     *
     * @param skull       the skull item to mutate
     * @param uuid        the stable UUID for the generated profile
     * @param textureData the base64 texture payload for the skin
     * @return the same item stack with the applied texture when supported
     */
    @NotNull ItemStack applyCustomTexture(@NotNull ItemStack skull, @NotNull UUID uuid, @NotNull String textureData);

    /**
     * Returns the server version string reported by the underlying platform.
     *
     * <p><strong>Usage:</strong> Useful for diagnostics or logging where the exact Bukkit
     * implementation version is required.</p>
     *
     * @return the server version string
     */
    @NotNull String getServerVersion();

    /**
     * Provides the scheduler adapter bound to this platform implementation.
     *
     * <p><strong>Usage:</strong> Schedule tasks through this adapter instead of directly using the
     * Bukkit scheduler so Folia and Paper integrations are respected.</p>
     *
     * @return the scheduler adapter instance for this platform
     */
    @NotNull ISchedulerAdapter scheduler();
}