package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.perk.RPerk;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RPerkRepositoryTest {

    private ExecutorService executor;
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUp() {
        this.executor = Executors.newSingleThreadExecutor();
        this.entityManagerFactory = mock(EntityManagerFactory.class);
    }

    @AfterEach
    void tearDown() {
        this.executor.shutdownNow();
    }

    @Test
    void constructorConfiguresIdentifierCacheKeyExtractor() {
        RPerkRepository repository = new RPerkRepository(this.executor, this.entityManagerFactory);

        Function<RPerk, String> cacheKeyExtractor = extractCacheKeyFunction(repository);
        RPerk perk = mock(RPerk.class);
        String expectedIdentifier = "perk.speed";
        when(perk.getIdentifier()).thenReturn(expectedIdentifier);

        assertEquals(expectedIdentifier, cacheKeyExtractor.apply(perk),
                "Cache key extractor should delegate to RPerk::getIdentifier");
        verify(perk).getIdentifier();
    }

    @Test
    void findByIdentifierAsyncDelegatesToCacheKeyLookup() {
        RPerkRepository repository = spy(new RPerkRepository(this.executor, this.entityManagerFactory));
        RPerk perk = mock(RPerk.class);
        String identifier = "perk.flight";

        doReturn(CompletableFuture.completedFuture(perk))
                .when(repository)
                .findByCacheKeyAsync("identifier", identifier);

        CompletableFuture<Optional<RPerk>> future = repository.findByIdentifierAsync(identifier);
        Optional<RPerk> resolved = assertTimeoutPreemptively(Duration.ofSeconds(1), future::join);

        assertTrue(resolved.isPresent(), "Expected Optional to contain the resolved perk");
        assertSame(perk, resolved.orElseThrow());

        verify(repository).findByCacheKeyAsync("identifier", identifier);
    }

    @SuppressWarnings("unchecked")
    private static Function<RPerk, String> extractCacheKeyFunction(RPerkRepository repository) {
        return Arrays.stream(GenericCachedRepository.class.getDeclaredFields())
                .filter(field -> Function.class.isAssignableFrom(field.getType()))
                .map(field -> (Function<RPerk, String>) readField(field, repository))
                .filter(Objects::nonNull)
                .filter(function -> {
                    RPerk probe = mock(RPerk.class);
                    String expected = "perk.cache";
                    when(probe.getIdentifier()).thenReturn(expected);
                    return expected.equals(function.apply(probe));
                })
                .findFirst()
                .orElseThrow(() -> new AssertionError("Cache key extractor not found on GenericCachedRepository"));
    }

    private static Object readField(Field field, RPerkRepository repository) {
        boolean accessible = field.canAccess(repository);
        field.setAccessible(true);
        try {
            return field.get(repository);
        } catch (IllegalAccessException exception) {
            fail(exception);
            return null;
        } finally {
            field.setAccessible(accessible);
        }
    }
}
