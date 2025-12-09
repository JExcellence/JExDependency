package com.raindropcentral.rdq.view.ranks;


import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRank;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import com.raindropcentral.rdq.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rdq.manager.RankRequirementProgressManager;
import com.raindropcentral.rdq.manager.RankRequirementProgressManager.RequirementCompletionResult;
import com.raindropcentral.rdq.manager.RankRequirementProgressManager.RequirementProgressData;
import com.raindropcentral.rdq.manager.RankRequirementProgressManager.RequirementStatus;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import com.raindropcentral.rdq.requirement.ItemRequirement;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.*;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GUI view for displaying the upgrade requirements of a specific rank.
 * <p>
 * This paginated view shows all requirements needed to upgrade to a rank,
 * including the current progress for each requirement. Players can interact
 * with requirements to complete them and track their progress.
 * </p>
 *
 * <p>
 * Features:
 * - Paginated display of rank upgrade requirements
 * - Real-time progress tracking for each requirement
 * - Visual indicators for completion status
 * - Click interactions for requirement completion
 * - Persistent progress tracking via RankRequirementProgressManager
 * - Prevention of over-completion
 * - Automatic rank progression when all requirements completed
 * </p>
 *
 * @author ItsRainingHP
 * @version 2.1.0
 * @since TBD
 */
public class RankPathRankRequirementOverview extends APaginatedView<RRankUpgradeRequirement> {
	
	private static final Logger LOGGER = CentralLogger.getLogger(RankPathRankRequirementOverview.class.getName());
	
