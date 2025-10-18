package com.raindropcentral.core.api;

import com.raindropcentral.core.database.entity.player.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Cross-module abstraction implemented by the Free/Premium runtime.
 * This decouples the common adapter from variant-specific RCoreImpl classes.
 */
public interface RCoreBackend {

    @NotNull ExecutorService getExecutor();

    @NotNull CompletableFuture<Optional<RPlayer>> findByUuidAsync(@NotNull UUID uniqueId);

    @NotNull CompletableFuture<Optional<RPlayer>> findByNameAsync(@NotNull String playerName);

    /**
     * Persist a new player; returns the persisted instance.
     */
    @NotNull CompletableFuture<RPlayer> createAsync(@NotNull RPlayer player);

    /**
     * Persist updates for a player; returns the updated instance.
     */
    @NotNull CompletableFuture<RPlayer> updateAsync(@NotNull RPlayer player);
}