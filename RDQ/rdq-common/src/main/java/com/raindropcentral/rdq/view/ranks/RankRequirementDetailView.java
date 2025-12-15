package com.raindropcentral.rdq.view.ranks;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import com.raindropcentral.rdq.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rdq.manager.RankRequirementProgressManager;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import com.raindropcentral.rdq.requirement.ItemRequirement;
import com.raindropcentral.rplatform.logging.CentralLogger;
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

	private static final Logger LOGGER = CentralLogger.getLogger(RankRequirementDetailView.class.getName());

	private final State<RDQ>                     rdq               = initialState("plugin");
	private final State<RDQPlayer>                 currentPlayer     = initialState("player");
	private final State<RRankTree>               selectedRankTree  = this.initialState("rankTree");
	private final State<RRankUpgradeRequirement> targetRequirement = initialState("requirement");

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

		public RequirementDetailItemType getType() {

			return type;
		}

	}

	public enum RequirementDetailItemType {
		ITEM_PROGRESS,
		SUMMARY,
		TIPS,
		FILLER
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

		return new String[]{
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
				final RRankUpgradeRequirement requirement = this.targetRequirement.get(context);
				final Player                  player      = context.getPlayer();

				if (
						requirement == null
				) {
					return null;
				}

				List<RequirementDetailItem> items       = new ArrayList<>();
				AbstractRequirement         abstractReq = requirement.getRequirement().getRequirement();

				if (
						abstractReq instanceof ItemRequirement itemReq
				) {
					List<ItemRequirement.ItemProgress> itemProgressList = itemReq.getDetailedProgress(player);

					for (
							int i = 0; i < itemProgressList.size(); i++
					) {
						ItemRequirement.ItemProgress itemProgress = itemProgressList.get(i);
						ItemStack                    displayItem  = this.createItemProgressItem(
								player,
								itemProgress,
								i + 1
						);
						items.add(new RequirementDetailItem(
								displayItem,
								"Item " + (i + 1),
								itemProgress,
								i
						));
					}

					if (
							itemProgressList.size() > 1
					) {
						ItemStack summaryItem = this.createItemRequirementSummary(
								player,
								itemReq
						);
						items.add(new RequirementDetailItem(
								summaryItem,
								"Summary",
								RequirementDetailItemType.SUMMARY
						));
					}

					ItemStack tipsItem = this.createItemRequirementTips(
							player,
							itemReq
					);
					items.add(new RequirementDetailItem(
							tipsItem,
							"Tips",
							RequirementDetailItemType.TIPS
					));
				} else {
					ItemStack summaryItem = this.createGenericSummaryItem(
							player,
							abstractReq,
							requirement,
							context
					);
					items.add(new RequirementDetailItem(
							summaryItem,
							"Summary",
							RequirementDetailItemType.SUMMARY
					));

					ItemStack tipsItem = this.createRequirementTipsItem(
							player,
							abstractReq
					);
					items.add(new RequirementDetailItem(
							tipsItem,
							"Tips",
							RequirementDetailItemType.TIPS
					));
				}

				return items;
			} catch (
					final Exception exception
			) {
				LOGGER.log(
						Level.SEVERE,
						"Error generating pagination source",
						exception
				);
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
			if (
					requirement == null
			) {
				this.renderErrorState(
						render,
						player
				);
				return;
			}

			final RDQPlayer                                                rdqPlayer  = this.currentPlayer.get(render);
			final RankRequirementProgressManager.RequirementProgressData progress = this.progressManager.getRequirementProgress(
					player,
					rdqPlayer,
					requirement
			);

			render.layoutSlot(
					'i',
					this.createRequirementInfoItem(
							player,
							requirement,
							progress
					)
			).onClick(this::handleInfoClick);

			render.layoutSlot(
					't',
					this.createProgressItem(
							player,
							requirement,
							progress
					)
			).onClick(context -> this.handleProgressClick(
					context,
					requirement
			));

			render.layoutSlot(
					's',
					this.createStatusItem(
							player,
							requirement,
							progress
					)
			).onClick(context -> this.handleStatusClick(
					context,
					requirement
			));

			render.layoutSlot(
					'a',
					this.createActionButton(
							player,
							requirement,
							progress
					)
			).onClick(context -> this.handleActionClick(
					context,
					requirement,
					progress
			));

			// Render back button
			render.layoutSlot(
					'b',
					this.createBackButton(player)
			).onClick(context -> context.back());
		} catch (
				final Exception exception
		) {
			LOGGER.log(
					Level.SEVERE,
					"Critical error during requirement detail render",
					exception
			);
			this.renderErrorState(
					render,
					player
			);
		}
	}

	/**
	 * Creates the back button item.
	 */
	private ItemStack createBackButton(final @NotNull Player player) {
		try {
			return UnifiedBuilderFactory.item(
					new com.raindropcentral.rplatform.utility.heads.view.Return().getHead(player)
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
	 * Creates an item showing progress for a specific required item.
	 */
	private ItemStack createItemProgressItem(
			final @NotNull Player player,
			final @NotNull ItemRequirement.ItemProgress itemProgress,
			final int itemNumber
	) {

		try {
			List<Component> lore = new ArrayList<>(List.of(
					Component.empty(),
					this.i18n(
							"progress_text",
							player
					).build().component(),
					this.createProgressBar(
							itemProgress.progress(),
							10
					),
					Component.empty(),
					this.i18n(
							"current_progress",
							player
					).withPlaceholders(
							Map.of(
									"current_amount",
									itemProgress.currentAmount(),
									"required_amount",
									itemProgress.requiredAmount()
							)
					).build().component(),
					Component.empty(),
					this.i18n(
							"current_progress_percentage",
							player
					).withPlaceholder("progress_percentage",
							itemProgress.getProgressPercentage()
					).build().component()
			));

			if (
					! itemProgress.completed()
			) {
				lore.addAll(
						List.of(
								Component.empty(),
								this.i18n(
										"required_amount_left",
										player
								).withPlaceholder("required_amount",
										itemProgress.getShortage()
								).build().component()
						)
				);
			}

			lore.add(Component.empty());

			if (
					itemProgress.completed()
			) {
				lore.add(
						this.i18n(
								"progress_completed",
								player
						).build().component()
				);
			} else if (
					itemProgress.currentAmount() > 0
			) {
				lore.add(
						this.i18n(
								"in_progress",
								player
						).build().component()
				);
			} else {
				lore.add(
						this.i18n(
								"progress_not_started",
								player
						).build().component()
				);
			}

			lore.addAll(
					List.of(
							Component.empty(),
							this.i18n(
									"progress_details",
									player
							).withPlaceholder("item_number",
									itemNumber
							).build().component()
					)
			);

			return
					UnifiedBuilderFactory
							.item(
									itemProgress.requiredItem()
							)
							.setLore(
									lore
							).setAmount(
									Math.max(
											1,
											Math.min(
													64,
													itemProgress.requiredAmount()
											)
									)
							).setGlowing(itemProgress.completed()).addItemFlags(
									ItemFlag.HIDE_ATTRIBUTES,
									ItemFlag.HIDE_ENCHANTS,
									ItemFlag.HIDE_STORED_ENCHANTS,
									ItemFlag.HIDE_DESTROYS
							).build()
					;
		} catch (
				final Exception exception
		) {
			LOGGER.log(
					Level.WARNING,
					"Failed to create item progress item",
					exception
			);
			return this.createFallbackItem(
					player,
					"Item " + itemNumber + " (Error)"
			);
		}
	}

	/**
	 * Creates a summary item for item requirements.
	 */
	private ItemStack createItemRequirementSummary(
			final @NotNull Player player,
			final @NotNull ItemRequirement itemReq
	) {

		List<ItemRequirement.ItemProgress> itemProgressList = itemReq.getDetailedProgress(player);

		int    completedItems  = 0;
		int    totalItems      = itemProgressList.size();
		double overallProgress = 0.0;

		for (
				ItemRequirement.ItemProgress progress : itemProgressList
		) {
			if (
					progress.completed()
			) {
				completedItems++;
			}
			overallProgress += progress.progress();
		}

		overallProgress = overallProgress / totalItems;

		List<Component> lore = new ArrayList<>(
				List.of(
						Component.empty(),
						this.i18n(
								"summary.items_completed",
								player
						).withPlaceholders(
								Map.of(
										"completed_item_amount",
										completedItems,
										"total_item_amount",
										totalItems
								)
						).build().component(),
						Component.empty(),
						this.createProgressBar(
								overallProgress,
								15
						),
						Component.empty(),
						this.i18n(
								"current_progress_percentage",
								player
						).withPlaceholder("progress_percentage",
								String.format(
										"%.1f",
										overallProgress * 100
								) + "%"
						).build().component(),
						this.i18n(
								"current_progress_status",
								player
						).withPlaceholder("progress_status",
								overallProgress >= 1.0 ?
										"COMPLETED" :
										"IN PROGRESS"
						).build().component()
				)
		);

		return UnifiedBuilderFactory.item(Material.CHEST)
				.setName(
						this.i18n(
								"current_progress_summary",
								player
						).build().component()
				)
				.setLore(lore)
				.addItemFlags(
						ItemFlag.HIDE_ATTRIBUTES,
						ItemFlag.HIDE_ENCHANTS,
						ItemFlag.HIDE_ATTRIBUTES,
						ItemFlag.HIDE_UNBREAKABLE
				)
				.setGlowing(overallProgress >= 1.0)
				.build();
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
	 */
	private ItemStack createGenericSummaryItem(
			final @NotNull Player player,
			final @NotNull AbstractRequirement abstractReq,
			final @NotNull RRankUpgradeRequirement requirement,
			final @NotNull Context context
	) {

		Material iconMaterial = switch (abstractReq.getType()) {
			case CURRENCY -> Material.GOLD_INGOT;
			case EXPERIENCE_LEVEL -> Material.EXPERIENCE_BOTTLE;
			case PLAYTIME -> Material.CLOCK;
			case PERMISSION -> Material.PAPER;
			case LOCATION -> Material.COMPASS;
			default -> Material.BOOK;
		};
		final RankRequirementProgressManager.RequirementProgressData progress = this.progressManager.getRequirementProgress(
				player,
				this.currentPlayer.get(context),
				requirement
		);

		List<Component> lore = new ArrayList<>(
				List.of(
						this.i18n(
								"summary.requirement_type",
								player
						).withPlaceholder("type",
								abstractReq.getType()
						).build().component(),
						Component.empty(),
						this.createProgressBar(
								progress.getProgressPercentage() / 100,
								15
						),
						this.i18n(
								"summary.progress_percentage",
								player
						).withPlaceholder("completion_progress_percentage",
								progress.getProgressAsPercentage() + "%"
						).build().component(),
						Component.empty(),
						this.i18n(
								"summary.completion_status",
								player
						).withPlaceholder("completion_status",
								progress.getStatus().name()
						).build().component()
				)
		);

		return
				UnifiedBuilderFactory.item(
						iconMaterial
				).setName(
						this.i18n(
								"summary.requirement_summary.name",
								player
						).build().component()
				).setLore(
						lore
				).addItemFlags(
						ItemFlag.HIDE_ATTRIBUTES,
						ItemFlag.HIDE_ENCHANTS,
						ItemFlag.HIDE_STORED_ENCHANTS
				).setGlowing(progress.isCompleted()).build();
	}

	/**
	 * Creates a tips item for the requirement.
	 */
	private ItemStack createRequirementTipsItem(
			final @NotNull Player player,
			final @NotNull AbstractRequirement abstractReq
	) {

		List<Component> lore = new ArrayList<>();

		switch (abstractReq.getType()) {
			case CURRENCY -> {
				lore.addAll(
						this.i18n(
								"requirement_tip.currency",
								player
						).build().children()
				);
			}
			case EXPERIENCE_LEVEL -> {
				lore.addAll(
						this.i18n(
								"requirement_tip.experience_level",
								player
						).build().children()
				);
			}
			case PLAYTIME -> {
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

		final Player player = context.getPlayer();

		try {
			this.i18n(
							"item_detail.header",
							player
					)
					.withPlaceholder("item_name",
							itemProgress.requiredItem().getType().name()
					)
					.withPlaceholder("item_number",
							itemProgress.index() + 1
					)
					.includePrefix()
					.build().sendMessage();

			this.i18n(
							"item_detail.progress",
							player
					)
					.withPlaceholder("current",
							itemProgress.currentAmount()
					)
					.withPlaceholder("required",
							itemProgress.requiredAmount()
					)
					.withPlaceholder("percentage",
							itemProgress.getProgressPercentage()
					)
					.includePrefix()
					.build().sendMessage();

			if (! itemProgress.completed()) {
				this.i18n(
								"item_detail.missing",
								player
						)
						.withPlaceholder("shortage",
								itemProgress.getShortage()
						)
						.includePrefix()
						.build().sendMessage();
			}
		} catch (Exception e) {
			LOGGER.log(
					Level.WARNING,
					"Failed to handle item progress click",
					e
			);
		}
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
									requirement.getRequirement().getRequirement().getType().name()
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

		final Player                  player      = context.getPlayer();
		final RRankUpgradeRequirement requirement = this.targetRequirement.get(context);

		try {
			this.sendDetailedRequirementInfo(
					player,
					requirement
			);
		} catch (
				final Exception exception
		) {
			LOGGER.log(
					Level.WARNING,
					"Failed to handle info click",
					exception
			);
		}
	}

	private void handleProgressClick(
			final @NotNull Context context,
			final @NotNull RRankUpgradeRequirement requirement
	) {

		final Player  player  = context.getPlayer();
		final RDQPlayer rdqPlayer = this.currentPlayer.get(context);

		try {
			RankRequirementProgressManager.RequirementProgressData progress =
					this.progressManager.getRequirementProgress(
							player,
							rdqPlayer,
							requirement
					);

			this.i18n(
							"progress_info",
							player
					)
					.withPlaceholder("progress",
							progress.getProgressAsPercentage()
					)
					.withPlaceholder("formatted_progress",
							progress.getFormattedProgress()
					)
					.withPlaceholder("status",
							progress.getStatus().name().toLowerCase()
					)
					.includePrefix()
					.build().sendMessage();

			context.update();
		} catch (
				final Exception exception
		) {
			LOGGER.log(
					Level.WARNING,
					"Failed to handle progress click",
					exception
			);
		}
	}

	private void handleStatusClick(
			final @NotNull Context context,
			final @NotNull RRankUpgradeRequirement requirement
	) {

		final Player  player  = context.getPlayer();
		final RDQPlayer rdqPlayer = this.currentPlayer.get(context);

		try {
			this.progressManager.refreshRequirementProgress(
					player,
					rdqPlayer,
					requirement
			);

			RankRequirementProgressManager.RequirementProgressData updatedProgress =
					this.progressManager.getRequirementProgress(
							player,
							rdqPlayer,
							requirement
					);

			this.i18n(
							"status_update",
							player
					)
					.withPlaceholder("status",
							updatedProgress.getStatus().name()
					)
					.withPlaceholder("progress",
							updatedProgress.getProgressAsPercentage()
					)
					.includePrefix()
					.build().sendMessage();

			context.update();
		} catch (
				final Exception exception
		) {
			LOGGER.log(
					Level.WARNING,
					"Failed to handle status click",
					exception
			);
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

	private void sendDetailedRequirementInfo(
			final @NotNull Player player,
			final @NotNull RRankUpgradeRequirement requirement
	) {

		try {
			this.i18n(
							"detailed_info.header",
							player
					)
					.withPlaceholder("requirement_type",
							this.getRequirementType(requirement)
					)
					.withPlaceholder("requirement_name",
							this.getRequirementName(requirement)
					)
					.includePrefix()
					.build().sendMessage();

			this.i18n(
							"detailed_info.lore",
							player
					)
					.includePrefix()
					.withPlaceholder("description",
							this.getRequirementDescription(requirement)
					)
					.build().sendMessage();

			AbstractRequirement abstractReq = requirement.getRequirement().getRequirement();
			if (
					abstractReq instanceof ItemRequirement itemReq
			) {
				List<ItemRequirement.ItemProgress> itemProgressList = itemReq.getDetailedProgress(player);

				this.i18n(
								"detailed_info.item_breakdown",
								player
						)
						.includePrefix()
						.build().sendMessage();

				for (
						ItemRequirement.ItemProgress itemProgress : itemProgressList
				) {
					this.i18n(
									"detailed_info.item_entry",
									player
							)
							.withPlaceholder("item_name",
									itemProgress.requiredItem().getType().name()
							)
							.withPlaceholder("current",
									itemProgress.currentAmount()
							)
							.withPlaceholder("required",
									itemProgress.requiredAmount()
							)
							.withPlaceholder("percentage",
									itemProgress.getProgressPercentage()
							)
							.includePrefix()
							.build().sendMessage();
				}
			}
		} catch (
				final Exception exception
		) {
			LOGGER.log(
					Level.WARNING,
					"Failed to send detailed requirement info",
					exception
			);
		}
	}

	private String getRequirementType(
			final @NotNull RRankUpgradeRequirement requirement
	) {

		try {
			return requirement.getRequirement().getRequirement().getType().name();
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
			return requirement.getRequirement().getRequirement().getType().name();
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