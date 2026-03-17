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

/**
 * Defines edition-specific runtime behavior for RDT.
 *
 * <p>The shared runtime delegates configuration mutability checks and edition metadata to this
 * abstraction so free and premium jars can share one codebase.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public interface TownService {

    /**
     * Returns whether the active edition allows runtime config changes.
     *
     * @return {@code true} when config changes are allowed
     */
    boolean canChangeConfigs();

    /**
     * Returns whether the active edition is premium.
     *
     * @return {@code true} for premium editions
     */
    boolean isPremium();
}
