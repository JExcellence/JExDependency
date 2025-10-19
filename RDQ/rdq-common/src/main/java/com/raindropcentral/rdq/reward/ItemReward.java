package com.raindropcentral.rdq.reward;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.evaluable.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a reward that provides a specific item or material to a player.
 * <p>
 * The reward can be configured through a raw {@link Material}, a predefined {@link ItemStack}, or
 * an {@link ItemBuilder}. When executed the reward attempts to place the configured items into the
 * player's inventory, optionally dropping any overflow on the ground.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ItemReward extends AbstractReward {

    @JsonProperty("material")
    private final Material material;

    @JsonProperty("amount")
    private final int amount;

    @JsonProperty("itemStack")
    private final ItemStack itemStack;

    @JsonProperty("itemBuilder")
    private final ItemBuilder itemBuilder;

    @JsonProperty("dropIfFull")
    private final boolean dropIfFull;

    /**
     * Constructs a new {@code ItemReward} that grants the provided material and amount.
     *
     * @param material the material to reward
     * @param amount   the quantity of the material to grant
     */
    public ItemReward(final @NotNull Material material, final int amount) {
        this(material, amount, null, null, true);
    }

    /**
     * Constructs a new {@code ItemReward} that grants the provided {@link ItemStack}.
     *
     * @param itemStack the item stack to reward
     */
    public ItemReward(final @NotNull ItemStack itemStack) {
        this(null, 0, itemStack, null, true);
    }

    /**
     * Constructs a new {@code ItemReward} with full configuration.
     *
     * @param material    the material to reward, or {@code null} if an item stack or builder is used
     * @param amount      the number of material items to give when {@code material} is specified
     * @param itemStack   the item stack to reward, or {@code null} when using a material or builder
     * @param itemBuilder the builder used to produce the rewarded stack, or {@code null}
     * @param dropIfFull  {@code true} to drop overflow items near the player, {@code false} to keep
     *                    them in the leftovers collection
     */
    @JsonCreator
    public ItemReward(
            @JsonProperty("material") final @Nullable Material material,
            @JsonProperty("amount") final int amount,
            @JsonProperty("itemStack") final @Nullable ItemStack itemStack,
            @JsonProperty("itemBuilder") final @Nullable ItemBuilder itemBuilder,
            @JsonProperty("dropIfFull") final boolean dropIfFull
    ) {
        super(Type.ITEM);

        if (material == null && itemStack == null && itemBuilder == null) {
            throw new IllegalArgumentException("Either material, itemStack, or itemBuilder must be provided");
        }

        if (material != null && amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amount);
        }

        this.material = material;
        this.amount = amount;
        this.itemStack = itemStack != null ? itemStack.clone() : null;
        this.itemBuilder = itemBuilder;
        this.dropIfFull = dropIfFull;
    }

    /**
     * Applies the reward to the specified player.
     * <p>
     * The configured items are inserted into the player's inventory. When the inventory does not
     * have enough free slots any overflow stacks are optionally dropped at the player's location
     * when {@link #isDropIfFull()} is {@code true}.
     * </p>
     *
     * @param player the player receiving the reward
     */
    @Override
    public void apply(final @NotNull Player player) {
        final ItemStack rewardItem = this.buildItemStack();
        final List<ItemStack> leftovers = this.giveItems(player, rewardItem);

        if (!leftovers.isEmpty() && this.dropIfFull) {
            leftovers.forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
    }

    /**
     * Retrieves the translation key that describes the reward in localized resources.
     *
     * @return the translation key representing an item reward
     */
    @Override
    @NotNull
    public String getDescriptionKey() {
        return "reward.item";
    }

    /**
     * Obtains the configured material used to construct the reward.
     *
     * @return the configured material, or {@code null} when the reward uses a stack or builder
     */
    @Nullable
    public Material getMaterial() {
        return this.material;
    }

    /**
     * Obtains the configured amount of material to reward.
     *
     * @return the configured material amount
     */
    public int getAmount() {
        return this.amount;
    }

    /**
     * Retrieves the configured {@link ItemStack} used by the reward.
     *
     * @return a clone of the configured stack, or {@code null} if the reward uses another source
     */
    @Nullable
    public ItemStack getItemStack() {
        return this.itemStack != null ? this.itemStack.clone() : null;
    }

    /**
     * Obtains the {@link ItemBuilder} responsible for producing the rewarded stack.
     *
     * @return the configured builder, or {@code null} when a stack or material is used
     */
    @Nullable
    public ItemBuilder getItemBuilder() {
        return this.itemBuilder;
    }

    /**
     * Indicates whether overflow stacks should be dropped when the player's inventory is full.
     *
     * @return {@code true} if excess items are dropped on the ground, {@code false} otherwise
     */
    public boolean isDropIfFull() {
        return this.dropIfFull;
    }

    /**
     * Builds the {@link ItemStack} representing the reward using the configured material, stack,
     * or builder.
     *
     * @return the item stack that should be delivered to the player
     */
    @JsonIgnore
    @NotNull
    private ItemStack buildItemStack() {
        if (this.itemBuilder != null) {
            return this.itemBuilder.build();
        }
        if (this.itemStack != null) {
            return this.itemStack.clone();
        }
        return new ItemStack(this.material, this.amount);
    }

    /**
     * Attempts to add the provided item to the player's inventory and collects any leftover
     * stacks that could not be inserted.
     *
     * @param player the player receiving the items
     * @param item   the item stack to distribute
     * @return a list containing stacks that did not fit in the player's inventory
     */
    @NotNull
    private List<ItemStack> giveItems(final @NotNull Player player, final @NotNull ItemStack item) {
        final List<ItemStack> leftovers = new ArrayList<>();
        int remaining = item.getAmount();
        final int maxStack = item.getMaxStackSize();

        final ItemStack singleStack = item.clone();
        singleStack.setAmount(1);

        while (remaining > 0) {
            final int giveAmount = Math.min(remaining, maxStack);
            final ItemStack stackToGive = singleStack.clone();
            stackToGive.setAmount(giveAmount);

            final Map<Integer, ItemStack> notFit = player.getInventory().addItem(stackToGive);
            if (!notFit.isEmpty()) {
                leftovers.addAll(notFit.values());
            }

            remaining -= giveAmount;
        }

        return leftovers;
    }
}