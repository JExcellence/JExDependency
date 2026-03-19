package de.jexcellence.oneblock.service;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IOneblockService {

    boolean isPremium();

    @NotNull
    CompletableFuture<Void> createIsland(@NotNull Player player);

    @NotNull
    CompletableFuture<Boolean> deleteIsland(@NotNull UUID playerId);

    @NotNull
    CompletableFuture<Boolean> teleportToIsland(@NotNull Player player);

    @NotNull
    CompletableFuture<Integer> getBlockLevel(@NotNull UUID playerId);
    
    @org.jetbrains.annotations.Nullable
    Long getPlayerIslandId(@NotNull Player player);
    
    @org.jetbrains.annotations.Nullable
    String getPlayerIslandIdentifier(@NotNull Player player);
    
    @org.jetbrains.annotations.Nullable
    Long getIslandIdAtLocation(@NotNull org.bukkit.Location location);
    
    boolean isOneBlockLocation(@NotNull org.bukkit.Location location);
    
    void showIslandInfo(@NotNull Player player);
    void showIslandLevel(@NotNull Player player);
    void showTopIslands(@NotNull Player player);
    
    void handlePrestige(@NotNull Player player);
    void setIslandHome(@NotNull Player player);
    void deleteIsland(@NotNull Player player);
    
    void handleInvite(@NotNull Player player, @NotNull String[] args);
    void handleAcceptInvite(@NotNull Player player, @NotNull String[] args);
    void handleDenyInvite(@NotNull Player player, @NotNull String[] args);
    void handleKick(@NotNull Player player, @NotNull String[] args);
    void handleBan(@NotNull Player player, @NotNull String[] args);
    void handleUnban(@NotNull Player player, @NotNull String[] args);
    void handleLeave(@NotNull Player player);
    
    void showStorageInfo(@NotNull Player player);
}
