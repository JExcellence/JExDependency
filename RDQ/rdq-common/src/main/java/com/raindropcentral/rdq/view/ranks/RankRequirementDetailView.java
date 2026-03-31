/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdq.view.ranks;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import com.raindropcentral.rdq.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rdq.manager.RankRequirementProgressManager;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.impl.ExperienceLevelRequirement;
import com.raindropcentral.rplatform.requirement.impl.ItemRequirement;
import com.raindropcentral.rplatform.requirement.impl.PlaytimeRequirement;
import com.raindropcentral.rplatform.utility.heads.view.Return;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enhanced paginated detailed view for a specific requirement showing comprehensive progress information.
 * This view provides detailed breakdowns of what players need to complete requirements with pagination support.
 *
 * @author ItsRainingHP
 * @version 4.0.0
 * @since TBD
 */
public class RankRequirementDetailView extends APaginatedView<RankRequirementDetailView.RequirementDetailItem> {

	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");

	private final State<RDQ>                     rdq               = initialState("plugin");
	private final State<RDQPlayer>               currentPlayer     = initialState("player");
	private final State<RRankTree>               selectedRankTree  = initialState("rankTree");
	private final State<com.raindropcentral.rdq.database.entity.rank.RRank> targetRank = initialState("targetRank");
	private final State<RRankUpgradeRequirement> targetRequirement = initialState("requirement");
	private final State<Boolean>                 previewMode       = initialState("previewMode");

	private RankRequirementProgressManager progressManager;

	/**
	 * Represents an item to display in the paginated view.
	 */
	public static class RequirementDetailItem {

		private final ItemStack                    displayItem;
		private final String                       itemName;
		private final ItemRequirement.ItemProgress itemProgress;
		private final int                          itemIndex;
		private final RequirementDetailItemType    type;

		/**
		 * Executes RequirementDetailItem.
		 */
		public RequirementDetailItem(
				final @NotNull ItemStack displayItem,
				final @NotNull String itemName,
				final @NotNull ItemRequirement.ItemProgress itemProgress,
				final int itemIndex
		) {

			this.displayItem = displayItem;
			this.itemName = itemName;
			this.itemProgress = itemProgress;
			this.itemIndex = itemIndex;
			this.type = RequirementDetailItemType.ITEM_PROGRESS;
		}

		/**
		 * Executes RequirementDetailItem.
		 */
		public RequirementDetailItem(
				final @NotNull ItemStack displayItem,
				final @NotNull String itemName,
				final @NotNull RequirementDetailItemType type
		) {

			this.displayItem = displayItem;
			this.itemName = itemName;
			this.itemProgress = null;
			this.itemIndex = - 1;
			this.type = type;
		}

		/**
		 * Gets displayItem.
		 */
		public ItemStack getDisplayItem() {

			return displayItem;
		}

		/**
		 * Gets itemName.
		 */
		public String getItemName() {

			return itemName;
		}

		/**
		 * Gets itemProgress.
		 */
		public ItemRequirement.ItemProgress getItemProgress() {

			return itemProgress;
		}

		/**
		 * Gets itemIndex.
		 */
		public int getItemIndex() {

			return itemIndex;
		}

		/**
		 * Gets type.
		 */
		public RequirementDetailItemType getType() {

			return type;
		}

	}

	/**
	 * Represents the RequirementDetailItemType API type.
	 */
	public enum RequirementDetailItemType {
		ITEM_PROGRESS,
		SUMMARY,
		TIPS,
		FILLER
	}

	/**
	 * Executes RankRequirementDetailView.
	 */
	public RankRequirementDetailView() {

		super(RankRequirementsJourneyView.class);
	}

	@Override
	protected String getKey() {

		return "rank_requirement_detail_ui";
	}

	@Override
	protected String[] getLayout() {

		return new String[]{
				"    T    ",  // Row 0: Tips at center (slot 4)
				" O O O O ",  // Row 1: Pagination items (4 per row)
				" O O O O ",  // Row 2: Pagination items
				" O O O O ",  // Row 3: Pagination items
				" O O O O ",  // Row 4: Pagination items
				"b  <p>  S"   // Row 5: Back, pagination controls, Submit
		};
	}

