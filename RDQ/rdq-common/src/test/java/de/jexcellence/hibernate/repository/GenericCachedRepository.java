package de.jexcellence.hibernate.repository;

import jakarta.persistence.EntityManagerFactory;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Test-scoped stub mirroring the constructor signature of the production
 * {@code GenericCachedRepository}. The stub captures constructor arguments so
 * unit tests can assert the values provided by repository subclasses without
 * touching the real persistence layer.
 */
public class GenericCachedRepository<T, ID, CACHE_KEY> {

    private static ConstructorInvocation<?, ?, ?> lastConstructorInvocation;

    public GenericCachedRepository(
            final ExecutorService executor,
            final EntityManagerFactory entityManagerFactory,
            final Class<T> entityType,
            final Function<T, ID> idExtractor
    ) {
        lastConstructorInvocation = new ConstructorInvocation<>(executor, entityManagerFactory, entityType, idExtractor);
    }

    /**
     * Resets the stored constructor invocation to {@code null} so subsequent
     * tests start from a clean slate.
     */
    public static void resetConstructorInvocation() {
        lastConstructorInvocation = null;
    }

    /**
     * Retrieves the most recent constructor invocation captured by the stub.
     *
     * @param <T> the entity type captured in the constructor
     * @param <ID> the identifier type associated with the entity
     * @param <CACHE_KEY> the cache key type used by the repository
     * @return the previously recorded constructor invocation or {@code null}
     *         when the constructor has not been invoked
     */
    @SuppressWarnings("unchecked")
    public static <T, ID, CACHE_KEY> ConstructorInvocation<T, ID, CACHE_KEY> getLastConstructorInvocation() {
        return (ConstructorInvocation<T, ID, CACHE_KEY>) lastConstructorInvocation;
    }

    /**
     * Record type storing the constructor arguments supplied to the stub.
     */
    public record ConstructorInvocation<T, ID, CACHE_KEY>(
            ExecutorService executor,
            EntityManagerFactory entityManagerFactory,
            Class<T> entityType,
            Function<T, ID> idExtractor
    ) {
    }
}
