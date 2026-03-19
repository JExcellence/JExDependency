package de.jexcellence.oneblock.view.island;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.database.entity.evolution.EvolutionEntity;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.factory.EvolutionFactory;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import lombok.Getter;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class EvolutionDetailView extends APaginatedView<EvolutionDetailView.DetailEntry> {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    private final State<JExOneblock> plugin = initialState("plugin");
    private final State<String> evolutionName = initialState("evolutionName");
    private final MutableState<ViewMode> viewMode = mutableState(ViewMode.BLOCKS);
    
    public EvolutionDetailView() {
        super(EvolutionBrowserView.class);
    }
    
    @Override
    protected String getKey() {
        return "evolution_detail_ui";
    }
    
    @Override
    protected String[] getLayout() {
        return new String[]{
            "  BIMCE  ",
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            "         ",
            "b  <p>   ",
        };
    }
    
    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(@NotNull OpenContext openContext) {
        String evolution = evolutionName.get(openContext);
        ViewMode mode = viewMode.get(openContext);
        
        return Map.of(
            "evolution", evolution != null ? evolution : "Unknown",
            "mode", mode != null ? mode.getDisplayName() : "Blocks"
        );
    }
    
    @Override
    protected CompletableFuture<List<DetailEntry>> getAsyncPaginationSource(@NotNull Context context) {
        return CompletableFuture.supplyAsync(() -> {
            String evolution = evolutionName.get(context);
            ViewMode mode = viewMode.get(context);
            
            if (evolution == null || mode == null) {
                return new ArrayList<>();
            }
            
            try {
                var evolutionFactory = EvolutionFactory.getInstance();
                var evolutionData = evolutionFactory.getCachedEvolution(evolution);
                
                if (evolutionData == null) {
                    LOGGER.warning("Evolution not found: " + evolution);
                    return new ArrayList<>();
                }
                
                return switch (mode) {
                    case BLOCKS -> loadBlocks(evolutionData);
                    case ITEMS -> loadItems(evolutionData);
                    case MOBS -> loadMobs(evolutionData);
                    case CHESTS -> loadChests(evolutionData);
                    case EFFECTS -> loadEffects(evolutionData);
                };
                
            } catch (Exception e) {
                LOGGER.severe("Error loading evolution details: " + e.getMessage());
                return new ArrayList<>();
            }
        });
    }
    
    @Override
    protected void renderEntry(@NotNull Context context, @NotNull BukkitItemComponentBuilder builder, int index, @NotNull DetailEntry entry) {
        var player = context.getPlayer();
        
        builder.withItem(UnifiedBuilderFactory
            .item(entry.material())
            .setName(i18n("entry.name", player)
                .withPlaceholder("name", entry.name())
                .withPlaceholder("rarity", entry.rarity().getFormattedName())
                .build().component())
            .setLore(i18n("entry.lore", player)
                .withPlaceholders(Map.of(
                    "rarity", entry.rarity().getFormattedName(),
                    "type", entry.type(),
                    "description", entry.description(),
                    "chance", String.format("%.2f%%", entry.chance())
                ))
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build());
    }
    
    @Override
    protected void onPaginatedRender(@NotNull RenderContext render, @NotNull Player player) {
        renderModeButtons(render, player);
    }
    
    private void renderModeButtons(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('B', UnifiedBuilderFactory
            .item(Material.COBBLESTONE)
            .setName(i18n("mode.blocks", player).build().component())
            .setLore(i18n("mode.blocks_lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .setGlowing(viewMode.get(render) == ViewMode.BLOCKS)
            .build()
        ).onClick(ctx -> {
            viewMode.set(ViewMode.BLOCKS, ctx);
            ctx.update();
        });
        
        render.layoutSlot('I', UnifiedBuilderFactory
            .item(Material.DIAMOND)
            .setName(i18n("mode.items", player).build().component())
            .setLore(i18n("mode.items_lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .setGlowing(viewMode.get(render) == ViewMode.ITEMS)
            .build()
        ).onClick(ctx -> {
            viewMode.set(ViewMode.ITEMS, ctx);
            ctx.update();
        });
        
        render.layoutSlot('M', UnifiedBuilderFactory
            .item(Material.ZOMBIE_HEAD)
            .setName(i18n("mode.mobs", player).build().component())
            .setLore(i18n("mode.mobs_lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .setGlowing(viewMode.get(render) == ViewMode.MOBS)
            .build()
        ).onClick(ctx -> {
            viewMode.set(ViewMode.MOBS, ctx);
            ctx.update();
        });
        
        render.layoutSlot('C', UnifiedBuilderFactory
            .item(Material.CHEST)
            .setName(i18n("mode.chests", player).build().component())
            .setLore(i18n("mode.chests_lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .setGlowing(viewMode.get(render) == ViewMode.CHESTS)
            .build()
        ).onClick(ctx -> {
            viewMode.set(ViewMode.CHESTS, ctx);
            ctx.update();
        });
        
        render.layoutSlot('E', UnifiedBuilderFactory
            .item(Material.ENCHANTED_BOOK)
            .setName(i18n("mode.effects", player).build().component())
            .setLore(i18n("mode.effects_lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .setGlowing(viewMode.get(render) == ViewMode.EFFECTS)
            .build()
        ).onClick(ctx -> {
            viewMode.set(ViewMode.EFFECTS, ctx);
            ctx.update();
        });
    }
    
    private List<DetailEntry> loadBlocks(@NotNull OneblockEvolution evolution) {
        var entries = new ArrayList<DetailEntry>();
        
        for (var blockConfig : evolution.getBlocks()) {
            if (!blockConfig.isValid()) continue;
            
            var materials = blockConfig.getMaterials();
            if (materials == null || materials.isEmpty()) continue;
            
            var rarity = blockConfig.getRarity();
            double chance = blockConfig.getWeight();
            
            for (var material : materials) {
                entries.add(new DetailEntry(
                    material,
                    formatMaterialName(material),
                    rarity,
                    "Block",
                    "Spawns when breaking the OneBlock",
                    chance
                ));
            }
        }
        
        entries.sort((a, b) -> Integer.compare(b.rarity().getTier(), a.rarity().getTier()));
        
        return entries;
    }
    
    private List<DetailEntry> loadItems(@NotNull OneblockEvolution evolution) {
        var entries = new ArrayList<DetailEntry>();
        
        for (var itemConfig : evolution.getItems()) {
            if (!itemConfig.isValid()) continue;
            
            var itemStacks = itemConfig.getItemStacks();
            if (itemStacks == null || itemStacks.isEmpty()) continue;
            
            var rarity = itemConfig.getRarity();
            double chance = itemConfig.getWeight();
            
            for (var itemStack : itemStacks) {
                entries.add(new DetailEntry(
                    itemStack.getType(),
                    formatMaterialName(itemStack.getType()),
                    rarity,
                    "Item Drop",
                    "Special item that can drop",
                    chance
                ));
            }
        }
        
        entries.sort((a, b) -> Integer.compare(b.rarity().getTier(), a.rarity().getTier()));
        
        return entries;
    }
    
    private List<DetailEntry> loadMobs(@NotNull OneblockEvolution evolution) {
        var entries = new ArrayList<DetailEntry>();
        
        for (EvolutionEntity mobConfig : evolution.getEntities()) {
            if (!mobConfig.isValid()) continue;
            
            var spawnEggs = mobConfig.getSpawnEggs();
            if (spawnEggs == null || spawnEggs.isEmpty()) continue;
            
            var rarity = mobConfig.getRarity();
            double chance = mobConfig.getSpawnChance();
            
            // Process each spawn egg in this entity configuration
            for (Material spawnEgg : spawnEggs) {
                // Convert spawn egg material to entity type for display name
                EntityType entityType = getEntityTypeFromSpawnEgg(spawnEgg);
                String entityName = entityType != null ? 
                    formatEntityName(entityType) : 
                    formatMaterialName(spawnEgg);
                
                entries.add(new DetailEntry(
                    spawnEgg, // Use the actual spawn egg material
                    entityName,
                    rarity,
                    "Mob Spawn",
                    "Can spawn when breaking blocks",
                    chance
                ));
            }
        }
        
        entries.sort((a, b) -> Integer.compare(b.rarity().getTier(), a.rarity().getTier()));
        
        return entries;
    }
    
    private List<DetailEntry> loadChests(@NotNull OneblockEvolution evolution) {
        var entries = new ArrayList<DetailEntry>();
        
        for (var chestConfig : evolution.getItems()) {
            if (!chestConfig.isValid()) continue;
            
            var lootItems = chestConfig.getItemStacks();
            if (lootItems == null || lootItems.isEmpty()) continue;
            
            var rarity = chestConfig.getRarity();
            double chance = chestConfig.getDropChance();
            
            for (var lootItem : lootItems) {
                entries.add(new DetailEntry(
                    lootItem.getType(),
                    formatMaterialName(lootItem.getType()),
                    rarity,
                    "Chest Loot",
                    "Found in treasure chests",
                    chance
                ));
            }
        }
        
        entries.sort((a, b) -> Integer.compare(b.rarity().getTier(), a.rarity().getTier()));
        
        return entries;
    }
    
    private List<DetailEntry> loadEffects(@NotNull OneblockEvolution evolution) {
        var entries = new ArrayList<DetailEntry>();
        
        entries.add(new DetailEntry(
            Material.EXPERIENCE_BOTTLE,
            "XP Multiplier",
            EEvolutionRarityType.SPECIAL,
            "Passive Effect",
            "Increases XP gained from breaking blocks",
            100.0
        ));
        
        entries.add(new DetailEntry(
            Material.GOLD_INGOT,
            "Drop Rate Bonus",
            EEvolutionRarityType.SPECIAL,
            "Passive Effect",
            "Increases rare drop chances",
            100.0
        ));
        
        entries.add(new DetailEntry(
            Material.NETHER_STAR,
            "Evolution Bonus",
            EEvolutionRarityType.UNIQUE,
            "Passive Effect",
            "Special bonuses for this evolution",
            100.0
        ));
        
        return entries;
    }
    
    /**
     * Gets the EntityType from a spawn egg material
     */
    private EntityType getEntityTypeFromSpawnEgg(Material spawnEgg) {
        return switch (spawnEgg) {
            case ZOMBIE_SPAWN_EGG -> EntityType.ZOMBIE;
            case SKELETON_SPAWN_EGG -> EntityType.SKELETON;
            case CREEPER_SPAWN_EGG -> EntityType.CREEPER;
            case SPIDER_SPAWN_EGG -> EntityType.SPIDER;
            case ENDERMAN_SPAWN_EGG -> EntityType.ENDERMAN;
            case BLAZE_SPAWN_EGG -> EntityType.BLAZE;
            case GHAST_SPAWN_EGG -> EntityType.GHAST;
            case WITHER_SKELETON_SPAWN_EGG -> EntityType.WITHER_SKELETON;
            case PIG_SPAWN_EGG -> EntityType.PIG;
            case COW_SPAWN_EGG -> EntityType.COW;
            case CHICKEN_SPAWN_EGG -> EntityType.CHICKEN;
            case SHEEP_SPAWN_EGG -> EntityType.SHEEP;
            case VILLAGER_SPAWN_EGG -> EntityType.VILLAGER;
            case BAT_SPAWN_EGG -> EntityType.BAT;
            case WOLF_SPAWN_EGG -> EntityType.WOLF;
            case CAT_SPAWN_EGG -> EntityType.CAT;
            case OCELOT_SPAWN_EGG -> EntityType.OCELOT;
            case HORSE_SPAWN_EGG -> EntityType.HORSE;
            case DONKEY_SPAWN_EGG -> EntityType.DONKEY;
            case MULE_SPAWN_EGG -> EntityType.MULE;
            case LLAMA_SPAWN_EGG -> EntityType.LLAMA;
            case PARROT_SPAWN_EGG -> EntityType.PARROT;
            case RABBIT_SPAWN_EGG -> EntityType.RABBIT;
            case POLAR_BEAR_SPAWN_EGG -> EntityType.POLAR_BEAR;
            case SQUID_SPAWN_EGG -> EntityType.SQUID;
            case DOLPHIN_SPAWN_EGG -> EntityType.DOLPHIN;
            case TURTLE_SPAWN_EGG -> EntityType.TURTLE;
            case COD_SPAWN_EGG -> EntityType.COD;
            case SALMON_SPAWN_EGG -> EntityType.SALMON;
            case PUFFERFISH_SPAWN_EGG -> EntityType.PUFFERFISH;
            case TROPICAL_FISH_SPAWN_EGG -> EntityType.TROPICAL_FISH;
            case DROWNED_SPAWN_EGG -> EntityType.DROWNED;
            case PHANTOM_SPAWN_EGG -> EntityType.PHANTOM;
            case PILLAGER_SPAWN_EGG -> EntityType.PILLAGER;
            case RAVAGER_SPAWN_EGG -> EntityType.RAVAGER;
            case VEX_SPAWN_EGG -> EntityType.VEX;
            case VINDICATOR_SPAWN_EGG -> EntityType.VINDICATOR;
            case EVOKER_SPAWN_EGG -> EntityType.EVOKER;
            case WITCH_SPAWN_EGG -> EntityType.WITCH;
            case GUARDIAN_SPAWN_EGG -> EntityType.GUARDIAN;
            case ELDER_GUARDIAN_SPAWN_EGG -> EntityType.ELDER_GUARDIAN;
            case SHULKER_SPAWN_EGG -> EntityType.SHULKER;
            case ENDERMITE_SPAWN_EGG -> EntityType.ENDERMITE;
            case SILVERFISH_SPAWN_EGG -> EntityType.SILVERFISH;
            case CAVE_SPIDER_SPAWN_EGG -> EntityType.CAVE_SPIDER;
            case SLIME_SPAWN_EGG -> EntityType.SLIME;
            case MAGMA_CUBE_SPAWN_EGG -> EntityType.MAGMA_CUBE;
            case ZOMBIFIED_PIGLIN_SPAWN_EGG -> EntityType.ZOMBIFIED_PIGLIN;
            case PIGLIN_SPAWN_EGG -> EntityType.PIGLIN;
            case PIGLIN_BRUTE_SPAWN_EGG -> EntityType.PIGLIN_BRUTE;
            case HOGLIN_SPAWN_EGG -> EntityType.HOGLIN;
            case ZOGLIN_SPAWN_EGG -> EntityType.ZOGLIN;
            case STRIDER_SPAWN_EGG -> EntityType.STRIDER;
            default -> null; // Unknown spawn egg
        };
    }

    /**
     * Gets the appropriate spawn egg material for an entity type
     */
    private Material getSpawnEggForEntity(EntityType entityType) {
        return switch (entityType) {
            case ZOMBIE -> Material.ZOMBIE_SPAWN_EGG;
            case SKELETON -> Material.SKELETON_SPAWN_EGG;
            case CREEPER -> Material.CREEPER_SPAWN_EGG;
            case SPIDER -> Material.SPIDER_SPAWN_EGG;
            case ENDERMAN -> Material.ENDERMAN_SPAWN_EGG;
            case BLAZE -> Material.BLAZE_SPAWN_EGG;
            case GHAST -> Material.GHAST_SPAWN_EGG;
            case WITHER_SKELETON -> Material.WITHER_SKELETON_SPAWN_EGG;
            case PIG -> Material.PIG_SPAWN_EGG;
            case COW -> Material.COW_SPAWN_EGG;
            case CHICKEN -> Material.CHICKEN_SPAWN_EGG;
            case SHEEP -> Material.SHEEP_SPAWN_EGG;
            case VILLAGER -> Material.VILLAGER_SPAWN_EGG;
            case IRON_GOLEM -> Material.IRON_INGOT; // No spawn egg for iron golem
            case SNOW_GOLEM -> Material.SNOWBALL; // No spawn egg for snow golem
            case ENDER_DRAGON -> Material.DRAGON_EGG;
            case WITHER -> Material.WITHER_SKELETON_SKULL;
            case BAT -> Material.BAT_SPAWN_EGG;
            case WOLF -> Material.WOLF_SPAWN_EGG;
            case CAT -> Material.CAT_SPAWN_EGG;
            case OCELOT -> Material.OCELOT_SPAWN_EGG;
            case HORSE -> Material.HORSE_SPAWN_EGG;
            case DONKEY -> Material.DONKEY_SPAWN_EGG;
            case MULE -> Material.MULE_SPAWN_EGG;
            case LLAMA -> Material.LLAMA_SPAWN_EGG;
            case PARROT -> Material.PARROT_SPAWN_EGG;
            case RABBIT -> Material.RABBIT_SPAWN_EGG;
            case POLAR_BEAR -> Material.POLAR_BEAR_SPAWN_EGG;
            case SQUID -> Material.SQUID_SPAWN_EGG;
            case DOLPHIN -> Material.DOLPHIN_SPAWN_EGG;
            case TURTLE -> Material.TURTLE_SPAWN_EGG;
            case COD -> Material.COD_SPAWN_EGG;
            case SALMON -> Material.SALMON_SPAWN_EGG;
            case PUFFERFISH -> Material.PUFFERFISH_SPAWN_EGG;
            case TROPICAL_FISH -> Material.TROPICAL_FISH_SPAWN_EGG;
            case DROWNED -> Material.DROWNED_SPAWN_EGG;
            case PHANTOM -> Material.PHANTOM_SPAWN_EGG;
            case PILLAGER -> Material.PILLAGER_SPAWN_EGG;
            case RAVAGER -> Material.RAVAGER_SPAWN_EGG;
            case VEX -> Material.VEX_SPAWN_EGG;
            case VINDICATOR -> Material.VINDICATOR_SPAWN_EGG;
            case EVOKER -> Material.EVOKER_SPAWN_EGG;
            case WITCH -> Material.WITCH_SPAWN_EGG;
            case GUARDIAN -> Material.GUARDIAN_SPAWN_EGG;
            case ELDER_GUARDIAN -> Material.ELDER_GUARDIAN_SPAWN_EGG;
            case SHULKER -> Material.SHULKER_SPAWN_EGG;
            case ENDERMITE -> Material.ENDERMITE_SPAWN_EGG;
            case SILVERFISH -> Material.SILVERFISH_SPAWN_EGG;
            case CAVE_SPIDER -> Material.CAVE_SPIDER_SPAWN_EGG;
            case SLIME -> Material.SLIME_SPAWN_EGG;
            case MAGMA_CUBE -> Material.MAGMA_CUBE_SPAWN_EGG;
            case ZOMBIFIED_PIGLIN -> Material.ZOMBIFIED_PIGLIN_SPAWN_EGG;
            case PIGLIN -> Material.PIGLIN_SPAWN_EGG;
            case PIGLIN_BRUTE -> Material.PIGLIN_BRUTE_SPAWN_EGG;
            case HOGLIN -> Material.HOGLIN_SPAWN_EGG;
            case ZOGLIN -> Material.ZOGLIN_SPAWN_EGG;
            case STRIDER -> Material.STRIDER_SPAWN_EGG;
            default -> Material.EGG; // Fallback for unknown entities
        };
    }
    
    /**
     * Gets a representative material for mobs that don't have spawn eggs
     */
    private Material getMaterialForMob(EntityType entityType) {
        return switch (entityType) {
            case ZOMBIE -> Material.ZOMBIE_HEAD;
            case SKELETON -> Material.SKELETON_SKULL;
            case CREEPER -> Material.CREEPER_HEAD;
            case SPIDER -> Material.SPIDER_EYE;
            case ENDERMAN -> Material.ENDER_PEARL;
            case BLAZE -> Material.BLAZE_ROD;
            case GHAST -> Material.GHAST_TEAR;
            case WITHER_SKELETON -> Material.WITHER_SKELETON_SKULL;
            case PIG -> Material.PORKCHOP;
            case COW -> Material.BEEF;
            case CHICKEN -> Material.CHICKEN;
            case SHEEP -> Material.WHITE_WOOL;
            case VILLAGER -> Material.EMERALD;
            default -> Material.EGG;
        };
    }
    
    private String formatMaterialName(Material material) {
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
    
    private String formatEntityName(EntityType entityType) {
        String name = entityType.name().toLowerCase().replace('_', ' ');
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
    
    @Getter
    public enum ViewMode {
        BLOCKS("Blocks"),
        ITEMS("Items"),
        MOBS("Mobs"),
        CHESTS("Chests"),
        EFFECTS("Effects");
        
        private final String displayName;
        
        ViewMode(String displayName) {
            this.displayName = displayName;
        }

    }
    
    public record DetailEntry(
        Material material,
        String name,
        EEvolutionRarityType rarity,
        String type,
        String description,
        double chance
    ) {}
}