package com.raindropcentral.rdq.reward;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a composite reward that aggregates multiple sub-rewards.
 * <p>
 * When this reward is applied, it sequentially applies each contained {@link AbstractReward}
 * to the specified player. This allows for complex reward structures by combining
 * different reward types (e.g., items, commands, currency) into a single reward.
 * </p>
 *
 * <p>
 * Example usage:
 * <pre>
 *     List&lt;AbstractReward&gt; subRewards = Arrays.asList(
 *         new CommandReward("give %player% diamond 1"),
 *         new CurrencyReward(100)
 *     );
 *     CompositeReward composite = new CompositeReward(subRewards);
 *     composite.apply(player);
 * </pre>
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class CompositeReward extends AbstractReward {
	
	/**
	 * The list of sub-rewards that make up this composite reward.
	 */
	private final List<AbstractReward> rewards;
	
	/**
	 * Constructs a new {@code CompositeReward} with the specified list of sub-rewards.
	 *
	 * @param rewards The list of {@link AbstractReward} instances to be applied as part of this composite reward.
	 */
	public CompositeReward(@NotNull List<AbstractReward> rewards) {
		super(Type.COMPOSITE);
		this.rewards = rewards;
	}
	
	/**
	 * Applies all sub-rewards in this composite reward to the specified player.
	 * <p>
	 * Each reward in the list is applied in order.
	 * </p>
	 *
	 * @param player The player to receive all sub-rewards.
	 */
	@Override
	public void apply(@NotNull Player player) {
		for (AbstractReward reward : rewards) {
			reward.apply(player);
		}
	}
	
	/**
	 * Returns the translation key for this composite reward's description.
	 * <p>
	 * Used for localization and display in user interfaces.
	 * </p>
	 *
	 * @return The language key for this reward's description.
	 */
	@Override
	@NotNull
	public String getDescriptionKey() {
		return "reward.composite";
	}
	
	/**
	 * Gets the list of sub-rewards contained in this composite reward.
	 *
	 * @return An immutable list of {@link AbstractReward} instances.
	 */
	public List<AbstractReward> getRewards() {
		return rewards;
	}
}