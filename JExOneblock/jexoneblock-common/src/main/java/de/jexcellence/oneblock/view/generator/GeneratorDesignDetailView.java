package de.jexcellence.oneblock.view.generator;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.database.entity.generator.GeneratorDesign;
import de.jexcellence.oneblock.database.entity.generator.GeneratorDesignLayer;
import de.jexcellence.oneblock.service.GeneratorRequirementService;
import de.jexcellence.oneblock.service.GeneratorStructureManager;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Detailed view of a specific generator design.
 * <p>
 * Shows 3D rotating preview, layer breakdown, material requirements,
 * unlock requirements, and build options.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class GeneratorDesignDetailView extends BaseView {

    private final State<JExOneblock> plugin = initialState("plugin");
    private final State<GeneratorStructureManager> structureManager = initialState("structureManager");
    private final State<GeneratorDesign> design = initialState("design");
    
    private final MutableState<Integer> selectedLayer = mutableState(0);

    private static final int DESIGN_INFO_SLOT = 4;
    private static final int PREVIEW_GRID_SLOT = 13; // Changed from PREVIEW_3D_SLOT
    private static final int LAYERS_SLOT = 20;
    private static final int MATERIALS_SLOT = 22;
    private static final int REQUIREMENTS_SLOT = 24;
    private static final int BUILD_BUTTON_SLOT = 40;
    private static final int BACK_BUTTON_SLOT = 45;
    
    // Layer preview slots (3x3 grid)
    private static final int[] LAYER_PREVIEW_SLOTS = {28, 29, 30, 37, 38, 39, 46, 47, 48};

    public GeneratorDesignDetailView() {
        super(GeneratorBrowserView.class);
    }

    @Override
    protected String getKey() {
        return "generator_design_detail_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "XXXXXXXXX",
            "XIXPXMXRX",
            "XXXXXXXXX",
            "XLLLXMMMX",
            "XLLLXMMMX",
            "XBXXAXXXX"
        };
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(@NotNull OpenContext openContext) {
        try {
            GeneratorDesign designObj = design.get(openContext);
            if (designObj != null) {
                return Map.of("design_name", designObj.getNameKey());
            }
        } catch (Exception ignored) {}
        return Map.of("design_name", "Unknown");
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        GeneratorDesign designObj = design.get(render);
        GeneratorStructureManager manager = structureManager.get(render);
        
        if (designObj == null || manager == null) {
            renderErrorState(render, player);
            return;
        }

        renderDesignInfo(render, player, designObj, manager);
        render3DPreviewButton(render, player, designObj, manager);
        renderLayersButton(render, player, designObj);
        renderMaterialsButton(render, player, designObj);
        renderRequirementsButton(render, player, designObj, manager);
        renderLayerPreview(render, player, designObj);
        renderBuildButton(render, player, designObj, manager);
        fillBorder(render, player);
    }

    /**
     * Renders the main design info panel.
     */
    private void renderDesignInfo(@NotNull RenderContext render, @NotNull Player player,
                                   @NotNull GeneratorDesign design, @NotNull GeneratorStructureManager manager) {
        render.layoutSlot('I')
            .renderWith(() -> {
                List<Component> lore = new ArrayList<>();
                
                // Type and tier
                lore.addAll(i18n("info.type", player)
                    .withPlaceholder("type", design.getDesignType().name())
                    .build().children());
                lore.addAll(i18n("info.tier", player)
                    .withPlaceholder("tier", String.valueOf(design.getTier()))
                    .build().children());
                
                lore.add(Component.empty());
                
                // Description
                if (design.getDescriptionKey() != null) {
                    lore.addAll(i18n("info.description", player)
                        .withPlaceholder("description", design.getDescriptionKey())
                        .build().children());
                    lore.add(Component.empty());
                }
                
                // Dimensions
                lore.addAll(i18n("info.dimensions", player)
                    .withPlaceholders(Map.of(
                        "width", String.valueOf(design.getWidth()),
                        "height", String.valueOf(design.getHeight()),
                        "depth", String.valueOf(design.getDepth())
                    ))
                    .build().children());
                
                // Layer count
                lore.addAll(i18n("info.layers", player)
                    .withPlaceholder("count", String.valueOf(design.getLayers().size()))
                    .build().children());
                
                lore.add(Component.empty());
                
                // Bonuses
                addBonusInfo(lore, player, design);
                
                return UnifiedBuilderFactory
                    .item(getDesignIconMaterial(design))
                    .setName(i18n("info.title", player)
                        .withPlaceholder("name", design.getNameKey())
                        .build().component())
                    .setLore(lore)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
            });
    }

    /**
     * Renders the grid preview button.
     */
    private void render3DPreviewButton(@NotNull RenderContext render, @NotNull Player player,
                                        @NotNull GeneratorDesign design, @NotNull GeneratorStructureManager manager) {
        render.layoutSlot('P')
            .renderWith(() -> UnifiedBuilderFactory
                .item(Material.COMPASS)
                .setName(i18n("preview_grid.title", player).build().component())
                .setLore(i18n("preview_grid.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build())
            .onClick(click -> {
                // Open grid-based visualization
                var params = new java.util.HashMap<String, Object>();
                params.put("plugin", plugin.get(click));
                params.put("structureManager", manager);
                params.put("design", design);
                params.put("playerStructure", null); // No player structure for preview
                click.openForPlayer(GeneratorVisualization3DView.class, params);
            });
    }

    /**
     * Renders the layers overview button.
     */
    private void renderLayersButton(@NotNull RenderContext render, @NotNull Player player,
                                     @NotNull GeneratorDesign design) {
        render.layoutSlot('L')
            .renderWith(() -> {
                List<Component> lore = new ArrayList<>();
                lore.addAll(i18n("layers.description", player).build().children());
                lore.add(Component.empty());
                
                // Show first few layers
                List<GeneratorDesignLayer> layers = design.getLayers();
                for (int i = 0; i < Math.min(5, layers.size()); i++) {
                    GeneratorDesignLayer layer = layers.get(i);
                    lore.addAll(i18n("layers.entry", player)
                        .withPlaceholders(Map.of(
                            "index", String.valueOf(i + 1),
                            "width", String.valueOf(layer.getWidth()),
                            "depth", String.valueOf(layer.getDepth())
                        ))
                        .build().children());
                }
                
                if (layers.size() > 5) {
                    lore.addAll(i18n("layers.more", player)
                        .withPlaceholder("count", String.valueOf(layers.size() - 5))
                        .build().children());
                }
                
                lore.add(Component.empty());
                lore.addAll(i18n("layers.click_hint", player).build().children());
                
                return UnifiedBuilderFactory
                    .item(Material.BOOKSHELF)
                    .setName(i18n("layers.title", player)
                        .withPlaceholder("count", String.valueOf(layers.size()))
                        .build().component())
                    .setLore(lore)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
            })
            .onClick(click -> {
                // Open grid-based layer visualization
                var params = new java.util.HashMap<String, Object>();
                params.put("plugin", plugin.get(click));
                params.put("structureManager", structureManager.get(click));
                params.put("design", design);
                params.put("playerStructure", null); // Preview mode
                click.openForPlayer(GeneratorVisualization3DView.class, params);
            });
    }

    /**
     * Renders the materials overview button.
     */
    private void renderMaterialsButton(@NotNull RenderContext render, @NotNull Player player,
                                        @NotNull GeneratorDesign design) {
        render.layoutSlot('M')
            .renderWith(() -> {
                Map<Material, Integer> totalMaterials = calculateTotalMaterials(design);
                
                List<Component> lore = new ArrayList<>();
                lore.addAll(i18n("materials.description", player).build().children());
                lore.add(Component.empty());
                
                // Show top materials
                int count = 0;
                for (Map.Entry<Material, Integer> entry : totalMaterials.entrySet()) {
                    if (count >= 5) {
                        lore.addAll(i18n("materials.more", player)
                            .withPlaceholder("count", String.valueOf(totalMaterials.size() - 5))
                            .build().children());
                        break;
                    }
                    lore.addAll(i18n("materials.entry", player)
                        .withPlaceholders(Map.of(
                            "material", formatMaterialName(entry.getKey()),
                            "amount", String.valueOf(entry.getValue())
                        ))
                        .build().children());
                    count++;
                }
                
                lore.add(Component.empty());
                lore.addAll(i18n("materials.click_hint", player).build().children());
                
                return UnifiedBuilderFactory
                    .item(Material.CHEST)
                    .setName(i18n("materials.title", player)
                        .withPlaceholder("count", String.valueOf(totalMaterials.size()))
                        .build().component())
                    .setLore(lore)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
            })
            .onClick(click -> {
                // Materials view not yet implemented in new system
                player.sendMessage(Component.text("Materials view coming soon!", NamedTextColor.YELLOW));
                click.closeForPlayer();
            });
    }

    /**
     * Renders the requirements button.
     */
    private void renderRequirementsButton(@NotNull RenderContext render, @NotNull Player player,
                                           @NotNull GeneratorDesign design, @NotNull GeneratorStructureManager manager) {
        render.layoutSlot('R')
            .renderWith(() -> {
                GeneratorRequirementService reqService = manager.getRequirementService();
                double progress = reqService.calculateProgress(player, design);
                boolean canUnlock = manager.getDesignService().canUnlock(player, design);
                
                List<Component> lore = new ArrayList<>();
                
                // Progress bar
                lore.addAll(i18n("requirements.progress", player)
                    .withPlaceholder("progress", String.format("%.0f%%", progress * 100))
                    .build().children());
                lore.add(createProgressBar(progress));
                lore.add(Component.empty());
                
                // Detailed requirements
                var detailedProgress = reqService.getDetailedProgress(player, design);
                for (var reqProgress : detailedProgress) {
                    String status = reqProgress.met() ? "<green>✓</green>" : "<red>✗</red>";
                    lore.add(Component.text(status + " " + reqProgress.requirement().getRequirement().getClass().getSimpleName()));
                }
                
                lore.add(Component.empty());
                
                if (canUnlock) {
                    lore.addAll(i18n("requirements.ready", player).build().children());
                } else {
                    lore.addAll(i18n("requirements.not_ready", player).build().children());
                }
                
                Material icon = canUnlock ? Material.LIME_DYE : Material.GRAY_DYE;
                
                return UnifiedBuilderFactory
                    .item(icon)
                    .setName(i18n("requirements.title", player).build().component())
                    .setLore(lore)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
            });
    }

    /**
     * Renders the layer preview grid.
     */
    private void renderLayerPreview(@NotNull RenderContext render, @NotNull Player player,
                                     @NotNull GeneratorDesign design) {
        List<GeneratorDesignLayer> layers = design.getLayers();
        if (layers.isEmpty()) return;
        
        int layerIndex = selectedLayer.get(render);
        if (layerIndex >= layers.size()) layerIndex = 0;
        
        GeneratorDesignLayer layer = layers.get(layerIndex);
        Material[][] pattern = layer.getPattern();
        
        if (pattern == null) return;
        
        int width = layer.getWidth();
        int depth = layer.getDepth();
        
        for (int i = 0; i < LAYER_PREVIEW_SLOTS.length; i++) {
            int slot = LAYER_PREVIEW_SLOTS[i];
            int row = i / 3;
            int col = i % 3;
            
            // Scale to pattern
            int patternX = (int) ((double) col / 2 * Math.max(0, width - 1));
            int patternZ = (int) ((double) row / 2 * Math.max(0, depth - 1));
            
            Material material = Material.AIR;
            if (patternZ < depth && patternX < width && pattern[patternX] != null) {
                material = pattern[patternX][patternZ];
            }
            
            final Material finalMaterial = material;
            final int finalX = patternX;
            final int finalZ = patternZ;
            
            render.slot(slot)
                .renderWith(() -> createPatternItem(finalMaterial, finalX, finalZ, player))
                .updateOnStateChange(selectedLayer);
        }
    }

    /**
     * Creates a pattern preview item.
     */
    @NotNull
    private ItemStack createPatternItem(@NotNull Material material, int x, int z, @NotNull Player player) {
        if (material == Material.AIR || material == null) {
            return UnifiedBuilderFactory
                .item(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .setName(i18n("pattern.empty", player).build().component())
                .setLore(i18n("pattern.position", player)
                    .withPlaceholders(Map.of("x", String.valueOf(x), "z", String.valueOf(z)))
                    .build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
        }
        
        Material displayMaterial = material;
        if (material == Material.WATER) displayMaterial = Material.WATER_BUCKET;
        if (material == Material.LAVA) displayMaterial = Material.LAVA_BUCKET;
        
        return UnifiedBuilderFactory
            .item(displayMaterial)
            .setName(i18n("pattern.material", player)
                .withPlaceholder("material", formatMaterialName(material))
                .build().component())
            .setLore(i18n("pattern.position", player)
                .withPlaceholders(Map.of("x", String.valueOf(x), "z", String.valueOf(z)))
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    /**
     * Renders the build button.
     */
    private void renderBuildButton(@NotNull RenderContext render, @NotNull Player player,
                                    @NotNull GeneratorDesign design, @NotNull GeneratorStructureManager manager) {
        render.layoutSlot('B')
            .renderWith(() -> {
                boolean canUnlock = manager.getDesignService().canUnlock(player, design);
                
                Material icon = canUnlock ? Material.GOLDEN_PICKAXE : Material.IRON_PICKAXE;
                String nameKey = canUnlock ? "build.ready" : "build.not_ready";
                
                List<Component> lore = new ArrayList<>();
                if (canUnlock) {
                    lore.addAll(i18n("build.ready_lore", player).build().children());
                } else {
                    lore.addAll(i18n("build.not_ready_lore", player).build().children());
                }
                
                return UnifiedBuilderFactory
                    .item(icon)
                    .setName(i18n(nameKey, player).build().component())
                    .setLore(lore)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                    .build();
            })
            .onClick(click -> {
                if (manager.getDesignService().canUnlock(player, design)) {
                    // Start build process
                    player.closeInventory();
                    i18n("build.starting", player)
                        .withPlaceholder("design", design.getNameKey())
                        .includePrefix()
                        .build().sendMessage();
                    
                    // Open build progress view or start building
                    manager.buildStructure(player, design, player.getLocation())
                        .thenAccept(result -> {
                            if (result.success()) {
                                i18n("build.success", player).includePrefix().build().sendMessage();
                            } else {
                                i18n("build.failed", player)
                                    .withPlaceholder("reason", result.message())
                                    .includePrefix()
                                    .build().sendMessage();
                            }
                        });
                } else {
                    i18n("build.requirements_not_met", player).includePrefix().build().sendMessage();
                }
            });
    }

    /**
     * Fills border slots.
     */
    private void fillBorder(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('X', UnifiedBuilderFactory
            .item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(Component.empty())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build());
    }

    /**
     * Adds bonus information to lore.
     */
    private void addBonusInfo(@NotNull List<Component> lore, @NotNull Player player, 
                              @NotNull GeneratorDesign design) {
        lore.addAll(i18n("info.bonuses_header", player).build().children());
        
        if (design.getSpeedMultiplier() != null && design.getSpeedMultiplier() > 1.0) {
            lore.addAll(i18n("info.bonus.speed", player)
                .withPlaceholder("value", String.format("%.1fx", design.getSpeedMultiplier()))
                .build().children());
        }
        if (design.getXpMultiplier() != null && design.getXpMultiplier() > 1.0) {
            lore.addAll(i18n("info.bonus.xp", player)
                .withPlaceholder("value", String.format("%.1fx", design.getXpMultiplier()))
                .build().children());
        }
        if (design.getFortuneBonus() != null && design.getFortuneBonus() > 0.0) {
            lore.addAll(i18n("info.bonus.fortune", player)
                .withPlaceholder("value", String.format("%.1fx", design.getFortuneBonus()))
                .build().children());
        }
    }

    /**
     * Gets the icon material for a design.
     */
    @NotNull
    private Material getDesignIconMaterial(@NotNull GeneratorDesign design) {
        if (design.getIconMaterial() != null) {
            try {
                return Material.valueOf(design.getIconMaterial().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        return Material.COBBLESTONE;
    }

    /**
     * Calculates total materials needed for a design.
     */
    @NotNull
    private Map<Material, Integer> calculateTotalMaterials(@NotNull GeneratorDesign design) {
        Map<Material, Integer> totals = new java.util.HashMap<>();
        
        for (GeneratorDesignLayer layer : design.getLayers()) {
            Material[][] pattern = layer.getPattern();
            if (pattern == null) continue;
            
            for (Material[] row : pattern) {
                if (row == null) continue;
                for (Material material : row) {
                    if (material != null && material != Material.AIR) {
                        totals.merge(material, 1, Integer::sum);
                    }
                }
            }
        }
        
        return totals;
    }

    /**
     * Creates a progress bar component.
     */
    @NotNull
    private Component createProgressBar(double progress) {
        int filled = (int) (progress * 20);
        int empty = 20 - filled;

        String bar = "<green>" + "█</green>".repeat(Math.max(0, filled)) +
                "<gray>" +
                "█</gray>".repeat(Math.max(0, empty));
        
        return Component.text(bar);
    }

    /**
     * Formats a material name for display.
     */
    @NotNull
    private String formatMaterialName(@NotNull Material material) {
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
    
    /**
     * Renders error state.
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
