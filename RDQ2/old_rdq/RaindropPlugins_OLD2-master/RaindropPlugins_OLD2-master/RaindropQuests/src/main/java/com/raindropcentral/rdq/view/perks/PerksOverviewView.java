package com.raindropcentral.rdq.view.perks;

import com.raindropcentral.rdq.RDQImpl;
import com.raindropcentral.rdq.database.entity.RDQPlayer;
import com.raindropcentral.rdq.service.PerkManagementService;
import com.raindropcentral.rdq.type.EPerkCategory;
import com.raindropcentral.rdq.type.EPerkState;
import com.raindropcentral.rplatform.logger.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.common.BaseView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
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
 * Comprehensive GUI view for displaying and managing player perks in RaindropQuests.
 * <p>
 * This view provides a sophisticated interface for players to browse, filter, and manage their perks.
 * Features include:
 * <ul>
 *   <li>Category-based filtering with dedicated category buttons</li>
 *   <li>Pagination support for large numbers of perks</li>
 *   <li>State-based visual indicators (locked, available, active, cooldown, disabled)</li>
 *   <li>Interactive perk activation/deactivation</li>
 *   <li>Real-time cooldown display</li>
 *   <li>Internationalization support</li>
 * </ul>
 * </p>
 *
 * <p>
 * The view is organized with category buttons at the top, perk display area in the middle,
 * and navigation controls at the bottom. The layout is designed to be intuitive and
 * provide easy access to all perk management functionality.
 * </p>
 *
 * @author ItsRainingHP
 * @version 2.0.0
 * @since TBD
 */
public class PerksOverviewView extends BaseView {
    
    private static final Logger LOGGER = CentralLogger.getLogger(PerksOverviewView.class.getName());
    
    // Layout constants
    private static final int INVENTORY_SIZE = 54; // 6 rows
    private static final int CATEGORY_ROW_START = 0;
    private static final int CATEGORY_ROW_END = 8;
    private static final int PERKS_AREA_START = 18; // Row 3
    private static final int PERKS_AREA_END = 44;   // Row 5
    private static final int PERKS_PER_PAGE = 27;   // 3 rows × 9 columns
    private static final int NAVIGATION_ROW = 45;   // Row 6
    
    // Navigation button positions
    private static final int PREVIOUS_PAGE_SLOT = 45;
    private static final int PAGE_INFO_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int ALL_CATEGORIES_SLOT = 4; // Center of category row
    
    /**
     * State for storing the main plugin instance.
     */
    private final State<RDQImpl> rdq = initialState("plugin");
    
    /**
     * State for storing the current player entity.
     */
    private final State<RDQPlayer> player = initialState("player");
    
    /**
     * State for the currently selected category filter.
     */
    private final MutableState<EPerkCategory> selectedCategory = mutableState(EPerkCategory.COMBAT);
    
    /**
     * State for the current page number (0-based).
     */
    private final MutableState<Integer> currentPage = mutableState(0);
    
    /**
     * State for storing all perk display data.
     */
    private final MutableState<List<PerkDisplayData>> allPerks = mutableState(new ArrayList<>());
    
    /**
     * State for storing filtered perks based on current category.
     */
    private final MutableState<List<PerkDisplayData>> filteredPerks = mutableState(new ArrayList<>());
    
    /**
     * State for tracking data refresh timestamp.
     */
    private final MutableState<Long> dataRefreshTimestamp = mutableState(0L);
    
    /**
     * Cached perk management service instance.
     */
    private PerkManagementService perkService;
    
