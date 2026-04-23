package de.jexcellence.quests.service;

import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.quests.database.entity.QuestsPlayer;
import de.jexcellence.quests.database.repository.QuestsPlayerRepository;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Facade over {@link QuestsPlayerRepository}. Used by the join
 * listener and by other services that need per-player JExQuests state.
 */
public class QuestsPlayerService {

    private final QuestsPlayerRepository repo;
    private final JExLogger logger;

    public QuestsPlayerService(@NotNull QuestsPlayerRepository repo, @NotNull JExLogger logger) {
        this.repo = repo;
        this.logger = logger;
    }

    public @NotNull CompletableFuture<QuestsPlayer> trackAsync(@NotNull UUID uuid) {
        return this.repo.findOrCreateAsync(uuid).exceptionally(ex -> {
            this.logger.error("track failed for {}: {}", uuid, ex.getMessage());
            return null;
        });
    }

    public @NotNull CompletableFuture<Optional<QuestsPlayer>> findAsync(@NotNull UUID uuid) {
        return this.repo.findByUuidAsync(uuid).exceptionally(ex -> {
            this.logger.error("find failed for {}: {}", uuid, ex.getMessage());
            return Optional.empty();
        });
    }

    public @NotNull QuestsPlayerRepository repository() {
        return this.repo;
    }
}
