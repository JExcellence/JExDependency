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
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Enhanced build progress view with grid-based structure visualization.
 */
public class GeneratorBuildProgressView extends BaseView {

    private final State<JExOneblock> plugin = initialState("plugin");
    private final State<GeneratorStructureManager> structureManager = initialState("structureManager");
    private final State<GeneratorDesign> design = initialState("design");
    private final State<PlayerGeneratorStructure> playerStructure = initialState("playerStructure");
    
    // Grid navigation state
    private final MutableState<Integer> viewOffsetX = mutableState(0);
    private final MutableState<Integer> viewOffsetZ = mutableState(0);
    private final MutableState<Integer> currentLayer = mutableState(0);
    
    // Progress tracking states
    private final MutableState<Double> overallProgress = mutableState(0.0);
    private final MutableState<Double> layerProgress = mutableState(0.0);
    private final MutableState<Integer> blocksPlaced = mutableState(0);
    private final MutableState<Integer> totalBlocks = mutableState(0);
    private final MutableState<Boolean> isBuilding = mutableState(true);
    
    // Cached build state data
    private final MutableState<Map<GeneratorGridPosition, BuildState>> cachedBuildStates = mutableState(new HashMap<>());
    private final MutableState<GeneratorGridPosition> layerDimensions = mutableState(new GeneratorGridPosition(1, 1));
    private final MutableState<Long> dataRefreshTimestamp = mutableState(System.currentTimeMillis());

    public enum BuildState {
        COMPLETED,
        IN_PROGRESS,
        MISSING,
        INCORRECT
    }
    
    private BukkitTask updateTask;

    public GeneratorBuildProgressView() {
        super();
    }

