package com.raindropcentral.rdq.view.perks;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.manager.perk.PerkManager;
import com.raindropcentral.rdq.perk.runtime.LoadedPerk;
import com.raindropcentral.rdq.perk.runtime.PerkRegistry;
import com.raindropcentral.rdq.perk.runtime.PerkStateService;
import com.raindropcentral.rdq.type.EPerkCategory;
import com.raindropcentral.rdq.type.EPerkState;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * GUI view for displaying and managing player perks.
 * <p>
 * Provides a paginated list of perks with filtering by category,
 * showing state (owned, enabled, active, cooldown) and allowing
 * toggle/enable/disable actions.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class PerkListViewFrame extends BaseView {

    private static final Logger LOGGER = CentralLogger.getLogger(PerkListViewFrame.class.getName());

    // Layout constants
    private static final int INVENTORY_SIZE = 54;
    private static final int PERKS_PER_PAGE = 27;
    private static final int PERKS_AREA_START = 9;
    private static final int PERKS_AREA_END = 44;
    private static final int NAVIGATION_ROW = 45;
    private static final int PREVIOUS_PAGE_SLOT = 45;
    private static final int PAGE_INFO_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int ALL_CATEGORIES_SLOT = 4;

    // States
    private final State<RDQ> rdq = initialState("plugin");
    private final State<RDQPlayer> player = initialState("player");
    private final MutableState<EPerkCategory> selectedCategory = mutableState(null);
    private final MutableState<Integer> currentPage = mutableState(0);
    private final MutableState<List<PerkDisplayItem>> displayItems = mutableState(new ArrayList<>());
    private final MutableState<Long> dataRefreshTimestamp = mutableState(0L);

    // Services
    private PerkRegistry perkRegistry;
    private PerkStateService perkStateService;

    @Override
    protected String getKey() {
        return "perk_list_ui";
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext openContext) {
        final EPerkCategory category = this.selectedCategory.get(openContext);
        final String categoryName = category != null ? category.getDisplayName() : "All Perks";
        return Map.of(
                "category_name", categoryName,
                "player_name", openContext.getPlayer().getName()
        );
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        try {
            // Initialize services
            final RDQ plugin = this.rdq.get(render);
            this.perkRegistry = plugin.getPerkRegistry();
            this.perkStateService = plugin.getPerkInitializationManager().getPerkStateService();

            // Load and render data
            this.loadPerkData(render, player);
            this.renderStaticComponents(render, player);
            this.renderDynamicContent(render, player);

        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Error during first render", e);
            this.renderErrorState(render, player);
        }
    }

    @Override
    public void onResume(@NotNull Context origin, @NotNull Context target) {
        try {
            final long currentTime = System.currentTimeMillis();
            final long lastRefresh = this.dataRefreshTimestamp.get(target);
            if (currentTime - lastRefresh > 30_000L) { // Refresh every 30 seconds
                this.loadPerkData((RenderContext) target, target.getPlayer());
            }
            this.renderDynamicContent((RenderContext) target, target.getPlayer());
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Error on resume", e);
        }
    }

    private void loadPerkData(@NotNull RenderContext render, @NotNull Player player) {
        try {
            final RDQPlayer rdqPlayer = this.perkStateService.getRDQPlayer(player);
            if (rdqPlayer == null) {
                this.displayItems.set(new ArrayList<>(), render);
                return;
            }

            final List<LoadedPerk> allPerks = this.perkRegistry.getAll();
            final List<PerkDisplayItem> items = allPerks.stream()
                    .map(perk -> this.createDisplayItem(render, perk, rdqPlayer, player))
                    .filter(Objects::nonNull)
                    .sorted(this::compareDisplayItems)
                    .collect(Collectors.toList());

            this.displayItems.set(items, render);
            this.dataRefreshTimestamp.set(System.currentTimeMillis(), render);
            this.currentPage.set(0, render); // Reset to first page

        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading perk data", e);
            this.displayItems.set(new ArrayList<>(), render);
        }
    }

    private void renderStaticComponents(@NotNull RenderContext render, @NotNull Player player) {
        // Decorations can be added here if needed
    }

    private void renderDynamicContent(@NotNull RenderContext render, @NotNull Player player) {
        this.renderCategoryButtons(render, player);
        this.renderPerks(render, player);
        this.renderNavigationButtons(render, player);
    }

    private void renderCategoryButtons(@NotNull RenderContext render, @NotNull Player player) {
        final EPerkCategory selected = this.selectedCategory.get(render);

        // All categories button
        render.slot(ALL_CATEGORIES_SLOT)
                .withItem(this.createCategoryButton(null, selected == null, player))
                .onClick(click -> {
                    this.selectedCategory.set(null, render);
                    this.loadPerkData(render, player);
                    this.renderDynamicContent(render, player);
                });

        // Individual category buttons (simplified)
        int slot = 0;
        for (final EPerkCategory category : EPerkCategory.values()) {
            if (slot == ALL_CATEGORIES_SLOT) slot++;
            if (slot >= 9) break; // Limit to top row

            final boolean isSelected = category == selected;
            render.slot(slot)
                    .withItem(this.createCategoryButton(category, isSelected, player))
                    .onClick(click -> {
                        this.selectedCategory.set(category, render);
                        this.loadPerkData(render, player);
                        this.renderDynamicContent(render, player);
                    });
            slot++;
        }
    }

    private void renderPerks(@NotNull RenderContext render, @NotNull Player player) {
        final List<PerkDisplayItem> allItems = this.displayItems.get(render);
        final EPerkCategory category = this.selectedCategory.get(render);
        final List<PerkDisplayItem> filteredItems = allItems.stream()
                .filter(item -> category == null || item.category == category)
                .collect(Collectors.toList());

        final int page = this.currentPage.get(render);
        final int startIndex = page * PERKS_PER_PAGE;
        final int endIndex = Math.min(startIndex + PERKS_PER_PAGE, filteredItems.size());

        // Clear perks area
        for (int slot = PERKS_AREA_START; slot <= PERKS_AREA_END; slot++) {
            render.slot(slot).withItem(new ItemStack(Material.AIR));
        }

        // Render current page
        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            final PerkDisplayItem item = filteredItems.get(i);
            final int slot = PERKS_AREA_START + slotIndex;
            render.slot(slot)
                    .withItem(this.createPerkItem(item, player))
                    .onClick(click -> this.handlePerkClick(render, player, item));
            slotIndex++;
        }
    }

    public void renderNavigationButtons(@NotNull RenderContext render, @NotNull Player player) {
        final List<PerkDisplayItem> allItems = this.displayItems.get(render);
        final EPerkCategory category = this.selectedCategory.get(render);
        final int totalItems = (int) allItems.stream()
                .filter(item -> category == null || item.category == category)
                .count();
        final int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / PERKS_PER_PAGE));
        final int currentPageNum = this.currentPage.get(render);

        // Previous page
        if (currentPageNum > 0) {
            render.slot(PREVIOUS_PAGE_SLOT)
                    .withItem(this.createNavigationButton(Material.ARROW, "§a← Previous", Collections.emptyList()))
                    .onClick(click -> {
                        this.currentPage.set(currentPageNum - 1, render);
                        this.renderPerks(render, player);
                        this.renderNavigationButtons(render, player);
                    });
        } else {
            render.slot(PREVIOUS_PAGE_SLOT)
                    .withItem(this.createNavigationButton(Material.GRAY_DYE, "§8← Previous", Collections.singletonList("§7No previous page")));
        }

        // Page info
        render.slot(PAGE_INFO_SLOT)
                .withItem(this.createNavigationButton(Material.PAPER,
                        "§fPage " + (currentPageNum + 1) + "/" + totalPages,
                        Arrays.asList("§7" + totalItems + " perks total", "§7" + PERKS_PER_PAGE + " per page")));

        // Next page
        if (currentPageNum < totalPages - 1) {
            render.slot(NEXT_PAGE_SLOT)
                    .withItem(this.createNavigationButton(Material.ARROW, "§aNext →", Collections.emptyList()))
                    .onClick(click -> {
                        this.currentPage.set(currentPageNum + 1, render);
                        this.renderPerks(render, player);
                        this.renderNavigationButtons(render, player);
                    });
        } else {
            render.slot(NEXT_PAGE_SLOT)
                    .withItem(this.createNavigationButton(Material.GRAY_DYE, "§8Next →", Collections.singletonList("§7No next page")));
        }
    }

    private void handlePerkClick(@NotNull RenderContext render, @NotNull Player player, @NotNull PerkDisplayItem item) {
        final RDQPlayer rdqPlayer = this.perkStateService.getRDQPlayer(player);
        if (rdqPlayer == null) return;

        switch (item.state) {
            case AVAILABLE -> {
                item.perk.type().activate(player, item.perk);
                this.loadPerkData(render, player);
                this.renderDynamicContent(render, player);
            }
            case ACTIVE -> {
                item.perk.type().deactivate(player, item.perk);
                this.loadPerkData(render, player);
                this.renderDynamicContent(render, player);
            }
            case LOCKED -> {
                player.sendMessage("§cYou don't own this perk!");
            }
            case COOLDOWN -> {
                player.sendMessage("§cPerk is on cooldown!");
            }
            case DISABLED -> {
                player.sendMessage("§cThis perk is disabled!");
            }
        }
    }

    private void renderErrorState(@NotNull RenderContext render, @NotNull Player player) {
        final ItemStack errorItem = UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("error.name", player).build().component())
                .setLore(this.i18n("error.lore", player).build().splitLines())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();

        render.slot(22).withItem(errorItem);
    }

    private @NotNull ItemStack createCategoryButton(@Nullable EPerkCategory category, boolean selected, @NotNull Player player) {
        final Material material;
        final String name;
        final List<String> lore = new ArrayList<>();

        if (category == null) {
            material = Material.COMPASS;
            name = "§f§lAll Categories";
            lore.add("§7View all perks");
        } else {
            material = category.getIconMaterial();
            name = "§f§l" + category.getDisplayName();
            lore.add("§7View " + category.getDisplayName().toLowerCase() + " perks");
        }

        if (selected) {
            lore.add("");
            lore.add("§a§l✓ Selected");
        } else {
            lore.add("");
            lore.add("§e§lClick to select");
        }

        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack createPerkItem(@NotNull PerkDisplayItem item, @NotNull Player player) {
        final Material material = item.category.getIconMaterial();
        final String name = item.state.getColorCode() + "§l" + item.perk.getId();
        final List<String> lore = new ArrayList<>();

        lore.add("§7Category: §f" + item.category.getDisplayName());
        lore.add("§7State: " + item.state.getColoredDisplayName());

        lore.add("");
        switch (item.state) {
            case AVAILABLE -> lore.add("§a§l▶ Click to activate");
            case ACTIVE -> lore.add("§c§l⏸ Click to deactivate");
            case LOCKED -> lore.add("§c§l✗ Locked");
            case COOLDOWN -> lore.add("§6§l⏳ On cooldown");
            case DISABLED -> lore.add("§8§l✗ Disabled");
        }

        final ItemStack stack = new ItemStack(material);
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private @NotNull ItemStack createNavigationButton(@NotNull Material material, @NotNull String name, @NotNull List<String> lore) {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private @Nullable PerkDisplayItem createDisplayItem(@NotNull RenderContext context, @NotNull LoadedPerk loadedPerk, @NotNull RDQPlayer rdqPlayer, @NotNull Player player) {
        final EPerkCategory category = loadedPerk.config().category();
        final EPerkState state = this.calculateState(context, loadedPerk, rdqPlayer, player);

        return new PerkDisplayItem(loadedPerk, category, state);
    }

    private @NotNull EPerkState calculateState(@NotNull RenderContext context, @NotNull LoadedPerk loadedPerk, @NotNull RDQPlayer rdqPlayer, @NotNull Player player) {
        final PerkManager perkManager = this.rdq.get(context).getPerkInitializationManager().getPerkManager();
        
        if (!loadedPerk.config().enabled()) {
            return EPerkState.DISABLED;
        }
        
        if (perkManager.isActive(player, loadedPerk.getId())) {
            return EPerkState.ACTIVE;
        }
        
        return EPerkState.AVAILABLE;
    }

    private int compareDisplayItems(@NotNull PerkDisplayItem a, @NotNull PerkDisplayItem b) {
        final int stateCompare = Integer.compare(a.state.ordinal(), b.state.ordinal());
        if (stateCompare != 0) return stateCompare;
        return a.perk.getId().compareTo(b.perk.getId());
    }

    private static class PerkDisplayItem {
        final LoadedPerk perk;
        final EPerkCategory category;
        final EPerkState state;

        PerkDisplayItem(LoadedPerk perk, EPerkCategory category, EPerkState state) {
            this.perk = perk;
            this.category = category;
            this.state = state;
        }
    }
}