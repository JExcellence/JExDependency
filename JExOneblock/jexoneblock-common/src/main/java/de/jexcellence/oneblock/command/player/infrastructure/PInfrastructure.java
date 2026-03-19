package de.jexcellence.oneblock.command.player.infrastructure;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import de.jexcellence.jextranslate.i18n.I18n;
import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.view.infrastructure.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Infrastructure command - Opens infrastructure GUI and provides info commands
 * Modern Java - clean command handling
 */
@Command
@SuppressWarnings("unused")
public class PInfrastructure extends PlayerCommand {
    
    private final JExOneblock plugin;
    
    public PInfrastructure(
        @NotNull PInfrastructureSection commandSection,
        @NotNull Object pluginObject
    ) {
        super(commandSection);
        if (!(pluginObject instanceof JExOneblock)) {
            throw new IllegalArgumentException("Plugin must be an instance of JExOneblock");
        }
        this.plugin = (JExOneblock) pluginObject;
    }
    
    @Override
    protected void onPlayerInvocation(
        @NotNull Player player,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (hasNoPermission(player, EInfrastructurePermission.COMMAND)) {
            return;
        }
        
        // Validate infrastructure access
        if (!validateInfrastructureAccess(player)) {
            return;
        }
        
        // Get player's island infrastructure
        var islandId = plugin.getOneblockService().getPlayerIslandId(player);
        var infra = plugin.getInfrastructureService().getInfrastructure(islandId, player.getUniqueId());
        
        if (infra == null) {
            new I18n.Builder("infrastructure.data_not_found", player).includePrefix().build().sendMessage();
            // Create new infrastructure
            infra = new de.jexcellence.oneblock.database.entity.infrastructure.IslandInfrastructure(islandId, player.getUniqueId());
            // This will be saved by the service layer
        }
        
        var action = enumParameterOrElse(args, 0, EInfrastructureAction.class, EInfrastructureAction.MAIN);
        
        try {
            switch (action) {
                case MAIN -> {
                    plugin.getViewFrame().open(InfrastructureMainView.class, player, Map.of(
                        "plugin", plugin,
                        "infrastructure", infra
                    ));
                }
                case STATS -> {
                    if (hasNoPermission(player, EInfrastructurePermission.STATS)) return;
                    plugin.getViewFrame().open(InfrastructureStatsView.class, player, Map.of(
                        "plugin", plugin,
                        "infrastructure", infra
                    ));
                }
                case ENERGY -> {
                    if (hasNoPermission(player, EInfrastructurePermission.ENERGY)) return;
                    sendEnergyInfo(player, infra);
                }
                case STORAGE -> {
                    if (hasNoPermission(player, EInfrastructurePermission.STORAGE)) return;
                    // Sync storage before opening view
                    if (plugin.getIslandStorageManager() != null) {
                        plugin.getIslandStorageManager().syncWithInfrastructure(islandId, infra);
                    }
                    plugin.getViewFrame().open(StorageView.class, player, Map.of(
                        "plugin", plugin,
                        "infrastructure", infra
                    ));
                }
                case AUTOMATION -> {
                    if (hasNoPermission(player, EInfrastructurePermission.AUTOMATION)) return;
                    plugin.getViewFrame().open(AutomationView.class, player, Map.of(
                        "plugin", plugin,
                        "infrastructure", infra
                    ));
                }
                case PROCESSORS -> {
                    if (hasNoPermission(player, EInfrastructurePermission.PROCESSORS)) return;
                    plugin.getViewFrame().open(ProcessorsView.class, player, Map.of(
                        "plugin", plugin,
                        "infrastructure", infra
                    ));
                }
                case GENERATORS -> {
                    if (hasNoPermission(player, EInfrastructurePermission.GENERATORS)) return;
                    plugin.getViewFrame().open(GeneratorsView.class, player, Map.of(
                        "plugin", plugin,
                        "infrastructure", infra
                    ));
                }
                case CRAFTING -> {
                    if (hasNoPermission(player, EInfrastructurePermission.CRAFTING)) return;
                    plugin.getViewFrame().open(CraftingQueueView.class, player, Map.of(
                        "plugin", plugin,
                        "infrastructure", infra
                    ));
                }
                case HELP -> sendHelp(player);
            }
        } catch (Exception e) {
            // Log the error and show user-friendly message
            plugin.getPlugin().getLogger().severe("Failed to execute infrastructure command: " + e.getMessage());
            e.printStackTrace();
            
            String errorKey = switch (action) {
                case MAIN -> "infrastructure.main.error";
                case STATS -> "infrastructure.stats.error";
                case STORAGE -> "infrastructure.storage.error";
                case AUTOMATION -> "infrastructure.automation.error";
                case PROCESSORS -> "infrastructure.processors.error";
                case GENERATORS -> "infrastructure.generators.error";
                case CRAFTING -> "infrastructure.crafting.error";
                default -> "infrastructure.main.error";
            };
            
            new I18n.Builder(errorKey, player).includePrefix().build().sendMessage();
        }
    }
    
