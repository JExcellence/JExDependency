package de.jexcellence.core.service;

import de.jexcellence.core.database.entity.CorePlayer;
import de.jexcellence.core.database.repository.CorePlayerRepository;
import de.jexcellence.jexplatform.logging.JExLogger;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service facade over {@link CorePlayerRepository}. Registered on the Bukkit
 * services manager so other plugins can resolve player records without
 * reaching into the persistence layer.
 */
public class CorePlayerService {

    private final CorePlayerRepository repo;
    private final JExLogger logger;

    public CorePlayerService(@NotNull CorePlayerRepository repo, @NotNull JExLogger logger) {
        this.repo = repo;
        this.logger = logger;
    }

    public @NotNull CompletableFuture<Optional<CorePlayer>> findByUuid(@NotNull UUID uuid) {
        return this.repo.findByUuidAsync(uuid).exceptionally(ex -> {
            this.logger.error("findByUuid failed for {}: {}", uuid, ex.getMessage());
            return Optional.empty();
        });
    }

    public @NotNull CompletableFuture<Optional<CorePlayer>> findByName(@NotNull String name) {
        return this.repo.findByNameAsync(name).exceptionally(ex -> {
            this.logger.error("findByName failed for {}: {}", name, ex.getMessage());
            return Optional.empty();
        });
    }

    public @NotNull CompletableFuture<CorePlayer> track(@NotNull UUID uuid, @NotNull String name) {
        return this.repo.findOrCreateAsync(uuid, name).exceptionally(ex -> {
            this.logger.error("track failed for {}/{}: {}", uuid, name, ex.getMessage());
            return null;
        });
    }

    public @NotNull CompletableFuture<Void> markSeen(@NotNull UUID uuid) {
        return this.repo.findByUuidAsync(uuid)
                .thenAccept(opt -> opt.ifPresent(player -> {
                    player.setLastSeen(LocalDateTime.now());
                    this.repo.update(player);
                }))
                .exceptionally(ex -> {
                    this.logger.error("markSeen failed for {}: {}", uuid, ex.getMessage());
                    return null;
                });
    }

    public @NotNull CorePlayerRepository repository() {
        return this.repo;
    }
}
