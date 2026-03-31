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

package com.raindropcentral.rdr.service;

import com.raindropcentral.rdr.configs.ConfigSection;
import org.jetbrains.annotations.NotNull;

/**
 * Premium-edition storage service behaviour for RDR.
 *
 * <p>The premium edition preserves the configured storage limits and allows players to edit
 * storage settings.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public final class PremiumStorageService implements StorageService {

    /**
     * Returns the configured premium-edition player storage cap.
     *
     * @param config active RDR configuration
     * @return configured storage cap
     * @throws NullPointerException if {@code config} is {@code null}
     */
    @Override
    public int getMaximumStorages(final @NotNull ConfigSection config) {
        return config.getMaxStorages();
    }

    /**
     * Returns whether players may edit storage settings in the premium edition.
     *
     * @return {@code true} because premium has full storage-setting access
     */
    @Override
    public boolean canChangeStorageSettings() {
        return true;
    }

    /**
     * Returns whether the active edition is premium.
     *
     * @return {@code true} because this service represents the premium edition
     */
    @Override
    public boolean isPremium() {
        return true;
    }
}
