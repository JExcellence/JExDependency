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

package com.raindropcentral.rdt.service;

import com.raindropcentral.rdt.utils.ChunkType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Distinguishes the supported shared level scopes.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public enum LevelScope {
    NEXUS("Nexus"),
    SECURITY("Security"),
    BANK("Bank"),
    FARM("Farm"),
    OUTPOST("Outpost"),
    MEDIC("Medic"),
    ARMORY("Armory");

    private final String displayName;

    LevelScope(final @NotNull String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the user-facing scope label.
     *
     * @return user-facing scope label
     */
    public @NotNull String getDisplayName() {
        return this.displayName;
    }

    /**
     * Returns whether this scope belongs to one chunk instead of the town nexus.
     *
     * @return {@code true} when this scope targets one claimed chunk
     */
    public boolean isChunkScope() {
        return this != NEXUS;
    }

    /**
     * Resolves the progression scope for one chunk type.
     *
     * @param chunkType chunk type to resolve
     * @return matching progression scope, or {@code null} when the chunk type has no progression path
     */
    public static @Nullable LevelScope fromChunkType(final @Nullable ChunkType chunkType) {
        if (chunkType == null) {
            return null;
        }

        return switch (chunkType) {
            case SECURITY -> SECURITY;
            case BANK -> BANK;
            case FARM -> FARM;
            case OUTPOST -> OUTPOST;
            case MEDIC -> MEDIC;
            case ARMORY -> ARMORY;
            case NEXUS, DEFAULT, CLAIM_PENDING -> null;
        };
    }
}
