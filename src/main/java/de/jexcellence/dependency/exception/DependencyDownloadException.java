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
 * Exception thrown when a dependency download operation fails.
 * This exception is used to signal failures during artifact resolution,
 * network communication, or validation of downloaded files.
 */
public class DependencyDownloadException extends Exception {

    /**
     * Constructs a new dependency download exception with the specified detail message.
     *
     * @param message the detail message explaining the failure
     */
    public DependencyDownloadException(@NotNull final String message) {
        super(message);
    }

    /**
     * Constructs a new dependency download exception with the specified detail message and cause.
     *
     * @param message the detail message explaining the failure
     * @param cause   the underlying cause of the failure
     */
    public DependencyDownloadException(@NotNull final String message, @Nullable final Throwable cause) {
        super(message, cause);
    }
}
