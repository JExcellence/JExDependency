/*
package de.jexcellence.oneblock.manager.permission;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer;
import de.jexcellence.oneblock.manager.base.IManager;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

*/
/**
 * Interface for managing permissions on islands.
 * Handles permission levels, specific permission types, and access control validation.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 *//*

public interface IIslandPermissionManager extends IManager {
    
    */
/**
     * Checks if a player has a specific permission on an island.
     * 
     * @param island the island
     * @param player the player
     * @param permissionType the permission type to check
     * @return CompletableFuture with true if player has permission
     *//*

    @NotNull
    CompletableFuture<Boolean> hasPermission(@NotNull OneblockIsland island, 
                                            @NotNull OneblockPlayer player, 
                                            @NotNull PermissionType permissionType);
    
    */
/**
     * Checks if a player has a specific permission on an island by UUID.
     * 
     * @param island the island
     * @param playerId the player UUID
     * @param permissionType the permission type to check
     * @return CompletableFuture with true if player has permission
     *//*

    @NotNull
    CompletableFuture<Boolean> hasPermission(@NotNull OneblockIsland island, 
                                            @NotNull UUID playerId, 
                                            @NotNull PermissionType permissionType);
    
    */
/**
     * Checks if a player has a minimum permission level on an island.
     * 
     * @param island the island
     * @param player the player
     * @param permissionLevel the minimum permission level required
     * @return CompletableFuture with true if player has the required level
     *//*

    @NotNull
    CompletableFuture<Boolean> hasPermissionLevel(@NotNull OneblockIsland island, 
                                                 @NotNull OneblockPlayer player, 
                                                 @NotNull PermissionLevel permissionLevel);
    
    */
/**
     * Checks if a player has a minimum permission level on an island by UUID.
     * 
     * @param island the island
     * @param playerId the player UUID
     * @param permissionLevel the minimum permission level required
     * @return CompletableFuture with true if player has the required level
     *//*

    @NotNull
    CompletableFuture<Boolean> hasPermissionLevel(@NotNull OneblockIsland island, 
                                                 @NotNull UUID playerId, 
                                                 @NotNull PermissionLevel permissionLevel);
    
    */
/**
     * Gets the effective permission level for a player on an island.
     * 
     * @param island the island
     * @param player the player
     * @return CompletableFuture with the player's permission level
     *//*

    @NotNull
    CompletableFuture<PermissionLevel> getPermissionLevel(@NotNull OneblockIsland island, 
                                                         @NotNull OneblockPlayer player);
    
    */
/**
     * Gets the effective permission level for a player on an island by UUID.
     * 
     * @param island the island
     * @param playerId the player UUID
     * @return CompletableFuture with the player's permission level
     *//*

    @NotNull
    CompletableFuture<PermissionLevel> getPermissionLevel(@NotNull OneblockIsland island, 
                                                         @NotNull UUID playerId);
    
    */
/**
     * Gets all permissions that a player has on an island.
     * 
     * @param island the island
     * @param player the player
     * @return CompletableFuture with set of permission types the player has
     *//*

    @NotNull
    CompletableFuture<Set<PermissionType>> getPlayerPermissions(@NotNull OneblockIsland island, 
                                                               @NotNull OneblockPlayer player);
    
    */
/**
     * Sets a custom permission for a player on an island.
     * This overrides the default permission based on trust level.
     * 
     * @param island the island
     * @param player the player
     * @param permissionType the permission type
     * @param allowed true to allow, false to deny
     * @return CompletableFuture that completes when permission is set
     *//*

    @NotNull
    CompletableFuture<Void> setCustomPermission(@NotNull OneblockIsland island, 
                                               @NotNull OneblockPlayer player, 
                                               @NotNull PermissionType permissionType, 
                                               boolean allowed);
    
    */
/**
     * Removes a custom permission for a player on an island.
     * The permission will revert to the default based on trust level.
     * 
     * @param island the island
     * @param player the player
     * @param permissionType the permission type
     * @return CompletableFuture that completes when custom permission is removed
     *//*

    @NotNull
    CompletableFuture<Void> removeCustomPermission(@NotNull OneblockIsland island, 
                                                  @NotNull OneblockPlayer player, 
                                                  @NotNull PermissionType permissionType);
    
    */
/**
     * Gets all custom permissions for a player on an island.
     * 
     * @param island the island
     * @param player the player
     * @return CompletableFuture with map of permission types to their allowed status
     *//*

    @NotNull
    CompletableFuture<Map<PermissionType, Boolean>> getCustomPermissions(@NotNull OneblockIsland island, 
                                                                        @NotNull OneblockPlayer player);
    
    */
/**
     * Clears all custom permissions for a player on an island.
     * 
     * @param island the island
     * @param player the player
     * @return CompletableFuture that completes when all custom permissions are cleared
     *//*

    @NotNull
    CompletableFuture<Void> clearCustomPermissions(@NotNull OneblockIsland island, 
                                                  @NotNull OneblockPlayer player);
    
    */
/**
     * Clears all custom permissions for an island.
     * 
     * @param island the island
     * @return CompletableFuture that completes when all custom permissions are cleared
     *//*

    @NotNull
    CompletableFuture<Void> clearAllCustomPermissions(@NotNull OneblockIsland island);
    
    */
/**
     * Sets the default permission level for visitors on an island.
     * 
     * @param island the island
     * @param permissionLevel the default permission level for visitors
     * @return CompletableFuture that completes when default permission is set
     *//*

    @NotNull
    CompletableFuture<Void> setDefaultVisitorPermission(@NotNull OneblockIsland island, 
                                                        @NotNull PermissionLevel permissionLevel);
    
    */
/**
     * Gets the default permission level for visitors on an island.
     * 
     * @param island the island
     * @return CompletableFuture with the default visitor permission level
     *//*

    @NotNull
    CompletableFuture<PermissionLevel> getDefaultVisitorPermission(@NotNull OneblockIsland island);
    
    */
/**
     * Validates a permission request and provides detailed information about why it was denied.
     * 
     * @param island the island
     * @param player the player
     * @param permissionType the permission type
     * @return CompletableFuture with permission validation result
     *//*

    @NotNull
    CompletableFuture<PermissionValidationResult> validatePermission(@NotNull OneblockIsland island, 
                                                                    @NotNull OneblockPlayer player, 
                                                                    @NotNull PermissionType permissionType);
    
    */
/**
     * Result of permission validation with detailed information.
     *//*

    record PermissionValidationResult(
        boolean allowed,
        PermissionLevel playerLevel,
        PermissionLevel requiredLevel,
        TrustLevel trustLevel,
        boolean hasCustomPermission,
        String reason
    ) {}
}*/
