package de.jexcellence.oneblock.view.island;

import com.raindropcentral.rplatform.utility.heads.view.Previous;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class OneblockCoreView extends BaseView {
    
    private final State<JExOneblock> plugin = initialState("plugin");
    private final State<OneblockIsland> island = initialState("island");
    
    @Override
    protected String getKey() {
        return "oneblock_core_ui";
    }
    
    @Override
    protected String[] getLayout() {
        return new String[]{
            "XXXXXXXXX",
            "X E L P X",
            "X D S M X",
            "X C R B X",
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
            "evolution", islandData.getCurrentEvolution(),
            "level", islandData.getOneblock() != null ? islandData.getOneblock().getEvolutionLevel() : 1
        );
    }
    
    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        var islandData = island.get(render);
        var oneblock = islandData.getOneblock();
        
        render.layoutSlot('E', UnifiedBuilderFactory
            .item(getEvolutionMaterial(islandData.getCurrentEvolution()))
            .setName(i18n("oneblock.evolution.name", player)
                .withPlaceholder("evolution", islandData.getCurrentEvolution())
                .withPlaceholder("level", oneblock != null ? oneblock.getEvolutionLevel() : 1)
                .build().component())
            .setLore(i18n("oneblock.evolution.lore", player)
                .withPlaceholder("experience", String.format("%.1f", oneblock != null ? oneblock.getEvolutionExperience() : 0.0))
                .withPlaceholder("required", getRequiredExperience(islandData))
                .withPlaceholder("progress", String.format("%.1f%%", getEvolutionProgress(islandData) * 100))
                .build().children())
            .setGlowing(true)
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> ctx.openForPlayer(EvolutionBrowserView.class, ctx.getInitialData()));
        
        render.layoutSlot('L', UnifiedBuilderFactory
            .item(Material.EXPERIENCE_BOTTLE)
            .setName(i18n("oneblock.level.name", player)
                .withPlaceholder("level", islandData.getLevel())
                .build().component())
            .setLore(i18n("oneblock.level.lore", player)
                .withPlaceholder("experience", String.format("%.1f", islandData.getExperience()))
                .withPlaceholder("next_level", islandData.getLevel() + 1)
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            i18n("oneblock.level.details.header", player).includePrefix().build().sendMessage();
            i18n("oneblock.level.details.current", player)
                .withPlaceholder("level", islandData.getLevel())
                .build().sendMessage();
            i18n("oneblock.level.details.experience", player)
                .withPlaceholder("experience", String.format("%.1f", islandData.getExperience()))
                .build().sendMessage();
            i18n("oneblock.level.details.next", player)
                .withPlaceholder("required", getRequiredLevelExperience(islandData.getLevel()))
                .build().sendMessage();
        });

        render.layoutSlot('P', UnifiedBuilderFactory
            .item(Material.NETHER_STAR)
            .setName(i18n("oneblock.prestige.name", player)
                .withPlaceholder("level", oneblock != null ? oneblock.getPrestigeLevel() : 0)
                .build().component())
            .setLore(i18n("oneblock.prestige.lore", player)
                .withPlaceholder("points", oneblock != null ? oneblock.getPrestigePoints() : 0)
                .withPlaceholder("multiplier", String.format("%.1fx", getPrestigeMultiplier(islandData)))
                .build().children())
            .setGlowing(oneblock != null && oneblock.getPrestigeLevel() > 0)
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            //TODO confirm
        });
        
        render.layoutSlot('D', UnifiedBuilderFactory
            .item(Material.DIAMOND)
            .setName(i18n("oneblock.drops.name", player).build().component())
            .setLore(i18n("oneblock.drops.lore", player)
                .withPlaceholder("common", getDropRate(EEvolutionRarityType.COMMON))
                .withPlaceholder("uncommon", getDropRate(EEvolutionRarityType.UNCOMMON))
                .withPlaceholder("rare", getDropRate(EEvolutionRarityType.RARE))
                .withPlaceholder("epic", getDropRate(EEvolutionRarityType.EPIC))
                .withPlaceholder("legendary", getDropRate(EEvolutionRarityType.LEGENDARY))
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            i18n("oneblock.drops.details.header", player).includePrefix().build().sendMessage();
            for (var rarity : EEvolutionRarityType.values()) {
                i18n("oneblock.drops.details.rarity", player)
                    .withPlaceholder("rarity", rarity.name())
                    .withPlaceholder("rate", getDropRate(rarity))
                    .build().sendMessage();
            }
        });
        
        render.layoutSlot('S', UnifiedBuilderFactory
            .item(Material.PAPER)
            .setName(i18n("oneblock.stats.name", player).build().component())
            .setLore(i18n("oneblock.stats.lore", player)
                .withPlaceholder("blocks_broken", formatNumber(islandData.getTotalBlocksBroken()))
                .withPlaceholder("time_played", getTimePlayed(islandData))
                .withPlaceholder("blocks_per_hour", getBlocksPerHour(islandData))
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            i18n("oneblock.stats.details.header", player).includePrefix().build().sendMessage();
            i18n("oneblock.stats.details.total_blocks", player)
                .withPlaceholder("blocks", formatNumber(islandData.getTotalBlocksBroken()))
                .build().sendMessage();
            i18n("oneblock.stats.details.time_played", player)
                .withPlaceholder("time", getTimePlayed(islandData))
                .build().sendMessage();
            i18n("oneblock.stats.details.efficiency", player)
                .withPlaceholder("rate", getBlocksPerHour(islandData))
                .build().sendMessage();
        });
        
        render.layoutSlot('M', UnifiedBuilderFactory
            .item(Material.CHICKEN_SPAWN_EGG)
            .setName(i18n("oneblock.mobs.name", player).build().component())
            .setLore(i18n("oneblock.mobs.lore", player)
                .withPlaceholder("evolution", islandData.getCurrentEvolution())
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            i18n("oneblock.mobs.details.header", player)
                .withPlaceholder("evolution", islandData.getCurrentEvolution())
                .includePrefix()
                .build().sendMessage();
            
            var mobChances = getMobSpawnChances(islandData.getCurrentEvolution());
            for (var entry : mobChances.entrySet()) {
                i18n("oneblock.mobs.details.mob", player)
                    .withPlaceholder("mob", entry.getKey())
                    .withPlaceholder("chance", String.format("%.2f%%", entry.getValue()))
                    .build().sendMessage();
            }
        });
        
        render.layoutSlot('R', UnifiedBuilderFactory
            .item(Material.CHEST)
            .setName(i18n("oneblock.rewards.name", player).build().component())
            .setLore(i18n("oneblock.rewards.lore", player)
                .withPlaceholder("multiplier", String.format("%.1fx", getRewardMultiplier(islandData)))
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            i18n("oneblock.rewards.details.header", player).includePrefix().build().sendMessage();
            i18n("oneblock.rewards.details.multiplier", player)
                .withPlaceholder("multiplier", String.format("%.1fx", getRewardMultiplier(islandData)))
                .build().sendMessage();
            i18n("oneblock.rewards.details.prestige_bonus", player)
                .withPlaceholder("bonus", String.format("%.1fx", getPrestigeMultiplier(islandData)))
                .build().sendMessage();
        });
        
        render.layoutSlot('B', new Previous().getHead(player)
        ).onClick(ctx -> ctx.openForPlayer(IslandMainView.class, ctx.getInitialData()));
        
        render.layoutSlot('C', UnifiedBuilderFactory
            .item(Material.BARRIER)
            .setName(i18n("common.close", player).build().component())
            .build()
        ).onClick(ctx -> ctx.getPlayer().closeInventory());
        
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
            case "copper" -> Material.COPPER_ORE;
            case "iron" -> Material.IRON_ORE;
            case "gold" -> Material.GOLD_ORE;
            case "diamond" -> Material.DIAMOND_ORE;
            case "emerald" -> Material.EMERALD_ORE;
            case "nether" -> Material.NETHERRACK;
            case "end" -> Material.END_STONE;
            case "cosmic" -> Material.BEACON;
            case "infinity" -> Material.NETHER_STAR;
            default -> Material.GRASS_BLOCK;
        };
    }
    
    private double getEvolutionProgress(OneblockIsland island) {
        var oneblock = island.getOneblock();
        if (oneblock == null) return 0.0;
        
        var currentExp = oneblock.getEvolutionExperience();
        var requiredExp = getRawRequiredExperience(island);
        
        return requiredExp > 0 ? Math.min(currentExp / requiredExp, 1.0) : 1.0;
    }
    
    private String getRequiredExperience(OneblockIsland island) {
        var oneblock = island.getOneblock();
        if (oneblock == null) return "1000";
        
        var level = oneblock.getEvolutionLevel();
        var required = level * 1000L + (level * level * 100L);
        
        return formatNumber(required);
    }
    
    private long getRawRequiredExperience(OneblockIsland island) {
        var oneblock = island.getOneblock();
        if (oneblock == null) return 1000L;
        
        var level = oneblock.getEvolutionLevel();
        return level * 1000L + (level * level * 100L);
    }
    
    private String getRequiredLevelExperience(int currentLevel) {
        var required = currentLevel * 500L + (currentLevel * currentLevel * 50L);
        return formatNumber(required);
    }
    
    private double getPrestigeMultiplier(OneblockIsland island) {
        var oneblock = island.getOneblock();
        if (oneblock == null) return 1.0;
        
        return 1.0 + (oneblock.getPrestigeLevel() * 0.5);
    }
    
    private double getRewardMultiplier(OneblockIsland island) {
        var oneblock = island.getOneblock();
        if (oneblock == null) return 1.0;
        
        var evolutionBonus = 1.0 + (oneblock.getEvolutionLevel() * 0.1);
        var prestigeBonus = getPrestigeMultiplier(island);
        
        return evolutionBonus * prestigeBonus;
    }
    
    private String getDropRate(EEvolutionRarityType rarity) {
        return switch (rarity) {
            case COMMON -> "65%";
            case UNCOMMON -> "25%";
            case RARE -> "8%";
            case EPIC -> "1.8%";
            case LEGENDARY -> "0.2%";
            case SPECIAL -> "0.1%";
            case UNIQUE -> "0.05%";
            case MYTHICAL -> "0.02%";
            case DIVINE -> "0.01%";
            case CELESTIAL -> "0.005%";
            case TRANSCENDENT -> "0.002%";
            case ETHEREAL -> "0.001%";
            case COSMIC -> "0.0005%";
            case INFINITE -> "0.0002%";
            case OMNIPOTENT -> "0.0001%";
            case RESERVED -> "0.00005%";
        };
    }
    
    private String getTimePlayed(OneblockIsland island) {
        var created = island.getCreatedAt();
        var now = java.time.LocalDateTime.now();
        var duration = java.time.Duration.between(created, now);
        
        var days = duration.toDays();
        var hours = duration.toHours() % 24;
        var minutes = duration.toMinutes() % 60;
        
        if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
    
    private String getBlocksPerHour(OneblockIsland island) {
        var created = island.getCreatedAt();
        var now = java.time.LocalDateTime.now();
        var hoursPlayed = java.time.Duration.between(created, now).toHours();
        
        if (hoursPlayed > 0) {
            var rate = (double) island.getTotalBlocksBroken() / hoursPlayed;
            return formatNumber((long) rate);
        }
        
        return "0";
    }
    
    private Map<String, Double> getMobSpawnChances(String evolution) {
        return switch (evolution.toLowerCase()) {
            case "stone" -> Map.of(
                "Pig", 15.0, "Cow", 15.0, "Chicken", 10.0, "Sheep", 10.0
            );
            case "coal" -> Map.of(
                "Pig", 12.0, "Cow", 12.0, "Chicken", 8.0, "Sheep", 8.0, "Spider", 5.0
            );
            case "iron" -> Map.of(
                "Pig", 10.0, "Cow", 10.0, "Zombie", 8.0, "Skeleton", 6.0, "Creeper", 4.0
            );
            case "gold" -> Map.of(
                "Villager", 12.0, "Iron Golem", 3.0, "Zombie", 6.0, "Skeleton", 6.0
            );
            case "diamond" -> Map.of(
                "Villager", 10.0, "Iron Golem", 5.0, "Witch", 4.0, "Enderman", 2.0
            );
            case "nether" -> Map.of(
                "Blaze", 8.0, "Wither Skeleton", 5.0, "Ghast", 3.0, "Piglin", 10.0
            );
            case "end" -> Map.of(
                "Enderman", 15.0, "Shulker", 5.0, "Ender Dragon", 0.1
            );
            default -> Map.of("No mobs", 0.0);
        };
    }
    
    private String formatNumber(long number) {
        if (number >= 1_000_000_000) return String.format("%.1fB", number / 1_000_000_000.0);
        if (number >= 1_000_000) return String.format("%.1fM", number / 1_000_000.0);
        if (number >= 1_000) return String.format("%.1fK", number / 1_000.0);
        return String.valueOf(number);
    }
}
