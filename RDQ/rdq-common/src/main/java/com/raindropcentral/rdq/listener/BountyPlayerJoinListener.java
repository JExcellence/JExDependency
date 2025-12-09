package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.bounty.utility.BountyFactory;
import com.raindropcentral.rdq.database.entity.bounty.Bounty;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener for player join events related to the bounty system.
 * Handles applying visual indicators and restoring bounty state when players log in.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class BountyPlayerJoinListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger(BountyPlayerJoinListener.class.getName());

    private final RDQ rdq;
    private final BountyFactory bountyFactory;
    private final boolean visualIndicatorsEnabled;

    public BountyPlayerJoinListener(@NotNull RDQ rdq) {
        this.rdq = rdq;
        this.bountyFactory = rdq.getBountyFactory();
        this.visualIndicatorsEnabled = bountyFactory.getBountyConfiguration().isVisualIndicatorsEnabled();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        var player = event.getPlayer();

        bountyFactory.getBountyAsync(player.getUniqueId()).thenAccept(bounty -> {
            if (bounty == null || !bounty.isActive()) {
                return;
            }

            // Apply visual indicators if enabled
            if (visualIndicatorsEnabled) {
                applyVisualIndicators(player, bounty);
            }

            // Notify player about their bounty (delayed to ensure they see it)
            Bukkit.getScheduler().runTaskLater(rdq.getPlugin(), () -> {
                if (player.isOnline()) {
                    notifyPlayerOfBounty(player, bounty);
                }
            }, 40L); // 2 second delay

        }).exceptionally(ex -> {
            LOGGER.log(Level.SEVERE, "Error checking bounty for player " + player.getName() + " on join", ex);
            return null;
        });
    }

    private void applyVisualIndicators(@NotNull Player player, @NotNull Bounty bounty) {
        // TODO: Integrate with VisualIndicatorManager when implemented
        // The manager will handle:
        // - Applying tab prefix
        // - Applying name color
        // - Starting particle effects
        LOGGER.fine("Visual indicators would be applied to " + player.getName() + " (bounty ID: " + bounty.getId() + ")");
    }

    private void notifyPlayerOfBounty(@NotNull Player player, @NotNull Bounty bounty) {
        TranslationService.create(TranslationKey.of("bounty_listener.bounty_active.warning"), player)
                .withPrefix()
                .send();
        TranslationService.create(TranslationKey.of("bounty_listener.bounty_active.value"), player)
                .with("bounty_value", String.format("%.2f", bounty.getTotalEstimatedValue()))
                .send();

        if (bounty.getExpiresAt() != null) {
            TranslationService.create(TranslationKey.of("bounty_listener.bounty_active.expires"), player)
                    .with("expires_at", bounty.getExpiresAt().toString())
                    .send();
        }

        TranslationService.create(TranslationKey.of("bounty_listener.bounty_active.commissioner"), player)
                .with("commissioner_uuid", bounty.getCommissionerUniqueId().toString())
                .send();
    }
}
