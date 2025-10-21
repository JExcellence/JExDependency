package com.raindropcentral.core.database.repository;

import com.raindropcentral.core.database.entity.player.RPlayer;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

class RPlayerRepositoryTest {

    private TrackingExecutorService executor;
    private TestRPlayerRepository repository;
    private RPlayer samplePlayer;

    @BeforeEach
    void setUp() {
        this.executor = new TrackingExecutorService();
        this.repository = new TestRPlayerRepository(this.executor);
        this.samplePlayer = new RPlayer(UUID.randomUUID(), "Notch1234");
    }

    @Test
    void findByUuidAsyncDelegatesToAttributeLookup() {
        this.repository.setFindResultSupplier(() -> this.samplePlayer);

        CompletableFuture<Optional<RPlayer>> future = this.repository.findByUuidAsync(this.samplePlayer.getUniqueId());

        assertFalse(future.isDone(), "Future should not complete before executor runs");
        Map<String, Object> capturedAttributes = this.repository.getLastAttributes();
        assertNotNull(capturedAttributes, "Attributes should be captured for cache lookup");
        assertEquals(1, capturedAttributes.size());
        assertTrue(capturedAttributes.containsKey("uniqueId"));
        assertEquals(this.samplePlayer.getUniqueId(), capturedAttributes.get("uniqueId"));

        this.executor.runNext();

        assertTrue(future.isDone(), "Future should complete after executor task runs");
        assertEquals(Optional.of(this.samplePlayer), future.join());
    }

    @Test
    void findByNameAsyncDelegatesToAttributeLookup() {
        this.repository.setFindResultSupplier(() -> this.samplePlayer);

        CompletableFuture<Optional<RPlayer>> future = this.repository.findByNameAsync(this.samplePlayer.getPlayerName());

        assertFalse(future.isDone(), "Future should not complete before executor runs");
        Map<String, Object> capturedAttributes = this.repository.getLastAttributes();
        assertNotNull(capturedAttributes, "Attributes should be captured for cache lookup");
        assertEquals(1, capturedAttributes.size());
        assertTrue(capturedAttributes.containsKey("playerName"));
        assertEquals(this.samplePlayer.getPlayerName(), capturedAttributes.get("playerName"));

        this.executor.runNext();

        assertTrue(future.isDone(), "Future should complete after executor task runs");
        assertEquals(Optional.of(this.samplePlayer), future.join());
    }

