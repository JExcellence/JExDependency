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

package com.raindropcentral.rplatform.requirement.plugin;

import com.raindropcentral.rplatform.protection.RProtectionBridge;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * {@link PluginIntegrationBridge} adapter for town plugins backed by {@link RProtectionBridge}.
 *
 * <p>This adapter currently exposes only the shared {@code town_level} key. RDT-specific chunk
 * and nexus keys remain in the dedicated {@link RdtPluginIntegrationBridge}.</p>
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
final class ProtectionTownPluginIntegrationBridge implements PluginIntegrationBridge {

    private static final String CATEGORY = "TOWNS";

    private final String integrationId;
    private final RProtectionBridge delegate;

    ProtectionTownPluginIntegrationBridge(final @NotNull String integrationId, final @NotNull RProtectionBridge delegate) {
        this.integrationId = integrationId;
        this.delegate = delegate;
    }

    @Override
    public @NotNull String getIntegrationId() {
        return this.integrationId;
    }

    @Override
    public @NotNull String getPluginName() {
        return this.delegate.getPluginName();
    }

    @Override
    public @NotNull String getCategory() {
        return CATEGORY;
    }

    @Override
    public boolean isAvailable() {
        return this.delegate.isAvailable();
    }

    @Override
    public double getValue(final @NotNull Player player, final @NotNull String key) {
        return "town_level".equals(normalizeKey(key))
            ? Math.max(0.0D, this.delegate.getPlayerTownLevel(player))
            : 0.0D;
    }

    private static @NotNull String normalizeKey(final @NotNull String key) {
        return key.trim()
            .toLowerCase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_');
    }
}
