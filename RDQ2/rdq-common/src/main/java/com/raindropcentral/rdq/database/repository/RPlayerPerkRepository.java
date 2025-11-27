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

/**
 * Repository responsible for loading and caching {@link RPlayerPerk} entities keyed by their identifier.
 * <p>
 * This repository provides asynchronous helpers that leverage the shared executor to retrieve perk
 * associations for a player without blocking the calling thread.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class RPlayerPerkRepository extends GenericCachedRepository<RPlayerPerk, Long, Long> {

    public RPlayerPerkRepository(
            final @NotNull ExecutorService executor, final @NotNull EntityManagerFactory entityManagerFactory) {
        super(executor, entityManagerFactory, RPlayerPerk.class, AbstractEntity::getId);
    }

    /**
     * Retrieves the {@link RPlayerPerk} mapping for the supplied player and perk asynchronously.
     *
     * @param player the player whose perk association should be inspected
     * @param perk the perk that should be resolved for the player
     * @return a future that resolves to the optional association for the provided player and perk
     */
    public @NotNull CompletableFuture<Optional<RPlayerPerk>> findByPlayerAndPerkAsync(
            final @NotNull RDQPlayer player,
            final @NotNull RPerk perk
    ) {
        return findByAttributesAsync(Map.of("player", player, "perk", perk))
                .thenApply(Optional::ofNullable);
    }
}