    @Override
    protected String getKey() {
        return "perk_overview_ui";
    }
    
    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext openContext) {
        try {
            final EPerkCategory selectedCat = this.selectedCategory.get(openContext);
            final String categoryName = selectedCat != null ? selectedCat.getDisplayName() : "All Categories";
            
            return Map.of(
                "category_name", categoryName,
                "player_name", openContext.getPlayer().getName()
            );
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to generate title placeholders", e);
            return Map.of(
                "category_name", "Perks",
                "player_name", openContext.getPlayer().getName()
            );
        }
    }
    
    @Override
    public void onResume(final @NotNull Context origin, final @NotNull Context target) {
        try {
            LOGGER.log(Level.FINE, "PerksOverviewView resumed, checking if data refresh is needed");
            
            final long currentTime = System.currentTimeMillis();
            final long lastRefresh = this.dataRefreshTimestamp.get(target);
            final long timeSinceRefresh = currentTime - lastRefresh;
            
            // Force refresh if more than 30 seconds have passed
            if (timeSinceRefresh > 30_000L) {
                LOGGER.log(Level.INFO, "Refreshing cached data due to time elapsed: {}ms", timeSinceRefresh);
                this.initializeAndLoadData((RenderContext) target, target.getPlayer());
            }
            
            this.renderDynamicContent((RenderContext) target, target.getPlayer());
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to refresh data on resume", e);
        }
    }
    
    @Override
    public void onFirstRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        try {
            LOGGER.log(Level.INFO, "Starting perks overview render for player: {}", player.getName());
            
            // Initialize service if needed
            if (this.perkService == null) {
                final RDQImpl plugin = this.rdq.get(render);
                this.perkService = new PerkManagementService(plugin);
            }
            
            // Initialize and load data
            this.initializeAndLoadData(render, player);
            
            // Render all components
            this.renderStaticComponents(render, player);
            this.renderDynamicContent(render, player);
            
            LOGGER.log(Level.INFO, "Perks overview render completed successfully");
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Critical error during perks overview render", e);
            this.renderErrorState(render, player);
        }
    }
    
    /**
     * Initializes and loads all perk data for the player.
     *
     * @param render the render context
     * @param player the player to load perks for
     */
    private void initializeAndLoadData(final @NotNull RenderContext render, final @NotNull Player player) {
        try {
            LOGGER.log(Level.FINE, "Loading perk data for player: {}", player.getName());
            
            // Load perks asynchronously and update UI when complete
            this.perkService.loadPlayerPerks(player)
                .thenAccept(perkData -> {
                    this.allPerks.set(perkData, render);
                    this.updateFilteredPerks(render);
                    this.dataRefreshTimestamp.set(System.currentTimeMillis(), render);
                    
                    // Re-render dynamic content
                    this.renderDynamicContent(render, player);
                    
                    LOGGER.log(Level.FINE, "Loaded {} perks for player {}", new Object[]{perkData.size(), player.getName()});
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Error loading perk data for player " + player.getName(), throwable);
                    this.allPerks.set(new ArrayList<>(), render);
                    this.updateFilteredPerks(render);
                    return null;
                });
                
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize and load perk data", e);
            this.allPerks.set(new ArrayList<>(), render);
            this.updateFilteredPerks(render);
        }
    }
    
    /**
     * Renders static UI components that don't change based on data.
     *
     * @param render the render context
     * @param player the player viewing the GUI
     */
    private void renderStaticComponents(final @NotNull RenderContext render, final @NotNull Player player) {
        try {
            this.renderDecorations(render);
            LOGGER.log(Level.FINE, "Static components rendered successfully");
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to render static components", e);
        }
    }
    
    /**
     * Renders dynamic content that changes based on data and user interaction.
     *
     * @param render the render context
     * @param player the player viewing the GUI
     */
    private void renderDynamicContent(final @NotNull RenderContext render, final @NotNull Player player) {
        try {
            this.renderCategoryButtons(render, player);
            this.renderPerks(render, player);
            this.renderNavigationButtons(render, player);
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to render dynamic content", e);
        }
    }
    
    /**
     * Updates the filtered perks list based on the current category selection.
     *
     * @param render the render context
     */
    private void updateFilteredPerks(final @NotNull RenderContext render) {
        final List<PerkDisplayData> allPerksList = this.allPerks.get(render);
        final EPerkCategory category = this.selectedCategory.get(render);
        
        final List<PerkDisplayData> filtered = allPerksList.stream()
            .filter(perk -> perk.matchesCategory(category))
            .sorted(this::comparePerksByPriority)
            .collect(Collectors.toList());

        this.filteredPerks.set(filtered, render);

        // Reset to first page when filter changes
        this.currentPage.set(0, render);
    }
    
    /**
     * Compares perks for sorting by priority and state.
     *
     * @param a first perk
     * @param b second perk
     * @return comparison result
     */
    private int comparePerksByPriority(final @NotNull PerkDisplayData a, final @NotNull PerkDisplayData b) {
        // Sort by state priority first (active > available > cooldown > locked > disabled)
        final int stateComparison = compareStates(a.getState(), b.getState());
        if (stateComparison != 0) {
            return stateComparison;
        }
        
        // Then by perk priority
        final int priorityComparison = Integer.compare(b.getPerk().getPriority(), a.getPerk().getPriority());
        if (priorityComparison != 0) {
            return priorityComparison;
        }
        
        // Finally by identifier for consistent ordering
        return a.getIdentifier().compareTo(b.getIdentifier());
    }
    
    /**
     * Compares perk states for sorting priority.
     *
     * @param a first state
     * @param b second state
     * @return comparison result
     */
    private int compareStates(final @NotNull EPerkState a, final @NotNull EPerkState b) {
        final int[] statePriority = {
            4, // ACTIVE
            3, // AVAILABLE
            2, // COOLDOWN
            1, // LOCKED
            0  // DISABLED
        };
        
        return Integer.compare(statePriority[b.ordinal()], statePriority[a.ordinal()]);
    }
    
    /**
     * Renders category filter buttons in the top row.
     *
     * @param render the render context
     * @param player the player viewing the GUI
     */
    private void renderCategoryButtons(final @NotNull RenderContext render, final @NotNull Player player) {
        final EPerkCategory selectedCat = this.selectedCategory.get(render);
        
        // "All Categories" button
        render.slot(ALL_CATEGORIES_SLOT)
            .withItem(createCategoryButton(null, selectedCat, player))
            .onClick(click -> {
                this.selectedCategory.set(null, render);
                updateFilteredPerks(render);
                renderPerks(render, player);
                renderNavigationButtons(render, player);
                renderCategoryButtons(render, player); // Refresh category buttons
            });
        
        // Individual category buttons
        final EPerkCategory[] categories = EPerkCategory.values();
        int slot = 0;
        
        for (final EPerkCategory category : categories) {
            if (slot == ALL_CATEGORIES_SLOT) {
                slot++; // Skip the "All Categories" slot
            }
            
            if (slot > CATEGORY_ROW_END) {
                break; // Don't exceed the category row
            }
            
            render.slot(slot)
                .withItem(createCategoryButton(category, selectedCat, player))
                .onClick(click -> {
                    this.selectedCategory.set(category, render);
                    updateFilteredPerks(render);
                    renderPerks(render, player);
                    renderNavigationButtons(render, player);
                    renderCategoryButtons(render, player); // Refresh category buttons
                });
            
            slot++;
        }
    }
    
    /**
     * Creates an item stack for a category button.
     *
     * @param category     the category (null for "All Categories")
     * @param selectedCat  the currently selected category
     * @param player       the player viewing the GUI
     * @return the category button item stack
     */
    private @NotNull ItemStack createCategoryButton(
        final @Nullable EPerkCategory category,
        final @Nullable EPerkCategory selectedCat,
        final @NotNull Player player
    ) {
        final Material material;
        final String name;
        final List<String> lore = new ArrayList<>();
        final boolean isSelected;
        
        if (category == null) {
            // "All Categories" button
            material = Material.COMPASS;
            name = "§f§lAll Categories";
            lore.add("§7View perks from all categories");
            isSelected = selectedCat == null;
        } else {
            material = category.getIconMaterial();
            name = "§f§l" + category.getDisplayName();
            lore.add("§7View " + category.getDisplayName().toLowerCase() + " perks");
            isSelected = category == selectedCat;
        }
        
        // Add selection indicator
        if (isSelected) {
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
    
    /**
     * Renders perk items in the main display area.
     *
     * @param render the render context
     * @param player the player viewing the GUI
     */
    private void renderPerks(final @NotNull RenderContext render, final @NotNull Player player) {
        final List<PerkDisplayData> filtered = this.filteredPerks.get(render);
        final int page = this.currentPage.get(render);
        final int startIndex = page * PERKS_PER_PAGE;
        final int endIndex = Math.min(startIndex + PERKS_PER_PAGE, filtered.size());
        
        // Clear the perks area first
        for (int slot = PERKS_AREA_START; slot <= PERKS_AREA_END; slot++) {
            render.slot(slot, new ItemStack(Material.AIR));
        }
        
        // Render perks for current page
        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            final PerkDisplayData perkData = filtered.get(i);
            final int slot = PERKS_AREA_START + slotIndex;
            
            render.slot(slot)
                .withItem(createPerkItem(perkData, player))
                .onClick(click -> handlePerkClick(render, player, perkData));
            
            slotIndex++;
        }
    }
    
    /**
     * Creates an item stack representing a perk.
     *
     * @param perkData the perk display data
     * @param player   the player viewing the GUI
     * @return the perk item stack
     */
    private @NotNull ItemStack createPerkItem(
        final @NotNull PerkDisplayData perkData,
        final @NotNull Player player
    ) {
        final EPerkState state = perkData.getState();
        final Material material = determinePerkMaterial(perkData);
        final String name = state.getColorCode() + "§l" + perkData.getIdentifier(); // TODO: Use i18n
        final List<String> lore = new ArrayList<>();
        
        // Add description
        lore.add("§7" + "Description for " + perkData.getIdentifier()); // TODO: Use i18n
        lore.add("");
        
        // Add state information
        lore.add("§7State: " + state.getColoredDisplayName());
        
        // Add category information
        lore.add("§7Category: §f" + perkData.getCategory().getDisplayName());
        
        // Add cooldown information if applicable
        if (perkData.isOnCooldown()) {
            lore.add("§7Cooldown: §c" + perkData.getFormattedCooldown());
        }
        
        // Add action hints
        lore.add("");
        if (perkData.canActivate()) {
            lore.add("§a§l▶ Click to activate");
        } else if (perkData.canToggle() && state == EPerkState.ACTIVE) {
            lore.add("§c§l⏸ Click to deactivate");
        } else if (state == EPerkState.LOCKED) {
            lore.add("§c§l✗ Locked");
        } else if (state == EPerkState.DISABLED) {
            lore.add("§8§l✗ Disabled");
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
    
    /**
     * Determines the appropriate material for a perk item based on its category and state.
     *
     * @param perkData the perk display data
     * @return the material to use for the perk item
     */
    private @NotNull Material determinePerkMaterial(final @NotNull PerkDisplayData perkData) {
        // Use category icon as base, but could be customized per perk
        return perkData.getCategory().getIconMaterial();
    }
    
    /**
     * Handles clicks on perk items.
     *
     * @param render    the render context
     * @param player    the player who clicked
     * @param perkData  the perk that was clicked
     */
    private void handlePerkClick(
        final @NotNull RenderContext render,
        final @NotNull Player player,
        final @NotNull PerkDisplayData perkData
    ) {
        final EPerkState state = perkData.getState();
        
        // Handle different click actions based on perk state
        switch (state) {
            case AVAILABLE -> {
                if (perkData.canActivate()) {
                    activatePerk(render, player, perkData);
                }
            }
            case ACTIVE -> {
                if (perkData.canToggle()) {
                    deactivatePerk(render, player, perkData);
                }
            }
            case LOCKED -> {
                player.sendMessage("§c§lThis perk is locked!"); // TODO: Use i18n
                // Could show unlock requirements here
            }
            case COOLDOWN -> {
                player.sendMessage("§c§lThis perk is on cooldown for " + perkData.getFormattedCooldown() + "!"); // TODO: Use i18n
            }
            case DISABLED -> {
                player.sendMessage("§c§lThis perk is currently disabled!"); // TODO: Use i18n
            }
        }
    }
    
    /**
     * Activates a perk for the player.
     *
     * @param render    the render context
     * @param player    the player
     * @param perkData  the perk to activate
     */
    private void activatePerk(
        final @NotNull RenderContext render,
        final @NotNull Player player,
        final @NotNull PerkDisplayData perkData
    ) {
        try {
            LOGGER.log(Level.INFO, "Attempting to activate perk {} for player {}", new Object[]{perkData.getIdentifier(), player.getName()});
            
            // Use the service to activate the perk
            this.perkService.activatePerk(player, perkData.getIdentifier(), false)
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        // Send success message
                        this.i18n("messages.perk_activated", player)
                            .withPlaceholder("perk_name", perkData.getIdentifier())
                            .includePrefix().sendMessage();
                        
                        // Refresh the view
                        this.refreshViewData(render, player);
                    } else {
                        // Send error message
                        this.i18n("messages.perk_activation_failed", player)
                            .withPlaceholder("perk_name", perkData.getIdentifier())
                            .withPlaceholder("reason", result.getMessage())
                            .includePrefix().sendMessage();
                    }
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Error activating perk " + perkData.getIdentifier(), throwable);
                    this.i18n("messages.perk_activation_error", player)
                        .withPlaceholder("perk_name", perkData.getIdentifier())
                        .includePrefix()
                        .sendMessage();
                    return null;
                });
                
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to activate perk " + perkData.getIdentifier(), e);
            player.sendMessage("§c§lError activating perk: " + perkData.getIdentifier());
        }
    }
    
    /**
     * Deactivates a perk for the player.
     *
     * @param render    the render context
     * @param player    the player
     * @param perkData  the perk to deactivate
     */
    private void deactivatePerk(
        final @NotNull RenderContext render,
        final @NotNull Player player,
        final @NotNull PerkDisplayData perkData
    ) {
        try {
            LOGGER.log(Level.INFO, "Attempting to deactivate perk {} for player {}", new Object[]{perkData.getIdentifier(), player.getName()});
            
            // Use the service to deactivate the perk
            this.perkService.deactivatePerk(player, perkData.getIdentifier())
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        // Send success message
                        this.i18n("messages.perk_deactivated", player)
                            .withPlaceholder("perk_name", perkData.getIdentifier())
                            .includePrefix().sendMessage();
                        
                        // Refresh the view
                        this.refreshViewData(render, player);
                    } else {
                        // Send error message
                        this.i18n("messages.perk_deactivation_failed", player)
                            .withPlaceholder("perk_name", perkData.getIdentifier())
                            .withPlaceholder("reason", result.getMessage())
                            .includePrefix().sendMessage();
                    }
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Error deactivating perk " + perkData.getIdentifier(), throwable);
                    this.i18n("messages.perk_deactivation_error", player)
                        .withPlaceholder("perk_name", perkData.getIdentifier())
                        .includePrefix()
                        .sendMessage();
                    return null;
                });
                
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to deactivate perk " + perkData.getIdentifier(), e);
            player.sendMessage("§c§lError deactivating perk: " + perkData.getIdentifier());
        }
    }
    
    /**
     * Refreshes the view data and re-renders dynamic content.
     *
     * @param render the render context
     * @param player the player
     */
    private void refreshViewData(final @NotNull RenderContext render, final @NotNull Player player) {
        try {
            // Clear player cache to force fresh data
            this.perkService.cleaRDQPlayerCache(player.getUniqueId());
            
            // Reload data
            this.initializeAndLoadData(render, player);
            
            LOGGER.log(Level.FINE, "Refreshed view data for player {}", player.getName());
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to refresh view data", e);
        }
    }
    
    /**
     * Renders an error state when critical errors occur.
     *
     * @param render the render context
     * @param player the player viewing the GUI
     */
    private void renderErrorState(final @NotNull RenderContext render, final @NotNull Player player) {
        try {
            final ItemStack errorItem = UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("error.critical", player).build().component())
                .setLore(this.i18n("error.critical.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
            
            render.slot(22).withItem(errorItem);
            
            // Fill other slots with background
            for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
                if (slot != 22) {
                    render.slot(slot).withItem(this.createBackgroundPane(player));
                }
            }
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to render error state", e);
        }
    }
    
    /**
     * Creates a background pane item.
     *
     * @param player the player viewing the GUI
     * @return the background pane item stack
     */
    private @NotNull ItemStack createBackgroundPane(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(Component.empty())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
    
    /**
     * Renders navigation buttons for pagination.
     *
     * @param render the render context
     * @param player the player viewing the GUI
     */
    @Override
    public void renderNavigationButtons(final @NotNull RenderContext render, final @NotNull Player player) {
        final List<PerkDisplayData> filtered = this.filteredPerks.get(render);
        final int currentPageNum = this.currentPage.get(render);
        final int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / PERKS_PER_PAGE));
        
        // Previous page button
        if (currentPageNum > 0) {
            render.slot(PREVIOUS_PAGE_SLOT)
                .withItem(createNavigationButton(Material.ARROW, "§a§l← Previous Page", Collections.emptyList()))
                .onClick(click -> {
                    this.currentPage.set(currentPageNum - 1, click);
                    renderPerks(render, player);
                    renderNavigationButtons(render, player);
                });
        } else {
            render.slot(PREVIOUS_PAGE_SLOT)
                .withItem(createNavigationButton(Material.GRAY_DYE, "§8§l← Previous Page",
                    Collections.singletonList("§7No previous page")));
        }
        
        // Page info
        render.slot(PAGE_INFO_SLOT)
            .withItem(createNavigationButton(Material.PAPER, "§f§lPage " + (currentPageNum + 1) + " of " + totalPages,
                Arrays.asList("§7Showing " + filtered.size() + " perks", "§7" + PERKS_PER_PAGE + " perks per page")));
        
        // Next page button
        if (currentPageNum < totalPages - 1) {
            render.slot(NEXT_PAGE_SLOT)
                .withItem(createNavigationButton(Material.ARROW, "§a§lNext Page →", Collections.emptyList()))
                .onClick(click -> {
                    this.currentPage.set(currentPageNum + 1, click);
                    renderPerks(render, player);
                    renderNavigationButtons(render, player);
                });
        } else {
            render.slot(NEXT_PAGE_SLOT)
                .withItem(createNavigationButton(Material.GRAY_DYE, "§8§lNext Page →",
                    Collections.singletonList("§7No next page")));
        }
    }
    
    /**
     * Creates a navigation button item stack.
     *
     * @param material the material for the button
     * @param name     the display name
     * @param lore     the lore lines
     * @return the navigation button item stack
     */
    private @NotNull ItemStack createNavigationButton(
        final @NotNull Material material,
        final @NotNull String name,
        final @NotNull List<String> lore
    ) {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Renders decorative elements and borders.
     *
     * @param render the render context
     */
    private void renderDecorations(final @NotNull RenderContext render) {
        // Add decorative glass panes to separate sections
        final ItemStack separator = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        final ItemMeta separatorMeta = separator.getItemMeta();
        if (separatorMeta != null) {
            separatorMeta.setDisplayName("§r");
            separator.setItemMeta(separatorMeta);
        }
        
        // Separator between categories and perks (row 2)
        for (int slot = 9; slot <= 17; slot++) {
            render.slot(slot).withItem(separator);
        }
    }
}