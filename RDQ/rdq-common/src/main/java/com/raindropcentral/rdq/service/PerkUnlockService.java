package com.raindropcentral.rdq.service;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.perk.*;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for managing perk unlocking, requirement validation, and reward distribution.
 * Handles the business logic for checking if players can unlock perks and distributing rewards.
 */
public final class PerkUnlockService {

    private static final Logger LOGGER = CentralLogger.getLogger(PerkUnlockService.class.getName());

    private final @NotNull RDQ rdq;

    public PerkUnlockService(final @NotNull RDQ rdq) {
        this.rdq = Objects.requireNonNull(rdq, "rdq cannot be null");
    }

    /**
     * Checks if a player can unlock a specific perk.
     * Validates all requirements and returns detailed status.
     *
     * @param player the player to check
     * @param rdqPlayer the RDQ player entity
     * @param perk the perk to check
     * @return true if all requirements are met, false otherwise
     */
    public boolean canUnlockPerk(
        final @NotNull Player player,
        final @NotNull RDQPlayer rdqPlayer,
        final @NotNull RPerk perk
    ) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(rdqPlayer, "rdqPlayer cannot be null");
        Objects.requireNonNull(perk, "perk cannot be null");

        try {
            if (!perk.isEnabled()) {
                LOGGER.log(Level.FINE, "Perk " + perk.getIdentifier() + " is disabled");
                return false;
            }

            final Set<RPerkUnlockRequirement> unlockRequirements = perk.getUnlockRequirements();

            if (unlockRequirements.isEmpty()) {
                return true;
            }

            for (final RPerkUnlockRequirement unlockRequirement : unlockRequirements) {
                if (!unlockRequirement.isMet(player)) {
                    LOGGER.log(Level.FINE, "Player " + player.getName() + " does not meet requirement for perk " + perk.getIdentifier());
                    return false;
                }
            }

            return true;
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Error checking if player can unlock perk " + perk.getIdentifier(), exception);
            return false;
        }
    }



    /**
     * Unlocks a perk for a player and distributes rewards.
     *
     * @param player the Bukkit player
     * @param rdqPlayer the RDQ player entity
     * @param perk the perk to unlock
     * @return true if the perk was successfully unlocked, false otherwise
     */
    public boolean unlockPerkForPlayer(
        final @NotNull Player player,
        final @NotNull RDQPlayer rdqPlayer,
        final @NotNull RPerk perk
    ) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(rdqPlayer, "rdqPlayer cannot be null");
        Objects.requireNonNull(perk, "perk cannot be null");

        try {
            if (!this.canUnlockPerk(player, rdqPlayer, perk)) {
                LOGGER.log(Level.INFO, "Player " + player.getName() + " cannot unlock perk " + perk.getIdentifier());
                return false;
            }

            // Check if player already has this perk
            final RPlayerPerk existingPerk = this.getPlayerPerk(rdqPlayer, perk);
            if (existingPerk != null) {
                LOGGER.log(Level.FINE, "Player " + player.getName() + " already owns perk " + perk.getIdentifier());
                return false;
            }

            // Create new player perk association
            final RPlayerPerk playerPerk = new RPlayerPerk(rdqPlayer, perk, true);
            rdq.getPlayerPerkRepository().create(playerPerk);

            // Distribute rewards
            this.distributeRewards(player, rdqPlayer, perk);

            LOGGER.log(Level.INFO, "Successfully unlocked perk " + perk.getIdentifier() + " for player " + player.getName());
            return true;
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Error unlocking perk " + perk.getIdentifier() + " for player " + player.getName(), exception);
            return false;
        }
    }

    /**
     * Distributes all rewards associated with a perk to a player.
     *
     * @param player the Bukkit player
     * @param rdqPlayer the RDQ player entity
     * @param perk the perk whose rewards to distribute
     */
    public void distributeRewards(
        final @NotNull Player player,
        final @NotNull RDQPlayer rdqPlayer,
        final @NotNull RPerk perk
    ) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(rdqPlayer, "rdqPlayer cannot be null");
        Objects.requireNonNull(perk, "perk cannot be null");

        try {
            final Set<RPerkUnlockReward> unlockRewards = perk.getUnlockRewards();

            if (unlockRewards.isEmpty()) {
                return;
            }

            for (final RPerkUnlockReward unlockReward : unlockRewards) {
                try {
                    unlockReward.apply(player);
                } catch (final Exception exception) {
                    LOGGER.log(Level.WARNING, "Error distributing reward: " + unlockReward.getReward().getType(), exception);
                }
            }

            LOGGER.log(Level.FINE, "Distributed " + unlockRewards.size() + " rewards for perk " + perk.getIdentifier());
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Error distributing rewards for perk " + perk.getIdentifier(), exception);
        }
    }



    /**
     * Tracks progress toward a perk requirement for a player.
     *
     * @param rdqPlayer the RDQ player entity
     * @param requirement the requirement to track
     * @return the progress tracker entity
     */
    public @NotNull RPlayerPerkRequirementProgress trackRequirementProgress(
        final @NotNull RDQPlayer rdqPlayer,
        final @NotNull com.raindropcentral.rdq.database.entity.rank.RRequirement requirement
    ) {
        Objects.requireNonNull(rdqPlayer, "rdqPlayer cannot be null");
        Objects.requireNonNull(requirement, "requirement cannot be null");

        try {
            final List<RPlayerPerkRequirementProgress> existing = rdq.getPlayerPerkRequirementProgressRepository()
                .findListByAttributes(Map.of(
                    "player.uniqueId", rdqPlayer.getUniqueId(),
                    "requirement.id", requirement.getId()
                ));

            if (!existing.isEmpty()) {
                return existing.get(0);
            }

            final RPlayerPerkRequirementProgress progress = new RPlayerPerkRequirementProgress(rdqPlayer, requirement);
            rdq.getPlayerPerkRequirementProgressRepository().create(progress);
            return progress;
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Error tracking requirement progress", exception);
            return new RPlayerPerkRequirementProgress(rdqPlayer, requirement);
        }
    }

    /**
     * Updates progress for a specific requirement.
     *
     * @param progress the progress tracker to update
     * @param amount the amount to increment (0.0 to 1.0)
     */
    public void updateRequirementProgress(
        final @NotNull RPlayerPerkRequirementProgress progress,
        final double amount
    ) {
        Objects.requireNonNull(progress, "progress cannot be null");

        try {
            progress.incrementProgress(amount);
            rdq.getPlayerPerkRequirementProgressRepository().update(progress);
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Error updating requirement progress", exception);
        }
    }





    private @Nullable RPlayerPerk getPlayerPerk(final @NotNull RDQPlayer rdqPlayer, final @NotNull RPerk perk) {
        try {
            final List<RPlayerPerk> playerPerks = rdq.getPlayerPerkRepository()
                .findListByAttributes(Map.of(
                    "player.uniqueId", rdqPlayer.getUniqueId(),
                    "perk.identifier", perk.getIdentifier()
                ));
            return playerPerks.isEmpty() ? null : playerPerks.get(0);
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Error retrieving player perk", exception);
            return null;
        }
    }
}