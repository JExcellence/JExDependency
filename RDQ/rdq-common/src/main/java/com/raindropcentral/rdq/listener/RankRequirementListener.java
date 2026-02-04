package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRankUpgradeProgress;
import com.raindropcentral.rdq.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rplatform.requirement.RequirementService;
import com.raindropcentral.rplatform.requirement.event.RequirementCheckEvent;
import com.raindropcentral.rplatform.requirement.event.RequirementConsumeEvent;
import com.raindropcentral.rplatform.requirement.event.RequirementMetEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Automatically tracks rank requirement progress by listening to requirement events.
 */
public class RankRequirementListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger(RankRequirementListener.class.getName());
    
    private final RDQ rdq;
    private final RequirementService requirementService;

    public RankRequirementListener(@NotNull RDQ rdq) {
        this.rdq = rdq;
        this.requirementService = RequirementService.getInstance();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRequirementCheck(@NotNull RequirementCheckEvent event) {
        Player player = event.getPlayer();
        String requirementTypeId = event.getRequirement().getTypeId();
        double progress = requirementService.calculateProgress(player, event.getRequirement());
        
        rdq.getExecutor().submit(() -> {
            try {
                updateProgressForPlayer(player, requirementTypeId, progress);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to update requirement progress for " + player.getName(), e);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRequirementMet(@NotNull RequirementMetEvent event) {
        Player player = event.getPlayer();
        String requirementTypeId = event.getRequirement().getTypeId();
        double progress = event.getProgress();
        
        LOGGER.info("Requirement met for " + player.getName() + ": " + requirementTypeId + 
                    " (progress: " + progress + ")");
        
        rdq.getExecutor().submit(() -> {
            try {
                updateProgressForPlayer(player, requirementTypeId, progress);
                checkForRankUp(player);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to handle requirement met event for " + player.getName(), e);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRequirementConsume(@NotNull RequirementConsumeEvent event) {
        LOGGER.info("Requirement consumed for " + event.getPlayer().getName() + ": " + 
                    event.getRequirement().getTypeId());
    }

    private void updateProgressForPlayer(@NotNull Player player, @NotNull String requirementTypeId, double progress) {
        try {
            Optional<RDQPlayer> rdqPlayerOpt = findRDQPlayer(player);
            if (rdqPlayerOpt.isEmpty()) {
                LOGGER.fine("RDQPlayer not found for " + player.getName() + ", skipping progress update");
                return;
            }
            
            RDQPlayer rdqPlayer = rdqPlayerOpt.get();
            Optional<RRankUpgradeRequirement> upgradeReqOpt = findMatchingRankRequirement(rdqPlayer, requirementTypeId);
            if (upgradeReqOpt.isEmpty()) {
                return;
            }
            
            updateRequirementProgress(rdqPlayer, upgradeReqOpt.get(), progress);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating progress for player " + player.getName() + 
                ": " + e.getMessage());
        }
    }

    private Optional<RDQPlayer> findRDQPlayer(@NotNull Player player) {
        try {
            // Use the cached repository's get method to avoid nested transactions
            // The cache key is UUID for RDQPlayerRepository
            RDQPlayer rdqPlayer = rdq.getPlayerRepository().getCachedByKey().get(player.getUniqueId());
            return Optional.ofNullable(rdqPlayer);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to find RDQPlayer for " + player.getName(), e);
            return Optional.empty();
        }
    }

    private Optional<RRankUpgradeRequirement> findMatchingRankRequirement(
            @NotNull RDQPlayer rdqPlayer,
            @NotNull String requirementTypeId
    ) {
        var activeRank = rdqPlayer.getActivePlayerRank();
        if (activeRank.isEmpty()) return Optional.empty();
        
        var currentRank = activeRank.get().getCurrentRank();
        
        return currentRank.getUpgradeRequirements().stream()
                .filter(req -> req.getRequirement().getRequirement().getTypeId().equals(requirementTypeId))
                .findFirst();
    }

    private void updateRequirementProgress(
            @NotNull RDQPlayer rdqPlayer,
            @NotNull RRankUpgradeRequirement upgradeReq,
            double progress
    ) {
        try {
            // Find existing progress entry or create new one
            // Use a single query to avoid nested transactions
            RPlayerRankUpgradeProgress progressEntry = null;
            
            try {
                var existingOpt = rdq.getPlayerRankUpgradeProgressRepository()
                        .findByAttributes(java.util.Map.of(
                                "player", rdqPlayer,
                                "upgradeRequirement", upgradeReq
                        ));
                progressEntry = existingOpt.orElse(null);
            } catch (Exception e) {
                LOGGER.fine("No existing progress entry found, will create new one");
            }
            
            if (progressEntry == null) {
                // Create new progress entry
                progressEntry = new RPlayerRankUpgradeProgress(rdqPlayer, upgradeReq);
                progressEntry.setProgress(progress);
                rdq.getPlayerRankUpgradeProgressRepository().create(progressEntry);
                LOGGER.fine("Created progress entry for " + rdqPlayer.getPlayerName() + 
                           ": " + upgradeReq.getRequirement().getRequirement().getTypeId() + 
                           " = " + progress);
            } else {
                // Update existing progress entry
                double oldProgress = progressEntry.getProgress();
                if (oldProgress != progress) {
                    progressEntry.setProgress(progress);
                    rdq.getPlayerRankUpgradeProgressRepository().update(progressEntry);
                    LOGGER.fine("Updated progress for " + rdqPlayer.getPlayerName() + 
                               ": " + upgradeReq.getRequirement().getRequirement().getTypeId() + 
                               " = " + progress);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update requirement progress in database", e);
        }
    }

    private void checkForRankUp(@NotNull Player player) {
        try {
            Optional<RDQPlayer> rdqPlayerOpt = findRDQPlayer(player);
            if (rdqPlayerOpt.isEmpty()) return;
            
            RDQPlayer rdqPlayer = rdqPlayerOpt.get();
            var activeRank = rdqPlayer.getActivePlayerRank();
            if (activeRank.isEmpty()) return;
            
            var currentRank = activeRank.get().getCurrentRank();
            
            boolean allRequirementsMet = currentRank.getUpgradeRequirements().stream()
                    .allMatch(req -> {
                        var progress = rdq.getPlayerRankUpgradeProgressRepository()
                                .findByAttributes(java.util.Map.of(
                                        "player", rdqPlayer,
                                        "upgradeRequirement", req
                                ));
                        return progress.isPresent() && progress.get().isCompleted();
                    });
            
            if (allRequirementsMet) {
                LOGGER.info("Player " + player.getName() + " has met all requirements for rank: " + 
                            currentRank.getIdentifier());
                
                Bukkit.getScheduler().runTask(rdq.getPlugin(), () -> {
                    if (player.isOnline()) {
                        player.sendMessage("§a§lRANK UP! §7You've completed all requirements for " + 
                                          currentRank.getDisplayNameKey());
                        player.sendMessage("§7Use the rank menu to claim your rank upgrade!");
                    }
                });
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to check for rank-up for " + player.getName(), e);
        }
    }
}
