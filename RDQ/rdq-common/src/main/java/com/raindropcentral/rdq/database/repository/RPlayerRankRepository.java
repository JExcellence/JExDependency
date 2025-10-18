package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRank;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import de.jexcellence.hibernate.entity.AbstractEntity;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class RPlayerRankRepository extends GenericCachedRepository<RPlayerRank, Long, Long> {

    public RPlayerRankRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RPlayerRank.class, AbstractEntity::getId);
    }

    public @NotNull CompletableFuture<Optional<RPlayerRank>> findByPlayerAndRankTreeAsync(
            final @NotNull RDQPlayer player,
            final @NotNull RRankTree rankTree
    ) {
        return findByAttributesAsync(Map.of("player", player, "rankTree", rankTree))
                .thenApply(Optional::ofNullable);
    }

    public @NotNull CompletableFuture<List<RPlayerRank>> findAllByPlayerAsync(final @NotNull RDQPlayer player) {
        return findListByAttributesAsync(Map.of("player", player));
    }

    public @NotNull CompletableFuture<Optional<RPlayerRank>> findActiveByPlayerAsync(final @NotNull RDQPlayer player) {
        return findByAttributesAsync(Map.of("player", player, "isActive", true))
                .thenApply(Optional::ofNullable);
    }
}