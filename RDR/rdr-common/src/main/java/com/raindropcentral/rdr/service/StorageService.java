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

import java.util.Objects;

import com.raindropcentral.rdr.configs.ConfigSection;
import org.jetbrains.annotations.NotNull;

/**
 * Defines edition-specific storage behaviour for the RDR runtime.
 *
 * <p>The shared runtime resolves storage limits and configuration-edit permissions through this
 * abstraction so free and premium editions can reuse the same commands, listeners, and views
 * without duplicating the rest of the plugin bootstrap.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public interface StorageService {

    /**
     * Returns the effective maximum number of storages a player may own for the active edition.
     *
     * @param config active RDR configuration
     * @return effective player storage cap for the active edition
     * @throws NullPointerException if {@code config} is {@code null}
     */
    int getMaximumStorages(@NotNull ConfigSection config);

    /**
     * Returns the effective number of storages that should be provisioned for a first-time player.
     *
     * @param config active RDR configuration
     * @return effective initial storage count for the active edition
     * @throws NullPointerException if {@code config} is {@code null}
     */
    default int getInitialProvisionedStorages(final @NotNull ConfigSection config) {
        final ConfigSection validatedConfig = Objects.requireNonNull(config, "config");
        return Math.min(validatedConfig.getInitialProvisionedStorages(), this.getMaximumStorages(validatedConfig));
    }

    /**
     * Returns whether players may change storage-related configuration through the UI.
     *
     * @return {@code true} when hotkeys and trusted-access settings may be edited
     */
    boolean canChangeStorageSettings();

    /**
     * Returns whether the active edition is premium.
     *
     * @return {@code true} when the runtime is running the premium edition
     */
    boolean isPremium();
}
