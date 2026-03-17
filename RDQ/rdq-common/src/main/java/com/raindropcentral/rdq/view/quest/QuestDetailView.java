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
import com.raindropcentral.rdq.quest.model.QuestAbandonResult;
import com.raindropcentral.rdq.quest.model.QuestProgress;
import com.raindropcentral.rdq.quest.model.QuestStartResult;
import com.raindropcentral.rdq.quest.model.TaskProgress;
import com.raindropcentral.rdq.quest.service.QuestService;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Detailed view for a specific quest showing progress, tasks, and actions.
 *
 * <p>This view displays comprehensive information about a quest including:
 * - Quest description and details
 * - Task progress and requirements
 * - Available actions (start, abandon, view progress)
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestDetailView extends BaseView {
	
	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
	
	private final State<RDQ> rdq = initialState("plugin");
	private final State<Quest> quest = initialState("quest");
	
	private static final int QUEST_INFO_SLOT = 4;
	private static final int PROGRESS_SLOT = 22;
	private static final int[] TASK_SLOTS = {28, 29, 30, 31, 32, 33, 34};
	private static final int START_BUTTON_SLOT = 48;
	private static final int ABANDON_BUTTON_SLOT = 50;
	
	/**
	 * Executes QuestDetailView.
	 */
	public QuestDetailView() {
		super(QuestListView.class);
	}
	
	@Override
	protected String getKey() {
		return "view.quest.detail";
	}
	
	@Override
	protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext open) {
		final Quest q = quest.get(open);
		return Map.of("quest", q != null ? q.getIdentifier() : "Unknown");
	}
	
	@Override
	protected @NotNull String[] getLayout() {
		return new String[]{
				"XXXXXXXXX",
				"X       X",
				"X   P   X",
				"X       X",
				"XXXXXXXXX",
				"         "
		};
	}
	
	/**
	 * Executes onFirstRender.
	 */
	@Override
	public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
		final Quest q = quest.get(render);
		final RDQ plugin = rdq.get(render);
		
		if (q == null) {
			renderErrorState(render, player);
			return;
		}
		
		renderQuestInfo(render, player, q);
		renderQuestStatus(render, player, q, plugin);
	}
	
	private void renderErrorState(final @NotNull RenderContext render, final @NotNull Player player) {
		render.slot(QUEST_INFO_SLOT).renderWith(() -> {
			final Component errorName = new I18n.Builder("quest.general.error", player).build().component();
			return UnifiedBuilderFactory.item(Material.BARRIER)
					.setName(errorName)
					.build();
		});
	}
	
	private void renderQuestInfo(
			final @NotNull RenderContext render,
			final @NotNull Player player,
			final @NotNull Quest q
	) {
		render.slot(QUEST_INFO_SLOT).renderWith(() -> {
			final Material material = Material.valueOf(q.getIcon().getMaterial().toUpperCase());
			final Component name = new I18n.Builder(q.getIcon().getDisplayNameKey(), player).build().component();
			
			final List<Component> lore = new ArrayList<>();
			lore.addAll(new I18n.Builder(q.getIcon().getDescriptionKey(), player).build().children());
			lore.add(Component.empty());
			
			// Add difficulty
			final String difficultyKey = "quest.difficulty." + q.getDifficulty().name().toLowerCase();
			lore.add(new I18n.Builder(difficultyKey, player).build().component());
			
			// Add repeatable info
			if (q.isRepeatable()) {
				lore.add(new I18n.Builder("quest.command.info.repeatable", player)
						.withPlaceholder("repeatable", "Yes")
						.build()
						.component());
			}
			
			return UnifiedBuilderFactory.item(material)
					.setName(name)
					.setLore(lore)
					.build();
		});
	}
	
	private void renderQuestStatus(
			final @NotNull RenderContext render,
			final @NotNull Player player,
			final @NotNull Quest q,
			final @NotNull RDQ plugin
	) {
		final QuestService questService = plugin.getQuestService();
		
		questService.isQuestActive(player.getUniqueId(), q.getIdentifier())
				.thenAccept(isActive -> {
					if (isActive) {
						renderActiveQuestStatus(render, player, q, plugin);
					} else {
						renderInactiveQuestStatus(render, player, q, plugin);
					}
				})
				.exceptionally(ex -> {
					LOGGER.log(Level.SEVERE, "Error checking quest status", ex);
					return null;
				});
	}
	
	private void renderActiveQuestStatus(
			final @NotNull RenderContext render,
			final @NotNull Player player,
			final @NotNull Quest q,
			final @NotNull RDQ plugin
	) {
		final QuestService questService = plugin.getQuestService();
		
		// Render progress
		questService.getProgress(player.getUniqueId(), q.getIdentifier())
				.thenAccept(progressOpt -> {
					if (progressOpt.isPresent()) {
						renderProgress(render, player, progressOpt.get());
					}
				});
		
		// Render abandon button
		renderAbandonButton(render, player, q);
	}
	
	private void renderInactiveQuestStatus(
			final @NotNull RenderContext render,
			final @NotNull Player player,
			final @NotNull Quest q,
			final @NotNull RDQ plugin
	) {
		final QuestService questService = plugin.getQuestService();
		
		// Check if can start
		questService.canStartQuest(player.getUniqueId(), q.getIdentifier())
				.thenAccept(result -> {
					if (result instanceof QuestStartResult.Success) {
						renderStartButton(render, player, q);
					} else {
						renderCooldownInfo(render, player);
					}
				});
	}
	
	private void renderProgress(
			final @NotNull RenderContext render,
			final @NotNull Player player,
			final @NotNull QuestProgress progress
	) {
		// Render overall progress
		render.slot(PROGRESS_SLOT).renderWith(() -> {
			final Component name = new I18n.Builder("view.quest.progress.overall", player).build().component();
			final List<Component> lore = List.of(
					new I18n.Builder("view.quest.progress.percentage", player)
							.withPlaceholder("progress", String.valueOf(progress.getOverallProgressPercentage()))
							.build()
							.component(),
					new I18n.Builder("view.quest.progress.tasks_completed", player)
							.withPlaceholder("completed", String.valueOf(progress.completedTasks()))
							.withPlaceholder("total", String.valueOf(progress.totalTasks()))
							.build()
							.component()
			);
			
			return UnifiedBuilderFactory.item(Material.EXPERIENCE_BOTTLE)
					.setName(name)
					.setLore(lore)
					.build();
		});
		
		// Render individual task progress
		renderTaskProgress(render, player, new ArrayList<>(progress.taskProgress().values()));
	}
	
	private void renderTaskProgress(
			final @NotNull RenderContext render,
			final @NotNull Player player,
			final @NotNull List<TaskProgress> taskProgress
	) {
		int slotIndex = 0;
		
		for (TaskProgress task : taskProgress) {
			if (slotIndex >= TASK_SLOTS.length) {
				break;
			}
			
			final int slot = TASK_SLOTS[slotIndex++];
			
			render.slot(slot).renderWith(() -> {
				final Material taskMaterial = task.completed() ? Material.LIME_DYE : Material.GRAY_DYE;
				
				final List<Component> taskLore = new ArrayList<>();
				taskLore.add(new I18n.Builder("view.quest.task.progress", player)
						.withPlaceholder("current", String.valueOf(task.current()))
						.withPlaceholder("required", String.valueOf(task.required()))
						.build()
						.component());
				
				if (task.completed()) {
					taskLore.add(new I18n.Builder("view.quest.task.completed", player).build().component());
				} else {
					final int remaining = task.getRemaining();
					taskLore.add(new I18n.Builder("view.quest.task.remaining", player)
							.withPlaceholder("remaining", String.valueOf(remaining))
							.build()
							.component());
				}
				
				return UnifiedBuilderFactory.item(taskMaterial)
						.setName(Component.text(task.taskName()))
						.setLore(taskLore)
						.build();
			});
		}
	}
	
	private void renderStartButton(
			final @NotNull RenderContext render,
			final @NotNull Player player,
			final @NotNull Quest q
	) {
		render.slot(START_BUTTON_SLOT).renderWith(() -> {
			final Component name = new I18n.Builder("view.quest.items.start.name", player).build().component();
			final List<Component> lore = new I18n.Builder("view.quest.items.start.lore", player).build().children();
			
			return UnifiedBuilderFactory.item(Material.LIME_DYE)
					.setName(name)
					.setLore(lore)
					.build();
		}).onClick(click -> handleStartQuest(click, q));
	}
	
	private void renderAbandonButton(
			final @NotNull RenderContext render,
			final @NotNull Player player,
			final @NotNull Quest q
	) {
		render.slot(ABANDON_BUTTON_SLOT).renderWith(() -> {
			final Component name = new I18n.Builder("view.quest.items.abandon.name", player).build().component();
			final List<Component> lore = new I18n.Builder("view.quest.items.abandon.lore", player).build().children();
			
			return UnifiedBuilderFactory.item(Material.RED_DYE)
					.setName(name)
					.setLore(lore)
					.build();
		}).onClick(click -> handleAbandonQuest(click, q));
	}
	
	private void renderCooldownInfo(
			final @NotNull RenderContext render,
			final @NotNull Player player
	) {
		render.slot(START_BUTTON_SLOT).renderWith(() -> {
			final Component name = new I18n.Builder("quest.status.on_cooldown", player).build().component();
			
			return UnifiedBuilderFactory.item(Material.CLOCK)
					.setName(name)
					.build();
		});
	}
	
	private void handleStartQuest(
			final @NotNull SlotClickContext click,
			final @NotNull Quest q
	) {
		final Player player = click.getPlayer();
		final RDQ plugin = rdq.get(click);
		final QuestService questService = plugin.getQuestService();
		
		questService.startQuest(player.getUniqueId(), q.getIdentifier())
				.thenAccept(result -> {
					handleQuestStartResult(player, result);
					if (result instanceof QuestStartResult.Success) {
						click.update(); // Refresh view
					}
				})
				.exceptionally(ex -> {
					LOGGER.log(Level.SEVERE, "Error starting quest", ex);
					new I18n.Builder("quest.general.error", player).build().sendMessage();
					return null;
				});
	}
	
	private void handleAbandonQuest(
			final @NotNull SlotClickContext click,
			final @NotNull Quest q
	) {
		final Player player = click.getPlayer();
		final RDQ plugin = rdq.get(click);
		final QuestService questService = plugin.getQuestService();
		
		questService.abandonQuest(player.getUniqueId(), q.getIdentifier())
				.thenAccept(result -> {
					switch (result) {
						case QuestAbandonResult.Success success -> {
							new I18n.Builder("quest.notification.abandoned", player)
									.withPlaceholder("quest", success.questName())
									.build()
									.sendMessage();
							click.update(); // Refresh view
						}
						case QuestAbandonResult.NotActive notActive -> {
							new I18n.Builder("quest.command.abandon.not_active", player).build().sendMessage();
						}
						case QuestAbandonResult.QuestNotFound notFound -> {
							new I18n.Builder("quest.general.quest_not_found", player)
									.withPlaceholder("quest", q.getIdentifier())
									.build()
									.sendMessage();
						}
					}
				})
				.exceptionally(ex -> {
					LOGGER.log(Level.SEVERE, "Error abandoning quest", ex);
					new I18n.Builder("quest.general.error", player).build().sendMessage();
					return null;
				});
	}
	
	private void handleQuestStartResult(
			final @NotNull Player player,
			final @NotNull QuestStartResult result
	) {
		switch (result) {
			case QuestStartResult.Success success -> {
				new I18n.Builder("quest.notification.started", player)
						.withPlaceholder("quest", success.questName())
						.build()
						.sendMessage();
			}
			case QuestStartResult.AlreadyActive alreadyActive -> {
				new I18n.Builder("quest.command.start.already_active", player).build().sendMessage();
			}
			case QuestStartResult.MaxActiveReached maxActive -> {
				new I18n.Builder("quest.command.start.max_active", player)
						.withPlaceholder("max", String.valueOf(maxActive.maxActive()))
						.build()
						.sendMessage();
			}
			case QuestStartResult.RequirementsNotMet requirements -> {
				new I18n.Builder("quest.command.start.requirements_not_met", player).build().sendMessage();
			}
			case QuestStartResult.OnCooldown cooldown -> {
				new I18n.Builder("quest.command.start.on_cooldown", player)
						.withPlaceholder("time", formatDuration(cooldown.remainingTime()))
						.build()
						.sendMessage();
			}
			case QuestStartResult.QuestNotFound notFound -> {
				new I18n.Builder("quest.general.quest_not_found", player)
						.withPlaceholder("quest", notFound.questId())
						.build()
						.sendMessage();
			}
		}
	}
	
	private String formatDuration(final @NotNull Duration duration) {
		final long hours = duration.toHours();
		final long minutes = duration.toMinutesPart();
		
		if (hours > 0) {
			return String.format("%dh %dm", hours, minutes);
		} else {
			return String.format("%dm", minutes);
		}
	}
}
