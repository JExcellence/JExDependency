package com.raindropcentral.rdq.reward;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a reward that gives a specific item to a player.
 * <p>
 * When this reward is applied, the specified {@link Material} and amount are added
 * to the player's inventory. This is commonly used for quest or achievement rewards
 * that grant items such as tools, blocks, or consumables.
 * </p>
 *
 * <p>
 * Example usage:
 * <pre>
 *     ItemReward reward = new ItemReward(Material.DIAMOND, 3);
 *     reward.apply(player);
 * </pre>
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class ItemReward extends AbstractReward {
	
	/**
	 * The type of item (material) to be given as a reward.
	 */
	private final Material material;
	
	/**
	 * The number of items to give to the player.
	 */
	private final int amount;
	
	/**
	 * Constructs a new {@code ItemReward} with the specified material and amount.
	 *
	 * @param material The {@link Material} to reward the player.
	 * @param amount   The number of items to give.
	 */
	public ItemReward(
		@NotNull Material material,
		int amount
	) {
		super(Type.ITEM);
		this.material = material;
		this.amount = amount;
	}
	
	/**
	 * Applies the item reward to the specified player.
	 * <p>
	 * Adds the configured material and amount to the player's inventory.
	 * </p>
	 *
	 * @param player The player to receive the item reward.
	 */
	@Override
	public void apply(@NotNull Player player) {
		player.getInventory().addItem(new ItemStack(
			material,
			amount
		));
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
		return "reward.item";
	}
	
	/**
	 * Gets the material (item type) to be rewarded.
	 *
	 * @return The {@link Material} to be given to the player.
	 */
	public Material getMaterial() {
		return material;
	}
	
	/**
	 * Gets the amount of items to be rewarded.
	 *
	 * @return The number of items to give.
	 */
	public int getAmount() {
		return amount;
	}
}