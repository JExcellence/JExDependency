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

package de.jexcellence.dependency.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown when plugin loading operations fail in the Paper plugin loader.
 * This exception is used to signal failures during library initialization,
 * directory creation, or other plugin loading operations.
 */
public class PluginLoaderException extends RuntimeException {

    /**
     * Constructs a new plugin loader exception with the specified detail message.
     *
     * @param message the detail message explaining the failure
     */
    public PluginLoaderException(@NotNull final String message) {
        super(message);
    }

    /**
     * Constructs a new plugin loader exception with the specified detail message and cause.
     *
     * @param message the detail message explaining the failure
     * @param cause   the underlying cause of the failure
     */
    public PluginLoaderException(@NotNull final String message, @Nullable final Throwable cause) {
        super(message, cause);
    }
}
