package de.jexcellence.oneblock.view.island;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class IslandMainView extends BaseView {
    
    private final State<JExOneblock> plugin = initialState("plugin");
    private final State<OneblockIsland> island = initialState("island");
    
    @Override
    protected String getKey() {
        return "island_main_ui";
    }
    
    @Override
    protected String[] getLayout() {
        return new String[]{
            "XXXXXXXXX",
            "X I E M X",
            "X S V B X",
            "X U O C X",
            "XXXXXXXXX"
        };
    }
    
    @Override
    protected int getSize() {
        return 5;
    }
    
    @Override
    protected Map<String, Object> getTitlePlaceholders(@NotNull OpenContext open) {
        var islandData = island.get(open);
        return Map.of(
            "name", islandData.getIslandName(),
            "level", islandData.getLevel(),
            "evolution", islandData.getCurrentEvolution()
        );
    }
    
    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        var islandData = island.get(render);
        var pluginInstance = plugin.get(render);
        
        // Island Info
        render.layoutSlot('I', UnifiedBuilderFactory
            .item(Material.PLAYER_HEAD)
            .setName(i18n("island.info.name", player)
                .withPlaceholder("name", islandData.getIslandName())
                .withPlaceholder("owner", islandData.getOwner().getPlayerName())
                .build().component())
            .setLore(i18n("island.info.lore", player)
                .withPlaceholder("level", islandData.getLevel())
                .withPlaceholder("experience", String.format("%.1f", islandData.getExperience()))
                .withPlaceholder("coins", formatNumber(islandData.getIslandCoins()))
                .withPlaceholder("size", islandData.getCurrentSize() + "x" + islandData.getCurrentSize())
                .withPlaceholder("created", islandData.getCreatedAt().toLocalDate().toString())
                .build().children())
            .build()
        ).onClick(ctx -> {
            i18n("island.info.detailed.header", player).includePrefix().build().sendMessage();
            i18n("island.info.detailed.name", player)
                .withPlaceholder("name", islandData.getIslandName())
                .build().sendMessage();
            i18n("island.info.detailed.owner", player)
                .withPlaceholder("owner", islandData.getOwner().getPlayerName())
                .build().sendMessage();
            i18n("island.info.detailed.level", player)
                .withPlaceholder("level", islandData.getLevel())
                .build().sendMessage();
            i18n("island.info.detailed.experience", player)
                .withPlaceholder("experience", String.format("%.1f", islandData.getExperience()))
                .build().sendMessage();
            i18n("island.info.detailed.coins", player)
                .withPlaceholder("coins", formatNumber(islandData.getIslandCoins()))
                .build().sendMessage();
            i18n("island.info.detailed.size", player)
                .withPlaceholder("size", islandData.getCurrentSize() + "x" + islandData.getCurrentSize())
                .build().sendMessage();
            i18n("island.info.detailed.created", player)
                .withPlaceholder("created", islandData.getCreatedAt().toLocalDate().toString())
                .build().sendMessage();
        });
        
        // Evolution Progress
        render.layoutSlot('E', UnifiedBuilderFactory
            .item(getEvolutionMaterial(islandData.getCurrentEvolution()))
            .setName(i18n("island.evolution.name", player)
                .withPlaceholder("evolution", islandData.getCurrentEvolution())
                .withPlaceholder("level", islandData.getOneblock() != null ? islandData.getOneblock().getEvolutionLevel() : 1)
                .build().component())
            .setLore(i18n("island.evolution.lore", player)
                .withPlaceholder("blocks_broken", formatNumber(islandData.getTotalBlocksBroken()))
                .withPlaceholder("prestige", islandData.getOneblock() != null ? islandData.getOneblock().getPrestigeLevel() : 0)
                .withPlaceholder("experience", String.format("%.1f", islandData.getOneblock() != null ? islandData.getOneblock().getEvolutionExperience() : 0.0))
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> ctx.openForPlayer(EvolutionBrowserView.class, ctx.getInitialData()));
        
        // Members
        render.layoutSlot('M', UnifiedBuilderFactory
            .item(Material.PLAYER_HEAD)
            .setName(i18n("island.members.name", player)
                .withPlaceholder("count", islandData.getMemberCount())
                .withPlaceholder("max", getMaxMembers(islandData))
                .build().component())
            .setLore(i18n("island.members.lore", player)
                .withPlaceholder("online", getOnlineMemberCount(islandData))
                .withPlaceholder("banned", islandData.getBannedCount())
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> ctx.openForPlayer(MembersListView.class, ctx.getInitialData()));
        
        // Settings
        render.layoutSlot('S', UnifiedBuilderFactory
            .item(Material.COMPARATOR)
            .setName(i18n("island.settings.name", player).build().component())
            .setLore(i18n("island.settings.lore", player)
                .withPlaceholder("privacy", islandData.isPrivacy() ? "Private" : "Public")
                .withPlaceholder("description", islandData.getDescription() != null ? islandData.getDescription() : "No description")
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            var memberSvc = plugin.get(ctx).getIslandMemberService();
            var ownerPlayer = pluginInstance.getOneblockPlayerRepository().findByUuid(player.getUniqueId());
            if (ownerPlayer != null) {
                var trustLevel = memberSvc.getTrustLevel(islandData, ownerPlayer);
                if (trustLevel.isAtLeast(de.jexcellence.oneblock.manager.permission.TrustLevel.CO_OWNER)) {
                        ctx.openForPlayer(IslandSettingsView.class, ctx.getInitialData());
                } else {
                    i18n("island.error.no_permission_settings", player).includePrefix().build().sendMessage();
                }
            }
        });
        
        // Visitor Settings
        render.layoutSlot('V', UnifiedBuilderFactory
            .item(Material.IRON_DOOR)
            .setName(i18n("island.visitors.name", player).build().component())
            .setLore(i18n("island.visitors.lore", player)
                .withPlaceholder("can_visit", islandData.getVisitorSettings() != null ? 
                    (islandData.getVisitorSettings().isCanVisit() ? "Allowed" : "Denied") : "Allowed")
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            ctx.openForPlayer(VisitorSettingsView.class, ctx.getInitialData());
        });
        
        // Banned Players
        render.layoutSlot('B', UnifiedBuilderFactory
            .item(Material.BARRIER)
            .setName(i18n("island.banned.name", player)
                .withPlaceholder("count", islandData.getBannedCount())
                .build().component())
            .setLore(i18n("island.banned.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            ctx.openForPlayer(BannedPlayersView.class, ctx.getInitialData());
        });
        
        // Upgrades
        render.layoutSlot('U', UnifiedBuilderFactory
            .item(Material.ANVIL)
            .setName(i18n("island.upgrades.name", player).build().component())
            .setLore(i18n("island.upgrades.lore", player)
                .withPlaceholder("size_level", calculateSizeLevel(islandData.getCurrentSize()))
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            i18n("island.upgrades.not_implemented", player).includePrefix().build().sendMessage();
        });
        
        // OneBlock Core
        render.layoutSlot('O', UnifiedBuilderFactory
            .item(Material.BEDROCK)
            .setName(i18n("island.oneblock.name", player).build().component())
            .setLore(i18n("island.oneblock.lore", player)
                .withPlaceholder("evolution", islandData.getCurrentEvolution())
                .withPlaceholder("blocks", formatNumber(islandData.getTotalBlocksBroken()))
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> ctx.openForPlayer(OneblockCoreView.class, ctx.getInitialData()));
        
        // Close Button
        render.layoutSlot('C', UnifiedBuilderFactory
            .item(Material.BARRIER)
            .setName(i18n("common.close", player).build().component())
            .build()
        ).onClick(ctx -> ctx.getPlayer().closeInventory());
        
        // Border
        render.layoutSlot('X', UnifiedBuilderFactory
            .item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(Component.text(""))
            .build()
        );
    }
    
    private Material getEvolutionMaterial(String evolution) {
        return switch (evolution.toLowerCase()) {
            case "stone" -> Material.STONE;
            case "coal" -> Material.COAL_ORE;
            case "iron" -> Material.IRON_ORE;
            case "gold" -> Material.GOLD_ORE;
            case "diamond" -> Material.DIAMOND_ORE;
            case "emerald" -> Material.EMERALD_ORE;
            case "nether" -> Material.NETHERRACK;
            case "end" -> Material.END_STONE;
            default -> Material.GRASS_BLOCK;
        };
    }
    
    private int getMaxMembers(OneblockIsland island) {
        return 5 + calculateMemberSlotUpgrades(island);
    }
    
    private int getOnlineMemberCount(OneblockIsland island) {
        var onlineCount = 0;
        
        if (org.bukkit.Bukkit.getPlayer(island.getOwner().getUniqueId()) != null) {
            onlineCount++;
        }
        
        for (var member : island.getMembers()) {
            if (org.bukkit.Bukkit.getPlayer(member.getUniqueId()) != null) {
                onlineCount++;
            }
        }
        
        return onlineCount;
    }
    
    private int calculateSizeLevel(int currentSize) {
        return Math.max(0, (currentSize - 50) / 25);
    }
    
    private int calculateMemberSlotUpgrades(OneblockIsland island) {
        var plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("JExOneblock");
        if (plugin instanceof de.jexcellence.oneblock.JExOneblock jexPlugin) {
            var upgradeService = new de.jexcellence.oneblock.service.UpgradeService();
            return upgradeService.getCurrentLevel(island, de.jexcellence.oneblock.service.UpgradeService.UpgradeType.MEMBER_SLOTS);
        }
        return 0;
    }
    
    private String formatNumber(long number) {
        if (number >= 1_000_000_000) return String.format("%.1fB", number / 1_000_000_000.0);
        if (number >= 1_000_000) return String.format("%.1fM", number / 1_000_000.0);
        if (number >= 1_000) return String.format("%.1fK", number / 1_000.0);
        return String.valueOf(number);
    }
}