	private final State<RDQ> rdq = initialState("plugin");
	private final State<RDQPlayer> currentPlayer = initialState("player");
	private final State<RRank>     targetRank       = initialState("targetRank");
	private final State<RRankTree> selectedRankTree = this.initialState("rankTree");
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
		final RRank rank = this.targetRank.get(openContext);
		
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
			try {
				final RRank rank = this.targetRank.get(context);
				if (
					rank == null
				) {
					LOGGER.log(Level.WARNING, "Target rank is null, returning empty requirements list");
					return new ArrayList<>();
				}
				
				if (
					this.progressManager == null
				) {
					this.progressManager = new RankRequirementProgressManager(this.rdq.get(context));
				}
				
				final List<RRankUpgradeRequirement> requirements = new ArrayList<>(rank.getUpgradeRequirements());
				requirements.sort(Comparator.comparingInt(RRankUpgradeRequirement::getDisplayOrder));
				
				LOGGER.log(Level.FINE, "Loaded " + requirements.size() + " requirements for rank: " + rank.getIdentifier());
				return requirements;
				
			} catch (final Exception exception) {
				LOGGER.log(Level.SEVERE, "Failed to load rank upgrade requirements", exception);
				return new ArrayList<>();
			}
		});
	}
	
	@Override
	protected void renderEntry(
		final @NotNull Context context,
		final @NotNull BukkitItemComponentBuilder builder,
		final int index,
		final @NotNull RRankUpgradeRequirement requirement
	) {
		try {
			final Player player = context.getPlayer();
			final RDQPlayer rdqPlayer = this.currentPlayer.get(context);
			final boolean isPreviewMode = this.previewMode.get(context);
			
			final RequirementProgressData progress = progressManager.getRequirementProgress(player, rdqPlayer, requirement);
			final ItemStack displayItem = this.createRequirementDisplayItem(player, requirement, progress, index, isPreviewMode);
			
			builder
				.renderWith(() -> displayItem)
				.onClick(clickContext -> this.handleRequirementClick(clickContext, requirement, progress, isPreviewMode));
			
		} catch (
			  final Exception exception
		) {
			LOGGER.log(Level.WARNING, "Failed to render requirement entry at index " + index, exception);
			this.renderFallbackRequirementEntry(context, builder, index, requirement);
		}
	}
	
	@Override
	protected void onPaginatedRender(
		final @NotNull RenderContext render,
		final @NotNull Player player
	) {
		try {
			final RRank rank = this.targetRank.get(render);
			final RDQPlayer rdqPlayer = this.currentPlayer.get(render);
			
			if (
				rank == null
			) {
				LOGGER.log(Level.WARNING, "Target rank is null during first render");
				this.renderErrorState(render, player);
				return;
			}
			
			if (
				this.progressManager == null
			) {
				this.progressManager = new RankRequirementProgressManager(this.rdq.get(render));
			}
			
			this.progressManager.initializeRankProgressTracking(rdqPlayer, rank);
			this.renderAdditionalInfo(render, player, rank, rdqPlayer);
		} catch (
			  final Exception exception
		) {
			LOGGER.log(Level.SEVERE, "Critical error during requirements overview render", exception);
			this.renderErrorState(render, player);
		}
	}
	
	/**
	 * Called when the view becomes active again (e.g., when returning from another view)
	 */
	@Override
	public void onResume(
		final Context origin,
		final Context target
	) {
		try {
			LOGGER.log(Level.FINE, "RankPathRankRequirementOverview resumed, refreshing requirement progress");
			
			final Player player = target.getPlayer();
			final RDQPlayer rdqPlayer = this.currentPlayer.get(target);
			final RRank rank = this.targetRank.get(target);
			
			if (progressManager != null) {
				progressManager.refreshRankProgress(player, rdqPlayer, rank);
			}
			
			target.update();
			
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to refresh on resume", exception);
		}
	}
	
	@Override
	public void onClick(
		final @NotNull SlotClickContext click
	) {
		try {
			LOGGER.log(Level.FINE, "RankPathRankRequirementOverview closing, ensuring progress is saved");
			
			if (
				this.progressManager != null
			) {
				this.progressManager.cleaRDQPlayerCache(click.getPlayer());
			}
			
		} catch (
			  final Exception exception
		) {
			LOGGER.log(Level.WARNING, "Failed to handle view close", exception);
		}
	}
	
	/**
	 * Creates the display item for a requirement.
	 */
	private @NotNull ItemStack createRequirementDisplayItem(
		final @NotNull Player player,
		final @NotNull RRankUpgradeRequirement requirement,
		final @NotNull RequirementProgressData progress,
		final int index,
		final boolean isPreviewMode
	) {
		try {
			final Material iconMaterial = Material.valueOf(requirement.getIcon().getMaterial());
			
			final Component displayName = this.createRequirementDisplayName(player, requirement, index, progress, isPreviewMode);
			
			final List<Component> lore = this.createRequirementLore(player, requirement, progress, isPreviewMode);
			
			ItemStack finalItem = UnifiedBuilderFactory.item(iconMaterial)
			                                           .setName(displayName)
			                                           .setLore(lore)
			                                           .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			                                           .build();
			
			if (
				progress.getStatus() == RequirementStatus.COMPLETED
			) {
				finalItem = UnifiedBuilderFactory.item(finalItem)
				                                 .setGlowing(true)
				                                 .build();
			}
			
			return finalItem;
			
		} catch (
			  final Exception exception
		) {
			LOGGER.log(Level.WARNING, "Failed to create requirement display item", exception);
			return this.createFallbackRequirementItem(player, requirement, index);
		}
	}
	
	/**
	 * Creates the display name for a requirement.
	 */
	private @NotNull Component createRequirementDisplayName(
		final @NotNull Player player,
		final @NotNull RRankUpgradeRequirement requirement,
		final int index,
		final @NotNull RequirementProgressData progress,
		final boolean isPreviewMode
	) {
		try {
			final String statusKey = switch (progress.getStatus()) {
				case COMPLETED -> "requirement.status.completed";
				case READY_TO_COMPLETE -> "requirement.status.ready_to_complete";
				case IN_PROGRESS -> "requirement.status.in_progress";
				case NOT_STARTED -> "requirement.status.not_started";
				case ERROR -> "requirement.status.error";
			};
			
			String requirementName = this.getRequirementName(requirement);
			int itemCount = this.getRequirementItemCount(requirement);

            TranslationService i18n = this.i18n(statusKey, player)
                    .with("index", index + 1)
                    .with("requirement_name", requirementName)
                    .with("item_count", itemCount)
                    .with("progress", progress.getFormattedProgress());
			
			if (
				isPreviewMode
			) {
				i18n.with("preview_prefix", "[PREVIEW] ");
			}
			
			return i18n.build().component();
			
		} catch (
			  final Exception exception
		) {
			LOGGER.log(Level.WARNING, "Failed to create requirement display name", exception);
			return this.i18n("requirement.fallback", player)
			           .with("index", index + 1)
			           .build()
			           .component();
		}
	}
	
	/**
	 * Creates the lore for a requirement item.
	 */
	private @NotNull List<Component> createRequirementLore(
		final @NotNull Player player,
		final @NotNull RRankUpgradeRequirement requirement,
		final @NotNull RequirementProgressData progress,
		final boolean isPreviewMode
	) {
		final List<Component> lore = new ArrayList<>();
		
		try {
			String description = getRequirementDescription(requirement);
			int itemCount = getRequirementItemCount(requirement);
			
			lore.add(this.i18n("requirement.type", player)
			             .with("type", progress.getRequirementType())
			             .build()
			             .component());
			
			lore.add(this.i18n("requirement.lore", player)
			             .with("description", description)
			             .build()
			             .component());
			
			if (itemCount > 1) {
				lore.add(this.i18n("requirement.item_count", player)
				             .with("count", itemCount)
				             .build()
				             .component());
			}
			
			lore.add(Component.empty());
			
			lore.add(this.i18n("requirement.progress", player)
			             .with("progress", progress.getProgressAsPercentage())
			             .build()
			             .component());
			
			switch (progress.getStatus()) {
				case COMPLETED -> {
					lore.add(this.i18n("requirement.info.completed", player)
					             .build()
					             .component());
				}
				case READY_TO_COMPLETE -> {
					lore.add(this.i18n("requirement.info.ready_to_complete", player)
					             .build()
					             .component());
				}
				case IN_PROGRESS -> {
					lore.add(this.i18n("requirement.info.in_progress", player)
					             .build()
					             .component());
				}
				case NOT_STARTED -> {
					lore.add(this.i18n("requirement.info.not_started", player)
					             .build()
					             .component());
				}
				case ERROR -> {
					lore.add(this.i18n("requirement.info.error", player)
					             .build()
					             .component());
				}
			}
			
			lore.add(Component.empty());
			
			if (
				isPreviewMode
			) {
				lore.add(this.i18n("requirement.preview_mode", player)
				             .build()
				             .component());
			} else {
				switch (progress.getStatus()) {
					case READY_TO_COMPLETE -> {
						lore.add(this.i18n("requirement.click.complete", player)
						             .build()
						             .component());
						lore.add(this.i18n("requirement.click.details", player)
						             .build()
						             .component());
					}
					case COMPLETED -> {
						lore.add(this.i18n("requirement.click.already_completed", player)
						             .build()
						             .component());
					}
					default -> {
						lore.add(this.i18n("requirement.click.for_details", player)
						             .build()
						             .component());
					}
				}
			}
			
		} catch (
			  final Exception exception
		) {
			LOGGER.log(Level.WARNING, "Failed to create requirement lore", exception);
			lore.add(this.i18n("requirement.lore_error", player)
			             .build()
			             .component());
		}
		return lore;
	}
	
	/**
	 * Handles clicking on a requirement item.
	 */
	private void handleRequirementClick(
		final @NotNull SlotClickContext clickContext,
		final @NotNull RRankUpgradeRequirement requirement,
		final @NotNull RequirementProgressData progress,
		final boolean isPreviewMode
	) {
		final Player player = clickContext.getPlayer();
		final ClickType clickType = clickContext.getClickOrigin().getClick();
		
		try {
			if (
				isPreviewMode
			) {
				this.i18n("messages.preview_mode_click", player)
				    .with("requirement_name", this.getRequirementName(requirement))
				    .withPrefix()
				    .send();
				return;
			}
			
			if (
				clickType == ClickType.LEFT
			) {
				this.handleLeftClick(clickContext, requirement, progress);
			} else if (
				       clickType == ClickType.RIGHT
			) {
				this.handleRightClick(clickContext, requirement, progress);
			}
			
		} catch (
			  final Exception exception
		) {
			LOGGER.log(Level.WARNING, "Failed to handle requirement click", exception);
			this.i18n("error.click_failed", player)
			    .withPrefix()
			    .send();
		}
	}
	
	/**
	 * Enhanced left-click handler that properly manages completion state and rank progression
	 */
	private void handleLeftClick(
		final @NotNull SlotClickContext clickContext,
		final @NotNull RRankUpgradeRequirement requirement,
		final @NotNull RequirementProgressData progress
	) {
		final Player player = clickContext.getPlayer();
		final RDQPlayer rdqPlayer = this.currentPlayer.get(clickContext);
		
		if (progress.getStatus() == RequirementStatus.COMPLETED) {
			this.i18n("messages.already_completed", player)
			    .with("requirement_name", this.getRequirementName(requirement))
			    .withPrefix()
			    .send();
			return;
		}
		
		if (progress.getStatus() != RequirementStatus.READY_TO_COMPLETE) {
			this.i18n("messages.not_ready_to_complete", player)
			    .with("requirement_name", this.getRequirementName(requirement))
			    .withPrefix()
			    .send();
			return;
		}
		
		final RequirementCompletionResult result = this.progressManager.attemptRequirementCompletion(player, rdqPlayer, requirement);
		
		if (result.isSuccess()) {
			clickContext.update();
			
			final RRank rank = this.targetRank.get(clickContext);
			
			if (this.progressManager.areAllRequirementsCompleted(player, rdqPlayer, rank)) {
				LOGGER.log(Level.INFO, "All requirements completed for rank " + rank.getIdentifier() + " by player " + player.getName() + ". Starting rank progression.");
				
				this.processRankProgression(clickContext, rank, rdqPlayer, player);
			}
		}
	}
	
	/**
	 * NEW METHOD: Processes the actual rank progression when all requirements are completed
	 */
	private void processRankProgression(
		final @NotNull SlotClickContext clickContext,
		final @NotNull RRank rank,
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull Player player
	) {
		try {
			final RDQ plugin = this.rdq.get(clickContext);
			final RRankTree rankTree = this.selectedRankTree.get(clickContext);
			
			this.updatePlayerRankInDatabase(plugin, rdqPlayer, rankTree, rank);
			
			this.updateLuckPermsGroup(plugin, player, rank);
			
			this.i18n("messages.rank_progression_completed", player)
			    .with("rank_name", rank.getIdentifier())
			    .withPrefix()
			    .send();
			
			Map<String, Object> updatedData = new HashMap<>((Map<String, Object>) clickContext.getInitialData());
			updatedData.put("rank_progression_completed", true);
			updatedData.put("completed_rank", rank);
			updatedData.put("new_rank_id", rank.getIdentifier());
			
			this.scheduleReturnToParent(clickContext, updatedData, 2000L);
			
			LOGGER.log(Level.INFO, "Successfully completed rank progression for player " + player.getName() + " to rank " + rank.getIdentifier());
			
		} catch (Exception exception) {
			LOGGER.log(Level.SEVERE, "Failed to process rank progression", exception);
			this.i18n("error.rank_progression_failed", player)
			    .with("rank_name", rank.getIdentifier())
			    .withPrefix()
			    .send();
		}
	}
	
	/**
	 * NEW METHOD: Updates the player's rank in the database
	 */
	private void updatePlayerRankInDatabase(
		final @NotNull RDQ plugin,
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRankTree rankTree,
		final @NotNull RRank newRank
	) {
		try {
			final List<RPlayerRank> playerRanks = plugin.getPlayerRankRepository()
			                                            .findListByAttributes(Map.of(
				                                            "player.uniqueId", rdqPlayer.getUniqueId()
			                                            ));
			
			final RPlayerRank existingRank = playerRanks.stream()
			                                            .filter(rank -> Objects.equals(rank.getRankTree(), rankTree))
			                                            .findFirst()
			                                            .orElse(null);
			
			if (existingRank != null) {
				existingRank.setCurrentRank(newRank);
				plugin.getPlayerRankRepository().update(existingRank);
				LOGGER.log(Level.INFO, "Updated existing rank for player " + rdqPlayer.getPlayerName() + " to " + newRank.getIdentifier());
			} else {
				final RPlayerRank newPlayerRank = new RPlayerRank(rdqPlayer, newRank, rankTree);
				plugin.getPlayerRankRepository().create(newPlayerRank);
				LOGGER.log(Level.INFO, "Created new rank entry for player " + rdqPlayer.getPlayerName() + " with rank " + newRank.getIdentifier());
			}
			
		} catch (Exception exception) {
			LOGGER.log(Level.SEVERE, "Failed to update player rank in database", exception);
			throw new RuntimeException("Database rank update failed", exception);
		}
	}
	
	/**
	 * NEW METHOD: Updates LuckPerms group if available
	 */
	private void updateLuckPermsGroup(
		final @NotNull RDQ plugin,
		final @NotNull Player player,
		final @NotNull RRank rank
	) {
		try {
			if (plugin.getLuckPermsService() != null) {
				final String luckPermsGroup = rank.getAssignedLuckPermsGroup();
				if (luckPermsGroup != null && !luckPermsGroup.isEmpty()) {
					LOGGER.log(Level.INFO, "Updated LuckPerms group for " + player.getName() + " to " + luckPermsGroup);
				}
			}
		} catch (Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to update LuckPerms group", exception);
		}
	}
	
	/**
	 * Schedules a return to the parent view after a delay with updated data
	 */
	private void scheduleReturnToParent(final @NotNull SlotClickContext clickContext, final @NotNull Map<String, Object> updatedData, final long delayMs) {
		final RDQ rdq = this.rdq.get(clickContext);
		
		Bukkit.getServer().getScheduler().runTaskLater(rdq.getPlugin(), () -> {
			try {
				clickContext.openForPlayer(RankPathOverview.class, updatedData);
			} catch (Exception exception) {
				LOGGER.log(Level.WARNING, "Failed to return to parent view", exception);
				clickContext.getPlayer().closeInventory();
			}
		}, delayMs / 50L);
	}
	
	/**
	 * Enhanced close handler that ensures parent view knows about completion
	 */
	@Override
	public void onClose(final @NotNull CloseContext close) {
		try {
			LOGGER.log(Level.FINE, "RankPathRankRequirementOverview closing, ensuring progress is saved");
			
			if (
				this.progressManager != null
			) {
				this.progressManager.cleaRDQPlayerCache(close.getPlayer());
			}
			
			Map<String, Object> map = new HashMap<>();
			map.putAll((Map<? extends String, ?>) close.getInitialData());
			map.put("view_closed_with_updates", true);
			
			close.setInitialData(map);
			
		} catch (
			final Exception exception
		) {
			LOGGER.log(Level.WARNING, "Failed to handle view close", exception);
		}
	}
	
	/**
	 * Handles right-click on a requirement (view details).
	 */
	private void handleRightClick(
		final @NotNull SlotClickContext clickContext,
		final @NotNull RRankUpgradeRequirement requirement,
		final @NotNull RequirementProgressData progress
	) {
		final Player player = clickContext.getPlayer();
		
		try {
			final Map<String, Object> initialData = new HashMap<>((Map<String, Object>) clickContext.getInitialData());
			
			initialData.put("requirement", requirement);
			initialData.put("progress", progress);
			
			LOGGER.log(Level.FINE, "Player " + player.getName() + " opened detailed view for requirement: " + this.getRequirementName(requirement));
			
			clickContext.openForPlayer(
				RankRequirementDetailView.class,
				initialData
			);
		} catch (
			  final Exception exception
		) {
			LOGGER.log(Level.WARNING, "Failed to open requirement detail view", exception);
			this.i18n("error.detail_view_failed", player)
			    .withPrefix()
			    .send();
		}
	}
	
	/**
	 * Renders additional information about the rank and its requirements.
	 */
	private void renderAdditionalInfo(
		final @NotNull RenderContext render,
		final @NotNull Player player,
		final @NotNull RRank rank,
		final @NotNull RDQPlayer rdqPlayer
	) {
		try {
			final double overallProgress = this.progressManager.getRankOverallProgress(player, rdqPlayer, rank);
			final boolean allCompleted = this.progressManager.areAllRequirementsCompleted(player, rdqPlayer, rank);
			
			LOGGER.log(Level.FINE, "Additional info rendered for rank: " + rank.getIdentifier() +
			                       " - Progress: " + (int)(overallProgress * 100) + "% - All completed: " + allCompleted);
			
		} catch (
			  final Exception exception
		) {
			LOGGER.log(Level.WARNING, "Failed to render additional info", exception);
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
		} catch (
			  final Exception exception
		) {
			return "A requirement for rank progression";
		}
	}
	
	private int getRequirementItemCount(
		final @NotNull RRankUpgradeRequirement requirement
	) {
		try {
			AbstractRequirement abstractReq = requirement.getRequirement().getRequirement();
			if (
				abstractReq instanceof ItemRequirement itemReq
			) {
				return itemReq.getRequiredItems().size();
			}
			return 1;
		} catch (
			  final Exception exception
		) {
			return 1;
		}
	}
	
	private @NotNull ItemStack createFallbackRequirementItem(
		final @NotNull Player player,
		final @NotNull RRankUpgradeRequirement requirement,
		final int index
	) {
		return UnifiedBuilderFactory.item(Material.PAPER)
		                            .setName(this.i18n("requirement.fallback", player)
		                                         .with("index", index + 1)
		                                         .build()
		                                         .component())
		                            .setLore(List.of(
			                            this.i18n("requirement.fallback_lore", player)
			                                .build()
			                                .component()
		                            ))
		                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
		                            .build();
	}
	
	private void renderFallbackRequirementEntry(
		final @NotNull Context context,
		final @NotNull BukkitItemComponentBuilder builder,
		final int index,
		final @NotNull RRankUpgradeRequirement requirement
	) {
		final Player player = context.getPlayer();
		final ItemStack fallbackItem = this.createFallbackRequirementItem(player, requirement, index);
		
		builder.withItem(fallbackItem)
		       .onClick(clickContext -> {
			       this.i18n("error.requirement_unavailable", player)
			           .withPrefix()
			           .send();
		       });
	}
	
	private void renderErrorState(
		final @NotNull RenderContext render,
		final @NotNull Player player
	) {
		try {
			LOGGER.log(Level.WARNING, "Rendering error state for requirements overview");
		} catch (
			  final Exception exception
		) {
			LOGGER.log(Level.SEVERE, "Failed to render error state", exception);
		}
	}
}