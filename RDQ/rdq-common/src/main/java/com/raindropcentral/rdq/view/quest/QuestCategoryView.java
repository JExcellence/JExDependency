package com.raindropcentral.rdq.view.quest;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.quest.QuestCategory;
import com.raindropcentral.rdq.quest.service.QuestService;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main quest category selection view.
 *
 * <p>Displays all available quest categories for the player to choose from.
 * Each category shows its icon, name, and description.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestCategoryView extends BaseView {
	
	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
	
	private final State<RDQ> rdq = initialState("plugin");
	
	private static final int[] CATEGORY_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
	
	/**
	 * Executes QuestCategoryView.
	 */
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
				"X       X",
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
		final RDQ plugin = rdq.get(render);
		final QuestService questService = plugin.getQuestService();
		
		// Load categories asynchronously
		questService.getCategories()
				.thenAccept(categories -> {
					// Switch back to main thread to update the view
					org.bukkit.Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> {
						renderCategories(render, player, categories);
						render.update();  // Update the view to show the categories
					});
				})
				.exceptionally(ex -> {
					LOGGER.log(Level.SEVERE, "Error loading quest categories for player " + player.getName(), ex);
					return null;
				});
	}
	
	private void renderCategories(
			final @NotNull RenderContext render,
			final @NotNull Player player,
			final @NotNull List<QuestCategory> categories
	) {
		int slotIndex = 0;
		
		for (QuestCategory category : categories) {
			if (!category.isEnabled()) {
				continue;
			}
			
			if (slotIndex >= CATEGORY_SLOTS.length) {
				break;
			}
			
			final int slot = CATEGORY_SLOTS[slotIndex++];
			renderCategory(render, player, category, slot);
		}
	}
	
	private void renderCategory(
			final @NotNull RenderContext render,
			final @NotNull Player player,
			final @NotNull QuestCategory category,
			final int slot
	) {
		render.slot(slot).renderWith(() -> {
			final Material material = Material.valueOf(category.getIcon().getMaterial().toUpperCase());
			final Component name = new I18n.Builder(category.getIcon().getDisplayNameKey(), player).build().component();
			final List<Component> lore = new I18n.Builder(category.getIcon().getDescriptionKey(), player).build().children();
			
			return UnifiedBuilderFactory.item(material)
					.setName(name)
					.setLore(lore)
					.build();
		}).onClick(click -> handleCategoryClick(click, category));
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
