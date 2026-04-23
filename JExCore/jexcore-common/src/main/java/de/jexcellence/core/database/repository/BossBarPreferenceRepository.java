package de.jexcellence.core.database.repository;

import de.jexcellence.core.database.entity.BossBarPreference;
import de.jexcellence.jehibernate.repository.base.AbstractCrudRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for {@link BossBarPreference} rows. Provider keys are
 * lower-cased before lookup so callers needn't worry about casing.
 */
public class BossBarPreferenceRepository extends AbstractCrudRepository<BossBarPreference, Long> {

    public BossBarPreferenceRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory emf,
            @NotNull Class<BossBarPreference> entityClass
    ) {
        super(executor, emf, entityClass);
    }

    public @NotNull CompletableFuture<Optional<BossBarPreference>> findByPlayerAndProviderAsync(
            @NotNull UUID playerUuid, @NotNull String providerKey) {
        return query()
                .and("playerUuid", playerUuid)
                .and("providerKey", providerKey.trim().toLowerCase(Locale.ROOT))
                .firstAsync();
    }

    public @NotNull CompletableFuture<List<BossBarPreference>> findAllByPlayerAsync(@NotNull UUID playerUuid) {
        return query().and("playerUuid", playerUuid).listAsync();
    }
}