	@Override
	protected CompletableFuture<List<RequirementDetailItem>> getAsyncPaginationSource(
			final @NotNull Context context
	) {

		return CompletableFuture.supplyAsync(() -> {
			try {
				final RRankUpgradeRequirement requirement = this.targetRequirement.get(context);
				final Player                  player      = context.getPlayer();

				if (requirement == null) {
					return null;
				}

				List<RequirementDetailItem> items = new ArrayList<>();
				AbstractRequirement abstractReq = requirement.getRequirement().getRequirement();

				if (abstractReq instanceof ItemRequirement itemReq) {
					// For item requirements, show each item in pagination
					List<ItemRequirement.ItemProgress> itemProgressList = itemReq.getDetailedProgress(player);

					for (int i = 0; i < itemProgressList.size(); i++) {
						ItemRequirement.ItemProgress itemProgress = itemProgressList.get(i);
						ItemStack displayItem = this.createItemProgressItem(player, itemProgress, i + 1);
						items.add(new RequirementDetailItem(
								displayItem,
								"Item " + (i + 1),
								itemProgress,
								i
						));
					}
					// No summary - it's redundant with the items shown
				} else {
					// For non-item requirements (experience, playtime, etc.)
					// Show the detailed info item in pagination
					ItemStack summaryItem = this.createGenericSummaryItem(
							player,
							abstractReq,
							requirement,
							context
					);
					items.add(new RequirementDetailItem(
							summaryItem,
							"Details",
							RequirementDetailItemType.SUMMARY
					));
				}

				return items;
			} catch (final Exception exception) {
				LOGGER.log(Level.SEVERE, "Error generating pagination source", exception);
				return null;
			}
		});
	}

	@Override
	protected void renderEntry(
			final @NotNull Context context,
			final @NotNull BukkitItemComponentBuilder builder,
			final int index,
			final @NotNull RequirementDetailItem entry
	) {

		builder.withItem(entry.getDisplayItem()).updateOnClick().onClick(clickContext -> handleDetailItemClick(
				context,
				entry
		));
	}

