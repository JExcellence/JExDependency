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

package com.raindropcentral.rdt.utils;

import org.jetbrains.annotations.Nullable;

/**
 * Enumerates the supported specializations for claimed town chunks.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public enum ChunkType {
    NEXUS,
    DEFAULT,
    CLAIM_PENDING,
    FARM,
    SECURITY,
    OUTPOST,
    MEDIC,
    BANK;

    /**
     * Returns whether this type matches the supplied type.
     *
     * @param other comparison target
     * @return {@code true} when both values are identical
     */
    public boolean equalsType(final @Nullable ChunkType other) {
        return this == other;
    }

    /**
     * Returns whether both supplied types match.
     *
     * @param left left comparison target
     * @param right right comparison target
     * @return {@code true} when both values are identical or both {@code null}
     */
    public static boolean equalsType(
        final @Nullable ChunkType left,
        final @Nullable ChunkType right
    ) {
        return left == right;
    }
}
