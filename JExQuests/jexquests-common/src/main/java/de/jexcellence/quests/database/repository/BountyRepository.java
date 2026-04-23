package de.jexcellence.quests.database.repository;

import de.jexcellence.jehibernate.repository.base.AbstractCrudRepository;
import de.jexcellence.quests.database.entity.Bounty;
import de.jexcellence.quests.database.entity.BountyStatus;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for Bounty entities.
 */
public class BountyRepository extends AbstractCrudRepository<Bounty, Long> {

    /**
     * Constructs a BountyRepository.
     */
    public BountyRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory emf,
            @NotNull Class<Bounty> entityClass
    ) {
        super(executor, emf, entityClass);
    }

    /**
     * Finds all active bounties.
     */
    public @NotNull CompletableFuture<List<Bounty>> findActiveAsync() {
        return query().and("status", BountyStatus.ACTIVE).orderByDesc("placedAt").listAsync();
    }

    /**
     * Finds an active bounty by target UUID.
     */
    public @NotNull CompletableFuture<Optional<Bounty>> findActiveByTargetAsync(@NotNull UUID targetUuid) {
        return query()
                .and("targetUuid", targetUuid)
                .and("status", BountyStatus.ACTIVE)
                .firstAsync();
    }

    /**
     * Finds bounties by issuer UUID.
     */
    public @NotNull CompletableFuture<List<Bounty>> findByIssuerAsync(@NotNull UUID issuerUuid) {
        return query().and("issuerUuid", issuerUuid).orderByDesc("placedAt").listAsync();
    }

    /**
     * Finds bounties by status.
     */
    public @NotNull CompletableFuture<List<Bounty>> findByStatusAsync(@NotNull BountyStatus status) {
        return query().and("status", status).listAsync();
    }
}
