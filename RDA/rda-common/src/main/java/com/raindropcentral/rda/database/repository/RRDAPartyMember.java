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

import com.raindropcentral.rda.database.entity.RDAPartyMember;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Cached repository for persisted RDA party memberships.
 *
 * @author Codex
 * @since 1.3.0
 * @version 1.3.0
 */
public class RRDAPartyMember extends CachedRepository<RDAPartyMember, Long, String> {

    /**
     * Creates the party-member repository.
     *
     * @param executorService async executor
     * @param entityManagerFactory entity manager factory
     * @param entityClass managed entity class
     * @param keyExtractor cache key extractor
     */
    public RRDAPartyMember(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory,
        final @NotNull Class<RDAPartyMember> entityClass,
        final @NotNull Function<RDAPartyMember, String> keyExtractor
    ) {
        super(executorService, entityManagerFactory, entityClass, keyExtractor);
    }

    /**
     * Finds a membership row by player UUID.
     *
     * @param playerUuid player UUID
     * @return matching membership, or {@code null} when none exists
     */
    public @Nullable RDAPartyMember findByPlayer(final @NotNull UUID playerUuid) {
        return this.findByAttributes(Map.of("playerProfile.playerUuid", playerUuid)).orElse(null);
    }

    /**
     * Finds every membership row owned by the supplied party.
     *
     * @param partyUuid owning party UUID
     * @return membership rows owned by the party
     */
    public @NotNull List<RDAPartyMember> findAllByParty(final @NotNull UUID partyUuid) {
        return this.findAll().stream()
            .filter(member -> member.getParty().getPartyUuid().equals(partyUuid))
            .toList();
    }
}
