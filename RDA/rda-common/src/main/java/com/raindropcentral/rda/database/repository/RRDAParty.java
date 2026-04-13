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

import com.raindropcentral.rda.database.entity.RDAParty;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Cached repository for persisted RDA parties.
 *
 * @author Codex
 * @since 1.3.0
 * @version 1.3.0
 */
public class RRDAParty extends CachedRepository<RDAParty, Long, UUID> {

    /**
     * Creates the party repository.
     *
     * @param executorService async executor
     * @param entityManagerFactory entity manager factory
     * @param entityClass managed entity class
     * @param keyExtractor cache key extractor
     */
    public RRDAParty(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory,
        final @NotNull Class<RDAParty> entityClass,
        final @NotNull Function<RDAParty, UUID> keyExtractor
    ) {
        super(executorService, entityManagerFactory, entityClass, keyExtractor);
    }

    /**
     * Finds a party by its stable UUID.
     *
     * @param partyUuid party UUID
     * @return matching party, or {@code null} when none exists
     */
    public @Nullable RDAParty findByPartyUuid(final @NotNull UUID partyUuid) {
        return this.findByAttributes(Map.of("partyUuid", partyUuid)).orElse(null);
    }
}