    @Test
    void findByUuidAsyncRejectsNullInput() {
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> this.repository.findByUuidAsync(null)
        );
        assertEquals("uniqueId cannot be null", exception.getMessage());
    }

    @Test
    void findByNameAsyncRejectsNullInput() {
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> this.repository.findByNameAsync(null)
        );
        assertEquals("playerName cannot be null", exception.getMessage());
    }

    @Test
    void createOrUpdateAsyncRejectsNullInput() {
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> this.repository.createOrUpdateAsync(null)
        );
        assertEquals("player cannot be null", exception.getMessage());
    }

    @Test
    void existsByUuidAsyncResolvesTrueWhenPlayerExists() {
        this.repository.setFindResultSupplier(() -> this.samplePlayer);

        CompletableFuture<Boolean> future = this.repository.existsByUuidAsync(this.samplePlayer.getUniqueId());

        assertFalse(future.isDone(), "Future should not complete before executor runs");

        this.executor.runNext();

        assertTrue(future.isDone(), "Future should complete after executor task runs");
        assertTrue(future.join());
    }

    @Test
    void existsByUuidAsyncResolvesFalseWhenPlayerMissing() {
        this.repository.setFindResultSupplier(() -> null);

        CompletableFuture<Boolean> future = this.repository.existsByUuidAsync(this.samplePlayer.getUniqueId());

        assertFalse(future.isDone(), "Future should not complete before executor runs");

        this.executor.runNext();

        assertTrue(future.isDone(), "Future should complete after executor task runs");
        assertFalse(future.join());
    }

    @Test
    void createOrUpdateAsyncBranchesToCreateWhenPlayerMissing() {
        this.repository.setFindResultSupplier(() -> null);
        this.repository.setCreateResultSupplier(() -> this.samplePlayer);

        CompletableFuture<RPlayer> future = this.repository.createOrUpdateAsync(this.samplePlayer);

        assertFalse(future.isDone(), "Future should not complete before executor runs");

        this.executor.runNext();
        assertEquals(1, this.repository.getCreateInvocationCount());
        assertEquals(0, this.repository.getUpdateInvocationCount());
        assertSame(this.samplePlayer, this.repository.getLastCreateEntity());
        assertFalse(future.isDone(), "Create stage should still be pending until executor runs");

        this.executor.runNext();

        assertTrue(future.isDone(), "Future should complete after create task runs");
        assertEquals(this.samplePlayer, future.join());
    }

    @Test
    void createOrUpdateAsyncBranchesToUpdateWhenPlayerExists() {
        this.repository.setFindResultSupplier(() -> this.samplePlayer);
        RPlayer updated = new RPlayer(this.samplePlayer.getUniqueId(), "UpdatedName");
        this.repository.setUpdateResultSupplier(() -> updated);

        CompletableFuture<RPlayer> future = this.repository.createOrUpdateAsync(this.samplePlayer);

        assertFalse(future.isDone(), "Future should not complete before executor runs");

        this.executor.runNext();
        assertEquals(0, this.repository.getCreateInvocationCount());
        assertEquals(1, this.repository.getUpdateInvocationCount());
        assertSame(this.samplePlayer, this.repository.getLastUpdateEntity());
        assertFalse(future.isDone(), "Update stage should still be pending until executor runs");

        this.executor.runNext();

        assertTrue(future.isDone(), "Future should complete after update task runs");
        assertEquals(updated, future.join());
    }

    private static final class TestRPlayerRepository extends RPlayerRepository {

        private final TrackingExecutorService executor;
        private Supplier<RPlayer> findResultSupplier = () -> null;
        private Supplier<RPlayer> createResultSupplier = () -> null;
        private Supplier<RPlayer> updateResultSupplier = () -> null;
        private Map<String, Object> lastAttributes;
        private final AtomicReference<RPlayer> lastCreateEntity = new AtomicReference<>();
        private final AtomicReference<RPlayer> lastUpdateEntity = new AtomicReference<>();
        private int createInvocationCount;
        private int updateInvocationCount;

        private TestRPlayerRepository(final TrackingExecutorService executor) {
            super(executor, Mockito.mock(EntityManagerFactory.class));
            this.executor = executor;
        }

        void setFindResultSupplier(final Supplier<RPlayer> supplier) {
            this.findResultSupplier = supplier;
        }

        void setCreateResultSupplier(final Supplier<RPlayer> supplier) {
            this.createResultSupplier = supplier;
        }

        void setUpdateResultSupplier(final Supplier<RPlayer> supplier) {
            this.updateResultSupplier = supplier;
        }

        Map<String, Object> getLastAttributes() {
            return this.lastAttributes;
        }

        int getCreateInvocationCount() {
            return this.createInvocationCount;
        }

        int getUpdateInvocationCount() {
            return this.updateInvocationCount;
        }

        RPlayer getLastCreateEntity() {
            return this.lastCreateEntity.get();
        }

        RPlayer getLastUpdateEntity() {
            return this.lastUpdateEntity.get();
        }

        @Override
        protected CompletableFuture<RPlayer> findByAttributesAsync(final Map<String, Object> attributes) {
            this.lastAttributes = attributes;
            CompletableFuture<RPlayer> future = new CompletableFuture<>();
            this.executor.execute(() -> future.complete(this.findResultSupplier.get()));
            return future;
        }

        @Override
        protected CompletableFuture<RPlayer> createAsync(final RPlayer entity) {
            this.createInvocationCount++;
            this.lastCreateEntity.set(entity);
            CompletableFuture<RPlayer> future = new CompletableFuture<>();
            this.executor.execute(() -> future.complete(this.createResultSupplier.get()));
            return future;
        }

        @Override
        protected CompletableFuture<RPlayer> updateAsync(final RPlayer entity) {
            this.updateInvocationCount++;
            this.lastUpdateEntity.set(entity);
            CompletableFuture<RPlayer> future = new CompletableFuture<>();
            this.executor.execute(() -> future.complete(this.updateResultSupplier.get()));
            return future;
        }
    }

    private static final class TrackingExecutorService extends AbstractExecutorService {

        private final Deque<Runnable> tasks = new ArrayDeque<>();
        private boolean shutdown;

        @Override
        public void shutdown() {
            this.shutdown = true;
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
            if (this.shutdown) {
                throw new IllegalStateException("Executor already shutdown");
            }
            this.tasks.add(command);
        }

        void runNext() {
            Runnable next = this.tasks.poll();
            if (next != null) {
                next.run();
            }
        }
    }
}
