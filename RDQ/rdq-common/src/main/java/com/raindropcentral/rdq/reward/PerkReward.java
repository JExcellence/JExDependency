package com.raindropcentral.rdq.reward;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.raindropcentral.rdq.database.entity.perk.Perk;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.repository.PerkRepository;
import com.raindropcentral.rdq.database.repository.RDQPlayerRepository;
import com.raindropcentral.rdq.perk.PerkManagementService;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.reward.AbstractReward;
import de.jexcellence.hibernate.repository.RepositoryManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reward that grants a perk to a player.
 * <p>
 * This reward type integrates with the perk system to unlock perks for players
 * as part of rank progression or other reward mechanisms.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
@JsonTypeName("PERK")
public final class PerkReward extends AbstractReward {

    private static final Logger LOGGER = CentralLogger.getLogger(PerkReward.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    
    // Service locator for perk management service
    private static PerkManagementService perkManagementService;

    private final String perkIdentifier;
    private final boolean autoEnable;
    
    /**
     * Sets the PerkManagementService instance for all PerkReward instances.
     * This should be called during plugin initialization.
     *
     * @param service the perk management service
     */
    public static void setPerkManagementService(@Nullable PerkManagementService service) {
        perkManagementService = service;
    }

    /**
     * Constructs a new PerkReward.
     *
     * @param perkIdentifier the identifier of the perk to grant
     * @param autoEnable whether to automatically enable the perk after granting
     */
    @JsonCreator
    public PerkReward(
        @JsonProperty("perkIdentifier") @NotNull String perkIdentifier,
        @JsonProperty("autoEnable") boolean autoEnable
    ) {
        this.perkIdentifier = perkIdentifier;
        this.autoEnable = autoEnable;
    }

    /**
     * Constructs a new PerkReward with auto-enable disabled.
     *
     * @param perkIdentifier the identifier of the perk to grant
     */
    public PerkReward(@NotNull String perkIdentifier) {
        this(perkIdentifier, false);
    }

    @Override
    public @NotNull String getTypeId() {
        return "PERK";
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if perk management service is available
                if (perkManagementService == null) {
                    LOGGER.severe("PerkManagementService not initialized. Call PerkReward.setPerkManagementService() during plugin startup.");
                    return false;
                }

                // Get repositories from RepositoryManager
                RepositoryManager repositoryManager = RepositoryManager.getInstance();
                if (repositoryManager == null) {
                    LOGGER.severe("RepositoryManager not available");
                    return false;
                }

                PerkRepository perkRepository = repositoryManager.getRepository(PerkRepository.class);
                if (perkRepository == null) {
                    LOGGER.severe("PerkRepository not available");
                    return false;
                }

                RDQPlayerRepository playerRepository = repositoryManager.getRepository(RDQPlayerRepository.class);
                if (playerRepository == null) {
                    LOGGER.severe("RDQPlayerRepository not available");
                    return false;
                }

                // Find the perk by identifier
                Perk perk = perkRepository.findAll().stream()
                        .filter(p -> p.getIdentifier().equals(perkIdentifier))
                        .findFirst()
                        .orElse(null);
                
                if (perk == null) {
                    LOGGER.warning("Perk not found: " + perkIdentifier);
                    return false;
                }

                // Get RDQPlayer
                RDQPlayer rdqPlayer = playerRepository.findAll().stream()
                        .filter(p -> p.getUniqueId().equals(player.getUniqueId()))
                        .findFirst()
                        .orElse(null);

                if (rdqPlayer == null) {
                    LOGGER.warning("RDQPlayer not found for: " + player.getName());
                    return false;
                }

                // Check if player already has the perk
                if (perkManagementService.hasUnlocked(rdqPlayer, perk)) {
                    LOGGER.log(Level.INFO, "Player {0} already has perk {1}", 
                        new Object[]{player.getName(), perkIdentifier});
                    return true;
                }

                // Grant the perk
                var playerPerk = perkManagementService.grantPerk(rdqPlayer, perk).join();
                if (playerPerk == null) {
                    LOGGER.warning("Failed to grant perk " + perkIdentifier + " to player " + player.getName());
                    return false;
                }

                // Auto-enable if configured
                if (autoEnable) {
                    boolean enabled = perkManagementService.enablePerk(rdqPlayer, perk).join();
                    if (!enabled) {
                        LOGGER.log(Level.INFO, "Perk {0} granted but could not be auto-enabled for player {1}", 
                            new Object[]{perkIdentifier, player.getName()});
                    }
                }

                // Send notification to player
                sendUnlockNotification(player, perk);

                LOGGER.log(Level.INFO, "Granted perk {0} to player {1}", 
                    new Object[]{perkIdentifier, player.getName()});
                return true;

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to grant perk " + perkIdentifier + 
                    " to player " + player.getName(), e);
                return false;
            }
        });
    }

    /**
     * Sends an unlock notification to the player.
     *
     * @param player the player
     * @param perk the perk that was unlocked
     */
    private void sendUnlockNotification(@NotNull Player player, @NotNull Perk perk) {
        try {
            // TODO: Use i18n translation keys when available
            String message = "<gold>✦</gold> <green>You unlocked the perk:</green> <yellow>" + 
                perk.getIdentifier() + "</yellow>";
            Component component = MINI_MESSAGE.deserialize(message);
            player.sendMessage(component);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to send perk unlock notification", e);
        }
    }

    @Override
    public double getEstimatedValue() {
        // Base value for perks
        // Could be enhanced to calculate based on perk rarity/power
        return 100.0;
    }

    @Override
    public void validate() {
        if (perkIdentifier == null || perkIdentifier.isEmpty()) {
            throw new IllegalArgumentException("Perk identifier cannot be empty");
        }
    }

    /**
     * Gets the perk identifier.
     *
     * @return the perk identifier
     */
    public String getPerkIdentifier() {
        return perkIdentifier;
    }

    /**
     * Gets whether the perk should be auto-enabled.
     *
     * @return true if auto-enable is enabled
     */
    public boolean isAutoEnable() {
        return autoEnable;
    }
}
