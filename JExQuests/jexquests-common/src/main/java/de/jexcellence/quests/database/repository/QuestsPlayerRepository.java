package de.jexcellence.quests.database.repository;

import de.jexcellence.jehibernate.repository.base.AbstractCrudRepository;
import de.jexcellence.quests.database.entity.QuestsPlayer;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for QuestsPlayer entities.
 */
public class QuestsPlayerRepository extends AbstractCrudRepository<QuestsPlayer, Long> {

    /**
     * Constructs a QuestsPlayerRepository.
     */
    public QuestsPlayerRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory emf,
            @NotNull Class<QuestsPlayer> entityClass
    ) {
        super(executor, emf, entityClass);
    }

    /**
     * Finds a QuestsPlayer by UUID.
     */
    public @NotNull Optional<QuestsPlayer> findByUuid(@NotNull UUID uuid) {
        return query().and("uniqueId", uuid).first();
    }

    /**
     * Finds a QuestsPlayer by UUID asynchronously.
     */
    public @NotNull CompletableFuture<Optional<QuestsPlayer>> findByUuidAsync(@NotNull UUID uuid) {
        return query().and("uniqueId", uuid).firstAsync();
    }

    /**
     * Finds or creates a QuestsPlayer by UUID.
     */
    public @NotNull QuestsPlayer findOrCreate(@NotNull UUID uuid) {
        return findByUuid(uuid).orElseGet(() -> create(new QuestsPlayer(uuid)));
    }

    /**
     * Finds or creates a QuestsPlayer by UUID asynchronously.
     */
    public @NotNull CompletableFuture<QuestsPlayer> findOrCreateAsync(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> findOrCreate(uuid));
    }
}
