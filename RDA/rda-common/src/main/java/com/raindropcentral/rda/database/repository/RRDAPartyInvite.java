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

import com.raindropcentral.rda.database.entity.RDAPartyInvite;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Cached repository for persisted RDA party invites.
 *
 * @author Codex
 * @since 1.3.0
 * @version 1.3.0
 */
public class RRDAPartyInvite extends CachedRepository<RDAPartyInvite, Long, UUID> {

    /**
     * Creates the party-invite repository.
     *
     * @param executorService async executor
     * @param entityManagerFactory entity manager factory
     * @param entityClass managed entity class
     * @param keyExtractor cache key extractor
     */
    public RRDAPartyInvite(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory,
        final @NotNull Class<RDAPartyInvite> entityClass,
        final @NotNull Function<RDAPartyInvite, UUID> keyExtractor
    ) {
        super(executorService, entityManagerFactory, entityClass, keyExtractor);
    }

    /**
     * Finds a pending invite for the supplied party and target player.
     *
     * @param partyUuid party UUID
     * @param targetPlayerUuid target player UUID
     * @return matching pending invite, or {@code null} when none exists
     */
    public @Nullable RDAPartyInvite findPendingByPartyAndTarget(
        final @NotNull UUID partyUuid,
        final @NotNull UUID targetPlayerUuid
    ) {
        return this.findAll().stream()
            .filter(invite -> invite.getParty().getPartyUuid().equals(partyUuid))
            .filter(invite -> invite.getInvitedPlayerUuid().equals(targetPlayerUuid))
            .filter(RDAPartyInvite::isPending)
            .findFirst()
            .orElse(null);
    }

    /**
     * Finds every invite belonging to the supplied party.
     *
     * @param partyUuid party UUID
     * @return matching party invites
     */
    public @NotNull List<RDAPartyInvite> findAllByParty(final @NotNull UUID partyUuid) {
        return this.findAll().stream()
            .filter(invite -> invite.getParty().getPartyUuid().equals(partyUuid))
            .toList();
    }

    /**
     * Finds every pending invite addressed to the supplied player.
     *
     * @param targetPlayerUuid invited player UUID
     * @return matching pending invites
     */
    public @NotNull List<RDAPartyInvite> findPendingByInvitedPlayer(final @NotNull UUID targetPlayerUuid) {
        return this.findAll().stream()
            .filter(invite -> invite.getInvitedPlayerUuid().equals(targetPlayerUuid))
            .filter(RDAPartyInvite::isPending)
            .toList();
    }
}
