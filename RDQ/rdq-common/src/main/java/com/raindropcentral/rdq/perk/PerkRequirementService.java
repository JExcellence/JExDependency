package com.raindropcentral.rdq.perk;

import com.raindropcentral.rdq.database.entity.perk.Perk;
import com.raindropcentral.rdq.database.entity.perk.PerkRequirement;
import com.raindropcentral.rdq.database.entity.perk.PerkUnlockReward;
import com.raindropcentral.rdq.database.entity.perk.PlayerPerk;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.RequirementService;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service for checking perk requirements, tracking progress, and handling perk unlocks.
 * <p>
 * This service handles:
 * - Checking if all requirements for a perk are met
 * - Calculating progress for individual requirements
 * - Calculating overall progress for a perk
 * - Attempting to unlock perks when requirements are met
 * - Granting unlock rewards
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PerkRequirementService {
    
    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
    
    private final PerkManagementService perkManagementService;
    private final RequirementService requirementService;
    
    /**
     * Constructs a new PerkRequirementService.
     *
     * @param perkManagementService the perk management service
     */
    public PerkRequirementService(
            @NotNull final PerkManagementService perkManagementService
    ) {
        this.perkManagementService = perkManagementService;
        this.requirementService = RequirementService.getInstance();
    }
    
    // ==================== Requirement Checking Methods ====================
    
    /**
     * Checks if a player can unlock a perk by verifying all requirements are met.
     *
     * @param player the player to check
     * @param perk the perk to check
     * @return true if all requirements are met, false otherwise
     */
    public boolean canUnlock(@NotNull final Player player, @NotNull final Perk perk) {
        try {
            final Set<PerkRequirement> requirements = perk.getRequirements();
            
            // If no requirements, perk can be unlocked
            if (requirements.isEmpty()) {
                LOGGER.log(Level.FINE, "Perk {0} has no requirements, can be unlocked", perk.getIdentifier());
                return true;
            }
            
            // Check all requirements using RequirementService
            for (PerkRequirement perkRequirement : requirements) {
                final AbstractRequirement requirement = perkRequirement.getRequirement();
                final boolean isMet = requirementService.isMet(player, requirement);
                
                if (!isMet) {
                    LOGGER.log(Level.FINE, "Requirement {0} not met for perk {1}", 
                            new Object[]{requirement.getTypeId(), perk.getIdentifier()});
                    return false;
                }
            }
            
            LOGGER.log(Level.FINE, "All requirements met for perk {0}", perk.getIdentifier());
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to check if player " + player.getName() + 
                    " can unlock perk " + perk.getIdentifier(), e);
            return false;
        }
    }
    
    /**
     * Checks all requirements for a perk and returns individual results.
     *
     * @param player the player to check
     * @param perk the perk to check
     * @return a map of PerkRequirement to boolean indicating if each requirement is met
     */
    public Map<PerkRequirement, Boolean> checkRequirements(
            @NotNull final Player player,
            @NotNull final Perk perk
    ) {
        final Map<PerkRequirement, Boolean> results = new LinkedHashMap<>();
        
        try {
            final Set<PerkRequirement> requirements = perk.getRequirements();
            
            // Sort requirements by display order
            final List<PerkRequirement> sortedRequirements = requirements.stream()
                    .sorted(Comparator.comparingInt(PerkRequirement::getDisplayOrder))
                    .collect(Collectors.toList());
            
            // Check each requirement
            for (PerkRequirement perkRequirement : sortedRequirements) {
                try {
                    final AbstractRequirement requirement = perkRequirement.getRequirement();
                    final boolean isMet = requirementService.isMet(player, requirement);
                    results.put(perkRequirement, isMet);
                    
                    LOGGER.log(Level.FINEST, "Requirement {0} for perk {1}: {2}", 
                            new Object[]{requirement.getTypeId(), perk.getIdentifier(), isMet ? "MET" : "NOT MET"});
                    
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to check requirement " + perkRequirement.getId() + 
                            " for perk " + perk.getIdentifier(), e);
                    results.put(perkRequirement, false);
                }
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to check requirements for perk " + perk.getIdentifier(), e);
        }
        
        return results;
    }
    
    // ==================== Progress Tracking Methods ====================
    
    /**
     * Gets the progress for each requirement of a perk.
     *
     * @param player the player to check
     * @param perk the perk to check
     * @return a map of PerkRequirement to progress value (0.0 to 1.0)
     */
    public Map<PerkRequirement, Double> getRequirementProgress(
            @NotNull final Player player,
            @NotNull final Perk perk
    ) {
        final Map<PerkRequirement, Double> progressMap = new LinkedHashMap<>();
        
        try {
            final Set<PerkRequirement> requirements = perk.getRequirements();
            
            // Sort requirements by display order
            final List<PerkRequirement> sortedRequirements = requirements.stream()
                    .sorted(Comparator.comparingInt(PerkRequirement::getDisplayOrder))
                    .collect(Collectors.toList());
            
            // Calculate progress for each requirement
            for (PerkRequirement perkRequirement : sortedRequirements) {
                try {
                    final AbstractRequirement requirement = perkRequirement.getRequirement();
                    final double progress = requirementService.calculateProgress(player, requirement);
                    
                    // Clamp progress between 0.0 and 1.0
                    final double clampedProgress = Math.max(0.0, Math.min(1.0, progress));
                    progressMap.put(perkRequirement, clampedProgress);
                    
                    LOGGER.log(Level.FINEST, "Progress for requirement {0} of perk {1}: {2}%", 
                            new Object[]{requirement.getTypeId(), perk.getIdentifier(), 
                                    (int)(clampedProgress * 100)});
                    
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to calculate progress for requirement " + 
                            perkRequirement.getId() + " of perk " + perk.getIdentifier(), e);
                    progressMap.put(perkRequirement, 0.0);
                }
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get requirement progress for perk " + perk.getIdentifier(), e);
        }
        
        return progressMap;
    }
    
    /**
     * Calculates the overall progress for a perk by averaging all requirement progress values.
     *
     * @param player the player to check
     * @param perk the perk to check
     * @return overall progress value (0.0 to 1.0)
     */
    public double getOverallProgress(@NotNull final Player player, @NotNull final Perk perk) {
        try {
            final Set<PerkRequirement> requirements = perk.getRequirements();
            
            // If no requirements, perk is 100% complete
            if (requirements.isEmpty()) {
                return 1.0;
            }
            
            // Get all requirement progress values
            final List<AbstractRequirement> abstractRequirements = requirements.stream()
                    .map(PerkRequirement::getRequirement)
                    .collect(Collectors.toList());
            
            // Use RequirementService to calculate overall progress
            final double overallProgress = requirementService.calculateOverallProgress(player, abstractRequirements);
            
            LOGGER.log(Level.FINE, "Overall progress for perk {0}: {1}%", 
                    new Object[]{perk.getIdentifier(), (int)(overallProgress * 100)});
            
            return overallProgress;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to calculate overall progress for perk " + perk.getIdentifier(), e);
            return 0.0;
        }
    }
    
    // ==================== Unlock Logic Methods ====================
    
    /**
     * Attempts to unlock a perk for a player.
     * <p>
     * This method:
     * 1. Checks if the player already has the perk unlocked
     * 2. Verifies all requirements are met
     * 3. Consumes requirement resources
     * 4. Grants the perk to the player
     * 5. Grants unlock rewards
     * 6. Sends a notification to the player
     * </p>
     *
     * @param player the player attempting to unlock the perk
     * @param rdqPlayer the RDQPlayer entity
     * @param perk the perk to unlock
     * @return a CompletableFuture containing the unlock result
     */
    public CompletableFuture<UnlockResult> attemptUnlock(
            @NotNull final Player player,
            @NotNull final RDQPlayer rdqPlayer,
            @NotNull final Perk perk
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if player already has the perk unlocked
                if (perkManagementService.hasUnlocked(rdqPlayer, perk)) {
                    LOGGER.log(Level.FINE, "Player {0} already has perk {1} unlocked", 
                            new Object[]{player.getName(), perk.getIdentifier()});
                    return new UnlockResult(
                            false,
                            UnlockResultType.ALREADY_UNLOCKED,
                            "perk.unlock.already_unlocked",
                            null
                    );
                }
                
                // Check if all requirements are met
                if (!canUnlock(player, perk)) {
                    LOGGER.log(Level.FINE, "Player {0} does not meet requirements for perk {1}", 
                            new Object[]{player.getName(), perk.getIdentifier()});
                    return new UnlockResult(
                            false,
                            UnlockResultType.REQUIREMENTS_NOT_MET,
                            "perk.unlock.requirements_not_met",
                            null
                    );
                }
                
                // Consume requirement resources on the main thread (required for Bukkit events)
                final Set<PerkRequirement> requirements = perk.getRequirements();
                final List<PerkRequirement> sortedRequirements = requirements.stream()
                        .sorted(Comparator.comparingInt(PerkRequirement::getDisplayOrder))
                        .collect(Collectors.toList());
                
                // Run consumption synchronously on main thread
                CompletableFuture<Boolean> consumptionFuture = new CompletableFuture<>();
                org.bukkit.Bukkit.getScheduler().runTask(
                        org.bukkit.Bukkit.getPluginManager().getPlugin("RDQ"),
                        () -> {
                            try {
                                for (PerkRequirement perkRequirement : sortedRequirements) {
                                    try {
                                        final AbstractRequirement requirement = perkRequirement.getRequirement();
                                        
                                        // Log the shouldConsume value for debugging
                                        LOGGER.log(Level.INFO, "Requirement {0} shouldConsume: {1}", 
                                                new Object[]{requirement.getTypeId(), requirement.shouldConsume()});
                                        
                                        // Only consume if the requirement is configured to consume
                                        if (requirement.shouldConsume()) {
                                            LOGGER.log(Level.INFO, "Consuming requirement {0} for perk {1}", 
                                                    new Object[]{requirement.getTypeId(), perk.getIdentifier()});
                                            requirementService.consume(player, requirement);
                                        } else {
                                            LOGGER.log(Level.INFO, "Skipping consumption for requirement {0} (shouldConsume=false)", 
                                                    requirement.getTypeId());
                                        }
                                        
                                    } catch (Exception e) {
                                        LOGGER.log(Level.SEVERE, "Failed to consume requirement " + perkRequirement.getId() + 
                                                " for perk " + perk.getIdentifier(), e);
                                        consumptionFuture.complete(false);
                                        return;
                                    }
                                }
                                consumptionFuture.complete(true);
                            } catch (Exception e) {
                                LOGGER.log(Level.SEVERE, "Error during requirement consumption", e);
                                consumptionFuture.complete(false);
                            }
                        }
                );
                
                // Wait for consumption to complete
                if (!consumptionFuture.join()) {
                    return new UnlockResult(
                            false,
                            UnlockResultType.CONSUMPTION_FAILED,
                            "perk.unlock.consumption_failed",
                            null
                    );
                }
                
                // Grant the perk to the player with auto-enable
                final PlayerPerk playerPerk = perkManagementService.grantPerk(rdqPlayer, perk, true).join();
                
                if (playerPerk == null) {
                    LOGGER.log(Level.SEVERE, "Failed to grant perk {0} to player {1}", 
                            new Object[]{perk.getIdentifier(), player.getName()});
                    return new UnlockResult(
                            false,
                            UnlockResultType.GRANT_FAILED,
                            "perk.unlock.grant_failed",
                            null
                    );
                }
                
                // Validate perk state after grant
                if (!playerPerk.isUnlocked()) {
                    LOGGER.log(Level.SEVERE, "Perk {0} granted but not unlocked for player {1}", 
                            new Object[]{perk.getIdentifier(), player.getName()});
                    return new UnlockResult(
                            false,
                            UnlockResultType.GRANT_FAILED,
                            "perk.unlock.grant_failed",
                            null
                    );
                }
                
                if (!playerPerk.isEnabled()) {
                    LOGGER.log(Level.SEVERE, "Perk {0} granted and unlocked but not enabled for player {1} (data inconsistency)", 
                            new Object[]{perk.getIdentifier(), player.getName()});
                    return new UnlockResult(
                            false,
                            UnlockResultType.GRANT_FAILED,
                            "perk.unlock.grant_failed",
                            null
                    );
                }
                
                LOGGER.log(Level.INFO, "Successfully granted and enabled perk {0} to player {1}", 
                        new Object[]{perk.getIdentifier(), player.getName()});
                
                // Grant unlock rewards
                final Set<PerkUnlockReward> unlockRewards = perk.getUnlockRewards();
                final List<PerkUnlockReward> sortedRewards = unlockRewards.stream()
                        .sorted(Comparator.comparingInt(PerkUnlockReward::getDisplayOrder))
                        .collect(Collectors.toList());
                
                for (PerkUnlockReward unlockReward : sortedRewards) {
                    try {
                        LOGGER.log(Level.FINE, "Granting unlock reward {0} for perk {1}", 
                                new Object[]{unlockReward.getTypeId(), perk.getIdentifier()});
                        
                        final boolean rewardGranted = unlockReward.grant(player).join();
                        
                        if (!rewardGranted) {
                            LOGGER.log(Level.WARNING, "Failed to grant unlock reward {0} for perk {1}", 
                                    new Object[]{unlockReward.getTypeId(), perk.getIdentifier()});
                        }
                        
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error granting unlock reward " + unlockReward.getId() + 
                                " for perk " + perk.getIdentifier(), e);
                    }
                }
                
                // Send notification to player
                sendUnlockNotification(player, perk);
                
                LOGGER.log(Level.INFO, "Player {0} successfully unlocked perk {1}", 
                        new Object[]{player.getName(), perk.getIdentifier()});
                
                return new UnlockResult(
                        true,
                        UnlockResultType.SUCCESS,
                        "perk.unlock.success",
                        playerPerk
                );
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to unlock perk " + perk.getIdentifier() + 
                        " for player " + player.getName(), e);
                return new UnlockResult(
                        false,
                        UnlockResultType.ERROR,
                        "perk.unlock.error",
                        null
                );
            }
        });
    }
    
    /**
     * Sends an unlock notification to the player.
     *
     * @param player the player to notify
     * @param perk the perk that was unlocked
     */
    private void sendUnlockNotification(@NotNull final Player player, @NotNull final Perk perk) {
        try {
            // Get the perk name from i18n
            final String nameKey = perk.getIcon().getDisplayNameKey();
            
            // Send notification message
            new I18n.Builder("reward.perk.unlocked", player)
                    .includePrefix()
                    .withPlaceholder("perk", nameKey != null ? nameKey : perk.getIdentifier())
                    .build()
                    .sendMessage();
            
            LOGGER.log(Level.FINE, "Sent unlock notification to player {0} for perk {1}", 
                    new Object[]{player.getName(), perk.getIdentifier()});
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to send unlock notification to player " + player.getName() + 
                    " for perk " + perk.getIdentifier(), e);
        }
    }
    
    // ==================== Result Classes ====================
    
    /**
     * Result of a perk unlock attempt.
     */
    public static class UnlockResult {
        private final boolean success;
        private final UnlockResultType resultType;
        private final String messageKey;
        private final PlayerPerk playerPerk;
        
        public UnlockResult(
                final boolean success,
                @NotNull final UnlockResultType resultType,
                @NotNull final String messageKey,
                final PlayerPerk playerPerk
        ) {
            this.success = success;
            this.resultType = resultType;
            this.messageKey = messageKey;
            this.playerPerk = playerPerk;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public UnlockResultType getResultType() {
            return resultType;
        }
        
        public String getMessageKey() {
            return messageKey;
        }
        
        public PlayerPerk getPlayerPerk() {
            return playerPerk;
        }
        
        /**
         * Sends the result message to the player.
         *
         * @param player the player to send the message to
         */
        public void sendMessage(@NotNull final Player player) {
            new I18n.Builder(messageKey, player)
                    .includePrefix()
                    .build()
                    .sendMessage();
        }
        
        @Override
        public String toString() {
            return "UnlockResult{" +
                    "success=" + success +
                    ", resultType=" + resultType +
                    ", messageKey='" + messageKey + '\'' +
                    ", playerPerk=" + (playerPerk != null ? playerPerk.getId() : null) +
                    '}';
        }
    }
    
    /**
     * Types of unlock results.
     */
    public enum UnlockResultType {
        SUCCESS,
        ALREADY_UNLOCKED,
        REQUIREMENTS_NOT_MET,
        CONSUMPTION_FAILED,
        GRANT_FAILED,
        ERROR
    }
}
