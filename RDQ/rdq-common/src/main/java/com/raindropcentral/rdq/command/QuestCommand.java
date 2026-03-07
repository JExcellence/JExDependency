package com.raindropcentral.rdq.command;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.quest.service.QuestService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Main command handler for the quest system.
 * <p>
 * Handles all quest-related commands including GUI access and quest management.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestCommand implements CommandExecutor, TabCompleter {
	
	private static final Logger LOGGER = Logger.getLogger(QuestCommand.class.getName());
	
	private final RDQ plugin;
	private final QuestService questService;
	
	/**
	 * Constructs a new QuestCommand.
	 *
	 * @param plugin the RDQ plugin instance
	 */
	public QuestCommand(@NotNull final RDQ plugin) {
		this.plugin = plugin;
		this.questService = plugin.getQuestService();
	}
	
	/**
	 * Executes the quest command.
	 *
	 * @param sender the command sender
	 * @param command the command
	 * @param label the command label
	 * @param args the command arguments
	 * @return true if the command was handled
	 */
	@Override
	public boolean onCommand(
			@NotNull final CommandSender sender,
			@NotNull final Command command,
			@NotNull final String label,
			@NotNull final String[] args
	) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage("§cThis command can only be used by players.");
			return true;
		}
		
		if (args.length == 0) {
			// Open quest GUI
			openQuestGUI(player);
			return true;
		}
		
		final String subCommand = args[0].toLowerCase();
		
		switch (subCommand) {
			case "list" -> handleList(player, args);
			case "start" -> handleStart(player, args);
			case "abandon" -> handleAbandon(player, args);
			case "progress" -> handleProgress(player);
			case "info" -> handleInfo(player, args);
			case "help" -> handleHelp(player);
			default -> {
				player.sendMessage("§cUnknown subcommand. Use /quest help for available commands.");
				return true;
			}
		}
		
		return true;
	}
	
	/**
	 * Opens the quest GUI for the player.
	 *
	 * @param player the player
	 */
	private void openQuestGUI(@NotNull final Player player) {
		try {
			plugin.getViewFrame().open(
				com.raindropcentral.rdq.view.quest.QuestCategoryView.class,
				player,
				Map.of("plugin", plugin)
			);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to open quest GUI for " + player.getName(), e);
			player.sendMessage("§cFailed to open quest menu. Please try again later.");
		}
	}
	
	/**
	 * Handles the list subcommand.
	 *
	 * @param player the player
	 * @param args the command arguments
	 */
	private void handleList(@NotNull final Player player, @NotNull final String[] args) {
		player.sendMessage("§6=== Available Quests ===");
		
		if (args.length > 1) {
			final String categoryId = args[1];
			
			questService.getQuestsByCategory(categoryId)
					.thenAccept(quests -> {
						if (quests.isEmpty()) {
							player.sendMessage("§eNo quests found in category: " + categoryId);
							return;
						}
						
						player.sendMessage("§eCategory: §f" + categoryId);
						quests.forEach(quest ->
							player.sendMessage("§7- §f" + quest.getIdentifier() + " §8(" + quest.getDifficulty() + ")")
						);
					})
					.exceptionally(ex -> {
						LOGGER.log(Level.SEVERE, "Error listing quests for category: " + categoryId, ex);
						player.sendMessage("§cFailed to list quests.");
						return null;
					});
		} else {
			questService.getCategories()
					.thenAccept(categories -> {
						if (categories.isEmpty()) {
							player.sendMessage("§eNo quest categories available.");
							return;
						}
						
						categories.forEach(category -> {
							player.sendMessage("§e" + category.getIdentifier());
							questService.getQuestsByCategory(category.getIdentifier())
									.thenAccept(quests -> {
										quests.forEach(quest ->
											player.sendMessage("  §7- §f" + quest.getIdentifier())
										);
									});
						});
					})
					.exceptionally(ex -> {
						LOGGER.log(Level.SEVERE, "Error listing quest categories", ex);
						player.sendMessage("§cFailed to list quests.");
						return null;
					});
		}
	}
	
	/**
	 * Handles the start subcommand.
	 *
	 * @param player the player
	 * @param args the command arguments
	 */
	private void handleStart(@NotNull final Player player, @NotNull final String[] args) {
		if (args.length < 2) {
			player.sendMessage("§cUsage: /quest start <questId>");
			return;
		}
		
		final String questId = args[1];
		
		questService.startQuest(player.getUniqueId(), questId)
				.thenAccept(result -> {
					switch (result) {
						case com.raindropcentral.rdq.quest.model.QuestStartResult.Success success ->
								player.sendMessage("§aQuest started: §f" + success.questName());
						case com.raindropcentral.rdq.quest.model.QuestStartResult.AlreadyActive alreadyActive ->
								player.sendMessage("§eYou already have this quest active.");
						case com.raindropcentral.rdq.quest.model.QuestStartResult.MaxActiveReached maxActive ->
								player.sendMessage("§cYou have reached the maximum number of active quests (" + maxActive.maxActive() + ").");
						case com.raindropcentral.rdq.quest.model.QuestStartResult.RequirementsNotMet requirements ->
								player.sendMessage("§cYou don't meet the requirements for this quest.");
						case com.raindropcentral.rdq.quest.model.QuestStartResult.OnCooldown cooldown ->
								player.sendMessage("§cThis quest is on cooldown. Try again later.");
						case com.raindropcentral.rdq.quest.model.QuestStartResult.QuestNotFound notFound ->
								player.sendMessage("§cQuest not found: " + questId);
					}
				})
				.exceptionally(ex -> {
					LOGGER.log(Level.SEVERE, "Error starting quest: " + questId, ex);
					player.sendMessage("§cFailed to start quest.");
					return null;
				});
	}
	
	/**
	 * Handles the abandon subcommand.
	 *
	 * @param player the player
	 * @param args the command arguments
	 */
	private void handleAbandon(@NotNull final Player player, @NotNull final String[] args) {
		if (args.length < 2) {
			player.sendMessage("§cUsage: /quest abandon <questId> [confirm]");
			return;
		}
		
		final String questId = args[1];
		final boolean confirmed = args.length > 2 && "confirm".equalsIgnoreCase(args[2]);
		
		if (!confirmed) {
			player.sendMessage("§eAre you sure you want to abandon this quest?");
			player.sendMessage("§eType §f/quest abandon " + questId + " confirm §eto confirm.");
			return;
		}
		
		questService.abandonQuest(player.getUniqueId(), questId)
				.thenAccept(result -> {
					switch (result) {
						case com.raindropcentral.rdq.quest.model.QuestAbandonResult.Success success ->
								player.sendMessage("§aQuest abandoned: §f" + success.questName());
						case com.raindropcentral.rdq.quest.model.QuestAbandonResult.NotActive notActive ->
								player.sendMessage("§eYou don't have this quest active.");
						case com.raindropcentral.rdq.quest.model.QuestAbandonResult.QuestNotFound notFound ->
								player.sendMessage("§cQuest not found: " + questId);
					}
				})
				.exceptionally(ex -> {
					LOGGER.log(Level.SEVERE, "Error abandoning quest: " + questId, ex);
					player.sendMessage("§cFailed to abandon quest.");
					return null;
				});
	}
	
	/**
	 * Handles the progress subcommand.
	 *
	 * @param player the player
	 */
	private void handleProgress(@NotNull final Player player) {
		questService.getActiveQuests(player.getUniqueId())
				.thenAccept(activeQuests -> {
					if (activeQuests.isEmpty()) {
						player.sendMessage("§eYou don't have any active quests.");
						return;
					}
					
					player.sendMessage("§6=== Your Active Quests ===");
					
					activeQuests.forEach(activeQuest -> {
						questService.getProgress(player.getUniqueId(), activeQuest.questId())
								.thenAccept(progressOpt -> {
									if (progressOpt.isPresent()) {
										final var progress = progressOpt.get();
										player.sendMessage("§e" + progress.questId() + " §7(" + progress.getOverallProgressPercentage() + "%)");
										
										progress.taskProgress().values().forEach(taskProgress -> {
											final String status = taskProgress.completed() ? "§a✓" : "§7○";
											player.sendMessage("  " + status + " §f" + taskProgress.taskName() +
												" §7(" + taskProgress.current() + "/" + taskProgress.required() + ")");
										});
									}
								});
					});
				})
				.exceptionally(ex -> {
					LOGGER.log(Level.SEVERE, "Error getting quest progress", ex);
					player.sendMessage("§cFailed to get quest progress.");
					return null;
				});
	}
	
	/**
	 * Handles the info subcommand.
	 *
	 * @param player the player
	 * @param args the command arguments
	 */
	private void handleInfo(@NotNull final Player player, @NotNull final String[] args) {
		if (args.length < 2) {
			player.sendMessage("§cUsage: /quest info <questId>");
			return;
		}
		
		final String questId = args[1];
		
		questService.getQuest(questId)
				.thenAccept(questOpt -> {
					if (questOpt.isEmpty()) {
						player.sendMessage("§cQuest not found: " + questId);
						return;
					}
					
					final var quest = questOpt.get();
					
					player.sendMessage("§6=== Quest Info ===");
					player.sendMessage("§eName: §f" + quest.getIdentifier());
					player.sendMessage("§eDifficulty: §f" + quest.getDifficulty());
					player.sendMessage("§eRepeatable: §f" + (quest.isRepeatable() ? "Yes" : "No"));
					
					if (quest.getTimeLimitSeconds() > 0) {
						player.sendMessage("§eTime Limit: §f" + (quest.getTimeLimitSeconds() / 60) + " minutes");
					}
					
					if (quest.getCooldownSeconds() > 0) {
						player.sendMessage("§eCooldown: §f" + (quest.getCooldownSeconds() / 60) + " minutes");
					}
				})
				.exceptionally(ex -> {
					LOGGER.log(Level.SEVERE, "Error getting quest info: " + questId, ex);
					player.sendMessage("§cFailed to get quest info.");
					return null;
				});
	}
	
	/**
	 * Handles the help subcommand.
	 *
	 * @param player the player
	 */
	private void handleHelp(@NotNull final Player player) {
		player.sendMessage("§6=== Quest Commands ===");
		player.sendMessage("§e/quest §7- Open quest menu");
		player.sendMessage("§e/quest list [category] §7- List available quests");
		player.sendMessage("§e/quest start <questId> §7- Start a quest");
		player.sendMessage("§e/quest abandon <questId> [confirm] §7- Abandon a quest");
		player.sendMessage("§e/quest progress §7- View your active quests");
		player.sendMessage("§e/quest info <questId> §7- View quest details");
		player.sendMessage("§e/quest help §7- Show this help message");
	}
	
	/**
	 * Provides tab completion for the quest command.
	 *
	 * @param sender the command sender
	 * @param command the command
	 * @param alias the command alias
	 * @param args the command arguments
	 * @return list of tab completions
	 */
	@Override
	public @Nullable List<String> onTabComplete(
			@NotNull final CommandSender sender,
			@NotNull final Command command,
			@NotNull final String alias,
			@NotNull final String[] args
	) {
		if (!(sender instanceof Player)) {
			return List.of();
		}
		
		if (args.length == 1) {
			return Arrays.asList("list", "start", "abandon", "progress", "info", "help")
					.stream()
					.filter(s -> s.startsWith(args[0].toLowerCase()))
					.collect(Collectors.toList());
		}
		
		if (args.length == 2) {
			final String subCommand = args[0].toLowerCase();
			
			if ("start".equals(subCommand) || "info".equals(subCommand) || "abandon".equals(subCommand)) {
				// TODO: Return available quest IDs
				return new ArrayList<>();
			}
			
			if ("list".equals(subCommand)) {
				// TODO: Return category IDs
				return new ArrayList<>();
			}
		}
		
		if (args.length == 3 && "abandon".equals(args[0].toLowerCase())) {
			return List.of("confirm");
		}
		
		return List.of();
	}
}
