package de.jexcellence.oneblock.view.generator;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.database.entity.generator.GeneratorDesign;
import de.jexcellence.oneblock.database.entity.generator.GeneratorDesignLayer;
import de.jexcellence.oneblock.database.entity.generator.PlayerGeneratorStructure;
import de.jexcellence.oneblock.service.GeneratorStructureManager;
import de.jexcellence.oneblock.view.generator.grid.GeneratorGridPosition;
import de.jexcellence.oneblock.view.generator.grid.GeneratorGridSlotMapper;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced generator structure visualization view with dynamic grid navigation.
 * <p>
 * This view provides layer-by-layer visualization of generator structures with
 * navigation controls similar to RDQ's rank system. It can handle structures
 * of any size by implementing a viewport-based navigation system.
 * </p>
 *
 * @author JExcellence
 * @version 3.0.0
 * @since 2.0.0
 */
public class GeneratorVisualization3DView extends BaseView {

    private final State<JExOneblock> plugin = initialState("plugin");
    private final State<GeneratorStructureManager> structureManager = initialState("structureManager");
    private final State<GeneratorDesign> design = initialState("design");
    private final State<PlayerGeneratorStructure> playerStructure = initialState("playerStructure");
    
    // Grid navigation state
    private final MutableState<Integer> viewOffsetX = mutableState(0);
    private final MutableState<Integer> viewOffsetZ = mutableState(0);
    private final MutableState<Integer> currentLayer = mutableState(0);
    
    // Cached structure data
    private final MutableState<Map<GeneratorGridPosition, Material>> cachedLayerBlocks = mutableState(new HashMap<>());
    private final MutableState<GeneratorGridPosition> layerDimensions = mutableState(new GeneratorGridPosition(1, 1));
    private final MutableState<Long> dataRefreshTimestamp = mutableState(System.currentTimeMillis());

    public GeneratorVisualization3DView() {
        super(GeneratorDesignDetailView.class);
    }

    @Override
    protected String getKey() {
        return "generator_visualization_grid_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "IXXXUXXX ",
            "XSSSSSSS ",
            "XSSSSSSS ",
            "XSSSSSSS ",
            "XSSSSSSS ",
            "BXXXDXXX "
        };
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(@NotNull OpenContext openContext) {
        try {
            GeneratorDesign designObj = design.get(openContext);
            if (designObj != null) {
                int layer = currentLayer.get(openContext) + 1;
                int totalLayers = designObj.getLayers() != null ? 
                    designObj.getLayers().size() : 1;
                return Map.of(
                    "design_name", designObj.getNameKey(),
                    "current_layer", layer,
                    "total_layers", totalLayers
                );
            }
        } catch (Exception ignored) {}
        return Map.of(
            "design_name", "Structure View",
            "current_layer", 1,
            "total_layers", 1
        );
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        GeneratorDesign designObj = design.get(render);
        
        if (designObj == null) {
            renderErrorState(render, player);
            return;
        }

        // Initialize cached data
        refreshLayerData(render, designObj);
        
        // Render all components
        renderNavigationControls(render, player);
        renderLayerControls(render, player, designObj);
        renderStructureGrid(render, player, designObj);
        renderUtilityControls(render, player);
    }

    /**
     * Refreshes the cached layer data for the current layer.
     */
    private void refreshLayerData(@NotNull me.devnatan.inventoryframework.context.Context render, @NotNull GeneratorDesign design) {
        int layer = currentLayer.get(render);
        Map<GeneratorGridPosition, Material> blocks = new HashMap<>();
        GeneratorGridPosition dimensions = new GeneratorGridPosition(1, 1);
        
        if (design.getLayers() != null && layer >= 0 && layer < design.getLayers().size()) {
            GeneratorDesignLayer layerData = design.getLayers().get(layer);
            Material[][] pattern = layerData.getPattern();
            
            if (pattern != null) {
                int width = layerData.getWidth();
                int depth = layerData.getDepth();
                
                for (int x = 0; x < width; x++) {
                    for (int z = 0; z < depth; z++) {
                        Material material = pattern[x][z];
                        
                        if (material != null && material != Material.AIR) {
                            blocks.put(new GeneratorGridPosition(x, z), material);
                        }
                    }
                }
                
                dimensions = new GeneratorGridPosition(width, depth);
            }
        }
        
        cachedLayerBlocks.set(blocks, render);
        layerDimensions.set(dimensions, render);
        dataRefreshTimestamp.set(System.currentTimeMillis(), render);
    }

