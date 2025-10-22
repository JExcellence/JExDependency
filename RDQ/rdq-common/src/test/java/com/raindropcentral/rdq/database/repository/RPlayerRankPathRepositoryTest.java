package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.rank.RPlayerRankPath;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RPlayerRankPathRepositoryTest {

    @AfterEach
    void resetConstructorCapture() {
        GenericCachedRepository.resetConstructorInvocation();
    }

    @Test
    @DisplayName("Constructor forwards RPlayerRankPath::getId to GenericCachedRepository")
    void constructorRegistersIdExtractor() {
        GenericCachedRepository.resetConstructorInvocation();
        ExecutorService executor = mock(ExecutorService.class);
        EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);

        new RPlayerRankPathRepository(executor, entityManagerFactory);

        GenericCachedRepository.ConstructorInvocation<RPlayerRankPath, Long, Long> invocation =
                GenericCachedRepository.getLastConstructorInvocation();

        assertNotNull(invocation, "Constructor invocation should be captured");
        assertSame(executor, invocation.executor(), "Executor should be forwarded to the parent constructor");
        assertSame(entityManagerFactory, invocation.entityManagerFactory(),
                "EntityManagerFactory should be forwarded to the parent constructor");
        assertSame(RPlayerRankPath.class, invocation.entityType(),
                "Repository must advertise the RPlayerRankPath entity type");

        Function<RPlayerRankPath, Long> idExtractor = invocation.idExtractor();
        assertNotNull(idExtractor, "ID extractor should not be null");

        RPlayerRankPath entity = mock(RPlayerRankPath.class);
        when(entity.getId()).thenReturn(99L);

        assertEquals(99L, idExtractor.apply(entity),
                "ID extractor should delegate to RPlayerRankPath::getId");
        verify(entity).getId();
    }
}
