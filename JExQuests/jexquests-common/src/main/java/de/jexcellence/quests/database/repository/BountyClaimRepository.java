package de.jexcellence.quests.database.repository;

import de.jexcellence.jehibernate.repository.base.AbstractCrudRepository;
import de.jexcellence.quests.database.entity.Bounty;
import de.jexcellence.quests.database.entity.BountyClaim;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for BountyClaim entities.
 */
public class BountyClaimRepository extends AbstractCrudRepository<BountyClaim, Long> {

    /**
     * Constructs a BountyClaimRepository.
     */
    public BountyClaimRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory emf,
            @NotNull Class<BountyClaim> entityClass
    ) {
        super(executor, emf, entityClass);
    }

    /**
     * Finds bounty claims by bounty.
     */
    public @NotNull CompletableFuture<List<BountyClaim>> findByBountyAsync(@NotNull Bounty bounty) {
        return query().and("bounty", bounty).orderByDesc("claimedAt").listAsync();
    }

    /**
     * Finds bounty claims by killer UUID.
     */
    public @NotNull CompletableFuture<List<BountyClaim>> findByKillerAsync(@NotNull UUID killerUuid) {
        return query().and("killerUuid", killerUuid).orderByDesc("claimedAt").listAsync();
    }
}