    @Override
    protected List<String> onPlayerTabCompletion(
        @NotNull Player player,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (hasNoPermission(player, EInfrastructurePermission.COMMAND)) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            var suggestions = Arrays.stream(EInfrastructureAction.values())
                .map(a -> a.name().toLowerCase())
                .toList();
            return StringUtil.copyPartialMatches(args[0].toLowerCase(), suggestions, new ArrayList<>());
        }
        
        return new ArrayList<>();
    }
    
    private void sendEnergyInfo(Player player, de.jexcellence.oneblock.database.entity.infrastructure.IslandInfrastructure infra) {
        var generation = infra.calculateEnergyGeneration();
        var consumption = infra.calculateEnergyConsumption();
        var net = generation - consumption;
        
        new I18n.Builder("infrastructure.energy.header", player).includePrefix().build().sendMessage();
        new I18n.Builder("infrastructure.energy.current", player)
            .withPlaceholder("current", formatNumber(infra.getCurrentEnergy()))
            .withPlaceholder("max", formatNumber(infra.getEnergyCapacity()))
            .build().sendMessage();
        new I18n.Builder("infrastructure.energy.generation", player)
            .withPlaceholder("value", String.format("%.1f", generation))
            .build().sendMessage();
        new I18n.Builder("infrastructure.energy.consumption", player)
            .withPlaceholder("value", String.format("%.1f", consumption))
            .build().sendMessage();
        new I18n.Builder("infrastructure.energy.net", player)
            .withPlaceholder("value", String.format("%s%.1f", net >= 0 ? "+" : "", net))
            .build().sendMessage();
    }
    
    private void sendHelp(Player player) {
        new I18n.Builder("infrastructure.help.header", player).includePrefix().build().sendMessage();
        new I18n.Builder("infrastructure.help.main", player).build().sendMessage();
        new I18n.Builder("infrastructure.help.stats", player).build().sendMessage();
        new I18n.Builder("infrastructure.help.energy", player).build().sendMessage();
        new I18n.Builder("infrastructure.help.storage", player).build().sendMessage();
        new I18n.Builder("infrastructure.help.automation", player).build().sendMessage();
        new I18n.Builder("infrastructure.help.processors", player).build().sendMessage();
        new I18n.Builder("infrastructure.help.generators", player).build().sendMessage();
        new I18n.Builder("infrastructure.help.crafting", player).build().sendMessage();
    }
    
    private String formatNumber(long number) {
        if (number >= 1_000_000_000) return String.format("%.1fB", number / 1_000_000_000.0);
        if (number >= 1_000_000) return String.format("%.1fM", number / 1_000_000.0);
        if (number >= 1_000) return String.format("%.1fK", number / 1_000.0);
        return String.valueOf(number);
    }
    
    /**
     * Validates that the player can access infrastructure functionality
     */
    private boolean validateInfrastructureAccess(Player player) {
        // Check if infrastructure service is available
        if (plugin.getInfrastructureService() == null) {
            new I18n.Builder("infrastructure.service_unavailable", player)
                .includePrefix().build().sendMessage();
            return false;
        }
        
        // Check if player has an island
        var islandId = plugin.getOneblockService().getPlayerIslandId(player);
        if (islandId == null) {
            new I18n.Builder("infrastructure.no_island", player)
                .includePrefix().build().sendMessage();
            return false;
        }
        
        return true;
    }
}
