package com.raindropcentral.rdq.view.quest;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.database.entity.quest.QuestCategory;
import com.raindropcentral.rdq.quest.model.QuestStartResult;
import com.raindropcentral.rdq.quest.service.QuestService;
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
 * Shows all available quests in the selected category with their status
 * and allows players to start or view quest details.
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
		final QuestService questService = plugin.getQuestService();
		
		if (cat == null) {
			return CompletableFuture.completedFuture(List.of());
		}
		
		return questService.getQuestsByCategory(cat.getIdentifier());
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
			final Material material = Material.valueOf(quest.getIcon().getMaterial().toUpperCase());
			final Component name = new I18n.Builder(quest.getIcon().getDisplayNameKey(), player).build().component();
			
			// Build lore with status information
			final List<Component> lore = new ArrayList<>();
			lore.addAll(new I18n.Builder(quest.getIcon().getDescriptionKey(), player).build().children());
			lore.add(Component.empty());
			
			// Add difficulty
			final String difficultyKey = "quest.difficulty." + quest.getDifficulty().name().toLowerCase();
			lore.add(new I18n.Builder(difficultyKey, player).build().component());
			
			// Add status placeholder (will be updated async)
			lore.add(new I18n.Builder("quest.status.loading", player).build().component());
			
			return UnifiedBuilderFactory.item(material)
					.setName(name)
					.setLore(lore)
					.build();
		}).onClick(click -> handleQuestClick(click, quest));
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
									handleQuestStartResult(player, result);
									if (result instanceof QuestStartResult.Success) {
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
