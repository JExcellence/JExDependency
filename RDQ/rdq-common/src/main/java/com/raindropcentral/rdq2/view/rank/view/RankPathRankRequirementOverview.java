/*
package com.raindropcentral.rdq2.view.rank.view;

import com.raindropcentral.rdq2.RDQ;
import com.raindropcentral.rdq2.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq2.database.entity.rank.RPlayerRank;
import com.raindropcentral.rdq2.database.entity.rank.RRank;
import com.raindropcentral.rdq2.database.entity.rank.RRankTree;
import com.raindropcentral.rdq2.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rdq2.manager.rank.RankRequirementProgressManager;
import com.raindropcentral.rdq2.view.rank.RequirementCompletionResult;
import com.raindropcentral.rdq2.view.rank.RequirementProgressData;
import com.raindropcentral.rdq2.view.rank.RequirementStatus;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public final class RankPathRankRequirementOverview extends APaginatedView<RRankUpgradeRequirement> {

	private static final Logger LOGGER = CentralLogger.getLogger(RankPathRankRequirementOverview.class.getName());
	private static final long RETURN_DELAY_TICKS = 40L; // 2 seconds

	private final State<RDQ>       rdq           = initialState("plugin");
	private final State<RDQPlayer> currentPlayer = initialState("player");
	private final State<RRank>     targetRank       = initialState("targetRank");
	private final State<RRankTree> selectedRankTree = initialState("rankTree");
	private final State<Boolean>   previewMode      = initialState("previewMode");

	private RankRequirementProgressManager progressManager;

	public RankPathRankRequirementOverview() {
		super(RankPathOverview.class);
	}

	@Override
	protected String getKey() {
		return "rank_requirements_overview_ui";
	}

	@Override
	protected @NotNull Map<String, Object> getTitlePlaceholders(
		final @NotNull OpenContext openContext
	) {
		final RRank rank = targetRank.get(openContext);
		return Map.of(
			"rank_name", rank.getIdentifier(),
			"requirement_count", rank.getUpgradeRequirements().size()
		);
	}

	@Override
	protected CompletableFuture<List<RRankUpgradeRequirement>> getAsyncPaginationSource(
		final @NotNull Context context
	) {
		return CompletableFuture.supplyAsync(() -> {
			final RRank rank = targetRank.get(context);
			if (rank == null) {
				LOGGER.warning("Target rank is null, returning empty requirements list");
				return List.of();
			}

			if (progressManager == null) {
				progressManager = new RankRequirementProgressManager(rdq.get(context));
			}

			return rank.getUpgradeRequirements().stream()
				.sorted(Comparator.comparingInt(RRankUpgradeRequirement::getDisplayOrder))
				.toList();
		});
	}

	@Override
	protected void renderEntry(
		final @NotNull Context context,
		final @NotNull BukkitItemComponentBuilder builder,
		final int index,
		final @NotNull RRankUpgradeRequirement requirement
	) {
		final Player player = context.getPlayer();
		final RDQPlayer rdqPlayer = currentPlayer.get(context);
		final boolean isPreviewMode = previewMode.get(context);

		final RequirementProgressData progress = progressManager.getRequirementProgress(player, rdqPlayer, requirement);
		final ItemStack displayItem = createRequirementDisplayItem(player, requirement, progress, index, isPreviewMode);

		builder.renderWith(() -> displayItem)
			.onClick(clickContext -> handleRequirementClick(clickContext, requirement, progress, isPreviewMode));
	}

	@Override
	protected void onPaginatedRender(
		final @NotNull RenderContext render,
		final @NotNull Player player
	) {
		final RRank rank = targetRank.get(render);
		if (rank == null) {
			LOGGER.warning("Target rank is null during render");
			return;
		}

		if (progressManager == null) {
			progressManager = new RankRequirementProgressManager(rdq.get(render));
		}

		final RDQPlayer rdqPlayer = currentPlayer.get(render);
		progressManager.initializeRankProgressTracking(rdqPlayer, rank);
	}

	@Override
	public void onResume(
		final @NotNull Context origin,
		final @NotNull Context target
	) {
		if (progressManager != null) {
			final Player player = target.getPlayer();
			final RDQPlayer rdqPlayer = currentPlayer.get(target);
			final RRank rank = targetRank.get(target);
			progressManager.refreshRankProgress(player, rdqPlayer, rank);
		}
		target.update();
	}

	@Override
	public void onClick(final @NotNull SlotClickContext click) {
		if (progressManager != null) {
			progressManager.cleaRDQPlayerCache(click.getPlayer());
		}
	}

	@Override
	public void onClose(final @NotNull CloseContext close) {
		if (progressManager != null) {
			progressManager.cleaRDQPlayerCache(close.getPlayer());
		}

		final Map<String, Object> map = new HashMap<>((Map<String, Object>) close.getInitialData());
		map.put("view_closed_with_updates", true);
		close.setInitialData(map);
	}

	private @NotNull ItemStack createRequirementDisplayItem(
		final @NotNull Player player,
		final @NotNull RRankUpgradeRequirement requirement,
		final @NotNull RequirementProgressData progress,
		final int index,
		final boolean isPreviewMode
	) {
		final Material iconMaterial = Material.valueOf(requirement.getIcon().getMaterial());
		final Component displayName = createRequirementDisplayName(player, requirement, index, progress, isPreviewMode);
		final List<Component> lore = createRequirementLore(player, requirement, progress, isPreviewMode);

		var builder = UnifiedBuilderFactory.item(iconMaterial)
			.setName(displayName)
			.setLore(lore)
			.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

		if (progress.getStatus() == RequirementStatus.COMPLETED) {
			builder.setGlowing(true);
		}

		return builder.build();
	}

	private @NotNull Component createRequirementDisplayName(
		final @NotNull Player player,
		final @NotNull RRankUpgradeRequirement requirement,
		final int index,
		final @NotNull RequirementProgressData progress,
		final boolean isPreviewMode
	) {
		final String statusKey = switch (progress.getStatus()) {
			case COMPLETED -> "requirement.status.completed";
			case READY_TO_COMPLETE -> "requirement.status.ready_to_complete";
			case IN_PROGRESS -> "requirement.status.in_progress";
			case NOT_STARTED -> "requirement.status.not_started";
			case ERROR -> "requirement.status.error";
		};

		var i18n = i18n(statusKey, player)
			.with("index", index + 1)
			.with("requirement_name", getRequirementName(requirement))
			.with("item_count", getRequirementItemCount(requirement))
			.with("progress", progress.getFormattedProgress());

		if (isPreviewMode) {
			i18n.with("preview_prefix", "[PREVIEW] ");
		}

		return i18n.build().component();
	}

	private @NotNull List<Component> createRequirementLore(
		final @NotNull Player player,
		final @NotNull RRankUpgradeRequirement requirement,
		final @NotNull RequirementProgressData progress,
		final boolean isPreviewMode
	) {
		final List<Component> lore = new ArrayList<>();

		lore.add(i18n("requirement.type", player)
			.with("type", progress.getRequirementType())
			.build()
			.component());

		lore.add(i18n("requirement.lore", player)
			.with("description", getRequirementDescription(requirement))
			.build()
			.component());

		final int itemCount = getRequirementItemCount(requirement);
		if (itemCount > 1) {
			lore.add(i18n("requirement.item_count", player)
				.with("count", itemCount)
				.build()
				.component());
		}

		lore.add(Component.empty());
		lore.add(i18n("requirement.progress", player)
			.with("progress", progress.getProgressAsPercentage())
			.build()
			.component());

		lore.add(switch (progress.getStatus()) {
			case COMPLETED -> i18n("requirement.info.completed", player).build().component();
			case READY_TO_COMPLETE -> i18n("requirement.info.ready_to_complete", player).build().component();
			case IN_PROGRESS -> i18n("requirement.info.in_progress", player).build().component();
			case NOT_STARTED -> i18n("requirement.info.not_started", player).build().component();
			case ERROR -> i18n("requirement.info.error", player).build().component();
		});

		lore.add(Component.empty());

		if (isPreviewMode) {
			lore.add(i18n("requirement.preview_mode", player).build().component());
		} else {
			addClickInstructions(lore, player, progress.getStatus());
		}

		return lore;
	}

	private void addClickInstructions(
		final @NotNull List<Component> lore,
		final @NotNull Player player,
		final @NotNull RequirementStatus status
	) {
		switch (status) {
			case READY_TO_COMPLETE -> {
				lore.add(i18n("requirement.click.complete", player).build().component());
				lore.add(i18n("requirement.click.details", player).build().component());
			}
			case COMPLETED -> lore.add(i18n("requirement.click.already_completed", player).build().component());
			default -> lore.add(i18n("requirement.click.for_details", player).build().component());
		}
	}

	private void handleRequirementClick(
		final @NotNull SlotClickContext clickContext,
		final @NotNull RRankUpgradeRequirement requirement,
		final @NotNull RequirementProgressData progress,
		final boolean isPreviewMode
	) {
		if (isPreviewMode) {
			i18n("messages.preview_mode_click", clickContext.getPlayer())
				.with("requirement_name", getRequirementName(requirement))
				.withPrefix()
				.send();
			return;
		}

		switch (clickContext.getClickOrigin().getClick()) {
			case LEFT -> handleLeftClick(clickContext, requirement, progress);
			case RIGHT -> handleRightClick(clickContext, requirement, progress);
			default -> {}
		}
	}

	private void handleLeftClick(
		final @NotNull SlotClickContext clickContext,
		final @NotNull RRankUpgradeRequirement requirement,
		final @NotNull RequirementProgressData progress
	) {
		final Player player = clickContext.getPlayer();

		if (progress.getStatus() == RequirementStatus.COMPLETED) {
			i18n("messages.already_completed", player)
				.with("requirement_name", getRequirementName(requirement))
				.withPrefix()
				.send();
			return;
		}

		if (progress.getStatus() != RequirementStatus.READY_TO_COMPLETE) {
			i18n("messages.not_ready_to_complete", player)
				.with("requirement_name", getRequirementName(requirement))
				.withPrefix()
				.send();
			return;
		}

		final RDQPlayer rdqPlayer = currentPlayer.get(clickContext);
		final RequirementCompletionResult result = progressManager.attemptRequirementCompletion(player, rdqPlayer, requirement);

		if (result.isSuccess()) {
			clickContext.update();

			final RRank rank = targetRank.get(clickContext);
			if (progressManager.areAllRequirementsCompleted(player, rdqPlayer, rank)) {
				LOGGER.info("All requirements completed for rank %s by player %s".formatted(rank.getIdentifier(), player.getName()));
				processRankProgression(clickContext, rank, rdqPlayer, player);
			}
		}
	}

	private void processRankProgression(
		final @NotNull SlotClickContext clickContext,
		final @NotNull RRank rank,
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull Player player
	) {
		final RDQ plugin = rdq.get(clickContext);
		final RRankTree rankTree = selectedRankTree.get(clickContext);

		updatePlayerRankInDatabase(plugin, rdqPlayer, rankTree, rank);
		updateLuckPermsGroup(plugin, player, rank);

		i18n("messages.rank_progression_completed", player)
			.with("rank_name", rank.getIdentifier())
			.withPrefix()
			.send();

		final Map<String, Object> updatedData = new HashMap<>((Map<String, Object>) clickContext.getInitialData());
		updatedData.put("rank_progression_completed", true);
		updatedData.put("completed_rank", rank);
		updatedData.put("new_rank_id", rank.getIdentifier());

		scheduleReturnToParent(clickContext, updatedData);

		LOGGER.info("Rank progression completed: %s -> %s".formatted(player.getName(), rank.getIdentifier()));
	}

	private void updatePlayerRankInDatabase(
		final @NotNull RDQ plugin,
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRankTree rankTree,
		final @NotNull RRank newRank
	) {
		final List<RPlayerRank> playerRanks = plugin.getPlayerRankRepository()
			.findListByAttributes(Map.of("player.uniqueId", rdqPlayer.getUniqueId()));

		playerRanks.stream()
			.filter(rank -> Objects.equals(rank.getRankTree(), rankTree))
			.findFirst()
			.ifPresentOrElse(
				existingRank -> {
					existingRank.setCurrentRank(newRank);
					plugin.getPlayerRankRepository().update(existingRank);
				},
				() -> {
					final RPlayerRank newPlayerRank = new RPlayerRank(rdqPlayer, newRank, rankTree);
					plugin.getPlayerRankRepository().create(newPlayerRank);
				}
			);
	}

	private void updateLuckPermsGroup(
		final @NotNull RDQ plugin,
		final @NotNull Player player,
		final @NotNull RRank rank
	) {
		if (plugin.getLuckPermsService() != null) {
			final String luckPermsGroup = rank.getAssignedLuckPermsGroup();
			if (!luckPermsGroup.isEmpty()) {
				// LuckPerms integration would happen here
			}
		}
	}

	private void scheduleReturnToParent(
		final @NotNull SlotClickContext clickContext,
		final @NotNull Map<String, Object> updatedData
	) {
		rdq.get(clickContext).getPlatform().getScheduler().runDelayed(
			() -> clickContext.openForPlayer(RankPathOverview.class, updatedData),
			RETURN_DELAY_TICKS
		);
	}

	private void handleRightClick(
		final @NotNull SlotClickContext clickContext,
		final @NotNull RRankUpgradeRequirement requirement,
		final @NotNull RequirementProgressData progress
	) {
		final Map<String, Object> initialData = new HashMap<>((Map<String, Object>) clickContext.getInitialData());
		initialData.put("requirement", requirement);
		initialData.put("progress", progress);

		clickContext.openForPlayer(RankRequirementDetailView.class, initialData);
	}

	private @NotNull String getRequirementName(final @NotNull RRankUpgradeRequirement requirement) {
		return requirement.getRequirement().getRequirement().getType().name();
	}

	private @NotNull String getRequirementDescription(final @NotNull RRankUpgradeRequirement requirement) {
		return requirement.getRequirement().getRequirement().getDescriptionKey();
	}

	private int getRequirementItemCount(final @NotNull RRankUpgradeRequirement requirement) {
		return 1;
	}
}*/
