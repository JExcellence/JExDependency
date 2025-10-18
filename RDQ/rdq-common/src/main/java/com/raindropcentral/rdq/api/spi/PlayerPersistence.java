package com.raindropcentral.rdq.api.spi;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;

import java.util.concurrent.CompletableFuture;

public interface PlayerPersistence {
    CompletableFuture<Void> updateAsync(RDQPlayer player);
}