    /**
     * Renders navigation controls (arrows for moving around large structures).
     */
    private void renderNavigationControls(@NotNull RenderContext render, @NotNull Player player) {
        GeneratorGridPosition dimensions = layerDimensions.get(render);
        int offsetX = viewOffsetX.get(render);
        int offsetZ = viewOffsetZ.get(render);
        
        // Calculate max visible area (based on available slots)
        int maxVisibleX = 9; // 9 columns available
        int maxVisibleZ = 5; // 5 rows available
        
        // Navigation Up (North, -Z)
        render.slot(GeneratorGridSlotMapper.NAVIGATION_UP_SLOT)
            .renderWith(() -> {
                boolean canMoveUp = offsetZ > 0;
                return createNavigationArrow(
                    canMoveUp ? Material.ARROW : Material.GRAY_DYE,
                    "controls.nav_up.title",
                    canMoveUp ? "controls.nav_up.lore" : "controls.nav_up.disabled",
                    player
                );
            })
            .onClick(click -> {
                if (offsetZ > 0) {
                    viewOffsetZ.set(offsetZ - 1, click);
                    refreshLayerData(click, design.get(click));
                }
            })
            .updateOnStateChange(viewOffsetZ);
        
        // Navigation Down (South, +Z)
        render.slot(GeneratorGridSlotMapper.NAVIGATION_DOWN_SLOT)
            .renderWith(() -> {
                boolean canMoveDown = offsetZ + maxVisibleZ < dimensions.z;
                return createNavigationArrow(
                    canMoveDown ? Material.ARROW : Material.GRAY_DYE,
                    "controls.nav_down.title",
                    canMoveDown ? "controls.nav_down.lore" : "controls.nav_down.disabled",
                    player
                );
            })
            .onClick(click -> {
                if (offsetZ + maxVisibleZ < dimensions.z) {
                    viewOffsetZ.set(offsetZ + 1, click);
                    refreshLayerData(click, design.get(click));
                }
            })
            .updateOnStateChange(viewOffsetZ);
        
        // Navigation Left (West, -X)
        render.slot(GeneratorGridSlotMapper.NAVIGATION_LEFT_SLOT)
            .renderWith(() -> {
                boolean canMoveLeft = offsetX > 0;
                return createNavigationArrow(
                    canMoveLeft ? Material.ARROW : Material.GRAY_DYE,
                    "controls.nav_left.title",
                    canMoveLeft ? "controls.nav_left.lore" : "controls.nav_left.disabled",
                    player
                );
            })
            .onClick(click -> {
                if (offsetX > 0) {
                    viewOffsetX.set(offsetX - 1, click);
                    refreshLayerData(click, design.get(click));
                }
            })
            .updateOnStateChange(viewOffsetX);
        
        // Navigation Right (East, +X)
        render.slot(GeneratorGridSlotMapper.NAVIGATION_RIGHT_SLOT)
            .renderWith(() -> {
                boolean canMoveRight = offsetX + maxVisibleX < dimensions.x;
                return createNavigationArrow(
                    canMoveRight ? Material.ARROW : Material.GRAY_DYE,
                    "controls.nav_right.title",
                    canMoveRight ? "controls.nav_right.lore" : "controls.nav_right.disabled",
                    player
                );
            })
            .onClick(click -> {
                if (offsetX + maxVisibleX < dimensions.x) {
                    viewOffsetX.set(offsetX + 1, click);
                    refreshLayerData(click, design.get(click));
                }
            })
            .updateOnStateChange(viewOffsetX);
        
        // Center View
        render.slot(GeneratorGridSlotMapper.CENTER_VIEW_SLOT)
            .renderWith(() -> createCenterViewButton(player))
            .onClick(click -> {
                viewOffsetX.set(0, click);
                viewOffsetZ.set(0, click);
                refreshLayerData(click, design.get(click));
            });
    }

