package de.jexcellence.core.service;

import de.jexcellence.core.database.entity.BossBarPreference;
import de.jexcellence.core.database.repository.BossBarPreferenceRepository;
import de.jexcellence.jexplatform.logging.JExLogger;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service facade over {@link BossBarPreferenceRepository}. Reads and
 * upserts one preference row per (player, provider) pair.
 */
public class BossBarService {

    private final BossBarPreferenceRepository repo;
    private final JExLogger logger;

    public BossBarService(@NotNull BossBarPreferenceRepository repo, @NotNull JExLogger logger) {
        this.repo = repo;
        this.logger = logger;
    }

    public @NotNull CompletableFuture<Optional<BossBarPreference>> get(@NotNull UUID playerUuid, @NotNull String providerKey) {
        return this.repo.findByPlayerAndProviderAsync(playerUuid, providerKey).exceptionally(ex -> {
            this.logger.error("get failed for {}/{}: {}", playerUuid, providerKey, ex.getMessage());
            return Optional.empty();
        });
    }

    public @NotNull CompletableFuture<List<BossBarPreference>> list(@NotNull UUID playerUuid) {
        return this.repo.findAllByPlayerAsync(playerUuid).exceptionally(ex -> {
            this.logger.error("list failed for {}: {}", playerUuid, ex.getMessage());
            return List.of();
        });
    }

    public @NotNull CompletableFuture<BossBarPreference> setEnabled(
            @NotNull UUID playerUuid, @NotNull String providerKey, boolean enabled) {
        return this.repo.findByPlayerAndProviderAsync(playerUuid, providerKey)
                .thenCompose(opt -> {
                    if (opt.isPresent()) {
                        final BossBarPreference row = opt.get();
                        row.setEnabled(enabled);
                        return this.repo.updateAsync(row);
                    }
                    return this.repo.createAsync(new BossBarPreference(playerUuid, providerKey, enabled));
                })
                .exceptionally(ex -> {
                    this.logger.error("setEnabled failed for {}/{}: {}", playerUuid, providerKey, ex.getMessage());
                    return null;
                });
    }
}
