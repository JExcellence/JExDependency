package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.bounty.tracking.DamageTracker;
import com.raindropcentral.rdq.bounty.type.ClaimMode;
import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.service.bounty.BountyService;
import com.raindropcentral.rdq.service.bounty.BountyServiceProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener for player death events related to the bounty system.
 * Handles bounty claiming when a player with an active bounty is killed.
 * 
 * Requirements: 10.1, 10.5
 */
public class BountyPlayerDeathListener implements Listener {
    
    private static final Logger LOGGER = Logger.getLogger(BountyPlayerDeathListener.class.getName());
    
    private final RDQ rdq;
    private final DamageTracker damageTracker;
    private final ClaimMode claimMode;
    
    /**
     * Creates a new BountyPlayerDeathListener.
     * Constructor compatible with CommandFactory auto-registration.
     *
     * @param rdq the RDQ plugin instance
     */
    public BountyPlayerDeathListener(@NotNull RDQ rdq) {
        this.rdq = rdq;
        var config = rdq.getBountyConfig();
        this.damageTracker = new DamageTracker(config.getDamageTrackingWindow());
        this.claimMode = config.getClaimMode();
    }
    
    /**
     * Handles player death events to process bounty claims.
     * 
     * Requirements:
     * - 10.1: Determine killer based on configured claim mode
     * - 10.5: Mark bounty as claimed with claimer's UUID and timestamp
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        Player victim = event.getEntity();
        UUID victimUuid = victim.getUniqueId();
        
        // Check if bounty service is initialized
        if (!BountyServiceProvider.isInitialized()) {
            return;
        }
        
        BountyService bountyService = BountyServiceProvider.getInstance();
        
        // Check if victim has an active bounty
        bountyService.getBountyByPlayer(victimUuid).thenAccept(bountyOpt -> {
            if (bountyOpt.isEmpty()) {
                // No bounty on this player, clean up damage tracking
                damageTracker.clearDamage(victimUuid);
                return;
            }
            
            RBounty bounty = bountyOpt.get();
            
            // Check if bounty is active
            if (!bounty.isActive()) {
                damageTracker.clearDamage(victimUuid);
                return;
            }
            
            // Determine who should claim the bounty based on claim mode
            UUID claimerUuid = determineClaimerUuid(victim, victimUuid);
            
            if (claimerUuid == null) {
                // No valid claimer (e.g., environmental death, suicide)
                LOGGER.fine("No valid claimer for bounty on " + victim.getName());
                damageTracker.clearDamage(victimUuid);
                return;
            }
            
            // Claim the bounty
            processBountyClaim(bounty, claimerUuid, victim);
            
            // Clear damage tracking for this victim
            damageTracker.clearDamage(victimUuid);
            
        }).exceptionally(ex -> {
            LOGGER.log(Level.SEVERE, "Error checking bounty for player " + victim.getName(), ex);
            return null;
        });
    }
    
    /**
     * Determines the UUID of the player who should claim the bounty based on the claim mode.
     *
     * @param victim the victim player
     * @param victimUuid the victim's UUID
     * @return the UUID of the claimer, or null if no valid claimer
     */
    private UUID determineClaimerUuid(@NotNull Player victim, @NotNull UUID victimUuid) {
        Player killer = victim.getKiller();
        UUID lastHitterUuid = killer != null ? killer.getUniqueId() : null;
        
        // Get damage map from tracker
        Map<UUID, Double> damageMap = damageTracker.getDamageMap(victimUuid);
        
        // Use claim mode to determine winner
        return claimMode.determineWinner(damageMap, lastHitterUuid);
    }
    
    /**
     * Processes the bounty claim by updating the bounty and notifying players.
     *
     * @param bounty the bounty to claim
     * @param claimerUuid the UUID of the player claiming the bounty
     * @param victim the victim player
     */
    private void processBountyClaim(@NotNull RBounty bounty, @NotNull UUID claimerUuid, @NotNull Player victim) {
        // Claim the bounty
        bounty.claim(claimerUuid);
        
        BountyService bountyService = BountyServiceProvider.getInstance();
        
        // Update the bounty in the database
        bountyService.updateBounty(bounty).thenAccept(updatedBounty -> {
            // Notify on the main thread
            Bukkit.getScheduler().runTask(rdq.getPlugin(), () -> {
                Player claimer = Bukkit.getPlayer(claimerUuid);
                if (claimer != null && claimer.isOnline()) {
                    claimer.sendMessage("§a§lBounty Claimed!");
                    claimer.sendMessage("§7You claimed the bounty on §c" + victim.getName() + "§7!");
                    claimer.sendMessage("§7Reward value: §6" + String.format("%.2f", bounty.getTotalEstimatedValue()));
                }
                
                // Broadcast to server
                Bukkit.broadcastMessage("§c§l[BOUNTY] §f" + (claimer != null ? claimer.getName() : "Someone") + 
                        " §7claimed the bounty on §c" + victim.getName() + 
                        "§7! (§6" + String.format("%.2f", bounty.getTotalEstimatedValue()) + "§7)");
            });
            
        }).exceptionally(ex -> {
            LOGGER.log(Level.SEVERE, "Error updating bounty " + bounty.getId(), ex);
            return null;
        });
    }
}
