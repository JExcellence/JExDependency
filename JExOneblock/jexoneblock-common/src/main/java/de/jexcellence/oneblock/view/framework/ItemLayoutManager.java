package de.jexcellence.oneblock.view.framework;

import de.jexcellence.jextranslate.i18n.I18n;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Item Layout Manager
 * 
 * Provides utilities for creating and managing items in large inventory layouts.
 * Handles common item creation patterns, styling, and layout positioning.
 * 
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class ItemLayoutManager {
    
    /**
     * Creates a standard UI item with translation support
     * 
     * @param material the item material
     * @param nameKey the translation key for the name
     * @param descriptionKey the translation key for the description
     * @param player the player for translation context
     * @return configured ItemStack
     */
    @NotNull
    public static ItemStack createUIItem(
            @NotNull Material material,
            @NotNull String nameKey,
            @NotNull String descriptionKey,
            @NotNull Player player
    ) {
        return createUIItem(material, nameKey, descriptionKey, player, null);
    }
    
    /**
     * Creates a standard UI item with translation support and placeholders
     * 
     * @param material the item material
     * @param nameKey the translation key for the name
     * @param descriptionKey the translation key for the description
     * @param player the player for translation context
     * @param placeholders placeholder values for translation
     * @return configured ItemStack
     */
    @NotNull
    public static ItemStack createUIItem(
            @NotNull Material material,
            @NotNull String nameKey,
            @NotNull String descriptionKey,
            @NotNull Player player,
            @Nullable java.util.Map<String, Object> placeholders
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Set name
            I18n.Builder nameBuilder = new I18n.Builder(nameKey, player);
            if (placeholders != null) {
                nameBuilder.withPlaceholders(placeholders);
            }
            meta.displayName(nameBuilder.build().component());
            
            // Set lore
            I18n.Builder loreBuilder = new I18n.Builder(descriptionKey, player);
            if (placeholders != null) {
                loreBuilder.withPlaceholders(placeholders);
            }
            meta.lore(List.of(loreBuilder.build().component()));
            
            // Hide attributes
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Creates a border item for UI layouts
     * 
     * @return border ItemStack
     */
    @NotNull
    public static ItemStack createBorderItem() {
        return createBorderItem(Material.GRAY_STAINED_GLASS_PANE);
    }
    
    /**
     * Creates a border item with custom material
     * 
     * @param material the border material
     * @return border ItemStack
     */
    @NotNull
    public static ItemStack createBorderItem(@NotNull Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(Component.empty());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Creates a status indicator item
     * 
     * @param status the status (true = positive, false = negative)
     * @param nameKey the translation key for the name
     * @param player the player for translation context
     * @return status ItemStack
     */
    @NotNull
    public static ItemStack createStatusItem(
            boolean status,
            @NotNull String nameKey,
            @NotNull Player player
    ) {
        Material material = status ? Material.LIME_DYE : Material.RED_DYE;
        String statusKey = status ? "ui.status.enabled" : "ui.status.disabled";
        
        return createUIItem(material, nameKey, statusKey, player);
    }
    
    /**
     * Creates a progress bar item
     * 
     * @param progress the progress value (0.0 to 1.0)
     * @param nameKey the translation key for the name
     * @param player the player for translation context
     * @return progress ItemStack
     */
    @NotNull
    public static ItemStack createProgressItem(
            double progress,
            @NotNull String nameKey,
            @NotNull Player player
    ) {
        Material material = getProgressMaterial(progress);
        
        java.util.Map<String, Object> placeholders = java.util.Map.of(
            "progress", String.format("%.1f", progress * 100),
            "bar", createProgressBar(progress)
        );
        
        return createUIItem(material, nameKey, "ui.progress.description", player, placeholders);
    }
    
    /**
     * Creates a numerical display item
     * 
     * @param value the numerical value
     * @param nameKey the translation key for the name
     * @param player the player for translation context
     * @return numerical ItemStack
     */
    @NotNull
    public static ItemStack createNumericalItem(
            long value,
            @NotNull String nameKey,
            @NotNull Player player
    ) {
        Material material = getNumericalMaterial(value);
        
        java.util.Map<String, Object> placeholders = java.util.Map.of(
            "value", formatNumber(value),
            "raw_value", String.valueOf(value)
        );
        
        return createUIItem(material, nameKey, "ui.numerical.description", player, placeholders);
    }
    
    /**
     * Creates an action button item
     * 
     * @param actionType the type of action
     * @param nameKey the translation key for the name
     * @param player the player for translation context
     * @return action ItemStack
     */
    @NotNull
    public static ItemStack createActionItem(
            @NotNull ActionType actionType,
            @NotNull String nameKey,
            @NotNull Player player
    ) {
        return createUIItem(actionType.getMaterial(), nameKey, actionType.getDescriptionKey(), player);
    }
    
    /**
     * Creates a category header item
     * 
     * @param nameKey the translation key for the name
     * @param player the player for translation context
     * @return category header ItemStack
     */
    @NotNull
    public static ItemStack createCategoryHeader(
            @NotNull String nameKey,
            @NotNull Player player
    ) {
        return createUIItem(Material.NAME_TAG, nameKey, "ui.category.description", player);
    }
    
    /**
     * Gets the appropriate material for progress display
     */
    @NotNull
    private static Material getProgressMaterial(double progress) {
        if (progress >= 1.0) return Material.LIME_DYE;
        if (progress >= 0.75) return Material.YELLOW_DYE;
        if (progress >= 0.5) return Material.ORANGE_DYE;
        if (progress >= 0.25) return Material.RED_DYE;
        return Material.GRAY_DYE;
    }
    
    /**
     * Gets the appropriate material for numerical display
     */
    @NotNull
    private static Material getNumericalMaterial(long value) {
        if (value >= 1000000) return Material.DIAMOND;
        if (value >= 100000) return Material.EMERALD;
        if (value >= 10000) return Material.GOLD_INGOT;
        if (value >= 1000) return Material.IRON_INGOT;
        if (value >= 100) return Material.COPPER_INGOT;
        return Material.COAL;
    }
    
    /**
     * Creates a visual progress bar
     */
    @NotNull
    private static String createProgressBar(double progress) {
        int totalBars = 20;
        int filledBars = (int) (progress * totalBars);
        
        StringBuilder bar = new StringBuilder();
        bar.append("§a");
        for (int i = 0; i < filledBars; i++) {
            bar.append("█");
        }
        bar.append("§7");
        for (int i = filledBars; i < totalBars; i++) {
            bar.append("█");
        }
        
        return bar.toString();
    }
    
    /**
     * Formats a number for display
     */
    @NotNull
    private static String formatNumber(long number) {
        if (number >= 1_000_000_000) {
            return String.format("%.1fB", number / 1_000_000_000.0);
        } else if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        } else {
            return String.valueOf(number);
        }
    }
    
    /**
     * Action type enumeration
     */
    public enum ActionType {
        CREATE(Material.EMERALD, "ui.action.create.description"),
        EDIT(Material.WRITABLE_BOOK, "ui.action.edit.description"),
        DELETE(Material.BARRIER, "ui.action.delete.description"),
        VIEW(Material.ENDER_EYE, "ui.action.view.description"),
        CONFIGURE(Material.REDSTONE, "ui.action.configure.description"),
        UPGRADE(Material.EXPERIENCE_BOTTLE, "ui.action.upgrade.description"),
        RESET(Material.TNT, "ui.action.reset.description"),
        COPY(Material.PAPER, "ui.action.copy.description"),
        MOVE(Material.MINECART, "ui.action.move.description"),
        TOGGLE(Material.LEVER, "ui.action.toggle.description");
        
        private final Material material;
        private final String descriptionKey;
        
        ActionType(Material material, String descriptionKey) {
            this.material = material;
            this.descriptionKey = descriptionKey;
        }
        
        public Material getMaterial() { return material; }
        public String getDescriptionKey() { return descriptionKey; }
    }
    
    /**
     * Layout position utility
     */
    public static class LayoutPosition {
        private final int row;
        private final int column;
        
        public LayoutPosition(int row, int column) {
            this.row = row;
            this.column = column;
        }
        
        public int getRow() { return row; }
        public int getColumn() { return column; }
        public int getSlot() { return row * 9 + column; }
        
        public static LayoutPosition fromSlot(int slot) {
            return new LayoutPosition(slot / 9, slot % 9);
        }
    }
    
    /**
     * Layout grid utility for positioning items
     */
    public static class LayoutGrid {
        private final int rows;
        private final int columns;
        private final boolean[][] occupied;
        
        public LayoutGrid(int rows, int columns) {
            this.rows = rows;
            this.columns = columns;
            this.occupied = new boolean[rows][columns];
        }
        
        public boolean isOccupied(int row, int column) {
            return occupied[row][column];
        }
        
        public void setOccupied(int row, int column, boolean occupied) {
            this.occupied[row][column] = occupied;
        }
        
        public LayoutPosition findNextFree() {
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < columns; col++) {
                    if (!occupied[row][col]) {
                        return new LayoutPosition(row, col);
                    }
                }
            }
            return null;
        }
        
        public List<LayoutPosition> findFreePositions(int count) {
            List<LayoutPosition> positions = new java.util.ArrayList<>();
            
            for (int row = 0; row < rows && positions.size() < count; row++) {
                for (int col = 0; col < columns && positions.size() < count; col++) {
                    if (!occupied[row][col]) {
                        positions.add(new LayoutPosition(row, col));
                    }
                }
            }
            
            return positions;
        }
    }
}