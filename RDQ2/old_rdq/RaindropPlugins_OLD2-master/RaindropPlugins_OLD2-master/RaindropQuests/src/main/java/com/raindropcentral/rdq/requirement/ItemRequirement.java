package com.raindropcentral.rdq.requirement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.evaluable.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.IntStream;

/**
 * Enhanced requirement that checks if a player possesses specific items in their inventory.
 * <p>
 * The {@code ItemRequirement} is satisfied when the player has all the specified items
 * in the required quantities. When consumed, the required items are removed from the
 * player's inventory. This requirement supports both ItemStack objects and ItemBuilder
 * configurations for maximum flexibility.
 * </p>
 *
 * <ul>
 *   <li>Supports multiple item types and quantities with metadata, enchantments, and custom names.</li>
 *   <li>Progress is calculated as the ratio of items collected to items required.</li>
 *   <li>Consumption removes the specified items from the player's inventory.</li>
 *   <li>Supports optional consumption (checking without consuming).</li>
 *   <li>Integrates with RequirementSection for flexible configuration.</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.1.0
 * @since TBD
 */
public class ItemRequirement extends AbstractRequirement {
	
	private static final Logger LOGGER = Logger.getLogger(ItemRequirement.class.getName());
	
	/**
	 * The list of required {@link ItemStack}s.
	 * <p>
	 * Each {@code ItemStack} specifies an item type and the amount needed.
	 * The player must possess at least the specified amount of each item.
	 * </p>
	 */
	@JsonProperty("requiredItems")
	private final List<ItemStack> requiredItems;
	
	/**
	 * List of ItemBuilder configurations for JSON serialization compatibility.
	 * This is used when the requirement is created from configuration.
	 */
	@JsonProperty("itemBuilders")
	private final List<ItemBuilder> itemBuilders;
	
	/**
	 * Whether this requirement should consume items when completed.
	 */
	@JsonProperty("consumeOnComplete")
	private final boolean consumeOnComplete;
	
	/**
	 * Optional description for this item requirement.
	 */
	@JsonProperty("description")
	private final String description;
	
	/**
	 * Whether to match items exactly (including metadata) or just by type and amount.
	 */
	@JsonProperty("exactMatch")
	private final boolean exactMatch;
	
	/**
	 * Default constructor for Jackson deserialization.
	 * <p>
	 * Required by Jackson to deserialize JSON into this class.
	 * Initializes the requirement as an item type with an empty required items list.
	 * </p>
	 */
	protected ItemRequirement() {
		
		super(Type.ITEM);
		this.requiredItems = new ArrayList<>();
		this.itemBuilders = new ArrayList<>();
		this.consumeOnComplete = true;
		this.description = null;
		this.exactMatch = true;
	}
	
	/**
	 * Constructs an {@code ItemRequirement} with full configuration options.
	 *
	 * @param requiredItems     A list of required ItemStack objects.
	 * @param itemBuilders      A list of ItemBuilder configurations (can be null).
	 * @param consumeOnComplete Whether to consume items when the requirement is met.
	 * @param description       Optional description for this requirement.
	 * @param exactMatch        Whether to match items exactly or just by type.
	 */
	@JsonCreator
	public ItemRequirement(
		@JsonProperty("requiredItems") @NotNull final List<ItemStack> requiredItems,
		@JsonProperty("itemBuilders") @NotNull final List<ItemBuilder> itemBuilders,
		@JsonProperty("consumeOnComplete") @NotNull final Boolean consumeOnComplete,
		@JsonProperty("description") @NotNull final String description,
		@JsonProperty("exactMatch") @NotNull final Boolean exactMatch
	) {
		
		super(Type.ITEM);
		
		// Handle different construction scenarios
		if (!requiredItems.isEmpty()) {
			this.requiredItems = new ArrayList<>(requiredItems);
			this.itemBuilders = new ArrayList<>(itemBuilders);
		} else if (!itemBuilders.isEmpty()) {
			this.itemBuilders = new ArrayList<>(itemBuilders);
			this.requiredItems = itemBuilders.stream().map(ItemBuilder::build).collect(
				ArrayList::new,
				ArrayList::add,
				ArrayList::addAll
			);
		} else {
			throw new IllegalArgumentException("At least one required item or item builder must be specified.");
		}
		
		this.consumeOnComplete = consumeOnComplete;
		this.description = description;
		this.exactMatch = exactMatch;
		
		// Validate items
		if (this.requiredItems.isEmpty()) {
			throw new IllegalArgumentException("At least one required item must be specified.");
		}
		
		for (final ItemStack item : this.requiredItems) {
			if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
				throw new IllegalArgumentException("Invalid item in requirements: " + item);
			}
		}
		
