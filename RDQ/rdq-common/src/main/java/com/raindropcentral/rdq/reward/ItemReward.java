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
 * Represents a reward that gives a specific item to a player.
 * <p>
 * When this reward is applied, the specified items are added to the player's inventory.
 * If the inventory is full, excess items are dropped at the player's location.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
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
     * Constructs a new {@code ItemReward} with the specified material and amount.
     *
     * @param material The material to reward.
     * @param amount   The number of items to give.
     */
    public ItemReward(final @NotNull Material material, final int amount) {
        this(material, amount, null, null, true);
    }

    /**
     * Constructs a new {@code ItemReward} with an ItemStack.
     *
     * @param itemStack The ItemStack to reward.
     */
    public ItemReward(final @NotNull ItemStack itemStack) {
        this(null, 0, itemStack, null, true);
    }

    /**
     * Constructs a new {@code ItemReward} with full configuration.
     *
     * @param material    The material (can be null if itemStack is provided).
     * @param amount      The amount (used if material is provided).
     * @param itemStack   The ItemStack (can be null if material is provided).
     * @param itemBuilder The ItemBuilder (can be null).
     * @param dropIfFull  Whether to drop items if inventory is full.
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

    @Override
    public void apply(final @NotNull Player player) {
        final ItemStack rewardItem = this.buildItemStack();
        final List<ItemStack> leftovers = this.giveItems(player, rewardItem);

        if (!leftovers.isEmpty() && this.dropIfFull) {
            leftovers.forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
    }

    @Override
    @NotNull
    public String getDescriptionKey() {
        return "reward.item";
    }

    @Nullable
    public Material getMaterial() {
        return this.material;
    }

    public int getAmount() {
        return this.amount;
    }

    @Nullable
    public ItemStack getItemStack() {
        return this.itemStack != null ? this.itemStack.clone() : null;
    }

    @Nullable
    public ItemBuilder getItemBuilder() {
        return this.itemBuilder;
    }

    public boolean isDropIfFull() {
        return this.dropIfFull;
    }

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