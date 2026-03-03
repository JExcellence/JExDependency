package com.raindropcentral.rds.items;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.UUID;

@JsonTypeName("ITEM")
public class ShopItem extends AbstractItem {
	
	@JsonProperty("entryId")
	private final UUID entryId;

	@JsonProperty("item")
	private final ItemStack item;
	
	@JsonProperty("amount")
	private final int amount;

	@JsonProperty("currencyType")
	private final String currencyType;

	@JsonProperty("value")
	private final double value;

	@JsonProperty("adminStockLimit")
	private final Integer adminStockLimit;

	@JsonProperty("adminRestockIntervalTicks")
	private final Long adminRestockIntervalTicks;

	@JsonProperty("adminStockReferenceTime")
	private final Long adminStockReferenceTime;
	
	@JsonCreator
	public ShopItem(
		@JsonProperty("entryId") @Nullable UUID entryId,
		@JsonProperty("item") @NotNull ItemStack item,
		@JsonProperty("amount") int amount,
		@JsonProperty("currencyType") @Nullable String currencyType,
		@JsonProperty("value") double value,
		@JsonProperty("adminStockLimit") @Nullable Integer adminStockLimit,
		@JsonProperty("adminRestockIntervalTicks") @Nullable Long adminRestockIntervalTicks,
		@JsonProperty("adminStockReferenceTime") @Nullable Long adminStockReferenceTime
	) {
		this.entryId = entryId == null ? UUID.randomUUID() : entryId;
		this.item = item.clone();
		this.item.setAmount(1);
		this.amount = Math.max(0, amount);
		this.currencyType = currencyType == null || currencyType.isBlank() ? "vault" : currencyType;
		this.value = Math.max(0D, value);
		this.adminStockLimit = adminStockLimit == null || adminStockLimit < 1 ? null : adminStockLimit;
		this.adminRestockIntervalTicks = adminRestockIntervalTicks == null || adminRestockIntervalTicks < 1L
				? null
				: adminRestockIntervalTicks;
		this.adminStockReferenceTime = adminStockReferenceTime == null || adminStockReferenceTime < 0L
				? null
				: adminStockReferenceTime;
	}
	
	/**
	 * Convenience constructor that uses the ItemStack's amount
	 */
	public ShopItem(@NotNull ItemStack item) {
		this(null, item, item.getAmount(), "vault", 0D, null, null, null);
	}

	public ShopItem(
		@Nullable UUID entryId,
		@NotNull ItemStack item,
		int amount
	) {
		this(entryId, item, amount, "vault", 0D, null, null, null);
	}

	public ShopItem(
		@NotNull ItemStack item,
		int amount,
		@Nullable String currencyType,
		double value
	) {
		this(null, item, amount, currencyType, value, null, null, null);
	}

	public ShopItem(
		@Nullable UUID entryId,
		@NotNull ItemStack item,
		int amount,
		@Nullable String currencyType,
		double value
	) {
		this(entryId, item, amount, currencyType, value, null, null, null);
	}
	
	@Override
	public @NotNull String getTypeId() {
		return "ITEM";
	}
	
