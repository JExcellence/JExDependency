package com.raindropcentral.rdq.view.ranks;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRankPath;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import com.raindropcentral.rdq.rank.IRankSystemService;
import com.raindropcentral.rdq.service.RankPathService;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.heads.view.Proceed;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Paginated GUI view for displaying all available rank trees in RaindropQuests.
 * <p>
 * This view allows players to browse, select, and interact with different rank trees.
 * It supports both preview (left click) and selection (right click) functionality.
 * Players with only the default rank must select a rank path before progressing.
 * </p>
 *
 * <ul>
 *   <li>Left click: Preview rank tree details and progression path.</li>
 *   <li>Right click: Select rank tree as active progression path.</li>
 *   <li>Displays prerequisite information and availability status.</li>
 *   <li>Supports pagination for large numbers of trees.</li>
 *   <li>Integrates with RankPathService for path management.</li>
 * </ul>
 *
 * @author JExcellence
 * @version 2.1.1
 * @since TBD
 */
public class RankTreeOverviewView extends APaginatedView<RRankTree> {

	private static final Logger LOGGER = CentralLogger.getLogger("RDQ");

	/**
	 * State holding the reference to the main RDQImpl plugin instance.
	 */
	private final State<RDQ> rdq = initialState("plugin");

	/**
	 * State holding the current player's entity.
	 */
	private final MutableState<RDQPlayer> rdqPlayer = initialState("player");

	/**
	 * Constructs a new RankTreeOverviewView, setting the parent view to {@link RankMainView}.
	 */
	public RankTreeOverviewView() {

		super(RankMainView.class);
	}

	@Override
	protected String getKey() {

		return "rank_tree_overview_ui";
	}

	/**
	 * Returns the layout of the inventory as rows.
	 */
	@Override
	protected String[] getLayout() {

		return new String[]{
				"         ",
				"  OOOOO  ",
				"   <p>   ",
				"         ",
				"         ",
				"b       c"
		};
	}

	/**
	 * Provides the asynchronous data source for the pagination.
	 * Fetches all available and enabled rank trees from the RankSystemFactory cache.
	 * 
	 * Note: Uses the in-memory cache instead of database queries to avoid
	 * transaction/session issues where newly created trees may not be visible
	 * in separate database sessions.
	 */
	@Override
	protected CompletableFuture<List<RRankTree>> getAsyncPaginationSource(
			final @NotNull Context context
	) {
		var plugin = this.rdq.get(context);
		
		// Use the cached trees from RankSystemFactory instead of database query
		// This ensures we see trees that were created during initialization
		var cachedTrees = plugin.getRankSystemFactory().getRankTrees();
		LOGGER.info("RankTreeOverviewView - Using cached trees: " + cachedTrees.size() + " trees: " + cachedTrees.keySet());

		return CompletableFuture.supplyAsync(() -> {
			List<RRankTree> enabledTrees = new ArrayList<>(
				cachedTrees.values().stream()
					.filter(RRankTree::isEnabled)
					.toList()
			);
			
			LOGGER.info("RankTreeOverviewView - After filtering enabled: " + enabledTrees.size() + " trees");
			
			for (var tree : enabledTrees) {
				LOGGER.info("  Tree: " + tree.getIdentifier() + ", enabled=" + tree.isEnabled() + 
					", ranks=" + (tree.getRanks() != null ? tree.getRanks().size() : 0));
			}
			
			enabledTrees.sort(Comparator.comparingInt(RRankTree::getDisplayOrder));
			return enabledTrees;
		});
	}

