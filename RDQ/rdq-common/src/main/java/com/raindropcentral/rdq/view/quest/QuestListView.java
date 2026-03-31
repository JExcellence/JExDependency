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

package com.raindropcentral.rdq.view.quest;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.database.entity.quest.QuestCategory;
import com.raindropcentral.rdq.database.entity.quest.QuestReward;
import com.raindropcentral.rdq.database.entity.quest.QuestTask;
import com.raindropcentral.rdq.model.quest.QuestStartResult;
import com.raindropcentral.rdq.service.quest.QuestService;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Paginated view displaying quests within a specific category.
 * <p>
 * Shows all available quests in the selected category with their status,
 * difficulty, rewards preview, and allows players to start or view quest details.
 * Uses the new I18n structure with proper quest.{quest_id}.name keys.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestListView extends APaginatedView<Quest> {
	
	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
	
	private final State<RDQ> rdq = initialState("plugin");
	private final State<QuestCategory> category = initialState("category");
	
	public QuestListView() {
		super(QuestCategoryView.class);
	}
	
	@Override
	protected String getKey() {
		return "view.quest.list";
	}
	
	@Override
	protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext open) {
		final QuestCategory cat = category.get(open);
		return Map.of("category", cat != null ? cat.getIdentifier() : "Unknown");
	}
	
	@Override
	protected CompletableFuture<List<Quest>> getAsyncPaginationSource(final @NotNull Context context) {
		final RDQ plugin = rdq.get(context);
		final QuestCategory cat = category.get(context);
		
		if (cat == null) {
			return CompletableFuture.completedFuture(List.of());
		}
		
		LOGGER.info("Loading quests for category: " + cat.getIdentifier());

		// Get quests from cache manager (instant access)
		final List<Quest> quests = plugin.getQuestCacheManager().getQuestsByCategory(cat.getIdentifier());

		LOGGER.info("Loaded " + quests.size() + " quests from cache");

		// Return instantly as CompletableFuture
		return CompletableFuture.completedFuture(quests);
	}
	
	@Override
	protected void renderEntry(
			final @NotNull Context context,
			final @NotNull BukkitItemComponentBuilder builder,
			final int index,
			final @NotNull Quest quest
	) {
		final Player player = context.getPlayer();
		final RDQ plugin = rdq.get(context);
		final QuestService questService = plugin.getQuestService();
		
		builder.renderWith(() -> {
			final Material material = Material.BOOK; // Default material for quest list

			// Quest-specific keys use absolute paths (quest.{id}.name)
			final Component name = new I18n.Builder("quest." + quest.getIdentifier() + ".name", player).build().component();

			// Build comprehensive lore
			final List<Component> lore = new ArrayList<>();

			// Add quest description (absolute key)
			lore.addAll(new I18n.Builder("quest." + quest.getIdentifier() + ".description", player).build().children());
			lore.add(Component.empty());

			// Add difficulty with color (absolute key under quest.difficulty.*)
			final String difficultyKey = "quest.difficulty." + quest.getDifficulty().name().toLowerCase();
			lore.add(new I18n.Builder(difficultyKey, player).build().component());

			// Add task count (relative to view.quest.list)
			lore.add(i18n("task_count", player)
					.withPlaceholder("count", String.valueOf(quest.getTasks().size()))
					.build()
					.component());

			// Add rewards preview
			if (!quest.getRewards().isEmpty()) {
				lore.add(Component.empty());
				lore.add(i18n("rewards", player).build().component());

				// Show first 3 rewards
				int rewardCount = 0;
				for (QuestReward reward : quest.getRewards()) {
					if (rewardCount >= 3) {
						final int remaining = quest.getRewards().size() - 3;
						lore.add(i18n("more_rewards", player)
								.withPlaceholder("count", String.valueOf(remaining))
								.build()
								.component());
						break;
					}
					lore.add(formatRewardPreview(player, reward));
					rewardCount++;
				}
			}

			// Add prerequisites if any
			if (quest.hasPrerequisites()) {
				lore.add(Component.empty());
				lore.add(i18n("prerequisites", player).build().component());
				for (String prereq : quest.getPreviousNodeIdentifiers()) {
					lore.add(Component.text("  §7- " + prereq));
				}
			}

			// Add status (will be updated async)
			lore.add(Component.empty());
			lore.add(new I18n.Builder("quest.status.loading", player).build().component());
			
			return UnifiedBuilderFactory.item(material)
					.setName(name)
					.setLore(lore)
					.build();
		}).onClick(click -> handleQuestClick(click, quest));
	}
	
	/**
	 * Formats a reward for preview display in the quest list.
	 *
	 * @param player the player viewing the quest
	 * @param reward the reward to format
	 * @return formatted reward component
	 */
	private Component formatRewardPreview(final @NotNull Player player, final @NotNull QuestReward reward) {
		// Use the reward's description or estimated value for display
		String description = reward.getReward().getDescription();
		if (description != null && !description.isBlank()) {
			return Component.text("§7" + description);
		}

		// Fallback to estimated value
		double value = reward.getEstimatedValue();
		if (value > 0) {
			return new I18n.Builder("view.quest.reward.value", player)
					.withPlaceholder("value", String.format("%.0f", value))
					.build()
					.component();
		}

		return Component.text("§7Reward");
	}

	@Override
	protected void onPaginatedRender(final @NotNull RenderContext render, final @NotNull Player player) {
		// Additional UI elements can be added here if needed
	}
	
	private void handleQuestClick(
			final @NotNull SlotClickContext click,
			final @NotNull Quest quest
	) {
		final Player player = click.getPlayer();
		final RDQ plugin = rdq.get(click);
		final QuestService questService = plugin.getQuestService();
		
		// Check if quest is active
		questService.isQuestActive(player.getUniqueId(), quest.getIdentifier())
				.thenAccept(isActive -> {
					if (isActive) {
						// Open quest detail view
						click.closeForPlayer();
						plugin.getViewFrame().open(QuestDetailView.class, player, Map.of(
								"plugin", plugin,
								"quest", quest
						));
					} else {
						// Try to start the quest
						questService.startQuest(player.getUniqueId(), quest.getIdentifier())
								.thenAccept(result -> {
									handleQuestStartResult(player, result, quest);
									if (result.success()) {
										// Refresh view to show updated status
										click.update();
									}
								})
								.exceptionally(ex -> {
									LOGGER.log(Level.SEVERE, "Error starting quest for player " + player.getName(), ex);
									new I18n.Builder("quest.general.error", player).build().sendMessage();
									return null;
								});
					}
				});
	}
	
	private void handleQuestStartResult(
			final @NotNull Player player,
			final @NotNull QuestStartResult result,
			final @NotNull Quest quest
	) {
		if (result.success()) {
			new I18n.Builder("quest.notification.started", player)
					.withPlaceholder("quest", quest.getDisplayName())
					.build()
					.sendMessage();
		} else {
			// Show failure reason
			new I18n.Builder("quest.command.start.failed", player)
					.withPlaceholder("reason", result.failureReason())
					.build()
					.sendMessage();
		}
	}
}
