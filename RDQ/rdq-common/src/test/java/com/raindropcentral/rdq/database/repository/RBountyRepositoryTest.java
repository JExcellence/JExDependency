package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RBountyRepositoryTest {

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
    void constructorWiresEntityClassAndCacheKeyExtractor() {
        RBountyRepository repository = new RBountyRepository(this.executor, this.entityManagerFactory);

        Class<?> entityClass = extractEntityClass(repository);
        assertSame(RBounty.class, entityClass, "Repository must configure the RBounty aggregate");

        Function<RBounty, Long> cacheKeyExtractor = extractCacheKeyFunction(repository);
        RBounty bounty = mock(RBounty.class);
        Long expectedId = 42L;
        when(bounty.getId()).thenReturn(expectedId);

        assertEquals(expectedId, cacheKeyExtractor.apply(bounty),
                "Cache key extractor should delegate to AbstractEntity::getId");
        verify(bounty).getId();
    }

    @Test
    void findByPlayerAsyncDelegatesToPlayerAttribute() {
        RBountyRepository repository = spy(new RBountyRepository(this.executor, this.entityManagerFactory));
        RDQPlayer player = mock(RDQPlayer.class);
        RBounty bounty = mock(RBounty.class);

        doReturn(CompletableFuture.completedFuture(bounty)).when(repository).findByAttributesAsync(anyMap());

        CompletableFuture<Optional<RBounty>> future = repository.findByPlayerAsync(player);
        Optional<RBounty> resolved = assertTimeoutPreemptively(Duration.ofSeconds(1), future::join);

        assertTrue(resolved.isPresent(), "Expected result to contain a bounty instance");
        assertSame(bounty, resolved.orElseThrow());

        ArgumentCaptor<Map<String, Object>> attributeCaptor = ArgumentCaptor.forClass(Map.class);
        verify(repository).findByAttributesAsync(attributeCaptor.capture());

        Map<String, Object> attributes = attributeCaptor.getValue();
        assertEquals(1, attributes.size(), "Player lookup should only provide one attribute");
        assertSame(player, attributes.get("player"));
    }

    @Test
    void findByPlayerUuidAsyncDelegatesToNestedUniqueIdAttribute() {
        RBountyRepository repository = spy(new RBountyRepository(this.executor, this.entityManagerFactory));
        UUID playerId = UUID.randomUUID();
        RBounty bounty = mock(RBounty.class);

        doReturn(CompletableFuture.completedFuture(bounty)).when(repository).findByAttributesAsync(anyMap());

        CompletableFuture<Optional<RBounty>> future = repository.findByPlayerAsync(playerId);
        Optional<RBounty> resolved = assertTimeoutPreemptively(Duration.ofSeconds(1), future::join);

        assertTrue(resolved.isPresent(), "Expected result to contain a bounty instance");
        assertSame(bounty, resolved.orElseThrow());

        ArgumentCaptor<Map<String, Object>> attributeCaptor = ArgumentCaptor.forClass(Map.class);
        verify(repository).findByAttributesAsync(attributeCaptor.capture());

        Map<String, Object> attributes = attributeCaptor.getValue();
        assertEquals(1, attributes.size(), "Player UUID lookup should only provide one attribute");
        assertSame(playerId, attributes.get("player.uniqueId"));
    }

    @Test
    void findByCommissionerAsyncDelegatesToCommissionerAttribute() {
        RBountyRepository repository = spy(new RBountyRepository(this.executor, this.entityManagerFactory));
        UUID commissioner = UUID.randomUUID();
        RBounty bounty = mock(RBounty.class);

        doReturn(CompletableFuture.completedFuture(bounty)).when(repository).findByAttributesAsync(anyMap());

        CompletableFuture<Optional<RBounty>> future = repository.findByCommissionerAsync(commissioner);
        Optional<RBounty> resolved = assertTimeoutPreemptively(Duration.ofSeconds(1), future::join);

        assertTrue(resolved.isPresent(), "Expected result to contain a bounty instance");
        assertSame(bounty, resolved.orElseThrow());

        ArgumentCaptor<Map<String, Object>> attributeCaptor = ArgumentCaptor.forClass(Map.class);
        verify(repository).findByAttributesAsync(attributeCaptor.capture());

        Map<String, Object> attributes = attributeCaptor.getValue();
        assertEquals(1, attributes.size(), "Commissioner lookup should only provide one attribute");
        assertSame(commissioner, attributes.get("commissioner"));
    }

    private static Class<?> extractEntityClass(RBountyRepository repository) {
        return Arrays.stream(GenericCachedRepository.class.getDeclaredFields())
                .filter(field -> Class.class.equals(field.getType()))
                .map(field -> (Class<?>) readField(field, repository))
                .filter(Objects::nonNull)
                .filter(RBounty.class::equals)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Entity class field not found on GenericCachedRepository"));
    }

    @SuppressWarnings("unchecked")
    private static Function<RBounty, Long> extractCacheKeyFunction(RBountyRepository repository) {
        return Arrays.stream(GenericCachedRepository.class.getDeclaredFields())
                .filter(field -> Function.class.isAssignableFrom(field.getType()))
                .map(field -> (Function<RBounty, Long>) readField(field, repository))
                .filter(Objects::nonNull)
                .filter(function -> {
                    RBounty probe = mock(RBounty.class);
                    Long expected = 99L;
                    when(probe.getId()).thenReturn(expected);
                    return expected.equals(function.apply(probe));
                })
                .findFirst()
                .orElseThrow(() -> new AssertionError("Cache key extractor not found on GenericCachedRepository"));
    }

    private static Object readField(Field field, RBountyRepository repository) {
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
