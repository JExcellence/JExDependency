/*
package com.raindropcentral.rdq2.player;

import com.raindropcentral.rdq2.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq2.shared.CacheManager;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public final class DefaultPlayerDataService implements PlayerDataService {

    private static final Logger LOGGER = Logger.getLogger(DefaultPlayerDataService.class.getName());

    private final PlayerRepository repository;
    private final CacheManager cacheManager;

    public DefaultPlayerDataService(@NotNull PlayerRepository repository, @NotNull CacheManager cacheManager) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.cacheManager = Objects.requireNonNull(cacheManager, "cacheManager");
    }

    @Override
    @NotNull
    public CompletableFuture<RDQPlayer> getOrCreatePlayer(@NotNull UUID playerId, @NotNull String playerName) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(playerName, "playerName");

        // TODO: Implement proper player data service with correct RDQPlayer API
        var cached = cacheManager.getPlayer(playerId);
        if (cached != null) {
            if (!cached.getPlayerName().equals(playerName)) {
                cached.setPlayerName(playerName);
                // TODO: Add lastSeen field to RDQPlayer entity
                // cached.setLastSeen(java.time.Instant.now());
                return repository.save(cached).thenApply(saved -> {
                    cacheManager.putPlayer(saved);
                    return saved;
                });
            }
            // TODO: Add lastSeen field to RDQPlayer entity
            // cached.setLastSeen(java.time.Instant.now());
            return CompletableFuture.completedFuture(cached);
        }

        return repository.findById(playerId)
            .thenCompose(optPlayer -> {
                if (optPlayer.isPresent()) {
                    var player = optPlayer.get();
                    if (!player.getPlayerName().equals(playerName)) {
                        player.setPlayerName(playerName);
                    }
                    // TODO: Add lastSeen field to RDQPlayer entity
                    // player.setLastSeen(java.time.Instant.now());
                    return repository.save(player).thenApply(saved -> {
                        cacheManager.putPlayer(saved);
                        LOGGER.fine("Loaded existing player: " + saved.getPlayerName());
                        return saved;
                    });
                } else {
                    var newPlayer = new RDQPlayer(playerId, playerName);
                    return repository.save(newPlayer).thenApply(saved -> {
                        cacheManager.putPlayer(saved);
                        LOGGER.info("Created new player: " + saved.getPlayerName());
                        return saved;
                    });
                }
            });
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<RDQPlayer>> getPlayer(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");

        var cached = cacheManager.getPlayer(playerId);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }

        return repository.findById(playerId)
            .thenApply(optPlayer -> {
                optPlayer.ifPresent(cacheManager::putPlayer);
                return optPlayer;
            });
    }

    @Override
    @NotNull
    public CompletableFuture<Void> savePlayer(@NotNull RDQPlayer player) {
        Objects.requireNonNull(player, "player");

        return repository.save(player)
            .thenAccept(saved -> {
                cacheManager.putPlayer(saved);
                LOGGER.fine("Saved player: " + saved.getPlayerName());
            });
    }

    @Override
    @NotNull
    public CompletableFuture<Void> updateLastSeen(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");

        var cached = cacheManager.getPlayer(playerId);
        if (cached != null) {
            // TODO: Add lastSeen field to RDQPlayer entity
            // cached.setLastSeen(java.time.Instant.now());
            return repository.save(cached).thenAccept(cacheManager::putPlayer);
        }

        return repository.findById(playerId)
            .thenCompose(optPlayer -> {
                if (optPlayer.isPresent()) {
                    var player = optPlayer.get();
                    // TODO: Add lastSeen field to RDQPlayer entity
                    // player.setLastSeen(java.time.Instant.now());
                    return repository.save(player).thenAccept(cacheManager::putPlayer);
                }
                return CompletableFuture.completedFuture(null);
            });
    }

    @Override
    public void cleanupPlayerCache(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        cacheManager.cleanupPlayer(playerId);
        LOGGER.fine("Cleaned up cache for player: " + playerId);
    }

    @NotNull
    public CompletableFuture<Long> getPlayerCount() {
        return repository.count();
    }

    @NotNull
    public CompletableFuture<Boolean> playerExists(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");

        var cached = cacheManager.getPlayer(playerId);
        if (cached != null) {
            return CompletableFuture.completedFuture(true);
        }

        return repository.exists(playerId);
    }

    @NotNull
    public CompletableFuture<Optional<RDQPlayer>> getPlayerByName(@NotNull String name) {
        Objects.requireNonNull(name, "name");
        return repository.findByName(name);
    }
}
*/
