package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.Query;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class RDQPlayerRepositoryTest {

    private ExecutorService executor;
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUp() {
        this.executor = Executors.newSingleThreadExecutor();
        this.entityManagerFactory = new InMemoryEntityManagerFactory();
    }

    @AfterEach
    void tearDown() {
        this.executor.shutdownNow();
    }

    @Test
    void findByUuidAsyncDelegatesToUniqueIdCacheKey() {
        RDQPlayerRepository repository = spy(new RDQPlayerRepository(this.executor, this.entityManagerFactory));
        UUID uniqueId = UUID.randomUUID();
        RDQPlayer player = new RDQPlayer(uniqueId, "DelegateTest");
        CompletableFuture<RDQPlayer> playerFuture = CompletableFuture.supplyAsync(() -> player, this.executor);

        doReturn(playerFuture).when(repository).findByCacheKeyAsync(eq("uniqueId"), eq(uniqueId));

        CompletableFuture<Optional<RDQPlayer>> result = repository.findByUuidAsync(uniqueId);
        Optional<RDQPlayer> resolved = assertTimeoutPreemptively(Duration.ofSeconds(1), result::join);

        assertTrue(resolved.isPresent(), "Expected the optional to contain the player");
        assertSame(player, resolved.orElseThrow());
        verify(repository).findByCacheKeyAsync("uniqueId", uniqueId);
    }

    @Test
    void findByNameAsyncDelegatesToPlayerNameAttribute() {
        RDQPlayerRepository repository = spy(new RDQPlayerRepository(this.executor, this.entityManagerFactory));
        UUID uniqueId = UUID.randomUUID();
        RDQPlayer player = new RDQPlayer(uniqueId, "CacheLookup");
        CompletableFuture<RDQPlayer> playerFuture = CompletableFuture.supplyAsync(() -> player, this.executor);

        doReturn(playerFuture).when(repository).findByAttributesAsync(anyMap());

        CompletableFuture<Optional<RDQPlayer>> result = repository.findByNameAsync("CacheLookup");
        Optional<RDQPlayer> resolved = assertTimeoutPreemptively(Duration.ofSeconds(1), result::join);

        assertTrue(resolved.isPresent(), "Expected the optional to contain the player");
        assertSame(player, resolved.orElseThrow());

        ArgumentCaptor<Map<String, Object>> attributeCaptor = ArgumentCaptor.forClass(Map.class);
        verify(repository).findByAttributesAsync(attributeCaptor.capture());
        assertEquals("CacheLookup", attributeCaptor.getValue().get("playerName"));
    }

    @Test
    void createOrUpdateAsyncCreatesWhenPlayerDoesNotExist() {
        RDQPlayerRepository repository = spy(new RDQPlayerRepository(this.executor, this.entityManagerFactory));
        UUID uniqueId = UUID.randomUUID();
        RDQPlayer player = new RDQPlayer(uniqueId, "NewPlayer");

        doReturn(CompletableFuture.completedFuture(false)).when(repository).existsByUuidAsync(uniqueId);
        CompletableFuture<RDQPlayer> createdFuture = CompletableFuture.supplyAsync(() -> player, this.executor);
        doReturn(createdFuture).when(repository).createAsync(player);

        CompletableFuture<RDQPlayer> result = repository.createOrUpdateAsync(player);
        RDQPlayer resolved = assertTimeoutPreemptively(Duration.ofSeconds(1), result::join);

        assertSame(player, resolved);
        verify(repository).existsByUuidAsync(uniqueId);
        verify(repository).createAsync(player);
        verify(repository, never()).updateAsync(any(RDQPlayer.class));
    }

    @Test
    void createOrUpdateAsyncUpdatesWhenPlayerExists() {
        RDQPlayerRepository repository = spy(new RDQPlayerRepository(this.executor, this.entityManagerFactory));
        UUID uniqueId = UUID.randomUUID();
        RDQPlayer player = new RDQPlayer(uniqueId, "ExistingPlayer");

        doReturn(CompletableFuture.completedFuture(true)).when(repository).existsByUuidAsync(uniqueId);
        CompletableFuture<RDQPlayer> updatedFuture = CompletableFuture.supplyAsync(() -> player, this.executor);
        doReturn(updatedFuture).when(repository).updateAsync(player);

        CompletableFuture<RDQPlayer> result = repository.createOrUpdateAsync(player);
        RDQPlayer resolved = assertTimeoutPreemptively(Duration.ofSeconds(1), result::join);

        assertSame(player, resolved);
        verify(repository).existsByUuidAsync(uniqueId);
        verify(repository).updateAsync(player);
        verify(repository, never()).createAsync(any(RDQPlayer.class));
    }

    private static final class InMemoryEntityManagerFactory implements EntityManagerFactory {

        @Override
        public EntityManager createEntityManager() {
            return Mockito.mock(EntityManager.class);
        }

        @Override
        public EntityManager createEntityManager(final Map map) {
            return Mockito.mock(EntityManager.class);
        }

        @Override
        public EntityManager createEntityManager(final SynchronizationType synchronizationType) {
            return Mockito.mock(EntityManager.class);
        }

        @Override
        public EntityManager createEntityManager(final SynchronizationType synchronizationType, final Map map) {
            return Mockito.mock(EntityManager.class);
        }

        @Override
        public CriteriaBuilder getCriteriaBuilder() {
            throw new UnsupportedOperationException("CriteriaBuilder is not supported in the in-memory stub");
        }

        @Override
        public Metamodel getMetamodel() {
            throw new UnsupportedOperationException("Metamodel is not supported in the in-memory stub");
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void close() {
            // Nothing to close for the in-memory stub
        }

        @Override
        public Map<String, Object> getProperties() {
            return Collections.emptyMap();
        }

        @Override
        public Cache getCache() {
            throw new UnsupportedOperationException("Cache is not supported in the in-memory stub");
        }

        @Override
        public PersistenceUnitUtil getPersistenceUnitUtil() {
            throw new UnsupportedOperationException("PersistenceUnitUtil is not supported in the in-memory stub");
        }

        @Override
        public void addNamedQuery(final String name, final Query query) {
            // Named queries are not tracked in the in-memory stub
        }

        @Override
        public <T> T unwrap(final Class<T> cls) {
            throw new UnsupportedOperationException("Unwrap is not supported in the in-memory stub");
        }

        @Override
        public <T> void addNamedEntityGraph(final String graphName, final EntityGraph<T> entityGraph) {
            // Named entity graphs are not tracked in the in-memory stub
        }
    }
}