		// Debug logging
		LOGGER.info("Created ItemRequirement with " + this.requiredItems.size() + " items:");
		for (int i = 0; i < this.requiredItems.size(); i++) {
			ItemStack item = this.requiredItems.get(i);
			LOGGER.info("  Item " + i + ": " + item.getType() + " x" + item.getAmount() +
			            " (exactMatch: " + this.exactMatch + ")");
		}
	}
	
	/**
	 * Checks if the player has each of the required items in the necessary amounts.
	 * <p>
	 * This method verifies that for every required item, the player's inventory contains
	 * at least the needed amount, considering the exactMatch setting.
	 * </p>
	 *
	 * @param player The player whose inventory will be checked.
	 *
	 * @return {@code true} if for every required item the player's inventory contains at least the needed amount; {@code false} otherwise.
	 */
	@Override
	public boolean isMet(
		@NotNull final Player player
	) {
		
		LOGGER.info("Checking ItemRequirement for player: " + player.getName());
		LOGGER.info("Required items count: " + this.requiredItems.size());
		
		boolean allMet = true;
		for (int i = 0; i < this.requiredItems.size(); i++) {
			ItemStack requiredItem = this.requiredItems.get(i);
			boolean hasEnough = this.hasEnoughItems(player, requiredItem);
			
			LOGGER.info("Item " + i + " (" + requiredItem.getType() + " x" + requiredItem.getAmount() + "): " +
			            (hasEnough ? "✓ HAS ENOUGH" : "✗ NOT ENOUGH"));
			
			if (!hasEnough) {
				int currentAmount = this.countItems(player, requiredItem);
				LOGGER.info("  Player has: " + currentAmount + ", needs: " + requiredItem.getAmount());
				allMet = false;
			}
		}
		
		LOGGER.info("Overall requirement met: " + allMet);
		return allMet;
	}
	
	/**
	 * Calculates the progress towards fulfilling the item requirement.
	 * <p>
	 * Progress is determined as the ratio between the total number of items the player holds
	 * (up to the required amount for each item) and the overall number required across all items.
	 * If no items are required, progress is considered complete ({@code 1.0}).
	 * </p>
	 *
	 * @param player The player whose inventory will be evaluated.
	 *
	 * @return A double between 0.0 and 1.0 representing progress.
	 */
	@Override
	public double calculateProgress(
		@NotNull final Player player
	) {
		
		if (this.requiredItems.isEmpty()) {
			return 1.0;
		}
		
		double totalCollected = 0.0;
		double totalRequired  = 0.0;
		
		for (final ItemStack requiredItem : this.requiredItems) {
			final int actualAmount   = this.countItems(
				player,
				requiredItem
			);
			final int requiredAmount = requiredItem.getAmount();
			
			totalCollected += Math.min(
				actualAmount,
				requiredAmount
			);
			totalRequired += requiredAmount;
		}
		
		double progress = totalRequired > 0 ? Math.min(1.0, totalCollected / totalRequired) : 1.0;
		
		LOGGER.info("Progress calculation for " + player.getName() + ": " +
		            totalCollected + "/" + totalRequired + " = " + (progress * 100) + "%");
		
		return progress;
	}
	
	/**
	 * Consumes the required items from the player's inventory if consumption is enabled.
	 * <p>
	 * For each required item, this method deducts the specified amount from the player's inventory,
	 * removing items from stacks as needed until the requirement is fulfilled.
	 * </p>
	 *
	 * @param player The player from whose inventory the items will be consumed.
	 */
	@Override
	public void consume(
		@NotNull final Player player
	) {
		
		if (! this.consumeOnComplete) {
			LOGGER.info("Consumption disabled for ItemRequirement");
			return; // Consumption disabled
		}
		
		LOGGER.info("Consuming items for player: " + player.getName());
		for (final ItemStack requiredItem : this.requiredItems) {
			LOGGER.info("Removing " + requiredItem.getAmount() + "x " + requiredItem.getType());
			this.removeItems(
				player,
				requiredItem
			);
		}
	}
	
	/**
	 * Returns the translation key for this requirement's description.
	 * <p>
	 * This key can be used for localization and user-facing descriptions.
	 * </p>
	 *
	 * @return The language key for this requirement's description, typically {@code "requirement.item"}.
	 */
	@Override
	@NotNull
	public String getDescriptionKey() {
		
		return "requirement.item";
	}
	
	/**
	 * Returns a defensive copy of the required items list.
	 * <p>
	 * This prevents external modification of the internal {@code requiredItems} list.
	 * </p>
	 *
	 * @return A new {@link List} containing copies of the required {@link ItemStack}s.
	 */
	@NotNull
	public List<ItemStack> getRequiredItems() {
		
		return this.requiredItems.stream()
		                         .map(ItemStack::clone)
		                         .collect(
			                         ArrayList::new,
			                         ArrayList::add,
			                         ArrayList::addAll
		                         );
	}
	
	/**
	 * Gets the ItemBuilder configurations used to create this requirement.
	 *
	 * @return A defensive copy of the ItemBuilder list.
	 */
	@NotNull
	public List<ItemBuilder> getItemBuilders() {
		
		return new ArrayList<>(this.itemBuilders);
	}
	
	/**
	 * Gets whether this requirement consumes items when completed.
	 *
	 * @return True if items are consumed, false otherwise.
	 */
	public boolean isConsumeOnComplete() {
		
		return this.consumeOnComplete;
	}
	
	/**
	 * Gets the optional description for this item requirement.
	 *
	 * @return The description, or null if not provided.
	 */
	@Nullable
	public String getDescription() {
		
		return this.description;
	}
	
	/**
	 * Gets whether this requirement uses exact matching.
	 *
	 * @return True if exact matching is used, false for type-only matching.
	 */
	public boolean isExactMatch() {
		
		return this.exactMatch;
	}
	
	/**
	 * Gets detailed progress information for each required item for the specified player.
	 *
	 * @param player The player whose progress will be calculated.
	 *
	 * @return A list of {@link ItemProgress} objects containing detailed progress information.
	 */
	@JsonIgnore
	@NotNull
	public List<ItemProgress> getDetailedProgress(
		@NotNull final Player player
	) {
		
		return IntStream.range(
			                0,
			                this.requiredItems.size()
		                )
		                .mapToObj(index -> {
			                final ItemStack requiredItem   = this.requiredItems.get(index);
			                final int       currentAmount  = this.countItems(
				                player,
				                requiredItem
			                );
			                final int       requiredAmount = requiredItem.getAmount();
			                final double    progress       = requiredAmount > 0 ?
			                                                 Math.min(
				                                                 1.0,
				                                                 (double) currentAmount / requiredAmount
			                                                 ) :
			                                                 1.0;
			                final boolean   completed      = currentAmount >= requiredAmount;
			                return new ItemProgress(
				                index,
				                requiredItem,
				                requiredAmount,
				                currentAmount,
				                progress,
				                completed
			                );
		                })
		                .toList();
	}
	
	/**
	 * Gets the items that are currently missing for the specified player.
	 *
	 * @param player The player whose missing items will be calculated.
	 *
	 * @return A list of ItemStack objects representing missing items and amounts.
	 */
	@JsonIgnore
	@NotNull
	public List<ItemStack> getMissingItems(
		@NotNull final Player player
	) {
		
		final List<ItemStack> missing = new ArrayList<>();
		
		for (final ItemStack requiredItem : this.requiredItems) {
			final int currentAmount = this.countItems(
				player,
				requiredItem
			);
			final int shortage      = Math.max(
				0,
				requiredItem.getAmount() - currentAmount
			);
			
			if (shortage > 0) {
				final ItemStack missingItem = requiredItem.clone();
				missingItem.setAmount(shortage);
				missing.add(missingItem);
			}
		}
		
		return missing;
	}
	
	/**
	 * Validates the internal state of this item requirement.
	 *
	 * @throws IllegalStateException If the requirement is in an invalid state.
	 */
	@JsonIgnore
	public void validate() {
		
		if (this.requiredItems.isEmpty()) {
			throw new IllegalStateException("ItemRequirement must have at least one required item.");
		}
		
		for (int i = 0; i < this.requiredItems.size(); i++) {
			final ItemStack item = this.requiredItems.get(i);
			if (item == null) {
				throw new IllegalStateException("Required item at index " + i + " is null.");
			}
			if (item.getType().isAir()) {
				throw new IllegalStateException("Required item at index " + i + " is air.");
			}
			if (item.getAmount() <= 0) {
				throw new IllegalStateException("Required item at index " + i + " has invalid amount: " + item.getAmount());
			}
		}
	}
	
	/**
	 * Debug method to print player's inventory contents.
	 */
	@JsonIgnore
	public void debugPlayerInventory(@NotNull final Player player) {
		LOGGER.info("=== Player Inventory Debug for " + player.getName() + " ===");
		ItemStack[] contents = player.getInventory().getContents();
		for (int i = 0; i < contents.length; i++) {
			ItemStack item = contents[i];
			if (item != null && !item.getType().isAir()) {
				LOGGER.info("Slot " + i + ": " + item.getType() + " x" + item.getAmount() +
				            (item.hasItemMeta() ? " (has meta)" : " (no meta)"));
			}
		}
		LOGGER.info("=== End Inventory Debug ===");
	}
	
	/**
	 * Checks if the player has enough of a specific item.
	 *
	 * @param player       The player to check.
	 * @param requiredItem The required item.
	 *
	 * @return True if the player has enough, false otherwise.
	 */
	private boolean hasEnoughItems(
		@NotNull final Player player,
		@NotNull final ItemStack requiredItem
	) {
		
		LOGGER.info("Checking if player has enough: " + requiredItem.getType() + " x" + requiredItem.getAmount() +
		            " (exactMatch: " + this.exactMatch + ")");
		
		// Debug player inventory
		debugPlayerInventory(player);
		
		if (this.exactMatch) {
			boolean result = player.getInventory().containsAtLeast(requiredItem, requiredItem.getAmount());
			LOGGER.info("Exact match result: " + result);
			return result;
		} else {
			// Count by type only
			final int totalAmount = player.getInventory().all(requiredItem.getType()).values().stream()
			                              .mapToInt(ItemStack::getAmount)
			                              .sum();
			boolean result = totalAmount >= requiredItem.getAmount();
			LOGGER.info("Type-only match - Player has " + totalAmount + ", needs " + requiredItem.getAmount() + ", result: " + result);
			return result;
		}
	}
	
	/**
	 * Counts how many of a specific item the player has.
	 *
	 * @param player       The player to check.
	 * @param requiredItem The item to count.
	 *
	 * @return The total amount the player has.
	 */
	private int countItems(
		@NotNull final Player player,
		@NotNull final ItemStack requiredItem
	) {
		
		if (this.exactMatch) {
			return player.getInventory().all(requiredItem).values().stream()
			             .filter(stack -> stack.isSimilar(requiredItem))
			             .mapToInt(ItemStack::getAmount)
			             .sum();
		} else {
			return player.getInventory().all(requiredItem.getType()).values().stream()
			             .mapToInt(ItemStack::getAmount)
			             .sum();
		}
	}
	
	/**
	 * Removes items from the player's inventory.
	 *
	 * @param player       The player from whose inventory items will be removed.
	 * @param requiredItem The item to remove.
	 */
	private void removeItems(
		@NotNull final Player player,
		@NotNull final ItemStack requiredItem
	) {
		
		int               remaining = requiredItem.getAmount();
		final ItemStack[] contents  = player.getInventory().getContents();
		
		for (int i = 0; i < contents.length && remaining > 0; i++) {
			final ItemStack stack = contents[i];
			if (stack == null)
				continue;
			
			final boolean matches = this.exactMatch ?
			                        stack.isSimilar(requiredItem) :
			                        stack.getType() == requiredItem.getType();
			if (matches) {
				final int remove = Math.min(
					remaining,
					stack.getAmount()
				);
				stack.setAmount(stack.getAmount() - remove);
				remaining -= remove;
				
				// Remove empty stacks
				if (stack.getAmount() <= 0) {
					contents[i] = null;
				}
			}
		}
		
		player.getInventory().setContents(contents);
	}
	
	/**
	 * Represents detailed progress information for a single item within an ItemRequirement.
	 */
	public record ItemProgress(
		int index,
		ItemStack requiredItem,
		int requiredAmount,
		int currentAmount,
		double progress,
		boolean completed
	) {
		
		/**
		 * Constructs a new ItemProgress instance.
		 *
		 * @param index          The index of the item in the requirements list.
		 * @param requiredItem   The required item.
		 * @param requiredAmount The required amount.
		 * @param currentAmount  The current amount the player has.
		 * @param progress       The progress value (0.0 to 1.0).
		 * @param completed      Whether the item requirement is completed.
		 */
		public ItemProgress(
			final int index,
			@NotNull final ItemStack requiredItem,
			final int requiredAmount,
			final int currentAmount,
			final double progress,
			final boolean completed
		) {
			
			this.index = index;
			this.requiredItem = requiredItem.clone();
			this.requiredAmount = requiredAmount;
			this.currentAmount = currentAmount;
			this.progress = progress;
			this.completed = completed;
		}
		
		/**
		 * Gets the index of this item in the requirements list.
		 *
		 * @return The item index.
		 */
		@Override
		public int index() {
			
			return this.index;
		}
		
		/**
		 * Gets the required item.
		 *
		 * @return A clone of the required item.
		 */
		@Override
		@NotNull
		public ItemStack requiredItem() {
			
			return this.requiredItem.clone();
		}
		
		/**
		 * Gets the required amount.
		 *
		 * @return The required amount.
		 */
		@Override
		public int requiredAmount() {
			
			return this.requiredAmount;
		}
		
		/**
		 * Gets the current amount the player has.
		 *
		 * @return The current amount.
		 */
		@Override
		public int currentAmount() {
			
			return this.currentAmount;
		}
		
		/**
		 * Gets the progress value for this item.
		 *
		 * @return The progress value (0.0 to 1.0).
		 *
		 */
		@Override
		public double progress() {
			
			return this.progress;
		}
		
		/**
		 * Gets whether this item requirement is completed.
		 *
		 * @return True if completed, false otherwise.
		 */
		@Override
		public boolean completed() {
			
			return this.completed;
		}
		
		/**
		 * Gets the progress as a percentage.
		 *
		 * @return The progress percentage (0 to 100).
		 */
		public int getProgressPercentage() {
			
			return (int) (this.progress * 100);
		}
		
		/**
		 * Gets the shortage amount (how much more is needed).
		 *
		 * @return The shortage amount, or 0 if requirement is met.
		 */
		public int getShortage() {
			
			return Math.max(
				0,
				this.requiredAmount - this.currentAmount
			);
		}
		
	}
	
}