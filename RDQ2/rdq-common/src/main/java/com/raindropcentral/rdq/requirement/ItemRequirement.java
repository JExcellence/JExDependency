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

    protected ItemRequirement() {
        super(Type.ITEM);
        this.requiredItems = new ArrayList<>();
        this.itemBuilders = new ArrayList<>();
        this.consumeOnComplete = true;
        this.description = null;
        this.exactMatch = true;
    }

    @JsonCreator
    public ItemRequirement(@JsonProperty("requiredItems") @Nullable List<ItemStack> requiredItems,
                          @JsonProperty("itemBuilders") @Nullable List<ItemBuilder> itemBuilders,
                          @JsonProperty("consumeOnComplete") @Nullable Boolean consumeOnComplete,
                          @JsonProperty("description") @Nullable String description,
                          @JsonProperty("exactMatch") @Nullable Boolean exactMatch) {
        super(Type.ITEM);

        var items = requiredItems != null ? requiredItems : new ArrayList<ItemStack>();
        var builders = itemBuilders != null ? itemBuilders : new ArrayList<ItemBuilder>();

        if (!items.isEmpty()) {
            this.requiredItems = new ArrayList<>(items);
            this.itemBuilders = new ArrayList<>(builders);
        } else if (!builders.isEmpty()) {
            this.itemBuilders = new ArrayList<>(builders);
            this.requiredItems = builders.stream().map(ItemBuilder::build).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        } else {
            throw new IllegalArgumentException("At least one required item or item builder must be specified.");
        }

        this.consumeOnComplete = consumeOnComplete != null ? consumeOnComplete : true;
        this.description = description;
        this.exactMatch = exactMatch != null ? exactMatch : true;

        if (this.requiredItems.isEmpty()) {
            throw new IllegalArgumentException("At least one required item must be specified.");
        }

        for (var item : this.requiredItems) {
            if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
                throw new IllegalArgumentException("Invalid item in requirements: " + item);
            }
        }
    }

    @Override
    public boolean isMet(@NotNull Player player) {
        return requiredItems.stream().allMatch(item -> hasEnoughItems(player, item));
    }

    @Override
    public double calculateProgress(@NotNull Player player) {
        if (requiredItems.isEmpty()) return 1.0;

        var totalCollected = 0.0;
        var totalRequired = 0.0;

        for (var requiredItem : requiredItems) {
            var actualAmount = countItems(player, requiredItem);
            var requiredAmount = requiredItem.getAmount();
            totalCollected += Math.min(actualAmount, requiredAmount);
            totalRequired += requiredAmount;
        }

        return totalRequired > 0 ? Math.min(1.0, totalCollected / totalRequired) : 1.0;
    }

    @Override
    public void consume(@NotNull Player player) {
        if (!consumeOnComplete) return;
        requiredItems.forEach(item -> removeItems(player, item));
    }

    @Override
    public @NotNull String getDescriptionKey() {
        return "requirement.item";
    }

    public List<ItemStack> getRequiredItems() {
        return requiredItems.stream().map(ItemStack::clone).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public List<ItemBuilder> getItemBuilders() { return new ArrayList<>(itemBuilders); }
    public boolean isConsumeOnComplete() { return consumeOnComplete; }
    public @Nullable String getDescription() { return description; }
    public boolean isExactMatch() { return exactMatch; }

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

    private boolean hasEnoughItems(@NotNull Player player, @NotNull ItemStack requiredItem) {
        if (exactMatch) {
            return player.getInventory().containsAtLeast(requiredItem, requiredItem.getAmount());
        } else {
            var totalAmount = player.getInventory().all(requiredItem.getType()).values().stream()
                .mapToInt(ItemStack::getAmount).sum();
            return totalAmount >= requiredItem.getAmount();
        }
    }

    private int countItems(@NotNull Player player, @NotNull ItemStack requiredItem) {
        if (exactMatch) {
            return player.getInventory().all(requiredItem).values().stream()
                .filter(stack -> stack.isSimilar(requiredItem))
                .mapToInt(ItemStack::getAmount).sum();
        } else {
            return player.getInventory().all(requiredItem.getType()).values().stream()
                .mapToInt(ItemStack::getAmount).sum();
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