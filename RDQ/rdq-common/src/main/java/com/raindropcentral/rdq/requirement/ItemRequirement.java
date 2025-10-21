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
 * player's inventory.
 * </p>
 *
 * @author JExcellence
 * @version 1.1.0
 * @since 1.0.0
 */
public final class ItemRequirement extends AbstractRequirement {

    private static final Logger LOGGER = Logger.getLogger(ItemRequirement.class.getName());

    @JsonProperty("requiredItems")
    private final List<ItemStack> requiredItems;

    @JsonProperty("itemBuilders")
    private final List<ItemBuilder> itemBuilders;

    @JsonProperty("consumeOnComplete")
    private final boolean consumeOnComplete;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("exactMatch")
    private final boolean exactMatch;

    /**
     * Creates an empty requirement that defaults to exact matching and consumption on completion.
     * This constructor is primarily intended for serialization frameworks.
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
     * Creates a requirement that compares the supplied items against a player's inventory.
     *
     * @param requiredItems   the concrete items that must be present, or {@code null} when builders should be used
     * @param itemBuilders    item builders that generate the required items when {@code requiredItems} is empty
     * @param consumeOnComplete {@code true} to consume items on completion, {@code false} to leave the inventory untouched
     * @param description     optional localized description key for the requirement
     * @param exactMatch      {@code true} to require exact matches including metadata, {@code false} to match by material type
     * @throws IllegalArgumentException if no items are supplied or an invalid item definition is detected
     */
    @JsonCreator
    public ItemRequirement(
            @JsonProperty("requiredItems") final @Nullable List<ItemStack> requiredItems,
            @JsonProperty("itemBuilders") final @Nullable List<ItemBuilder> itemBuilders,
            @JsonProperty("consumeOnComplete") final @Nullable Boolean consumeOnComplete,
            @JsonProperty("description") final @Nullable String description,
            @JsonProperty("exactMatch") final @Nullable Boolean exactMatch
    ) {
        super(Type.ITEM);

        final List<ItemStack> items = requiredItems != null ? requiredItems : new ArrayList<>();
        final List<ItemBuilder> builders = itemBuilders != null ? itemBuilders : new ArrayList<>();

        if (!items.isEmpty()) {
            this.requiredItems = new ArrayList<>(items);
            this.itemBuilders = new ArrayList<>(builders);
        } else if (!builders.isEmpty()) {
            this.itemBuilders = new ArrayList<>(builders);
            this.requiredItems = builders.stream()
                    .map(ItemBuilder::build)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        } else {
            throw new IllegalArgumentException("At least one required item or item builder must be specified.");
        }

        this.consumeOnComplete = consumeOnComplete != null ? consumeOnComplete : true;
        this.description = description;
        this.exactMatch = exactMatch != null ? exactMatch : true;

        if (this.requiredItems.isEmpty()) {
            throw new IllegalArgumentException("At least one required item must be specified.");
        }

        for (final ItemStack item : this.requiredItems) {
            if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
                throw new IllegalArgumentException("Invalid item in requirements: " + item);
            }
        }
    }

    /**
     * Determines whether the player currently satisfies the requirement.
     *
     * @param player the player whose inventory is evaluated
     * @return {@code true} when all required items are present in sufficient quantities
     */
    @Override
    public boolean isMet(final @NotNull Player player) {
        for (final ItemStack requiredItem : this.requiredItems) {
            if (!this.hasEnoughItems(player, requiredItem)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates the aggregate completion progress for the player based on owned item quantities.
     *
     * @param player the player whose progress is calculated
     * @return a value between {@code 0.0} and {@code 1.0} representing completion progress
     */
    @Override
    public double calculateProgress(final @NotNull Player player) {
        if (this.requiredItems.isEmpty()) {
            return 1.0;
        }

        double totalCollected = 0.0;
        double totalRequired = 0.0;

        for (final ItemStack requiredItem : this.requiredItems) {
            final int actualAmount = this.countItems(player, requiredItem);
            final int requiredAmount = requiredItem.getAmount();
            totalCollected += Math.min(actualAmount, requiredAmount);
            totalRequired += requiredAmount;
        }

        return totalRequired > 0 ? Math.min(1.0, totalCollected / totalRequired) : 1.0;
    }

    /**
     * Consumes the required items from the player's inventory when the requirement is configured to do so.
     *
     * @param player the player whose inventory will be modified
     */
    @Override
    public void consume(final @NotNull Player player) {
        if (!this.consumeOnComplete) {
            return;
        }

        for (final ItemStack requiredItem : this.requiredItems) {
            this.removeItems(player, requiredItem);
        }
    }

    /**
     * Supplies the translation key used to describe this requirement within the UI layer.
     *
     * @return the localization key representing the item requirement
     */
    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.item";
    }

    /**
     * Provides defensive copies of the required items for external inspection.
     *
     * @return a new list containing clones of the required item stacks
     */
    @NotNull
    public List<ItemStack> getRequiredItems() {
        return this.requiredItems.stream()
                .map(ItemStack::clone)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Exposes the configured item builders backing this requirement.
     *
     * @return a mutable copy of the configured builders
     */
    @NotNull
    public List<ItemBuilder> getItemBuilders() {
        return new ArrayList<>(this.itemBuilders);
    }

    /**
     * Indicates whether required items are consumed when the requirement is fulfilled.
     *
     * @return {@code true} when items are removed on completion
     */
    public boolean isConsumeOnComplete() {
        return this.consumeOnComplete;
    }

    /**
     * Retrieves the optional description key for translation lookups.
     *
     * @return the description key, or {@code null} when none is defined
     */
    @Nullable
    public String getDescription() {
        return this.description;
    }

    /**
     * States whether the requirement demands exact metadata matches for inventory comparison.
     *
     * @return {@code true} when exact matching is enforced
     */
    public boolean isExactMatch() {
        return this.exactMatch;
    }

    /**
     * Creates detailed progress entries describing the fulfillment state of each required item.
     *
     * @param player the player whose progress is requested
     * @return immutable progress snapshots for every tracked item definition
     */
    @JsonIgnore
    @NotNull
    public List<ItemProgress> getDetailedProgress(final @NotNull Player player) {
        return IntStream.range(0, this.requiredItems.size())
                .mapToObj(index -> {
                    final ItemStack requiredItem = this.requiredItems.get(index);
                    final int currentAmount = this.countItems(player, requiredItem);
                    final int requiredAmount = requiredItem.getAmount();
                    final double progress = requiredAmount > 0
                            ? Math.min(1.0, (double) currentAmount / requiredAmount)
                            : 1.0;
                    final boolean completed = currentAmount >= requiredAmount;
                    return new ItemProgress(index, requiredItem, requiredAmount, currentAmount, progress, completed);
                })
                .toList();
    }

    /**
     * Computes the missing items a player still needs to collect to satisfy the requirement.
     *
     * @param player the player whose inventory is inspected
     * @return a list of item stacks representing outstanding requirements
     */
    @JsonIgnore
    @NotNull
    public List<ItemStack> getMissingItems(final @NotNull Player player) {
        final List<ItemStack> missing = new ArrayList<>();
        for (final ItemStack requiredItem : this.requiredItems) {
            final int currentAmount = this.countItems(player, requiredItem);
            final int shortage = Math.max(0, requiredItem.getAmount() - currentAmount);
            if (shortage > 0) {
                final ItemStack missingItem = requiredItem.clone();
                missingItem.setAmount(shortage);
                missing.add(missingItem);
            }
        }
        return missing;
    }

    /**
     * Ensures the requirement is internally consistent and ready for evaluation.
     *
     * @throws IllegalStateException if invalid or empty item definitions are discovered
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
     * Checks whether the player has the required quantity of the supplied item.
     *
     * @param player        the player whose inventory is queried
     * @param requiredItem  the item definition to match
     * @return {@code true} if the player has sufficient matching items
     */
    private boolean hasEnoughItems(final @NotNull Player player, final @NotNull ItemStack requiredItem) {
        if (this.exactMatch) {
            return player.getInventory().containsAtLeast(requiredItem, requiredItem.getAmount());
        } else {
            final int totalAmount = player.getInventory().all(requiredItem.getType()).values().stream()
                    .mapToInt(ItemStack::getAmount)
                    .sum();
            return totalAmount >= requiredItem.getAmount();
        }
    }

    /**
     * Counts the number of matching items in a player's inventory, respecting the matching mode.
     *
     * @param player        the player whose inventory is inspected
     * @param requiredItem  the item definition to match
     * @return the number of items that meet the criteria
     */
    private int countItems(final @NotNull Player player, final @NotNull ItemStack requiredItem) {
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
     * Removes the supplied item from the player's inventory up to the required amount.
     *
     * @param player        the player whose inventory will be mutated
     * @param requiredItem  the item definition to deduct
     */
    private void removeItems(final @NotNull Player player, final @NotNull ItemStack requiredItem) {
        int remaining = requiredItem.getAmount();
        final ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            final ItemStack stack = contents[i];
            if (stack == null) continue;

            final boolean matches = this.exactMatch
                    ? stack.isSimilar(requiredItem)
                    : stack.getType() == requiredItem.getType();

            if (matches) {
                final int remove = Math.min(remaining, stack.getAmount());
                stack.setAmount(stack.getAmount() - remove);
                remaining -= remove;

                if (stack.getAmount() <= 0) {
                    contents[i] = null;
                }
            }
        }

        player.getInventory().setContents(contents);
    }

    /**
     * Detailed snapshot describing the status of a required item for a given player.
     */
    public record ItemProgress(
            int index,
            @NotNull ItemStack requiredItem,
            int requiredAmount,
            int currentAmount,
            double progress,
            boolean completed
    ) {
        /**
         * Creates a new immutable progress snapshot.
         *
         * @param index          positional index within the requirement list
         * @param requiredItem   the item definition being tracked
         * @param requiredAmount the amount needed to fulfill the requirement
         * @param currentAmount  the amount currently owned by the player
         * @param progress       normalized completion from {@code 0.0} to {@code 1.0}
         * @param completed      {@code true} when the requirement for this item is satisfied
         */
        public ItemProgress(
                final int index,
                final @NotNull ItemStack requiredItem,
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
         * Provides a defensive copy of the tracked item stack.
         *
         * @return a clone of the required item definition
         */
        @Override
        @NotNull
        public ItemStack requiredItem() {
            return this.requiredItem.clone();
        }

        /**
         * Converts the normalized progress into an integer percentage.
         *
         * @return the completion percentage rounded down to the nearest integer
         */
        public int getProgressPercentage() {
            return (int) (this.progress * 100);
        }

        /**
         * Computes how many additional items are still needed to meet the requirement.
         *
         * @return the remaining quantity required to satisfy this entry
         */
        public int getShortage() {
            return Math.max(0, this.requiredAmount - this.currentAmount);
        }
    }
}