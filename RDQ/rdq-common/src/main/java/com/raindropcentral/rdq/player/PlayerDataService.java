package com.raindropcentral.rdq.player;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerDataService {

    CompletableFuture<RDQPlayer> getOrCreatePlayer(@NotNull UUID playerId, @NotNull String playerName);

    CompletableFuture<Optional<RDQPlayer>> getPlayer(@NotNull UUID playerId);

    CompletableFuture<Void> savePlayer(@NotNull RDQPlayer player);

    CompletableFuture<Void> updateLastSeen(@NotNull UUID playerId);

    void cleanupPlayerCache(@NotNull UUID playerId);
}
