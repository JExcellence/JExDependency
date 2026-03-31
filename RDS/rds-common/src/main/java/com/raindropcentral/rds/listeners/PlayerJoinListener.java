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

package com.raindropcentral.rds.listeners;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.RDSPlayer;
import com.raindropcentral.rds.service.tax.ShopTaxSummarySupport;
import com.raindropcentral.rplatform.protection.RProtectionBridge;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;

/**
 * Player join listener that provisions persistent RDS player rows and restores join-time UI state.
 *
 * <p>When a player already has a persisted profile, the listener restores the saved shop sidebar
 * scoreboard type and optionally sends the configured tax reminder.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings("unused")
public class PlayerJoinListener implements Listener {

    private final RDS rds;

    /**
     * Creates a listener bound to the active plugin instance.
     *
     * @param rds active plugin instance
     */
    public PlayerJoinListener(RDS rds) {
        this.rds = rds;
    }

    /**
     * Creates a new persisted player row when needed and restores saved sidebar state on join.
     *
     * @param event Bukkit player join event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event == null) return;
        final Player player = event.getPlayer();

        var rPlayer = this.rds.getPlayerRepository().findByPlayer(player.getUniqueId());

        if (rPlayer == null) {
            var newPlayer = new RDSPlayer(player.getUniqueId());
            this.rds.getPlayerRepository().createAsync(newPlayer);
        } else {
            rPlayer.getShopSidebarScoreboardType().ifPresent(scoreboardType -> {
                if ("ledger".equalsIgnoreCase(scoreboardType)) {
                    this.rds.getShopSidebarScoreboardService().enableLedger(player);
                    return;
                }
                if ("stock".equalsIgnoreCase(scoreboardType)) {
                    this.rds.getShopSidebarScoreboardService().enableStock(player);
                }
            });
        }

        if (!this.rds.getDefaultConfig().getTaxes().shouldNotifyOnJoin()) {
            return;
        }

        final ShopTaxSummarySupport.ShopTaxSummary taxSummary = ShopTaxSummarySupport.summarize(
                this.rds,
                player.getUniqueId()
        );
        if (!taxSummary.hasTaxableShops() || !taxSummary.hasConfiguredCharges()) {
            return;
        }
        final String protectionPluginName = this.resolveProtectionPluginName();

        new I18n.Builder("player_join.tax_notice", player)
                .withPlaceholders(Map.of(
                        "taxed_shops", taxSummary.taxedShops(),
                        "taxes", taxSummary.amountSummary(),
                        "protection_taxes", taxSummary.protectionAmountSummary(),
                        "protection_tax_currency_count", taxSummary.protectionTaxCurrencyCount(),
                        "protection_plugin", protectionPluginName,
                        "next_tax_at", taxSummary.nextTaxDisplay(),
                        "time_until", taxSummary.timeUntilDisplay()
                ))
                .build()
                .sendMessage();
    }

    /**
     * Clears runtime-only player UI state when a player disconnects.
     *
     * @param event Bukkit player quit event
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event == null) return;
        this.rds.getShopBossBarService().clearPlayer(event.getPlayer());
        this.rds.getShopSidebarScoreboardService().disable(event.getPlayer());
    }

    private String resolveProtectionPluginName() {
        final RProtectionBridge bridge = RProtectionBridge.getBridge();
        if (bridge == null || !bridge.isAvailable()) {
            return "None";
        }
        return bridge.getPluginName();
    }
}
