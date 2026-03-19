/*
package de.jexcellence.oneblock.manager.permission;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer;
import de.jexcellence.oneblock.manager.base.IManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

*/
/**
 * Interface for managing trust relationships between players and islands.
 * Handles trust levels, permissions, and access control.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 *//*

public interface IIslandTrustManager extends IManager {
    
    */
/**
     * Sets the trust level for a player on an island.
     * 
     * @param island the island
     * @param player the player
     * @param trustLevel the trust level to set
     * @return CompletableFuture that completes when trust is set
     *//*

    @NotNull
    CompletableFuture<Void> setTrustLevel(@NotNull OneblockIsland island, 
                                         @NotNull OneblockPlayer player, 
                                         @NotNull TrustLevel trustLevel);
    
    */
/**
     * Gets the trust level for a player on an island.
     * 
     * @param island the island
     * @param player the player
     * @return CompletableFuture with the trust level
     *//*

    @NotNull
    CompletableFuture<TrustLevel> getTrustLevel(@NotNull OneblockIsland island, 
                                               @NotNull OneblockPlayer player);
    
    */
/**
     * Gets the trust level for a player on an island by UUID.
     * 
     * @param island the island
     * @param playerId the player UUID
     * @return CompletableFuture with the trust level
     *//*

    @NotNull
    CompletableFuture<TrustLevel> getTrustLevel(@NotNull OneblockIsland island, 
                                               @NotNull UUID playerId);
    
    */
/**
     * Checks if a player is trusted on an island.
     * 
     * @param island the island
     * @param player the player
     * @return CompletableFuture with true if player is trusted
     *//*

    @NotNull
    CompletableFuture<Boolean> isTrusted(@NotNull OneblockIsland island, 
                                        @NotNull OneblockPlayer player);
    
    */
/**
     * Checks if a player is trusted on an island by UUID.
     * 
     * @param island the island
     * @param playerId the player UUID
     * @return CompletableFuture with true if player is trusted
     *//*

    @NotNull
    CompletableFuture<Boolean> isTrusted(@NotNull OneblockIsland island, 
                                        @NotNull UUID playerId);
    
    */
/**
     * Checks if a player can perform a specific action on an island.
     * 
     * @param island the island
     * @param player the player
     * @param action the action to check
     * @return CompletableFuture with true if action is allowed
     *//*

    @NotNull
    CompletableFuture<Boolean> canPerformAction(@NotNull OneblockIsland island, 
                                               @NotNull OneblockPlayer player, 
                                               @NotNull TrustLevel.TrustAction action);
    
    */
/**
     * Checks if a player can perform a specific action on an island by UUID.
     * 
     * @param island the island
     * @param playerId the player UUID
     * @param action the action to check
     * @return CompletableFuture with true if action is allowed
     *//*

    @NotNull
    CompletableFuture<Boolean> canPerformAction(@NotNull OneblockIsland island, 
                                               @NotNull UUID playerId, 
                                               @NotNull TrustLevel.TrustAction action);
    
    */
/**
     * Removes trust for a player on an island.
     * 
     * @param island the island
     * @param player the player
     * @return CompletableFuture that completes when trust is removed
     *//*

    @NotNull
    CompletableFuture<Void> removeTrust(@NotNull OneblockIsland island, 
                                       @NotNull OneblockPlayer player);
    
    */
/**
     * Gets all trusted players for an island.
     * 
     * @param island the island
     * @return CompletableFuture with list of trusted players
     *//*

    @NotNull
    CompletableFuture<List<OneblockPlayer>> getTrustedPlayers(@NotNull OneblockIsland island);
    
    */
/**
     * Gets all trusted players for an island with a minimum trust level.
     * 
     * @param island the island
     * @param minTrustLevel the minimum trust level
     * @return CompletableFuture with list of trusted players
     *//*

    @NotNull
    CompletableFuture<List<OneblockPlayer>> getTrustedPlayers(@NotNull OneblockIsland island, 
                                                             @NotNull TrustLevel minTrustLevel);
    
    */
/**
     * Gets the number of trusted players for an island.
     * 
     * @param island the island
     * @return CompletableFuture with the count of trusted players
     *//*

    @NotNull
    CompletableFuture<Integer> getTrustedPlayerCount(@NotNull OneblockIsland island);
    
    */
/**
     * Clears all trust relationships for an island.
     * 
     * @param island the island
     * @return CompletableFuture that completes when all trust is cleared
     *//*

    @NotNull
    CompletableFuture<Void> clearAllTrust(@NotNull OneblockIsland island);
    
    */
/**
     * Transfers ownership of an island to another player.
     * 
     * @param island the island
     * @param newOwner the new owner
     * @return CompletableFuture that completes when ownership is transferred
     *//*

    @NotNull
    CompletableFuture<Void> transferOwnership(@NotNull OneblockIsland island, 
                                             @NotNull OneblockPlayer newOwner);
}*/
