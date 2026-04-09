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

import org.jetbrains.annotations.NotNull;

/**
 * Enumerates the possible outcomes of one requested town relationship change.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public enum TownRelationshipChangeStatus {
    PENDING("pending"),
    CONFIRMED("confirmed"),
    COOLDOWN("cooldown"),
    LOCKED("locked"),
    UNCHANGED("unchanged"),
    FAILED("failed");

    private final String translationKey;

    TownRelationshipChangeStatus(final @NotNull String translationKey) {
        this.translationKey = translationKey;
    }

    /**
     * Returns the translation key suffix for this change outcome.
     *
     * @return translation key suffix
     */
    public @NotNull String getTranslationKey() {
        return this.translationKey;
    }
}