    /**
     * Renders layer navigation controls (up/down through structure layers).
     */
    private void renderLayerControls(@NotNull RenderContext render, @NotNull Player player, 
                                   @NotNull GeneratorDesign design) {
        int layer = currentLayer.get(render);
        int totalLayers = design.getLayers() != null ? design.getLayers().size() : 1;
        
        // Layer Up
        render.slot(GeneratorGridSlotMapper.LAYER_UP_SLOT)
            .renderWith(() -> {
                boolean canGoUp = layer < totalLayers - 1;
                return createLayerButton(
                    canGoUp ? Material.LADDER : Material.GRAY_DYE,
                    "controls.layer_up.title",
                    canGoUp ? "controls.layer_up.lore" : "controls.layer_up.disabled",
                    layer + 1, totalLayers, player
                );
            })
            .onClick(click -> {
                if (layer < totalLayers - 1) {
                    currentLayer.set(layer + 1, click);
                    viewOffsetX.set(0, click); // Reset viewport when changing layers
                    viewOffsetZ.set(0, click);
                    refreshLayerData(click, design);
                }
            })
            .updateOnStateChange(currentLayer);
        
        // Layer Down
        render.slot(GeneratorGridSlotMapper.LAYER_DOWN_SLOT)
            .renderWith(() -> {
                boolean canGoDown = layer > 0;
                return createLayerButton(
                    canGoDown ? Material.LADDER : Material.GRAY_DYE,
                    "controls.layer_down.title",
                    canGoDown ? "controls.layer_down.lore" : "controls.layer_down.disabled",
                    layer + 1, totalLayers, player
                );
            })
            .onClick(click -> {
                if (layer > 0) {
                    currentLayer.set(layer - 1, click);
                    viewOffsetX.set(0, click); // Reset viewport when changing layers
                    viewOffsetZ.set(0, click);
                    refreshLayerData(click, design);
                }
            })
            .updateOnStateChange(currentLayer);
    }

    /**
     * Renders the structure grid showing blocks in the current viewport.
     */
    private void renderStructureGrid(@NotNull RenderContext render, @NotNull Player player, 
                                   @NotNull GeneratorDesign design) {
        Map<GeneratorGridPosition, Material> blocks = cachedLayerBlocks.get(render);
        int offsetX = viewOffsetX.get(render);
        int offsetZ = viewOffsetZ.get(render);
        
        // Render all structure slots
        for (Integer slot : GeneratorGridSlotMapper.getAllStructureSlotNumbers()) {
            GeneratorGridPosition gridPos = GeneratorGridSlotMapper.getPositionForSlot(slot);
            
            if (gridPos != null) {
                // Calculate world position based on viewport offset
                GeneratorGridPosition worldPos = new GeneratorGridPosition(
                    gridPos.x + offsetX,
                    gridPos.z + offsetZ
                );
                
                Material blockMaterial = blocks.get(worldPos);
                
                render.slot(slot)
                    .renderWith(() -> {
                        if (blockMaterial != null) {
                            return createStructureBlockItem(blockMaterial, worldPos, player);
                        } else {
                            return createEmptySlotItem(worldPos, player);
                        }
                    })
                    .onClick(click -> handleBlockClick(click, worldPos, blockMaterial))
                    .updateOnStateChange(cachedLayerBlocks)
                    .updateOnStateChange(viewOffsetX)
                    .updateOnStateChange(viewOffsetZ);
            }
        }
    }

    /**
     * Renders utility controls (info, back button).
     */
    private void renderUtilityControls(@NotNull RenderContext render, @NotNull Player player) {
        // Info button
        render.slot(GeneratorGridSlotMapper.INFO_SLOT)
            .renderWith(() -> {
                GeneratorDesign designObj = design.get(render);
                GeneratorGridPosition dimensions = layerDimensions.get(render);
                int layer = currentLayer.get(render) + 1;
                int totalLayers = designObj != null && designObj.getLayers() != null ? 
                    designObj.getLayers().size() : 1;
                
                return createInfoButton(designObj, dimensions, layer, totalLayers, player);
            })
            .updateOnStateChange(currentLayer)
            .updateOnStateChange(layerDimensions);
        
        // Back button
        render.slot(GeneratorGridSlotMapper.BACK_BUTTON_SLOT)
            .renderWith(() -> createBackButton(player))
            .onClick(click -> click.closeForPlayer());
    }

    /**
     * Handles clicking on a structure block.
     */
    private void handleBlockClick(@NotNull SlotClickContext click, @NotNull GeneratorGridPosition position, 
                                @Nullable Material material) {
        Player player = click.getPlayer();
        
        if (material != null) {
            player.sendMessage(Component.text("§7Clicked block: §f" + formatMaterialName(material) + 
                " §7at position §f" + position.x + ", " + position.z));
        } else {
            player.sendMessage(Component.text("§7Clicked empty space at position §f" + 
                position.x + ", " + position.z));
        }
    }

