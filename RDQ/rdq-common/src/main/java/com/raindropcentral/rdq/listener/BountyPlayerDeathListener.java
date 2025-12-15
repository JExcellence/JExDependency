package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.bounty.claim.ClaimResult;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener for player death events related to the bounty system.
 * Handles bounty claiming when a player with an active bounty is killed.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class BountyPlayerDeathListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger(BountyPlayerDeathListener.class.getName());

    private final RDQ rdq;

    public BountyPlayerDeathListener(@NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        var victim = event.getEntity();
        var victimUuid = victim.getUniqueId();
        var killer = victim.getKiller();
        var lastHitterUuid = killer != null ? killer.getUniqueId() : null;
        var deathLocation = victim.getLocation();

        LOGGER.info("Player death: " + victim.getName() + ", killer: " + 
                   (killer != null ? killer.getName() : "null") + 
                   ", has bounty: " + rdq.getBountyFactory().hasBounty(victimUuid));

        // Use BountyFactory's claim method which handles everything
        rdq.getBountyFactory().claimBounty(victimUuid, lastHitterUuid, deathLocation).thenAccept(claimResult -> {
            if (!claimResult.hasWinners()) {
                return;
            }

            // Remove visual indicators from the victim (Requirement 14.4)
            rdq.getVisualIndicatorManager().removeIndicators(victimUuid);
            
            // Announce the claim
            Bukkit.getScheduler().runTask(rdq.getPlugin(), () -> {
                announceClaimToWinners(claimResult, victim);
                announceClaimToBroadcast(claimResult, victim);
            });

        }).exceptionally(ex -> {
            LOGGER.log(Level.SEVERE, "Error processing bounty claim for " + victim.getName(), ex);
            return null;
        });
    }

    private void announceClaimToWinners(@NotNull ClaimResult claimResult, @NotNull Player victim) {
        for (var entry : claimResult.winners().entrySet()) {
            var winnerUuid = entry.getKey();
            var proportion = entry.getValue();
            var winner = Bukkit.getPlayer(winnerUuid);

            if (winner != null && winner.isOnline()) {
                new I18n.Builder("bounty_listener.bounty_claimed.title", winner)
                        .includePrefix()
                        .build().sendMessage();
                new I18n.Builder("bounty_listener.bounty_claimed.victim", winner)
                        .withPlaceholder("victim_name", victim.getName())
                        .build().sendMessage();
                
                if (claimResult.getWinnerCount() > 1) {
                    new I18n.Builder("bounty_listener.bounty_claimed.share", winner)
                            .withPlaceholder("percentage", String.format("%.1f", proportion * 100))
                            .build().sendMessage();
                }
            }
        }
    }

    private void announceClaimToBroadcast(@NotNull ClaimResult claimResult, @NotNull Player victim) {
        if (claimResult.getWinnerCount() == 1) {
            var winnerUuid = claimResult.winners().keySet().iterator().next();
            var winner = Bukkit.getPlayer(winnerUuid);
            var winnerName = winner != null ? winner.getName() : "Someone";

            net.kyori.adventure.text.Component broadcastMsg = new I18n.Builder("bounty_listener.bounty_claimed.broadcast", victim)
                    .withPlaceholder("claimer_name", winnerName)
                    .withPlaceholder("victim_name", victim.getName())
                    .build().component();
            Bukkit.broadcast(broadcastMsg);
        } else {
            net.kyori.adventure.text.Component broadcastMsg = new I18n.Builder("bounty_listener.bounty_claimed.broadcast_multiple", victim)
                    .withPlaceholder("winner_count", String.valueOf(claimResult.getWinnerCount()))
                    .withPlaceholder("victim_name", victim.getName())
                    .build().component();
            Bukkit.broadcast(broadcastMsg);
        }
    }
}
