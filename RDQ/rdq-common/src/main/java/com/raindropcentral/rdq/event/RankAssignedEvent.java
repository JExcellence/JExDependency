package com.raindropcentral.rdq.event;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player is assigned a new rank.
 * This event is called after the rank has been successfully assigned in the database.
 */
public class RankAssignedEvent extends Event {
	
	private static final HandlerList HANDLERS = new HandlerList();
	
	private final Player player;
	private final RDQPlayer rdqPlayer;
	private final RRank rank;
	
	/**
	 * Executes RankAssignedEvent.
	 */
	public RankAssignedEvent(
		final @NotNull Player player,
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRank rank
	) {
		this.player = player;
		this.rdqPlayer = rdqPlayer;
		this.rank = rank;
	}
	
	/**
	 * Gets player.
	 */
	public @NotNull Player getPlayer() {
		return this.player;
	}
	
	/**
	 * Gets rDQPlayer.
	 */
	public @NotNull RDQPlayer getRDQPlayer() {
		return this.rdqPlayer;
	}
	
	/**
	 * Gets rank.
	 */
	public @NotNull RRank getRank() {
		return this.rank;
	}
	
	/**
	 * Gets handlers.
	 */
	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLERS;
	}
	
	/**
	 * Gets handlerList.
	 */
	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
