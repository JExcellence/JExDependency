package com.raindropcentral.rdq.view.quest;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.quest.QuestCategory;
import com.raindropcentral.rdq.service.quest.QuestService;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Quest category selection view with pagination support.
 * <p>
 * Displays all available quest categories for players to browse and select.
 * Quest counts per category are fetched asynchronously alongside the categories
 * so the lore always reflects the real database count.
 *
 * @author RaindropCentral
 * @version 2.1.0
 */
public class QuestCategoryView extends APaginatedView<QuestCategory> {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
    private static final Material FALLBACK_MATERIAL = Material.BOOK;
    private static final Material STATS_MATERIAL = Material.WRITABLE_BOOK;
    private static final int STATS_SLOT = 4;

    private final State<RDQ> rdq = initialState("plugin");
    /** Pre-fetched quest counts keyed by category identifier. */
    private final MutableState<Map<String, Integer>> questCounts = mutableState(new HashMap<>());

    /**
     * Constructs a new quest category view.
     */
    public QuestCategoryView() {
        super();
    }

    @Override
    protected String getKey() {
        return "view.quest.categories";
    }

    /**
     * Loads all enabled categories and, in parallel, fetches the quest count for
     * each category. The counts are stored in the context under
     * so {@link #renderEntry} can read them without touching the lazy collection.
     *
     * @param context the inventory context
     * @return a future resolving to the sorted list of enabled categories
     */
    @Override
    protected @NotNull CompletableFuture<List<QuestCategory>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        final RDQ plugin = rdq.get(context);
        final QuestService questService = plugin.getQuestService();

        return questService.getCategories()
                .thenCompose(categories -> {
                    final List<QuestCategory> enabled = categories.stream()
                            .filter(QuestCategory::isEnabled)
                            .sorted(Comparator.comparingInt(QuestCategory::getDisplayOrder))
                            .collect(Collectors.toList());

                    // Fetch quest counts for every category in parallel
                    final Map<String, Integer> counts = new HashMap<>();
                    final List<CompletableFuture<Void>> countFutures = enabled.stream()
                            .map(cat -> questService.getQuestsByCategory(cat.getIdentifier())
                                    .thenAccept(quests -> counts.put(cat.getIdentifier(), quests.size()))
                                    .exceptionally(ex -> {
                                        LOGGER.log(Level.WARNING, "Failed to count quests for category: " + cat.getIdentifier(), ex);
                                        return null;
                                    }))
                            .collect(Collectors.toList());

                    return CompletableFuture.allOf(countFutures.toArray(new CompletableFuture[0]))
                            .thenApply(v -> {
                                questCounts.set(counts, context);
                                return enabled;
                            });
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Failed to load quest categories", ex);
                    return List.of();
                });
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

        builder.renderWith(() -> createCategoryItem(player, context, category))
                .onClick(click -> handleCategoryClick(click, category));
    }

    @Override
    protected void onPaginatedRender(final @NotNull RenderContext render, final @NotNull Player player) {
        renderPlayerStats(render, player);
    }

    /**
     * Creates the item stack for a quest category entry.
     *
     * @param player   the viewing player
     * @param context  the render context (used to read pre-fetched quest counts)
     * @param category the quest category
     * @return the configured item stack
     */
    private @NotNull org.bukkit.inventory.ItemStack createCategoryItem(
            final @NotNull Player player,
            final @NotNull Context context,
            final @NotNull QuestCategory category
    ) {
        final Material material = parseMaterial(category.getIcon().getMaterial(), FALLBACK_MATERIAL);
        final Component name = new I18n.Builder(category.getIcon().getDisplayNameKey(), player).build().component();
        final List<Component> lore = buildCategoryLore(player, context, category);

        return UnifiedBuilderFactory.item(material)
                .setName(name)
                .setLore(lore)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    /**
     * Builds the lore lines for a category item using the pre-fetched quest counts.
     *
     * @param player   the viewing player
     * @param context  the render context
     * @param category the quest category
     * @return ordered list of lore components
     */
    private @NotNull List<Component> buildCategoryLore(
            final @NotNull Player player,
            final @NotNull Context context,
            final @NotNull QuestCategory category
    ) {
        final List<Component> lore = new ArrayList<>();

        lore.addAll(new I18n.Builder(category.getIcon().getDescriptionKey(), player).build().children());
        lore.add(Component.empty());

        final int total = getQuestCount(context, category.getIdentifier());
        lore.add(new I18n.Builder("view.quest.categories.category.lore.quests_total", player)
                .withPlaceholder("total", total)
                .build().component());

        lore.add(Component.empty());
        lore.add(new I18n.Builder("view.quest.categories.category.lore.click_to_view", player).build().component());

        return lore;
    }

    /**
     * Reads the pre-fetched quest count for a category from the mutable state.
     *
     * @param context    the context
     * @param identifier the category identifier
     * @return the quest count, or 0 if not yet available
     */
    private int getQuestCount(final @NotNull Context context, final @NotNull String identifier) {
        final Map<String, Integer> counts = questCounts.get(context);
        if (counts == null) {
            return 0;
        }
        return counts.getOrDefault(identifier, 0);
    }

    /**
     * Renders the player's overall quest progress summary in the stats slot.
     *
     * @param render the render context
     * @param player the viewing player
     */
    private void renderPlayerStats(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        render.slot(STATS_SLOT).renderWith(() -> {
            final Component name = new I18n.Builder("view.quest.categories.stats.name", player).build().component();

            // TODO: Implement actual quest progress tracking
            final List<Component> lore = new I18n.Builder("view.quest.categories.stats.lore", player)
                    .withPlaceholder("total", 0)
                    .withPlaceholder("completed", 0)
                    .withPlaceholder("active", 0)
                    .withPlaceholder("available", 0)
                    .build().children();

            return UnifiedBuilderFactory.item(STATS_MATERIAL)
                    .setName(name)
                    .setLore(lore)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        });
    }

    /**
     * Handles a click on a category item, opening the quest list for that category.
     *
     * @param click    the click context
     * @param category the clicked category
     */
    private void handleCategoryClick(
            final @NotNull SlotClickContext click,
            final @NotNull QuestCategory category
    ) {
        final Player player = click.getPlayer();
        final RDQ plugin = rdq.get(click);

        try {
            // Use the framework's navigation - don't close manually
            click.openForPlayer(QuestListView.class, Map.of(
                    "plugin", plugin,
                    "category", category
            ));
        } catch (Exception e) {
            org.bukkit.Bukkit.getLogger().severe("[QuestCategoryView] Exception opening QuestListView: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Parses a material name safely, returning the fallback on failure.
     *
     * @param name     the material name string
     * @param fallback the fallback material
     * @return the resolved material
     */
    private static @NotNull Material parseMaterial(
            final @NotNull String name,
            final @NotNull Material fallback
    ) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (final IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Unknown material ''{0}'', falling back to {1}", new Object[]{name, fallback});
            return fallback;
        }
    }
}