    @Override
    protected String getKey() {
        return "generator_build_progress_grid_ui";
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
                double progress = overallProgress.get(openContext);
                return Map.of(
                    "design_name", designObj.getNameKey(),
                    "progress", String.format("%.1f%%", progress * 100)
                );
            }
        } catch (Exception ignored) {}
        return Map.of(
            "design_name", "Building...",
            "progress", "0.0%"
        );
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        GeneratorDesign designObj = design.get(render);
        GeneratorStructureManager manager = structureManager.get(render);
        
        if (designObj == null || manager == null) {
            renderErrorState(render, player);
            return;
        }

        int total = calculateTotalBlocks(designObj);
        totalBlocks.set(total, render);
        
        refreshBuildStateData(render, designObj);
        
        renderNavigationControls(render, player);
        renderLayerControls(render, player, designObj);
        renderBuildStateGrid(render, player, designObj);
        renderUtilityControls(render, player, manager);
        
        startProgressUpdates(render, player, manager);
    }

    @Override
    public void onClose(@NotNull me.devnatan.inventoryframework.context.CloseContext close) {
        stopProgressUpdates();
        super.onClose(close);
    }

    private void refreshBuildStateData(@NotNull me.devnatan.inventoryframework.context.Context render, @NotNull GeneratorDesign design) {
        int layer = currentLayer.get(render);
        Map<GeneratorGridPosition, BuildState> buildStates = new HashMap<>();
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
                            GeneratorGridPosition pos = new GeneratorGridPosition(x, z);
                            BuildState state = determineBuildState(pos, material, render);
                            buildStates.put(pos, state);
                        }
                    }
                }
                
                dimensions = new GeneratorGridPosition(width, depth);
            }
        }
        
        cachedBuildStates.set(buildStates, render);
        layerDimensions.set(dimensions, render);
        dataRefreshTimestamp.set(System.currentTimeMillis(), render);
    }

    private BuildState determineBuildState(@NotNull GeneratorGridPosition position, @NotNull Material expectedMaterial, 
                                         @NotNull me.devnatan.inventoryframework.context.Context render) {
        PlayerGeneratorStructure structure = playerStructure.get(render);
        
        if (structure == null) {
            return BuildState.MISSING;
        }
        
        double progress = overallProgress.get(render);
        int positionHash = Objects.hash(position.x, position.z, currentLayer.get(render));
        double positionProgress = (positionHash % 100) / 100.0;
        
        if (positionProgress < progress - 0.1) {
            return BuildState.COMPLETED;
        } else if (positionProgress < progress + 0.05) {
            return BuildState.IN_PROGRESS;
        } else {
            return BuildState.MISSING;
        }
    }

    private void renderNavigationControls(@NotNull RenderContext render, @NotNull Player player) {
        GeneratorGridPosition dimensions = layerDimensions.get(render);
        int offsetX = viewOffsetX.get(render);
        int offsetZ = viewOffsetZ.get(render);
        
        int maxVisibleX = 9;
        int maxVisibleZ = 5;
        
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
                    refreshBuildStateData(click, design.get(click));
                }
            })
            .updateOnStateChange(viewOffsetZ);
        
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
                    refreshBuildStateData(click, design.get(click));
                }
            })
            .updateOnStateChange(viewOffsetZ);
        
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
                    refreshBuildStateData(click, design.get(click));
                }
            })
            .updateOnStateChange(viewOffsetX);
        
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
                    refreshBuildStateData(click, design.get(click));
                }
            })
            .updateOnStateChange(viewOffsetX);
        
        render.slot(GeneratorGridSlotMapper.CENTER_VIEW_SLOT)
            .renderWith(() -> createCenterViewButton(player))
            .onClick(click -> {
                viewOffsetX.set(0, click);
                viewOffsetZ.set(0, click);
                refreshBuildStateData(click, design.get(click));
            });
    }

    private void renderLayerControls(@NotNull RenderContext render, @NotNull Player player, 
                                   @NotNull GeneratorDesign design) {
        int layer = currentLayer.get(render);
        int totalLayers = design.getLayers() != null ? design.getLayers().size() : 1;
        
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
                    viewOffsetX.set(0, click);
                    viewOffsetZ.set(0, click);
                    refreshBuildStateData(click, design);
                }
            })
            .updateOnStateChange(currentLayer);
        
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
                    viewOffsetX.set(0, click);
                    viewOffsetZ.set(0, click);
                    refreshBuildStateData(click, design);
                }
            })
            .updateOnStateChange(currentLayer);
    }

    private void renderBuildStateGrid(@NotNull RenderContext render, @NotNull Player player, 
                                    @NotNull GeneratorDesign design) {
        Map<GeneratorGridPosition, BuildState> buildStates = cachedBuildStates.get(render);
        int offsetX = viewOffsetX.get(render);
        int offsetZ = viewOffsetZ.get(render);
        
        for (Integer slot : GeneratorGridSlotMapper.getAllStructureSlotNumbers()) {
            GeneratorGridPosition gridPos = GeneratorGridSlotMapper.getPositionForSlot(slot);
            
            if (gridPos != null) {
                GeneratorGridPosition worldPos = new GeneratorGridPosition(
                    gridPos.x + offsetX,
                    gridPos.z + offsetZ
                );
                
                BuildState buildState = buildStates.get(worldPos);
                
                render.slot(slot)
                    .renderWith(() -> {
                        if (buildState != null) {
                            return createBuildStateItem(buildState, worldPos, design, player);
                        } else {
                            return createEmptySlotItem(worldPos, player);
                        }
                    })
                    .onClick(click -> handleBuildStateClick(click, worldPos, buildState))
                    .updateOnStateChange(cachedBuildStates)
                    .updateOnStateChange(viewOffsetX)
                    .updateOnStateChange(viewOffsetZ)
                    .updateOnStateChange(overallProgress);
            }
        }
    }

    private void renderUtilityControls(@NotNull RenderContext render, @NotNull Player player, 
                                     @NotNull GeneratorStructureManager manager) {
        render.slot(GeneratorGridSlotMapper.INFO_SLOT)
            .renderWith(() -> {
                GeneratorDesign designObj = design.get(render);
                double progress = overallProgress.get(render);
                int placed = blocksPlaced.get(render);
                int total = totalBlocks.get(render);
                int layer = currentLayer.get(render) + 1;
                int totalLayers = designObj != null && designObj.getLayers() != null ? 
                    designObj.getLayers().size() : 1;
                
                return createProgressInfoButton(designObj, progress, placed, total, layer, totalLayers, player);
            })
            .updateOnStateChange(overallProgress)
            .updateOnStateChange(blocksPlaced)
            .updateOnStateChange(currentLayer);
        
        render.slot(GeneratorGridSlotMapper.BACK_BUTTON_SLOT)
            .renderWith(() -> {
                boolean building = isBuilding.get(render);
                return createCancelButton(building, player);
            })
            .onClick(click -> {
                if (isBuilding.get(click)) {
                    isBuilding.set(false, click);
                    stopProgressUpdates();
                    player.sendMessage(Component.text("§cBuild process cancelled."));
                }
                click.closeForPlayer();
            })
            .updateOnStateChange(isBuilding);
    }

    private void handleBuildStateClick(@NotNull SlotClickContext click, @NotNull GeneratorGridPosition position, 
                                     @Nullable BuildState buildState) {
        Player player = click.getPlayer();
        
        if (buildState != null) {
            String stateText = switch (buildState) {
                case COMPLETED -> "§aCompleted";
                case IN_PROGRESS -> "§eIn Progress";
                case MISSING -> "§cMissing";
                case INCORRECT -> "§6Incorrect Material";
            };
            
            player.sendMessage(Component.text("§7Block at position §f" + position.x + ", " + position.z + 
                " §7is §f" + stateText));
        }
    }

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

    private ItemStack createBuildStateItem(@NotNull BuildState buildState, @NotNull GeneratorGridPosition position, 
                                         @NotNull GeneratorDesign design, @NotNull Player player) {
        Material material = getBuildStateMaterial(buildState);
        String stateText = getBuildStateText(buildState);
        Material expectedMaterial = getExpectedMaterial(position, design);
        
        return UnifiedBuilderFactory
            .item(material)
            .setName(i18n("build_state.block.title", player)
                .withPlaceholder("state", stateText)
                .build().component())
            .setLore(i18n("build_state.block.lore", player)
                .withPlaceholders(Map.of(
                    "x", String.valueOf(position.x),
                    "z", String.valueOf(position.z),
                    "state", stateText,
                    "expected_material", expectedMaterial != null ? formatMaterialName(expectedMaterial) : "Unknown"
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

    private ItemStack createProgressInfoButton(@Nullable GeneratorDesign design, double progress, 
                                             int placed, int total, int currentLayer, int totalLayers, 
                                             @NotNull Player player) {
        String designName = design != null ? design.getNameKey() : "Unknown";
        
        return UnifiedBuilderFactory
            .item(Material.NETHER_STAR)
            .setName(i18n("progress.info.title", player).build().component())
            .setLore(i18n("progress.info.lore", player)
                .withPlaceholders(Map.of(
                    "design_name", designName,
                    "progress", String.format("%.1f%%", progress * 100),
                    "blocks_placed", String.valueOf(placed),
                    "total_blocks", String.valueOf(total),
                    "current_layer", String.valueOf(currentLayer),
                    "total_layers", String.valueOf(totalLayers)
                ))
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private ItemStack createCancelButton(boolean isBuilding, @NotNull Player player) {
        return UnifiedBuilderFactory
            .item(isBuilding ? Material.BARRIER : Material.GRAY_DYE)
            .setName(i18n(isBuilding ? "controls.cancel.title" : "controls.finished.title", player).build().component())
            .setLore(i18n(isBuilding ? "controls.cancel.lore" : "controls.finished.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private Material getBuildStateMaterial(@NotNull BuildState buildState) {
        return switch (buildState) {
            case COMPLETED -> Material.LIME_STAINED_GLASS;
            case IN_PROGRESS -> Material.YELLOW_STAINED_GLASS;
            case MISSING -> Material.RED_STAINED_GLASS;
            case INCORRECT -> Material.ORANGE_STAINED_GLASS;
        };
    }

    private String getBuildStateText(@NotNull BuildState buildState) {
        return switch (buildState) {
            case COMPLETED -> "§aCompleted";
            case IN_PROGRESS -> "§eIn Progress";
            case MISSING -> "§cMissing";
            case INCORRECT -> "§6Incorrect";
        };
    }

    @Nullable
    private Material getExpectedMaterial(@NotNull GeneratorGridPosition position, @NotNull GeneratorDesign design) {
        int layer = currentLayer.get(null);
        
        if (design.getLayers() == null || layer < 0 || layer >= design.getLayers().size()) {
            return null;
        }
        
        GeneratorDesignLayer layerData = design.getLayers().get(layer);
        Material[][] pattern = layerData.getPattern();
        
        if (pattern == null || position.x >= layerData.getWidth() || position.z >= layerData.getDepth()) {
            return null;
        }
        
        return pattern[position.x][position.z];
    }

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

    private int calculateTotalBlocks(@NotNull GeneratorDesign design) {
        int total = 0;
        
        if (design.getLayers() != null) {
            for (GeneratorDesignLayer layer : design.getLayers()) {
                Material[][] pattern = layer.getPattern();
                
                if (pattern != null) {
                    for (int x = 0; x < layer.getWidth(); x++) {
                        for (int z = 0; z < layer.getDepth(); z++) {
                            Material material = pattern[x][z];
                            if (material != null && material != Material.AIR) {
                                total++;
                            }
                        }
                    }
                }
            }
        }
        
        return total;
    }

    private void startProgressUpdates(@NotNull RenderContext render, @NotNull Player player, 
                                    @NotNull GeneratorStructureManager manager) {
        JExOneblock pluginInstance = plugin.get(render);
        
        if (pluginInstance != null && updateTask == null) {
            updateTask = pluginInstance.getPlugin().getServer().getScheduler().runTaskTimer(pluginInstance.getPlugin(), () -> {
                double currentProgress = overallProgress.get(render);
                if (currentProgress < 1.0 && isBuilding.get(render)) {
                    double newProgress = Math.min(1.0, currentProgress + 0.01);
                    overallProgress.set(newProgress, render);
                    
                    int newBlocksPlaced = (int) (newProgress * totalBlocks.get(render));
                    blocksPlaced.set(newBlocksPlaced, render);
                    
                    if (newProgress >= 1.0) {
                        isBuilding.set(false, render);
                        player.sendMessage(Component.text("§aBuild completed!"));
                    }
                }
            }, 20L, 20L);
        }
    }

    private void stopProgressUpdates() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private void renderErrorState(@NotNull RenderContext render, @NotNull Player player) {
        render.slot(22)
            .renderWith(() -> UnifiedBuilderFactory
                .item(Material.BARRIER)
                .setName(i18n("error.title", player).build().component())
                .setLore(i18n("error.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build());
        
        render.layoutSlot('X', UnifiedBuilderFactory
            .item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(Component.empty())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build());
    }
}
