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

import com.raindropcentral.rdt.database.entity.NationInvite;
import com.raindropcentral.rdt.database.entity.NationInviteStatus;
import com.raindropcentral.rdt.database.entity.NationInviteType;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Cached repository for persisted {@link NationInvite} records.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class RRNationInvite extends CachedRepository<NationInvite, Long, Long> {

    /**
     * Creates the nation-invite repository.
     *
     * @param executorService async executor
     * @param entityManagerFactory entity manager factory
     */
    public RRNationInvite(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executorService, entityManagerFactory, NationInvite.class, NationInvite::getId);
    }

    /**
     * Finds every invite belonging to one nation.
     *
     * @param nationUuid nation UUID
     * @return matching nation invites
     */
    public @NotNull List<NationInvite> findByNationUuid(final @NotNull UUID nationUuid) {
        return this.findAll().stream()
            .filter(invite -> java.util.Objects.equals(invite.getNationUuid(), nationUuid))
            .toList();
    }

    /**
     * Finds every invite addressed to one town.
     *
     * @param targetTownUuid target town UUID
     * @return matching nation invites
     */
    public @NotNull List<NationInvite> findByTargetTownUuid(final @NotNull UUID targetTownUuid) {
        return this.findAll().stream()
            .filter(invite -> java.util.Objects.equals(invite.getTargetTownUuid(), targetTownUuid))
            .toList();
    }

    /**
     * Finds every pending invite addressed to one town.
     *
     * @param targetTownUuid target town UUID
     * @return matching pending invites
     */
    public @NotNull List<NationInvite> findPendingByTargetTownUuid(final @NotNull UUID targetTownUuid) {
        return this.findByTargetTownUuid(targetTownUuid).stream()
            .filter(NationInvite::isPending)
            .toList();
    }

    /**
     * Finds pending invites of one type for one nation.
     *
     * @param nationUuid nation UUID
     * @param inviteType invite type
     * @return matching pending invites
     */
    public @NotNull List<NationInvite> findPendingByNationUuid(
        final @NotNull UUID nationUuid,
        final @NotNull NationInviteType inviteType
    ) {
        return this.findByNationUuid(nationUuid).stream()
            .filter(invite -> invite.getStatus() == NationInviteStatus.PENDING)
            .filter(invite -> invite.getInviteType() == inviteType)
            .toList();
    }
}
