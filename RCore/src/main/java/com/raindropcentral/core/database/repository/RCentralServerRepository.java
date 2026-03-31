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

package com.raindropcentral.core.database.repository;

import com.raindropcentral.core.database.entity.central.RCentralServer;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing RCentralServer entities.
 * Handles persistence of server connection state to RaindropCentral platform.
 */
public class RCentralServerRepository extends CachedRepository<RCentralServer, Long, Long> {

    /**
     * Executes RCentralServerRepository.
     */
    public RCentralServerRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<RCentralServer> entityClass,
            @NotNull Function<RCentralServer, Long> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
    }

    /**
     * Finds a server by its unique server UUID.
     *
     * @param serverUuid the server UUID to search for
     * @return CompletableFuture containing the server if found
     */
    public CompletableFuture<Optional<RCentralServer>> findByServerUuid(final @NotNull UUID serverUuid) {
        return CompletableFuture.supplyAsync(
            () -> findByAttribute("serverUuid", serverUuid),
            getExecutorService()
        );
    }

    /**
     * Finds the currently connected server (if any).
     *
     * @return CompletableFuture containing the connected server if found
     */
    public CompletableFuture<Optional<RCentralServer>> findConnectedServer() {
        return CompletableFuture.supplyAsync(
            () -> findByAttribute("connectionStatus", RCentralServer.ConnectionStatus.CONNECTED),
            getExecutorService()
        );
    }
}
