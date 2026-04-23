package de.jexcellence.quests.database.repository;

import de.jexcellence.jehibernate.repository.base.AbstractCrudRepository;
import de.jexcellence.quests.database.entity.Machine;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for Machine entities.
 */
public class MachineRepository extends AbstractCrudRepository<Machine, Long> {

    /**
     * Constructs a MachineRepository.
     */
    public MachineRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory emf,
            @NotNull Class<Machine> entityClass
    ) {
        super(executor, emf, entityClass);
    }

    /**
     * Finds a machine by location.
     */
    public @NotNull CompletableFuture<Optional<Machine>> findByLocationAsync(
            @NotNull String world, int x, int y, int z) {
        return query()
                .and("world", world)
                .and("x", x)
                .and("y", y)
                .and("z", z)
                .and("dismantled", false)
                .firstAsync();
    }

    /**
     * Finds machines by owner UUID.
     */
    public @NotNull CompletableFuture<List<Machine>> findByOwnerAsync(@NotNull UUID ownerUuid) {
        return query()
                .and("ownerUuid", ownerUuid)
                .and("dismantled", false)
                .listAsync();
    }

    /**
     * Finds machines by type.
     */
    public @NotNull CompletableFuture<List<Machine>> findByTypeAsync(@NotNull String machineType) {
        return query()
                .and("machineType", machineType)
                .and("dismantled", false)
                .listAsync();
    }

    /**
     * Finds machines by world.
     */
    public @NotNull CompletableFuture<List<Machine>> findByWorldAsync(@NotNull String world) {
        return query()
                .and("world", world)
                .and("dismantled", false)
                .listAsync();
    }
}
