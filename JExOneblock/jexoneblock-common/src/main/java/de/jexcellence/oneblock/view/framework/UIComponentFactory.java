package de.jexcellence.oneblock.view.framework;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.Requirement;
import de.jexcellence.jextranslate.i18n.I18n;
import de.jexcellence.oneblock.bonus.Bonus;
import de.jexcellence.oneblock.bonus.EnhancedBonusSystem;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.service.AsyncEvolutionService;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * UI Component Factory
 * 
 * Creates reusable UI components for OneBlock views, including evolution displays,
 * requirement progress, bonus information, and other common UI elements.
 * 
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class UIComponentFactory {
    
    /**
     * Creates an evolution display item
     * 
     * @param evolution the evolution to display
     * @param player the player for translation context
     * @return evolution display ItemStack
     */
    @NotNull
    public static ItemStack createEvolutionItem(@NotNull OneblockEvolution evolution, @NotNull Player player) {
        Material material = evolution.getShowcase() != null ? evolution.getShowcase() : Material.GRASS_BLOCK;
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(new I18n.Builder("evolution.display.name", player)
                .withPlaceholder("evolution", evolution.getEvolutionName())
                .build().component());
            
            List<Component> lore = new ArrayList<>();
            
            lore.add(new I18n.Builder("evolution.display.level", player)
                .withPlaceholder("level", String.valueOf(evolution.getLevel()))
                .build().component());
            
            lore.add(new I18n.Builder("evolution.display.experience", player)
                .withPlaceholder("experience", String.format("%.1f", evolution.getExperienceToPass()))
                .build().component());
            
            if (evolution.getDescription() != null && !evolution.getDescription().isEmpty()) {
                lore.add(Component.empty());
                lore.add(new I18n.Builder("evolution.display.description", player)
                    .withPlaceholder("description", evolution.getDescription())
                    .build().component());
            }
            
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Creates a requirement progress item from EvolutionRequirement
     * 
     * @param requirement the requirement to display
     * @param player the player for translation context
     * @return requirement progress ItemStack
     */
    @NotNull
    public static ItemStack createRequirementItem(@NotNull AsyncEvolutionService.EvolutionRequirement requirement, @NotNull Player player) {
        Material material = getRequirementMaterial(requirement.getType());
        boolean isMet = requirement.isMet(player.getUniqueId());
        double progress = requirement.calculateProgress(player.getUniqueId());
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(new I18n.Builder("requirement.display.name", player)
                .withPlaceholder("requirement", requirement.getDescription())
                .build().component());
            
            List<Component> lore = new ArrayList<>();
            
            String statusKey = isMet ? "requirement.status.complete" : "requirement.status.incomplete";
            lore.add(new I18n.Builder(statusKey, player).build().component());
            
            lore.add(new I18n.Builder("requirement.display.progress", player)
                .withPlaceholder("progress", String.format("%.1f%%", progress * 100))
                .build().component());
            
            lore.add(new I18n.Builder("requirement.display.type", player)
                .withPlaceholder("type", requirement.getType())
                .build().component());
            
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Creates a requirement progress item from AbstractRequirement
     * 
     * @param requirement the requirement to display
     * @param player the player for translation context
     * @return requirement progress ItemStack
     */
    @NotNull
    public static ItemStack createRequirementItem(@NotNull AbstractRequirement requirement, @NotNull Player player) {
        Material material = getRequirementMaterial(requirement.getTypeId());
        boolean isMet = requirement.isMet(player);
        double progress = requirement.calculateProgress(player);
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(new I18n.Builder("requirement.display.name", player)
                .withPlaceholder("requirement", requirement.getDescriptionKey())
                .build().component());
            
            List<Component> lore = new ArrayList<>();
            
            String statusKey = isMet ? "requirement.status.complete" : "requirement.status.incomplete";
            lore.add(new I18n.Builder(statusKey, player).build().component());
            
            lore.add(new I18n.Builder("requirement.display.progress", player)
                .withPlaceholder("progress", String.format("%.1f%%", progress * 100))
                .build().component());
            
            lore.add(new I18n.Builder("requirement.display.type", player)
                .withPlaceholder("type", requirement.getTypeId())
                .build().component());
            
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Creates a bonus display item
     * 
     * @param bonus the bonus to display
     * @param player the player for translation context
     * @return bonus display ItemStack
     */
    @NotNull
    public static ItemStack createBonusItem(@NotNull EnhancedBonusSystem.EnhancedBonus bonus, @NotNull Player player) {
        Material material = getBonusMaterial(bonus.getType());
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(new I18n.Builder("bonus.display.name", player)
                .withPlaceholder("bonus", bonus.getDescription())
                .build().component());
            
            List<Component> lore = new ArrayList<>();
            
            lore.add(new I18n.Builder("bonus.display.value", player)
                .withPlaceholder("value", bonus.getFormattedDescription())
                .build().component());
            
            lore.add(new I18n.Builder("bonus.display.source", player)
                .withPlaceholder("source", bonus.getSource().getDisplayName())
                .build().component());
            
            String statusKey = bonus.isActive() ? "bonus.status.active" : "bonus.status.inactive";
            lore.add(new I18n.Builder(statusKey, player).build().component());
            
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Creates an island statistics item
     * 
     * @param island the island to display stats for
     * @param player the player for translation context
     * @return island stats ItemStack
     */
    @NotNull
    public static ItemStack createIslandStatsItem(@NotNull OneblockIsland island, @NotNull Player player) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(new I18n.Builder("island.stats.title", player)
                .withPlaceholder("island", island.getIslandName())
                .build().component());
            
            List<Component> lore = new ArrayList<>();
            
            lore.add(new I18n.Builder("island.stats.level", player)
                .withPlaceholder("level", String.valueOf(island.getLevel()))
                .build().component());
            
            lore.add(new I18n.Builder("island.stats.evolution", player)
                .withPlaceholder("evolution", island.getCurrentEvolution())
                .build().component());
            
            if (island.getOneblock() != null) {
                lore.add(new I18n.Builder("island.stats.evolution_level", player)
                    .withPlaceholder("level", String.valueOf(island.getOneblock().getEvolutionLevel()))
                    .build().component());
                
                lore.add(new I18n.Builder("island.stats.experience", player)
                    .withPlaceholder("experience", String.format("%.1f", island.getOneblock().getEvolutionExperience()))
                    .build().component());
                
                lore.add(new I18n.Builder("island.stats.blocks_broken", player)
                    .withPlaceholder("blocks", String.valueOf(island.getOneblock().getTotalBlocksBroken()))
                    .build().component());
            }
            
            lore.add(new I18n.Builder("island.stats.coins", player)
                .withPlaceholder("coins", String.valueOf(island.getIslandCoins()))
                .build().component());
            
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Creates an evolution progress overview item
     * 
     * @param evolution the evolution to show progress for
     * @param player the player for translation context
     * @return progress overview ItemStack
     */
    @NotNull
    public static ItemStack createEvolutionProgressItem(
            @NotNull OneblockEvolution evolution,
            @NotNull Player player
    ) {
        List<AbstractRequirement> requirements = evolution.getRequirements();
        boolean allMet = evolution.areRequirementsMet(player);
        double overallProgress = evolution.calculateRequirementProgress(player);
        
        Material material = allMet ? Material.LIME_DYE : Material.YELLOW_DYE;
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(new I18n.Builder("progress.overview.title", player)
                .withPlaceholder("evolution", evolution.getEvolutionName())
                .build().component());
            
            List<Component> lore = new ArrayList<>();
            
            lore.add(new I18n.Builder("progress.overview.overall", player)
                .withPlaceholder("progress", String.format("%.1f", overallProgress * 100))
                .build().component());
            
            if (requirements != null && !requirements.isEmpty()) {
                long completedRequirements = requirements.stream()
                    .filter(req -> req.isMet(player))
                    .count();
                
                lore.add(new I18n.Builder("progress.overview.requirements", player)
                    .withPlaceholder("completed", String.valueOf(completedRequirements))
                    .withPlaceholder("total", String.valueOf(requirements.size()))
                    .build().component());
            }
            
            String statusKey = allMet ? "progress.status.ready" : "progress.status.in_progress";
            lore.add(new I18n.Builder(statusKey, player).build().component());
            
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Creates a navigation breadcrumb item
     * 
     * @param titleKey the translation key for the breadcrumb title
     * @param player the player for translation context
     * @return breadcrumb ItemStack
     */
    @NotNull
    public static ItemStack createBreadcrumbItem(@NotNull String titleKey, @NotNull Player player) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(new I18n.Builder(titleKey, player).build().component());
            meta.lore(List.of(new I18n.Builder("ui.breadcrumb.description", player).build().component()));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Creates a loading placeholder item
     * 
     * @param player the player for translation context
     * @return loading ItemStack
     */
    @NotNull
    public static ItemStack createLoadingItem(@NotNull Player player) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(new I18n.Builder("ui.loading.title", player).build().component());
            meta.lore(List.of(new I18n.Builder("ui.loading.description", player).build().component()));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Creates an error display item
     * 
     * @param errorKey the translation key for the error message
     * @param player the player for translation context
     * @return error ItemStack
     */
    @NotNull
    public static ItemStack createErrorItem(@NotNull String errorKey, @NotNull Player player) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(new I18n.Builder("ui.error.title", player).build().component());
            meta.lore(List.of(new I18n.Builder(errorKey, player).build().component()));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Gets the appropriate material for a requirement type
     */
    @NotNull
    private static Material getRequirementMaterial(@NotNull String typeId) {
        return switch (typeId.toUpperCase()) {
            case "ITEM" -> Material.CHEST;
            case "CURRENCY" -> Material.GOLD_INGOT;
            case "EXPERIENCE_LEVEL" -> Material.EXPERIENCE_BOTTLE;
            case "PERMISSION" -> Material.NAME_TAG;
            case "PLAYTIME" -> Material.CLOCK;
            case "ACHIEVEMENT" -> Material.DIAMOND;
            case "PREVIOUS_LEVEL" -> Material.LADDER;
            case "COMPOSITE" -> Material.BUNDLE;
            case "CHOICE" -> Material.COMPARATOR;
            case "TIME_BASED" -> Material.DAYLIGHT_DETECTOR;
            case "LOCATION" -> Material.COMPASS;
            case "JOBS" -> Material.IRON_PICKAXE;
            case "SKILLS" -> Material.ENCHANTED_BOOK;
            case "EVOLUTION_LEVEL" -> Material.NETHER_STAR;
            case "BLOCKS_BROKEN" -> Material.DIAMOND_PICKAXE;
            default -> Material.PAPER;
        };
    }

    /**
     * Gets the appropriate material for a bonus type
     */
    @NotNull
    private static Material getBonusMaterial(@NotNull Bonus.Type bonusType) {
        return switch (bonusType) {
            case BLOCK_BREAK_SPEED -> Material.DIAMOND_PICKAXE;
            case EXPERIENCE_MULTIPLIER -> Material.EXPERIENCE_BOTTLE;
            case RARE_DROPS -> Material.DIAMOND;
            case ENERGY -> Material.REDSTONE;
            case AUTOMATION_EFFICIENCY -> Material.PISTON;
            case STORAGE_CAPACITY -> Material.CHEST;
            case GENERATOR_SPEED -> Material.FURNACE;
            case EVOLUTION_PROGRESS -> Material.NETHER_STAR;
            case ALL_STATS -> Material.BEACON;
            case ULTIMATE -> Material.DRAGON_EGG;
            case LUCK -> Material.RABBIT_FOOT;
            case PROBABILITY -> Material.PAPER;
            case EFFICIENCY -> Material.CLOCK;
            default -> Material.ENCHANTED_BOOK;
        };
    }
    
    /**
     * Status levels for system indicators
     */
    public enum StatusLevel {
        EXCELLENT("§a", "Excellent"),
        GOOD("§e", "Good"),
        WARNING("§6", "Warning"),
        CRITICAL("§c", "Critical");
        
        private final String colorCode;
        private final String displayName;
        
        StatusLevel(String colorCode, String displayName) {
            this.colorCode = colorCode;
            this.displayName = displayName;
        }
        
        public String getColorCode() { return colorCode; }
        public String getDisplayName() { return displayName; }
    }
    
    /**
     * Creates a status indicator with color-coded status
     */
    @NotNull
    public static ItemStack createStatusIndicator(@NotNull Material material, @NotNull Component name, 
                                                @NotNull StatusLevel status, @NotNull List<Component> lore) {
        List<Component> statusLore = new ArrayList<>();
        statusLore.add(Component.text(status.getColorCode() + "Status: " + status.getDisplayName()));
        statusLore.add(Component.text(""));
        statusLore.addAll(lore);
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(name);
            meta.lore(statusLore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Creates a mode button with selection indicator
     */
    @NotNull
    public static ItemStack createModeButton(@NotNull Material material, @NotNull Component name, 
                                           @NotNull List<Component> lore, boolean selected) {
        List<Component> modeLore = new ArrayList<>();
        
        if (selected) {
            modeLore.add(Component.text("§a§l» SELECTED «"));
            modeLore.add(Component.text(""));
        }
        
        modeLore.addAll(lore);
        
        if (!selected) {
            modeLore.add(Component.text(""));
            modeLore.add(Component.text("§7Click to select"));
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(selected ? Component.text("§a§l").append(name) : name);
            meta.lore(modeLore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Creates a statistic display item
     */
    @NotNull
    public static ItemStack createStatItem(@NotNull Material material, @NotNull Component name, 
                                         @NotNull String value, @NotNull List<Component> description) {
        List<Component> statLore = new ArrayList<>();
        statLore.add(Component.text("§f§lValue: §b§l" + value));
        statLore.add(Component.text(""));
        statLore.addAll(description);
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(name);
            meta.lore(statLore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Creates an action button for quick actions
     */
    @NotNull
    public static ItemStack createActionButton(@NotNull Material material, @NotNull Component name, 
                                             @NotNull List<Component> description) {
        List<Component> actionLore = new ArrayList<>(description);
        actionLore.add(Component.text(""));
        actionLore.add(Component.text("§e§lClick to execute"));
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(name);
            meta.lore(actionLore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Creates a navigation button with consistent styling
     */
    @NotNull
    public static ItemStack createNavigationButton(@NotNull Material material, @NotNull Component name, @NotNull List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(name);
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Creates an info item with consistent styling
     */
    @NotNull
    public static ItemStack createInfoItem(@NotNull Material material, @NotNull Component name, @NotNull List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(name);
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        
        return item;
    }
}
