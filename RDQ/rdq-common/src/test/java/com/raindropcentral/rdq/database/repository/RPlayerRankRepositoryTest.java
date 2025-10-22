package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRank;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import de.jexcellence.hibernate.entity.AbstractEntity;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class RPlayerRankRepositoryTest {

    private ExecutorService executor;
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUp() {
        this.executor = Executors.newSingleThreadExecutor();
        this.entityManagerFactory = mock(EntityManagerFactory.class);
        GenericCachedRepository.resetConstructorInvocation();
    }

    @AfterEach
    void tearDown() {
        this.executor.shutdownNow();
        GenericCachedRepository.resetConstructorInvocation();
    }

    @Test
    @DisplayName("Constructor forwards entity metadata and identifier extractor to GenericCachedRepository")
    void constructorWiresEntityMetadata() {
        new RPlayerRankRepository(this.executor, this.entityManagerFactory);

        GenericCachedRepository.ConstructorInvocation<RPlayerRank, Long, Long> invocation =
                GenericCachedRepository.getLastConstructorInvocation();

        assertNotNull(invocation, "Constructor invocation should be recorded");
        assertSame(this.executor, invocation.executor(), "Executor should be forwarded to the parent constructor");
        assertSame(this.entityManagerFactory, invocation.entityManagerFactory(),
                "EntityManagerFactory should be forwarded to the parent constructor");
        assertSame(RPlayerRank.class, invocation.entityType(),
                "Repository must supply the RPlayerRank entity type to the parent constructor");

        Function<RPlayerRank, Long> idExtractor = invocation.idExtractor();
        assertNotNull(idExtractor, "Identifier extractor should be provided to the parent constructor");

        RPlayerRank entity = instantiatePlayerRank(91L);
        assertEquals(91L, idExtractor.apply(entity),
                "Identifier extractor should resolve AbstractEntity::getId");
    }

    @Test
    @DisplayName("findByPlayerAndRankTreeAsync delegates to attribute lookup with player and rankTree keys")
    void findByPlayerAndRankTreeAsyncDelegatesToAttributeLookup() {
        RPlayerRankRepository repository = spy(new RPlayerRankRepository(this.executor, this.entityManagerFactory));
        RDQPlayer player = mock(RDQPlayer.class);
        RRankTree rankTree = mock(RRankTree.class);
        RPlayerRank association = mock(RPlayerRank.class);

        doReturn(CompletableFuture.completedFuture(association)).when(repository).findByAttributesAsync(anyMap());

        CompletableFuture<Optional<RPlayerRank>> resultFuture = repository.findByPlayerAndRankTreeAsync(player, rankTree);
        Optional<RPlayerRank> result = assertTimeoutPreemptively(Duration.ofSeconds(1), resultFuture::join);

        assertTrue(result.isPresent(), "Expected the optional to contain the resolved association");
        assertSame(association, result.orElseThrow());

        ArgumentCaptor<Map<String, Object>> attributeCaptor = ArgumentCaptor.forClass(Map.class);
        verify(repository).findByAttributesAsync(attributeCaptor.capture());

        Map<String, Object> attributes = attributeCaptor.getValue();
        assertEquals(2, attributes.size(), "Attribute lookup should be performed with player and rankTree keys");
        assertSame(player, attributes.get("player"));
        assertSame(rankTree, attributes.get("rankTree"));
    }

    @Test
    @DisplayName("findAllByPlayerAsync delegates to list lookup with the player attribute")
    void findAllByPlayerAsyncDelegatesToListLookup() {
        RPlayerRankRepository repository = spy(new RPlayerRankRepository(this.executor, this.entityManagerFactory));
        RDQPlayer player = mock(RDQPlayer.class);
        List<RPlayerRank> associations = List.of(mock(RPlayerRank.class));

        doReturn(CompletableFuture.completedFuture(associations)).when(repository).findListByAttributesAsync(anyMap());

        CompletableFuture<List<RPlayerRank>> resultFuture = repository.findAllByPlayerAsync(player);
        List<RPlayerRank> result = assertTimeoutPreemptively(Duration.ofSeconds(1), resultFuture::join);

        assertSame(associations, result, "Expected the future to return the list provided by the delegate");

        ArgumentCaptor<Map<String, Object>> attributeCaptor = ArgumentCaptor.forClass(Map.class);
        verify(repository).findListByAttributesAsync(attributeCaptor.capture());

        Map<String, Object> attributes = attributeCaptor.getValue();
        assertEquals(1, attributes.size(), "List lookup should only include the player attribute");
        assertSame(player, attributes.get("player"));
    }

    @Test
    @DisplayName("findActiveByPlayerAsync delegates to attribute lookup with player and active flag")
    void findActiveByPlayerAsyncDelegatesToAttributeLookup() {
        RPlayerRankRepository repository = spy(new RPlayerRankRepository(this.executor, this.entityManagerFactory));
        RDQPlayer player = mock(RDQPlayer.class);
        RPlayerRank association = mock(RPlayerRank.class);

        doReturn(CompletableFuture.completedFuture(association)).when(repository).findByAttributesAsync(anyMap());

        CompletableFuture<Optional<RPlayerRank>> resultFuture = repository.findActiveByPlayerAsync(player);
        Optional<RPlayerRank> result = assertTimeoutPreemptively(Duration.ofSeconds(1), resultFuture::join);

        assertTrue(result.isPresent(), "Expected the optional to contain the resolved association");
        assertSame(association, result.orElseThrow());

        ArgumentCaptor<Map<String, Object>> attributeCaptor = ArgumentCaptor.forClass(Map.class);
        verify(repository).findByAttributesAsync(attributeCaptor.capture());

        Map<String, Object> attributes = attributeCaptor.getValue();
        assertEquals(2, attributes.size(), "Lookup should provide both the player and active attributes");
        assertSame(player, attributes.get("player"));
        assertEquals(Boolean.TRUE, attributes.get("isActive"));
    }

    private static RPlayerRank instantiatePlayerRank(final long id) {
        try {
            Constructor<RPlayerRank> constructor = RPlayerRank.class.getDeclaredConstructor();
            boolean accessible = constructor.canAccess(null);
            constructor.setAccessible(true);
            RPlayerRank rank = constructor.newInstance();
            constructor.setAccessible(accessible);

            Field idField = AbstractEntity.class.getDeclaredField("id");
            boolean fieldAccessible = idField.canAccess(rank);
            idField.setAccessible(true);
            idField.set(rank, id);
            idField.setAccessible(fieldAccessible);

            return rank;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to instantiate RPlayerRank for testing", exception);
        }
    }
}
