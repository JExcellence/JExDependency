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
 * @since TBD
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

    protected ItemRequirement() {
        super(Type.ITEM);
        this.requiredItems = new ArrayList<>();
        this.itemBuilders = new ArrayList<>();
        this.consumeOnComplete = true;
        this.description = null;
        this.exactMatch = true;
    }

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

    @Override
    public boolean isMet(final @NotNull Player player) {
        for (final ItemStack requiredItem : this.requiredItems) {
            if (!this.hasEnoughItems(player, requiredItem)) {
                return false;
            }
        }
        return true;
    }

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

    @Override
    public void consume(final @NotNull Player player) {
        if (!this.consumeOnComplete) {
            return;
        }

        for (final ItemStack requiredItem : this.requiredItems) {
            this.removeItems(player, requiredItem);
        }
    }

    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.item";
    }

    @NotNull
    public List<ItemStack> getRequiredItems() {
        return this.requiredItems.stream()
                .map(ItemStack::clone)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    @NotNull
    public List<ItemBuilder> getItemBuilders() {
        return new ArrayList<>(this.itemBuilders);
    }

    public boolean isConsumeOnComplete() {
        return this.consumeOnComplete;
    }

    @Nullable
    public String getDescription() {
        return this.description;
    }

    public boolean isExactMatch() {
        return this.exactMatch;
    }

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

    public record ItemProgress(
            int index,
            @NotNull ItemStack requiredItem,
            int requiredAmount,
            int currentAmount,
            double progress,
            boolean completed
    ) {
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

        @Override
        @NotNull
        public ItemStack requiredItem() {
            return this.requiredItem.clone();
        }

        public int getProgressPercentage() {
            return (int) (this.progress * 100);
        }

        public int getShortage() {
            return Math.max(0, this.requiredAmount - this.currentAmount);
        }
    }
}