package de.jexcellence.oneblock.view.island;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.bonus.BonusManager;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockCore;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.factory.EvolutionFactory;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class EvolutionBrowserView extends APaginatedView<EvolutionBrowserView.EvolutionInfo> {

    private final State<JExOneblock> plugin = initialState("plugin");
    private final State<OneblockIsland> island = initialState("island");

    @Override
    protected String getKey() {
        return "evolution_browser_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "XXXXXXXXX",
            "XOOOOOOOX",
            "XOOOOOOOX",
            "XOOOOOOOX",
            "XXXXXXXXX",
            " H<p>F   "
        };
    }

    @Override
    protected void onPaginatedRender(@NotNull RenderContext render, @NotNull Player player) {
        renderActionButtons(render, player);
        renderBorder(render);
    }

    @Override
    protected CompletableFuture<List<EvolutionInfo>> getAsyncPaginationSource(@NotNull Context context) {
        return CompletableFuture.supplyAsync(() -> {
            var evolutionInfos = new ArrayList<EvolutionInfo>();
            
            try {
                var evolutionFactory = EvolutionFactory.getInstance();
                var evolutionNames = evolutionFactory.getRegisteredEvolutionNames();
                
                if (!evolutionNames.isEmpty()) {
                    for (String evolutionName : evolutionNames) {
                        try {
                            var evolution = evolutionFactory.getCachedEvolution(evolutionName);
                            if (evolution != null) {
                                var material = evolution.getShowcase() != null ? 
                                    evolution.getShowcase() : Material.COBBLESTONE;
                                var minLevel = evolution.getLevel();
                                var requiredBlocks = evolution.getExperienceToPass();
                                var description = evolution.getDescription() != null ? 
                                    evolution.getDescription() : "Evolution: " + evolutionName;
                                
                                evolutionInfos.add(new EvolutionInfo(
                                    evolutionName, 
                                    material, 
                                    minLevel, 
                                    requiredBlocks, 
                                    description
                                ));
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to load evolution " + evolutionName + " from factory: " + e.getMessage());
                        }
                    }
                }
                
                if (evolutionInfos.isEmpty()) {
                    var pluginInstance = plugin.get(context);
                    if (pluginInstance != null) {
                        try {
                            var repository = pluginInstance.getOneblockEvolutionRepository();
                            if (repository != null) {
                                var dbEvolutions = repository.findAllActive();
                                
                                for (var evolution : dbEvolutions) {
                                    try {
                                        var material = evolution.getShowcase() != null ? 
                                            evolution.getShowcase() : Material.COBBLESTONE;
                                        var minLevel = evolution.getLevel();
                                        var requiredBlocks = evolution.getExperienceToPass();
                                        var description = evolution.getDescription() != null ? 
                                            evolution.getDescription() : "Evolution: " + evolution.getEvolutionName();
                                        
                                        evolutionInfos.add(new EvolutionInfo(
                                            evolution.getEvolutionName(), 
                                            material, 
                                            minLevel, 
                                            requiredBlocks, 
                                            description
                                        ));
                                    } catch (Exception e) {
                                        System.err.println("Failed to process evolution " + evolution.getEvolutionName() + " from repository: " + e.getMessage());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to access repository: " + e.getMessage());
                        }
                    }
                }
                
                evolutionInfos.sort((a, b) -> Integer.compare(a.minLevel(), b.minLevel()));
                
                return evolutionInfos;
                
            } catch (Exception e) {
                System.err.println("Critical error in evolution loading: " + e.getMessage());
                e.printStackTrace();
                
                return new ArrayList<>();
            }
        });
    }
    
    @Override
    protected void renderEntry(@NotNull Context context, @NotNull BukkitItemComponentBuilder builder,
                               int index, @NotNull EvolutionInfo evolution) {
        var player = context.getPlayer();
        var islandData = island.get(context);
        
        // Handle case where island might not be set
        if (islandData == null) {
            builder.withItem(UnifiedBuilderFactory
                .item(Material.BARRIER)
                .setName(Component.text("§cNo Island Data"))
                .build());
            return;
        }

        var isUnlocked = isEvolutionUnlocked(islandData, evolution);
        var isCurrent = islandData.getCurrentEvolution().equalsIgnoreCase(evolution.name());

        var material = isUnlocked ? evolution.material() : Material.GRAY_STAINED_GLASS;

        if (isCurrent) {
            builder.withItem(UnifiedBuilderFactory
                .item(material)
                .setName(i18n("current", player)
                    .withPlaceholder("evolution", evolution.name())
                    .build().component())
                .setLore(i18n("current_lore", player)
                    .withPlaceholder("level", evolution.minLevel())
                    .withPlaceholder("blocks", formatNumber(evolution.requiredBlocks()))
                    .withPlaceholder("description", evolution.description())
                    .build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .setGlowing(true)
                .build());
        } else if (isUnlocked) {
            builder.withItem(UnifiedBuilderFactory
                .item(material)
                .setName(i18n("unlocked", player)
                    .withPlaceholder("evolution", evolution.name())
                    .build().component())
                .setLore(i18n("unlocked_lore", player)
                    .withPlaceholder("level", evolution.minLevel())
                    .withPlaceholder("blocks", formatNumber(evolution.requiredBlocks()))
                    .withPlaceholder("description", evolution.description())
                    .build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build());
        } else {
            builder.withItem(UnifiedBuilderFactory
                .item(material)
                .setName(i18n("locked", player)
                    .withPlaceholder("evolution", evolution.name())
                    .build().component())
                .setLore(i18n("locked_lore", player)
                    .withPlaceholder("level", evolution.minLevel())
                    .withPlaceholder("blocks", formatNumber(evolution.requiredBlocks()))
                    .withPlaceholder("current_blocks", formatNumber(islandData.getTotalBlocksBroken()))
                    .withPlaceholder("description", evolution.description())
                    .build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build());
        }

        builder.onClick(ctx -> {
            // Open detail view on click
            ctx.openForPlayer(EvolutionDetailView.class, Map.of(
                "plugin", plugin.get(ctx),
                "evolutionName", evolution.name()
            ));
        });
    }

    private void renderActionButtons(@NotNull RenderContext render, @NotNull Player player) {
        // Filter (F)
        render.layoutSlot('F', UnifiedBuilderFactory
            .item(Material.HOPPER)
            .setName(i18n("filter", player).build().component())
            .setLore(i18n("filter_lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            i18n("filter_coming_soon", player).includePrefix().build().sendMessage();
        });

        // Help (H)
        render.layoutSlot('H', UnifiedBuilderFactory
            .item(Material.BOOK)
            .setName(i18n("common.help", player).build().component())
            .setLore(Arrays.asList(Component.text("§7Click for help")))
            .build()
        ).onClick(ctx -> {
            i18n("help_header", player).includePrefix().build().sendMessage();
            i18n("help_unlocked", player).build().sendMessage();
            i18n("help_locked", player).build().sendMessage();
            i18n("help_current", player).build().sendMessage();
            i18n("help_navigation", player).build().sendMessage();
        });
    }

    private void renderBorder(@NotNull RenderContext render) {
        render.layoutSlot('X', UnifiedBuilderFactory
            .item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(Component.text(""))
            .build()
        );
    }

    private void showEvolutionDetails(@NotNull Player player, @NotNull EvolutionInfo evolution,
                                      boolean isUnlocked, boolean isCurrent, @NotNull OneblockIsland island) {
        i18n("evolution.details.header", player)
            .withPlaceholder("evolution", evolution.name())
            .includePrefix()
            .build().sendMessage();

        i18n("evolution.details.level", player)
            .withPlaceholder("level", evolution.minLevel())
            .build().sendMessage();

        i18n("evolution.details.blocks", player)
            .withPlaceholder("blocks", formatNumber(evolution.requiredBlocks()))
            .build().sendMessage();

        i18n("evolution.details.description", player)
            .withPlaceholder("description", evolution.description())
            .build().sendMessage();

        if (isCurrent) {
            i18n("evolution.details.current_status", player).build().sendMessage();
        } else if (isUnlocked) {
            i18n("evolution.details.unlocked_status", player).build().sendMessage();
        } else {
            var remaining = evolution.requiredBlocks() - island.getTotalBlocksBroken();
            i18n("evolution.details.locked_status", player)
                .withPlaceholder("remaining", formatNumber(remaining))
                .build().sendMessage();
        }

        var bonuses = getEvolutionBonuses(evolution.name());
        if (!bonuses.isEmpty()) {
            i18n("evolution.details.bonuses", player).build().sendMessage();
            for (var bonus : bonuses) {
                i18n("evolution.details.bonus_item", player)
                    .withPlaceholder("bonus", bonus)
                    .build().sendMessage();
            }
        }
    }

    private boolean isEvolutionUnlocked(@NotNull OneblockIsland island, @NotNull EvolutionInfo evolution) {
        return island.getTotalBlocksBroken() >= evolution.requiredBlocks();
    }

    private List<String> getEvolutionBonuses(@NotNull String evolutionName) {
        try {
            var bonusManager = new BonusManager(EvolutionFactory.getInstance());
            
            var dummyIsland = new OneblockIsland();
            var dummyCore = new OneblockCore();
            dummyCore.setCurrentEvolution(evolutionName);
            dummyCore.setEvolutionLevel(1);
            dummyIsland.setOneblock(dummyCore);
            
            var bonuses = bonusManager.getActiveBonuses(dummyIsland);
            var bonusDescriptions = new ArrayList<String>();
            
            for (var bonus : bonuses) {
                bonusDescriptions.add(bonus.getFormattedDescription());
            }
            
            return bonusDescriptions;
            
        } catch (Exception e) {
            System.err.println("Failed to get bonuses for evolution " + evolutionName + ": " + e.getMessage());
        }
        
        return new ArrayList<>();
    }

    private String formatNumber(long number) {
        if (number >= 1_000_000_000) return String.format("%.1fB", number / 1_000_000_000.0);
        if (number >= 1_000_000) return String.format("%.1fM", number / 1_000_000.0);
        if (number >= 1_000) return String.format("%.1fK", number / 1_000.0);
        return String.valueOf(number);
    }

    public record EvolutionInfo(
        String name,
        Material material,
        int minLevel,
        long requiredBlocks,
        String description
    ) {}
}
