package com.raindropcentral.rdq.view.quest;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.quest.QuestCategory;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Main quest category selection view.
 * <p>
 * Displays all available quest categories for the player to choose from.
 * Each category shows its icon, name, and description.
 * Uses APaginatedView for async loading pattern.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestCategoryView extends APaginatedView<QuestCategory> {
	
	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
	
	private final State<RDQ> rdq = initialState("plugin");

	public QuestCategoryView() {
		super();
	}
	
	@Override
	protected String getKey() {
		return "view.quest.categories";
	}
	
	@Override
	protected @NotNull String[] getLayout() {
		return new String[]{
				"XXXXXXXXX",
				"XOOOOOOOX",
				"XOOOOOOOX",
				"XXXXXXXXX",
				"   <p>   "
		};
	}
	
	@Override
	protected CompletableFuture<List<QuestCategory>> getAsyncPaginationSource(final @NotNull Context context) {
		LOGGER.info("Loading quest categories from cache...");

		final RDQ plugin = rdq.get(context);

		// Get categories from cache manager (instant access)
		final List<QuestCategory> categories = plugin.getQuestCacheManager().getAllCategories();

		LOGGER.info("Loaded " + categories.size() + " categories from cache");

		// Return instantly as CompletableFuture
		return CompletableFuture.completedFuture(categories);
	}

	@Override
	protected void renderEntry(
			final @NotNull Context context,
			final @NotNull BukkitItemComponentBuilder builder,
			final int index,
			final @NotNull QuestCategory category
	) {
		final Player player = context.getPlayer();
		final RDQ plugin = rdq.get(context);
		
		builder.renderWith(() -> {
			// Use iconMaterial if available, otherwise default to BOOK
			final Material material = category.getIconMaterial() != null
					? Material.valueOf(category.getIconMaterial().toUpperCase())
					: Material.BOOK;

			// Quest category names use absolute keys (quest.category.{id}.name)
			final Component name = new I18n.Builder("quest.category." + category.getIdentifier() + ".name", player).build().component();

			// Build lore with category description and quest count
			final List<Component> lore = new ArrayList<>();

			// Add category description from absolute icon.lore key
			lore.addAll(new I18n.Builder("quest.category." + category.getIdentifier() + ".icon.lore", player).build().children());
			lore.add(Component.empty());

			// Add quest count (relative to view.quest.categories)
			final int questCount = plugin.getQuestCacheManager().getQuestsByCategory(category.getIdentifier()).size();
			lore.add(i18n("quest_count", player)
					.withPlaceholder("count", String.valueOf(questCount))
					.build()
					.component());

			// Add click hint (relative to view.quest.categories)
			lore.add(Component.empty());
			lore.add(i18n("click_to_view", player).build().component());
			
			return UnifiedBuilderFactory.item(material)
					.setName(name)
					.setLore(lore)
					.build();
		}).onClick(click -> handleCategoryClick(click, category));
	}
	
	@Override
	protected void onPaginatedRender(final @NotNull RenderContext render, final @NotNull Player player) {
		// No additional UI elements needed for category view
	}

	private void handleCategoryClick(
			final @NotNull SlotClickContext click,
			final @NotNull QuestCategory category
	) {
		final Player player = click.getPlayer();
		final RDQ plugin = rdq.get(click);
		
		// Open quest list view for this category
		click.closeForPlayer();
		plugin.getViewFrame().open(QuestListView.class, player, Map.of(
				"plugin", plugin,
				"category", category
		));
	}
}
