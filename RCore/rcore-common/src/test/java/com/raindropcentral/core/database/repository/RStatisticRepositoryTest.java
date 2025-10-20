package com.raindropcentral.core.database.repository;

import com.raindropcentral.core.database.entity.statistic.RAbstractStatistic;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validates that {@link RStatisticRepository} wires its entity configuration correctly and pushes
 * inherited CRUD work through the provided executor. Repository consumers coordinating batch
 * updates should instrument cache invalidations and aggregate refreshes with structured log
 * messages before submission and after completion so operations staff can trace the workflow; the
 * executor hooks exercised here provide the natural extension points for those log statements.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.0
 */
class RStatisticRepositoryTest {

    private RecordingExecutor executor;
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUp() {
        this.executor = new RecordingExecutor();
        this.entityManagerFactory = mock(EntityManagerFactory.class);
    }

    @Test
    @DisplayName("Constructor binds statistic entity type and AbstractEntity#getId key extractor")
    void constructorConfiguresEntityClassAndCacheKey() throws Exception {
        RStatisticRepository repository = new RStatisticRepository(this.executor, this.entityManagerFactory);

        Class<?> resolvedEntity = (Class<?>) resolveField(
            repository,
            Class.class,
            candidate -> Objects.equals(RAbstractStatistic.class, candidate)
        );
        assertSame(RAbstractStatistic.class, resolvedEntity, "Repository must target RAbstractStatistic entities");

        RAbstractStatistic statistic = mock(RAbstractStatistic.class);
        when(statistic.getId()).thenReturn(42L);
        @SuppressWarnings("unchecked")
        Function<RAbstractStatistic, Long> keyExtractor = (Function<RAbstractStatistic, Long>) resolveField(
            repository,
            Function.class,
            value -> Objects.equals(42L, ((Function<RAbstractStatistic, Long>) value).apply(statistic))
        );

        assertEquals(42L, keyExtractor.apply(statistic), "Cache key extractor must delegate to AbstractEntity#getId");
    }

    @Test
    @DisplayName("Inherited CRUD futures run on the supplied executor")
    void inheritedCrudMethodsUseProvidedExecutor() throws Exception {
        EntityManager entityManager = mock(EntityManager.class);
        EntityTransaction transaction = mock(EntityTransaction.class);
        Cache cache = mock(Cache.class);

        when(this.entityManagerFactory.createEntityManager()).thenReturn(entityManager);
        when(this.entityManagerFactory.getCache()).thenReturn(cache);
        when(entityManager.getTransaction()).thenReturn(transaction);
        when(transaction.isActive()).thenReturn(false);
        when(entityManager.merge(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(entityManager.find(RAbstractStatistic.class, 7L)).thenReturn(new TestStatistic("stat", "core", 7L));

        doAnswer(invocation -> {
            TestStatistic stat = invocation.getArgument(0);
            assignEntityId(stat, 3L);
            return null;
        }).when(entityManager).persist(any());

        RStatisticRepository repository = new RStatisticRepository(this.executor, this.entityManagerFactory);
        TestStatistic statistic = new TestStatistic("damage", "combat", null);

        CompletableFuture<RAbstractStatistic> created = repository.createAsync(statistic);
        CompletableFuture<RAbstractStatistic> updated = repository.updateAsync(statistic.withId(5L));
        CompletableFuture<Void> deleted = repository.deleteAsync(7L);

        created.join();
        updated.join();
        deleted.join();

        assertTrue(this.executor.hasScheduled("createAsync"), "createAsync should run on the provided executor");
        assertTrue(this.executor.hasScheduled("updateAsync"), "updateAsync should run on the provided executor");
        assertTrue(this.executor.hasScheduled("deleteAsync"), "deleteAsync should run on the provided executor");
    }

    private static void assignEntityId(final AbstractEntity entity, final Long id) throws IllegalAccessException {
        Field idField = null;
        Class<?> current = entity.getClass();
        while (current != null && idField == null) {
            for (Field candidate : current.getDeclaredFields()) {
                if (candidate.getType() == Long.class || candidate.getType() == long.class) {
                    idField = candidate;
                    if ("id".equalsIgnoreCase(candidate.getName())) {
                        break;
                    }
                }
            }
            current = current.getSuperclass();
        }

        if (idField == null) {
            throw new IllegalStateException("Unable to locate identifier field on AbstractEntity");
        }

        idField.setAccessible(true);
        idField.set(entity, id);
    }

    private static Object resolveField(
        final Object target,
        final Class<?> type,
        final Predicate<Object> predicate
    ) throws IllegalAccessException {
        Class<?> current = target.getClass();
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(target);
                if (value != null && type.isInstance(value) && predicate.test(value)) {
                    return value;
                }
            }
            current = current.getSuperclass();
        }
        throw new IllegalStateException("Unable to locate field of type " + type.getName());
    }

