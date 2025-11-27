package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.RDQImpl;
import com.raindropcentral.rdq.database.entity.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRank;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.utility.bounty.BountyManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Listener for bounty-related player events.
 * <p>
 * Handles tracking of player damage for bounty purposes, bounty claim logic on player death,
 * and updating player display when they join the server.
 * </p>
 *
 * <ul>
 *     <li>Tracks damage dealt to players with active bounties.</li>
 *     <li>Handles bounty reward distribution on player death.</li>
 *     <li>Updates player display to reflect bounty status on join.</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class BountyListener implements Listener {
	
	/**
	 * The manager responsible for handling bounty logic and state.
	 */
	private final BountyManager bountyManager;
	
	private final RDQImpl rdq;
	
	/**
	 * Constructs a new {@code BountyListener} with the specified plugin instance.
	 *
	 * @param rdq the main plugin instance providing access to the bounty manager
	 */
	public BountyListener(
		final @NotNull RDQ rdq
	) {
		this.rdq = rdq.getImpl();
		this.bountyManager = new BountyManager(this.rdq);
	}
	
	/**
	 * Handles {@link EntityDamageByEntityEvent} to track damage dealt to players with active bounties.
	 * <p>
	 * If both the entity and damager are players, records the damage for bounty tracking.
	 * </p>
	 *
	 * @param event the entity damage event
	 */
	@EventHandler
	public void onDamage(
		EntityDamageByEntityEvent event
	) {
		if (
			!(event.getEntity() instanceof final Player target)
		) {
			return;
		}
		
		this.bountyManager.trackDamage(
			target.getUniqueId(),
			event
				.getDamager()
				.getUniqueId(),
			event.getFinalDamage()
		);
	}
	
	/**
	 * Handles {@link PlayerDeathEvent} to process bounty claims when a player with an active bounty dies.
	 *
	 * @param event the player death event
	 */
	@EventHandler
	public void onDeath(
		PlayerDeathEvent event
	) {
		this.bountyManager.handleBountyKill(event.getEntity());
	}
	
	/**
	 * Handles {@link PlayerJoinEvent} to update the player's display if they have an active bounty.
	 *
	 * @param event the player join event
	 */
	@EventHandler
	public void onJoin(
		PlayerJoinEvent event
	) {
		final Player player     = event.getPlayer();
		final UUID   playerUUID = player.getUniqueId();
		
		RDQPlayer rdqPlayer = this.rdq.getPlayerRepository().findByAttributes(Map.of("uniqueId", playerUUID));
		if (
			rdqPlayer != null
		) {
			RPlayerRank playerRank = this.rdq.getPlayerRankRepository().findByAttributes(Map.of("player", rdqPlayer));
			if (
				playerRank == null &&
				this.rdq.getRankSystemFactory().getDefaultRank() != null
			) {
				final RRank  defaultRank = this.rdq.getRankRepository().findByAttributes(Map.of("identifier", this.rdq.getRankSystemFactory().getDefaultRank().getIdentifier()));
				if (defaultRank != null) {
					RPlayerRank defaultPlayerRank = new RPlayerRank(rdqPlayer, defaultRank);
					rdqPlayer.addPlayerRank(defaultPlayerRank);
					this.rdq.getPlayerRankRepository().create(defaultPlayerRank);
				}
			}
			
		}
		
		this.bountyManager.updateBountyPlayerDisplay(playerUUID);
	}
}