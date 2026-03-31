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

package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRankUpgradeProgress;
import com.raindropcentral.rdq.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.requirement.RequirementService;
import com.raindropcentral.rplatform.requirement.event.RequirementCheckEvent;
import com.raindropcentral.rplatform.requirement.event.RequirementConsumeEvent;
import com.raindropcentral.rplatform.requirement.event.RequirementMetEvent;
import de.jexcellence.jextranslate.i18n.I18n;
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
 * Only processes requirements that belong to ranks, not perks.
 */
public class RankRequirementListener implements Listener {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
    
    private final RDQ rdq;
    private final RequirementService requirementService;

    /**
     * Executes RankRequirementListener.
     */
    public RankRequirementListener(@NotNull RDQ rdq) {
        this.rdq = rdq;
        this.requirementService = RequirementService.getInstance();
    }

    /**
     * Executes onRequirementCheck.
     */
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

    /**
     * Executes onRequirementMet.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRequirementMet(@NotNull RequirementMetEvent event) {
        Player player = event.getPlayer();
        String requirementTypeId = event.getRequirement().getTypeId();
        double progress = event.getProgress();
        
        LOGGER.fine("Requirement met for " + player.getName() + ": " + requirementTypeId + 
                    " (progress: " + progress + ")");
        
        rdq.getExecutor().submit(() -> {
            try {
                // Only process rank requirements, not perk requirements
                Optional<RDQPlayer> rdqPlayerOpt = findRDQPlayer(player);
                if (rdqPlayerOpt.isEmpty()) {
                    return;
                }
                
                RDQPlayer rdqPlayer = rdqPlayerOpt.get();
                Optional<RRankUpgradeRequirement> upgradeReqOpt = findMatchingRankRequirement(rdqPlayer, requirementTypeId);
                
                if (upgradeReqOpt.isEmpty()) {
                    // This requirement doesn't belong to any rank requirement for this player
                    // It might be a perk requirement, so we skip it
                    LOGGER.fine("Skipping non-rank requirement: " + requirementTypeId + 
                               " for player " + player.getName());
                    return;
                }
                
                // This is a rank requirement, process it
                updateProgressForPlayer(player, requirementTypeId, progress);
                checkForRankUp(player);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to handle requirement met event for " + player.getName(), e);
            }
        });
    }

    /**
     * Executes onRequirementConsume.
     */
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
                // This requirement doesn't belong to any rank requirement for this player
                // It might be a perk requirement, so we skip it
                LOGGER.fine("No matching rank requirement found for " + requirementTypeId + 
                           " for player " + player.getName() + " (might be a perk requirement)");
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
                
                rdq.getPlatform().getScheduler().runAtEntity(player, () -> {
                    if (player.isOnline()) {
                        // Send translated rank-up notification
                        new I18n.Builder("rank.messages.requirements_complete", player)
                                .withPlaceholder("rank", currentRank.getDisplayNameKey())
                                .build()
                                .sendMessage();
                        
                        new I18n.Builder("rank.messages.claim_upgrade", player)
                                .build()
                                .sendMessage();
                    }
                });
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to check for rank-up for " + player.getName(), e);
        }
    }
}
