package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.bounty.claim.ClaimResult;
import com.raindropcentral.rdq.bounty.utility.BountyFactory;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
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
    private final BountyFactory bountyFactory;

    public BountyPlayerDeathListener(@NotNull RDQ rdq) {
        this.rdq = rdq;
        this.bountyFactory = rdq.getBountyFactory();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        var victim = event.getEntity();
        var victimUuid = victim.getUniqueId();
        var killer = victim.getKiller();
        var lastHitterUuid = killer != null ? killer.getUniqueId() : null;
        var deathLocation = victim.getLocation();

        // Use BountyFactory's claim method which handles everything
        bountyFactory.claimBounty(victimUuid, lastHitterUuid, deathLocation).thenAccept(claimResult -> {
            if (!claimResult.hasWinners()) {
                return;
            }

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
                TranslationService.create(TranslationKey.of("bounty_listener.bounty_claimed.title"), winner)
                        .withPrefix()
                        .send();
                TranslationService.create(TranslationKey.of("bounty_listener.bounty_claimed.victim"), winner)
                        .with("victim_name", victim.getName())
                        .send();
                
                if (claimResult.getWinnerCount() > 1) {
                    TranslationService.create(TranslationKey.of("bounty_listener.bounty_claimed.share"), winner)
                            .with("percentage", String.format("%.1f", proportion * 100))
                            .send();
                }
            }
        }
    }

    private void announceClaimToBroadcast(@NotNull ClaimResult claimResult, @NotNull Player victim) {
        if (claimResult.getWinnerCount() == 1) {
            var winnerUuid = claimResult.winners().keySet().iterator().next();
            var winner = Bukkit.getPlayer(winnerUuid);
            var winnerName = winner != null ? winner.getName() : 
                    TranslationService.create(TranslationKey.of("bounty_listener.bounty_claimed.someone"), victim)
                            .build().component().toString();

            var broadcastMsg = TranslationService.create(TranslationKey.of("bounty_listener.bounty_claimed.broadcast"), victim)
                    .with("claimer_name", winnerName)
                    .with("victim_name", victim.getName())
                    .build().component().toString();
            Bukkit.broadcastMessage(broadcastMsg);
        } else {
            var broadcastMsg = TranslationService.create(TranslationKey.of("bounty_listener.bounty_claimed.broadcast_multiple"), victim)
                    .with("winner_count", String.valueOf(claimResult.getWinnerCount()))
                    .with("victim_name", victim.getName())
                    .build().component().toString();
            Bukkit.broadcastMessage(broadcastMsg);
        }
    }
}
