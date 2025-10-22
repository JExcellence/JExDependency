package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.rank.RRank;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RRankRepositoryTest {

    @AfterEach
    void resetConstructorCapture() {
        GenericCachedRepository.resetConstructorInvocation();
    }

    @Test
    @DisplayName("Constructor forwards entity metadata and identifier extractor to GenericCachedRepository")
    void constructorRegistersIdentifierExtractor() {
        GenericCachedRepository.resetConstructorInvocation();
        ExecutorService executor = mock(ExecutorService.class);
        EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);

        new RRankRepository(executor, entityManagerFactory);

        GenericCachedRepository.ConstructorInvocation<RRank, Object, Object> invocation =
                GenericCachedRepository.getLastConstructorInvocation();

        assertNotNull(invocation, "Constructor invocation should be captured");
        assertSame(executor, invocation.executor(), "Executor should be forwarded to the parent constructor");
        assertSame(entityManagerFactory, invocation.entityManagerFactory(),
                "EntityManagerFactory should be forwarded to the parent constructor");
        assertSame(RRank.class, invocation.entityType(),
                "Repository must advertise the RRank entity type");

        @SuppressWarnings("unchecked")
        Function<RRank, String> cacheKeyExtractor = (Function<RRank, String>) invocation.idExtractor();
        assertNotNull(cacheKeyExtractor, "Cache key extractor should not be null");

        RRank rank = mock(RRank.class);
        when(rank.getIdentifier()).thenReturn("emerald");

        assertEquals("emerald", cacheKeyExtractor.apply(rank),
                "Cache key extractor should resolve RRank::getIdentifier");
        verify(rank).getIdentifier();
    }

    @Test
    @DisplayName("findByIdentifierAsync delegates to cache lookup with identifier column")
    void findByIdentifierAsyncDelegatesToCacheLookup() {
        ExecutorService executor = mock(ExecutorService.class);
        EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);
        RRankRepository repository = spy(new RRankRepository(executor, entityManagerFactory));

        String identifier = "raindrop";
        RRank rank = mock(RRank.class);
        CompletableFuture<RRank> delegateResult = CompletableFuture.completedFuture(rank);

        doReturn(delegateResult).when(repository).findByCacheKeyAsync(anyString(), any());

        Optional<RRank> result = repository.findByIdentifierAsync(identifier).join();

        assertTrue(result.isPresent(), "Lookup should wrap delegate result in Optional");
        assertSame(rank, result.orElseThrow(), "Result should surface the entity provided by the delegate");

        ArgumentCaptor<String> fieldNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> cacheKeyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(repository).findByCacheKeyAsync(fieldNameCaptor.capture(), cacheKeyCaptor.capture());

        assertEquals("identifier", fieldNameCaptor.getValue(),
                "Lookup should forward the identifier field name");
        assertEquals(identifier, cacheKeyCaptor.getValue(),
                "Lookup should provide the identifier cache key value");
    }

    @Test
    @DisplayName("findByLuckPermsGroupAsync delegates to attribute lookup with group column")
    void findByLuckPermsGroupAsyncDelegatesToAttributeLookup() {
        ExecutorService executor = mock(ExecutorService.class);
        EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);
        RRankRepository repository = spy(new RRankRepository(executor, entityManagerFactory));

        String group = "vip";
        RRank rank = mock(RRank.class);
        CompletableFuture<RRank> delegateResult = CompletableFuture.completedFuture(rank);

        doReturn(delegateResult).when(repository).findByAttributesAsync(any());

        Optional<RRank> result = repository.findByLuckPermsGroupAsync(group).join();

        assertTrue(result.isPresent(), "Lookup should wrap delegate result in Optional");
        assertSame(rank, result.orElseThrow(), "Result should surface the entity provided by the delegate");

        ArgumentCaptor<Map<String, Object>> attributesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(repository).findByAttributesAsync(attributesCaptor.capture());

        Map<String, Object> attributes = attributesCaptor.getValue();
        assertEquals(1, attributes.size(), "Lookup should only supply a single attribute");
        assertEquals(group, attributes.get("assignedLuckPermsGroup"),
                "Lookup should forward the assignedLuckPermsGroup attribute");
    }
}
