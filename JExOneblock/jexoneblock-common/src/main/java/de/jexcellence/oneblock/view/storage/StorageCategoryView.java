package de.jexcellence.oneblock.view.storage;

import com.raindropcentral.rplatform.utility.heads.view.Next;
import com.raindropcentral.rplatform.utility.heads.view.Previous;
import com.raindropcentral.rplatform.utility.heads.view.Return;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.jextranslate.i18n.I18n;
import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.storage.StorageCategory;
import de.jexcellence.oneblock.manager.IslandStorageManager;
import de.jexcellence.oneblock.view.framework.LargeInventoryView;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class StorageCategoryView extends LargeInventoryView {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    private final State<JExOneblock> plugin = initialState("plugin");
    private final State<OneblockIsland> island = initialState("island");
    private final State<StorageCategory> category = initialState("category");
    private final MutableState<Integer> currentPage = mutableState(0);
    private final MutableState<SortMode> sortMode = mutableState(SortMode.QUANTITY);
    
    private static final int ITEMS_PER_PAGE = 35;
    
    // Slot definitions
    private static final int CATEGORY_INFO_SLOT = 4;
    private static final int NAVIGATION_LEFT_SLOT = 18;
    private static final int NAVIGATION_RIGHT_SLOT = 26;
    private static final int FILTER_SLOT = 46;
    private static final int SORT_SLOT = 47;
    private static final int WITHDRAW_ALL_SLOT = 52;
    private static final int BACK_BUTTON_SLOT = 45;
    
    private static final List<Integer> ITEM_SLOTS = List.of(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    );
    
    /**
     * Sort modes for category items
     */
    public enum SortMode {
        QUANTITY("Quantity", Material.CHEST),
        ALPHABETICAL("Alphabetical", Material.BOOK),
        RARITY("Rarity", Material.NETHER_STAR),
        RECENT("Recent", Material.CLOCK);
        
        private final String displayName;
        private final Material icon;
        
        SortMode(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }
        
        public String getDisplayName() { return displayName; }
        public Material getIcon() { return icon; }
    }
    
    public StorageCategoryView() {
    }
    
    @Override
    protected String getKey() {
        return "storage_category_enhanced";
    }
    
    @Override
    protected @NotNull LayoutTemplate getLayoutTemplate() {
        return LayoutTemplate.FULL_CONTENT;
    }
    
    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(@NotNull OpenContext openContext) {
        try {
            StorageCategory categoryData = category.get(openContext);
            OneblockIsland islandData = island.get(openContext);
            
            if (categoryData != null && islandData != null) {
                IslandStorageManager storageManager = plugin.get(openContext).getIslandStorageManager();
                if (storageManager != null) {
                    long itemCount = getCategoryItemCount(storageManager, islandData.getId(), categoryData);
                    
                    return Map.of(
                        "category", categoryData.getDisplayName(),
                        "count", String.valueOf(itemCount)
                    );
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to get category info for title: " + e.getMessage());
        }
        
        return Map.of(
            "category", "Unknown",
            "count", "0"
        );
    }
    
    @Override
    protected void renderContent(@NotNull RenderContext render, @NotNull Player player) {
        try {
            OneblockIsland islandData = island.get(render);
            JExOneblock pluginInstance = plugin.get(render);
            StorageCategory categoryData = category.get(render);
            
            if (islandData == null || pluginInstance == null || categoryData == null) {
                renderErrorState(render, player);
                return;
            }
            
            IslandStorageManager storageManager = pluginInstance.getIslandStorageManager();
            if (storageManager == null) {
                renderErrorState(render, player);
                return;
            }
            
            renderStaticComponents(render, player, categoryData);
            renderCategoryItems(render, player, storageManager, islandData, categoryData);
            
        } catch (Exception e) {
            LOGGER.severe("Error rendering storage category view: " + e.getMessage());
            e.printStackTrace();
            renderErrorState(render, player);
        }
    }
    
    private void renderStaticComponents(@NotNull RenderContext render, @NotNull Player player, 
                                      @NotNull StorageCategory categoryData) {
        
        render.slot(CATEGORY_INFO_SLOT)
              .renderWith(() -> createCategoryInfoItem(player, categoryData));

        render.slot(NAVIGATION_LEFT_SLOT)
              .renderWith(() -> createNavigationItem(new Previous(), player, "previous"))
              .onClick(click -> handlePreviousPage(click));

        render.slot(NAVIGATION_RIGHT_SLOT)
              .renderWith(() -> createNavigationItem(new Next(), player, "next"))
              .onClick(click -> handleNextPage(click));

        render.slot(FILTER_SLOT)
              .renderWith(() -> createFilterButton(player))
              .onClick(click -> handleFilter(click));
              
        render.slot(SORT_SLOT)
              .renderWith(() -> createSortButton(player))
              .onClick(click -> handleSort(click));
              
        render.slot(WITHDRAW_ALL_SLOT)
              .renderWith(() -> createWithdrawAllButton(player, categoryData))
              .onClick(click -> handleWithdrawAll(click, categoryData));

        render.slot(BACK_BUTTON_SLOT)
              .renderWith(() -> new Return().getHead(player))
              .onClick(click -> click.back());

        fillBorderSlots(render, player);
    }
    
    private void renderCategoryItems(@NotNull RenderContext render, @NotNull Player player,
                                   @NotNull IslandStorageManager storageManager, @NotNull OneblockIsland islandData,
                                   @NotNull StorageCategory categoryData) {
        
        var storage = storageManager.getStorage(islandData.getId());
        if (storage == null) {
            storage = storageManager.getOrCreateStorage(islandData.getId(), player.getUniqueId());
        }
        
        var storedItems = storage.getStoredItems();
        var categoryItems = new java.util.ArrayList<Map.Entry<Material, Long>>();
        
        for (var entry : storedItems.entrySet()) {
            if (StorageCategory.categorize(entry.getKey()) == categoryData) {
                categoryItems.add(entry);
            }
        }
        
        int currentPageNum = currentPage.get(render);
        int startIndex = currentPageNum * ITEMS_PER_PAGE;

        for (int i = 0; i < ITEM_SLOTS.size(); i++) {
            int slotIndex = ITEM_SLOTS.get(i);
            int itemIndex = startIndex + i;

            render.slot(slotIndex)
                .renderWith(() -> {
                    if (itemIndex < categoryItems.size()) {
                        return createItemDisplay(player, categoryItems.get(itemIndex).getKey(), categoryItems.get(itemIndex).getValue());
                    } else {
                        return createEmptySlot(player);
                    }
                })
                .updateOnStateChange(currentPage)
                .onClick(click -> {
                    if (itemIndex < categoryItems.size()) {
                        handleItemClick(click, categoryItems.get(itemIndex).getKey(), categoryItems.get(itemIndex).getValue());
                    }
                });
        }
    }
    
    private void handlePreviousPage(@NotNull me.devnatan.inventoryframework.context.SlotClickContext click) {
        int current = currentPage.get(click);
        if (current > 0) {
            currentPage.set(current - 1, click);
        }
    }

    private void handleNextPage(@NotNull me.devnatan.inventoryframework.context.SlotClickContext click) {
        OneblockIsland islandData = island.get(click);
        StorageCategory categoryData = category.get(click);
        
        if (islandData != null && categoryData != null) {
            IslandStorageManager storageManager = plugin.get(click).getIslandStorageManager();
            if (storageManager != null) {
                var storage = storageManager.getStorage(islandData.getId());
                if (storage == null) {
                    storage = storageManager.getOrCreateStorage(islandData.getId(), click.getPlayer().getUniqueId());
                }
                
                var storedItems = storage.getStoredItems();
                var categoryItems = new java.util.ArrayList<Map.Entry<Material, Long>>();
                
                for (var entry : storedItems.entrySet()) {
                    if (StorageCategory.categorize(entry.getKey()) == categoryData) {
                        categoryItems.add(entry);
                    }
                }
                
                int totalPages = Math.max(1, (int) Math.ceil((double) categoryItems.size() / ITEMS_PER_PAGE));
                int current = currentPage.get(click);
                
                if (current < totalPages - 1) {
                    currentPage.set(current + 1, click);
                }
            }
        }
    }
    
    private @NotNull org.bukkit.inventory.ItemStack createCategoryInfoItem(@NotNull Player player, 
                                                                          @NotNull StorageCategory categoryData) {
        return UnifiedBuilderFactory
            .item(categoryData.getIcon())
            .setName(Component.text("§a" + categoryData.getDisplayName()))
            .setLore(List.of(
                Component.text("§7Category: §f" + categoryData.getDisplayName()),
                Component.text("§7Items in category: §f" + categoryData.getMaterials().size()),
                Component.empty(),
                Component.text("§eShowing items in this category")
            ))
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
    
    private @NotNull org.bukkit.inventory.ItemStack createNavigationItem(@NotNull Object headProvider, 
                                                                        @NotNull Player player, 
                                                                        @NotNull String direction) {
        try {
            org.bukkit.inventory.ItemStack headItem = extractHeadFromProvider(headProvider, player);
            return UnifiedBuilderFactory.item(headItem)
                .setName(i18n("nav." + direction, player).build().component())
                .setLore(List.of())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
        } catch (Exception e) {
            return UnifiedBuilderFactory.item(Material.ARROW)
                .setName(Component.text("§e" + direction))
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
        }
    }
    
    private @NotNull org.bukkit.inventory.ItemStack extractHeadFromProvider(@NotNull Object headProvider, 
                                                                           @NotNull Player player) {
        if (headProvider instanceof Previous) {
            return ((Previous) headProvider).getHead(player);
        } else if (headProvider instanceof Next) {
            return ((Next) headProvider).getHead(player);
        }
        return UnifiedBuilderFactory.item(Material.ARROW).addItemFlags(ItemFlag.HIDE_ATTRIBUTES).build();
    }
    
    private @NotNull org.bukkit.inventory.ItemStack createFilterButton(@NotNull Player player) {
        return UnifiedBuilderFactory
            .item(Material.HOPPER)
            .setName(i18n("storage_category.actions.filter", player).build().component())
            .setLore(i18n("storage_category.actions.filter_lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
    
    private @NotNull org.bukkit.inventory.ItemStack createSortButton(@NotNull Player player) {
        return UnifiedBuilderFactory
            .item(Material.COMPARATOR)
            .setName(i18n("storage_category.actions.sort", player).build().component())
            .setLore(i18n("storage_category.actions.sort_lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
    
    private @NotNull org.bukkit.inventory.ItemStack createWithdrawAllButton(@NotNull Player player, 
                                                                           @NotNull StorageCategory categoryData) {
        return UnifiedBuilderFactory
            .item(Material.ENDER_CHEST)
            .setName(i18n("storage_category.actions.withdraw_all", player)
                .withPlaceholder("category", categoryData.getDisplayName())
                .build().component())
            .setLore(i18n("storage_category.actions.withdraw_all_lore", player)
                .withPlaceholder("category", categoryData.getDisplayName())
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
    
    private @NotNull org.bukkit.inventory.ItemStack createItemDisplay(@NotNull Player player, @NotNull Material material, @NotNull Long quantity) {
        var category = StorageCategory.categorize(material);
        return UnifiedBuilderFactory
            .item(material)
            .setName(Component.text("§a" + formatMaterialName(material)))
            .setLore(List.of(
                Component.text("§7Quantity: §f" + formatNumber(quantity)),
                Component.text("§7Category: §f" + (category != null ? category.getDisplayName() : "Unknown")),
                Component.empty(),
                Component.text("§eLeft click to withdraw 1"),
                Component.text("§eRight click to withdraw stack"),
                Component.text("§eShift+click to withdraw all")
            ))
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
    
    private @NotNull org.bukkit.inventory.ItemStack createEmptySlot(@NotNull Player player) {
        return UnifiedBuilderFactory
            .item(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
            .setName(Component.text("§7Empty Slot"))
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
    
    private void fillBorderSlots(@NotNull RenderContext render, @NotNull Player player) {
        for (int i = 0; i < 9; i++) {
            if (i != 4) {
                render.slot(i, createBorderItem(player));
            }
        }
        for (int i = 45; i < 54; i++) {
            if (i != BACK_BUTTON_SLOT && i != FILTER_SLOT && i != SORT_SLOT && i != WITHDRAW_ALL_SLOT) {
                render.slot(i, createBorderItem(player));
            }
        }

        int[] sideBorderSlots = {9, 17, 18, 26, 27, 35, 36, 44};
        for (int slot : sideBorderSlots) {
            if (slot != NAVIGATION_LEFT_SLOT && slot != NAVIGATION_RIGHT_SLOT) {
                render.slot(slot, createBorderItem(player));
            }
        }
    }
    
    private @NotNull org.bukkit.inventory.ItemStack createBorderItem(@NotNull Player player) {
        return UnifiedBuilderFactory
            .item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(Component.text(" "))
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
    
    private void handleFilter(final @NotNull me.devnatan.inventoryframework.context.SlotClickContext click) {
        new I18n.Builder("storage.filter.coming_soon", click.getPlayer())
            .includePrefix().build().sendMessage();
    }
    
    private void handleSort(final @NotNull me.devnatan.inventoryframework.context.SlotClickContext click) {
        new I18n.Builder("storage.sort.coming_soon", click.getPlayer())
            .includePrefix().build().sendMessage();
    }
    
    private void handleWithdrawAll(final @NotNull me.devnatan.inventoryframework.context.SlotClickContext click, 
                                 final @NotNull StorageCategory categoryData) {
        new I18n.Builder("storage.withdraw_all.coming_soon", click.getPlayer())
            .withPlaceholder("category", categoryData.getDisplayName())
            .includePrefix().build().sendMessage();
    }
    
    private void handleItemClick(final @NotNull me.devnatan.inventoryframework.context.SlotClickContext click, 
                                final @NotNull Material material, final @NotNull Long quantity) {
        final Player player = click.getPlayer();
        
        switch (click.getClickOrigin().getClick()) {
            case LEFT -> new I18n.Builder("storage.withdraw.single", player)
                .withPlaceholder("material", material.name())
                .includePrefix().build().sendMessage();
            case RIGHT -> new I18n.Builder("storage.withdraw.stack", player)
                .withPlaceholder("material", material.name())
                .includePrefix().build().sendMessage();
            case SHIFT_LEFT, SHIFT_RIGHT -> new I18n.Builder("storage.withdraw.all", player)
                .withPlaceholder("material", material.name())
                .includePrefix().build().sendMessage();
            default -> new I18n.Builder("storage.interact.default", player)
                .withPlaceholder("material", material.name())
                .includePrefix().build().sendMessage();
        }
    }
    
    private void renderErrorState(@NotNull RenderContext render, @NotNull Player player) {
        render.slot(22, UnifiedBuilderFactory
            .item(Material.BARRIER)
            .setName(Component.text("§cCategory View Error"))
            .setLore(List.of(
                Component.text("§7Failed to load category data"),
                Component.text("§7Please try again later")
            ))
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        );
    }
    
    private @NotNull String formatMaterialName(@NotNull Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        StringBuilder formatted = new StringBuilder();
        
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (c == ' ') {
                formatted.append(' ');
                capitalizeNext = true;
            } else if (capitalizeNext) {
                formatted.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                formatted.append(c);
            }
        }
        
        return formatted.toString();
    }
    
    private @NotNull String formatNumber(long number) {
        if (number >= 1_000_000_000) return String.format("%.1fB", number / 1_000_000_000.0);
        if (number >= 1_000_000) return String.format("%.1fM", number / 1_000_000.0);
        if (number >= 1_000) return String.format("%.1fK", number / 1_000.0);
        return String.valueOf(number);
    }
    
    private @NotNull String formatTime(@NotNull java.time.LocalDateTime dateTime) {
        java.time.Duration duration = java.time.Duration.between(dateTime, java.time.LocalDateTime.now());
        
        if (duration.toDays() > 0) {
            return duration.toDays() + "d ago";
        } else if (duration.toHours() > 0) {
            return duration.toHours() + "h ago";
        } else if (duration.toMinutes() > 0) {
            return duration.toMinutes() + "m ago";
        } else {
            return "Just now";
        }
    }
    
    private long getCategoryItemCount(@NotNull IslandStorageManager storageManager, @NotNull Long islandId, @NotNull StorageCategory category) {
        var storage = storageManager.getStorage(islandId);
        if (storage == null) {
            return 0;
        }
        
        var storedItems = storage.getStoredItems();
        return storedItems.entrySet().stream()
            .filter(entry -> StorageCategory.categorize(entry.getKey()) == category)
            .mapToLong(Map.Entry::getValue)
            .sum();
    }
}