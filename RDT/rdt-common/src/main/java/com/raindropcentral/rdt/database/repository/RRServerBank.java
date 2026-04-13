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

import com.raindropcentral.rdt.database.entity.RServerBank;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Cached repository for the singleton {@link RServerBank} aggregate.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class RRServerBank extends CachedRepository<RServerBank, Long, String> {

    /**
     * Creates the server-bank repository.
     *
     * @param executorService async executor
     * @param entityManagerFactory entity manager factory
     */
    public RRServerBank(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executorService, entityManagerFactory, RServerBank.class, RServerBank::getBankKey);
    }

    /**
     * Finds the singleton server-bank aggregate by key.
     *
     * @param bankKey stable bank key
     * @return matching server-bank aggregate, or {@code null} when none exists
     */
    public @Nullable RServerBank findByBankKey(final @NotNull String bankKey) {
        return this.findByAttributes(Map.of("bankKey", bankKey.trim().toUpperCase(java.util.Locale.ROOT))).orElse(null);
    }

    /**
     * Returns the singleton default server-bank aggregate when it exists.
     *
     * @return default server-bank aggregate, or {@code null} when it has not been created yet
     */
    public @Nullable RServerBank findDefaultBank() {
        return this.findByBankKey(RServerBank.DEFAULT_BANK_KEY);
    }
}