    private static final class RecordingExecutor implements ExecutorService {

        private final ConcurrentLinkedQueue<String> invokedMethods = new ConcurrentLinkedQueue<>();
        private final AtomicInteger activeTasks = new AtomicInteger();

        @Override
        public void execute(final Runnable command) {
            Objects.requireNonNull(command, "command");
            this.invokedMethods.add(currentInvocation());
            try {
                this.activeTasks.incrementAndGet();
                command.run();
            } finally {
                this.activeTasks.decrementAndGet();
            }
        }

        boolean hasScheduled(final String method) {
            return this.invokedMethods.stream().anyMatch(name -> Objects.equals(name, method));
        }

        private static String currentInvocation() {
            for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                if (RStatisticRepository.class.getName().equals(element.getClassName())) {
                    return element.getMethodName();
                }
            }
            return "unknown";
        }

        @Override
        public void shutdown() {
            // no-op for tests
        }

        @Override
        public java.util.List<Runnable> shutdownNow() {
            return java.util.List.of();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return this.activeTasks.get() == 0;
        }

        @Override
        public boolean awaitTermination(final long timeout, final TimeUnit unit) {
            return true;
        }

        @Override
        public <T> Future<T> submit(final java.util.concurrent.Callable<T> task) {
            CompletableFuture<T> future = new CompletableFuture<>();
            execute(() -> {
                try {
                    future.complete(task.call());
                } catch (Exception ex) {
                    future.completeExceptionally(ex);
                }
            });
            return future;
        }

        @Override
        public <T> Future<T> submit(final Runnable task, final T result) {
            CompletableFuture<T> future = new CompletableFuture<>();
            execute(() -> {
                task.run();
                future.complete(result);
            });
            return future;
        }

        @Override
        public Future<?> submit(final Runnable task) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            execute(() -> {
                task.run();
                future.complete(null);
            });
            return future;
        }

        @Override
        public <T> java.util.List<Future<T>> invokeAll(final java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks) {
            throw new UnsupportedOperationException("invokeAll is not supported in tests");
        }

        @Override
        public <T> java.util.List<Future<T>> invokeAll(
            final java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks,
            final long timeout,
            final TimeUnit unit
        ) {
            throw new UnsupportedOperationException("invokeAll with timeout is not supported in tests");
        }

        @Override
        public <T> T invokeAny(final java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks) {
            throw new UnsupportedOperationException("invokeAny is not supported in tests");
        }

        @Override
        public <T> T invokeAny(
            final java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks,
            final long timeout,
            final TimeUnit unit
        ) {
            throw new UnsupportedOperationException("invokeAny with timeout is not supported in tests");
        }
    }

    private static final class TestStatistic extends RAbstractStatistic {

        private final Map<String, Object> payload;

        TestStatistic(final String identifier, final String plugin, final Long id) {
            super(identifier, plugin);
            this.payload = Map.of("value", 1);
            if (id != null) {
                try {
                    assignEntityId(this, id);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException(exception);
                }
            }
        }

        TestStatistic withId(final long id) {
            try {
                assignEntityId(this, id);
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException(exception);
            }
            return this;
        }

        @Override
        public Object getValue() {
            return this.payload;
        }
    }
}
