package de.jexcellence.hibernate.repository;

import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Test-scoped in-memory implementation of the {@code GenericCachedRepository} contract.
 * <p>
 * The production repository lives in an external dependency which is substituted during
 * unit tests. This lightweight variant provides deterministic behaviour for lookups,
 * cache inspection, and asynchronous execution so repository tests can focus on their
 * coordination logic without touching a real database layer.
 * </p>
 *
 * @param <T>         entity type managed by the repository
 * @param <ID>        identifier type associated with the entity (unused in tests)
 * @param <CACHE_KEY> cache key type derived from the entity
 */
public class GenericCachedRepository<T, ID, CACHE_KEY> {

        private final ExecutorService executor;
        private final Function<T, CACHE_KEY> cacheKeyExtractor;
        private final ConcurrentMap<CACHE_KEY, T> cache = new ConcurrentHashMap<>();
        private final AtomicReference<LastCacheLookup<CACHE_KEY>> lastCacheLookup = new AtomicReference<>();

        public GenericCachedRepository(
                final ExecutorService executor,
                final EntityManagerFactory entityManagerFactory,
                final Class<T> entityType,
                final Function<T, CACHE_KEY> cacheKeyExtractor
        ) {
                this.executor = executor;
                this.cacheKeyExtractor = cacheKeyExtractor;
        }

        public CompletableFuture<T> findByCacheKeyAsync(
                final String fieldName,
                final CACHE_KEY cacheKey
        ) {
                lastCacheLookup.set(new LastCacheLookup<>(fieldName, cacheKey));
                return CompletableFuture.supplyAsync(() -> cache.get(cacheKey), executor);
        }

        public T findByCacheKey(
                final String fieldName,
                final CACHE_KEY cacheKey
        ) {
                lastCacheLookup.set(new LastCacheLookup<>(fieldName, cacheKey));
                return cache.get(cacheKey);
        }

        public List<T> findAll(final int page, final int pageSize) {
                return new ArrayList<>(cache.values());
        }

        public CompletableFuture<List<T>> findAllAsync(final int page, final int pageSize) {
                return CompletableFuture.supplyAsync(() -> findAll(page, pageSize), executor);
        }

        public T update(final T entity) {
                cache.put(cacheKeyExtractor.apply(entity), entity);
                return entity;
        }

        public CompletableFuture<T> updateAsync(final T entity) {
                return CompletableFuture.supplyAsync(() -> update(entity), executor);
        }

        public Map<CACHE_KEY, T> snapshotCache() {
                return Map.copyOf(cache);
        }

        public LastCacheLookup<CACHE_KEY> getLastCacheLookup() {
                return lastCacheLookup.get();
        }

        public record LastCacheLookup<CACHE_KEY>(String field, CACHE_KEY cacheKey) {
        }
}
