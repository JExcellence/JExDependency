package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.bounty.Bounty;
import de.jexcellence.jextranslate.i18n.I18n;
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

    private static final Logger LOGGER = Logger.getLogger("RDQ");

    private final RDQ rdq;

    public BountyPlayerJoinListener(@NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        var player = event.getPlayer();

        LOGGER.info("Checking for bounty on player join: " + player.getName());

        try {
            Bounty bounty = rdq.getBountyFactory().getBounty(player.getUniqueId());
            if (bounty != null && bounty.isActive()) {
                LOGGER.info("Found active bounty immediately for " + player.getName() + " (ID: " + bounty.getId() + ")");

                rdq.getPlatform().getScheduler().runDelayed(() -> {
                    if (player.isOnline()) {
                        applyVisualIndicators(player, bounty);
                        notifyPlayerOfBounty(player, bounty);
                    }
                }, 20L);
                
                return;
            }
        } catch (Exception e) {
            LOGGER.warning("Immediate bounty check failed for " + player.getName() + ": " + e.getMessage());
        }

        scheduleVisualIndicatorCheck(player, 0);
    }
    
    private void scheduleVisualIndicatorCheck(@NotNull Player player, int attempt) {
        if (!player.isOnline() || attempt >= 3) {
            return;
        }
        
        long delay = 40L * (attempt + 1);
        
        rdq.getPlatform().getScheduler().runDelayed(() -> {
            if (!player.isOnline()) {
                return;
            }
            
            rdq.getBountyFactory().getBountyAsync(player.getUniqueId()).thenAccept(bounty -> {
                if (bounty == null) {
                    scheduleVisualIndicatorCheck(player, attempt + 1);
                    return;
                }
                
                if (!bounty.isActive()) {
                    return;
                }

                rdq.getPlatform().getScheduler().runSync(() -> {
                    if (player.isOnline()) {
                        applyVisualIndicators(player, bounty);

                        if (attempt == 0) {
                            notifyPlayerOfBounty(player, bounty);
                        }
                    }
                });

            }).exceptionally(ex -> {
                LOGGER.log(Level.SEVERE, "Error checking bounty for player " + player.getName() + " on async attempt " + (attempt + 1), ex);
                scheduleVisualIndicatorCheck(player, attempt + 1);
                return null;
            });
            
        }, delay);
    }

    private void applyVisualIndicators(@NotNull Player player, @NotNull Bounty bounty) {
        try {
            LOGGER.info("Applying visual indicators to " + player.getName() + " (bounty ID: " + bounty.getId() + ")");

            if (rdq.getVisualIndicatorManager() != null) {
                rdq.getVisualIndicatorManager().forceRefreshIndicators(player);
            }

            rdq.getPlatform().getScheduler().runDelayed(() -> {
                if (player.isOnline()) {
                    rdq.getVisualIndicatorManager().forceRefreshIndicators(player);
                    LOGGER.info("Reapplied visual indicators to " + player.getName() + " (second attempt)");
                }
            }, 40L);
            
            rdq.getPlatform().getScheduler().runDelayed(() -> {
                if (player.isOnline()) {
                    rdq.getVisualIndicatorManager().forceRefreshIndicators(player);
                }
            }, 100L);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to apply visual indicators to " + player.getName(), e);
        }
    }

    private void notifyPlayerOfBounty(@NotNull Player player, @NotNull Bounty bounty) {
        new I18n.Builder("bounty_listener.bounty_active.warning", player)
                .includePrefix()
                .build().sendMessage();
        new I18n.Builder("bounty_listener.bounty_active.value", player)
                .withPlaceholder("bounty_value", String.format("%.2f", bounty.getTotalEstimatedValue()))
                .build().sendMessage();

        if (bounty.getExpiresAt() != null) {
            new I18n.Builder("bounty_listener.bounty_active.expires", player)
                    .withPlaceholder("expires_at", bounty.getExpiresAt().toString())
                    .build().sendMessage();
        }

        new I18n.Builder("bounty_listener.bounty_active.commissioner", player)
                .withPlaceholder("commissioner_uuid", bounty.getCommissionerUniqueId().toString())
                .build().sendMessage();
    }
}
