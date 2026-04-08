package de.jexcellence.multiverse.api;

import de.jexcellence.multiverse.database.entity.MVWorld;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * API interface for external plugins to interact with JExMultiverse.
 * <p>
 * This adapter provides methods for:
 * <ul>
 *   <li>Retrieving world information</li>
 *   <li>Checking spawn configuration</li>
 *   <li>Teleporting players to spawn locations</li>
 * </ul>
 * </p>
 * <p>
 * All methods return {@link CompletableFuture} for async operations.
 * External plugins can obtain this adapter via Bukkit's service provider:
 * <pre>{@code
 * RegisteredServiceProvider<IMultiverseAdapter> provider =
 *     Bukkit.getServicesManager().getRegistration(IMultiverseAdapter.class);
 * if (provider != null) {
 *     IMultiverseAdapter adapter = provider.getProvider();
 *     // Use adapter...
 * }
 * }</pre>
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @see MVWorld
 */
public interface IMultiverseAdapter {

    /**
     * Gets the world designated as the global spawn location.
     * <p>
     * Only one world can be the global spawn at a time. If no world
     * is configured as global spawn, an empty Optional is returned.
     * </p>
     *
     * @return a CompletableFuture containing an Optional with the global spawn world
     */
    @NotNull
    CompletableFuture<Optional<MVWorld>> getGlobalMVWorld();

    /**
     * Gets a managed world by its name/identifier.
     *
     * @param worldName the world identifier (name)
     * @return a CompletableFuture containing an Optional with the world if found
     */
    @NotNull
    CompletableFuture<Optional<MVWorld>> getMVWorld(@NotNull String worldName);

    /**
     * Checks if a world has a multiverse spawn configured.
     * <p>
     * A world has a multiverse spawn if:
     * <ul>
     *   <li>It is managed by JExMultiverse, OR</li>
     *   <li>A global spawn world is configured</li>
     * </ul>
     * </p>
     *
     * @param worldName the world identifier to check
     * @return a CompletableFuture containing true if a spawn is configured
     */
    @NotNull
    CompletableFuture<Boolean> hasMultiverseSpawn(@NotNull String worldName);

    /**
     * Teleports a player to the appropriate spawn location.
     * <p>
     * Spawn location priority:
     * <ol>
     *   <li>Global spawn world (if configured)</li>
     *   <li>Current world's spawn (if managed by JExMultiverse)</li>
     *   <li>Default world spawn (fallback)</li>
     * </ol>
     * </p>
     * <p>
     * The messageKey parameter is used to send a localized message to the player
     * upon successful teleportation. Pass null to skip the message.
     * </p>
     *
     * @param player     the player to teleport
     * @param messageKey the i18n message key to send on success, or null for no message
     * @return a CompletableFuture containing true if teleportation was successful
     */
    @NotNull
    CompletableFuture<Boolean> spawn(@NotNull Player player, @NotNull String messageKey);
}
