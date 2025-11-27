package com.raindropcentral.rdq.view.rank.view;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import com.raindropcentral.rdq.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rdq.manager.rank.RankRequirementProgressManager;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import com.raindropcentral.rdq.requirement.ItemRequirement;
import com.raindropcentral.rdq.view.rank.RequirementCompletionResult;
import com.raindropcentral.rdq.view.rank.RequirementProgressData;
import com.raindropcentral.rdq.view.rank.RequirementStatus;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
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

public final class RankRequirementDetailView extends APaginatedView<RankRequirementDetailView.RequirementDetailItem> {

	private static final Logger LOGGER = CentralLogger.getLogger(RankRequirementDetailView.class.getName());

	private final State<RDQ>       rdq           = initialState("plugin");
	private final State<RDQPlayer> currentPlayer = initialState("player");
	private final State<RRankTree>               selectedRankTree  = initialState("rankTree");
	private final State<RRankUpgradeRequirement> targetRequirement = initialState("requirement");

	private RankRequirementProgressManager progressManager;

	public static final class RequirementDetailItem {
		private final ItemStack displayItem;
		private final String itemName;
		private final ItemRequirement.ItemProgress itemProgress;
		private final int itemIndex;
		private final DetailItemType type;

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
			this.type = DetailItemType.ITEM_PROGRESS;
		}

		public RequirementDetailItem(
			final @NotNull ItemStack displayItem,
			final @NotNull String itemName,
			final @NotNull DetailItemType type
		) {
			this.displayItem = displayItem;
			this.itemName = itemName;
			this.itemProgress = null;
			this.itemIndex = -1;
			this.type = type;
		}

		public ItemStack getDisplayItem() {
			return displayItem;
		}

		public String getItemName() {
			return itemName;
		}

		public ItemRequirement.ItemProgress getItemProgress() {
			return itemProgress;
		}

		public int getItemIndex() {
			return itemIndex;
		}

