package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.service.bounty.BountyService;
import com.raindropcentral.rdq.service.bounty.BountyServiceProvider;
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
 * Requirements: 14.1, 14.2, 14.5
 */
public class BountyPlayerJoinListener implements Listener {
    
    private static final Logger LOGGER = Logger.getLogger(BountyPlayerJoinListener.class.getName());
    
    private final RDQ rdq;
    private final boolean visualIndicatorsEnabled;
    
    /**
     * Creates a new BountyPlayerJoinListener.
     * Constructor compatible with CommandFactory auto-registration.
     *
     * @param rdq the RDQ plugin instance
     */
    public BountyPlayerJoinListener(@NotNull RDQ rdq) {
        this.rdq = rdq;
        var config = rdq.getBountyConfig();
        this.visualIndicatorsEnabled = config.isVisualIndicatorsEnabled();
    }
    
    /**
     * Handles player join events to restore bounty state and apply visual indicators.
     * 
     * Requirements:
     * - 14.1: Apply configured tab prefix to players with active bounties
     * - 14.2: Apply configured name color to players with active bounties
     * - 14.5: Preserve bounty state after logout
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check if bounty service is initialized
        if (!BountyServiceProvider.isInitialized()) {
            return;
        }
        
        BountyService bountyService = BountyServiceProvider.getInstance();
        
        // Check if player has an active bounty
        bountyService.getBountyByPlayer(player.getUniqueId()).thenAccept(bountyOpt -> {
            if (bountyOpt.isEmpty()) {
                return;
            }
            
            RBounty bounty = bountyOpt.get();
            
            // Check if bounty is active
            if (!bounty.isActive()) {
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
    
    /**
     * Applies visual indicators to a player with an active bounty.
     * This method will be integrated with VisualIndicatorManager when it's implemented (task 8).
     *
     * @param player the player to apply indicators to
     * @param bounty the active bounty on the player
     */
    private void applyVisualIndicators(@NotNull Player player, @NotNull RBounty bounty) {
        // TODO: Integrate with VisualIndicatorManager when implemented (task 8)
        // For now, this is a placeholder that will be connected later
        // The manager will handle:
        // - Applying tab prefix (Requirement 14.1)
        // - Applying name color (Requirement 14.2)
        // - Starting particle effects (Requirement 14.3)
        
        LOGGER.fine("Visual indicators would be applied to " + player.getName() + " (bounty ID: " + bounty.getId() + ")");
    }
    
    /**
     * Notifies a player that they have an active bounty on their head.
     *
     * @param player the player to notify
     * @param bounty the active bounty
     */
    private void notifyPlayerOfBounty(@NotNull Player player, @NotNull RBounty bounty) {
        player.sendMessage("§c§l[WARNING] §7You have an active bounty on your head!");
        player.sendMessage("§7Value: §6" + String.format("%.2f", bounty.getTotalEstimatedValue()));
        
        if (bounty.getExpiresAt().isPresent()) {
            player.sendMessage("§7Expires: §e" + bounty.getExpiresAt().get());
        }
        
        // Get commissioner name from player repository if needed
        player.sendMessage("§7Commissioner: §c" + bounty.getCommissionerUniqueId());
    }
}
