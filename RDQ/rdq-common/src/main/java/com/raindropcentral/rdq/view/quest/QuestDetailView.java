package com.raindropcentral.rdq.view.quest;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.database.entity.quest.QuestRequirement;
import com.raindropcentral.rdq.database.entity.quest.QuestReward;
import com.raindropcentral.rdq.database.entity.quest.QuestTask;
import com.raindropcentral.rdq.model.quest.QuestAbandonResult;
import com.raindropcentral.rdq.model.quest.QuestProgress;
import com.raindropcentral.rdq.model.quest.QuestStartResult;
import com.raindropcentral.rdq.model.quest.TaskProgress;
import com.raindropcentral.rdq.service.quest.QuestService;
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
 * Detailed view for a specific quest showing comprehensive information.
 * <p>
 * This view displays:
 * - Quest description and difficulty
 * - All tasks with progress indicators
 * - All rewards with icons and descriptions
 * - All requirements with status indicators
 * - Prerequisite quests with completion status
 * - Available actions (start, abandon)
 * </p>
 *
 * @author RaindropCentral
 * @version 2.0.0
 */
public class QuestDetailView extends BaseView {
	
	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
	
	private final State<RDQ> rdq = initialState("plugin");
	private final State<Quest> quest = initialState("quest");
	
	// Layout slots
	private static final int QUEST_INFO_SLOT = 4;
	private static final int[] TASK_SLOTS = {19, 20, 21, 22, 23, 24, 25};
	private static final int[] REWARD_SLOTS = {37, 38, 39, 40, 41, 42, 43};
	private static final int[] REQUIREMENT_SLOTS = {10, 11, 12, 13, 14, 15, 16};
	private static final int START_BUTTON_SLOT = 48;
	private static final int ABANDON_BUTTON_SLOT = 50;
	
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
				"XXXXXXXXX",  // Row 1: Decoration
				"XrrrrrrrX",  // Row 2: Requirements (r)
				"XXXXXXXXX",  // Row 3: Decoration
				"XtttttttX",  // Row 4: Tasks (t)
				"XXXXXXXXX",  // Row 5: Decoration
				"XwwwwwwwX",  // Row 6: Rewards (w)
				"         "   // Row 7: Action buttons + back
		};
	}
	
	@Override
	public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
		final Quest q = quest.get(render);
		final RDQ plugin = rdq.get(render);
		
		if (q == null) {
			renderErrorState(render, player);
			return;
		}
		
		// Render quest info at top (with overall progress if active)
		renderQuestInfo(render, player, q, plugin);

		// Render requirements section
		renderRequirements(render, player, q);

		// Render tasks section
		renderTasks(render, player, q, plugin);

		// Render rewards section
		renderRewards(render, player, q);

		// Render action buttons based on quest status
		renderQuestStatus(render, player, q, plugin);
	}
	
	private void renderErrorState(final @NotNull RenderContext render, final @NotNull Player player) {
		render.slot(QUEST_INFO_SLOT).renderWith(() -> {
			final Component errorName = i18n("error.quest_not_found", player).build().component();
			return UnifiedBuilderFactory.item(Material.BARRIER)
					.setName(errorName)
					.build();
		});
	}
	
	private void renderQuestInfo(
			final @NotNull RenderContext render,
			final @NotNull Player player,
			final @NotNull Quest q,
			final @NotNull RDQ plugin
	) {
		render.slot(QUEST_INFO_SLOT).renderWith(() -> {
			final Material material = Material.BOOK;
			final Component name = new I18n.Builder("quest." + q.getIdentifier() + ".name", player).build().component();

			final List<Component> lore = new ArrayList<>();
			lore.addAll(new I18n.Builder("quest." + q.getIdentifier() + ".description", player).build().children());
			lore.add(Component.empty());

			// Add difficulty
			final String difficultyKey = "difficulty." + q.getDifficulty().name().toLowerCase();
			lore.add(i18n(difficultyKey, player).build().component());

			// Add repeatable info
			if (q.isRepeatable()) {
				lore.add(i18n("info.repeatable", player).build().component());
				if (q.getCooldownMinutes() != null && q.getCooldownMinutes() > 0) {
					lore.add(i18n("info.cooldown", player)
							.withPlaceholder("time", formatDuration(Duration.ofMinutes(q.getCooldownMinutes())))
							.build()
							.component());
				}
			}

			// Add time limit if present
			if (q.hasTimeLimit()) {
				lore.add(i18n("info.time_limit", player)
						.withPlaceholder("time", formatDuration(q.getTimeLimit()))
						.build()
						.component());
			}

			// Add prerequisite count
			if (!q.getPrerequisiteQuestIds().isEmpty()) {
				lore.add(Component.empty());
				lore.add(i18n("info.prerequisites", player)
						.withPlaceholder("count", q.getPrerequisiteQuestIds().size())
						.build()
						.component());
			}

			return UnifiedBuilderFactory.item(material)
					.setName(name)
					.setLore(lore)
					.build();
		});

		// Async: add overall progress if quest is active
		final QuestService questService = plugin.getQuestService();
		questService.isQuestActive(player.getUniqueId(), q.getIdentifier())
				.thenCompose(isActive -> {
					if (isActive) {
						return questService.getProgress(player.getUniqueId(), q.getIdentifier());
					}
					return java.util.concurrent.CompletableFuture.completedFuture(java.util.Optional.empty());
				})
				.thenAccept(progressOpt -> {
					if (progressOpt.isPresent()) {
						final QuestProgress progress = progressOpt.get();
						render.slot(QUEST_INFO_SLOT).renderWith(() -> {
							final Component infoName = new I18n.Builder("quest." + q.getIdentifier() + ".name", player).build().component();

							final List<Component> lore = new ArrayList<>();
							lore.addAll(new I18n.Builder("quest." + q.getIdentifier() + ".description", player).build().children());
							lore.add(Component.empty());

							// Overall progress bar
							lore.add(buildProgressBar(progress.overallProgress() / 100.0));
							lore.add(i18n("overall.percentage", player)
									.withPlaceholder("percentage", (int) progress.overallProgress())
									.build()
									.component());
							lore.add(i18n("overall.completed_tasks", player)
									.withPlaceholder("completed", progress.taskProgress().stream().filter(TaskProgress::completed).count())
									.withPlaceholder("total", progress.taskProgress().size())
									.build()
									.component());
							lore.add(Component.empty());

							// Difficulty
							lore.add(i18n("difficulty." + q.getDifficulty().name().toLowerCase(), player).build().component());

							// Repeatable
							if (q.isRepeatable()) {
								lore.add(i18n("info.repeatable", player).build().component());
							}

							// Time limit
							if (q.hasTimeLimit()) {
								lore.add(i18n("info.time_limit", player)
										.withPlaceholder("time", formatDuration(q.getTimeLimit()))
										.build()
										.component());
							}

							return UnifiedBuilderFactory.item(Material.ENCHANTED_BOOK)
									.setName(infoName)
									.setLore(lore)
									.build();
						});
					}
				})
				.exceptionally(ex -> {
					LOGGER.log(Level.SEVERE, "Error loading quest progress for info display", ex);
					return null;
				});
	}
	
	private void renderRequirements(
			final @NotNull RenderContext render,
			final @NotNull Player player,
			final @NotNull Quest q
	) {
		final List<QuestRequirement> requirements = q.getRequirements();

		for (int i = 0; i < REQUIREMENT_SLOTS.length && i < requirements.size(); i++) {
			final QuestRequirement requirement = requirements.get(i);
			final int slot = REQUIREMENT_SLOTS[i];

			render.slot(slot).renderWith(() -> {
				final boolean isMet = requirement.isMet(player);
				final Material material = Material.valueOf(requirement.getIcon().getMaterial().toUpperCase());
				final Component name = new I18n.Builder(requirement.getIcon().getDisplayNameKey(), player).build().component();

				final List<Component> lore = new ArrayList<>();
				lore.addAll(new I18n.Builder(requirement.getIcon().getDescriptionKey(), player).build().children());
				lore.add(Component.empty());

				// Add status
				if (isMet) {
					lore.add(i18n("requirement.met", player).build().component());
				} else {
					lore.add(i18n("requirement.not_met", player).build().component());

					// Add progress if available
					final double progress = requirement.calculateProgress(player);
					if (progress > 0 && progress < 1.0) {
						lore.add(i18n("requirement.progress", player)
								.withPlaceholder("progress", String.format("%.1f", progress * 100))
								.build()
								.component());
					}
				}

				return UnifiedBuilderFactory.item(material)
						.setName(name)
						.setLore(lore)
						.build();
			});
		}

		// Fill empty requirement slots
		for (int i = requirements.size(); i < REQUIREMENT_SLOTS.length; i++) {
			render.slot(REQUIREMENT_SLOTS[i]).renderWith(() -> createFillItem(player));
		}
	}

	private void renderTasks(
			final @NotNull RenderContext render,
			final @NotNull Player player,
			final @NotNull Quest q,
			final @NotNull RDQ plugin
	) {
		final List<QuestTask> tasks = q.getTasks();
		final QuestService questService = plugin.getQuestService();
		
		// Check if quest is active to show progress
		questService.isQuestActive(player.getUniqueId(), q.getIdentifier())
				.thenAccept(isActive -> {
					if (isActive) {
						// Show tasks with progress
						questService.getProgress(player.getUniqueId(), q.getIdentifier())
								.thenAccept(progressOpt -> {
									if (progressOpt.isPresent()) {
										renderTasksWithProgress(render, player, q, progressOpt.get());
									} else {
										renderTasksWithoutProgress(render, player, tasks);
									}
								});
					} else {
						// Show tasks without progress
						renderTasksWithoutProgress(render, player, tasks);
					}
				})
				.exceptionally(ex -> {
					LOGGER.log(Level.SEVERE, "Error checking quest status", ex);
					renderTasksWithoutProgress(render, player, tasks);
					return null;
				});
	}
	
	private void renderTasksWithoutProgress(
			final @NotNull RenderContext render,
			final @NotNull Player player,
			final @NotNull List<QuestTask> tasks
	) {
		for (int i = 0; i < TASK_SLOTS.length && i < tasks.size(); i++) {
			final QuestTask task = tasks.get(i);
			final int slot = TASK_SLOTS[i];
			final int taskNumber = i + 1;

			render.slot(slot).renderWith(() -> {
				final Component name = new I18n.Builder("quest." + task.getQuest().getIdentifier() + ".task" + taskNumber + ".name", player)
						.build()
						.component();

				final List<Component> lore = new ArrayList<>();
				lore.add(i18n("task.not_started", player).build().component());
				lore.add(Component.empty());
				lore.add(i18n("task.difficulty", player)
						.withPlaceholder("difficulty", task.getDifficulty().name())
						.build()
						.component());

				if (task.isOptional()) {
					lore.add(i18n("task.optional", player).build().component());
				}

				return UnifiedBuilderFactory.item(Material.PAPER)
						.setName(name)
						.setLore(lore)
						.build();
			});
		}
		
		// Fill empty task slots
		for (int i = tasks.size(); i < TASK_SLOTS.length; i++) {
			render.slot(TASK_SLOTS[i]).renderWith(() -> createFillItem(player));
		}
	}
	
	private void renderTasksWithProgress(
			final @NotNull RenderContext render,
			final @NotNull Player player,
			final @NotNull Quest q,
			final @NotNull QuestProgress progress
	) {
		final List<QuestTask> tasks = q.getTasks();

		for (int i = 0; i < TASK_SLOTS.length && i < tasks.size(); i++) {
			final QuestTask task = tasks.get(i);
			final int slot = TASK_SLOTS[i];
			final int taskNumber = i + 1;

			// Find task progress by task identifier
			final TaskProgress taskProgress = progress.taskProgress().stream()
					.filter(tp -> tp.taskIdentifier().equals(task.getIdentifier()))
					.findFirst()
					.orElse(null);

			render.slot(slot).renderWith(() -> {
				final boolean completed = taskProgress != null && taskProgress.completed();

				// Dynamic material based on progress stage
				final Material material = getTaskProgressMaterial(taskProgress, completed);

				final Component name = new I18n.Builder("quest." + q.getIdentifier() + ".task" + taskNumber + ".name", player)
						.build()
						.component();

				final List<Component> lore = new ArrayList<>();

				if (taskProgress != null) {
					// Visual progress bar
					lore.add(buildProgressBar(taskProgress.progressPercentage()));

					lore.add(i18n("task.progress_bar", player)
							.withPlaceholder("current", taskProgress.currentProgress())
							.withPlaceholder("required", taskProgress.requiredProgress())
							.build()
							.component());

					final int percentage = (int) (taskProgress.progressPercentage() * 100);
					lore.add(i18n("task.progress_percentage", player)
							.withPlaceholder("percentage", percentage)
							.build()
							.component());

					if (completed) {
						lore.add(Component.empty());
						lore.add(i18n("task.completed", player).build().component());
					} else {
						lore.add(Component.empty());
						int remaining = taskProgress.requiredProgress() - taskProgress.currentProgress();
						lore.add(i18n("task.remaining", player)
								.withPlaceholder("remaining", remaining)
								.build()
								.component());

						// Milestone indicators
						final double pct = taskProgress.progressPercentage();
						if (pct >= 0.75) {
							lore.add(i18n("task.milestone.almost_done", player).build().component());
						} else if (pct >= 0.50) {
							lore.add(i18n("task.milestone.halfway", player).build().component());
						} else if (pct >= 0.25) {
							lore.add(i18n("task.milestone.quarter", player).build().component());
						}
					}
				} else {
					lore.add(i18n("task.not_started", player).build().component());
				}

				lore.add(Component.empty());
				lore.add(i18n("task.difficulty", player)
						.withPlaceholder("difficulty", task.getDifficulty().name())
						.build()
						.component());

				if (task.isOptional()) {
					lore.add(i18n("task.optional", player).build().component());
				}

				return UnifiedBuilderFactory.item(material)
						.setName(name)
						.setLore(lore)
						.build();
			});
		}

		// Fill empty task slots
		for (int i = tasks.size(); i < TASK_SLOTS.length; i++) {
			render.slot(TASK_SLOTS[i]).renderWith(() -> createFillItem(player));
		}
	}

	/**
	 * Gets the material for a task based on its progress state.
	 */
	private Material getTaskProgressMaterial(final TaskProgress taskProgress, final boolean completed) {
		if (completed) {
			return Material.LIME_DYE;
		}
		if (taskProgress == null) {
			return Material.GRAY_DYE;
		}
		final double pct = taskProgress.progressPercentage();
		if (pct >= 0.75) {
			return Material.YELLOW_DYE;
		} else if (pct >= 0.25) {
			return Material.ORANGE_DYE;
		} else if (pct > 0) {
			return Material.RED_DYE;
		}
		return Material.GRAY_DYE;
	}

	/**
	 * Builds a visual progress bar component using colored Unicode characters.
	 *
	 * @param percentage progress percentage (0.0 to 1.0)
	 * @return a component representing a visual progress bar
	 */
	private Component buildProgressBar(final double percentage) {
		final int totalBars = 20;
		final int filledBars = (int) (percentage * totalBars);
		final int emptyBars = totalBars - filledBars;

		final String filled = "█".repeat(filledBars);
		final String empty = "░".repeat(emptyBars);

		// Color the bar based on progress
		final String color;
		if (percentage >= 1.0) {
			color = "<green>";
		} else if (percentage >= 0.75) {
			color = "<yellow>";
		} else if (percentage >= 0.50) {
			color = "<gold>";
		} else {
			color = "<red>";
		}

		return Component.text(color + filled + "<gray>" + empty);
	}

	private void renderRewards(
			final @NotNull RenderContext render,
			final @NotNull Player player,
			final @NotNull Quest q
	) {
		final List<QuestReward> rewards = q.getRewards();

		for (int i = 0; i < REWARD_SLOTS.length && i < rewards.size(); i++) {
			final QuestReward reward = rewards.get(i);
			final int slot = REWARD_SLOTS[i];

			render.slot(slot).renderWith(() -> {
				final Material material = Material.valueOf(reward.getIcon().getMaterial().toUpperCase());
				final Component name = new I18n.Builder(reward.getIcon().getDisplayNameKey(), player).build().component();

				final List<Component> lore = new ArrayList<>();
				lore.addAll(new I18n.Builder(reward.getIcon().getDescriptionKey(), player).build().children());
				lore.add(Component.empty());

				// Add estimated value
				final double value = reward.getEstimatedValue();
				if (value > 0) {
					lore.add(i18n("reward.value", player)
							.withPlaceholder("value", String.format("%.2f", value))
							.build()
							.component());
				}
				
				return UnifiedBuilderFactory.item(material)
						.setName(name)
						.setLore(lore)
						.build();
			});
		}

		// Fill empty reward slots
		for (int i = rewards.size(); i < REWARD_SLOTS.length; i++) {
			render.slot(REWARD_SLOTS[i]).renderWith(() -> createFillItem(player));
		}
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
						renderAbandonButton(render, player, q);
					} else {
						renderStartButton(render, player, q, plugin);
					}
				})
				.exceptionally(ex -> {
					LOGGER.log(Level.SEVERE, "Error checking quest status", ex);
					return null;
				});
	}
	
	private void renderStartButton(
			final @NotNull RenderContext render,
			final @NotNull Player player,
			final @NotNull Quest q,
			final @NotNull RDQ plugin
	) {
		final QuestService questService = plugin.getQuestService();

		questService.canStartQuest(player.getUniqueId(), q.getIdentifier())
				.thenAccept(result -> {
					if (result.success()) {
						render.slot(START_BUTTON_SLOT).renderWith(() -> {
							final Component name = i18n("items.start.name", player).build().component();
							final List<Component> lore = i18n("items.start.lore", player).build().children();

							return UnifiedBuilderFactory.item(Material.LIME_DYE)
									.setName(name)
									.setLore(lore)
									.build();
						}).onClick(click -> handleStartQuest(click, q));
					} else {
						render.slot(START_BUTTON_SLOT).renderWith(() -> {
							final Component name = i18n("items.cannot_start.name", player).build().component();
							final List<Component> lore = List.of(
									i18n("items.cannot_start.reason", player)
											.withPlaceholder("reason", result.failureReason())
											.build()
											.component()
							);

							return UnifiedBuilderFactory.item(Material.BARRIER)
									.setName(name)
									.setLore(lore)
									.build();
						});
					}
				})
				.exceptionally(ex -> {
					LOGGER.log(Level.SEVERE, "Error checking if can start quest", ex);
					return null;
				});
	}
	
	private void renderAbandonButton(
			final @NotNull RenderContext render,
			final @NotNull Player player,
			final @NotNull Quest q
	) {
		render.slot(ABANDON_BUTTON_SLOT).renderWith(() -> {
			final Component name = i18n("items.abandon.name", player).build().component();
			final List<Component> lore = i18n("items.abandon.lore", player).build().children();
			
			return UnifiedBuilderFactory.item(Material.RED_DYE)
					.setName(name)
					.setLore(lore)
					.build();
		}).onClick(click -> handleAbandonQuest(click, q));
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
					handleQuestStartResult(player, result, q);
					if (result.success()) {
						click.update(); // Refresh view
					}
				})
				.exceptionally(ex -> {
					LOGGER.log(Level.SEVERE, "Error starting quest", ex);
					i18n("error.general", player).build().sendMessage();
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
					if (result.success()) {
						i18n("notification.abandoned", player)
								.withPlaceholder("quest", q.getDisplayName())
								.build()
								.sendMessage();
						click.update(); // Refresh view
					} else {
						i18n("error.abandon_failed", player)
								.withPlaceholder("reason", result.failureReason())
								.build()
								.sendMessage();
					}
				})
				.exceptionally(ex -> {
					LOGGER.log(Level.SEVERE, "Error abandoning quest", ex);
					i18n("error.general", player).build().sendMessage();
					return null;
				});
	}
	
	private void handleQuestStartResult(
			final @NotNull Player player,
			final @NotNull QuestStartResult result,
			final @NotNull Quest quest
	) {
		if (result.success()) {
			i18n("notification.started", player)
					.withPlaceholder("quest", quest.getDisplayName())
					.build()
					.sendMessage();
		} else {
			i18n("error.start_failed", player)
					.withPlaceholder("reason", result.failureReason())
					.build()
					.sendMessage();
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
