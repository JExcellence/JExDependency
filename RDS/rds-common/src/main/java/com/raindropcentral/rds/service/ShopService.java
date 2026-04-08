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

package com.raindropcentral.rds.service;

import com.raindropcentral.rds.configs.ConfigSection;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Defines edition-specific shop behaviour for the RDS runtime.
 *
 * <p>The shared runtime resolves player shop limits, admin shop limits, and config-edit
 * permissions through this abstraction so free and premium editions can reuse the same
 * commands, listeners, and views.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public interface ShopService {

    /**
     * Returns the effective maximum number of shops a player may unlock for the active edition.
     *
     * @param config active RDS configuration
     * @return effective player shop cap for the active edition
     * @throws NullPointerException if {@code config} is {@code null}
     */
    int getMaximumShops(@NotNull ConfigSection config);

    /**
     * Returns the maximum number of admin shops allowed on the server for the active edition.
     *
     * @return admin shop cap, or {@code -1} when unlimited
     */
    int getMaximumAdminShops();

    /**
     * Returns whether the active edition may change plugin-wide RDS config values.
     *
     * @return {@code true} when config changes are allowed
     */
    boolean canChangeConfigs();

    /**
     * Returns whether the active edition is premium.
     *
     * @return {@code true} when the runtime is running the premium edition
     */
    boolean isPremium();

    /**
     * Returns whether the active edition enforces a finite player shop cap.
     *
     * @param config active RDS configuration
     * @return {@code true} when a finite player shop cap applies
     * @throws NullPointerException if {@code config} is {@code null}
     */
    default boolean hasShopLimit(final @NotNull ConfigSection config) {
        return this.getMaximumShops(Objects.requireNonNull(config, "config")) > 0;
    }

    /**
     * Returns whether the active edition enforces a finite admin shop cap.
     *
     * @return {@code true} when a finite admin shop cap applies
     */
    default boolean hasAdminShopLimit() {
        return this.getMaximumAdminShops() > 0;
    }
}
