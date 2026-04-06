/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdt.database.repository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.raindropcentral.rdt.database.entity.RTownChunk;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RRTownChunkTest {

    @Mock
    private ExecutorService executorService;

    @Mock
    private EntityManagerFactory entityManagerFactory;

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<RTownChunk> townChunkQuery;

    @Mock
    private TypedQuery<RTownChunk> townChunksQuery;

    @Mock
    private RTownChunk townChunk;

    @Test
    void findByChunkFetchesOwningTownForDetachedRuntimeUse() {
        final RRTownChunk repository = new TestRRTownChunkRepository(
            this.executorService,
            this.entityManagerFactory,
            this.entityManager
        );

        when(this.entityManager.createQuery(
            "select chunk from RTownChunk chunk join fetch chunk.town "
                + "where chunk.worldName = :worldName and chunk.xLoc = :chunkX and chunk.zLoc = :chunkZ",
            RTownChunk.class
        )).thenReturn(this.townChunkQuery);
        when(this.townChunkQuery.setParameter("worldName", "world")).thenReturn(this.townChunkQuery);
        when(this.townChunkQuery.setParameter("chunkX", 12)).thenReturn(this.townChunkQuery);
        when(this.townChunkQuery.setParameter("chunkZ", -4)).thenReturn(this.townChunkQuery);
        when(this.townChunkQuery.setMaxResults(1)).thenReturn(this.townChunkQuery);
        when(this.townChunkQuery.getResultStream()).thenReturn(Stream.of(this.townChunk));

        assertSame(this.townChunk, repository.findByChunk(" world ", 12, -4));

        verify(this.townChunkQuery).setParameter("worldName", "world");
        verify(this.townChunkQuery).setParameter("chunkX", 12);
        verify(this.townChunkQuery).setParameter("chunkZ", -4);
        verify(this.townChunkQuery).setMaxResults(1);
    }

    @Test
    void findByTownUuidFetchesOwningTownForReturnedChunks() {
        final RRTownChunk repository = new TestRRTownChunkRepository(
            this.executorService,
            this.entityManagerFactory,
            this.entityManager
        );
        final UUID townUuid = UUID.randomUUID();

        when(this.entityManager.createQuery(
            "select chunk from RTownChunk chunk join fetch chunk.town town "
                + "where town.townUuid = :townUuid order by chunk.worldName asc, chunk.xLoc asc, chunk.zLoc asc",
            RTownChunk.class
        )).thenReturn(this.townChunksQuery);
        when(this.townChunksQuery.setParameter("townUuid", townUuid)).thenReturn(this.townChunksQuery);
        when(this.townChunksQuery.getResultList()).thenReturn(List.of(this.townChunk));

        assertSame(this.townChunk, repository.findByTownUuid(townUuid).getFirst());

        verify(this.townChunksQuery).setParameter("townUuid", townUuid);
    }

    private static final class TestRRTownChunkRepository extends RRTownChunk {

        private final EntityManager entityManager;

        private TestRRTownChunkRepository(
            final ExecutorService executorService,
            final EntityManagerFactory entityManagerFactory,
            final EntityManager entityManager
        ) {
            super(executorService, entityManagerFactory);
            this.entityManager = entityManager;
        }

        @Override
        public <R> R executeInTransaction(final Function<EntityManager, R> action) {
            return action.apply(this.entityManager);
        }
    }
}
