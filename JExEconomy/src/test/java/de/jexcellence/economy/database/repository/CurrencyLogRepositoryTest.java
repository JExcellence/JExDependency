package de.jexcellence.economy.database.repository;

import de.jexcellence.economy.database.entity.CurrencyLog;
import de.jexcellence.economy.type.ELogLevel;
import de.jexcellence.economy.type.ELogType;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrencyLogRepositoryTest {

    private ExecutorService executor;

    @Mock
    private EntityManagerFactory entityManagerFactory;

    private CurrencyLogRepository repository;

    @BeforeEach
    void setUp() {
        this.executor = new DirectExecutorService();
        this.repository = new CurrencyLogRepository(executor, entityManagerFactory);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        GenericCachedRepository.resetConstructorInvocation();
    }

    @Test
    void createPersistsAndCachesLogs() {
        CurrencyLog log = new CurrencyLog(ELogType.SYSTEM, ELogLevel.INFO, "create-test");
        setEntityId(log, 5L);

        CurrencyLog stored = repository.create(log);

        assertSame(log, stored, "create should return the supplied entity");
        assertSame(log, repository.getPersistenceSnapshot().get(5L), "Entity should be tracked in the persistence snapshot");
        assertSame(log, repository.getCacheSnapshot().get(5L), "Entity should be available in the cache snapshot");
        assertSame(log, repository.findByCacheKeyAsync("id", 5L).join(),
                "Cache lookup should resolve the newly cached entity");
    }

    @Test
    void updateRefreshesCachedLogInstance() {
        CurrencyLog original = new CurrencyLog(ELogType.SYSTEM, ELogLevel.INFO, "original");
        setEntityId(original, 7L);
        repository.create(original);

        CurrencyLog updated = new CurrencyLog(ELogType.SYSTEM, ELogLevel.INFO, "updated");
        setEntityId(updated, 7L);

        CurrencyLog result = repository.update(updated);

        assertSame(updated, result, "update should return the updated entity instance");
        assertSame(updated, repository.getPersistenceSnapshot().get(7L),
                "Persistence snapshot should contain the updated instance");
        assertSame(updated, repository.getCacheSnapshot().get(7L),
                "Cache snapshot should contain the updated instance");
    }

    @Test
    void deleteEvictsEntitiesFromCacheAndPersistence() {
        CurrencyLog log = new CurrencyLog(ELogType.SYSTEM, ELogLevel.INFO, "delete-test");
        setEntityId(log, 9L);
        repository.create(log);

        boolean deleted = repository.delete(9L);

        assertTrue(deleted, "delete should report successful removal when entity existed");
        assertFalse(repository.getPersistenceSnapshot().containsKey(9L), "Entity should be removed from persistence snapshot");
        assertFalse(repository.getCacheSnapshot().containsKey(9L), "Entity should be removed from cache snapshot");
        assertNull(repository.findByCacheKeyAsync("id", 9L).join(), "Cache lookup should return null after eviction");
    }

    @Test
    void findByPlayerAndTimeRangeReturnsResultsFromEntityManager() {
        EntityManager entityManager = mock(EntityManager.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        CriteriaQuery<CurrencyLog> criteriaQuery = mock(CriteriaQuery.class);
        Root<CurrencyLog> root = mock(Root.class);
        Path<UUID> playerPath = mock(Path.class);
        Path<LocalDateTime> timestampPath = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        Order order = mock(Order.class);
        TypedQuery<CurrencyLog> typedQuery = mock(TypedQuery.class);

        UUID playerUuid = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();

        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createQuery(CurrencyLog.class)).thenReturn(criteriaQuery);
        when(criteriaQuery.from(CurrencyLog.class)).thenReturn(root);
        when(root.get("playerUuid")).thenReturn(playerPath);
        when(root.get("timestamp")).thenReturn(timestampPath);
        when(criteriaBuilder.equal(playerPath, playerUuid)).thenReturn(predicate);
        when(criteriaBuilder.greaterThanOrEqualTo(timestampPath, start)).thenReturn(predicate);
        when(criteriaBuilder.lessThan(timestampPath, end)).thenReturn(predicate);
        when(criteriaBuilder.desc(timestampPath)).thenReturn(order);
        when(criteriaQuery.where(any(Predicate[].class))).thenReturn(criteriaQuery);
        when(criteriaQuery.orderBy(any(Order[].class))).thenReturn(criteriaQuery);
        when(entityManager.createQuery(criteriaQuery)).thenReturn(typedQuery);

        List<CurrencyLog> expected = List.of(new CurrencyLog(ELogType.TRANSACTION, ELogLevel.INFO, "first"));
        when(typedQuery.getResultList()).thenReturn(expected);

        List<CurrencyLog> actual = repository.findByPlayerAndTimeRange(playerUuid, start, end);

        assertSame(expected, actual, "Repository should return results from the typed query");
        verify(typedQuery).getResultList();
        verify(entityManager).close();
    }

    @Test
    void findMostActivePlayersByTransactionCountRespectsLimit() {
        EntityManager entityManager = mock(EntityManager.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        CriteriaQuery<UUID> criteriaQuery = mock(CriteriaQuery.class);
        Root<CurrencyLog> root = mock(Root.class);
        Path<Object> currencyPath = mock(Path.class);
        Path<Long> currencyIdPath = mock(Path.class);
        Path<ELogType> logTypePath = mock(Path.class);
        Path<UUID> playerPath = mock(Path.class);
        Path<LocalDateTime> timestampPath = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        Expression<Long> countExpression = mock(Expression.class);
        Order order = mock(Order.class);
        TypedQuery<UUID> typedQuery = mock(TypedQuery.class);

        Long currencyId = 42L;
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        int limit = 3;

        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createQuery(UUID.class)).thenReturn(criteriaQuery);
        when(criteriaQuery.from(CurrencyLog.class)).thenReturn(root);
        when(root.get("currency")).thenReturn(currencyPath);
        when(currencyPath.get("id")).thenReturn(currencyIdPath);
        when(root.get("logType")).thenReturn(logTypePath);
        when(root.get("playerUuid")).thenReturn(playerPath);
        when(root.get("timestamp")).thenReturn(timestampPath);
        when(criteriaBuilder.equal(currencyIdPath, currencyId)).thenReturn(predicate);
        when(criteriaBuilder.equal(logTypePath, ELogType.TRANSACTION)).thenReturn(predicate);
        when(criteriaBuilder.isNotNull(playerPath)).thenReturn(predicate);
        when(criteriaBuilder.greaterThanOrEqualTo(timestampPath, start)).thenReturn(predicate);
        when(criteriaBuilder.lessThan(timestampPath, end)).thenReturn(predicate);
        when(criteriaBuilder.count(root)).thenReturn(countExpression);
        when(criteriaBuilder.desc(countExpression)).thenReturn(order);
        when(criteriaQuery.select(playerPath)).thenReturn(criteriaQuery);
        when(criteriaQuery.where(any(Predicate[].class))).thenReturn(criteriaQuery);
        when(criteriaQuery.groupBy(playerPath)).thenReturn(criteriaQuery);
        when(criteriaQuery.orderBy(any(Order[].class))).thenReturn(criteriaQuery);
        when(entityManager.createQuery(criteriaQuery)).thenReturn(typedQuery);
        when(typedQuery.setMaxResults(limit)).thenReturn(typedQuery);

        List<UUID> expected = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(typedQuery.getResultList()).thenReturn(expected);

        List<UUID> actual = repository.findMostActivePlayersByTransactionCount(currencyId, start, end, limit);

        assertEquals(expected, actual, "Repository should surface player UUID results from the query");
        verify(criteriaBuilder).equal(currencyIdPath, currencyId);
        verify(criteriaBuilder).equal(logTypePath, ELogType.TRANSACTION);
        verify(criteriaBuilder).isNotNull(playerPath);
        verify(typedQuery).setMaxResults(limit);
        verify(entityManager).close();
    }

    @Test
    void findByCriteriaAppliesFiltersAndPagination() {
        EntityManager entityManager = mock(EntityManager.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        CriteriaQuery<CurrencyLog> criteriaQuery = mock(CriteriaQuery.class);
        Root<CurrencyLog> root = mock(Root.class);
        Path<ELogType> logTypePath = mock(Path.class);
        Path<ELogLevel> logLevelPath = mock(Path.class);
        Path<UUID> playerPath = mock(Path.class);
        Path<Object> currencyPath = mock(Path.class);
        Path<Long> currencyIdPath = mock(Path.class);
        Path<LocalDateTime> timestampPath = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        Order order = mock(Order.class);
        TypedQuery<CurrencyLog> typedQuery = mock(TypedQuery.class);

        ELogType logType = ELogType.SYSTEM;
        ELogLevel logLevel = ELogLevel.ERROR;
        UUID playerUuid = UUID.randomUUID();
        Long currencyId = 24L;
        LocalDateTime startTime = LocalDateTime.now().minusHours(3);
        LocalDateTime endTime = LocalDateTime.now();
        int limit = 5;

        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createQuery(CurrencyLog.class)).thenReturn(criteriaQuery);
        when(criteriaQuery.from(CurrencyLog.class)).thenReturn(root);
        when(root.get("logType")).thenReturn(logTypePath);
        when(root.get("logLevel")).thenReturn(logLevelPath);
        when(root.get("playerUuid")).thenReturn(playerPath);
        when(root.get("currency")).thenReturn(currencyPath);
        when(currencyPath.get("id")).thenReturn(currencyIdPath);
        when(root.get("timestamp")).thenReturn(timestampPath);
        when(criteriaBuilder.equal(logTypePath, logType)).thenReturn(predicate);
        when(criteriaBuilder.equal(logLevelPath, logLevel)).thenReturn(predicate);
        when(criteriaBuilder.equal(playerPath, playerUuid)).thenReturn(predicate);
        when(criteriaBuilder.equal(currencyIdPath, currencyId)).thenReturn(predicate);
        when(criteriaBuilder.greaterThanOrEqualTo(timestampPath, startTime)).thenReturn(predicate);
        when(criteriaBuilder.lessThan(timestampPath, endTime)).thenReturn(predicate);
        when(criteriaBuilder.desc(timestampPath)).thenReturn(order);
        when(criteriaQuery.where(any(Predicate[].class))).thenReturn(criteriaQuery);
        when(criteriaQuery.orderBy(any(Order[].class))).thenReturn(criteriaQuery);
        when(entityManager.createQuery(criteriaQuery)).thenReturn(typedQuery);
        when(typedQuery.setMaxResults(limit)).thenReturn(typedQuery);

        List<CurrencyLog> expected = List.of(
                new CurrencyLog(ELogType.SYSTEM, ELogLevel.INFO, "first"),
                new CurrencyLog(ELogType.ERROR, ELogLevel.ERROR, "second")
        );
        when(typedQuery.getResultList()).thenReturn(expected);

        List<CurrencyLog> actual = repository.findByCriteria(
                logType,
                logLevel,
                playerUuid,
                currencyId,
                startTime,
                endTime,
                limit
        );

        assertSame(expected, actual, "Repository should return results from the criteria query");
        verify(criteriaBuilder).equal(logTypePath, logType);
        verify(criteriaBuilder).equal(logLevelPath, logLevel);
        verify(criteriaBuilder).equal(playerPath, playerUuid);
        verify(criteriaBuilder).equal(currencyIdPath, currencyId);
        verify(criteriaBuilder).greaterThanOrEqualTo(timestampPath, startTime);
        verify(criteriaBuilder).lessThan(timestampPath, endTime);
        verify(typedQuery).setMaxResults(limit);
        verify(entityManager).close();
    }

    private static void setEntityId(final CurrencyLog log, final long id) {
        try {
            Method setId = log.getClass().getSuperclass().getDeclaredMethod("setId", Long.class);
            setId.setAccessible(true);
            setId.invoke(log, id);
        } catch (final ReflectiveOperationException exception) {
            try {
                Field field = log.getClass().getSuperclass().getDeclaredField("id");
                field.setAccessible(true);
                field.set(log, id);
            } catch (final ReflectiveOperationException reflectionException) {
                throw new IllegalStateException("Unable to assign identifier to CurrencyLog", reflectionException);
            }
        }
    }

    private static final class DirectExecutorService extends AbstractExecutorService {

        private volatile boolean shutdown;

        @Override
        public void shutdown() {
            this.shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            this.shutdown = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return this.shutdown;
        }

        @Override
        public boolean isTerminated() {
            return this.shutdown;
        }

        @Override
        public boolean awaitTermination(final long timeout, final TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(final Runnable command) {
            command.run();
        }
    }
}
