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

package com.raindropcentral.rds.service.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.RDSPlayer;
import com.raindropcentral.rplatform.service.RCoreBossBarBridge;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Optional bridge that registers the RDS shop boss bar with the shared RCore settings service.
 *
 * @author Codex
 * @since 2.1.0
 * @version 2.1.0
 */
public final class ShopBossBarIntegration {

    private final RDS rds;
    private final @Nullable RCoreBossBarBridge bossBarBridge;

    /**
     * Creates the optional integration bridge.
     *
     * @param rds active RDS runtime
     */
    public ShopBossBarIntegration(final @NotNull RDS rds) {
        this.rds = rds;
        this.bossBarBridge = RCoreBossBarBridge.create();
    }

    /**
     * Registers the RDS shop boss bar as an RCore-managed provider.
     *
     * @return {@code true} when the provider was registered
     */
    public boolean register() {
        if (this.bossBarBridge == null) {
            return false;
        }

        this.bossBarBridge.registerProvider(new RCoreBossBarBridge.ProviderRegistration(
            RDS.SHOP_BOSS_BAR_PROVIDER_KEY,
            Material.CHEST,
            "rds_boss_bar_provider.shop.name",
            "rds_boss_bar_provider.shop.description",
            false,
            List.of(),
            (playerUuid, providerKey) -> this.resolveLegacyPreference(playerUuid),
            (playerUuid, preferenceSnapshot) -> this.handlePreferenceChange(playerUuid, preferenceSnapshot.enabled())
        ));
        return true;
    }

    /**
     * Unregisters the RDS provider from RCore.
     */
    public void unregister() {
        if (this.bossBarBridge != null) {
            this.bossBarBridge.unregisterProvider(RDS.SHOP_BOSS_BAR_PROVIDER_KEY);
        }
    }

    /**
     * Returns whether the shop boss bar is enabled for one player.
     *
     * @param playerUuid player UUID to inspect
     * @return {@code true} when enabled
     */
    public boolean isEnabled(final @NotNull UUID playerUuid) {
        return this.bossBarBridge != null
            && this.bossBarBridge.resolvePreferences(playerUuid, RDS.SHOP_BOSS_BAR_PROVIDER_KEY).enabled();
    }

    /**
     * Toggles the shop boss bar enabled state for one player.
     *
     * @param player target player
     * @return resulting enabled state
     */
    public boolean toggleEnabled(final @NotNull Player player) {
        if (this.bossBarBridge == null) {
            return false;
        }
        return this.bossBarBridge.toggleEnabled(player.getUniqueId(), RDS.SHOP_BOSS_BAR_PROVIDER_KEY).enabled();
    }

    /**
     * Opens the RCore-managed provider settings view for one player.
     *
     * @param player target player
     */
    public void openSettings(final @NotNull Player player) {
        if (this.bossBarBridge != null) {
            this.bossBarBridge.openSettingsView(player, RDS.SHOP_BOSS_BAR_PROVIDER_KEY);
        }
    }

    private @Nullable RCoreBossBarBridge.PreferenceSeed resolveLegacyPreference(final @NotNull UUID playerUuid) {
        final RDSPlayer playerData = this.rds.getPlayerRepository() == null
            ? null
            : this.rds.getPlayerRepository().findByPlayer(playerUuid);
        return playerData == null
            ? null
            : new RCoreBossBarBridge.PreferenceSeed(playerData.isShopBarEnabled(), Map.of());
    }

    private void handlePreferenceChange(final @NotNull UUID playerUuid, final boolean enabled) {
        final Player player = Bukkit.getPlayer(playerUuid);
        if (player == null || this.rds.getShopBossBarService() == null) {
            return;
        }

        if (enabled) {
            this.rds.getShopBossBarService().refreshPlayer(player);
            return;
        }
        this.rds.getShopBossBarService().clearPlayer(player);
    }
}