    // Helper methods for creating UI items

    private ItemStack createNavigationArrow(@NotNull Material material, @NotNull String titleKey, 
                                          @NotNull String loreKey, @NotNull Player player) {
        return UnifiedBuilderFactory
            .item(material)
            .setName(i18n(titleKey, player).build().component())
            .setLore(i18n(loreKey, player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private ItemStack createLayerButton(@NotNull Material material, @NotNull String titleKey, 
                                      @NotNull String loreKey, int currentLayer, int totalLayers, 
                                      @NotNull Player player) {
        return UnifiedBuilderFactory
            .item(material)
            .setName(i18n(titleKey, player).build().component())
            .setLore(i18n(loreKey, player)
                .withPlaceholders(Map.of(
                    "current_layer", String.valueOf(currentLayer),
                    "total_layers", String.valueOf(totalLayers)
                ))
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private ItemStack createCenterViewButton(@NotNull Player player) {
        return UnifiedBuilderFactory
            .item(Material.COMPASS)
            .setName(i18n("controls.center_view.title", player).build().component())
            .setLore(i18n("controls.center_view.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private ItemStack createStructureBlockItem(@NotNull Material material, @NotNull GeneratorGridPosition position, 
                                             @NotNull Player player) {
        // Handle special materials that can't be displayed as items
        Material displayMaterial = getDisplayMaterial(material);
        
        return UnifiedBuilderFactory
            .item(displayMaterial)
            .setName(i18n("structure.block.title", player)
                .withPlaceholder("material", formatMaterialName(material))
                .build().component())
            .setLore(i18n("structure.block.lore", player)
                .withPlaceholders(Map.of(
                    "x", String.valueOf(position.x),
                    "z", String.valueOf(position.z),
                    "material", formatMaterialName(material)
                ))
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private ItemStack createEmptySlotItem(@NotNull GeneratorGridPosition position, @NotNull Player player) {
        return UnifiedBuilderFactory
            .item(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
            .setName(i18n("structure.empty.title", player).build().component())
            .setLore(i18n("structure.empty.lore", player)
                .withPlaceholders(Map.of(
                    "x", String.valueOf(position.x),
                    "z", String.valueOf(position.z)
                ))
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private ItemStack createInfoButton(@Nullable GeneratorDesign design, @NotNull GeneratorGridPosition dimensions, 
                                     int currentLayer, int totalLayers, @NotNull Player player) {
        String designName = design != null ? design.getNameKey() : "Unknown";
        
        return UnifiedBuilderFactory
            .item(Material.NETHER_STAR)
            .setName(i18n("info.title", player).build().component())
            .setLore(i18n("info.lore", player)
                .withPlaceholders(Map.of(
                    "design_name", designName,
                    "current_layer", String.valueOf(currentLayer),
                    "total_layers", String.valueOf(totalLayers),
                    "width", String.valueOf(dimensions.x),
                    "depth", String.valueOf(dimensions.z)
                ))
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private ItemStack createBackButton(@NotNull Player player) {
        return UnifiedBuilderFactory
            .item(Material.BARRIER)
            .setName(i18n("controls.back.title", player).build().component())
            .setLore(i18n("controls.back.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    /**
     * Gets a displayable material for items that can't be shown directly.
     */
    private Material getDisplayMaterial(@NotNull Material material) {
        return switch (material) {
            case WATER -> Material.BLUE_STAINED_GLASS;
            case LAVA -> Material.ORANGE_STAINED_GLASS;
            case FIRE -> Material.ORANGE_STAINED_GLASS;
            case SOUL_FIRE -> Material.CYAN_STAINED_GLASS;
            case AIR -> Material.LIGHT_GRAY_STAINED_GLASS_PANE;
            default -> material;
        };
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
     * Renders error state when design is not available.
     */
    private void renderErrorState(@NotNull RenderContext render, @NotNull Player player) {
        render.slot(22)
            .renderWith(() -> UnifiedBuilderFactory
                .item(Material.BARRIER)
                .setName(i18n("error.title", player).build().component())
                .setLore(i18n("error.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build());
        
        // Fill border
        render.layoutSlot('X', UnifiedBuilderFactory
            .item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(Component.empty())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build());
    }
}