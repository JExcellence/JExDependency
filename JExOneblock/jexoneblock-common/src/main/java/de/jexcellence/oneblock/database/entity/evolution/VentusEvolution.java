package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Ventus Evolution - Air and Basic Sky Materials
 * Focus on air-related blocks and early flight concepts
 * Stage 5 of 50 - Tier 1: Genesis
 */
public class VentusEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Ventus", 5, 500)
            .showcase(Material.FEATHER)
            .description("The breath of freedom, where air brings movement and the promise of flight")
            
            // Requirements: Fire materials from Ignis
            .requireCurrency(400)
            .requireItem(Material.TORCH, 16)
            .requireItem(Material.COAL, 24)
            
            // Common blocks - Light materials
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.GLASS, Material.GLASS_PANE, Material.WHITE_WOOL,
                Material.LIGHT_GRAY_WOOL, Material.SCAFFOLDING)

            // Uncommon blocks - Sky materials
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.WHITE_STAINED_GLASS, Material.LIGHT_GRAY_STAINED_GLASS,
                Material.WHITE_STAINED_GLASS_PANE, Material.COBWEB, Material.STRING)

            // Rare blocks - Flight-related
            .addBlocks(EEvolutionRarityType.RARE,
                Material.HAY_BLOCK, Material.DRIED_KELP_BLOCK, Material.BAMBOO,
                Material.LADDER, Material.VINE)

            // Epic blocks - Advanced air
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.SLIME_BLOCK, Material.HONEY_BLOCK, Material.TARGET,
                Material.END_ROD, Material.LIGHTNING_ROD)

            // Legendary blocks - Ultimate air power
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.BEACON, Material.CONDUIT)
            
            // Flying entities
            .addEntity(EEvolutionRarityType.RARE, Material.CHICKEN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.BAT_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.PARROT_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.EPIC, Material.BEE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.VEX_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.PHANTOM_SPAWN_EGG)

            // Air-themed items
            .addItem(EEvolutionRarityType.COMMON, Material.FEATHER, 3)
            .addItem(EEvolutionRarityType.COMMON, Material.STRING, 4)
            .addItem(EEvolutionRarityType.COMMON, Material.PAPER, 2)
            
            .addItem(EEvolutionRarityType.UNCOMMON, Material.ARROW, 8)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.BOW, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.LEAD, 2)
            
            .addItem(EEvolutionRarityType.RARE, Material.CROSSBOW, 1)
            .addItem(EEvolutionRarityType.RARE, Material.FIREWORK_ROCKET, 3)
            .addItem(EEvolutionRarityType.RARE, Material.SLIME_BALL, 2)
            
            .addItem(EEvolutionRarityType.EPIC, Material.HONEY_BOTTLE, 2)
            .addItem(EEvolutionRarityType.EPIC, Material.PHANTOM_MEMBRANE, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.GUNPOWDER, 3)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.ELYTRA, 1)

            .build();
    }
}