	@Override
	protected Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext open) {

		try {
			final RRankUpgradeRequirement requirement = this.targetRequirement.get(open);

			if (
					requirement == null
			) {
				return Map.of(
						"requirement_type",
						"UNKNOWN"
				);
			}

			return Map.of(
					"requirement_type",
					this.getRequirementType(requirement),
					"requirement_name",
					this.getRequirementName(requirement)
			);
		} catch (
				final Exception exception
		) {
			LOGGER.log(
					Level.WARNING,
					"Error in getTitlePlaceholders",
					exception
			);
			return Map.of(
					"requirement_type",
					"ERROR"
			);
		}
	}

	@Override
	protected void onPaginatedRender(
			final @NotNull RenderContext render,
			final @NotNull Player player
	) {

		this.progressManager = new RankRequirementProgressManager(this.rdq.get(render));

		try {
			final RRankUpgradeRequirement requirement = this.targetRequirement.get(render);
			if (requirement == null) {
				this.renderErrorState(render, player);
				return;
			}

			final RDQPlayer rdqPlayer = this.currentPlayer.get(render);
			final RankRequirementProgressManager.RequirementProgressData progress = this.progressManager.getRequirementProgress(
					player,
					rdqPlayer,
					requirement
			);

			// Render tips at top center (T slot)
			AbstractRequirement abstractReq = requirement.getRequirement().getRequirement();
			render.layoutSlot('T', this.createRequirementTipsItem(player, abstractReq));

			// Render back button
			render.layoutSlot('b', this.createBackButton(player)).onClick(this::navigateBackToJourneyView);

			// Render submit button (S slot) - only for item requirements and not completed
			if (abstractReq instanceof ItemRequirement itemReq) {
				if (progress.isCompleted()) {
					// Show completed indicator instead of submit button
					render.layoutSlot('S', this.createCompletedIndicator(player));
				} else {
					render.layoutSlot('S', this.createSubmitButton(player, itemReq))
							.onClick(context -> this.handleSubmitClick(context, requirement, itemReq));
				}
			}
		} catch (final Exception exception) {
			LOGGER.log(Level.SEVERE, "Critical error during requirement detail render", exception);
			this.renderErrorState(render, player);
		}
	}

	/**
	 * Creates the back button item.
	 */
	private ItemStack createBackButton(final @NotNull Player player) {
		try {
			return UnifiedBuilderFactory.item(
					new Return().getHead(player)
			).setLore(
					this.i18n("back.lore", player).build().children()
			).build();
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to create back button", exception);
			return UnifiedBuilderFactory.item(Material.ARROW)
					.setName(Component.text("Back"))
					.build();
		}
	}

	/**
	 * Creates the submit items button.
	 */
	private ItemStack createSubmitButton(
			final @NotNull Player player,
			final @NotNull ItemRequirement itemReq
	) {
		boolean canSubmit = itemReq.isMet(player);
		
		Material material = canSubmit ? Material.HOPPER : Material.BARRIER;
		NamedTextColor color = canSubmit ? NamedTextColor.GREEN : NamedTextColor.RED;
		String text = canSubmit ? "Submit Items" : "Missing Items";
		
		List<Component> lore = new ArrayList<>();
		lore.add(Component.empty());
		if (canSubmit) {
			lore.add(Component.text("Click to submit all items").color(NamedTextColor.GRAY));
			lore.add(Component.text("Items will be consumed").color(NamedTextColor.YELLOW));
		} else {
			lore.add(Component.text("Collect all required items first").color(NamedTextColor.GRAY));
		}

		return UnifiedBuilderFactory.item(material)
				.setName(Component.text(text).color(color))
				.setLore(lore)
				.setGlowing(canSubmit)
				.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
				.build();
	}

	/**
	 * Creates a completed indicator item for already completed requirements.
	 */
	private ItemStack createCompletedIndicator(final @NotNull Player player) {
		List<Component> lore = new ArrayList<>();
		lore.add(Component.empty());
		lore.add(Component.text("This requirement has been completed").color(NamedTextColor.GRAY));
		lore.add(Component.text("Click back to return").color(NamedTextColor.YELLOW));

		return UnifiedBuilderFactory.item(Material.LIME_CONCRETE)
				.setName(Component.text("✓ Completed").color(NamedTextColor.GREEN))
				.setLore(lore)
				.setGlowing(true)
				.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
				.build();
	}

	/**
	 * Handles clicking the submit button.
	 */
	private void handleSubmitClick(
			final @NotNull Context context,
			final @NotNull RRankUpgradeRequirement requirement,
			final @NotNull ItemRequirement itemReq
	) {
		final Player player = context.getPlayer();
		final RDQPlayer rdqPlayer = this.currentPlayer.get(context);

		// Check if requirement is already completed first
		final RankRequirementProgressManager.RequirementProgressData currentProgress = 
				this.progressManager.getRequirementProgress(player, rdqPlayer, requirement);
		
		if (currentProgress.isCompleted()) {
			this.i18n("submit.already_completed", player).includePrefix().build().sendMessage();
			this.navigateBackToJourneyView(context);
			return;
		}

		if (!itemReq.isMet(player)) {
			this.i18n("submit.missing_items", player).includePrefix().build().sendMessage();
			return;
		}

		try {
			RankRequirementProgressManager.RequirementCompletionResult result =
					this.progressManager.attemptRequirementCompletion(player, rdqPlayer, requirement);

			if (result.isSuccess()) {
				this.i18n("submit.success", player).includePrefix().build().sendMessage();
				this.navigateBackToJourneyView(context);
			} else {
				// Check if it was already completed (race condition)
				if (result.getUpdatedProgress().isCompleted()) {
					this.i18n("submit.already_completed", player).includePrefix().build().sendMessage();
					this.navigateBackToJourneyView(context);
				} else {
					this.i18n("submit.failed", player).includePrefix().build().sendMessage();
				}
			}
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to submit items", exception);
			this.i18n("submit.error", player).includePrefix().build().sendMessage();
		}
	}
	
	/**
	 * Navigates back to the journey view using openForPlayer to avoid back() navigation issues.
	 */
	private void navigateBackToJourneyView(final @NotNull Context context) {
		try {
			final Map<String, Object> data = new HashMap<>();
			data.put("plugin", this.rdq.get(context));
			data.put("player", this.currentPlayer.get(context));
			data.put("rankTree", this.selectedRankTree.get(context));
			data.put("targetRank", this.targetRank.get(context));
			data.put("previewMode", this.previewMode.get(context));
			
			context.openForPlayer(RankRequirementsJourneyView.class, data);
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to navigate back to journey view, using fallback", exception);
			context.back();
		}
	}

	/**
	 * Creates an item showing progress for a specific required item.
	 * Simplified display with essential information only.
	 */
	private ItemStack createItemProgressItem(
			final @NotNull Player player,
			final @NotNull ItemRequirement.ItemProgress itemProgress,
			final int itemNumber
	) {
		try {
			List<Component> lore = new ArrayList<>();
			
			// Progress bar
			lore.add(Component.empty());
			lore.add(this.createProgressBar(itemProgress.progress(), 12));
			
			// Amount info
			lore.add(Component.empty());
			lore.add(Component.text("Have: ").color(NamedTextColor.GRAY)
					.append(Component.text(itemProgress.currentAmount()).color(
							itemProgress.completed() ? NamedTextColor.GREEN : NamedTextColor.WHITE)));
			lore.add(Component.text("Need: ").color(NamedTextColor.GRAY)
					.append(Component.text(itemProgress.requiredAmount()).color(NamedTextColor.YELLOW)));
			
			// Shortage or completed status
			lore.add(Component.empty());
			if (itemProgress.completed()) {
				lore.add(Component.text("✓ Collected").color(NamedTextColor.GREEN));
			} else {
				lore.add(Component.text("Missing: " + itemProgress.getShortage()).color(NamedTextColor.RED));
			}

			return UnifiedBuilderFactory.item(itemProgress.requiredItem())
					.setLore(lore)
					.setAmount(Math.max(1, Math.min(64, itemProgress.requiredAmount())))
					.setGlowing(itemProgress.completed())
					.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
					.build();
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to create item progress item", exception);
			return this.createFallbackItem(player, "Item " + itemNumber + " (Error)");
		}
	}

	/**
	 * Creates a tips item for item requirements.
	 */
	private ItemStack createItemRequirementTips(
			final @NotNull Player player,
			final @NotNull ItemRequirement itemReq
	) {

		return
				UnifiedBuilderFactory
						.item(Material.KNOWLEDGE_BOOK)
						.setLore(
								this.i18n(
										"item_requirement.tip",
										player
								).build().children()
						)
						.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
						.build();
	}

	/**
	 * Creates a visual progress bar string.
	 */
	private Component createProgressBar(
			double progress,
			int length
	) {

		int filled = (int) (progress * length);
		return
				Component
						.text("")
						.append(
								MiniMessage.miniMessage().deserialize(
										"<green>█</green>".repeat(Math.max(0, filled))
								)
						)
						.append(
								MiniMessage.miniMessage().deserialize(
										"<gray>█</gray>".repeat(Math.max(0, length - filled))
								)
						);
	}

	/**
	 * Creates a summary item for generic requirements.
	 * Enhanced to show specific details for Experience Level and Playtime requirements.
	 */
	private ItemStack createGenericSummaryItem(
			final @NotNull Player player,
			final @NotNull AbstractRequirement abstractReq,
			final @NotNull RRankUpgradeRequirement requirement,
			final @NotNull Context context
	) {

		Material iconMaterial = switch (abstractReq.getTypeId()) {
			case "CURRENCY" -> Material.GOLD_INGOT;
			case "EXPERIENCE_LEVEL" -> Material.EXPERIENCE_BOTTLE;
			case "PLAYTIME" -> Material.CLOCK;
			case "PERMISSION" -> Material.PAPER;
			case "LOCATION" -> Material.COMPASS;
			default -> Material.BOOK;
		};
		final RankRequirementProgressManager.RequirementProgressData progress = this.progressManager.getRequirementProgress(
				player,
				this.currentPlayer.get(context),
				requirement
		);

		List<Component> lore = new ArrayList<>();
		
		// Add type-specific details
		if (abstractReq instanceof ExperienceLevelRequirement expReq) {
			// Experience Level specific display
			int currentLevel = expReq.getCurrentExperience(player);
			int requiredLevel = expReq.getRequiredLevel();
			int levelsNeeded = expReq.getShortage(player);
			
			lore.add(Component.empty());
			lore.add(this.i18n("experience.current_level", player)
					.withPlaceholder("current", currentLevel)
					.build().component());
			lore.add(this.i18n("experience.required_level", player)
					.withPlaceholder("required", requiredLevel)
					.build().component());
			
			if (levelsNeeded > 0) {
				lore.add(this.i18n("experience.levels_needed", player)
						.withPlaceholder("needed", levelsNeeded)
						.build().component());
			} else {
				lore.add(this.i18n("experience.completed", player).build().component());
			}
			
			lore.add(Component.empty());
			// Progress bar - getProgressPercentage() returns 0.0-1.0
			lore.add(this.createProgressBar(progress.getProgressPercentage(), 15));
			lore.add(Component.text(progress.getProgressAsPercentage() + "% Complete").color(
					progress.isCompleted() ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
			
		} else if (abstractReq instanceof PlaytimeRequirement playReq) {
			// Playtime specific display
			long currentSeconds = playReq.getTotalPlaytimeSeconds(player);
			long requiredSeconds = playReq.getRequiredPlaytimeSeconds();
			long remainingSeconds = Math.max(0, requiredSeconds - currentSeconds);
			
			// Convert to hours and minutes
			long currentHours = currentSeconds / 3600;
			long currentMinutes = (currentSeconds % 3600) / 60;
			long requiredHours = requiredSeconds / 3600;
			long requiredMinutes = (requiredSeconds % 3600) / 60;
			long remainingHours = remainingSeconds / 3600;
			long remainingMinutes = (remainingSeconds % 3600) / 60;
			
			lore.add(Component.empty());
			lore.add(this.i18n("playtime.current", player)
					.withPlaceholder("hours", currentHours)
					.withPlaceholder("minutes", currentMinutes)
					.build().component());
			lore.add(this.i18n("playtime.required", player)
					.withPlaceholder("hours", requiredHours)
					.withPlaceholder("minutes", requiredMinutes)
					.build().component());
			
			if (remainingSeconds > 0) {
				lore.add(this.i18n("playtime.remaining", player)
						.withPlaceholder("hours", remainingHours)
						.withPlaceholder("minutes", remainingMinutes)
						.build().component());
			} else {
				lore.add(this.i18n("playtime.completed", player).build().component());
			}
			
			lore.add(Component.empty());
			// Progress bar - getProgressPercentage() returns 0.0-1.0
			lore.add(this.createProgressBar(progress.getProgressPercentage(), 15));
			lore.add(Component.text(progress.getProgressAsPercentage() + "% Complete").color(
					progress.isCompleted() ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
			
		} else {
			// Default generic display
			lore.add(this.i18n("summary.requirement_type", player)
					.withPlaceholder("type", abstractReq.getTypeId())
					.build().component());
			lore.add(Component.empty());
			// Progress bar - getProgressPercentage() returns 0.0-1.0
			lore.add(this.createProgressBar(progress.getProgressPercentage(), 15));
			lore.add(this.i18n("summary.progress_percentage", player)
					.withPlaceholder("completion_progress_percentage", progress.getProgressAsPercentage() + "%")
					.build().component());
			lore.add(Component.empty());
			lore.add(this.i18n("summary.completion_status", player)
					.withPlaceholder("completion_status", progress.getStatus().name())
					.build().component());
		}

		return UnifiedBuilderFactory.item(iconMaterial)
				.setName(this.i18n("summary.requirement_summary.name", player).build().component())
				.setLore(lore)
				.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_STORED_ENCHANTS)
				.setGlowing(progress.isCompleted())
				.build();
	}

	/**
	 * Creates a tips item for the requirement.
	 */
	private ItemStack createRequirementTipsItem(
			final @NotNull Player player,
			final @NotNull AbstractRequirement abstractReq
	) {

		List<Component> lore = new ArrayList<>();

		switch (abstractReq.getTypeId()) {
			case "CURRENCY" -> {
				lore.addAll(
						this.i18n(
								"requirement_tip.currency",
								player
						).build().children()
				);
			}
			case "EXPERIENCE_LEVEL" -> {
				lore.addAll(
						this.i18n(
								"requirement_tip.experience_level",
								player
						).build().children()
				);
			}
			case "PLAYTIME" -> {
				lore.addAll(
						this.i18n(
								"requirement_tip.playtime",
								player
						).build().children()
				);
			}
			default -> {
				lore.addAll(
						this.i18n(
								"requirement_tip.other",
								player
						).build().children()
				);
			}
		}

		return UnifiedBuilderFactory.item(
				Material.KNOWLEDGE_BOOK
		).setName(
				this.i18n(
						"summary.requirement_tip.name",
						player
				).build().component()
		).setLore(
				lore
		).addItemFlags(
				ItemFlag.HIDE_ATTRIBUTES,
				ItemFlag.HIDE_ENCHANTS,
				ItemFlag.HIDE_STORED_ENCHANTS
		).build();
	}

	/**
	 * Handles clicking on detail items in the pagination.
	 */
	private void handleDetailItemClick(
			final @NotNull Context context,
			final @NotNull RequirementDetailItem detailItem
	) {

		final Player player = context.getPlayer();

		try {
			switch (detailItem.getType()) {
				case ITEM_PROGRESS -> {
					if (detailItem.getItemProgress() != null) {
						handleItemProgressClick(
								context,
								detailItem.getItemProgress()
						);
					}
				}
				case SUMMARY -> {
					this.i18n(
									"summary_clicked",
									player
							)
							.includePrefix()
							.build().sendMessage();
				}
				case TIPS -> {
					this.i18n(
									"tips_clicked",
									player
							)
							.includePrefix()
							.build().sendMessage();
				}
				default -> {
				}
			}
		} catch (Exception e) {
			LOGGER.log(
					Level.WARNING,
					"Failed to handle detail item click",
					e
			);
		}
	}

	/**
	 * Handles clicking on individual item progress items.
	 */
	private void handleItemProgressClick(
			final @NotNull Context context,
			final @NotNull ItemRequirement.ItemProgress itemProgress
	) {
		// Item progress click now just refreshes the view - details are shown in the UI
		context.update();
	}

	/**
	 * Enhanced requirement info item with more details.
	 */
	private ItemStack createRequirementInfoItem(
			final @NotNull Player player,
			final @NotNull RRankUpgradeRequirement requirement,
			final @NotNull RankRequirementProgressManager.RequirementProgressData progress
	) {

		try {
			Material iconMaterial = Material.valueOf(requirement.getIcon().getMaterial());

			List<Component> lore = new ArrayList<>(
					List.of(
							this.i18n(
									"requirement_info.requirement_type",
									player
							).withPlaceholder("type",
									requirement.getRequirement().getRequirement().getTypeId()
							).build().component(),
							Component.empty(),
							this.createProgressBar(
									progress.getProgressPercentage() / 100,
									20
							),
							this.i18n(
									"requirement_info.progress_percentage",
									player
							).withPlaceholder("completion_progress_percentage",
									progress.getProgressAsPercentage() + "%"
							).build().component(),
							Component.empty(),
							this.i18n(
									"requirement_info.completion_status",
									player
							).withPlaceholder("completion_status",
									progress.getStatus().name()
							).build().component()
					)
			);

			AbstractRequirement abstractReq = requirement.getRequirement().getRequirement();
			if (
					abstractReq instanceof ItemRequirement itemReq
			) {
				List<ItemStack> missingItems = itemReq.getMissingItems(player);
				if (
						! missingItems.isEmpty()
				) {
					lore.addAll(
							List.of(
									Component.empty(),
									this.i18n("requirement_info.missing_items.item", player).build().component()
							)
					);

					// Dynamically build the missing items list instead of using fixed placeholders
					int itemCount = Math.min(3, missingItems.size());
					for (int i = 0; i < itemCount; i++) {
						ItemStack missingItem = missingItems.get(i);
						String itemInfo = "• " + missingItem.getAmount() + "x " + missingItem.getType().name();
						lore.add(this.i18n("requirement_info.missing_items.entry", player)
								.withPlaceholder("item_info", itemInfo)
								.build().component());
					}

					if (
							missingItems.size() > 3
					) {
						lore.add(this.i18n("requirement_info.missing_items.more", player).withPlaceholder("item_amount", missingItems.size() - 3).build().component());
					}
				}
			}

			lore.addAll(
					List.of(
							Component.empty(),
							this.i18n("requirement_info.detailed_information", player).build().component()
					)
			);

			return UnifiedBuilderFactory
					.item(iconMaterial)
					.setName(
							this.i18n("requirement_info.name", player).withPlaceholder("requirement_name", this.getRequirementName(requirement)).build().component()
					)
					.setLore(lore)
					.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
					.setGlowing(progress.isCompleted())
					.build();
		} catch (
				final Exception exception
		) {
			LOGGER.log(
					Level.WARNING,
					"Failed to create requirement info item",
					exception
			);
			return this.createFallbackItem(
					player,
					"Requirement Info"
			);
		}
	}

	/**
	 * Enhanced status item showing current state.
	 */
	private ItemStack createStatusItem(
			final @NotNull Player player,
			final @NotNull RRankUpgradeRequirement requirement,
			final @NotNull RankRequirementProgressManager.RequirementProgressData progress
	) {

		Material statusMaterial = switch (progress.getStatus()) {
			case COMPLETED -> Material.EMERALD_BLOCK;
			case READY_TO_COMPLETE -> Material.GOLD_BLOCK;
			case IN_PROGRESS -> Material.IRON_BLOCK;
			case NOT_STARTED -> Material.REDSTONE_BLOCK;
			case ERROR -> Material.BARRIER;
		};

		List<Component> lore = new ArrayList<>();

		switch (progress.getStatus()) {
			case COMPLETED -> {
				lore.addAll(
						this.i18n("requirement.completed", player).build().children()
				);
			}
			case READY_TO_COMPLETE -> {
				lore.addAll(
						this.i18n("requirement.ready_to_complete", player).build().children()
				);
			}
			case IN_PROGRESS -> {
				lore.addAll(
						this.i18n("requirement.in_progress", player).build().children()
				);
			}
			case NOT_STARTED -> {
				lore.addAll(
						this.i18n("requirement.not_started", player).build().children()
				);
			}
			case ERROR -> {
				lore.addAll(
						this.i18n("requirement.error", player).build().children()
				);
			}
		}

		return UnifiedBuilderFactory.item(statusMaterial)
				.setName(
						this.i18n("requirement.status_name", player).withPlaceholder("requirement_status", progress.getStatus().name()).build().component()
				)
				.setLore(lore)
				.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
				.setGlowing(progress.getStatus() == RankRequirementProgressManager.RequirementStatus.READY_TO_COMPLETE)
				.build();
	}

	/**
	 * Gets the appropriate color for a requirement status.
	 */
	private NamedTextColor getStatusColor(RankRequirementProgressManager.RequirementStatus status) {

		return switch (status) {
			case COMPLETED -> NamedTextColor.GREEN;
			case READY_TO_COMPLETE -> NamedTextColor.GOLD;
			case IN_PROGRESS -> NamedTextColor.YELLOW;
			case NOT_STARTED -> NamedTextColor.RED;
			case ERROR -> NamedTextColor.DARK_RED;
		};
	}

	private ItemStack createProgressItem(
			final @NotNull Player player,
			final @NotNull RRankUpgradeRequirement requirement,
			final @NotNull RankRequirementProgressManager.RequirementProgressData progress
	) {

		try {
			Material progressMaterial = switch (progress.getStatus()) {
				case COMPLETED -> Material.LIME_CONCRETE;
				case READY_TO_COMPLETE -> Material.YELLOW_CONCRETE;
				case IN_PROGRESS -> Material.ORANGE_CONCRETE;
				case NOT_STARTED -> Material.RED_CONCRETE;
				case ERROR -> Material.GRAY_CONCRETE;
			};

			List<Component> lore = new ArrayList<>(
					List.of(
							Component.empty(),
							this.i18n("progress_item", player).build().component(),
							this.createProgressBar(progress.getProgressPercentage() / 100.0, 15),
							this.i18n("progress_item_completion", player).withPlaceholder("progress_item_percentage", progress.getProgressAsPercentage()).build().component(),
							Component.empty(),
							this.i18n("progress_item_status", player).withPlaceholder("progress_item_status", progress.getStatus().name()).build().component(),
							Component.empty(),
							this.i18n("progress_click_to_refresh", player).build().component()
					)
			);

			return UnifiedBuilderFactory.item(progressMaterial)
					.setName(
							this.i18n("progress_overview", player).build().component()
					)
					.setLore(lore)
					.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
					.setGlowing(progress.getStatus() == RankRequirementProgressManager.RequirementStatus.COMPLETED)
					.build();
		} catch (
				final Exception exception
		) {
			LOGGER.log(
					Level.WARNING,
					"Failed to create progress item",
					exception
			);
			return this.createFallbackItem(
					player,
					"Progress Unknown"
			);
		}
	}

	private ItemStack createActionButton(
			final @NotNull Player player,
			final @NotNull RRankUpgradeRequirement requirement,
			final @NotNull RankRequirementProgressManager.RequirementProgressData progress
	) {

		try {
			boolean canComplete = progress.getStatus() == RankRequirementProgressManager.RequirementStatus.READY_TO_COMPLETE;
			boolean isCompleted = progress.getStatus() == RankRequirementProgressManager.RequirementStatus.COMPLETED;

			Material buttonMaterial;
			String   nameKey;
			String   loreKey;

			if (isCompleted) {
				buttonMaterial = Material.EMERALD_BLOCK;
				nameKey = "action_button.completed_name";
				loreKey = "action_button.completed_lore";
			} else if (canComplete) {
				buttonMaterial = Material.EMERALD;
				nameKey = "action_button.complete_name";
				loreKey = "action_button.complete_lore";
			} else {
				buttonMaterial = Material.DIAMOND;
				nameKey = "action_button.refresh_name";
				loreKey = "action_button.refresh_lore";
			}

			return UnifiedBuilderFactory.item(buttonMaterial)
					.setName(this.i18n(
							nameKey,
							player
					).build().component())
					.setLore(this.i18n(
							loreKey,
							player
					).build().children())
					.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
					.setGlowing(canComplete)
					.build();
		} catch (
				final Exception exception
		) {
			LOGGER.log(
					Level.WARNING,
					"Failed to create action button",
					exception
			);
			return this.createFallbackItem(
					player,
					"Action (Error)"
			);
		}
	}

	private void handleInfoClick(
			final @NotNull Context context
	) {
		// Info click now just refreshes the view - detailed info is shown in the UI
		context.update();
	}

	private void handleProgressClick(
			final @NotNull Context context,
			final @NotNull RRankUpgradeRequirement requirement
	) {
		// Progress click now just refreshes the view - progress is shown in the UI
		context.update();
	}

	private void handleStatusClick(
			final @NotNull Context context,
			final @NotNull RRankUpgradeRequirement requirement
	) {
		final Player player = context.getPlayer();
		final RDQPlayer rdqPlayer = this.currentPlayer.get(context);

		try {
			this.progressManager.refreshRequirementProgress(player, rdqPlayer, requirement);
			context.update();
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to refresh requirement progress", exception);
		}
	}

	private void handleActionClick(
			final @NotNull Context context,
			final @NotNull RRankUpgradeRequirement requirement,
			final @NotNull RankRequirementProgressManager.RequirementProgressData currentProgress
	) {

		final Player  player  = context.getPlayer();
		final RDQPlayer rdqPlayer = this.currentPlayer.get(context);

		try {
			if (
					currentProgress.getStatus() == RankRequirementProgressManager.RequirementStatus.COMPLETED
			) {
				this.i18n(
								"already_completed",
								player
						)
						.withPlaceholder("requirement_name",
								this.getRequirementName(requirement)
						)
						.includePrefix()
						.build().sendMessage();
				return;
			}

			if (
					currentProgress.getStatus() == RankRequirementProgressManager.RequirementStatus.READY_TO_COMPLETE
			) {
				RankRequirementProgressManager.RequirementCompletionResult result =
						this.progressManager.attemptRequirementCompletion(
								player,
								rdqPlayer,
								requirement
						);

				if (result.isSuccess()) {
					context.update();
				}
			} else {
				this.progressManager.refreshRankProgress(
						player,
						rdqPlayer,
						requirement.getRank()
				);

				this.i18n(
								"progress_refreshed",
								player
						)
						.includePrefix()
						.build().sendMessage();

				context.update();
			}
		} catch (
				final Exception exception
		) {
			LOGGER.log(
					Level.WARNING,
					"Failed to handle action click",
					exception
			);
			this.i18n(
							"action_error",
							player
					)
					.includePrefix()
					.build().sendMessage();
		}
	}



	private String getRequirementType(
			final @NotNull RRankUpgradeRequirement requirement
	) {

		try {
			return requirement.getRequirement().getRequirement().getTypeId();
		} catch (
				final Exception exception
		) {
			return "UNKNOWN";
		}
	}

	private String getRequirementName(
			final @NotNull RRankUpgradeRequirement requirement
	) {

		try {
			return requirement.getRequirement().getRequirement().getTypeId();
		} catch (
				final Exception exception
		) {
			return "Unknown Requirement";
		}
	}

	private String getRequirementDescription(
			final @NotNull RRankUpgradeRequirement requirement
	) {

		try {
			return requirement.getRequirement().getRequirement().getDescriptionKey();
		} catch (final Exception exception) {
			return "A requirement for rank progression";
		}
	}

	private ItemStack createFallbackItem(
			final @NotNull Player player,
			final @NotNull String name
	) {

		return UnifiedBuilderFactory.item(Material.PAPER)
				.setName(this.i18n("fallback.name", player).build().component())
				.setLore(this.i18n("fallback.lore", player).build().children())
				.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
				.build();
	}

	private void renderErrorState(
			final @NotNull RenderContext render,
			final @NotNull Player player
	) {

		try {
			render.slot(
					22,
					UnifiedBuilderFactory.item(Material.BARRIER)
							.setName(this.i18n("error.name", player).build().component())
							.setLore(this.i18n("error.lore", player).build().children())
							.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
							.build()
			);

			LOGGER.log(
					Level.WARNING,
					"Rendering error state for requirement detail view"
			);
		} catch (
				final Exception exception
		) {
			LOGGER.log(
					Level.SEVERE,
					"Failed to render error state",
					exception
			);
		}
	}

}
