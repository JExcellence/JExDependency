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
import de.jexcellence.jextranslate.i18n.I18n;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reward that grants a perk to a player.
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Getter
@JsonTypeName("PERK")
public final class PerkReward extends AbstractReward {
	
	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
	private static final RepositoryManager REPOSITORY_MANAGER = RepositoryManager.getInstance();
	private static final PerkRepository PERK_REPOSITORY = REPOSITORY_MANAGER.getRepository(PerkRepository.class);
	private static final RDQPlayerRepository PLAYER_REPOSITORY = REPOSITORY_MANAGER.getRepository(RDQPlayerRepository.class);
	
	private static PerkManagementService perkManagementService;
	
	private final String perkIdentifier;
	private final boolean autoEnable;
	
	/**
	 * Sets the PerkManagementService instance.
	 *
	 * @param service the perk management service
	 */
	public static void setPerkManagementService(@Nullable PerkManagementService service) {
		perkManagementService = service;
	}
	
	/**
	 * Executes PerkReward.
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
	 * Executes PerkReward.
	 */
	public PerkReward(@NotNull String perkIdentifier) {
		this(perkIdentifier, false);
	}
	
	/**
	 * Gets typeId.
	 */
	@Override
	public @NotNull String getTypeId() {
		return "PERK";
	}
	
	/**
	 * Executes grant.
	 */
	@Override
	public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
		if (perkManagementService == null) {
			LOGGER.severe("PerkManagementService not initialized");
			return CompletableFuture.completedFuture(false);
		}
		
		return PERK_REPOSITORY.findByKeyAsync("identifier", perkIdentifier)
				.thenCompose(perkOpt -> {
					if (perkOpt.isEmpty()) {
						LOGGER.warning("Perk not found: " + perkIdentifier);
						return CompletableFuture.completedFuture(false);
					}
					
					Perk perk = perkOpt.get();
					
					return PLAYER_REPOSITORY.findByKeyAsync("uniqueId", player.getUniqueId())
							.thenCompose(playerOpt -> {
								if (playerOpt.isEmpty()) {
									LOGGER.warning("RDQPlayer not found: " + player.getUniqueId());
									return CompletableFuture.completedFuture(false);
								}
								
								RDQPlayer rdqPlayer = playerOpt.get();
								
								if (perkManagementService.hasUnlocked(rdqPlayer, perk)) {
									return CompletableFuture.completedFuture(true);
								}
								
								return perkManagementService.grantPerk(rdqPlayer, perk, autoEnable)
										.thenApply(playerPerk -> {
											if (playerPerk == null || !playerPerk.isUnlocked()) {
												LOGGER.log(Level.WARNING, "Failed to grant perk {0} to {1}",
														new Object[]{perkIdentifier, player.getName()});
												return false;
											}
											
											if (autoEnable && !playerPerk.isEnabled()) {
												LOGGER.log(Level.WARNING, "Perk {0} not auto-enabled for {1}",
														new Object[]{perkIdentifier, player.getName()});
												return false;
											}
											
											sendUnlockNotification(player, perk);
											return true;
										});
							});
				})
				.exceptionally(ex -> {
					LOGGER.log(Level.SEVERE, "Error granting perk " + perkIdentifier + " to " + player.getName(), ex);
					return false;
				});
	}
	
	private void sendUnlockNotification(@NotNull Player player, @NotNull Perk perk) {
		new I18n.Builder("reward.perk.unlocked", player)
				.withPlaceholder("perk", perk.getIdentifier())
				.build()
				.sendMessage();
	}
	
	/**
	 * Gets estimatedValue.
	 */
	@Override
	public double getEstimatedValue() {
		return 100.0;
	}
	
	/**
	 * Executes validate.
	 */
	@Override
	public void validate() {
		if (perkIdentifier == null || perkIdentifier.isEmpty()) {
			throw new IllegalArgumentException("Perk identifier cannot be empty");
		}
	}
}
