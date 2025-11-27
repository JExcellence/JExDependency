package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class RDQPlayerRepository extends GenericCachedRepository<RDQPlayer, Long, UUID> {

    public RDQPlayerRepository(@NotNull ExecutorService executor, @NotNull EntityManagerFactory entityManagerFactory) {
        super(executor, entityManagerFactory, RDQPlayer.class, RDQPlayer::getUniqueId);
    }

    public @NotNull CompletableFuture<Optional<RDQPlayer>> findByUuidAsync(@NotNull UUID uniqueId) {
        return findByCacheKeyAsync("uniqueId", uniqueId)
                .thenApply(Optional::ofNullable);
    }

    public @NotNull CompletableFuture<Optional<RDQPlayer>> findByNameAsync(@NotNull String playerName) {
        return findByAttributesAsync(Map.of("playerName", playerName))
                .thenApply(Optional::ofNullable);
    }

    public @NotNull CompletableFuture<Boolean> existsByUuidAsync(@NotNull UUID uniqueId) {
        return findByUuidAsync(uniqueId)
                .thenApply(Optional::isPresent);
    }

    public @NotNull CompletableFuture<RDQPlayer> createOrUpdateAsync(@NotNull RDQPlayer player) {
        return existsByUuidAsync(player.getUniqueId())
                .thenCompose(exists -> exists ? updateAsync(player) : createAsync(player));
    }
}