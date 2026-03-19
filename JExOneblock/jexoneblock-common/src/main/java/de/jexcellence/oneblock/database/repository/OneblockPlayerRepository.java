package de.jexcellence.oneblock.database.repository;

import de.jexcellence.hibernate.repository.CachedRepository;
import de.jexcellence.hibernate.repository.InjectRepository;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

@InjectRepository
public class OneblockPlayerRepository extends CachedRepository<OneblockPlayer, Long, UUID> {

    public OneblockPlayerRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<OneblockPlayer> entityClass,
            @NotNull Function<OneblockPlayer, UUID> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
    }

    @Nullable
    public OneblockPlayer findByUuid(@NotNull UUID uniqueId) {
        return findByKey("uniqueId", uniqueId).orElse(null);
    }

    @Nullable
    public OneblockPlayer findByPlayerId(@NotNull UUID playerId) {
        return findByUuid(playerId);
    }

    @Nullable
    public OneblockPlayer findByName(@NotNull String playerName) {
        return findByAttributes(Map.of("playerName", playerName)).orElse(null);
    }

    @NotNull
    public List<OneblockPlayer> findAllActive() {
        return findAll().stream()
            .filter(OneblockPlayer::isActive)
            .toList();
    }

    @NotNull
    public List<OneblockPlayer> findAllWithIslands() {
        return findAll().stream()
            .filter(OneblockPlayer::hasIsland)
            .toList();
    }

    @NotNull
    public CompletableFuture<Optional<OneblockPlayer>> findByUuidAsync(@NotNull UUID uniqueId) {
        return findByKeyAsync("uniqueId", uniqueId);
    }

    @NotNull
    public CompletableFuture<Optional<OneblockPlayer>> findByNameAsync(@NotNull String playerName) {
        return CompletableFuture.supplyAsync(
            () -> findByAttributes(Map.of("playerName", playerName)),
            getExecutorService()
        );
    }

    @NotNull
    public CompletableFuture<Boolean> existsByUuidAsync(@NotNull UUID uniqueId) {
        return findByUuidAsync(uniqueId).thenApply(Optional::isPresent);
    }
}