	/**
	 * Defines how to render a single rank tree entry in the pagination.
	 * Shows different visual states based on availability and selection status.
	 */
	@Override
	protected void renderEntry(
			final @NotNull Context context,
			final @NotNull BukkitItemComponentBuilder builder,
			final int index,
			final @NotNull RRankTree rankTree
	) {

		try {
			Player          player      = context.getPlayer();
			RDQ             plugin      = this.rdq.get(context);
			RankPathService pathService = plugin.getRankPathService();
			IRankSystemService rankSystemService = plugin.getRankSystemService();

			RDQPlayer currentPlayer = this.rdqPlayer.get(context);
			
			if (currentPlayer == null) {
				LOGGER.warning("RankTreeOverviewView - currentPlayer is null for tree: " + rankTree.getIdentifier());
				// Render a fallback item
				builder.withItem(UnifiedBuilderFactory.item(Material.PAPER)
						.setName(this.i18n(rankTree.getDisplayNameKey(), player).build().component())
						.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
						.build());
				return;
			}

			boolean hasSelectedPath = this.hasPlayerSelectedRankTree(
					currentPlayer,
					rankTree
			);

			boolean isCurrentlyActive = pathService.hasSelectedRankPath(
					currentPlayer,
					rankTree
			);

			boolean meetsPrerequisites = this.checkRankTreePrerequisites(
					currentPlayer,
					rankTree,
					pathService
			);

			boolean isPreviewOnlyDueToFreeVersion = rankSystemService != null && rankSystemService.isPreviewOnly(currentPlayer, rankTree);

			RankTreeDisplayState displayState = this.determineDisplayState(
					hasSelectedPath,
					isCurrentlyActive,
					meetsPrerequisites,
					isPreviewOnlyDueToFreeVersion,
					rankTree
			);

			Material        material = this.getMaterialForState(
					rankTree,
					displayState
			);
			
			List<Component> lore     = this.buildLoreForRankTree(
					player,
					rankTree,
					displayState,
					hasSelectedPath,
					isCurrentlyActive
			);

			builder.withItem(UnifiedBuilderFactory.item(material)
							.setName(this.i18n(
									rankTree.getDisplayNameKey(),
									player
							).build().component())
							.setLore(lore)
							.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
							.build())
					.updateOnClick().onClick(clickContext -> {
						this.handleRankTreeClick(
								clickContext,
								rankTree,
								displayState,
								hasSelectedPath,
								isCurrentlyActive
						);
					});
		} catch (final Exception exception) {
			LOGGER.log(java.util.logging.Level.SEVERE, "Failed to render rank tree entry: " + rankTree.getIdentifier(), exception);
			// Render a fallback error item
			builder.withItem(UnifiedBuilderFactory.item(Material.BARRIER)
					.setName(Component.text("Error: " + rankTree.getIdentifier()))
					.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
					.build());
		}
	}

	/**
	 * Checks if the player has ever selected this rank tree (regardless of current active status).
	 */
	private boolean hasPlayerSelectedRankTree(
			final @NotNull RDQPlayer player,
			final @NotNull RRankTree rankTree
	) {

		if (player.getPlayerRankPaths() == null) {
			return false;
		}
		return player.getPlayerRankPaths().stream()
				.anyMatch(rankPath -> rankPath.getSelectedRankPath() != null && 
						rankPath.getSelectedRankPath().equals(rankTree));
	}

	/**
	 * Determines the display state of a rank tree based on player status and prerequisites.
	 */
	private RankTreeDisplayState determineDisplayState(
			boolean hasSelectedPath,
			boolean isCurrentlyActive,
			boolean meetsPrerequisites,
			boolean isPreviewOnlyDueToFreeVersion,
			RRankTree rankTree
	) {

		if (
				hasSelectedPath
		) {
			if (
					isCurrentlyActive
			) {
				return RankTreeDisplayState.CURRENTLY_ACTIVE;
			} else {
				return RankTreeDisplayState.PREVIOUSLY_SELECTED;
			}
		} else if (
				isPreviewOnlyDueToFreeVersion
		) {
			return RankTreeDisplayState.FREE_VERSION_PREVIEW_ONLY;
		} else if (
				! meetsPrerequisites
		) {
			return RankTreeDisplayState.LOCKED;
		} else {
			return RankTreeDisplayState.AVAILABLE;
		}
	}

