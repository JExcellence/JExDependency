/*
package de.jexcellence.oneblock.manager.region;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer;
import org.bukkit.Location;
import org.bukkit.PortalType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

*/
/**
 * Interface for managing nether portals and cross-dimensional teleportation.
 * Handles nether portal creation, safe spawn location finding, and dimensional travel.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 *//*

public interface INetherPortalManager {
    
    */
/**
     * Creates a nether portal for an island at the specified location.
     * 
     * @param island the island to create a portal for
     * @param location the location to create the portal
     * @param portalType the type of portal to create
     * @return CompletableFuture that completes when portal is created
     *//*

    @NotNull
    CompletableFuture<Void> createPortal(@NotNull OneblockIsland island, @NotNull Location location, @NotNull PortalType portalType);
    
    */
/**
     * Finds a safe nether spawn location for an island.
     * 
     * @param island the island to find a spawn location for
     * @return CompletableFuture with optional safe location
     *//*

    @NotNull
    CompletableFuture<Optional<Location>> findSafeNetherSpawn(@NotNull OneblockIsland island);
    
    */
/**
     * Finds a safe end spawn location for an island.
     * 
     * @param island the island to find a spawn location for
     * @return CompletableFuture with optional safe location
     *//*

    @NotNull
    CompletableFuture<Optional<Location>> findSafeEndSpawn(@NotNull OneblockIsland island);
    
    */
/**
     * Handles cross-dimensional teleportation for a player.
     * 
     * @param player the player to teleport
     * @param fromLocation the location the player is teleporting from
     * @param targetDimension the target dimension name
     * @return CompletableFuture that completes when teleportation is done
     *//*

    @NotNull
    CompletableFuture<Void> handleCrossDimensionalTeleport(@NotNull OneblockPlayer player, 
                                                          @NotNull Location fromLocation, 
                                                          @NotNull String targetDimension);
    
    */
/**
     * Gets the corresponding location in another dimension for an island.
     * 
     * @param island the island
     * @param currentLocation the current location
     * @param targetDimension the target dimension
     * @return the corresponding location in the target dimension, or null if not available
     *//*

    @Nullable
    Location getCorrespondingLocation(@NotNull OneblockIsland island, 
                                    @NotNull Location currentLocation, 
                                    @NotNull String targetDimension);
    
    */
/**
     * Checks if a dimension is enabled for an island.
     * 
     * @param island the island to check
     * @param dimension the dimension name
     * @return true if the dimension is enabled
     *//*

    boolean isDimensionEnabled(@NotNull OneblockIsland island, @NotNull String dimension);
    
    */
/**
     * Enables or disables a dimension for an island.
     * 
     * @param island the island
     * @param dimension the dimension name
     * @param enabled true to enable, false to disable
     * @return CompletableFuture that completes when update is done
     *//*

    @NotNull
    CompletableFuture<Void> setDimensionEnabled(@NotNull OneblockIsland island, @NotNull String dimension, boolean enabled);
    
    */
/**
     * Creates the nether region for an island if it doesn't exist.
     * 
     * @param island the island
     * @return CompletableFuture that completes when nether region is created
     *//*

    @NotNull
    CompletableFuture<Void> createNetherRegion(@NotNull OneblockIsland island);
    
    */
/**
     * Creates the end region for an island if it doesn't exist.
     * 
     * @param island the island
     * @return CompletableFuture that completes when end region is created
     *//*

    @NotNull
    CompletableFuture<Void> createEndRegion(@NotNull OneblockIsland island);
    
    */
/**
     * Handles portal creation event from Bukkit.
     * 
     * @param player the player who triggered the portal creation
     * @param location the location where the portal was created
     * @param portalType the type of portal created
     * @return true if the event was handled
     *//*

    boolean handlePortalCreate(@NotNull Player player, @NotNull Location location, @NotNull PortalType portalType);
    
    */
/**
     * Handles player portal use event from Bukkit.
     * 
     * @param player the player using the portal
     * @param fromLocation the location the player is teleporting from
     * @param portalType the type of portal being used
     * @return true if the event was handled
     *//*

    boolean handlePortalUse(@NotNull Player player, @NotNull Location fromLocation, @NotNull PortalType portalType);
    
    */
/**
     * Gets the portal location for an island in a specific dimension.
     * 
     * @param island the island
     * @param dimension the dimension name
     * @return Optional containing the portal location if it exists
     *//*

    @NotNull
    Optional<Location> getPortalLocation(@NotNull OneblockIsland island, @NotNull String dimension);
    
    */
/**
     * Sets the portal location for an island in a specific dimension.
     * 
     * @param island the island
     * @param dimension the dimension name
     * @param location the portal location
     * @return CompletableFuture that completes when update is done
     *//*

    @NotNull
    CompletableFuture<Void> setPortalLocation(@NotNull OneblockIsland island, @NotNull String dimension, @NotNull Location location);
    
    */
/**
     * Removes a portal for an island in a specific dimension.
     * 
     * @param island the island
     * @param dimension the dimension name
     * @return CompletableFuture that completes when portal is removed
     *//*

    @NotNull
    CompletableFuture<Void> removePortal(@NotNull OneblockIsland island, @NotNull String dimension);
    
    */
/**
     * Validates that a portal location is safe and within the island's region.
     * 
     * @param island the island
     * @param location the portal location
     * @param dimension the dimension name
     * @return true if the location is valid for a portal
     *//*

    boolean validatePortalLocation(@NotNull OneblockIsland island, @NotNull Location location, @NotNull String dimension);
    
    */
/**
     * Gets the spawn location for a player in a specific dimension.
     * Considers player permissions and island settings.
     * 
     * @param player the player
     * @param island the island
     * @param dimension the target dimension
     * @return Optional containing the spawn location
     *//*

    @NotNull
    Optional<Location> getPlayerSpawnLocation(@NotNull OneblockPlayer player, @NotNull OneblockIsland island, @NotNull String dimension);
}*/
