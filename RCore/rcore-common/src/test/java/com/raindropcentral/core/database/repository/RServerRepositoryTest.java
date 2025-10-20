package com.raindropcentral.core.database.repository;

import com.raindropcentral.core.database.entity.server.RServer;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests covering {@link RServerRepository} interactions with {@link GenericCachedRepository}.
 */
@ExtendWith(MockitoExtension.class)
class RServerRepositoryTest {

    @Mock
    private EntityManagerFactory entityManagerFactory;

    @Mock
    private EntityManager entityManager;

    @Mock
    private EntityTransaction entityTransaction;

    private RecordingExecutorService executor;

    private RServerRepository repository;

    private Map<UUID, RServer> persistedEntities;

    @BeforeEach
    void setUp() {
        this.executor = spy(new RecordingExecutorService());
        this.persistedEntities = new ConcurrentHashMap<>();

        lenient().when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
        lenient().when(entityManager.getTransaction()).thenReturn(entityTransaction);
        lenient().doNothing().when(entityManager).close();
        lenient().doNothing().when(entityManager).clear();
        lenient().doNothing().when(entityManager).flush();
        lenient().doNothing().when(entityTransaction).begin();
        lenient().doNothing().when(entityTransaction).commit();
        lenient().doNothing().when(entityTransaction).rollback();
        lenient().when(entityTransaction.isActive()).thenReturn(false);

        lenient().doAnswer(invocation -> {
            RServer server = invocation.getArgument(0);
            persistedEntities.put(server.getUniqueId(), server);
            return null;
        }).when(entityManager).persist(any(RServer.class));
        lenient().when(entityManager.merge(any(RServer.class))).thenAnswer(invocation -> {
            RServer server = invocation.getArgument(0);
            persistedEntities.put(server.getUniqueId(), server);
            return server;
        });

        this.repository = new RServerRepository(executor, entityManagerFactory);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    @DisplayName("Constructor wires entity metadata and UUID cache extractor")
    void constructorBindsSuperclassConfiguration() {
        Class<?> entityClass = extractEntityClass(repository);
        Function<RServer, UUID> cacheKeyExtractor = extractCacheKeyFunction(repository);

        assertSame(RServer.class, entityClass, "Repository must declare the RServer aggregate");

        UUID expectedId = UUID.randomUUID();
        RServer probe = new RServer(expectedId, "ConfigCheck");
        assertEquals(expectedId, cacheKeyExtractor.apply(probe),
            "Cache key extractor should rely on the RServer unique identifier");
    }

    @Test
    @DisplayName("createAsync persists entities and applies the UUID cache key")
    void createAsyncPersistsUsingCacheKey() {
        UUID uniqueId = UUID.randomUUID();
        RServer server = spy(new RServer(uniqueId, "CacheProbe"));

        RServer persisted = repository.createAsync(server).join();

        verify(server, atLeastOnce()).getUniqueId();
        verify(entityManager).persist(server);
        assertSame(server, persisted, "createAsync should resolve the managed instance");
        assertTrue(persistedEntities.containsKey(uniqueId),
            "The entity should be tracked by the UUID cache key after persistence");
    }

    @Test
    @DisplayName("Asynchronous persistence delegates to the supplied executor")
    void createAsyncUsesSuppliedExecutor() {
        RServer server = new RServer(UUID.randomUUID(), "ExecutorProbe");

        repository.createAsync(server).join();

        verify(executor, atLeastOnce()).execute(any());
        assertTrue(executor.executedTaskCount() >= 1,
            "createAsync should schedule work on the provided executor so logging hooks can observe execution");
    }

    private static Class<?> extractEntityClass(RServerRepository repository) {
        return Arrays.stream(GenericCachedRepository.class.getDeclaredFields())
            .filter(field -> Class.class.equals(field.getType()))
            .map(field -> (Class<?>) readField(field, repository))
            .filter(Objects::nonNull)
            .filter(RServer.class::equals)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Entity class field not found on GenericCachedRepository"));
    }

    @SuppressWarnings("unchecked")
    private static Function<RServer, UUID> extractCacheKeyFunction(RServerRepository repository) {
        return Arrays.stream(GenericCachedRepository.class.getDeclaredFields())
            .filter(field -> Function.class.isAssignableFrom(field.getType()))
            .map(field -> (Function<RServer, UUID>) readField(field, repository))
            .filter(Objects::nonNull)
            .filter(function -> {
                UUID probeId = UUID.randomUUID();
                RServer probe = new RServer(probeId, "KeyProbe");
                return probeId.equals(function.apply(probe));
            })
            .findFirst()
            .orElseThrow(() -> new AssertionError("Cache key extractor not found on GenericCachedRepository"));
    }

    private static Object readField(Field field, RServerRepository repository) {
        boolean accessible = field.canAccess(repository);
        field.setAccessible(true);
        try {
            return field.get(repository);
        } catch (IllegalAccessException ex) {
            fail(ex);
            return null;
        } finally {
            field.setAccessible(accessible);
        }
    }

    /**
     * Executor capturing task execution metadata for assertions.
     */
    private static final class RecordingExecutorService extends AbstractExecutorService {

        private final AtomicBoolean shutdown = new AtomicBoolean();
        private final AtomicInteger executedTasks = new AtomicInteger();
        private final List<String> executedThreadNames = new java.util.concurrent.CopyOnWriteArrayList<>();

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
            return isTerminated();
        }

        @Override
        public void execute(Runnable command) {
            executedTasks.incrementAndGet();
            command.run();
            executedThreadNames.add(Thread.currentThread().getName());
        }

        int executedTaskCount() {
            return executedTasks.get();
        }

        List<String> executedThreadNames() {
            return executedThreadNames;
        }

        @Override
        public boolean isTerminating() {
            return shutdown.get();
        }

        @Override
        public <T> Future<T> submit(java.util.concurrent.Callable<T> task) {
            throw new UnsupportedOperationException("submit is not used in these tests");
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            throw new UnsupportedOperationException("submit is not used in these tests");
        }

        @Override
        public Future<?> submit(Runnable task) {
            throw new UnsupportedOperationException("submit is not used in these tests");
        }
    }
}