	@Override
	public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
		return CompletableFuture.supplyAsync(() -> {
			int remaining = amount;
			int maxStack = item.getMaxStackSize();
			
			while (remaining > 0) {
				int stackAmount = Math.min(remaining, maxStack);
				ItemStack stack = item.clone();
				stack.setAmount(stackAmount);
				
				if (player.getInventory().firstEmpty() == -1) {
					player.getWorld().dropItem(player.getLocation(), stack);
				} else {
					player.getInventory().addItem(stack);
				}
				
				remaining -= stackAmount;
			}
			return true;
		});
	}
	
	@Override
	public double getEstimatedValue() {
		return amount * value;
	}
	
	/**
	 * Gets the item template (always amount 1)
	 */
	public ItemStack getItem() {
		return item.clone();
	}
	
	/**
	 * Gets the actual amount (can exceed max stack size)
	 */
	public int getAmount() {
		return amount;
	}

	public @NotNull UUID getEntryId() {
		return entryId;
	}

	public @NotNull String getCurrencyType() {
		return currencyType;
	}

	public double getValue() {
		return value;
	}

	public boolean hasAdminStockLimit() {
		return this.adminStockLimit != null && this.adminStockLimit > 0;
	}

	public int getAdminStockLimit() {
		return this.adminStockLimit == null ? -1 : this.adminStockLimit;
	}

	public long getAdminRestockIntervalTicks() {
		return this.adminRestockIntervalTicks == null ? -1L : this.adminRestockIntervalTicks;
	}

	public long getAdminStockReferenceTime() {
		return this.adminStockReferenceTime == null ? -1L : this.adminStockReferenceTime;
	}

	public @NotNull ShopItem withAmount(int updatedAmount) {
		return new ShopItem(
				this.entryId,
				this.item,
				updatedAmount,
				this.currencyType,
				this.value,
				this.adminStockLimit,
				this.adminRestockIntervalTicks,
				this.adminStockReferenceTime
		);
	}

	public @NotNull ShopItem withCurrencyType(@NotNull String updatedCurrencyType) {
		return new ShopItem(
				this.entryId,
				this.item,
				this.amount,
				updatedCurrencyType,
				this.value,
				this.adminStockLimit,
				this.adminRestockIntervalTicks,
				this.adminStockReferenceTime
		);
	}

	public @NotNull ShopItem withValue(double updatedValue) {
		return new ShopItem(
				this.entryId,
				this.item,
				this.amount,
				this.currencyType,
				updatedValue,
				this.adminStockLimit,
				this.adminRestockIntervalTicks,
				this.adminStockReferenceTime
		);
	}

	public @NotNull ShopItem withPricing(@NotNull String updatedCurrencyType, double updatedValue) {
		return new ShopItem(
				this.entryId,
				this.item,
				this.amount,
				updatedCurrencyType,
				updatedValue,
				this.adminStockLimit,
				this.adminRestockIntervalTicks,
				this.adminStockReferenceTime
		);
	}

	public @NotNull ShopItem withAdminStockSettings(
		@Nullable Integer updatedStockLimit,
		@Nullable Long updatedRestockIntervalTicks,
		@Nullable Long updatedReferenceTime
	) {
		return new ShopItem(
				this.entryId,
				this.item,
				this.amount,
				this.currencyType,
				this.value,
				updatedStockLimit,
				updatedRestockIntervalTicks,
				updatedReferenceTime
		);
	}

	public @NotNull ShopItem withAdminStockState(
		int updatedAmount,
		@Nullable Integer updatedStockLimit,
		@Nullable Long updatedRestockIntervalTicks,
		@Nullable Long updatedReferenceTime
	) {
		return new ShopItem(
				this.entryId,
				this.item,
				updatedAmount,
				this.currencyType,
				this.value,
				updatedStockLimit,
				updatedRestockIntervalTicks,
				updatedReferenceTime
		);
	}

	public @NotNull ShopItem clearAdminStockSettings() {
		return new ShopItem(
				this.entryId,
				this.item,
				this.amount,
				this.currencyType,
				this.value,
				null,
				null,
				null
		);
	}
	
	@Override
	public void validate() {
		if (item == null || item.getType().isAir()) {
			throw new IllegalArgumentException("Item reward must have a valid item");
		}
		if (amount < 0) {
			throw new IllegalArgumentException("Item amount must not be negative");
		}
		if (currencyType.isBlank()) {
			throw new IllegalArgumentException("Currency type must not be blank");
		}
		if (value < 0) {
			throw new IllegalArgumentException("Item value must not be negative");
		}
		if (this.adminStockLimit != null && this.adminStockLimit < 1) {
			throw new IllegalArgumentException("Admin stock limit must be at least 1 when set");
		}
		if (this.adminRestockIntervalTicks != null && this.adminRestockIntervalTicks < 1L) {
			throw new IllegalArgumentException("Admin restock interval must be at least 1 tick when set");
		}
	}

}
