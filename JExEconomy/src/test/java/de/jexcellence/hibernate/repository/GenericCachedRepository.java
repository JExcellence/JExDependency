package de.jexcellence.hibernate.repository;

import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.EntityManagerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Test-scoped stub mirroring the production {@code GenericCachedRepository} API.
 * <p>
 * The stub captures constructor invocations while providing an in-memory cache
 * and persistence map so unit tests can assert cache interactions without
 * touching the real persistence layer.
 * </p>
 */
public class GenericCachedRepository<T, ID, CACHE_KEY> {

    private static ConstructorInvocation<?, ?, ?> lastConstructorInvocation;

    private final ExecutorService executor;
    private final EntityManagerFactory entityManagerFactory;
    private final Class<T> entityType;
    private final Function<T, CACHE_KEY> cacheKeyExtractor;
    private final Map<ID, T> persistenceStore = new ConcurrentHashMap<>();
    private final Map<CACHE_KEY, T> cacheStore = new ConcurrentHashMap<>();

    public GenericCachedRepository(
            final ExecutorService executor,
            final EntityManagerFactory entityManagerFactory,
            final Class<T> entityType,
            final Function<T, CACHE_KEY> cacheKeyExtractor
    ) {
        this.executor = executor;
        this.entityManagerFactory = entityManagerFactory;
        this.entityType = entityType;
        this.cacheKeyExtractor = cacheKeyExtractor;
        lastConstructorInvocation = new ConstructorInvocation<>(executor, entityManagerFactory, entityType, cacheKeyExtractor);
    }

    public static void resetConstructorInvocation() {
        lastConstructorInvocation = null;
    }

    @SuppressWarnings("unchecked")
    public static <T, ID, CACHE_KEY> ConstructorInvocation<T, ID, CACHE_KEY> getLastConstructorInvocation() {
        return (ConstructorInvocation<T, ID, CACHE_KEY>) lastConstructorInvocation;
    }

    public T create(final T entity) {
        CACHE_KEY cacheKey = cacheKeyExtractor.apply(entity);
        ID identifier = extractIdentifier(entity);
        if (identifier != null) {
            persistenceStore.put(identifier, entity);
        }
        cacheStore.put(cacheKey, entity);
        return entity;
    }

    public CompletableFuture<T> createAsync(final T entity) {
        return CompletableFuture.completedFuture(create(entity));
    }

    public T update(final T entity) {
        CACHE_KEY cacheKey = cacheKeyExtractor.apply(entity);
        ID identifier = extractIdentifier(entity);
        if (identifier != null) {
            persistenceStore.put(identifier, entity);
        }
        cacheStore.put(cacheKey, entity);
        return entity;
    }

    public CompletableFuture<T> updateAsync(final T entity) {
        return CompletableFuture.completedFuture(update(entity));
    }

    public boolean delete(final ID identifier) {
        T removed = persistenceStore.remove(identifier);
        if (removed != null) {
            CACHE_KEY cacheKey = cacheKeyExtractor.apply(removed);
            cacheStore.remove(cacheKey);
            return true;
        }

        try {
            cacheStore.remove((CACHE_KEY) identifier);
        } catch (ClassCastException ignored) {
            // Identifier type does not map directly to the cache key; ignore.
        }
        return false;
    }

    public CompletableFuture<Void> deleteAsync(final ID identifier) {
        delete(identifier);
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<T> findByCacheKeyAsync(final String fieldName, final Object cacheKey) {
        @SuppressWarnings("unchecked")
        T value = cacheStore.get((CACHE_KEY) cacheKey);
        return CompletableFuture.completedFuture(value);
    }

    public CompletableFuture<T> findByAttributesAsync(final Map<String, Object> attributes) {
        return CompletableFuture.completedFuture(null);
    }

    public Map<ID, T> getPersistenceSnapshot() {
        return Collections.unmodifiableMap(persistenceStore);
    }

    public Map<CACHE_KEY, T> getCacheSnapshot() {
        return Collections.unmodifiableMap(cacheStore);
    }

    private ID extractIdentifier(final T entity) {
        if (entity instanceof AbstractEntity abstractEntity) {
            @SuppressWarnings("unchecked")
            ID identifier = (ID) abstractEntity.getId();
            return identifier;
        }
        return null;
    }

    public record ConstructorInvocation<T, ID, CACHE_KEY>(
            ExecutorService executor,
            EntityManagerFactory entityManagerFactory,
            Class<T> entityType,
            Function<T, CACHE_KEY> cacheKeyExtractor
    ) {
    }
}
