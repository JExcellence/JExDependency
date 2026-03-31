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

package com.raindropcentral.rplatform.scheduler;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a scheduled task that can be cancelled.
 *
 * <p>Implementations may wrap Bukkit, Folia, or synthetic fallback tasks. Callers can use this
 * handle to stop future executions during plugin shutdown or view disposal.
 */
public interface CancellableTaskHandle {

    /**
     * Attempts to cancel future executions of this task.
     *
     * @return {@code true} when the task transitioned into a cancelled state during this call
     */
    boolean cancel();

    /**
     * Returns whether the task is currently cancelled.
     *
     * @return {@code true} when the task will not execute again
     */
    boolean isCancelled();

    /**
     * Returns a handle that is permanently cancelled and performs no work.
     *
     * @return no-op handle
     */
    static @NotNull CancellableTaskHandle noop() {
        return NoOpTaskHandle.INSTANCE;
    }

    /**
     * No-op task handle that always reports itself as cancelled.
     */
    enum NoOpTaskHandle implements CancellableTaskHandle {
        INSTANCE;

        @Override
        public boolean cancel() {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return true;
        }
    }
}
