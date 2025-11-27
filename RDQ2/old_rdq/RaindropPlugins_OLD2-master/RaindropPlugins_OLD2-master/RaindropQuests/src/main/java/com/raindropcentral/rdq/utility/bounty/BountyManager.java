package com.raindropcentral.rdq.utility.bounty;

import com.raindropcentral.rdq.RDQImpl;
import com.raindropcentral.rdq.config.bounty.BountySection;
import com.raindropcentral.rdq.config.bounty.EBountyClaimMode;
import com.raindropcentral.rdq.database.entity.RBounty;
import com.raindropcentral.rdq.database.entity.RDQPlayer;
import com.raindropcentral.rdq.view.bounty.RewardItem;
import com.raindropcentral.rplatform.api.PlatformAPI;
import com.raindropcentral.rplatform.logger.CentralLogger;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.translate.api.I18n;
import de.jexcellence.translate.api.MessageKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages all bounty-related operations in the RaindropQuests system.
 * <p>
 * This class is responsible for creating, tracking, and resolving bounties on players,
 * including reward distribution, damage tracking, and updating player displays.
 * It also handles configuration for bounty claim modes and integrates with the
 * plugin's repositories and messaging systems.
 * </p>
 *
 * <ul>
 *     <li>Tracks active bounties and damage dealt by attackers.</li>
 *     <li>Handles creation, removal, and claiming of bounties.</li>
 *     <li>Distributes item rewards and manages inventory overflow.</li>
 *     <li>Updates player display names to indicate bounty status.</li>
 *     <li>Supports configurable claim modes (e.g., last hit, most damage).</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class BountyManager {
	
	private final static Logger LOGGER = CentralLogger.getLogger(BountyManager.class.getName());
	
	/**
	 * The folder where bounty configuration files are stored.
	 */
	private final static String BOUNTY_FOLDER = "bounty";
	
	/**
	 * The name of the bounty configuration file.
	 */
	private final static String BOUNTY_FILE   = "bounty.yml";
	
	/**
	 * Map of active bounties, keyed by the target player's UUID.
	 */
	private final Map<UUID, RBounty>           activeBounties = new ConcurrentHashMap<>();
	
	/**
	 * Tracks damage dealt to each bounty target, mapping target UUIDs to a map of attacker UUIDs and their damage.
	 */
	private final Map<UUID, Map<UUID, Double>> damageTracker  = new ConcurrentHashMap<>();
	
	/**
	 * Reference to the main RDQImpl plugin instance.
	 */
	private final RDQImpl rdq;
	
	/**
	 * The current bounty claim mode (e.g., last hit, most damage).
	 */
	private EBountyClaimMode claimMode;
	
	/**
	 * Platform API for cross-platform compatibility.
	 */
	private final PlatformAPI platformAPI;
	
	/**
	 * Constructs a new BountyManager and loads configuration for claim mode.
	 *
	 * @param rdq the main RDQImpl plugin instance
	 */
	public BountyManager(final @NotNull RDQImpl rdq) {
		this.rdq = rdq;
		this.platformAPI = rdq.getPlatform().getPlatformAPI();
		
		try {
			final ConfigManager cfgManager = new ConfigManager(
				this.rdq.getImpl(),
				BOUNTY_FOLDER
			);
			final ConfigKeeper<BountySection> cfgKeeper = new ConfigKeeper<>(
				cfgManager,
				BOUNTY_FILE,
				BountySection.class
			);
			
			this.claimMode = cfgKeeper.rootSection.getClaimMode();
		} catch (final Exception exception) {
			this.claimMode = EBountyClaimMode.LAST_HIT;
			LOGGER.log(
				Level.WARNING,
				"Error while loading bounty configuration, loading fallback",
				exception
			);
		}
	}
	
	/**
	 * Creates a new bounty for the specified target player, with the given commissioner, item rewards, and currency rewards.
	 * <p>
	 * The bounty is persisted asynchronously, and upon success, all online players are notified.
	 * </p>
	 *
	 * @param targetPlayer     the player who is the target of the bounty
	 * @param commissioner     the player who places the bounty
	 * @param rewardItems      the set of item rewards for the bounty
	 * @param rewardCurrencies the map of currency rewards for the bounty
	 */
	public void createBounty(
		final @NotNull RDQPlayer targetPlayer,
		final @NotNull Player commissioner,
		final @NotNull Set<RewardItem> rewardItems,
		final @NotNull Map<String, Double> rewardCurrencies
	) {
		final RBounty bounty = new RBounty(targetPlayer, commissioner, rewardItems, rewardCurrencies);
		
		this.rdq
			.getBountyRepository()
			.createAsync(bounty)
			.whenCompleteAsync(
				(rBounty, throwable) -> {
					if (
						throwable != null
					) {
						LOGGER.warning("Error occurred while creating bounty: " + throwable.getMessage());
						return;
					}
					
					activeBounties.put(targetPlayer.getUniqueId(), rBounty);
					damageTracker.put(targetPlayer.getUniqueId(), new HashMap<>());
					
					Bukkit
						.getOnlinePlayers()
						.forEach(player -> {
                            I18n.create(
                                    MessageKey.of("bounty.created.broadcast"),
                                    player
                            ).withPlaceholders(Map.of(
                                    "target_name", targetPlayer.getPlayerName(),
                                    "commissioner_name", commissioner.getName()
                            )).sendTitle();
						});
					
					this.updateBountyPlayerDisplay(targetPlayer.getUniqueId());
				}, this.rdq.getExecutor()
			)
		;
	}
	
	/**
	 * Removes an active bounty and its associated damage tracking for the specified player.
	 * Also updates the player's display to remove bounty indicators.
	 *
	 * @param targetUniqueId the UUID of the player whose bounty should be removed
	 */
	public void removeBounty(
		final @NotNull UUID targetUniqueId
	) {
		activeBounties.remove(targetUniqueId);
		damageTracker.remove(targetUniqueId);
		this.updateBountyPlayerDisplay(targetUniqueId);
	}
	
	/**
	 * Tracks damage dealt by an attacker to a bounty target.
	 * <p>
	 * Accumulates damage for each attacker per target.
	 * </p>
	 *
	 * @param targetUniqueId   the UUID of the bounty target
	 * @param attackerUniqueId the UUID of the attacker
	 * @param damage           the amount of damage dealt
	 */
	public void trackDamage(
		final @NotNull UUID targetUniqueId,
		final @NotNull UUID attackerUniqueId,
		final double damage
	) {
		if (
			this.activeBounties.containsKey(targetUniqueId)
		) {
			Map<UUID, Double> attackerDamages = this.damageTracker.computeIfAbsent(
				targetUniqueId, k -> new HashMap<>()
			);
			attackerDamages.merge(attackerUniqueId, damage, Double::sum);
		}
	}
	
	/**
	 * Handles the event when a player with an active bounty is killed.
	 * <p>
	 * Determines the winner based on the configured claim mode, distributes rewards,
	 * updates repositories, removes the bounty, and notifies all players.
	 * </p>
	 *
	 * @param killedPlayer the player who was killed
	 */
	public void handleBountyKill(
		final @NotNull Player killedPlayer
	) {
		final UUID targetUniqueId = killedPlayer.getUniqueId();
		RBounty    bounty         = activeBounties.get(targetUniqueId);
		if (
			bounty == null
		) {
			return;
		}
		
		final UUID receiverUniqueId = this.determineBountyWinner(targetUniqueId);
		if (
			receiverUniqueId == null
		) {
			return;
		}
		
		Player winner = Bukkit.getPlayer(receiverUniqueId);
		if (
			winner != null
		) {
			this.giveRewardItemsToPlayer(
				winner,
				bounty.getRewardItems()
			);
			
			Bukkit
				.getOnlinePlayers()
				.forEach(player -> {
                    I18n.create(
                            MessageKey.of("bounty.claimed.player"),
                            player
                    ).withPlaceholders(Map.of(
                            "target_name", killedPlayer.getName(),
                            "receiver_name", winner.getName()
                    )).sendMessage();
				});
		}
		
		RDQPlayer targetPlayer = bounty.getPlayer();
		targetPlayer.setBounty(null);

		this.rdq
			.getPlayerRepository()
			.updateAsync(targetPlayer)
			.whenCompleteAsync(
				(
					(unused, throwable) -> {
						if (
							throwable != null
						) {
							LOGGER.warning("Error occurred while deleting bounty: " + throwable.getMessage());
							return;
						}
						
						this.rdq
							.getBountyRepository()
							.delete(bounty.getId());
						
						this.removeBounty(targetUniqueId);
						
						Bukkit
							.getOnlinePlayers()
							.forEach(player -> {
                                I18n.create(
                                        MessageKey.of("bounty.claimed.broadcast"),
                                        player
                                ).withPlaceholders(Map.of(
                                        "target_name", targetPlayer.getPlayerName(),
                                        "receiver_name", winner.getName()
                                )).sendMessage();
							});
					}
				)
			)
		;
	}
	
	/**
	 * Adds item rewards to an existing bounty.
	 * <p>
	 * (Currently a stub; implement logic to add items to the bounty's reward set.)
	 * </p>
	 *
	 * @param bounty the bounty to add rewards to
	 * @param items  the list of item stacks to add as rewards
	 * @return the updated bounty
	 */
	public RBounty addItemRewards(
		final @NotNull RBounty bounty,
		final @NotNull List<ItemStack> items
	) {
		return bounty;
	}
	
	/**
	 * Adds a currency reward to an existing bounty.
	 * <p>
	 * (Currently a stub; implement logic to add currency to the bounty's reward map.)
	 * </p>
	 *
	 * @param bounty       the bounty to add the currency reward to
	 * @param currencyName the name of the currency
	 * @param amount       the amount to add
	 * @return the updated bounty
	 */
	public RBounty addCurrencyReward(
		final @NotNull RBounty bounty,
		final @NotNull String currencyName,
		final double amount
	) {
		return bounty;
	}
	
	/**
	 * Updates the display name and player list name of a player to indicate bounty status.
	 * Uses cross-platform compatibility to work with both Paper and Spigot/Bukkit.
	 *
	 * @param playerUniqueId the UUID of the player to update
	 */
	public void updateBountyPlayerDisplay(final @NotNull UUID playerUniqueId) {
		final Player player = Bukkit.getPlayer(playerUniqueId);
		if (player == null) {
			return;
		}
		
		if (this.activeBounties.containsKey(playerUniqueId)) {
			updateBountyDisplay(player);
		} else {
			resetPlayerDisplay(player);
		}
	}
	
	private void updateBountyDisplay(final @NotNull Player player) {
		platformAPI.setDisplayName(
			player,
            I18n.create(MessageKey.of("bounty.display.player_list_name"), player).withPlaceholders(
                    Map.of(
                            "bounty_symbol", "☠",
                            "player_name", player.getName()
                    )
            ).build().component()
		);
		
		LOGGER.fine("Updated bounty display for " + player.getName() + " using " + platformAPI.getPlatformType().getDisplayName());
	}
	
	private void resetPlayerDisplay(final @NotNull Player player) {
		platformAPI.setDisplayName(player, Component.text(player.getName()));
		LOGGER.fine("Reset display names for " + player.getName());
	}
	
	/**
	 * Checks if a player currently has an active bounty.
	 *
	 * @param playerUniqueId the UUID of the player to check
	 * @return true if the player has an active bounty, false otherwise
	 */
	public boolean hasActiveBounty(
		final @NotNull UUID playerUniqueId
	) {
		return this.activeBounties.containsKey(playerUniqueId);
	}
	
	/**
	 * Retrieves the active bounty for a player, if any.
	 *
	 * @param playerUniqueId the UUID of the player
	 * @return the RBounty instance, or null if none exists
	 */
	public RBounty getBounty(
		final @NotNull UUID playerUniqueId
	) {
		return activeBounties.get(playerUniqueId);
	}
	
	/**
	 * Determines the winner of a bounty based on the configured claim mode.
	 * <p>
	 * Returns the UUID of the player who should receive the bounty rewards.
	 * </p>
	 *
	 * @param targetUniqueId the UUID of the bounty target
	 * @return the UUID of the winning player, or null if no winner can be determined
	 */
	private UUID determineBountyWinner(
		final @NotNull UUID targetUniqueId
	) {
		Map<UUID, Double> damages = this.damageTracker.get(targetUniqueId);
		if (
			damages == null ||
			damages.isEmpty()
		) {
			return null;
		}
		
		return (
			claimMode == EBountyClaimMode.LAST_HIT
		) ? this.getLastDamager(targetUniqueId) : this.getTopDamager(damages);
	}
	
	/**
	 * Gets the UUID of the last damager for a bounty target.
	 * <p>
	 * (Currently falls back to top damager.)
	 * </p>
	 *
	 * @param targetUniqueId the UUID of the bounty target
	 * @return the UUID of the last damager, or null if not found
	 */
	private UUID getLastDamager(
		final @NotNull UUID targetUniqueId
	) {
		return this.getTopDamager(
			this.damageTracker.get(targetUniqueId)
		); //Fallback
	}
	
	/**
	 * Gets the UUID of the player who dealt the most damage to a bounty target.
	 *
	 * @param damages a map of attacker UUIDs to damage dealt
	 * @return the UUID of the top damager, or null if not found
	 */
	private @Nullable UUID getTopDamager(
		final @NotNull Map<UUID, Double> damages
	) {
		return damages
			       .entrySet()
			       .stream()
			       .max(Map.Entry.comparingByValue())
			       .map(Map.Entry::getKey)
			       .orElse(null);
	}
	
	/**
	 * Gives the specified reward items to the player, splitting stacks as needed.
	 * If the player's inventory is full, excess items are dropped at their location.
	 * Notifies the player if any items could not fit in their inventory.
	 *
	 * @param player      the player to receive the items
	 * @param rewardItems the set of RewardItem to give
	 */
	public void giveRewardItemsToPlayer(
		@NotNull Player player,
		@NotNull Set<RewardItem> rewardItems
	) {
		List<ItemStack> leftovers = new ArrayList<>();
		for (RewardItem rewardItem : rewardItems) {
			ItemStack item     = rewardItem.getItem();
			int       amount   = rewardItem.getAmount();
			int       maxStack = item.getMaxStackSize();
			
			ItemStack singleStack = item.clone();
			singleStack.setAmount(1);
			
			while (amount > 0) {
				int       giveAmount  = Math.min(amount, maxStack);
				ItemStack stackToGive = singleStack.clone();
				stackToGive.setAmount(giveAmount);
				
				HashMap<Integer, ItemStack> notFit = player
					                                     .getInventory()
					                                     .addItem(stackToGive);
				if (!notFit.isEmpty()) {
					leftovers.addAll(notFit.values());
				}
				amount -= giveAmount;
			}
		}
		if (!leftovers.isEmpty()) {
			leftovers.forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));

            I18n.create(MessageKey.of("bounty_reward_ui.left_overs"), player).sendMessage();
		}
	}
}
