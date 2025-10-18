package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.perk.RPerk;
import com.raindropcentral.rdq.database.entity.perk.RPlayerPerk;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.entity.AbstractEntity;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class RPlayerPerkRepository extends GenericCachedRepository<RPlayerPerk, Long, Long> {

    public RPlayerPerkRepository(
            final @NotNull ExecutorService executor, final @NotNull EntityManagerFactory entityManagerFactory) {
        super(executor, entityManagerFactory, RPlayerPerk.class, AbstractEntity::getId);
    }

    public @NotNull CompletableFuture<Optional<RPlayerPerk>> findByPlayerAndPerkAsync(
            final @NotNull RDQPlayer player,
            final @NotNull RPerk perk
    ) {
        return findByAttributesAsync(Map.of("player", player, "perk", perk))
                .thenApply(Optional::ofNullable);
    }
}