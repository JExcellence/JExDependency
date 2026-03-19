package de.jexcellence.oneblock.view.generator;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.database.entity.generator.EGeneratorDesignType;
import de.jexcellence.oneblock.database.entity.generator.GeneratorDesign;
import de.jexcellence.oneblock.service.GeneratorDesignService;
import de.jexcellence.oneblock.service.GeneratorRequirementService;
import de.jexcellence.oneblock.service.GeneratorStructureManager;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Browser view for all generator designs.
 * <p>
 * Shows a grid layout of all 10 generator types with locked/unlocked status,
 * tier progression, and click handlers for design details.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class GeneratorBrowserView extends BaseView {

    private final State<JExOneblock> plugin = initialState("plugin");
    private final State<GeneratorStructureManager> structureManager = initialState("structureManager");

    // Grid layout slots for 10 generator designs (2 rows of 5)
    private static final int[] DESIGN_SLOTS = {10, 11, 12, 13, 14, 19, 20, 21, 22, 23};
    
    // Info and navigation slots
    private static final int INFO_SLOT = 4;
    private static final int HELP_SLOT = 49;
    private static final int REFRESH_SLOT = 53;

    public GeneratorBrowserView() {
        super();
    }

    @Override
    protected String getKey() {
        return "generator_browser_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "XXXXXXXXX",
            "XOOOOO00X",
            "bXX<p>XXX",
        };
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(@NotNull OpenContext openContext) {
        return Map.of();
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        GeneratorStructureManager manager = structureManager.get(render);
        if (manager == null || !manager.isInitialized()) {
            renderErrorState(render, player);
            return;
        }

        renderInfoPanel(render, player, manager);
        renderDesignGrid(render, player, manager);
        renderNavigationButtons(render, player);
        fillBorder(render, player);
    }

    /**
     * Renders the info panel at the top.
     */
    private void renderInfoPanel(@NotNull RenderContext render, @NotNull Player player, 
                                  @NotNull GeneratorStructureManager manager) {
        GeneratorDesignService designService = manager.getDesignService();
        GeneratorRequirementService requirementService = manager.getRequirementService();
        
        int totalDesigns = designService.getEnabledDesignCount();
        int availableDesigns = designService.getAvailableDesigns(player).size();
        int unlockedDesigns = countUnlockedDesigns(player, manager);
        
        render.slot(INFO_SLOT)
            .renderWith(() -> UnifiedBuilderFactory
                .item(Material.NETHER_STAR)
                .setName(i18n("info.title", player).build().component())
                .setLore(i18n("info.lore", player)
                    .withPlaceholders(Map.of(
                        "unlocked", String.valueOf(unlockedDesigns),
                        "available", String.valueOf(availableDesigns),
                        "total", String.valueOf(totalDesigns)
                    ))
                    .build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build());
    }

    /**
     * Renders the grid of generator designs.
     */
    private void renderDesignGrid(@NotNull RenderContext render, @NotNull Player player,
                                   @NotNull GeneratorStructureManager manager) {
        GeneratorDesignService designService = manager.getDesignService();
        List<GeneratorDesign> designs = designService.getEnabledDesigns();
        
        for (int i = 0; i < DESIGN_SLOTS.length && i < designs.size(); i++) {
            int slot = DESIGN_SLOTS[i];
            GeneratorDesign design = designs.get(i);
            
            render.slot(slot)
                .renderWith(() -> createDesignItem(player, design, manager))
                .onClick(click -> handleDesignClick(click, design, manager));
        }
        
        // Fill remaining slots if fewer than 10 designs
        for (int i = designs.size(); i < DESIGN_SLOTS.length; i++) {
            int slot = DESIGN_SLOTS[i];
            render.slot(slot)
                .renderWith(() -> createEmptyDesignSlot(player));
        }
    }

    /**
     * Creates an item representing a generator design.
     */
    @NotNull
    private ItemStack createDesignItem(@NotNull Player player, @NotNull GeneratorDesign design,
                                        @NotNull GeneratorStructureManager manager) {
        GeneratorDesignService designService = manager.getDesignService();
        GeneratorRequirementService requirementService = manager.getRequirementService();
        
        boolean canUnlock = designService.canUnlock(player, design);
        boolean hasUnlocked = hasUnlockedDesign(player, design, manager);
        double progress = requirementService.calculateProgress(player, design);
        
        Material iconMaterial = getDesignIcon(design, canUnlock, hasUnlocked);
        
        List<Component> lore = new ArrayList<>();
        
        // Tier and difficulty
        lore.addAll(i18n("design.tier_info", player)
            .withPlaceholders(Map.of(
                "tier", String.valueOf(design.getTier()),
                "difficulty", design.getDifficulty() != null ? design.getDifficulty() : "Normal"
            ))
            .build().children());
        
        lore.add(Component.empty());
        
        // Description
        if (design.getDescriptionKey() != null) {
            lore.addAll(i18n("design.description", player)
                .withPlaceholder("description", design.getDescriptionKey())
                .build().children());
            lore.add(Component.empty());
        }
        
        // Status and progress
        if (hasUnlocked) {
            lore.addAll(i18n("design.status.unlocked", player).build().children());
        } else if (canUnlock) {
            lore.addAll(i18n("design.status.available", player).build().children());
        } else {
            lore.addAll(i18n("design.status.locked", player)
                .withPlaceholder("progress", String.format("%.0f%%", progress * 100))
                .build().children());
        }
        
        lore.add(Component.empty());
        
        // Bonuses
        addBonusLore(lore, player, design);
        
        // Action hint
        lore.add(Component.empty());
        if (hasUnlocked) {
            lore.addAll(i18n("design.action.view", player).build().children());
        } else if (canUnlock) {
            lore.addAll(i18n("design.action.unlock", player).build().children());
        } else {
            lore.addAll(i18n("design.action.requirements", player).build().children());
        }
        
        return UnifiedBuilderFactory
            .item(iconMaterial)
            .setName(i18n("design.name", player)
                .withPlaceholder("name", design.getNameKey())
                .withPlaceholder("status", getStatusIndicator(canUnlock, hasUnlocked))
                .build().component())
            .setLore(lore)
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    /**
     * Gets the icon material for a design based on unlock status.
     */
    @NotNull
    private Material getDesignIcon(@NotNull GeneratorDesign design, boolean canUnlock, boolean hasUnlocked) {
        if (!canUnlock && !hasUnlocked) {
            return Material.BARRIER;
        }
        
        String iconMaterial = design.getIconMaterial();
        if (iconMaterial != null) {
            try {
                return Material.valueOf(iconMaterial.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        
        // Default icons by type
        return getDefaultIconForType(design.getDesignType());
    }

    /**
     * Gets the default icon for a design type.
     */
    @NotNull
    private Material getDefaultIconForType(@NotNull EGeneratorDesignType type) {
        return switch (type) {
            case FOUNDRY -> Material.FURNACE;
            case AQUATIC -> Material.PRISMARINE;
            case VOLCANIC -> Material.MAGMA_BLOCK;
            case CRYSTAL -> Material.AMETHYST_BLOCK;
            case MECHANICAL -> Material.PISTON;
            case NATURE -> Material.MOSS_BLOCK;
            case NETHER -> Material.BLACKSTONE;
            case END -> Material.END_STONE;
            case ANCIENT -> Material.DEEPSLATE;
            case CELESTIAL -> Material.BEACON;
        };
    }

    /**
     * Gets the status indicator string.
     */
    @NotNull
    private String getStatusIndicator(boolean canUnlock, boolean hasUnlocked) {
        if (hasUnlocked) return "<green>✓</green>";
        if (canUnlock) return "<yellow>◆</yellow>";
        return "<red>✗</red>";
    }

    /**
     * Adds bonus information to the lore.
     */
    private void addBonusLore(@NotNull List<Component> lore, @NotNull Player player, 
                              @NotNull GeneratorDesign design) {
        boolean hasBonus = false;
        
        if (design.getSpeedMultiplier() != null && design.getSpeedMultiplier() > 1.0) {
            lore.addAll(i18n("design.bonus.speed", player)
                .withPlaceholder("value", String.format("%.1fx", design.getSpeedMultiplier()))
                .build().children());
            hasBonus = true;
        }
        
        if (design.getXpMultiplier() != null && design.getXpMultiplier() > 1.0) {
            lore.addAll(i18n("design.bonus.xp", player)
                .withPlaceholder("value", String.format("%.1fx", design.getXpMultiplier()))
                .build().children());
            hasBonus = true;
        }
        
        if (design.getFortuneBonus() != null && design.getFortuneBonus() > 0.0) {
            lore.addAll(i18n("design.bonus.fortune", player)
                .withPlaceholder("value", String.format("%.1fx", design.getFortuneBonus()))
                .build().children());
            hasBonus = true;
        }
        
        if (!hasBonus) {
            lore.addAll(i18n("design.bonus.none", player).build().children());
        }
    }

    /**
     * Creates an empty design slot item.
     */
    @NotNull
    private ItemStack createEmptyDesignSlot(@NotNull Player player) {
        return UnifiedBuilderFactory
            .item(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
            .setName(i18n("design.empty", player).build().component())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    /**
     * Handles click on a design item.
     */
    private void handleDesignClick(@NotNull SlotClickContext click, @NotNull GeneratorDesign design,
                                    @NotNull GeneratorStructureManager manager) {
        Player player = click.getPlayer();
        
        // Open detail view
        click.openForPlayer(GeneratorDesignDetailView.class, Map.of(
            "plugin", plugin.get(click),
            "structureManager", manager,
            "design", design
        ));
    }

    /**
     * Renders navigation buttons.
     */
    public void renderNavigationButtons(@NotNull RenderContext render, @NotNull Player player) {
        // Help button
        render.slot(HELP_SLOT)
            .renderWith(() -> UnifiedBuilderFactory
                .item(Material.BOOK)
                .setName(i18n("help.title", player).build().component())
                .setLore(i18n("help.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build())
            .onClick(click -> showHelp(click.getPlayer()));
        
        // Refresh button
        render.slot(REFRESH_SLOT)
            .renderWith(() -> UnifiedBuilderFactory
                .item(Material.CLOCK)
                .setName(i18n("refresh.title", player).build().component())
                .setLore(i18n("refresh.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build())
            .onClick(click -> {
                // Refresh the view
                click.getPlayer().closeInventory();
                click.openForPlayer(GeneratorBrowserView.class, click.getInitialData());
            });
    }

    /**
     * Fills border slots with glass panes.
     */
    private void fillBorder(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('X', UnifiedBuilderFactory
            .item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(Component.empty())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build());
    }

    /**
     * Shows help information to the player.
     */
    private void showHelp(@NotNull Player player) {
        i18n("help.message", player).includePrefix().build().sendMessage();
    }

    /**
     * Checks if player has unlocked a design.
     */
    private boolean hasUnlockedDesign(@NotNull Player player, @NotNull GeneratorDesign design,
                                       @NotNull GeneratorStructureManager manager) {
        // TODO: Implement proper unlock checking
        // For now, assume all designs are available
        return true;
    }

    /**
     * Counts total unlocked designs for a player.
     */
    private int countUnlockedDesigns(@NotNull Player player, @NotNull GeneratorStructureManager manager) {
        // TODO: Implement proper unlock counting
        // For now, return total design count
        return manager.getDesignService().getAllDesigns().size();
    }

    /**
     * Renders error state when manager is not available.
     */
    private void renderErrorState(@NotNull RenderContext render, @NotNull Player player) {
        render.slot(22)
            .renderWith(() -> UnifiedBuilderFactory
                .item(Material.BARRIER)
                .setName(i18n("error.title", player).build().component())
                .setLore(i18n("error.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build());
        
        fillBorder(render, player);
    }
}
