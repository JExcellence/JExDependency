package com.raindropcentral.rds.items;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/**
 * Represents shop item.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@JsonTypeName("ITEM")
@JsonIgnoreProperties(
		value = {"availableNow", "estimatedValue", "typeId", "descriptionKey"},
		ignoreUnknown = true
)
public class ShopItem extends AbstractItem {

	public static final int DEFAULT_ROTATION_MINUTES = 60;
	private static final long ROTATION_MINUTE_MILLIS = TimeUnit.MINUTES.toMillis(1L);

	/**
	 * Defines the item availability policies for customer views.
	 */
	public enum AvailabilityMode {
		ALWAYS,
		ROTATE,
		NEVER;

		/**
		 * Returns the next availability mode in cycle order.
		 *
		 * @return next availability mode
		 */
		public @NotNull AvailabilityMode next() {
			return switch (this) {
				case ALWAYS -> ROTATE;
				case ROTATE -> NEVER;
				case NEVER -> ALWAYS;
			};
		}
	}

	/**
	 * Defines who executes configured admin-purchase commands.
	 */
	public enum CommandExecutionMode {
		SERVER,
		PLAYER;

		/**
		 * Returns the next execution mode in cycle order.
		 *
		 * @return next execution mode
		 */
		public @NotNull CommandExecutionMode next() {
			return switch (this) {
				case SERVER -> PLAYER;
				case PLAYER -> SERVER;
			};
		}
	}

	/**
	 * Represents a single command action to run after an admin-shop purchase.
	 *
	 * @param command command text to execute
	 * @param executionMode execution source
	 * @param delayTicks delay in ticks before execution
	 */
	public record AdminPurchaseCommand(
			@NotNull String command,
			@NotNull CommandExecutionMode executionMode,
			long delayTicks
	) {
		/**
		 * Creates a new normalized admin purchase command.
		 *
		 * @param command command text to execute
		 * @param executionMode execution source
		 * @param delayTicks delay in ticks before execution
		 */
		public AdminPurchaseCommand {
			command = normalizeCommand(command);
			executionMode = executionMode == null ? CommandExecutionMode.SERVER : executionMode;
			delayTicks = Math.max(0L, delayTicks);
		}

		/**
		 * Parses admin command input from the item editor anvil prompt.
		 *
		 * <p>Supported formats:</p>
		 * <p>{@code command}</p>
		 * <p>{@code delayTicks | command}</p>
		 *
		 * @param input raw anvil input
		 * @param executionMode selected execution mode
		 * @return parsed command action
		 * @throws IllegalArgumentException when input is empty or malformed
		 */
		public static @NotNull AdminPurchaseCommand fromInput(
				final @NotNull String input,
				final @NotNull CommandExecutionMode executionMode
		) {
			final String trimmedInput = input.trim();
			if (trimmedInput.isEmpty()) {
				throw new IllegalArgumentException("Admin command input cannot be empty");
			}

			long parsedDelay = 0L;
			String parsedCommand = trimmedInput;
			final int delimiterIndex = trimmedInput.indexOf('|');
			if (delimiterIndex > -1) {
				final String left = trimmedInput.substring(0, delimiterIndex).trim();
				final String right = trimmedInput.substring(delimiterIndex + 1).trim();
				if (!left.isEmpty()) {
					try {
						parsedDelay = Math.max(0L, Long.parseLong(left));
					} catch (NumberFormatException exception) {
						throw new IllegalArgumentException("Delay must be a whole number of ticks", exception);
					}
				}
				parsedCommand = right;
			}

			return new AdminPurchaseCommand(parsedCommand, executionMode, parsedDelay);
		}

		private static @NotNull String normalizeCommand(
				final @Nullable String command
		) {
			if (command == null || command.trim().isEmpty()) {
				throw new IllegalArgumentException("Admin purchase command cannot be empty");
			}

			return command.trim();
		}
	}
	
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

	@JsonProperty("availabilityMode")
	private final AvailabilityMode availabilityMode;

	@JsonProperty("availabilityRotationMinutes")
	private final Integer availabilityRotationMinutes;

	@JsonProperty("adminPurchaseCommands")
	private final List<AdminPurchaseCommand> adminPurchaseCommands;
	
	/**
	 * Creates a new shop item.
	 *
	 * @param entryId entry id
	 * @param item target item payload
	 * @param amount amount
	 * @param currencyType currency type
	 * @param value value
	 * @param adminStockLimit admin stock limit
	 * @param adminRestockIntervalTicks admin restock interval ticks
	 * @param adminStockReferenceTime admin stock reference time
	 * @param availabilityMode availability mode
	 * @param availabilityRotationMinutes rotation window in minutes
	 */
	@JsonCreator
	public ShopItem(
		@JsonProperty("entryId") @Nullable UUID entryId,
		@JsonProperty("item") @NotNull ItemStack item,
		@JsonProperty("amount") int amount,
		@JsonProperty("currencyType") @Nullable String currencyType,
		@JsonProperty("value") double value,
		@JsonProperty("adminStockLimit") @Nullable Integer adminStockLimit,
		@JsonProperty("adminRestockIntervalTicks") @Nullable Long adminRestockIntervalTicks,
		@JsonProperty("adminStockReferenceTime") @Nullable Long adminStockReferenceTime,
		@JsonProperty("availabilityMode") @Nullable AvailabilityMode availabilityMode,
		@JsonProperty("availabilityRotationMinutes") @Nullable Integer availabilityRotationMinutes,
		@JsonProperty("adminPurchaseCommands") @Nullable List<AdminPurchaseCommand> adminPurchaseCommands
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
		this.availabilityMode = availabilityMode == null ? AvailabilityMode.ALWAYS : availabilityMode;
		this.availabilityRotationMinutes = availabilityRotationMinutes == null || availabilityRotationMinutes < 1
				? DEFAULT_ROTATION_MINUTES
				: availabilityRotationMinutes;
		this.adminPurchaseCommands = this.normalizeAdminPurchaseCommands(adminPurchaseCommands);
	}

	/**
	 * Creates a new shop item.
	 *
	 * @param entryId entry id
	 * @param item target item payload
	 * @param amount amount
	 * @param currencyType currency type
	 * @param value value
	 * @param adminStockLimit admin stock limit
	 * @param adminRestockIntervalTicks admin restock interval ticks
	 * @param adminStockReferenceTime admin stock reference time
	 */
	public ShopItem(
		@Nullable UUID entryId,
		@NotNull ItemStack item,
		int amount,
		@Nullable String currencyType,
		double value,
		@Nullable Integer adminStockLimit,
		@Nullable Long adminRestockIntervalTicks,
		@Nullable Long adminStockReferenceTime
	) {
		this(
			entryId,
			item,
			amount,
			currencyType,
			value,
			adminStockLimit,
			adminRestockIntervalTicks,
			adminStockReferenceTime,
			AvailabilityMode.ALWAYS,
			DEFAULT_ROTATION_MINUTES,
			List.of()
		);
	}
	
	/**
	 * Convenience constructor that uses the ItemStack's amount
	 */
	public ShopItem(@NotNull ItemStack item) {
		this(null, item, item.getAmount(), "vault", 0D, null, null, null);
	}

	/**
	 * Creates a new shop item.
	 *
	 * @param entryId entry id
	 * @param item target item payload
	 * @param amount amount
	 */
	public ShopItem(
		@Nullable UUID entryId,
		@NotNull ItemStack item,
		int amount
	) {
		this(entryId, item, amount, "vault", 0D, null, null, null);
	}

	/**
	 * Creates a new shop item.
	 *
	 * @param item target item payload
	 * @param amount amount
	 * @param currencyType currency type
	 * @param value value
	 */
	public ShopItem(
		@NotNull ItemStack item,
		int amount,
		@Nullable String currencyType,
		double value
	) {
		this(null, item, amount, currencyType, value, null, null, null);
	}

	/**
	 * Creates a new shop item.
	 *
	 * @param entryId entry id
	 * @param item target item payload
	 * @param amount amount
	 * @param currencyType currency type
	 * @param value value
	 */
	public ShopItem(
		@Nullable UUID entryId,
		@NotNull ItemStack item,
		int amount,
		@Nullable String currencyType,
		double value
	) {
		this(entryId, item, amount, currencyType, value, null, null, null);
	}
	
	/**
	 * Returns the type id.
	 *
	 * @return the type id
	 */
	@Override
	public @NotNull String getTypeId() {
		return "ITEM";
	}
	
	/**
	 * Grants this item payload to the supplied player.
	 *
	 * @param player target player
	 * @return the grant result
	 */
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
	
	/**
	 * Returns the estimated value.
	 *
	 * @return the estimated value
	 */
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

	/**
	 * Returns the entry id.
	 *
	 * @return the entry id
	 */
	public @NotNull UUID getEntryId() {
		return entryId;
	}

	/**
	 * Returns the currency type.
	 *
	 * @return the currency type
	 */
	public @NotNull String getCurrencyType() {
		return currencyType;
	}

	/**
	 * Returns the value.
	 *
	 * @return the value
	 */
	public double getValue() {
		return value;
	}

	/**
	 * Indicates whether admin stock limit is available.
	 *
	 * @return {@code true} if admin stock limit; otherwise {@code false}
	 */
	public boolean hasAdminStockLimit() {
		return this.adminStockLimit != null && this.adminStockLimit > 0;
	}

	/**
	 * Returns the admin stock limit.
	 *
	 * @return the admin stock limit
	 */
	public int getAdminStockLimit() {
		return this.adminStockLimit == null ? -1 : this.adminStockLimit;
	}

	/**
	 * Returns the admin restock interval ticks.
	 *
	 * @return the admin restock interval ticks
	 */
	public long getAdminRestockIntervalTicks() {
		return this.adminRestockIntervalTicks == null ? -1L : this.adminRestockIntervalTicks;
	}

	/**
	 * Returns the admin stock reference time.
	 *
	 * @return the admin stock reference time
	 */
	public long getAdminStockReferenceTime() {
		return this.adminStockReferenceTime == null ? -1L : this.adminStockReferenceTime;
	}

	/**
	 * Returns the availability mode.
	 *
	 * @return the availability mode
	 */
	public @NotNull AvailabilityMode getAvailabilityMode() {
		return this.availabilityMode;
	}

	/**
	 * Returns the configured rotation window in minutes.
	 *
	 * @return rotation window in minutes
	 */
	public int getAvailabilityRotationMinutes() {
		return this.availabilityRotationMinutes == null
				? DEFAULT_ROTATION_MINUTES
				: Math.max(1, this.availabilityRotationMinutes);
	}

	/**
	 * Returns configured admin-purchase commands for this item.
	 *
	 * @return immutable command list
	 */
	public @NotNull List<AdminPurchaseCommand> getAdminPurchaseCommands() {
		return this.adminPurchaseCommands;
	}

	/**
	 * Indicates whether this item has configured admin-purchase commands.
	 *
	 * @return {@code true} when at least one command exists
	 */
	public boolean hasAdminPurchaseCommands() {
		return !this.adminPurchaseCommands.isEmpty();
	}

	/**
	 * Indicates whether this item should be visible and purchasable right now.
	 *
	 * @return {@code true} when currently available; otherwise {@code false}
	 */
	@JsonIgnore
	public boolean isAvailableNow() {
		return this.isAvailableAt(System.currentTimeMillis());
	}

	/**
	 * Indicates whether this item is available at the provided epoch timestamp.
	 *
	 * @param timestampMillis epoch timestamp in milliseconds
	 * @return {@code true} when available; otherwise {@code false}
	 */
	public boolean isAvailableAt(final long timestampMillis) {
		return switch (this.getAvailabilityMode()) {
			case ALWAYS -> true;
			case NEVER -> false;
			case ROTATE -> {
				final long rotationWindowMillis = Math.max(1L, this.getAvailabilityRotationMinutes())
						* ROTATION_MINUTE_MILLIS;
				final long cycleMillis = Math.max(1L, rotationWindowMillis * 2L);
				final long rotationOffset = Math.floorMod(
						this.entryId.getMostSignificantBits() ^ this.entryId.getLeastSignificantBits(),
						cycleMillis
				);
				final long cyclePosition = Math.floorMod(timestampMillis + rotationOffset, cycleMillis);
				yield cyclePosition < rotationWindowMillis;
			}
		};
	}

	/**
	 * Returns a copy with updated admin-purchase commands.
	 *
	 * @param updatedCommands updated command list
	 * @return copied shop item with updated commands
	 */
	public @NotNull ShopItem withAdminPurchaseCommands(
			final @Nullable List<AdminPurchaseCommand> updatedCommands
	) {
		return new ShopItem(
				this.entryId,
				this.item,
				this.amount,
				this.currencyType,
				this.value,
				this.adminStockLimit,
				this.adminRestockIntervalTicks,
				this.adminStockReferenceTime,
				this.availabilityMode,
				this.availabilityRotationMinutes,
				updatedCommands
		);
	}

	/**
	 * Returns a copy with one additional admin-purchase command appended.
	 *
	 * @param command command to append
	 * @return copied shop item with appended command
	 */
	public @NotNull ShopItem withAddedAdminPurchaseCommand(
			final @NotNull AdminPurchaseCommand command
	) {
		final List<AdminPurchaseCommand> updatedCommands = new ArrayList<>(this.adminPurchaseCommands);
		updatedCommands.add(command);
		return this.withAdminPurchaseCommands(updatedCommands);
	}

	/**
	 * Returns a copy with the last admin-purchase command removed.
	 *
	 * @return copied shop item with last command removed
	 */
	public @NotNull ShopItem withoutLastAdminPurchaseCommand() {
		if (this.adminPurchaseCommands.isEmpty()) {
			return this;
		}

		final List<AdminPurchaseCommand> updatedCommands = new ArrayList<>(this.adminPurchaseCommands);
		updatedCommands.remove(updatedCommands.size() - 1);
		return this.withAdminPurchaseCommands(updatedCommands);
	}

	/**
	 * Returns a copy without any admin-purchase commands.
	 *
	 * @return copied shop item with no commands
	 */
	public @NotNull ShopItem clearAdminPurchaseCommands() {
		return this.withAdminPurchaseCommands(List.of());
	}
	
	/**
	 * Returns a copy with updated amount.
	 *
	 * @param updatedAmount updated item amount
	 * @return a copy with updated amount
	 */
	public @NotNull ShopItem withAmount(int updatedAmount) {
		return new ShopItem(
				this.entryId,
				this.item,
				updatedAmount,
				this.currencyType,
				this.value,
				this.adminStockLimit,
				this.adminRestockIntervalTicks,
				this.adminStockReferenceTime,
				this.availabilityMode,
				this.availabilityRotationMinutes,
				this.adminPurchaseCommands
		);
	}

	/**
	 * Returns a copy with updated currency type.
	 *
	 * @param updatedCurrencyType updated currency type
	 * @return a copy with updated currency type
	 */
	public @NotNull ShopItem withCurrencyType(@NotNull String updatedCurrencyType) {
		return new ShopItem(
				this.entryId,
				this.item,
				this.amount,
				updatedCurrencyType,
				this.value,
				this.adminStockLimit,
				this.adminRestockIntervalTicks,
				this.adminStockReferenceTime,
				this.availabilityMode,
				this.availabilityRotationMinutes,
				this.adminPurchaseCommands
		);
	}

	/**
	 * Returns a copy with updated value.
	 *
	 * @param updatedValue updated price value
	 * @return a copy with updated value
	 */
	public @NotNull ShopItem withValue(double updatedValue) {
		return new ShopItem(
				this.entryId,
				this.item,
				this.amount,
				this.currencyType,
				updatedValue,
				this.adminStockLimit,
				this.adminRestockIntervalTicks,
				this.adminStockReferenceTime,
				this.availabilityMode,
				this.availabilityRotationMinutes,
				this.adminPurchaseCommands
		);
	}

	/**
	 * Returns a copy with updated pricing.
	 *
	 * @param updatedCurrencyType updated currency type
	 * @param updatedValue updated price value
	 * @return a copy with updated pricing
	 */
	public @NotNull ShopItem withPricing(@NotNull String updatedCurrencyType, double updatedValue) {
		return new ShopItem(
				this.entryId,
				this.item,
				this.amount,
				updatedCurrencyType,
				updatedValue,
				this.adminStockLimit,
				this.adminRestockIntervalTicks,
				this.adminStockReferenceTime,
				this.availabilityMode,
				this.availabilityRotationMinutes,
				this.adminPurchaseCommands
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
				updatedReferenceTime,
				this.availabilityMode,
				this.availabilityRotationMinutes,
				this.adminPurchaseCommands
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
				updatedReferenceTime,
				this.availabilityMode,
				this.availabilityRotationMinutes,
				this.adminPurchaseCommands
		);
	}

	/**
	 * Returns a copy with updated availability mode.
	 *
	 * @param updatedMode updated availability mode
	 * @return a copy with updated availability mode
	 */
	public @NotNull ShopItem withAvailabilityMode(
		final @NotNull AvailabilityMode updatedMode
	) {
		return new ShopItem(
				this.entryId,
				this.item,
				this.amount,
				this.currencyType,
				this.value,
				this.adminStockLimit,
				this.adminRestockIntervalTicks,
				this.adminStockReferenceTime,
				updatedMode,
				this.availabilityRotationMinutes,
				this.adminPurchaseCommands
		);
	}

	/**
	 * Returns a copy with updated availability rotation window.
	 *
	 * @param updatedRotationMinutes updated rotation window in minutes
	 * @return a copy with updated availability rotation window
	 */
	public @NotNull ShopItem withAvailabilityRotationMinutes(
		final int updatedRotationMinutes
	) {
		return new ShopItem(
				this.entryId,
				this.item,
				this.amount,
				this.currencyType,
				this.value,
				this.adminStockLimit,
				this.adminRestockIntervalTicks,
				this.adminStockReferenceTime,
				this.availabilityMode,
				updatedRotationMinutes,
				this.adminPurchaseCommands
		);
	}

	/**
	 * Returns a copy with updated availability settings.
	 *
	 * @param updatedMode updated availability mode
	 * @param updatedRotationMinutes updated rotation window in minutes
	 * @return a copy with updated availability settings
	 */
	public @NotNull ShopItem withAvailabilitySettings(
		final @NotNull AvailabilityMode updatedMode,
		final int updatedRotationMinutes
	) {
		return new ShopItem(
				this.entryId,
				this.item,
				this.amount,
				this.currencyType,
				this.value,
				this.adminStockLimit,
				this.adminRestockIntervalTicks,
				this.adminStockReferenceTime,
				updatedMode,
				updatedRotationMinutes,
				this.adminPurchaseCommands
		);
	}

	/**
	 * Clears the admin stock settings.
	 *
	 * @return the clear admin stock settings result
	 */
	public @NotNull ShopItem clearAdminStockSettings() {
		return new ShopItem(
				this.entryId,
				this.item,
				this.amount,
				this.currencyType,
				this.value,
				null,
				null,
				null,
				this.availabilityMode,
				this.availabilityRotationMinutes,
				this.adminPurchaseCommands
		);
	}

	private @NotNull List<AdminPurchaseCommand> normalizeAdminPurchaseCommands(
			final @Nullable List<AdminPurchaseCommand> commands
	) {
		if (commands == null || commands.isEmpty()) {
			return List.of();
		}

		final List<AdminPurchaseCommand> normalized = new ArrayList<>();
		for (final AdminPurchaseCommand command : commands) {
			if (command == null) {
				continue;
			}
			normalized.add(command);
		}

		return normalized.isEmpty() ? List.of() : List.copyOf(normalized);
	}
	
	/**
	 * Validates the current shop item.
	 *
	 * @throws IllegalArgumentException if the current value set is invalid
	 */
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
		if (this.availabilityRotationMinutes != null && this.availabilityRotationMinutes < 1) {
			throw new IllegalArgumentException("Availability rotation minutes must be at least 1 when set");
		}
		for (final AdminPurchaseCommand command : this.adminPurchaseCommands) {
			if (command == null || command.command().isBlank()) {
				throw new IllegalArgumentException("Admin purchase command cannot be blank");
			}
			if (command.delayTicks() < 0L) {
				throw new IllegalArgumentException("Admin purchase command delay must not be negative");
			}
		}
	}

}
