package com.raindropcentral.rds.database.repository;

import com.raindropcentral.rds.database.entity.ServerBank;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for server bank balances.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings({
        "unused",
        "FieldCanBeLocal"
})
/**
 * Represents the RServerBank API type.
 */
public class RServerBank extends CachedRepository<ServerBank, Long, String> {

    private final EntityManagerFactory entityManagerFactory;

    /**
     * Creates a new server bank repository.
     *
     * @param executorService executor used for repository work
     * @param entityManagerFactory entity manager factory backing the repository
     * @param entityClass entity type managed by the repository
     * @param keyExtractor cache key extractor for loaded entities
     */
    public RServerBank(
            final @NotNull ExecutorService executorService,
            final @NotNull EntityManagerFactory entityManagerFactory,
            final @NotNull Class<ServerBank> entityClass,
            final @NotNull Function<ServerBank, String> keyExtractor
    ) {
        super(executorService, entityManagerFactory, entityClass, keyExtractor);
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * Finds the server bank entry for the provided currency.
     *
     * @param currencyType currency identifier
     * @return matching entry, or {@code null} when none exists
     */
    public @Nullable ServerBank findByCurrencyType(
            final @NotNull String currencyType
    ) {
        final String normalizedCurrencyType = this.normalizeCurrencyType(currencyType);
        return this.findByAttributes(Map.of("currency_type", normalizedCurrencyType)).orElse(null);
    }

    /**
     * Returns every persisted server-bank entry ordered by currency type.
     *
     * @return all server-bank entries
     */
    public @NotNull List<ServerBank> findAllEntries() {
        try (var entityManager = this.entityManagerFactory.createEntityManager()) {
            final List<ServerBank> entries = new ArrayList<>(
                    entityManager
                            .createQuery("SELECT entry FROM ServerBank entry", ServerBank.class)
                            .getResultList()
            );
            entries.sort(Comparator.comparing(ServerBank::getCurrencyType, String.CASE_INSENSITIVE_ORDER));
            return entries;
        }
    }

    private @NotNull String normalizeCurrencyType(
            final @NotNull String currencyType
    ) {
        if (currencyType.isBlank()) {
            return "vault";
        }

        return currencyType.trim().toLowerCase(Locale.ROOT);
    }
}