	/**
	 * Checks if the player meets the prerequisites for a rank tree.
	 */
	private boolean checkRankTreePrerequisites(
			final @NotNull RDQPlayer rdqPlayer,
			final @NotNull RRankTree rankTree,
			final @NotNull RankPathService pathService
	) {

		int minimumRequired = rankTree.getMinimumRankTreesToBeDone();
		int completedTrees  = this.getCompletedRankTreesCount(rdqPlayer);

		if (
				completedTrees < minimumRequired
		) {
			return false;
		}

		// Check if prerequisiteRankTrees collection is initialized before accessing
		// to avoid LazyInitializationException when entities are cached outside session
		if (!org.hibernate.Hibernate.isInitialized(rankTree.getPrerequisiteRankTrees())) {
			// Collection not initialized - assume no prerequisites (safe default)
			// Prerequisites are typically empty for most rank trees
			return true;
		}

		for (
				RRankTree prerequisite : rankTree.getPrerequisiteRankTrees()
		) {
			if (
					! this.isRankTreeCompleted(
							rdqPlayer,
							prerequisite
					)
			) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Gets the material to display based on the rank tree state.
	 */
	private Material getMaterialForState(
			final @NotNull RRankTree rankTree,
			final @NotNull RankTreeDisplayState state
	) {

		try {
			return switch (state) {
				case LOCKED -> Material.BARRIER;
				case FREE_VERSION_PREVIEW_ONLY -> Material.SPYGLASS; // Preview icon for free version
				default -> Material.valueOf(rankTree.getIcon().getMaterial().toUpperCase());
			};
		} catch (
				final IllegalArgumentException exception
		) {
			return Material.PAPER;
		}
	}

	/**
	 * Builds the lore for a rank tree based on its state and player status.
	 */
	private List<Component> buildLoreForRankTree(
			final @NotNull Player player,
			final @NotNull RRankTree rankTree,
			final @NotNull RankTreeDisplayState state,
			boolean hasSelectedPath,
			boolean isCurrentlyActive
	) {

		List<Component> lore = new ArrayList<>(this.i18n(
				rankTree.getDescriptionKey(),
				player
		).build().children());

		lore.add(Component.empty());

		switch (state) {
			case AVAILABLE:
				lore.addAll(this.i18n(
						"click.preview_rank_path",
						player
				).build().children());
				break;

			case LOCKED:
				lore.add(this.i18n(
						"click.locked",
						player
				).build().component());
				if (
						rankTree.getMinimumRankTreesToBeDone() > 0
				) {
					lore.addAll(this.i18n(
									"click.locked.required",
									player
							)
							.withPlaceholder("minimum_rank_paths_to_be_absolved_needed",
									rankTree.getMinimumRankTreesToBeDone()
							)
							.build().children());
				}
				// Check if prerequisiteRankTrees is initialized before accessing
				if (
						org.hibernate.Hibernate.isInitialized(rankTree.getPrerequisiteRankTrees())
						&& ! rankTree.getPrerequisiteRankTrees().isEmpty()
				) {
					lore.add(this.i18n(
							"click.pre_requisites",
							player
					).build().component());
					for (
							RRankTree prereq : rankTree.getPrerequisiteRankTrees()
					) {
						lore.addAll(this.i18n(
								"click.pre_requisites_" + prereq.getDisplayNameKey(),
								player
						).build().children());
					}
				}
				break;

			case CURRENTLY_ACTIVE:
				lore.add(this.i18n(
						"currently_active",
						player
				).build().component());
				lore.add(this.i18n(
						"click_to_preview",
						player
				).build().component());
				lore.add(this.i18n(
						"click_to_continue",
						player
				).build().component());
				break;

			case PREVIOUSLY_SELECTED:
				lore.add(this.i18n(
						"previously_selected",
						player
				).build().component());
				lore.add(this.i18n(
						"click_to_preview",
						player
				).build().component());
				lore.add(this.i18n(
						"click_to_reactivate",
						player
				).build().component());
				break;

			case FREE_VERSION_PREVIEW_ONLY:
				lore.add(Component.empty());
				lore.add(this.i18n(
						"free_version_preview_only",
						player
				).build().component());
				lore.addAll(this.i18n(
						"free_version_preview_only_lore",
						player
				).build().children());
				break;
		}

		return lore;
	}

	/**
	 * Handles click events on rank tree entries.
	 */
	private void handleRankTreeClick(
			final @NotNull SlotClickContext clickContext,
			final @NotNull RRankTree rankTree,
			final @NotNull RankTreeDisplayState state,
			boolean hasSelectedPath,
			boolean isCurrentlyActive
	) {

		Player    player    = clickContext.getPlayer();
		ClickType clickType = clickContext.getClickOrigin().getClick();

		switch (state) {
			case AVAILABLE:
				if (
						clickType == ClickType.LEFT
				) {
					this.openRankTreePreview(
							clickContext,
							rankTree
					);
				} else if (
						clickType == ClickType.RIGHT
				) {
					final int maximumRankPathsAllowed = this.rdq.get(clickContext).getRankSystemFactory().getRankSystemSection().getProgressionRule().getMaximumActiveRankTrees();
					if (
							maximumRankPathsAllowed <= this.rdq.get(clickContext).getPlayerRankPathRepository().findAllByAttributes(Map.of(
									"player.uniqueId",
									player.getUniqueId()
							)).stream().toList().size()
					) {
						this.i18n(
								"maximum_active_rank_paths_reached",
								player
						).includePrefix().withPlaceholder("maximum_rank_path_amount",
								maximumRankPathsAllowed
						).build().sendMessage();
						break;
					}
					this.selectRankTree(
							clickContext,
							rankTree
					);
				}
				break;

			case LOCKED:
				if (
						clickType == ClickType.LEFT
				) {
					this.openRankTreePreview(
							clickContext,
							rankTree
					);
					return;
				}
				this.i18n(
						"click.rank_path_locked",
						player
				).includePrefix().build().sendMessage();
				break;

			case CURRENTLY_ACTIVE:
				if (
						clickType == ClickType.LEFT
				) {
					this.openRankTreePreview(
							clickContext,
							rankTree
					);
				} else if (
						clickType == ClickType.RIGHT
				) {
					this.openRankPathOverview(
							clickContext,
							rankTree,
							false
					);
				}
				break;

			case PREVIOUSLY_SELECTED:
				if (
						clickType == ClickType.LEFT
				) {
					this.openRankTreePreview(
							clickContext,
							rankTree
					);
				} else if (
						clickType == ClickType.RIGHT
				) {
					this.reactivateRankTree(
							clickContext,
							rankTree
					);
				}
				break;

			case FREE_VERSION_PREVIEW_ONLY:
				// Free version: only allow preview, show upgrade message on right-click
				if (
						clickType == ClickType.LEFT
				) {
					this.openRankTreePreview(
							clickContext,
							rankTree
					);
				} else if (
						clickType == ClickType.RIGHT
				) {
					this.i18n(
							"free_version_upgrade_required",
							player
					).includePrefix().build().sendMessage();
				}
				break;
		}
	}

	/**
	 * Opens a preview of the rank tree without selecting it.
	 */
	private void openRankTreePreview(
			final @NotNull Context context,
			final @NotNull RRankTree rankTree
	) {

		context.openForPlayer(
				RankPathOverview.class,
				Map.of(
						"plugin",
						this.rdq.get(context),
						"player",
						this.rdqPlayer.get(context),
						"rankTree",
						rankTree,
						"previewMode",
						true
				)
		);
	}

	/**
	 * Opens the rank path overview for progression.
	 */
	private void openRankPathOverview(
			final @NotNull Context context,
			final @NotNull RRankTree rankTree,
			final boolean previewMode
	) {

		context.openForPlayer(
				RankPathOverview.class,
				Map.of(
						"plugin",
						this.rdq.get(context),
						"player",
						this.rdqPlayer.get(context),
						"rankTree",
						rankTree,
						"previewMode",
						previewMode
				)
		);
	}

	/**
	 * Selects a rank tree as the player's active progression path.
	 */
	private void selectRankTree(
			final @NotNull Context context,
			final @NotNull RRankTree rankTree
	) {

		Player player       = context.getPlayer();
		RRank  startingRank = this.findStartingRank(rankTree);

		if (
				startingRank == null
		) {
			this.i18n(
					"select.rank_path_starting_rank_not_found",
					player
			).includePrefix().build().sendMessage();
			return;
		}

		boolean success = this.rdq.get(context).getRankPathService().selectRankPath(
				this.rdqPlayer.get(context),
				rankTree,
				startingRank
		);

		if (success) {
			this.i18n(
					"select.rank_path_selected",
					player
			).includePrefix().withPlaceholders(
					Map.of(
							"rank_path",
							rankTree.getIdentifier(),
							"rank",
							startingRank.getIdentifier()
					)
			).build().sendMessage();

			this.openRankPathOverview(
					context,
					rankTree,
					false
			);
		} else {
			this.i18n(
					"select.rank_path_selection_failed",
					player
			).includePrefix().build().sendMessage();
		}
	}

	/**
	 * Reactivates a previously selected rank tree.
	 */
	private void reactivateRankTree(
			final @NotNull Context context,
			final @NotNull RRankTree rankTree
	) {

		Player player       = context.getPlayer();
		RRank  startingRank = this.findStartingRank(rankTree);

		if (
				startingRank == null
		) {
			this.i18n(
					"select.rank_path_starting_rank_not_found",
					player
			).includePrefix().build().sendMessage();
			return;
		}

		boolean success = this.rdq.get(context).getRankPathService().switchRankPath(
				this.rdqPlayer.get(context),
				rankTree,
				startingRank
		);

		if (
				success
		) {
			this.i18n(
					"select.rank_path_reactivated",
					player
			).includePrefix().withPlaceholders(
					Map.of(
							"rank_path",
							rankTree.getIdentifier()
					)
			).build().sendMessage();

			this.openRankPathOverview(
					context,
					rankTree,
					false
			);
		} else {
			this.i18n(
					"select.rank_path_reactivation_failed",
					player
			).includePrefix().build().sendMessage();
		}
	}

	/**
	 * Finds the starting rank for a rank tree.
	 */
	private RRank findStartingRank(
			final @NotNull RRankTree rankTree
	) {

		return rankTree.getRanks().stream().filter(RRank::isInitialRank).findFirst().orElse(null);
	}

	/**
	 * Gets the count of completed rank trees for a player.
	 */
	private int getCompletedRankTreesCount(
			final @NotNull RDQPlayer rdqPlayer
	) {

		return rdqPlayer.getPlayerRankPaths().stream().filter(RPlayerRankPath::isCompleted).toList().size();
	}

	/**
	 * Checks if a specific rank tree is completed by a player.
	 */
	private boolean isRankTreeCompleted(
			final @NotNull RDQPlayer rdqPlayer,
			final @NotNull RRankTree rankTree
	) {

		return rdqPlayer.getPlayerRankPaths().stream().anyMatch(RDQPlayerRankPath -> RDQPlayerRankPath.getSelectedRankPath().equals(rankTree) && RDQPlayerRankPath.isCompleted());
	}

	@Override
	protected void onPaginatedRender(
			final @NotNull RenderContext render,
			final @NotNull Player player
	) {

		render
				.layoutSlot(
						'c',
						UnifiedBuilderFactory
								.item(new Proceed().getHead(player))
								.setName(
										this.i18n(
												"create_rank_tree.name",
												player
										).build().component()
								)
								.setLore(
										this.i18n(
												"create_rank_tree.lore",
												player
										).build().children()
								)
								.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
								.build()
				)
				.onClick(slotClickContext -> {
					this.i18n(
							"select.rank_tree_creation_coming_soon",
							player
					).includePrefix().build().sendMessage();
				})
				.displayIf(player::isOp);
	}

	/**
	 * Enum representing the different display states of a rank tree.
	 */
	private enum RankTreeDisplayState {
		AVAILABLE,
		LOCKED,
		CURRENTLY_ACTIVE,
		PREVIOUSLY_SELECTED,
		FREE_VERSION_PREVIEW_ONLY
	}

}