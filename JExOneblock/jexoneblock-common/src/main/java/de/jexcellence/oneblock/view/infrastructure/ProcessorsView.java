package de.jexcellence.oneblock.view.infrastructure;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.database.entity.infrastructure.IslandInfrastructure;
import de.jexcellence.oneblock.database.entity.infrastructure.ProcessorType;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Processors View - Paginated list of all processor types with upgrade options
 * Clean, modern Java - no verbose patterns
 */
public class ProcessorsView extends APaginatedView<ProcessorType> {
    
    private final State<JExOneblock> plugin = initialState("plugin");
    private final State<IslandInfrastructure> infrastructure = initialState("infrastructure");
    
    public ProcessorsView() {
        super(InfrastructureMainView.class);
    }
    
    @Override
    protected String getKey() {
        return "processors_ui";
    }
    
    @Override
    protected CompletableFuture<List<ProcessorType>> getAsyncPaginationSource(@NotNull Context context) {
        return CompletableFuture.completedFuture(Arrays.asList(ProcessorType.values()));
    }
    
    @Override
    protected void renderEntry(@NotNull Context context, @NotNull BukkitItemComponentBuilder builder, 
                               int index, @NotNull ProcessorType processor) {
        var player = context.getPlayer();
        var infra = infrastructure.get(context);
        var currentLevel = infra.getProcessors().getOrDefault(processor, 0);
        var maxLevel = processor.getMaxLevel();
        var isMaxed = currentLevel >= maxLevel;
        var isUnlocked = currentLevel > 0;
        
        builder
            .withItem(UnifiedBuilderFactory
                .item(getProcessorMaterial(processor, isUnlocked))
                .setName(i18n("processor.name", player)
                    .withPlaceholder("name", processor.getDisplayName())
                    .withPlaceholder("level", currentLevel)
                    .withPlaceholder("max", maxLevel)
                    .build().component())
                .setLore(i18n(isMaxed ? "processor.lore_maxed" : "processor.lore", player)
                    .withPlaceholder("description", processor.getDescription())
                    .withPlaceholder("speed", String.format("%.1f", processor.getProcessingSpeed(currentLevel)))
                    .withPlaceholder("energy", String.format("%.1f", processor.getEnergyConsumption(currentLevel)))
                    .withPlaceholder("stage", processor.getRequiredStage())
                    .build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build())
            .onClick(click -> {
                if (!isMaxed) {
                    click.openForPlayer(ProcessorUpgradeView.class, Map.of(
                        "plugin", plugin.get(click),
                        "infrastructure", infra,
                        "processor", processor
                    ));
                }
            });
    }
    
    @Override
    protected void onPaginatedRender(@NotNull RenderContext render, @NotNull Player player) {
        // Stats header could go here if needed
    }
    
    private Material getProcessorMaterial(ProcessorType processor, boolean unlocked) {
        if (!unlocked) return Material.GRAY_DYE;
        return switch (processor) {
            case BASIC_MINER -> Material.DIAMOND_PICKAXE;
            case ADVANCED_SMELTER -> Material.BLAST_FURNACE;
            case QUANTUM_CRAFTER -> Material.CRAFTER;
            case MOLECULAR_ASSEMBLER -> Material.SCULK_CATALYST;
            case DIMENSIONAL_PROCESSOR -> Material.END_PORTAL_FRAME;
            case REALITY_MANIPULATOR -> Material.COMMAND_BLOCK;
        };
    }
}
