package com.raindropcentral.rdq.reward;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a reward that executes a server command when granted to a player.
 * <p>
 * The command may include the placeholder <code>%player%</code>, which will be replaced
 * with the target player's name at execution time. The command is executed by the server console.
 * </p>
 *
 * <p>
 * Example usage:
 * <pre>
 *     CommandReward reward = new CommandReward("give %player% diamond 1");
 *     reward.apply(player);
 * </pre>
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class CommandReward extends AbstractReward {
	
	/**
	 * The command to execute as a reward.
	 * May contain the placeholder <code>%player%</code> for player name substitution.
	 */
	private final String command;
	
	/**
	 * Constructs a new {@code CommandReward} with the specified command.
	 *
	 * @param command The command to execute when the reward is applied. May include <code>%player%</code> as a placeholder.
	 */
	public CommandReward(@NotNull String command) {
		super(Type.COMMAND);
		this.command = command;
	}
	
	/**
	 * Applies the command reward to the specified player.
	 * <p>
	 * Replaces <code>%player%</code> in the command with the player's name and executes the command
	 * as the server console.
	 * </p>
	 *
	 * @param target The player to receive the reward.
	 */
	@Override
	public void apply(@NotNull Player target) {
		String cmd = command.replace(
			"%target_name%",
			target.getName()
		);
		Bukkit.dispatchCommand(
			Bukkit.getConsoleSender(),
			cmd
		);
	}
	
	/**
	 * Returns the translation key for this reward's description.
	 * <p>
	 * Used for localization and display in user interfaces.
	 * </p>
	 *
	 * @return The language key for this reward's description.
	 */
	@Override
	@NotNull
	public String getDescriptionKey() {
		return "reward.command";
	}
	
	/**
	 * Gets the raw command string associated with this reward.
	 *
	 * @return The command string, possibly containing <code>%player%</code> as a placeholder.
	 */
	public String getCommand() {
		return this.command;
	}
}