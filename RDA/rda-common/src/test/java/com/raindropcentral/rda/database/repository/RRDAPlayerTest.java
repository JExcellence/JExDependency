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

package com.raindropcentral.rda.database.repository;

import com.raindropcentral.rda.database.entity.RDAPlayer;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests {@link RRDAPlayer}.
 */
class RRDAPlayerTest {

    @Test
    void returnsExistingPlayerWithoutCreatingDuplicateRow() {
        final UUID playerUuid = UUID.randomUUID();
        final RDAPlayer existingPlayer = new RDAPlayer(playerUuid);
        final RRDAPlayer repository = this.createRepositorySpy();
        doReturn(existingPlayer).when(repository).findByPlayer(playerUuid);

        final RDAPlayer resolvedPlayer = repository.findOrCreateByPlayer(playerUuid);

        assertSame(existingPlayer, resolvedPlayer);
        verify(repository, never()).create(any(RDAPlayer.class));
    }

    @Test
    void returnsPersistedPlayerWhenConcurrentInsertWinsRace() {
        final UUID playerUuid = UUID.randomUUID();
        final RDAPlayer persistedPlayer = new RDAPlayer(playerUuid);
        final RRDAPlayer repository = this.createRepositorySpy();
        doReturn(null, null, persistedPlayer).when(repository).findByPlayer(playerUuid);
        doThrow(new RuntimeException("duplicate")).when(repository).create(any(RDAPlayer.class));

        final RDAPlayer resolvedPlayer = repository.findOrCreateByPlayer(playerUuid);

        assertSame(persistedPlayer, resolvedPlayer);
        verify(repository).create(any(RDAPlayer.class));
    }

    private RRDAPlayer createRepositorySpy() {
        final ExecutorService executorService = mock(ExecutorService.class);
        final EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);
        return spy(new RRDAPlayer(
            executorService,
            entityManagerFactory,
            RDAPlayer.class,
            RDAPlayer::getIdentifier
        ));
    }
}
