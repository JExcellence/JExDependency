package de.jexcellence.quests.database.repository;

import de.jexcellence.jehibernate.repository.base.AbstractCrudRepository;
import de.jexcellence.quests.database.entity.PlayerPerk;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for PlayerPerk entities.
 */
public class PlayerPerkRepository extends AbstractCrudRepository<PlayerPerk, Long> {

    /**
     * Constructs a PlayerPerkRepository.
     */
    public PlayerPerkRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory emf,
            @NotNull Class<PlayerPerk> entityClass
    ) {
        super(executor, emf, entityClass);
    }

    /**
     * Finds a player perk by player and perk identifier.
     */
    public @NotNull CompletableFuture<Optional<PlayerPerk>> findAsync(
            @NotNull UUID playerUuid, @NotNull String perkIdentifier) {
        return query()
                .and("playerUuid", playerUuid)
                .and("perkIdentifier", perkIdentifier)
                .firstAsync();
    }

    /**
     * Finds all player perks by player UUID.
     */
    public @NotNull CompletableFuture<List<PlayerPerk>> findByPlayerAsync(@NotNull UUID playerUuid) {
        return query().and("playerUuid", playerUuid).listAsync();
    }

    /**
     * Finds enabled player perks by player UUID.
     */
    public @NotNull CompletableFuture<List<PlayerPerk>> findEnabledByPlayerAsync(@NotNull UUID playerUuid) {
        return query()
                .and("playerUuid", playerUuid)
                .and("enabled", true)
                .listAsync();
    }
}
