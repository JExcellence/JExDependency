package com.raindropcentral.core.database.repository;


import com.raindropcentral.core.database.entity.player.RPlayer;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class RPlayerRepository extends GenericCachedRepository<RPlayer, Long, UUID> {
    
    public RPlayerRepository(
        final @NotNull ExecutorService executor,
        final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RPlayer.class, RPlayer::getUniqueId);
    }
    
    public CompletableFuture<Optional<RPlayer>> findByUuidAsync(final @NotNull UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        
        return findByAttributesAsync(Map.of("uniqueId", uniqueId))
            .thenApply(Optional::ofNullable);
    }
    
    public CompletableFuture<Optional<RPlayer>> findByNameAsync(final @NotNull String playerName) {
        Objects.requireNonNull(playerName, "playerName cannot be null");
        
        return findByAttributesAsync(Map.of("playerName", playerName))
            .thenApply(Optional::ofNullable);
    }
    
    public CompletableFuture<Boolean> existsByUuidAsync(final @NotNull UUID uniqueId) {
        return findByUuidAsync(uniqueId)
            .thenApply(Optional::isPresent);
    }
    
    public CompletableFuture<RPlayer> createOrUpdateAsync(final @NotNull RPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        
        return existsByUuidAsync(player.getUniqueId())
            .thenCompose(exists -> exists
                ? updateAsync(player)
                : createAsync(player)
            );
    }
}
