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

import com.raindropcentral.rdt.database.entity.TownInvite;
import com.raindropcentral.rdt.database.entity.TownInviteStatus;
import de.jexcellence.hibernate.repository.BaseRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Repository for persisted {@link TownInvite} records.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class RRTownInvite extends BaseRepository<TownInvite, Long> {

    /**
     * Creates the town invite repository.
     *
     * @param executorService async executor
     * @param entityManagerFactory entity manager factory
     */
    public RRTownInvite(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executorService, entityManagerFactory, TownInvite.class);
    }

    /**
     * Finds active invites for a player.
     *
     * @param playerUuid invited player UUID
     * @return active invites
     */
    public @NotNull List<TownInvite> findActiveInvites(final @NotNull UUID playerUuid) {
        return findAllByAttributes(Map.of(
            "invitedPlayerUuid", playerUuid,
            "status", TownInviteStatus.ACTIVE
        ));
    }
}
