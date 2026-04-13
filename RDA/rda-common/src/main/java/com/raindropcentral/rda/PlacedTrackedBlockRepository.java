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

package com.raindropcentral.rda;

import de.jexcellence.hibernate.entity.BaseEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Minimal repository contract required by placed tracked-block suppression services.
 *
 * @param <M> persisted placed-block marker type
 *
 * @author Codex
 * @since 1.0.0
 * @version 1.0.0
 */
public interface PlacedTrackedBlockRepository<M extends BaseEntity> {

    /**
     * Returns every persisted placed-block marker.
     *
     * @return persisted placed-block markers
     */
    @NotNull List<M> findAll();

    /**
     * Persists a new placed-block marker asynchronously.
     *
     * @param marker marker to persist
     * @return future completing with the persisted marker
     */
    @NotNull CompletableFuture<M> createAsync(@NotNull M marker);

    /**
     * Deletes the placed-block marker with the supplied identifier asynchronously.
     *
     * @param id marker identifier to delete
     * @return completion future for the delete operation
     */
    @NotNull CompletableFuture<?> deleteAsync(@NotNull Long id);
}
