package com.raindropcentral.rdq.reward;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a reward that grants experience levels to a player.
 * <p>
 * When this reward is applied, the specified number of experience levels are added
 * to the player's current total. This is useful for incentivizing players with progression
 * rewards in quests or achievements.
 * </p>
 *
 * <p>
 * Example usage:
 * <pre>
 *     ExperienceReward reward = new ExperienceReward(5);
 *     reward.apply(player);
 * </pre>
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class ExperienceReward extends AbstractReward {
	
	/**
	 * The number of experience levels to grant to the player.
	 */
	private final int levels;
	
	/**
	 * Constructs a new {@code ExperienceReward} with the specified number of levels.
	 *
	 * @param levels The number of experience levels to reward the player.
	 */
	public ExperienceReward(int levels) {
		super(Type.EXPERIENCE);
		this.levels = levels;
	}
	
	/**
	 * Applies the experience reward to the specified player.
	 * <p>
	 * Adds the configured number of experience levels to the player's current total.
	 * </p>
	 *
	 * @param player The player to receive the experience reward.
	 */
	@Override
	public void apply(@NotNull Player player) {
		player.giveExpLevels(levels);
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
		return "reward.experience";
	}
	
	/**
	 * Gets the number of experience levels to be rewarded.
	 *
	 * @return The experience level amount.
	 */
	public int getLevels() {
		return levels;
	}
}