package de.jexcellence.oneblock.view.infrastructure;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.database.entity.infrastructure.IslandInfrastructure;
import de.jexcellence.oneblock.database.entity.infrastructure.ProcessorType;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

/**
 * Processor Upgrade View - Detailed upgrade interface for processors
 * Modern Java, clean code
 */
public class ProcessorUpgradeView extends BaseView {
    
    private final State<JExOneblock> plugin = initialState("plugin");
    private final State<IslandInfrastructure> infrastructure = initialState("infrastructure");
    private final State<ProcessorType> processor = initialState("processor");
    
    public ProcessorUpgradeView() {
        super(ProcessorsView.class);
    }
    
    @Override
    protected String getKey() {
        return "processor_upgrade_ui";
    }
    
    @Override
    protected String[] getLayout() {
        return new String[]{
            "XXXXXXXXX",
            "X I   R X",
            "X   C   X",
            "XXXXXXXXX"
        };
    }
    
    @Override
    protected int getSize() {
        return 4;
    }
    
    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        var infra = infrastructure.get(render);
        var proc = processor.get(render);
        var currentLevel = infra.getProcessors().getOrDefault(proc, 0);
        var nextLevel = currentLevel + 1;
        var upgrade = proc.getUpgrade(nextLevel);
        var canUpgrade = upgrade != null && infra.canSupportUpgrade(proc, nextLevel);
        
        // Info (I)
        render.layoutSlot('I', UnifiedBuilderFactory
            .item(Material.BOOK)
            .setName(i18n("info.name", player)
                .withPlaceholder("name", proc.getDisplayName())
                .build().component())
            .setLore(i18n("info.lore", player)
                .withPlaceholder("description", proc.getDescription())
                .withPlaceholder("current_level", currentLevel)
                .withPlaceholder("max_level", proc.getMaxLevel())
                .withPlaceholder("current_speed", String.format("%.1f", proc.getProcessingSpeed(currentLevel)))
                .withPlaceholder("next_speed", upgrade != null ? String.format("%.1f", upgrade.getProcessingSpeed()) : "MAX")
                .withPlaceholder("energy", String.format("%.1f/s", proc.getEnergyConsumption(currentLevel)))
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        );
        
        // Requirements (R)
        if (upgrade != null) {
            var materials = upgrade.getMaterials();
            var loreBuilder = i18n("requirements.lore", player)
                .withPlaceholder("cost", formatNumber(upgrade.getCost()));
            
            render.layoutSlot('R', UnifiedBuilderFactory
                .item(Material.PAPER)
                .setName(i18n("requirements.name", player).build().component())
                .setLore(loreBuilder.build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build()
            );
        }
        
        // Confirm (C)
        render.layoutSlot('C', UnifiedBuilderFactory
            .item(canUpgrade ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK)
            .setName(i18n(canUpgrade ? "confirm.name" : "confirm.name_locked", player)
                .withPlaceholder("level", nextLevel)
                .build().component())
            .setLore(i18n(canUpgrade ? "confirm.lore" : "confirm.lore_locked", player)
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(click -> {
            if (canUpgrade) {
                var result = plugin.get(click).getInfrastructureManager()
                    .upgradeProcessor(player, infra.getIslandId(), proc);
                
                if (result == de.jexcellence.oneblock.manager.infrastructure.InfrastructureManager.CraftingResult.QUEUED) {
                    i18n("upgrade.queued", player)
                        .withPlaceholder("name", proc.getDisplayName())
                        .withPlaceholder("level", nextLevel)
                        .includePrefix().build().sendMessage();
                    click.openForPlayer(ProcessorsView.class, click.getInitialData());
                } else {
                    i18n("upgrade.failed", player)
                        .withPlaceholder("reason", result.name())
                        .includePrefix().build().sendMessage();
                }
            }
        });
    }
    
    private String formatNumber(long number) {
        if (number >= 1_000_000_000) return String.format("%.1fB", number / 1_000_000_000.0);
        if (number >= 1_000_000) return String.format("%.1fM", number / 1_000_000.0);
        if (number >= 1_000) return String.format("%.1fK", number / 1_000.0);
        return String.valueOf(number);
    }
}
