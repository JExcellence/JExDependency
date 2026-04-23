package de.jexcellence.core.database.repository;

import de.jexcellence.core.database.entity.CorePlayer;
import de.jexcellence.jehibernate.repository.base.AbstractCrudRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for {@link CorePlayer} entities. Looks rows up by UUID or name
 * and supports a find-or-create helper driven by the join flow.
 */
public class CorePlayerRepository extends AbstractCrudRepository<CorePlayer, Long> {

    public CorePlayerRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory emf,
            @NotNull Class<CorePlayer> entityClass
    ) {
        super(executor, emf, entityClass);
    }

    public @NotNull Optional<CorePlayer> findByUuid(@NotNull UUID uuid) {
        return query().and("uniqueId", uuid).first();
    }

    public @NotNull CompletableFuture<Optional<CorePlayer>> findByUuidAsync(@NotNull UUID uuid) {
        return query().and("uniqueId", uuid).firstAsync();
    }

    public @NotNull CompletableFuture<Optional<CorePlayer>> findByNameAsync(@NotNull String playerName) {
        return query().and("playerName", playerName).firstAsync();
    }

    public @NotNull CompletableFuture<Boolean> existsByUuidAsync(@NotNull UUID uuid) {
        return findByUuidAsync(uuid).thenApply(Optional::isPresent);
    }

    public @NotNull CorePlayer findOrCreate(@NotNull UUID uuid, @NotNull String playerName) {
        final Optional<CorePlayer> existing = findByUuid(uuid);
        if (existing.isPresent()) {
            final CorePlayer player = existing.get();
            boolean dirty = false;
            if (!player.getPlayerName().equals(playerName)) {
                player.setPlayerName(playerName);
                dirty = true;
            }
            player.setLastSeen(LocalDateTime.now());
            return dirty ? update(player) : update(player);
        }
        return create(new CorePlayer(uuid, playerName));
    }

    public @NotNull CompletableFuture<CorePlayer> findOrCreateAsync(@NotNull UUID uuid, @NotNull String playerName) {
        return CompletableFuture.supplyAsync(() -> findOrCreate(uuid, playerName));
    }
}
