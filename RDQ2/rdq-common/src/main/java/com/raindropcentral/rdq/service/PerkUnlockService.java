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

public final class PerkUnlockService {

    private static final Logger LOGGER = CentralLogger.getLogger(PerkUnlockService.class.getName());

    private final RDQ rdq;

    public PerkUnlockService(final @NotNull RDQ rdq) {
        this.rdq = Objects.requireNonNull(rdq, "rdq cannot be null");
    }

    public boolean canUnlockPerk(@NotNull Player player, @NotNull RDQPlayer rdqPlayer, @NotNull RPerk perk) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(rdqPlayer, "rdqPlayer cannot be null");
        Objects.requireNonNull(perk, "perk cannot be null");

        try {
            if (!perk.isEnabled()) {
                LOGGER.log(Level.FINE, "Perk " + perk.getIdentifier() + " is disabled");
                return false;
            }

            var unlockRequirements = perk.getUnlockRequirements();
            return unlockRequirements.isEmpty() || unlockRequirements.stream().allMatch(req -> req.isMet(player));
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Error checking if player can unlock perk " + perk.getIdentifier(), exception);
            return false;
        }
    }

    public boolean unlockPerkForPlayer(@NotNull Player player, @NotNull RDQPlayer rdqPlayer, @NotNull RPerk perk) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(rdqPlayer, "rdqPlayer cannot be null");
        Objects.requireNonNull(perk, "perk cannot be null");

        try {
            if (!this.canUnlockPerk(player, rdqPlayer, perk)) {
                LOGGER.log(Level.INFO, "Player " + player.getName() + " cannot unlock perk " + perk.getIdentifier());
                return false;
            }

            var existingPerk = this.getPlayerPerk(rdqPlayer, perk);
            if (existingPerk != null) {
                LOGGER.log(Level.FINE, "Player " + player.getName() + " already owns perk " + perk.getIdentifier());
                return false;
            }

            var playerPerk = new RPlayerPerk(rdqPlayer, perk, true);
            rdq.getPlayerPerkRepository().create(playerPerk);
            this.distributeRewards(player, rdqPlayer, perk);

            LOGGER.log(Level.INFO, "Successfully unlocked perk " + perk.getIdentifier() + " for player " + player.getName());
            return true;
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Error unlocking perk " + perk.getIdentifier() + " for player " + player.getName(), exception);
            return false;
        }
    }

    public void distributeRewards(@NotNull Player player, @NotNull RDQPlayer rdqPlayer, @NotNull RPerk perk) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(rdqPlayer, "rdqPlayer cannot be null");
        Objects.requireNonNull(perk, "perk cannot be null");

        try {
            var unlockRewards = perk.getUnlockRewards();
            if (unlockRewards.isEmpty()) return;

            unlockRewards.forEach(reward -> {
                try {
                    reward.apply(player);
                } catch (Exception exception) {
                    LOGGER.log(Level.WARNING, "Error distributing reward: " + reward.getReward().getType(), exception);
                }
            });

            LOGGER.log(Level.FINE, "Distributed " + unlockRewards.size() + " rewards for perk " + perk.getIdentifier());
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Error distributing rewards for perk " + perk.getIdentifier(), exception);
        }
    }

    public @NotNull RPlayerPerkRequirementProgress trackRequirementProgress(
        @NotNull RDQPlayer rdqPlayer,
        @NotNull com.raindropcentral.rdq.database.entity.rank.RRequirement requirement
    ) {
        Objects.requireNonNull(rdqPlayer, "rdqPlayer cannot be null");
        Objects.requireNonNull(requirement, "requirement cannot be null");

        try {
            var existing = rdq.getPlayerPerkRequirementProgressRepository()
                .findListByAttributes(Map.of(
                    "player.uniqueId", rdqPlayer.getUniqueId(),
                    "requirement.id", requirement.getId()
                ));

            if (!existing.isEmpty()) {
                return existing.get(0);
            }

            var progress = new RPlayerPerkRequirementProgress(rdqPlayer, requirement);
            rdq.getPlayerPerkRequirementProgressRepository().create(progress);
            return progress;
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Error tracking requirement progress", exception);
            return new RPlayerPerkRequirementProgress(rdqPlayer, requirement);
        }
    }

    public void updateRequirementProgress(
        @NotNull RPlayerPerkRequirementProgress progress,
        double amount
    ) {
        Objects.requireNonNull(progress, "progress cannot be null");

        try {
            progress.incrementProgress(amount);
            rdq.getPlayerPerkRequirementProgressRepository().update(progress);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Error updating requirement progress", exception);
        }
    }

    private @Nullable RPlayerPerk getPlayerPerk(@NotNull RDQPlayer rdqPlayer, @NotNull RPerk perk) {
        try {
            var playerPerks = rdq.getPlayerPerkRepository()
                .findListByAttributes(Map.of(
                    "player.uniqueId", rdqPlayer.getUniqueId(),
                    "perk.identifier", perk.getIdentifier()
                ));
            return playerPerks.isEmpty() ? null : playerPerks.get(0);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Error retrieving player perk", exception);
            return null;
        }
    }
}