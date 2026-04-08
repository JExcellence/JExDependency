package de.jexcellence.oneblock.command.player.island;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import de.jexcellence.jextranslate.i18n.I18n;
import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.database.entity.biome.EBiomeCategory;
import de.jexcellence.oneblock.database.entity.storage.StorageCategory;
import de.jexcellence.oneblock.view.infrastructure.InfrastructureStatsView;
import de.jexcellence.oneblock.view.island.BiomeSelectionView;
import de.jexcellence.oneblock.view.island.EvolutionBrowserView;
import de.jexcellence.oneblock.view.island.IslandMainView;
import de.jexcellence.oneblock.view.island.IslandSettingsView;
import de.jexcellence.oneblock.view.island.MembersListView;
import de.jexcellence.oneblock.view.island.OneblockCoreView;
import de.jexcellence.oneblock.view.island.VisitorSettingsView;
import de.jexcellence.oneblock.view.storage.StorageCategoryView;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Command
@SuppressWarnings("unused")
public class PIsland extends PlayerCommand {
    
    private final JExOneblock plugin;
    
    public PIsland(@NotNull PIslandSection commandSection, @NotNull JExOneblock plugin) {
        super(commandSection);
        this.plugin = plugin;
    }
    
    @Override
    protected void onPlayerInvocation(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        if (this.hasNoPermission(player, EIslandPermission.COMMAND)) {
            return;
        }
        
        EIslandAction action = enumParameterOrElse(args, 0, EIslandAction.class, EIslandAction.MAIN);
        
        switch (action) {
            case MAIN -> {
                if (this.hasNoPermission(player, EIslandPermission.COMMAND)) return;
                var oneblockPlayer = this.plugin.getOneblockPlayerRepository().findByUuid(player.getUniqueId());
                if (oneblockPlayer != null && oneblockPlayer.getOwnedIsland() != null) {
                    this.plugin.getViewFrame().open(IslandMainView.class, player, Map.of(
                        "plugin", this.plugin,
                        "island", oneblockPlayer.getOwnedIsland()
                    ));
                } else {
                    new I18n.Builder("island.command.no_island", player).includePrefix().build().sendMessage();
                }
            }
            case INFO -> {
                if (this.hasNoPermission(player, EIslandPermission.INFO)) return;
                this.plugin.getOneblockService().showIslandInfo(player);
            }
            case STATS -> {
                if (this.hasNoPermission(player, EIslandPermission.STATS)) return;
                this.plugin.getViewFrame().open(InfrastructureStatsView.class, player, Map.of("plugin", this.plugin));
            }
            case LEVEL -> {
                if (this.hasNoPermission(player, EIslandPermission.LEVEL)) return;
                this.plugin.getOneblockService().showIslandLevel(player);
            }
            case TOP -> {
                if (this.hasNoPermission(player, EIslandPermission.TOP)) return;
                this.plugin.getOneblockService().showTopIslands(player);
            }
            case EVOLUTION -> {
                if (this.hasNoPermission(player, EIslandPermission.EVOLUTION)) return;
                var oneblockPlayer = this.plugin.getOneblockPlayerRepository().findByUuid(player.getUniqueId());
                if (oneblockPlayer != null && oneblockPlayer.getOwnedIsland() != null) {
                    this.plugin.getViewFrame().open(EvolutionBrowserView.class, player, Map.of(
                        "plugin", this.plugin,
                        "island", oneblockPlayer.getOwnedIsland()
                    ));
                } else {
                    new I18n.Builder("island.command.no_island", player).includePrefix().build().sendMessage();
                }
            }
            case ONEBLOCK -> {
                if (this.hasNoPermission(player, EIslandPermission.ONEBLOCK)) return;
                this.plugin.getViewFrame().open(OneblockCoreView.class, player, Map.of("plugin", this.plugin));
            }
            case PRESTIGE -> {
                if (this.hasNoPermission(player, EIslandPermission.PRESTIGE)) return;
                this.plugin.getOneblockService().handlePrestige(player);
            }
            case HOME, TP -> {
                if (this.hasNoPermission(player, EIslandPermission.HOME)) return;
                this.plugin.getOneblockService().teleportToIsland(player);
            }
            case SETHOME -> {
                if (this.hasNoPermission(player, EIslandPermission.SETHOME)) return;
                this.plugin.getOneblockService().setIslandHome(player);
            }
            case MEMBERS -> {
                if (this.hasNoPermission(player, EIslandPermission.MEMBERS)) return;
                this.plugin.getViewFrame().open(MembersListView.class, player, Map.of("plugin", this.plugin, "currentPage", 0));
            }
            case INVITE -> {
                if (this.hasNoPermission(player, EIslandPermission.INVITE)) return;
                this.plugin.getOneblockService().handleInvite(player, args);
            }
            case ACCEPT -> {
                if (this.hasNoPermission(player, EIslandPermission.ACCEPT)) return;
                this.plugin.getOneblockService().handleAcceptInvite(player, args);
            }
            case DENY -> {
                if (this.hasNoPermission(player, EIslandPermission.DENY)) return;
                this.plugin.getOneblockService().handleDenyInvite(player, args);
            }
            case KICK -> {
                if (this.hasNoPermission(player, EIslandPermission.KICK)) return;
                this.plugin.getOneblockService().handleKick(player, args);
            }
            case BAN -> {
                if (this.hasNoPermission(player, EIslandPermission.BAN)) return;
                this.plugin.getOneblockService().handleBan(player, args);
            }
            case UNBAN -> {
                if (this.hasNoPermission(player, EIslandPermission.UNBAN)) return;
                this.plugin.getOneblockService().handleUnban(player, args);
            }
            case LEAVE -> {
                if (this.hasNoPermission(player, EIslandPermission.LEAVE)) return;
                this.plugin.getOneblockService().handleLeave(player);
            }
            case SETTINGS -> {
                if (this.hasNoPermission(player, EIslandPermission.SETTINGS)) return;
                this.plugin.getViewFrame().open(IslandSettingsView.class, player, Map.of("plugin", this.plugin));
            }
            case VISITORS -> {
                if (this.hasNoPermission(player, EIslandPermission.VISITORS)) return;
                this.plugin.getViewFrame().open(VisitorSettingsView.class, player, Map.of("plugin", this.plugin));
            }
            case BIOME -> {
                if (this.hasNoPermission(player, EIslandPermission.BIOME)) return;
                var oneblockPlayer = this.plugin.getOneblockPlayerRepository().findByUuid(player.getUniqueId());
                if (oneblockPlayer != null && oneblockPlayer.getOwnedIsland() != null) {
                    EBiomeCategory category = args.length > 1 
                        ? EBiomeCategory.fromName(args[1])
                        : EBiomeCategory.TEMPERATE;
                    this.plugin.getViewFrame().open(BiomeSelectionView.class, player, Map.of(
                        "plugin", this.plugin,
                        "island", oneblockPlayer.getOwnedIsland(),
                        "selectedCategory", category != null ? category : EBiomeCategory.TEMPERATE
                    ));
                } else {
                    new I18n.Builder("island.command.no_island", player).includePrefix().build().sendMessage();
                }
            }
            case UPGRADES -> {
                if (this.hasNoPermission(player, EIslandPermission.UPGRADES)) return;
                new I18n.Builder("island.upgrades.coming_soon", player).includePrefix().build().sendMessage();
            }
            case STORAGE -> {
                if (this.hasNoPermission(player, EIslandPermission.STORAGE)) return;
                if (args.length <= 1) {
                    var oneblockPlayer = this.plugin.getOneblockPlayerRepository().findByUuid(player.getUniqueId());
                    if (oneblockPlayer != null && oneblockPlayer.getOwnedIsland() != null) {
                        /*this.plugin.getViewFrame().open(StorageMainView.class, player, Map.of(
                            "plugin", this.plugin,
                            "island", oneblockPlayer.getOwnedIsland()
                        ));*/
                    } else {
                        new I18n.Builder("island.command.no_island", player).includePrefix().build().sendMessage();
                    }
                } else {
                    String subcommand = args[1].toLowerCase();
                    switch (subcommand) {
                        case "info", "stats" -> this.plugin.getOneblockService().showStorageInfo(player);
                        case "search" -> {
                            if (args.length < 3) {
                                new I18n.Builder("storage.commands.search_usage", player).includePrefix().build().sendMessage();
                            } else {
                                new I18n.Builder("storage.search.coming_soon", player).includePrefix().build().sendMessage();
                            }
                        }
                        default -> {
                            StorageCategory category = StorageCategory.fromName(subcommand);
                            this.plugin.getViewFrame().open(StorageCategoryView.class, player, Map.of(
                                "plugin", this.plugin,
                                "category", category
                            ));
                        }
                    }
                }
            }
            case CREATE -> {
                if (this.hasNoPermission(player, EIslandPermission.CREATE)) return;
                this.plugin.getOneblockService().createIsland(player);
            }
            case DELETE -> {
                if (this.hasNoPermission(player, EIslandPermission.DELETE)) return;
                this.plugin.getOneblockService().deleteIsland(player);
            }
            case HELP -> {
                new I18n.Builder("island.help.header", player).includePrefix().build().sendMessage();
                for (EIslandAction act : EIslandAction.values()) {
                    if (act != EIslandAction.HELP) {
                        new I18n.Builder("island.help.action", player)
                            .withPlaceholder("usage", act.getUsage())
                            .withPlaceholder("description", act.getDescription())
                            .build().sendMessage();
                    }
                }
            }
        }
    }
    
    @Override
    protected List<String> onPlayerTabCompletion(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        if (this.hasNoPermission(player, EIslandPermission.COMMAND)) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>(
                Arrays.stream(EIslandAction.values()).map(Enum::name).map(String::toLowerCase).toList()
            );
            return StringUtil.copyPartialMatches(args[0].toLowerCase(), suggestions, new ArrayList<>());
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("storage")) {
            List<String> suggestions = new ArrayList<>(Arrays.asList("info", "search"));
            for (StorageCategory category : StorageCategory.values()) {
                suggestions.add(category.name().toLowerCase());
            }
            return StringUtil.copyPartialMatches(args[1].toLowerCase(), suggestions, new ArrayList<>());
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("biome")) {
            List<String> suggestions = new ArrayList<>(Arrays.asList(EBiomeCategory.getCategoryNames()));
            return StringUtil.copyPartialMatches(args[1].toUpperCase(), suggestions, new ArrayList<>());
        }
        
        return new ArrayList<>();
    }
}