		public DetailItemType getType() {
			return type;
		}
	}

	public enum DetailItemType {
		ITEM_PROGRESS, SUMMARY, TIPS, FILLER
	}

	public RankRequirementDetailView() {
		super(RankPathRankRequirementOverview.class);
	}

	@Override
	protected String getKey() {
		return "rank_requirement_detail_ui";
	}

	@Override
	protected String[] getLayout() {
		return new String[] {
			"         ",
			"    i    ",
			"  t s a  ",
			" O O O O ",
			" O O O O ",
			"b  <p>   "
		};
	}

	@Override
	protected CompletableFuture<List<RequirementDetailItem>> getAsyncPaginationSource(
		final @NotNull Context context
	) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				final RRankUpgradeRequirement requirement = targetRequirement.get(context);
				final Player player = context.getPlayer();

				if (requirement == null) {
					return null;
				}

				final List<RequirementDetailItem> items = new ArrayList<>();
				final AbstractRequirement abstractReq = requirement.getRequirement().getRequirement();

				if (abstractReq instanceof ItemRequirement itemReq) {
					final List<ItemRequirement.ItemProgress> itemProgressList = itemReq.getDetailedProgress(player);

					for (int i = 0; i < itemProgressList.size(); i++) {
						final ItemRequirement.ItemProgress itemProgress = itemProgressList.get(i);
						final ItemStack displayItem = createItemProgressItem(player, itemProgress, i + 1);

						items.add(new RequirementDetailItem(displayItem, "Item " + (i + 1), itemProgress, i));
					}

					if (itemProgressList.size() > 1) {
						final ItemStack summaryItem = createItemRequirementSummary(player, itemReq);
						items.add(new RequirementDetailItem(summaryItem, "Summary", DetailItemType.SUMMARY));
					}

					final ItemStack tipsItem = createItemRequirementTips(player, itemReq);
					items.add(new RequirementDetailItem(tipsItem, "Tips", DetailItemType.TIPS));
				} else {
					final ItemStack summaryItem = createGenericSummaryItem(player, abstractReq, requirement, context);
					items.add(new RequirementDetailItem(summaryItem, "Summary", DetailItemType.SUMMARY));

					final ItemStack tipsItem = createRequirementTipsItem(player, abstractReq);
					items.add(new RequirementDetailItem(tipsItem, "Tips", DetailItemType.TIPS));
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
		builder.withItem(entry.getDisplayItem())
			.updateOnClick()
			.onClick(clickContext -> handleDetailItemClick(context, entry));
	}

	@Override
	protected Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext open) {
		try {
			final RRankUpgradeRequirement requirement = targetRequirement.get(open);

			if (requirement == null) {
				return Map.of("requirement_type", "UNKNOWN");
			}

			return Map.of(
				"requirement_type", getRequirementType(requirement),
				"requirement_name", getRequirementName(requirement)
			);
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Error in getTitlePlaceholders", exception);
			return Map.of("requirement_type", "ERROR");
		}
	}

	@Override
	protected void onPaginatedRender(
		final @NotNull RenderContext render,
		final @NotNull Player player
	) {
		progressManager = new RankRequirementProgressManager(rdq.get(render));

		try {
			final RRankUpgradeRequirement requirement = targetRequirement.get(render);

			if (requirement == null) {
				renderErrorState(render, player);
				return;
			}

			final RDQPlayer rdqPlayer = currentPlayer.get(render);
			final RequirementProgressData progress = progressManager.getRequirementProgress(player, rdqPlayer, requirement);

			render.layoutSlot('i', createRequirementInfoItem(player, requirement, progress))
				.onClick(this::handleInfoClick);

			render.layoutSlot('t', createProgressItem(player, requirement, progress))
				.onClick(context -> handleProgressClick(context, requirement));

			render.layoutSlot('s', createStatusItem(player, requirement, progress))
				.onClick(context -> handleStatusClick(context, requirement));

			render.layoutSlot('a', createActionButton(player, requirement, progress))
				.onClick(context -> handleActionClick(context, requirement, progress));
		} catch (final Exception exception) {
			LOGGER.log(Level.SEVERE, "Critical error during requirement detail render", exception);
			renderErrorState(render, player);
		}
	}

	private @NotNull ItemStack createItemProgressItem(
		final @NotNull Player player,
		final @NotNull ItemRequirement.ItemProgress itemProgress,
		final int itemNumber
	) {
		try {
			final List<Component> lore = new ArrayList<>(List.of(
				Component.empty(),
				i18n("progress_text", player).build().component(),
				createProgressBar(itemProgress.progress(), 10),
				Component.empty(),
				i18n("current_progress", player)
					.withAll(Map.of(
						"current_amount", itemProgress.currentAmount(),
						"required_amount", itemProgress.requiredAmount()
					))
					.build().component(),
				Component.empty(),
				i18n("current_progress_percentage", player)
					.with("progress_percentage", itemProgress.getProgressPercentage())
					.build().component()
			));

			if (!itemProgress.completed()) {
				lore.addAll(List.of(
					Component.empty(),
					i18n("required_amount_left", player)
						.with("required_amount", itemProgress.getShortage())
						.build().component()
				));
			}

			lore.add(Component.empty());

			if (itemProgress.completed()) {
				lore.add(i18n("progress_completed", player).build().component());
			} else if (itemProgress.currentAmount() > 0) {
				lore.add(i18n("in_progress", player).build().component());
			} else {
				lore.add(i18n("progress_not_started", player).build().component());
			}

			lore.addAll(List.of(
				Component.empty(),
				i18n("progress_details", player)
					.with("item_number", itemNumber)
					.build().component()
			));

			return UnifiedBuilderFactory.item(itemProgress.requiredItem())
				.setLore(lore)
				.setAmount(Math.max(1, Math.min(64, itemProgress.requiredAmount())))
				.setGlowing(itemProgress.completed())
				.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_STORED_ENCHANTS, ItemFlag.HIDE_DESTROYS)
				.build();
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to create item progress item", exception);
			return createFallbackItem(player, "Item " + itemNumber + " (Error)");
		}
	}

	private @NotNull ItemStack createItemRequirementSummary(
		final @NotNull Player player,
		final @NotNull ItemRequirement itemReq
	) {
		final List<ItemRequirement.ItemProgress> itemProgressList = itemReq.getDetailedProgress(player);

		int completedItems = 0;
		int totalItems = itemProgressList.size();
		double overallProgress = 0.0;

		for (final ItemRequirement.ItemProgress progress : itemProgressList) {
			if (progress.completed()) {
				completedItems++;
			}
			overallProgress += progress.progress();
		}

		overallProgress = overallProgress / totalItems;

		final List<Component> lore = new ArrayList<>(List.of(
			Component.empty(),
			i18n("summary.items_completed", player)
				.withAll(Map.of(
					"completed_item_amount", completedItems,
					"total_item_amount", totalItems
				))
				.build().component(),
			Component.empty(),
			createProgressBar(overallProgress, 15),
			Component.empty(),
			i18n("current_progress_percentage", player)
				.with("progress_percentage", String.format("%.1f", overallProgress * 100) + "%")
				.build().component(),
			i18n("current_progress_status", player)
				.with("progress_status", overallProgress >= 1.0 ? "COMPLETED" : "IN PROGRESS")
				.build().component()
		));

		return UnifiedBuilderFactory.item(Material.CHEST)
			.setName(i18n("current_progress_summary", player).build().component())
			.setLore(lore)
			.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE)
			.setGlowing(overallProgress >= 1.0)
			.build();
	}

	private @NotNull ItemStack createItemRequirementTips(
		final @NotNull Player player,
		final @NotNull ItemRequirement itemReq
	) {
		return UnifiedBuilderFactory.item(Material.KNOWLEDGE_BOOK)
			.setLore(i18n("item_requirement.tip", player).build().splitLines())
			.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			.build();
	}

	private @NotNull Component createProgressBar(final double progress, final int length) {
		final int filled = (int) (progress * length);

		return Component.text("")
			.append(MiniMessage.miniMessage().deserialize("<green>█</green>".repeat(Math.max(0, filled))))
			.append(MiniMessage.miniMessage().deserialize("<gray>█</gray>".repeat(Math.max(0, length - filled))));
	}

	private @NotNull ItemStack createGenericSummaryItem(
		final @NotNull Player player,
		final @NotNull AbstractRequirement abstractReq,
		final @NotNull RRankUpgradeRequirement requirement,
		final @NotNull Context context
	) {
		final Material iconMaterial = switch (abstractReq.getType()) {
			case CURRENCY -> Material.GOLD_INGOT;
			case EXPERIENCE_LEVEL -> Material.EXPERIENCE_BOTTLE;
			case PLAYTIME -> Material.CLOCK;
			case PERMISSION -> Material.PAPER;
			case LOCATION -> Material.COMPASS;
			default -> Material.BOOK;
		};

		final RequirementProgressData progress = progressManager.getRequirementProgress(
			player,
			currentPlayer.get(context),
			requirement
		);

		final List<Component> lore = new ArrayList<>(List.of(
			i18n("summary.requirement_type", player)
				.with("type", abstractReq.getType())
				.build().component(),
			Component.empty(),
			createProgressBar(progress.getProgressPercentage() / 100, 15),
			i18n("summary.progress_percentage", player)
				.with("completion_progress_percentage", progress.getProgressAsPercentage() + "%")
				.build().component(),
			Component.empty(),
			i18n("summary.completion_status", player)
				.with("completion_status", progress.getStatus().name())
				.build().component()
		));

		return UnifiedBuilderFactory.item(iconMaterial)
			.setName(i18n("summary.requirement_summary.name", player).build().component())
			.setLore(lore)
			.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_STORED_ENCHANTS)
			.setGlowing(progress.isCompleted())
			.build();
	}

	private @NotNull ItemStack createRequirementTipsItem(
		final @NotNull Player player,
		final @NotNull AbstractRequirement abstractReq
	) {
		final List<Component> lore = new ArrayList<>();

		switch (abstractReq.getType()) {
			case CURRENCY -> {
				lore.addAll(i18n("requirement_tip.currency", player).build().splitLines());
			}
			case EXPERIENCE_LEVEL -> {
				lore.addAll(i18n("requirement_tip.experience_level", player).build().splitLines());
			}
			case PLAYTIME -> {
				lore.addAll(i18n("requirement_tip.playtime", player).build().splitLines());
			}
			default -> {
				lore.addAll(i18n("requirement_tip.other", player).build().splitLines());
			}
		}

		return UnifiedBuilderFactory.item(Material.KNOWLEDGE_BOOK)
			.setName(i18n("summary.requirement_tip.name", player).build().component())
			.setLore(lore)
			.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_STORED_ENCHANTS)
			.build();
	}

	private void handleDetailItemClick(
		final @NotNull Context context,
		final @NotNull RequirementDetailItem detailItem
	) {
		final Player player = context.getPlayer();

		try {
			switch (detailItem.getType()) {
				case ITEM_PROGRESS -> {
					if (detailItem.getItemProgress() != null) {
						handleItemProgressClick(context, detailItem.getItemProgress());
					}
				}
				case SUMMARY -> {
					i18n("summary_clicked", player).withPrefix().send();
				}
				case TIPS -> {
					i18n("tips_clicked", player).withPrefix().send();
				}
				default -> {
				}
			}
		} catch (final Exception e) {
			LOGGER.log(Level.WARNING, "Failed to handle detail item click", e);
		}
	}

	private void handleItemProgressClick(
		final @NotNull Context context,
		final @NotNull ItemRequirement.ItemProgress itemProgress
	) {
		final Player player = context.getPlayer();

		try {
			i18n("item_detail.header", player)
				.with("item_name", itemProgress.requiredItem().getType().name())
				.with("item_number", itemProgress.index() + 1)
				.withPrefix()
				.send();

			i18n("item_detail.progress", player)
				.with("current", itemProgress.currentAmount())
				.with("required", itemProgress.requiredAmount())
				.with("percentage", itemProgress.getProgressPercentage())
				.withPrefix()
				.send();

			if (!itemProgress.completed()) {
				i18n("item_detail.missing", player)
					.with("shortage", itemProgress.getShortage())
					.withPrefix()
					.send();
			}
		} catch (final Exception e) {
			LOGGER.log(Level.WARNING, "Failed to handle item progress click", e);
		}
	}

	private @NotNull ItemStack createRequirementInfoItem(
		final @NotNull Player player,
		final @NotNull RRankUpgradeRequirement requirement,
		final @NotNull RequirementProgressData progress
	) {
		try {
			final Material iconMaterial = Material.valueOf(requirement.getIcon().getMaterial());

			final List<Component> lore = new ArrayList<>(List.of(
				i18n("requirement_info.requirement_type", player)
					.with("type", requirement.getRequirement().getRequirement().getType().name())
					.build().component(),
				Component.empty(),
				createProgressBar(progress.getProgressPercentage() / 100, 20),
				i18n("requirement_info.progress_percentage", player)
					.with("completion_progress_percentage", progress.getProgressAsPercentage() + "%")
					.build().component(),
				Component.empty(),
				i18n("requirement_info.completion_status", player)
					.with("completion_status", progress.getStatus().name())
					.build().component()
			));

			final AbstractRequirement abstractReq = requirement.getRequirement().getRequirement();

			if (abstractReq instanceof ItemRequirement itemReq) {
				final List<ItemStack> missingItems = itemReq.getMissingItems(player);

				if (!missingItems.isEmpty()) {
					lore.addAll(List.of(
						Component.empty(),
						i18n("requirement_info.missing_items.item", player).build().component()
					));

					final Map<String, Object> placeholder = new HashMap<>();
					for (int i = 0; i < Math.min(3, missingItems.size()); i++) {
						final ItemStack missingItem = missingItems.get(i);
						placeholder.put("item_" + i, "• " + missingItem.getAmount() + "x " + missingItem.getType().name());
					}

					lore.addAll(i18n("requirement_info.missing_items.items", player).withAll(placeholder).build().splitLines());

					if (missingItems.size() > 3) {
						lore.add(i18n("requirement_info.missing_items.more", player)
							.with("item_amount", missingItems.size() - 3)
							.build().component());
					}
				}
			}

			lore.addAll(List.of(
				Component.empty(),
				i18n("requirement_info.detailed_information", player).build().component()
			));

			return UnifiedBuilderFactory.item(iconMaterial)
				.setName(i18n("requirement_info.name", player)
					.with("requirement_name", getRequirementName(requirement))
					.build().component())
				.setLore(lore)
				.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
				.setGlowing(progress.isCompleted())
				.build();
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to create requirement info item", exception);
			return createFallbackItem(player, "Requirement Info");
		}
	}

	private @NotNull ItemStack createStatusItem(
		final @NotNull Player player,
		final @NotNull RRankUpgradeRequirement requirement,
		final @NotNull RequirementProgressData progress
	) {
		final Material statusMaterial = switch (progress.getStatus()) {
			case COMPLETED -> Material.EMERALD_BLOCK;
			case READY_TO_COMPLETE -> Material.GOLD_BLOCK;
			case IN_PROGRESS -> Material.IRON_BLOCK;
			case NOT_STARTED -> Material.REDSTONE_BLOCK;
			case ERROR -> Material.BARRIER;
		};

		final List<Component> lore = new ArrayList<>();

		switch (progress.getStatus()) {
			case COMPLETED -> {
				lore.addAll(i18n("requirement.completed", player).build().splitLines());
			}
			case READY_TO_COMPLETE -> {
				lore.addAll(i18n("requirement.ready_to_complete", player).build().splitLines());
			}
			case IN_PROGRESS -> {
				lore.addAll(i18n("requirement.in_progress", player).build().splitLines());
			}
			case NOT_STARTED -> {
				lore.addAll(i18n("requirement.not_started", player).build().splitLines());
			}
			case ERROR -> {
				lore.addAll(i18n("requirement.error", player).build().splitLines());
			}
		}

		return UnifiedBuilderFactory.item(statusMaterial)
			.setName(i18n("requirement.status_name", player)
				.with("requirement_status", progress.getStatus().name())
				.build().component())
			.setLore(lore)
			.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			.setGlowing(progress.getStatus() == RequirementStatus.READY_TO_COMPLETE)
			.build();
	}

	private @NotNull ItemStack createProgressItem(
		final @NotNull Player player,
		final @NotNull RRankUpgradeRequirement requirement,
		final @NotNull RequirementProgressData progress
	) {
		try {
			final Material progressMaterial = switch (progress.getStatus()) {
				case COMPLETED -> Material.LIME_CONCRETE;
				case READY_TO_COMPLETE -> Material.YELLOW_CONCRETE;
				case IN_PROGRESS -> Material.ORANGE_CONCRETE;
				case NOT_STARTED -> Material.RED_CONCRETE;
				case ERROR -> Material.GRAY_CONCRETE;
			};

			final List<Component> lore = new ArrayList<>(List.of(
				Component.empty(),
				i18n("progress_item", player).build().component(),
				createProgressBar(progress.getProgressPercentage() / 100.0, 15),
				i18n("progress_item_completion", player)
					.with("progress_item_percentage", progress.getProgressAsPercentage())
					.build().component(),
				Component.empty(),
				i18n("progress_item_status", player)
					.with("progress_item_status", progress.getStatus().name())
					.build().component(),
				Component.empty(),
				i18n("progress_click_to_refresh", player).build().component()
			));

			return UnifiedBuilderFactory.item(progressMaterial)
				.setName(i18n("progress_overview", player).build().component())
				.setLore(lore)
				.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
				.setGlowing(progress.getStatus() == RequirementStatus.COMPLETED)
				.build();
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to create progress item", exception);
			return createFallbackItem(player, "Progress Unknown");
		}
	}

	private @NotNull ItemStack createActionButton(
		final @NotNull Player player,
		final @NotNull RRankUpgradeRequirement requirement,
		final @NotNull RequirementProgressData progress
	) {
		try {
			final boolean canComplete = progress.getStatus() == RequirementStatus.READY_TO_COMPLETE;
			final boolean isCompleted = progress.getStatus() == RequirementStatus.COMPLETED;

			final Material buttonMaterial;
			final String nameKey;
			final String loreKey;

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
				.setName(i18n(nameKey, player).build().component())
				.setLore(i18n(loreKey, player).build().splitLines())
				.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
				.setGlowing(canComplete)
				.build();
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to create action button", exception);
			return createFallbackItem(player, "Action (Error)");
		}
	}

	private void handleInfoClick(final @NotNull Context context) {
		final Player player = context.getPlayer();
		final RRankUpgradeRequirement requirement = targetRequirement.get(context);

		try {
			sendDetailedRequirementInfo(player, requirement);
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to handle info click", exception);
		}
	}

	private void handleProgressClick(
		final @NotNull Context context,
		final @NotNull RRankUpgradeRequirement requirement
	) {
		final Player player = context.getPlayer();
		final RDQPlayer rdqPlayer = currentPlayer.get(context);

		try {
			final RequirementProgressData progress = progressManager.getRequirementProgress(player, rdqPlayer, requirement);

			i18n("progress_info", player)
				.with("progress", progress.getProgressAsPercentage())
				.with("formatted_progress", progress.getFormattedProgress())
				.with("status", progress.getStatus().name().toLowerCase())
				.withPrefix()
				.send();

			context.update();
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to handle progress click", exception);
		}
	}

	private void handleStatusClick(
		final @NotNull Context context,
		final @NotNull RRankUpgradeRequirement requirement
	) {
		final Player player = context.getPlayer();
		final RDQPlayer rdqPlayer = currentPlayer.get(context);

		try {
			progressManager.refreshRequirementProgress(player, rdqPlayer, requirement);

			final RequirementProgressData updatedProgress = progressManager.getRequirementProgress(player, rdqPlayer, requirement);

			i18n("status_update", player)
				.with("status", updatedProgress.getStatus().name())
				.with("progress", updatedProgress.getProgressAsPercentage())
				.withPrefix()
				.send();

			context.update();
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to handle status click", exception);
		}
	}

	private void handleActionClick(
		final @NotNull Context context,
		final @NotNull RRankUpgradeRequirement requirement,
		final @NotNull RequirementProgressData currentProgress
	) {
		final Player player = context.getPlayer();
		final RDQPlayer rdqPlayer = currentPlayer.get(context);

		try {
			if (currentProgress.getStatus() == RequirementStatus.COMPLETED) {
				i18n("already_completed", player)
					.with("requirement_name", getRequirementName(requirement))
					.withPrefix()
					.send();
				return;
			}

			if (currentProgress.getStatus() == RequirementStatus.READY_TO_COMPLETE) {
				final RequirementCompletionResult result = progressManager.attemptRequirementCompletion(player, rdqPlayer, requirement);

				if (result.isSuccess()) {
					context.update();
				}
			} else {
				progressManager.refreshRankProgress(player, rdqPlayer, requirement.getRank());

				i18n("progress_refreshed", player).withPrefix().send();
				context.update();
			}
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to handle action click", exception);
			i18n("action_error", player).withPrefix().send();
		}
	}

	private void sendDetailedRequirementInfo(
		final @NotNull Player player,
		final @NotNull RRankUpgradeRequirement requirement
	) {
		try {
			i18n("detailed_info.header", player)
				.with("requirement_type", getRequirementType(requirement))
				.with("requirement_name", getRequirementName(requirement))
				.withPrefix()
				.send();

			i18n("detailed_info.lore", player)
				.withPrefix()
				.with("description", getRequirementDescription(requirement))
				.send();

			final AbstractRequirement abstractReq = requirement.getRequirement().getRequirement();

			if (abstractReq instanceof ItemRequirement itemReq) {
				final List<ItemRequirement.ItemProgress> itemProgressList = itemReq.getDetailedProgress(player);

				i18n("detailed_info.item_breakdown", player).withPrefix().send();

				for (final ItemRequirement.ItemProgress itemProgress : itemProgressList) {
					i18n("detailed_info.item_entry", player)
						.with("item_name", itemProgress.requiredItem().getType().name())
						.with("current", itemProgress.currentAmount())
						.with("required", itemProgress.requiredAmount())
						.with("percentage", itemProgress.getProgressPercentage())
						.withPrefix()
						.send();
				}
			}
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to send detailed requirement info", exception);
		}
	}

	private @NotNull String getRequirementType(final @NotNull RRankUpgradeRequirement requirement) {
		try {
			return requirement.getRequirement().getRequirement().getType().name();
		} catch (final Exception exception) {
			return "UNKNOWN";
		}
	}

	private @NotNull String getRequirementName(final @NotNull RRankUpgradeRequirement requirement) {
		try {
			return requirement.getRequirement().getRequirement().getType().name();
		} catch (final Exception exception) {
			return "Unknown Requirement";
		}
	}

	private @NotNull String getRequirementDescription(final @NotNull RRankUpgradeRequirement requirement) {
		try {
			return requirement.getRequirement().getRequirement().getDescriptionKey();
		} catch (final Exception exception) {
			return "A requirement for rank progression";
		}
	}

	private @NotNull ItemStack createFallbackItem(
		final @NotNull Player player,
		final @NotNull String name
	) {
		return UnifiedBuilderFactory.item(Material.PAPER)
			.setName(i18n("fallback.name", player).build().component())
			.setLore(i18n("fallback.lore", player).build().splitLines())
			.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			.build();
	}

	private void renderErrorState(
		final @NotNull RenderContext render,
		final @NotNull Player player
	) {
		try {
			render.slot(22,
				UnifiedBuilderFactory.item(Material.BARRIER)
					.setName(i18n("error.name", player).build().component())
					.setLore(i18n("error.lore", player).build().splitLines())
					.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
					.build()
			);

			LOGGER.log(Level.WARNING, "Rendering error state for requirement detail view");
		} catch (final Exception exception) {
			LOGGER.log(Level.SEVERE, "Failed to render error state", exception);
		}
	}
}