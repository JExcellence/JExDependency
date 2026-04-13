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

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rplatform.service.RCoreBossBarBridge;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Optional bridge that registers the RDT town boss bar with the shared RCore settings service.
 *
 * @author Codex
 * @since 1.1.0
 * @version 1.1.0
 */
public final class TownBossBarIntegration {

    private final RDT rdt;
    private final @Nullable RCoreBossBarBridge bossBarBridge;

    /**
     * Creates the optional integration bridge.
     *
     * @param rdt active RDT runtime
     */
    public TownBossBarIntegration(final @NotNull RDT rdt) {
        this.rdt = rdt;
        this.bossBarBridge = RCoreBossBarBridge.create();
    }

    /**
     * Registers the RDT town boss bar as an RCore-managed provider.
     *
     * @return {@code true} when the provider was registered
     */
    public boolean register() {
        if (this.bossBarBridge == null) {
            return false;
        }

        this.bossBarBridge.registerProvider(new RCoreBossBarBridge.ProviderRegistration(
            RDT.TOWN_BOSS_BAR_PROVIDER_KEY,
            Material.GRASS_BLOCK,
            "rdt_boss_bar_provider.town.name",
            "rdt_boss_bar_provider.town.description",
            true,
            List.of(),
            (playerUuid, providerKey) -> this.resolveLegacyPreference(playerUuid),
            (playerUuid, preferenceSnapshot) -> this.handlePreferenceChange(playerUuid, preferenceSnapshot.enabled())
        ));
        return true;
    }

    /**
     * Unregisters the RDT provider from RCore.
     */
    public void unregister() {
        if (this.bossBarBridge != null) {
            this.bossBarBridge.unregisterProvider(RDT.TOWN_BOSS_BAR_PROVIDER_KEY);
        }
    }

    /**
     * Returns whether the town boss bar is enabled for one player.
     *
     * @param playerUuid player UUID to inspect
     * @return {@code true} when enabled
     */
    public boolean isEnabled(final @NotNull UUID playerUuid) {
        return this.bossBarBridge != null
            && this.bossBarBridge.resolvePreferences(playerUuid, RDT.TOWN_BOSS_BAR_PROVIDER_KEY).enabled();
    }

    private @Nullable RCoreBossBarBridge.PreferenceSeed resolveLegacyPreference(final @NotNull UUID playerUuid) {
        final RDTPlayer playerData = this.rdt.getPlayerRepository() == null
            ? null
            : this.rdt.getPlayerRepository().findByPlayer(playerUuid);
        return playerData == null ? null : RCoreBossBarBridge.PreferenceSeed.enabledOnly(playerData.isBossBarEnabled());
    }

    private void handlePreferenceChange(final @NotNull UUID playerUuid, final boolean enabled) {
        final Player player = Bukkit.getPlayer(playerUuid);
        if (player == null || this.rdt.getTownBossBarService() == null) {
            return;
        }

        if (enabled) {
            this.rdt.getTownBossBarService().refreshPlayer(player);
            return;
        }
        this.rdt.getTownBossBarService().clearPlayer(player);
    }
}
