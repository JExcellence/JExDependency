package de.jexcellence.economy.database.repository;

import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CurrencyRepositoryTest {

        @Test
        @DisplayName("findByIdentifier resolves cached currency instances")
        void findByIdentifierResolvesCachedCurrency() {
                CountingExecutorService executor = new CountingExecutorService();
                try {
                        CurrencyRepository repository = new CurrencyRepository(executor, mock(EntityManagerFactory.class));

                        Currency tokens = new Currency("tokens");
                        tokens.setPrefix("T");
                        repository.update(tokens);

                        Optional<Currency> result = repository.findByIdentifier("tokens");

                        assertTrue(result.isPresent(), "Currency should be returned when present in the cache");
                        assertSame(tokens, result.orElseThrow(), "Repository must surface the cached entity instance");

                        GenericCachedRepository.LastCacheLookup<String> lookup = repository.getLastCacheLookup();
                        assertNotNull(lookup, "Cache lookup metadata should be recorded");
                        assertEquals("identifier", lookup.field(), "Identifier field must be used for cache lookups");
                        assertEquals("tokens", lookup.cacheKey(), "Cache key must reflect the requested identifier");
                } finally {
                        executor.shutdownNow();
                }
        }

        @Test
        @DisplayName("findByIdentifierAsync uses the executor and wraps results in Optional")
        void findByIdentifierAsyncUsesExecutor() {
                CountingExecutorService executor = new CountingExecutorService();
                try {
                        CurrencyRepository repository = new CurrencyRepository(executor, mock(EntityManagerFactory.class));

                        Currency gems = new Currency("gems");
                        repository.update(gems);

                        Optional<Currency> result = repository.findByIdentifierAsync("gems").join();

                        assertTrue(result.isPresent(), "Asynchronous lookup should surface the cached entity");
                        assertSame(gems, result.orElseThrow(), "Optional should unwrap to the cached currency");
                        assertTrue(executor.getExecutedTaskCount() > 0,
                                "Executor should have processed asynchronous lookup work");

                        GenericCachedRepository.LastCacheLookup<String> lookup = repository.getLastCacheLookup();
                        assertNotNull(lookup, "Cache lookups should be tracked for assertions");
                        assertEquals("identifier", lookup.field(), "Identifier lookups must use the identifier column");
                } finally {
                        executor.shutdownNow();
                }
        }

        @Test
        @DisplayName("findByIdentifier returns Optional.empty for missing currencies")
        void findByIdentifierHandlesMissingCurrency() {
                CountingExecutorService executor = new CountingExecutorService();
                try {
                        CurrencyRepository repository = new CurrencyRepository(executor, mock(EntityManagerFactory.class));

                        assertTrue(repository.findByIdentifier("missing").isEmpty(),
                                "Lookup should be empty when the currency is unknown");
                        assertTrue(repository.findByIdentifierAsync("missing").join().isEmpty(),
                                "Asynchronous lookup should also report an empty result");
                } finally {
                        executor.shutdownNow();
                }
        }

        @Test
        @DisplayName("findAllAsync returns a snapshot of cached currencies")
        void findAllAsyncReturnsCachedSnapshot() {
                CountingExecutorService executor = new CountingExecutorService();
                try {
                        CurrencyRepository repository = new CurrencyRepository(executor, mock(EntityManagerFactory.class));

                        Currency gold = new Currency("gold");
                        Currency silver = new Currency("silver");
                        repository.update(gold);
                        repository.update(silver);

                        List<Currency> asyncResult = repository.findAllAsync(0, 16).join();

                        assertEquals(2, asyncResult.size(), "Snapshot should include all cached currencies");
                        assertEquals(Set.of(gold, silver), Set.copyOf(asyncResult),
                                "Result should match the cached entity instances");
                        assertTrue(executor.getExecutedTaskCount() >= 1,
                                "Executor should process the asynchronous retrieval");

                        asyncResult.clear();
                        List<Currency> secondSnapshot = repository.findAll(0, 16);
                        assertEquals(2, secondSnapshot.size(),
                                "Clearing the result list must not mutate the cached snapshot");
                } finally {
                        executor.shutdownNow();
                }
        }

        @Test
        @DisplayName("Concurrent updates ensure the latest write remains cached")
        void concurrentUpdatesMaintainLatestSnapshot() {
                ManualExecutorService executor = new ManualExecutorService();
                CurrencyRepository repository = new CurrencyRepository(executor, mock(EntityManagerFactory.class));

                Currency initial = new Currency("credits");
                initial.setPrefix("initial");
                Currency updated = new Currency("credits");
                updated.setPrefix("updated");

                CompletableFuture<Currency> firstFuture = repository.updateAsync(initial);
                CompletableFuture<Currency> secondFuture = repository.updateAsync(updated);

                List<Runnable> tasks = executor.drainQueuedTasks();
                assertEquals(2, tasks.size(), "Two update tasks should be queued for execution");

                // Run the second update before the first to simulate interleaved execution
                tasks.get(1).run();
                tasks.get(0).run();

                assertSame(initial, firstFuture.join(), "First update future should resolve to the initial entity");
                assertSame(updated, secondFuture.join(), "Second update future should resolve to the updated entity");

                Currency cached = repository.findByIdentifier("credits").orElseThrow();
                assertSame(initial, cached, "The final cached entity should reflect the last executed update");
                assertSame(initial, repository.snapshotCache().get("credits"),
                        "Cache snapshot should expose the latest currency state");

                executor.shutdownNow();
        }

        private static final class CountingExecutorService extends AbstractExecutorService {

                private final AtomicBoolean shutdown = new AtomicBoolean();
                private final AtomicInteger executedTasks = new AtomicInteger();

                @Override
                public void shutdown() {
                        shutdown.set(true);
                }

                @Override
                public List<Runnable> shutdownNow() {
                        shutdown();
                        return List.of();
                }

                @Override
                public boolean isShutdown() {
                        return shutdown.get();
                }

                @Override
                public boolean isTerminated() {
                        return shutdown.get();
                }

                @Override
                public boolean awaitTermination(long timeout, TimeUnit unit) {
                        return shutdown.get();
                }

                @Override
                public void execute(final Runnable command) {
                        executedTasks.incrementAndGet();
                        command.run();
                }

                int getExecutedTaskCount() {
                        return executedTasks.get();
                }
        }

        private static final class ManualExecutorService extends AbstractExecutorService {

                private final AtomicBoolean shutdown = new AtomicBoolean();
                private final LinkedBlockingQueue<Runnable> queuedTasks = new LinkedBlockingQueue<>();

                @Override
                public void shutdown() {
                        shutdown.set(true);
                }

                @Override
                public List<Runnable> shutdownNow() {
                        shutdown();
                        List<Runnable> remaining = new ArrayList<>();
                        queuedTasks.drainTo(remaining);
                        return remaining;
                }

                @Override
                public boolean isShutdown() {
                        return shutdown.get();
                }

                @Override
                public boolean isTerminated() {
                        return shutdown.get() && queuedTasks.isEmpty();
                }

                @Override
                public boolean awaitTermination(long timeout, TimeUnit unit) {
                        return shutdown.get() && queuedTasks.isEmpty();
                }

                @Override
                public void execute(final Runnable command) {
                        queuedTasks.add(command);
                }

                List<Runnable> drainQueuedTasks() {
                        List<Runnable> tasks = new ArrayList<>();
                        queuedTasks.drainTo(tasks);
                        return tasks;
                }
        }
}
