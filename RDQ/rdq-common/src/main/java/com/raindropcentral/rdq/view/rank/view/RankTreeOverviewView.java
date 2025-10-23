package com.raindropcentral.rdq.view.rank.view;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRankPath;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import com.raindropcentral.rdq.service.RankPathService;
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

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class RankTreeOverviewView extends APaginatedView<RRankTree> {

	private final State<RDQ>              rdq       = initialState("plugin");
	private final MutableState<RDQPlayer> rdqPlayer = initialState("player");

	public RankTreeOverviewView() {
		super(RankMainView.class);
	}

	@Override
	protected String getKey() {
		return "rank_tree_overview_ui";
	}

	@Override
	protected String[] getLayout() {
		return new String[] {
			"         ",
			"  OOOOO  ",
			"   <p>   ",
			"         ",
			"         ",
			"b       c"
		};
	}

	@Override
	protected CompletableFuture<List<RRankTree>> getAsyncPaginationSource(
		final @NotNull Context context
	) {
		return rdq.get(context).getRankTreeRepository()
			.findAllAsync(1, 128)
			.thenApply(trees -> {
				final List<RRankTree> mutableTrees = new ArrayList<>(
					trees.stream().filter(RRankTree::isEnabled).toList()
				);
				mutableTrees.sort(Comparator.comparingInt(RRankTree::getDisplayOrder));
				return mutableTrees;
			});
	}

	@Override
	protected void renderEntry(
		final @NotNull Context context,
		final @NotNull BukkitItemComponentBuilder builder,
		final int index,
		final @NotNull RRankTree rankTree
	) {
		final Player player = context.getPlayer();
		final RankPathService pathService   = rdq.get(context).getRankPathService();
		final RDQPlayer       currentPlayer = rdqPlayer.get(context);

		final boolean hasSelectedPath = hasPlayerSelectedRankTree(currentPlayer, rankTree, rdq.get(context));
		final boolean isCurrentlyActive = pathService.hasSelectedRankPath(currentPlayer, rankTree);
		final boolean meetsPrerequisites = checkRankTreePrerequisites(currentPlayer, rankTree, rdq.get(context));

		final TreeDisplayState displayState = determineDisplayState(hasSelectedPath, isCurrentlyActive, meetsPrerequisites, rankTree);
		final Material material = getMaterialForState(rankTree, displayState);
		final List<Component> lore = buildLoreForRankTree(player, rankTree, displayState, hasSelectedPath, isCurrentlyActive);

		builder.withItem(
			UnifiedBuilderFactory.item(material)
				.setName(i18n(rankTree.getDisplayNameKey(), player).build().component())
				.setLore(lore)
				.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
				.build()
		).updateOnClick().onClick(clickContext -> {
			handleRankTreeClick(clickContext, rankTree, displayState, hasSelectedPath, isCurrentlyActive);
		});
	}

	private boolean hasPlayerSelectedRankTree(
		final @NotNull RDQPlayer player,
		final @NotNull RRankTree rankTree,
        final @NotNull RDQ rdq
	) {
		return player.getPlayerRankPaths().stream().anyMatch(rankPath -> Objects.equals(rankPath.getSelectedRankPath(rdq, player), rankTree));
	}

	private TreeDisplayState determineDisplayState(
		final boolean hasSelectedPath,
		final boolean isCurrentlyActive,
		final boolean meetsPrerequisites,
		final @NotNull RRankTree rankTree
	) {
		if (hasSelectedPath) {
			return isCurrentlyActive ? TreeDisplayState.CURRENTLY_ACTIVE : TreeDisplayState.PREVIOUSLY_SELECTED;
		} else if (!meetsPrerequisites) {
			return TreeDisplayState.LOCKED;
		} else {
			return TreeDisplayState.AVAILABLE;
		}
	}

	private boolean checkRankTreePrerequisites(
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRankTree rankTree,
        final @NotNull RDQ rdq
	) {
		final int minimumRequired = rankTree.getMinimumRankTreesToBeDone();
		final int completedTrees = getCompletedRankTreesCount(rdqPlayer);

		if (completedTrees < minimumRequired) {
			return false;
		}

		for (final RRankTree prerequisite : rankTree.getPrerequisiteRankTrees()) {
			if (!isRankTreeCompleted(rdq, rdqPlayer, prerequisite)) {
				return false;
			}
		}

		return true;
	}

	private Material getMaterialForState(
		final @NotNull RRankTree rankTree,
		final @NotNull TreeDisplayState state
	) {
		try {
			return switch (state) {
				case LOCKED -> Material.BARRIER;
				default -> Material.valueOf(rankTree.getIcon().getMaterial().toUpperCase());
			};
		} catch (final IllegalArgumentException exception) {
			return Material.PAPER;
		}
	}

	private List<Component> buildLoreForRankTree(
		final @NotNull Player player,
		final @NotNull RRankTree rankTree,
		final @NotNull TreeDisplayState state,
		final boolean hasSelectedPath,
		final boolean isCurrentlyActive
	) {
		final List<Component> lore = new ArrayList<>(
			i18n(rankTree.getDescriptionKey(), player).build().splitLines()
		);
		lore.add(Component.empty());

		switch (state) {
			case AVAILABLE -> {
				lore.addAll(i18n("click.preview_rank_path", player).build().splitLines());
			}
			case LOCKED -> {
				lore.add(i18n("click.locked", player).build().component());

				if (rankTree.getMinimumRankTreesToBeDone() > 0) {
					lore.addAll(i18n("click.locked.required", player)
						.with("minimum_rank_paths_to_be_absolved_needed", rankTree.getMinimumRankTreesToBeDone())
						.build().splitLines());
				}

				if (!rankTree.getPrerequisiteRankTrees().isEmpty()) {
					lore.add(i18n("click.pre_requisites", player).build().component());
					for (final RRankTree prereq : rankTree.getPrerequisiteRankTrees()) {
						lore.addAll(i18n("click.pre_requisites_" + prereq.getDisplayNameKey(), player).build().splitLines());
					}
				}
			}
			case CURRENTLY_ACTIVE -> {
				lore.add(i18n("currently_active", player).build().component());
				lore.add(i18n("click_to_preview", player).build().component());
				lore.add(i18n("click_to_continue", player).build().component());
			}
			case PREVIOUSLY_SELECTED -> {
				lore.add(i18n("previously_selected", player).build().component());
				lore.add(i18n("click_to_preview", player).build().component());
				lore.add(i18n("click_to_reactivate", player).build().component());
			}
		}

		return lore;
	}

	private void handleRankTreeClick(
		final @NotNull SlotClickContext clickContext,
		final @NotNull RRankTree rankTree,
		final @NotNull TreeDisplayState state,
		final boolean hasSelectedPath,
		final boolean isCurrentlyActive
	) {
		final Player player = clickContext.getPlayer();
		final ClickType clickType = clickContext.getClickOrigin().getClick();

		switch (state) {
			case AVAILABLE -> {
				if (clickType == ClickType.LEFT) {
					openRankTreePreview(clickContext, rankTree);
				} else if (clickType == ClickType.RIGHT) {
					final int maximumRankPathsAllowed = rdq.get(clickContext)
						.getRankSystemFactory()
						.getRankSystemSection()
						.getProgressionRule()
						.getMaximumActiveRankTrees();

					final int currentActiveCount = rdq.get(clickContext)
						.getPlayerRankPathRepository()
						.findListByAttributes(Map.of("player.uniqueId", player.getUniqueId()))
						.stream()
						.toList()
						.size();

					if (maximumRankPathsAllowed <= currentActiveCount) {
						i18n("maximum_active_rank_paths_reached", player)
							.withPrefix()
							.with("maximum_rank_path_amount", maximumRankPathsAllowed)
							.send();
						break;
					}

					selectRankTree(clickContext, rankTree);
				}
			}
			case LOCKED -> {
				if (clickType == ClickType.LEFT) {
					openRankTreePreview(clickContext, rankTree);
					return;
				}
				i18n("click.rank_path_locked", player).withPrefix().send();
			}
			case CURRENTLY_ACTIVE -> {
				if (clickType == ClickType.LEFT) {
					openRankTreePreview(clickContext, rankTree);
				} else if (clickType == ClickType.RIGHT) {
					openRankPathOverview(clickContext, rankTree, false);
				}
			}
			case PREVIOUSLY_SELECTED -> {
				if (clickType == ClickType.LEFT) {
					openRankTreePreview(clickContext, rankTree);
				} else if (clickType == ClickType.RIGHT) {
					reactivateRankTree(clickContext, rankTree);
				}
			}
		}
	}

	private void openRankTreePreview(
		final @NotNull Context context,
		final @NotNull RRankTree rankTree
	) {
		context.openForPlayer(
			RankPathOverview.class,
			Map.of(
				"plugin", rdq.get(context),
				"player", rdqPlayer.get(context),
				"rankTree", rankTree,
				"previewMode", true
			)
		);
	}

	private void openRankPathOverview(
		final @NotNull Context context,
		final @NotNull RRankTree rankTree,
		final boolean previewMode
	) {
		context.openForPlayer(
			RankPathOverview.class,
			Map.of(
				"plugin", rdq.get(context),
				"player", rdqPlayer.get(context),
				"rankTree", rankTree,
				"previewMode", previewMode
			)
		);
	}

	private void selectRankTree(
		final @NotNull Context context,
		final @NotNull RRankTree rankTree
	) {
		final Player player = context.getPlayer();
		final RRank startingRank = findStartingRank(rankTree);

		if (startingRank == null) {
			i18n("select.rank_path_starting_rank_not_found", player).withPrefix().send();
			return;
		}

		final boolean success = rdq.get(context).getRankPathService().selectRankPath(
			rdqPlayer.get(context),
			rankTree,
			startingRank
		);

		if (success) {
			i18n("select.rank_path_selected", player)
				.withPrefix()
				.withAll(Map.of(
					"rank_path", rankTree.getIdentifier(),
					"rank", startingRank.getIdentifier()
				))
				.send();
			openRankPathOverview(context, rankTree, false);
		} else {
			i18n("select.rank_path_selection_failed", player).withPrefix().send();
		}
	}

	private void reactivateRankTree(
		final @NotNull Context context,
		final @NotNull RRankTree rankTree
	) {
		final Player player = context.getPlayer();
		final RRank startingRank = findStartingRank(rankTree);

		if (startingRank == null) {
			i18n("select.rank_path_starting_rank_not_found", player).withPrefix().send();
			return;
		}

		final boolean success = rdq.get(context).getRankPathService().switchRankPath(
			rdqPlayer.get(context),
			rankTree,
			startingRank
		);

		if (success) {
			i18n("select.rank_path_reactivated", player)
				.withPrefix()
				.withAll(Map.of("rank_path", rankTree.getIdentifier()))
				.send();
			openRankPathOverview(context, rankTree, false);
		} else {
			i18n("select.rank_path_reactivation_failed", player).withPrefix().send();
		}
	}

	private RRank findStartingRank(final @NotNull RRankTree rankTree) {
		return rankTree.getRanks().stream()
			.filter(RRank::isInitialRank)
			.findFirst()
			.orElse(null);
	}

	private int getCompletedRankTreesCount(final @NotNull RDQPlayer rdqPlayer) {
		return (int) rdqPlayer.getPlayerRankPaths().stream()
			.filter(RPlayerRankPath::isCompleted)
			.count();
	}

	private boolean isRankTreeCompleted(
            final @NotNull RDQ rdq,
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRankTree rankTree
	) {
		return rdqPlayer.getPlayerRankPaths().stream()
			.anyMatch(rankPath -> Objects.equals(rankPath.getSelectedRankPath(rdq, rdqPlayer), rankTree) && rankPath.isCompleted());
	}

	@Override
	protected void onPaginatedRender(
		final @NotNull RenderContext render,
		final @NotNull Player player
	) {
		render.layoutSlot(
			'c',
			UnifiedBuilderFactory.item(new Proceed().getHead(player))
				.setName(i18n("create_rank_tree.name", player).build().component())
				.setLore(i18n("create_rank_tree.lore", player).build().splitLines())
				.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
				.build()
		).onClick(slotClickContext -> {
			i18n("select.rank_tree_creation_coming_soon", player).withPrefix().send();
		}).displayIf(player::isOp);
	}

	private enum TreeDisplayState {
		AVAILABLE, LOCKED, CURRENTLY_ACTIVE, PREVIOUSLY_SELECTED
	}
}