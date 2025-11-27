package com.raindropcentral.rdq.reward;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class for all reward types in the RaindropQuests system.
 * <p>
 * This class defines the contract for all rewards, including type identification,
 * application logic, and description key for localization.
 * Concrete implementations should extend this class to provide specific reward behaviors,
 * such as executing commands, giving items, or awarding currency.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public abstract class AbstractReward {
	
	/**
	 * Enumerates the various types of rewards supported by the system.
	 * <ul>
	 *   <li>{@link #COMMAND} - Executes a server command as a reward.</li>
	 *   <li>{@link #ITEM} - Gives an item to the player.</li>
	 *   <li>{@link #CURRENCY} - Awards in-game currency.</li>
	 *   <li>{@link #EXPERIENCE} - Grants experience points or levels.</li>
	 *   <li>{@link #COMPOSITE} - Composite reward combining multiple sub-rewards.</li>
	 * </ul>
	 */
	public enum Type {
		COMMAND,
		ITEM,
		CURRENCY,
		EXPERIENCE,
		COMPOSITE
	}
	
	/**
	 * The type of this reward.
	 */
	protected final Type type;
	
	/**
	 * Constructs a new {@code AbstractReward} with the specified type.
	 *
	 * @param type The reward type that identifies the concrete implementation.
	 */
	protected AbstractReward(@NotNull final Type type) {
		
		this.type = type;
	}
	
	/**
	 * Returns the type of this reward.
	 *
	 * @return The reward {@link Type}.
	 */
	@NotNull
	public Type getType() {
		
		return this.type;
	}
	
	/**
	 * Applies the reward to the specified player.
	 * <p>
	 * Implementations should define the logic for granting the reward,
	 * such as executing a command, giving an item, or awarding currency.
	 * </p>
	 *
	 * @param player The player to receive the reward.
	 */
	public abstract void apply(@NotNull Player player);
	
	/**
	 * Gets the translation key for the reward's description.
	 * <p>
	 * This key is used to retrieve localized descriptions of the reward
	 * from the plugin's language or resource files, enabling internationalization.
	 * </p>
	 *
	 * @return The language key for this reward's description.
	 */
	@JsonIgnore
	@NotNull
	public abstract String getDescriptionKey();
	
}