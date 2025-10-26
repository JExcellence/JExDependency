package de.jexcellence.economy.database.repository;

import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.User;
import de.jexcellence.economy.database.entity.UserCurrency;
import de.jexcellence.hibernate.entity.AbstractEntity;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("UserCurrencyRepository integration with cached lookups")
class UserCurrencyRepositoryTest {

    private TrackingExecutorService executor;
    private TestUserCurrencyRepository repository;
    private UserCurrency cachedAssociation;
    private Currency coinsCurrency;

    @BeforeEach
    void setUp() throws Exception {
        this.executor = new TrackingExecutorService();
        EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);
        this.repository = new TestUserCurrencyRepository(this.executor, entityManagerFactory);

        User player = new User(UUID.randomUUID(), "BalancePlayer");
        this.coinsCurrency = mock(Currency.class);
        when(this.coinsCurrency.getId()).thenReturn(73L);

        this.cachedAssociation = new UserCurrency(player, this.coinsCurrency, 245.5);
        assignEntityId(this.cachedAssociation, 11L);
        this.repository.preload(this.cachedAssociation);
    }

    @Test
    @DisplayName("Constructor binds cache extractor to player UUID")
    void constructorConfiguresCacheKeyExtractor() {
        Function<UserCurrency, UUID> cacheKeyExtractor = resolveCacheKeyExtractor(this.repository);

        UUID expectedKey = this.cachedAssociation.getPlayer().getUniqueId();
        assertEquals(expectedKey, cacheKeyExtractor.apply(this.cachedAssociation),
            "Cache extractor should delegate to the player unique identifier");
    }

    @Test
    @DisplayName("findByUserAsync returns cached associations without hitting persistence supplier")
    void findByUserAsyncReturnsCachedAssociations() {
        this.repository.setSupplier(attributes -> List.of(this.cachedAssociation));

        CompletableFuture<List<UserCurrency>> future = this.repository.findByUserAsync(
            this.cachedAssociation.getPlayer().getUniqueId()
        );

        assertTrue(this.executor.hasPendingTasks(), "Lookup should defer to executor for completion");

        Map<String, Object> attributes = this.repository.getLastAttributes();
        assertNotNull(attributes, "Attribute map should be captured for cache lookups");
        assertEquals(this.cachedAssociation.getPlayer().getUniqueId(), attributes.get("player.uniqueId"));

        this.executor.runNext();

        List<UserCurrency> resolved = future.join();
        assertEquals(List.of(this.cachedAssociation), resolved,
            "Cached association should be returned to callers");
        assertEquals(0, this.repository.getSupplierInvocationCount(),
            "Cache hit should avoid invoking the backing supplier");
    }

    @Test
    @DisplayName("findByCurrencyAsync delegates to supplier using currency identifier attributes")
    void findByCurrencyAsyncDelegatesToSupplier() {
        UserCurrency otherAssociation = new UserCurrency(
            new User(UUID.randomUUID(), "Other"),
            this.coinsCurrency,
            512.0
        );
        assignEntityId(otherAssociation, 19L);

        this.repository.setSupplier(attributes -> List.of(this.cachedAssociation, otherAssociation));

        CompletableFuture<List<UserCurrency>> future = this.repository.findByCurrencyAsync(this.coinsCurrency);

        Map<String, Object> attributes = this.repository.getLastAttributes();
        assertNotNull(attributes, "Attribute map should be captured for currency lookups");
        assertEquals(73L, attributes.get("currency.id"));

        this.executor.runNext();

        List<UserCurrency> results = future.join();
        assertEquals(List.of(this.cachedAssociation, otherAssociation), results);
        assertEquals(1, this.repository.getSupplierInvocationCount(),
            "Currency lookups should consult the backing supplier once");
    }

    @Test
    @DisplayName("update synchronises cache and persistence maps")
    void updatePropagatesToCacheAndPersistence() {
        this.cachedAssociation.setBalance(900.0);

        UserCurrency updated = this.repository.update(this.cachedAssociation);

        assertSame(this.cachedAssociation, updated, "update should return the managed instance");

        this.executor.runAll();

        assertEquals(1, this.repository.getUpdateInvocationCount(),
            "update should schedule a single persistence task");

        Optional<UserCurrency> persisted = this.repository.findPersistedById(this.cachedAssociation.getId());
        assertTrue(persisted.isPresent(), "Persisted map should contain the updated association");
        assertEquals(900.0, persisted.orElseThrow().getBalance());

        Optional<UserCurrency> cached = this.repository.findCachedByPlayer(
            this.cachedAssociation.getPlayer().getUniqueId()
        );
        assertTrue(cached.isPresent(), "Cache should be refreshed with updated association");
        assertEquals(900.0, cached.orElseThrow().getBalance());
    }

    @Test
    @DisplayName("delete evicts cache entries and persisted copies")
    void deletePropagatesToCacheAndPersistence() {
        this.repository.delete(this.cachedAssociation.getId());

        this.executor.runAll();

        assertEquals(1, this.repository.getDeleteInvocationCount(),
            "delete should schedule a single removal task");

        assertTrue(this.repository.findPersistedById(this.cachedAssociation.getId()).isEmpty(),
            "Persisted map should no longer contain the association");
        assertTrue(this.repository.findCachedByPlayer(
            this.cachedAssociation.getPlayer().getUniqueId()
        ).isEmpty(), "Cache should evict the association after delete");
    }

    private static Function<UserCurrency, UUID> resolveCacheKeyExtractor(UserCurrencyRepository repository) {
        for (Field field : GenericCachedRepository.class.getDeclaredFields()) {
            if (!Function.class.isAssignableFrom(field.getType())) {
                continue;
            }
            field.setAccessible(true);
            try {
                @SuppressWarnings("unchecked")
                Function<UserCurrency, UUID> extractor = (Function<UserCurrency, UUID>) field.get(repository);
                if (extractor != null) {
                    return extractor;
                }
            } catch (IllegalAccessException ignored) {
                // continue searching
            }
        }
        throw new IllegalStateException("Unable to resolve cache key extractor from GenericCachedRepository");
    }

    private static void assignEntityId(final AbstractEntity entity, final long identifier) throws Exception {
        Field idField = null;
        Class<?> current = entity.getClass();
        while (current != null && idField == null) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getType() == Long.class || field.getType() == long.class) {
                    idField = field;
                    break;
                }
            }
            current = current.getSuperclass();
        }

        if (idField == null) {
            throw new IllegalStateException("Unable to locate identifier field on entity");
        }

        idField.setAccessible(true);
        idField.set(entity, identifier);
    }

    private static final class TestUserCurrencyRepository extends UserCurrencyRepository {

        private final TrackingExecutorService executor;
        private final Map<Long, UserCurrency> persistedEntities = new ConcurrentHashMap<>();
        private final Map<UUID, UserCurrency> cachedEntities = new ConcurrentHashMap<>();
        private final AtomicInteger supplierInvocationCount = new AtomicInteger();
        private final AtomicInteger updateInvocationCount = new AtomicInteger();
        private final AtomicInteger deleteInvocationCount = new AtomicInteger();
        private Map<String, Object> lastAttributes;
        private Function<Map<String, Object>, List<UserCurrency>> supplier = attributes -> List.of();

        private TestUserCurrencyRepository(
                final TrackingExecutorService executor,
                final EntityManagerFactory entityManagerFactory
        ) {
            super(executor, entityManagerFactory);
            this.executor = executor;
        }

        void preload(final UserCurrency association) {
            this.persistedEntities.put(association.getId(), association);
            this.cachedEntities.put(association.getPlayer().getUniqueId(), association);
        }

        void setSupplier(final Function<Map<String, Object>, List<UserCurrency>> supplier) {
            this.supplier = supplier;
        }

        Map<String, Object> getLastAttributes() {
            return this.lastAttributes;
        }

        int getSupplierInvocationCount() {
            return this.supplierInvocationCount.get();
        }

        int getUpdateInvocationCount() {
            return this.updateInvocationCount.get();
        }

        int getDeleteInvocationCount() {
            return this.deleteInvocationCount.get();
        }

        Optional<UserCurrency> findPersistedById(final Long id) {
            return Optional.ofNullable(this.persistedEntities.get(id));
        }

        Optional<UserCurrency> findCachedByPlayer(final UUID playerId) {
            return Optional.ofNullable(this.cachedEntities.get(playerId));
        }

        @Override
        public CompletableFuture<List<UserCurrency>> findByUserAsync(final UUID playerUniqueId) {
            return super.findByUserAsync(playerUniqueId);
        }

        @Override
        protected CompletableFuture<List<UserCurrency>> findListByAttributesAsync(final Map<String, Object> attributes) {
            this.lastAttributes = attributes;
            CompletableFuture<List<UserCurrency>> future = new CompletableFuture<>();
            this.executor.execute(() -> {
                if (attributes.containsKey("player.uniqueId")) {
                    UUID playerId = (UUID) attributes.get("player.uniqueId");
                    UserCurrency cached = this.cachedEntities.get(playerId);
                    if (cached != null) {
                        future.complete(List.of(cached));
                        return;
                    }
                }

                this.supplierInvocationCount.incrementAndGet();
                future.complete(this.supplier.apply(attributes));
            });
            return future;
        }

        @Override
        protected CompletableFuture<UserCurrency> updateAsync(final UserCurrency entity) {
            CompletableFuture<UserCurrency> future = new CompletableFuture<>();
            this.executor.execute(() -> {
                this.updateInvocationCount.incrementAndGet();
                this.persistedEntities.put(entity.getId(), entity);
                this.cachedEntities.put(entity.getPlayer().getUniqueId(), entity);
                future.complete(entity);
            });
            return future;
        }

        @Override
        protected CompletableFuture<Void> deleteAsync(final Long identifier) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            this.executor.execute(() -> {
                this.deleteInvocationCount.incrementAndGet();
                this.persistedEntities.remove(identifier);
                this.cachedEntities.values().removeIf(entity -> Objects.equals(entity.getId(), identifier));
                future.complete(null);
            });
            return future;
        }
    }

    private static final class TrackingExecutorService extends AbstractExecutorService {

        private final Deque<Runnable> tasks = new ArrayDeque<>();
        private boolean shutdown;

        boolean hasPendingTasks() {
            return !this.tasks.isEmpty();
        }

        void runNext() {
            Runnable next = this.tasks.poll();
            if (next != null) {
                next.run();
            }
        }

        void runAll() {
            Runnable next;
            while ((next = this.tasks.poll()) != null) {
                next.run();
            }
        }

        @Override
        public void shutdown() {
            this.shutdown = true;
            this.tasks.clear();
        }

        @Override
        public List<Runnable> shutdownNow() {
            this.shutdown = true;
            List<Runnable> remaining = new ArrayList<>(this.tasks);
            this.tasks.clear();
            return remaining;
        }

        @Override
        public boolean isShutdown() {
            return this.shutdown;
        }

        @Override
        public boolean isTerminated() {
            return this.shutdown && this.tasks.isEmpty();
        }

        @Override
        public boolean awaitTermination(final long timeout, final TimeUnit unit) {
            long deadline = System.nanoTime() + unit.toNanos(timeout);
            while (!isTerminated() && System.nanoTime() < deadline) {
                Thread.onSpinWait();
            }
            return isTerminated();
        }

        @Override
        public void execute(final Runnable command) {
            Objects.requireNonNull(command, "command");
            this.tasks.add(command);
        }
    }
}

