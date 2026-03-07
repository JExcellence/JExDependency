package com.raindropcentral.rplatform.requirement.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import de.jexcellence.evaluable.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public final class ItemRequirement extends AbstractRequirement {

    private static final Logger LOGGER = Logger.getLogger(ItemRequirement.class.getName());

    @JsonProperty("requiredItems")
    private final List<ItemStack> requiredItems;

    @JsonProperty("requiredAmounts")
    private final List<Integer> requiredAmounts;

    @JsonProperty("itemBuilders")
    private final List<ItemBuilder> itemBuilders;

    @JsonProperty("consumeOnComplete")
    private final boolean consumeOnComplete;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("exactMatch")
    private final boolean exactMatch;

    protected ItemRequirement() {
        super("ITEM");
        this.requiredItems = new ArrayList<>();
        this.requiredAmounts = new ArrayList<>();
        this.itemBuilders = new ArrayList<>();
        this.consumeOnComplete = true;
        this.description = null;
        this.exactMatch = true;
    }

    public ItemRequirement(
            @JsonProperty("requiredItems") @Nullable final List<ItemStack> requiredItems,
            @JsonProperty("itemBuilders") @Nullable final List<ItemBuilder> itemBuilders,
            @JsonProperty("consumeOnComplete") @Nullable final Boolean consumeOnComplete,
            @JsonProperty("description") @Nullable final String description,
            @JsonProperty("exactMatch") @Nullable final Boolean exactMatch
    ) {
        this(requiredItems, null, itemBuilders, consumeOnComplete, description, exactMatch);
    }

    @JsonCreator
    public ItemRequirement(
            @JsonProperty("requiredItems") @Nullable final List<ItemStack> requiredItems,
            @JsonProperty("requiredAmounts") @Nullable final List<Integer> requiredAmounts,
            @JsonProperty("itemBuilders") @Nullable final List<ItemBuilder> itemBuilders,
            @JsonProperty("consumeOnComplete") @Nullable final Boolean consumeOnComplete,
            @JsonProperty("description") @Nullable final String description,
            @JsonProperty("exactMatch") @Nullable final Boolean exactMatch
    ) {
        super("ITEM");

        final List<ItemStack> items = requiredItems != null ? requiredItems : new ArrayList<>();
        final List<ItemBuilder> builders = itemBuilders != null ? itemBuilders : new ArrayList<>();
        this.requiredItems = new ArrayList<>();
        this.requiredAmounts = new ArrayList<>();
        this.itemBuilders = new ArrayList<>(builders);

        if (!items.isEmpty()) {
            for (int index = 0; index < items.size(); index++) {
                final Integer explicitAmount = requiredAmounts != null && index < requiredAmounts.size()
                        ? requiredAmounts.get(index)
                        : null;
                this.addRequiredItem(items.get(index), explicitAmount);
            }
        } else if (!builders.isEmpty()) {
            for (final ItemBuilder builder : builders) {
                if (builder == null) {
                    throw new IllegalArgumentException("Item builder in requirements cannot be null.");
                }
                this.addRequiredItem(builder.build(), null);
            }
        } else {
            throw new IllegalArgumentException("At least one required item or item builder must be specified.");
        }

        this.consumeOnComplete = consumeOnComplete != null ? consumeOnComplete : true;
        this.description = description;
        this.exactMatch = exactMatch != null ? exactMatch : true;

        if (this.requiredItems.isEmpty()) {
            throw new IllegalArgumentException("At least one required item must be specified.");
        }

        for (int index = 0; index < this.requiredItems.size(); index++) {
            final ItemStack item = this.requiredItems.get(index);
            final int requiredAmount = this.requiredAmounts.get(index);
            if (item == null || item.getType().isAir() || requiredAmount <= 0) {
                throw new IllegalArgumentException("Invalid item in requirements: " + item);
            }
        }
    }


    @Override
    public boolean isMet(@NotNull Player player) {
        return IntStream.range(0, this.requiredItems.size())
                .allMatch(index -> this.hasEnoughItems(
                        player,
                        this.requiredItems.get(index),
                        this.requiredAmounts.get(index)
                ));
    }

    @Override
    public double calculateProgress(@NotNull Player player) {
        if (this.requiredItems.isEmpty()) return 1.0;

        double totalCollected = 0.0;
        double totalRequired = 0.0;

        for (int index = 0; index < this.requiredItems.size(); index++) {
            final ItemStack requiredItem = this.requiredItems.get(index);
            final int requiredAmount = this.requiredAmounts.get(index);
            final int actualAmount = this.countItems(player, requiredItem);
            totalCollected += Math.min(actualAmount, requiredAmount);
            totalRequired += requiredAmount;
        }

        return totalRequired > 0 ? Math.min(1.0, totalCollected / totalRequired) : 1.0;
    }

    @Override
    public void consume(@NotNull Player player) {
        if (!this.consumeOnComplete) return;
        for (int index = 0; index < this.requiredItems.size(); index++) {
            this.removeItems(player, this.requiredItems.get(index), this.requiredAmounts.get(index));
        }
    }

    @Override
    public @NotNull String getDescriptionKey() {
        return "requirement.item";
    }

    public List<ItemStack> getRequiredItems() {
        return IntStream.range(0, this.requiredItems.size())
                .mapToObj(this::createRequiredItemCopy)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public List<ItemBuilder> getItemBuilders() { return new ArrayList<>(itemBuilders); }
    public boolean isConsumeOnComplete() { return consumeOnComplete; }
    public @Nullable String getDescription() { return description; }
    public boolean isExactMatch() { return exactMatch; }

    @JsonIgnore
    @NotNull
    public List<ItemProgress> getDetailedProgress(final @NotNull Player player) {
        return IntStream.range(0, this.requiredItems.size())
                .mapToObj(index -> {
                    final ItemStack requiredItem = this.requiredItems.get(index);
                    final int requiredAmount = this.requiredAmounts.get(index);
                    final int currentAmount = this.countItems(player, requiredItem);
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
        for (int index = 0; index < this.requiredItems.size(); index++) {
            final ItemStack requiredItem = this.requiredItems.get(index);
            final int requiredAmount = this.requiredAmounts.get(index);
            final int currentAmount = this.countItems(player, requiredItem);
            final int shortage = Math.max(0, requiredAmount - currentAmount);
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
            if (this.requiredAmounts.get(i) <= 0) {
                throw new IllegalStateException("Required item at index " + i + " has invalid amount: " + this.requiredAmounts.get(i));
            }
        }
    }

    private boolean hasEnoughItems(
            @NotNull final Player player,
            @NotNull final ItemStack requiredItem,
            final int requiredAmount
    ) {
        if (exactMatch) {
            return player.getInventory().containsAtLeast(requiredItem, requiredAmount);
        } else {
            var totalAmount = player.getInventory().all(requiredItem.getType()).values().stream()
                .mapToInt(ItemStack::getAmount).sum();
            return totalAmount >= requiredAmount;
        }
    }

    private int countItems(@NotNull Player player, @NotNull ItemStack requiredItem) {
        if (exactMatch) {
            return Arrays.stream(player.getInventory().getContents())
                    .filter(Objects::nonNull)
                    .filter(stack -> stack.isSimilar(requiredItem))
                    .mapToInt(ItemStack::getAmount)
                    .sum();
        } else {
            return player.getInventory().all(requiredItem.getType()).values().stream()
                .mapToInt(ItemStack::getAmount).sum();
        }
    }

    private void removeItems(
            final @NotNull Player player,
            final @NotNull ItemStack requiredItem,
            final int requiredAmount
    ) {
        int remaining = requiredAmount;
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

    private void addRequiredItem(
            @Nullable final ItemStack item,
            @Nullable final Integer explicitAmount
    ) {
        if (item == null || item.getType().isAir()) {
            throw new IllegalArgumentException("Invalid item in requirements: " + item);
        }

        final int requiredAmount = explicitAmount != null ? explicitAmount : item.getAmount();
        if (requiredAmount <= 0) {
            throw new IllegalArgumentException("Invalid required item amount: " + requiredAmount);
        }

        final ItemStack template = item.clone();
        template.setAmount(1);

        this.requiredItems.add(template);
        this.requiredAmounts.add(requiredAmount);
    }

    private @NotNull ItemStack createRequiredItemCopy(final int index) {
        final ItemStack item = this.requiredItems.get(index).clone();
        item.setAmount(this.requiredAmounts.get(index));
        return item;
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
