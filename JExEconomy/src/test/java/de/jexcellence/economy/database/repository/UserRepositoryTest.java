package de.jexcellence.economy.database.repository;

import de.jexcellence.economy.database.entity.User;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class UserRepositoryTest {

    private TrackingExecutorService executor;
    private TestUserRepository repository;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        this.executor = new TrackingExecutorService();
        this.repository = new TestUserRepository(this.executor);
        this.sampleUser = new User(UUID.randomUUID(), "Notch1234");
    }

    @Test
    void findByUuidAsyncDelegatesToUniqueIdAttribute() {
        this.repository.setAsyncFindSupplier(() -> this.sampleUser);

        CompletableFuture<Optional<User>> future = this.repository.findByUuidAsync(this.sampleUser.getUniqueId());

        assertFalse(future.isDone(), "Future should not complete before executor runs");
        Map<String, Object> attributes = this.repository.getLastAsyncAttributes();
        assertNotNull(attributes, "Attributes should be captured for cache lookup");
        assertEquals(Map.of("uniqueId", this.sampleUser.getUniqueId()), attributes);

        this.executor.runNext();

        assertTrue(future.isDone(), "Future should complete after executor task runs");
        assertEquals(Optional.of(this.sampleUser), future.join());
    }

    @Test
    void findByUuidAsyncReturnsEmptyWhenUserMissing() {
        this.repository.setAsyncFindSupplier(() -> null);

        CompletableFuture<Optional<User>> future = this.repository.findByUuidAsync(this.sampleUser.getUniqueId());

        assertFalse(future.isDone(), "Future should not complete before executor runs");

        this.executor.runNext();

        assertTrue(future.isDone(), "Future should complete after executor task runs");
        assertEquals(Optional.empty(), future.join());
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
    void findByUuidDelegatesToSynchronousAttributesLookup() {
        this.repository.setSyncFindSupplier(() -> this.sampleUser);

        Optional<User> result = this.repository.findByUuid(this.sampleUser.getUniqueId());

        Map<String, Object> attributes = this.repository.getLastSyncAttributes();
        assertNotNull(attributes, "Attributes should be captured for synchronous lookup");
        assertEquals(Map.of("uniqueId", this.sampleUser.getUniqueId()), attributes);
        assertEquals(Optional.of(this.sampleUser), result);
    }

    @Test
    void findByUuidReturnsEmptyWhenMissing() {
        this.repository.setSyncFindSupplier(() -> null);

        Optional<User> result = this.repository.findByUuid(this.sampleUser.getUniqueId());

        assertEquals(Optional.empty(), result);
    }

    @Test
    void findByUuidRejectsNullInput() {
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> this.repository.findByUuid(null)
        );
        assertEquals("uniqueId cannot be null", exception.getMessage());
    }

    @Test
    void findByNameAsyncDelegatesToPlayerNameAttribute() {
        this.repository.setAsyncFindSupplier(() -> this.sampleUser);

        CompletableFuture<Optional<User>> future = this.repository.findByNameAsync(this.sampleUser.getPlayerName());

        assertFalse(future.isDone(), "Future should not complete before executor runs");
        Map<String, Object> attributes = this.repository.getLastAsyncAttributes();
        assertNotNull(attributes, "Attributes should be captured for cache lookup");
        assertEquals(Map.of("playerName", this.sampleUser.getPlayerName()), attributes);

        this.executor.runNext();

        assertTrue(future.isDone(), "Future should complete after executor task runs");
        assertEquals(Optional.of(this.sampleUser), future.join());
    }

    @Test
    void findByNameAsyncReturnsEmptyWhenUserMissing() {
        this.repository.setAsyncFindSupplier(() -> null);

        CompletableFuture<Optional<User>> future = this.repository.findByNameAsync(this.sampleUser.getPlayerName());

        assertFalse(future.isDone(), "Future should not complete before executor runs");

        this.executor.runNext();

        assertTrue(future.isDone(), "Future should complete after executor task runs");
        assertEquals(Optional.empty(), future.join());
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
    void findByNameDelegatesToSynchronousAttributesLookup() {
        this.repository.setSyncFindSupplier(() -> this.sampleUser);

        Optional<User> result = this.repository.findByName(this.sampleUser.getPlayerName());

        Map<String, Object> attributes = this.repository.getLastSyncAttributes();
        assertNotNull(attributes, "Attributes should be captured for synchronous lookup");
        assertEquals(Map.of("playerName", this.sampleUser.getPlayerName()), attributes);
        assertEquals(Optional.of(this.sampleUser), result);
    }

    @Test
    void findByNameReturnsEmptyWhenMissing() {
        this.repository.setSyncFindSupplier(() -> null);

        Optional<User> result = this.repository.findByName(this.sampleUser.getPlayerName());

        assertEquals(Optional.empty(), result);
    }

    @Test
    void findByNameRejectsNullInput() {
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> this.repository.findByName(null)
        );
        assertEquals("playerName cannot be null", exception.getMessage());
    }

    @Test
    void existsByUuidAsyncResolvesTrueWhenUserPresent() {
        this.repository.setAsyncFindSupplier(() -> this.sampleUser);

        CompletableFuture<Boolean> future = this.repository.existsByUuidAsync(this.sampleUser.getUniqueId());

        assertFalse(future.isDone(), "Future should not complete before executor runs");

        this.executor.runNext();

        assertTrue(future.isDone(), "Future should complete after executor task runs");
        assertTrue(future.join());
    }

    @Test
    void existsByUuidAsyncResolvesFalseWhenUserMissing() {
        this.repository.setAsyncFindSupplier(() -> null);

        CompletableFuture<Boolean> future = this.repository.existsByUuidAsync(this.sampleUser.getUniqueId());

        assertFalse(future.isDone(), "Future should not complete before executor runs");

        this.executor.runNext();

        assertTrue(future.isDone(), "Future should complete after executor task runs");
        assertFalse(future.join());
    }

    @Test
    void createOrUpdateAsyncBranchesToCreateWhenUserMissing() {
        this.repository.setAsyncFindSupplier(() -> null);
        this.repository.setCreateSupplier(() -> this.sampleUser);

        CompletableFuture<User> future = this.repository.createOrUpdateAsync(this.sampleUser);

        assertFalse(future.isDone(), "Future should not complete before executor runs");

        this.executor.runNext();
        assertEquals(1, this.repository.getCreateInvocationCount(), "Create should be invoked when cache misses");
        assertEquals(0, this.repository.getUpdateInvocationCount(), "Update should not be invoked when cache misses");
        assertSame(this.sampleUser, this.repository.getLastCreateEntity());
        assertFalse(future.isDone(), "Create stage should still be pending until executor runs");

        this.executor.runNext();

        assertTrue(future.isDone(), "Future should complete after create task runs");
        assertEquals(this.sampleUser, future.join());
    }

    @Test
    void createOrUpdateAsyncBranchesToUpdateWhenUserExists() {
        this.repository.setAsyncFindSupplier(() -> this.sampleUser);
        User updated = new User(this.sampleUser.getUniqueId(), "UpdatedName");
        this.repository.setUpdateSupplier(() -> updated);

        CompletableFuture<User> future = this.repository.createOrUpdateAsync(this.sampleUser);

        assertFalse(future.isDone(), "Future should not complete before executor runs");

        this.executor.runNext();
        assertEquals(0, this.repository.getCreateInvocationCount(), "Create should not be invoked when cache hits");
        assertEquals(1, this.repository.getUpdateInvocationCount(), "Update should be invoked when cache hits");
        assertSame(this.sampleUser, this.repository.getLastUpdateEntity());
        assertFalse(future.isDone(), "Update stage should still be pending until executor runs");

        this.executor.runNext();

        assertTrue(future.isDone(), "Future should complete after update task runs");
        assertEquals(updated, future.join());
    }

    @Test
    void createOrUpdateAsyncRejectsNullInput() {
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> this.repository.createOrUpdateAsync(null)
        );
        assertEquals("user cannot be null", exception.getMessage());
    }

    @Test
    void deleteAsyncRunsOnTrackingExecutor() {
        CompletableFuture<Void> future = this.repository.deleteAsync(87L);

        assertFalse(future.isDone(), "Future should not complete before executor runs");
        assertEquals(1, this.repository.getDeleteInvocationCount());
        assertEquals(87L, this.repository.getLastDeletedId());

        this.executor.runNext();

        assertTrue(future.isDone(), "Future should complete after delete task runs");
    }

    private static final class TestUserRepository extends UserRepository {

        private final TrackingExecutorService executor;
        private Supplier<User> asyncFindSupplier = () -> null;
        private Supplier<User> syncFindSupplier = () -> null;
        private Supplier<User> createSupplier = () -> null;
        private Supplier<User> updateSupplier = () -> null;
        private Map<String, Object> lastAsyncAttributes;
        private Map<String, Object> lastSyncAttributes;
        private final AtomicReference<User> lastCreateEntity = new AtomicReference<>();
        private final AtomicReference<User> lastUpdateEntity = new AtomicReference<>();
        private int createInvocationCount;
        private int updateInvocationCount;
        private int deleteInvocationCount;
        private Long lastDeletedId;

        private TestUserRepository(final TrackingExecutorService executor) {
            super(executor, mock(EntityManagerFactory.class));
            this.executor = executor;
        }

        void setAsyncFindSupplier(final Supplier<User> supplier) {
            this.asyncFindSupplier = supplier;
        }

        void setSyncFindSupplier(final Supplier<User> supplier) {
            this.syncFindSupplier = supplier;
        }

        void setCreateSupplier(final Supplier<User> supplier) {
            this.createSupplier = supplier;
        }

        void setUpdateSupplier(final Supplier<User> supplier) {
            this.updateSupplier = supplier;
        }

        Map<String, Object> getLastAsyncAttributes() {
            return this.lastAsyncAttributes;
        }

        Map<String, Object> getLastSyncAttributes() {
            return this.lastSyncAttributes;
        }

        int getCreateInvocationCount() {
            return this.createInvocationCount;
        }

        int getUpdateInvocationCount() {
            return this.updateInvocationCount;
        }

        int getDeleteInvocationCount() {
            return this.deleteInvocationCount;
        }

        User getLastCreateEntity() {
            return this.lastCreateEntity.get();
        }

        User getLastUpdateEntity() {
            return this.lastUpdateEntity.get();
        }

        Long getLastDeletedId() {
            return this.lastDeletedId;
        }

        @Override
        protected CompletableFuture<User> findByAttributesAsync(final Map<String, Object> attributes) {
            this.lastAsyncAttributes = attributes;
            CompletableFuture<User> future = new CompletableFuture<>();
            this.executor.execute(() -> future.complete(this.asyncFindSupplier.get()));
            return future;
        }

        @Override
        protected User findByAttributes(final Map<String, Object> attributes) {
            this.lastSyncAttributes = attributes;
            return this.syncFindSupplier.get();
        }

        @Override
        protected CompletableFuture<User> createAsync(final User entity) {
            this.createInvocationCount++;
            this.lastCreateEntity.set(entity);
            CompletableFuture<User> future = new CompletableFuture<>();
            this.executor.execute(() -> future.complete(this.createSupplier.get()));
            return future;
        }

        @Override
        protected CompletableFuture<User> updateAsync(final User entity) {
            this.updateInvocationCount++;
            this.lastUpdateEntity.set(entity);
            CompletableFuture<User> future = new CompletableFuture<>();
            this.executor.execute(() -> future.complete(this.updateSupplier.get()));
            return future;
        }

        @Override
        public CompletableFuture<Void> deleteAsync(final Long id) {
            this.deleteInvocationCount++;
            this.lastDeletedId = id;
            CompletableFuture<Void> future = new CompletableFuture<>();
            this.executor.execute(() -> future.complete(null));
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
