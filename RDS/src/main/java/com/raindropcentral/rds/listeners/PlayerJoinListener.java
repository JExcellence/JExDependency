package com.raindropcentral.rds.listeners;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.RDSPlayer;
import com.raindropcentral.rds.service.tax.ShopTaxSummarySupport;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;

@SuppressWarnings("unused")
public class PlayerJoinListener implements Listener {

    private final RDS rds;

    public PlayerJoinListener(RDS rds) {
        this.rds = rds;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event == null) return;
        final Player player = event.getPlayer();

        var rPlayer = this.rds.getPlayerRepository().findByPlayer(player.getUniqueId());

        if (rPlayer == null) {
            var newPlayer = new RDSPlayer(player.getUniqueId());
            this.rds.getPlayerRepository().createAsync(newPlayer);
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

        new I18n.Builder("player_join.tax_notice", player)
                .withPlaceholders(Map.of(
                        "taxed_shops", taxSummary.taxedShops(),
                        "taxes", taxSummary.amountSummary(),
                        "next_tax_at", taxSummary.nextTaxDisplay(),
                        "time_until", taxSummary.timeUntilDisplay()
                ))
                .build()
                .sendMessage();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event == null) return;
        this.rds.getShopBossBarService().clearPlayer(event.getPlayer());
        this.rds.getShopSidebarScoreboardService().disable(event.getPlayer());
    }
}
