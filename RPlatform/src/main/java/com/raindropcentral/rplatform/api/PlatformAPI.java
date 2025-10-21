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
 * <p><strong>Threading:</strong> Unless noted otherwise, callers must execute operations from the
 * primary server thread so Bukkit state remains consistent. For asynchronous work schedule the
 * action through {@link #scheduler()} and re-enter the main thread before mutating players or
 * items.</p>
 *
 * <p><strong>Lifecycle:</strong> Instances are resolved during {@code RPlatform#initialize()} via
 * the reflective {@code PlatformAPIFactory}. Downstream modules—most notably {@code RCore} and
 * {@code RDQ}—reuse the same adapter for the lifetime of the plugin and should invoke
 * {@link #close()} during disable to release platform resources.</p>
 *
 * <p><strong>Integration:</strong> The API bridges Adventure messaging, scheduler coordination,
 * and skull utilities so modules authored against the RDC platform can share behaviour without
 * branching on Paper, Folia, or Spigot specifics. Extensions should add default methods instead
 * of breaking existing implementers.</p>
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
     * <p><strong>Error handling:</strong> Implementations should not return {@code null}; callers
     * may treat a {@link NullPointerException} as a fatal misconfiguration.</p>
     *
     * <p><strong>Extension guidance:</strong> Additional platform types must update
     * {@link PlatformType} and provide a compatible implementation before exposing the value
     * through this method.</p>
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
     * <p><strong>Error handling:</strong> Implementations should remain side effect free; callers
     * should guard against {@link UnsupportedOperationException} from legacy platforms.</p>
     *
     * <p><strong>Extension guidance:</strong> Override when adding new rendering bridges so mixed
     * deployments understand the capability matrix.</p>
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
     * <p><strong>Error handling:</strong> Implementations should surface {@link IllegalStateException}
     * if Folia support is misreported; callers must fail fast because downstream logic assumes the
     * return value is authoritative.</p>
     *
     * <p><strong>Extension guidance:</strong> Override when introducing new schedulers that mimic
     * Folia's region model.</p>
     *
     * @return {@code true} when Folia features are available
     */
    boolean supportsFolia();

    /**
     * Releases any resources held by the platform implementation.
     *
     * <p><strong>Usage:</strong> Invoke during plugin disable to ensure adapters dispose of
     * listeners or schedulers they own.</p>
     *
     * <p><strong>Error handling:</strong> Implementations should be idempotent and suppress
     * recoverable exceptions after logging so shutdown never leaves dangling resources.</p>
     *
     * <p><strong>Extension guidance:</strong> Override when adapters create external resources such
     * as executor services or metrics clients.</p>
     */
    void close();

    /**
     * Sends a single Adventure component to the target player.
     *
     * <p><strong>Usage:</strong> Prefer for ad-hoc messaging where formatting should respect the
     * platform's native capabilities.</p>
     *
     * <p><strong>Threading:</strong> Execute on the primary server thread; asynchronous callers must
     * reschedule through {@link #scheduler()}.</p>
     *
     * <p><strong>Error handling:</strong> Propagate serialization failures so calling services can
     * log and abort the user-facing action.</p>
     *
     * <p><strong>Extension guidance:</strong> Maintain player-facing semantics even when adding
     * formatting hooks.</p>
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
     * <p><strong>Threading:</strong> Invoke on the server thread or after synchronizing via the
     * scheduler.</p>
     *
     * <p><strong>Error handling:</strong> Implementations should short-circuit on the first
     * serialization failure and communicate the issue via an unchecked exception.</p>
     *
     * <p><strong>Extension guidance:</strong> Preserve iteration order if overriding to perform
     * batching or pipeline optimisations.</p>
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
     * <p><strong>Threading:</strong> Execute on the main thread.</p>
     *
     * <p><strong>Error handling:</strong> Implementations should degrade gracefully when the action
     * bar feature is absent, typically by logging and throwing {@link UnsupportedOperationException}.</p>
     *
     * <p><strong>Extension guidance:</strong> When providing fallbacks, document whether the action
     * bar displays through chat or titles.</p>
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
     * <p><strong>Threading:</strong> Invoke from the server thread so native title APIs are safe.</p>
     *
     * <p><strong>Error handling:</strong> Implementations should validate tick durations and throw
     * {@link IllegalArgumentException} when values fall outside platform limits.</p>
     *
     * <p><strong>Extension guidance:</strong> New platform implementations should honour tick-based
     * timing even when underlying APIs use durations.</p>
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
     * <p><strong>Threading:</strong> Invoke from the server thread.</p>
     *
     * <p><strong>Error handling:</strong> Implementations may return {@link Component#empty()} when
     * the platform stores {@code null}; callers should not assume non-empty content.</p>
     *
     * <p><strong>Extension guidance:</strong> Preserve custom formatting for nickname providers.</p>
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
     * <p><strong>Threading:</strong> Execute on the main thread.</p>
     *
     * <p><strong>Error handling:</strong> Implementations should validate non-null display names and
     * surface {@link NullPointerException} when contract violations occur.</p>
     *
     * <p><strong>Extension guidance:</strong> If additional formatting hooks are added ensure
     * Adventure components remain the canonical input.</p>
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
     * <p><strong>Threading:</strong> Query item metadata on the server thread to avoid concurrent
     * modification.</p>
     *
     * <p><strong>Error handling:</strong> Implementations should return {@code null} when the
     * display name is unavailable rather than throwing.</p>
     *
     * <p><strong>Extension guidance:</strong> Preserve immutability by avoiding metadata mutation.</p>
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
     * <p><strong>Threading:</strong> Perform metadata mutations on the main thread.</p>
     *
     * <p><strong>Error handling:</strong> Implementations should ignore {@code null} metadata and
     * return the original item rather than failing.</p>
     *
     * <p><strong>Extension guidance:</strong> Always return the provided {@link ItemStack} to keep
     * fluent usage predictable.</p>
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
     * <p><strong>Threading:</strong> Read lore on the server thread.</p>
     *
     * <p><strong>Error handling:</strong> Return an empty list for missing lore rather than
     * throwing.</p>
     *
     * <p><strong>Extension guidance:</strong> Prefer immutable return values to protect callers from
     * accidental modification.</p>
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
     * <p><strong>Threading:</strong> Mutate lore from the server thread only.</p>
     *
     * <p><strong>Error handling:</strong> Implementations should treat {@code null} metadata as a
     * no-op to prevent {@link NullPointerException}s.</p>
     *
     * <p><strong>Extension guidance:</strong> Preserve order and size when transforming lore
     * collections.</p>
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
     * <p><strong>Threading:</strong> Execute on the main thread to satisfy Bukkit inventory rules.</p>
     *
     * <p><strong>Error handling:</strong> Implementations should tolerate {@code null} players by
     * returning an unbound head.</p>
     *
     * <p><strong>Extension guidance:</strong> Avoid caching heads globally to prevent stale
     * profiles.</p>
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
     * <p><strong>Threading:</strong> Execute on the main thread.</p>
     *
     * <p><strong>Error handling:</strong> Implementations should return an empty head when profile
     * data is unavailable.</p>
     *
     * <p><strong>Extension guidance:</strong> Preserve backwards compatibility with legacy profile
     * lookups.</p>
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
     * <p><strong>Threading:</strong> Invoke on the main thread.</p>
     *
     * <p><strong>Error handling:</strong> Implementations should validate inputs and throw
     * {@link IllegalArgumentException} for malformed payloads.</p>
     *
     * <p><strong>Extension guidance:</strong> Reuse {@link #applyCustomTexture(ItemStack, UUID, String)}
     * to avoid duplicating reflection logic.</p>
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
     * <p><strong>Threading:</strong> Invoke on the main thread.</p>
     *
     * <p><strong>Error handling:</strong> Implementations should treat {@code null} display names as
     * a request to omit the label.</p>
     *
     * <p><strong>Extension guidance:</strong> Delegate to the simpler overload for texture
     * application consistency.</p>
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
     * <p><strong>Threading:</strong> Invoke from the main thread.</p>
     *
     * <p><strong>Error handling:</strong> Implementations should return the original stack unchanged
     * when textures cannot be applied.</p>
     *
     * <p><strong>Extension guidance:</strong> Share logic with the creation helpers to keep texture
     * semantics aligned.</p>
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
     * <p><strong>Error handling:</strong> Implementations should never return {@code null}; callers
     * may log and proceed when the string is empty.</p>
     *
     * <p><strong>Extension guidance:</strong> Keep the format consistent with Bukkit's
     * {@code getVersion()} for compatibility with existing diagnostics.</p>
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
     * <p><strong>Error handling:</strong> Implementations should return a fully initialised adapter
     * and throw {@link IllegalStateException} when lifecycle ordering prevents scheduler creation.</p>
     *
     * <p><strong>Extension guidance:</strong> Wrap platform-native schedulers to maintain
     * consistent semantics.</p>
     *
     * @return the scheduler adapter instance for this platform
     */
    @NotNull ISchedulerAdapter scheduler();
}