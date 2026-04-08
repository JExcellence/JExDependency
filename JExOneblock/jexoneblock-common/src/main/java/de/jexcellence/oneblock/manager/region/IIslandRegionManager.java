/*
package de.jexcellence.oneblock.manager.region;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

*/
/**
 * Interface for managing island regions across multiple dimensions.
 * Handles region creation, expansion, player tracking, and multi-dimensional support.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 *//*

public interface IIslandRegionManager {
    
    */
/**
     * Creates a region for an island at the specified center location.
     * 
     * @param island the island to create a region for
     * @param center the center location of the region
     * @param size the initial size of the region
     * @return CompletableFuture that completes when region is created
     *//*

    @NotNull
    CompletableFuture<Void> createRegion(@NotNull OneblockIsland island, @NotNull Location center, int size);
    
    */
/**
     * Expands an island's region by the specified amount asynchronously.
     * 
     * @param island the island whose region to expand
     * @param additionalSize the additional size to expand by
     * @return CompletableFuture that completes when expansion is done
     *//*

    @NotNull
    CompletableFuture<Void> expandRegion(@NotNull OneblockIsland island, int additionalSize);
    
    */
/**
     * Checks if a location is within an island's region boundaries.
     * 
     * @param island the island to check
     * @param location the location to check
     * @return true if the location is within the region
     *//*

    boolean contains(@NotNull OneblockIsland island, @NotNull Location location);
    
    */
/**
     * Gets all players currently within an island's region.
     * 
     * @param island the island to check
     * @return list of players in the region
     *//*

    @NotNull
    List<Player> getPlayersInRegion(@NotNull OneblockIsland island);
    
    */
/**
     * Gets the bounding box for an island's region in a specific dimension.
     * 
     * @param island the island
     * @param dimension the dimension name (overworld, nether, end)
     * @return the bounding box for the specified dimension, or null if not supported
     *//*

    @Nullable
    BoundingBox getBoundingBox(@NotNull OneblockIsland island, @NotNull String dimension);
    
    */
/**
     * Finds the island region that contains a specific location.
     * 
     * @param location the location to check
     * @return Optional containing the island if found
     *//*

    @NotNull
    CompletableFuture<Optional<OneblockIsland>> findIslandAt(@NotNull Location location);
    
    */
/**
     * Gets all regions that overlap with the specified area.
     * 
     * @param corner1 first corner of the area
     * @param corner2 second corner of the area
     * @return CompletableFuture with list of overlapping islands
     *//*

    @NotNull
    CompletableFuture<List<OneblockIsland>> findOverlappingIslands(@NotNull Location corner1, @NotNull Location corner2);
    
    */
/**
     * Checks if a player is currently within any island region.
     * 
     * @param player the player to check
     * @return Optional containing the island if player is in a region
     *//*

    @NotNull
    Optional<OneblockIsland> getPlayerCurrentIsland(@NotNull OneblockPlayer player);
    
    */
/**
     * Moves an island's region to a new center location.
     * 
     * @param island the island to move
     * @param newCenter the new center location
     * @return CompletableFuture that completes when move is done
     *//*

    @NotNull
    CompletableFuture<Void> moveRegion(@NotNull OneblockIsland island, @NotNull Location newCenter);
    
    */
/**
     * Validates that a region doesn't overlap with existing regions.
     * 
     * @param center the center location
     * @param size the region size
     * @return CompletableFuture with true if location is valid
     *//*

    @NotNull
    CompletableFuture<Boolean> validateRegionLocation(@NotNull Location center, int size);
    
    */
/**
     * Gets the safe spawn location within an island's region.
     * 
     * @param island the island
     * @param isVisitor true if getting visitor spawn location
     * @return the safe spawn location
     *//*

    @NotNull
    Optional<Location> getSafeSpawnLocation(@NotNull OneblockIsland island, boolean isVisitor);
    
    */
/**
     * Updates the spawn location for an island's region.
     * 
     * @param island the island
     * @param newSpawn the new spawn location
     * @param isVisitor true if updating visitor spawn location
     * @return CompletableFuture that completes when update is done
     *//*

    @NotNull
    CompletableFuture<Void> updateSpawnLocation(@NotNull OneblockIsland island, @NotNull Location newSpawn, boolean isVisitor);
    
    */
/**
     * Gets the region entity for an island.
     * 
     * @param island the island
     * @return Optional containing the region entity if found
     *//*

    @NotNull
    Optional<OneblockRegion> getRegionEntity(@NotNull OneblockIsland island);
    
    */
/**
     * Checks if two regions overlap.
     * 
     * @param region1 the first region
     * @param region2 the second region
     * @return true if regions overlap
     *//*

    boolean regionsOverlap(@NotNull OneblockRegion region1, @NotNull OneblockRegion region2);
    
    */
/**
     * Calculates the distance between two regions.
     * 
     * @param region1 the first region
     * @param region2 the second region
     * @return the distance between region centers, or -1 if in different worlds
     *//*

    double calculateRegionDistance(@NotNull OneblockRegion region1, @NotNull OneblockRegion region2);
}*/
