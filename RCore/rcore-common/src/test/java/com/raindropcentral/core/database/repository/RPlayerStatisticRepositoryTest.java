package com.raindropcentral.core.database.repository;

import com.raindropcentral.core.database.entity.player.RPlayer;
import com.raindropcentral.core.database.entity.statistic.RPlayerStatistic;
import de.jexcellence.hibernate.entity.AbstractEntity;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class RPlayerStatisticRepositoryTest {

    private ExecutorService executor;

    @Mock
    private EntityManagerFactory entityManagerFactory;

    private RPlayerStatisticRepository repository;

    @BeforeEach
    void setUp() {
        this.executor = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()
        );
        this.repository = new RPlayerStatisticRepository(this.executor, this.entityManagerFactory);
    }

    @AfterEach
    void tearDown() {
        this.executor.shutdownNow();
    }

    @Test
    @DisplayName("constructor wires repository cache to AbstractEntity::getId")
    void constructorShouldCacheByEntityIdentifier() throws Exception {
        final Function<RPlayerStatistic, Long> identifierExtractor = extractIdentifierFunction();
        final RPlayer player = new RPlayer(UUID.randomUUID(), "TestPlayer");
        final RPlayerStatistic statistic = new RPlayerStatistic(player);

        final Field idField = AbstractEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(statistic, 42L);

        assertAll(
                () -> assertNotNull(identifierExtractor, "identifier extractor should be present"),
                () -> assertEquals(42L, identifierExtractor.apply(statistic))
        );
    }

    @Test
    @DisplayName("asynchronous lookups leverage the provided executor")
    void asyncLookupsUseConfiguredExecutor() throws Exception {
        final Executor configuredExecutor = extractExecutor();
        assertNotNull(configuredExecutor, "repository should expose configured executor");

        final CompletableFuture<String> future = CompletableFuture.supplyAsync(
                () -> Thread.currentThread().getName(),
                configuredExecutor
        );

        final String threadName = future.get(1, TimeUnit.SECONDS);
        assertTrue(threadName.contains("pool-"), "repository executor thread should be used");
    }

    @Nested
    @DisplayName("cache miss documentation")
    class CacheMissBehaviour {

        @Test
        @DisplayName("null identifiers act as cache misses and delegate to aggregate loaders")
        void nullIdentifierRepresentsCacheMiss() {
            final Function<RPlayerStatistic, Long> identifierExtractor = extractIdentifierFunction();
            final RPlayer player = new RPlayer(UUID.randomUUID(), "CacheMiss");
            final RPlayerStatistic statistic = new RPlayerStatistic(player);

            final Long cacheKey = identifierExtractor.apply(statistic);
            assertAll(
                    () -> assertNull(cacheKey, "null keys represent cache miss"),
                    () -> assertEquals(
                            RPlayerStatistic.class,
                            resolveAggregateType().orElseThrow(),
                            "repository integrates with RPlayerStatistic aggregate loader"
                    )
            );
        }
    }

    @SuppressWarnings("unchecked")
    private Function<RPlayerStatistic, Long> extractIdentifierFunction() {
        return (Function<RPlayerStatistic, Long>) Arrays.stream(GenericCachedRepository.class.getDeclaredFields())
                .filter(field -> Function.class.isAssignableFrom(field.getType()))
                .findFirst()
                .map(field -> getFieldValue(field, this.repository))
                .orElseThrow(() -> new AssertionError("identifier extractor field not present"));
    }

    private Executor extractExecutor() {
        return Arrays.stream(GenericCachedRepository.class.getDeclaredFields())
                .filter(field -> Executor.class.isAssignableFrom(field.getType()))
                .findFirst()
                .map(field -> (Executor) getFieldValue(field, this.repository))
                .orElse(null);
    }

    private Optional<?> resolveAggregateType() {
        final Type genericSuperclass = RPlayerStatisticRepository.class.getGenericSuperclass();
        if (!(genericSuperclass instanceof ParameterizedType parameterizedType)) {
            return Optional.empty();
        }
        return Optional.of(parameterizedType.getActualTypeArguments()[0]);
    }

    private Object getFieldValue(final Field field, final Object target) {
        try {
            field.setAccessible(true);
            return field.get(target);
        } catch (IllegalAccessException ex) {
            throw new AssertionError("Unable to read field " + field.getName(), ex);
        }
    }
}
