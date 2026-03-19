package de.jexcellence.oneblock.factory;

import de.jexcellence.oneblock.database.entity.generator.*;
import de.jexcellence.oneblock.requirement.generator.*;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating default generator designs.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class GeneratorDesignFactory {
    
    /**
     * Creates all default generator designs.
     *
     * @return list of all 10 default designs
     */
    @NotNull
    public static List<GeneratorDesign> createAllDefaultDesigns() {
        List<GeneratorDesign> designs = new ArrayList<>();
        
        designs.add(createFoundryDesign());
        designs.add(createAquaticDesign());
        designs.add(createVolcanicDesign());
        designs.add(createCrystalDesign());
        designs.add(createMechanicalDesign());
        designs.add(createNatureDesign());
        designs.add(createNetherDesign());
        designs.add(createEndDesign());
        designs.add(createAncientDesign());
        designs.add(createCelestialDesign());
        
        return designs;
    }
    
    /**
     * Creates the Foundry design (Tier 1).
     */
    @NotNull
    public static GeneratorDesign createFoundryDesign() {
        GeneratorDesign design = new GeneratorDesign("foundry", EGeneratorDesignType.FOUNDRY);
        design.setDifficulty(1);
        design.setSpeedMultiplier(1.2);
        design.setXpMultiplier(1.1);
        design.setParticleEffect("foundry_smoke");
        
        // Requirements
        design.addRequirement(createRequirement(new EvolutionLevelRequirement(5), 0));
        design.addRequirement(createRequirement(new BlocksBrokenRequirement(500), 1));
        
        // Rewards
        design.addReward(createReward(GeneratorDesignReward.RewardType.SPEED_BONUS, 0.2, 0));
        
        // Layers
        design.addLayer(createFoundryLayer(0, true, false));
        design.addLayer(createFoundryLayer(1, false, false));
        design.addLayer(createFoundryLayer(2, false, true));
        
        return design;
    }
    
    /**
     * Creates the Aquatic design (Tier 2).
     */
    @NotNull
    public static GeneratorDesign createAquaticDesign() {
        GeneratorDesign design = new GeneratorDesign("aquatic", EGeneratorDesignType.AQUATIC);
        design.setDifficulty(2);
        design.setSpeedMultiplier(1.4);
        design.setXpMultiplier(1.2);
        design.setParticleEffect("aquatic_bubbles");
        
        // Requirements
        design.addRequirement(createRequirement(new EvolutionLevelRequirement(10), 0));
        design.addRequirement(createRequirement(new BlocksBrokenRequirement(2000), 1));
        design.addRequirement(createRequirement(new GeneratorTierRequirement(EGeneratorDesignType.FOUNDRY), 2));
        
        // Rewards
        design.addReward(createReward(GeneratorDesignReward.RewardType.SPEED_BONUS, 0.4, 0));
        design.addReward(createReward(GeneratorDesignReward.RewardType.DROP_CHANCE, 0.05, 1));
        
        return design;
    }
    
    /**
     * Creates the Volcanic design (Tier 3).
     */
    @NotNull
    public static GeneratorDesign createVolcanicDesign() {
        GeneratorDesign design = new GeneratorDesign("volcanic", EGeneratorDesignType.VOLCANIC);
        design.setDifficulty(3);
        design.setSpeedMultiplier(1.6);
        design.setXpMultiplier(1.3);
        design.setParticleEffect("volcanic_flames");
        
        // Requirements
        design.addRequirement(createRequirement(new EvolutionLevelRequirement(15), 0));
        design.addRequirement(createRequirement(new BlocksBrokenRequirement(5000), 1));
        design.addRequirement(createRequirement(new GeneratorTierRequirement(EGeneratorDesignType.AQUATIC), 2));
        
        // Rewards
        design.addReward(createReward(GeneratorDesignReward.RewardType.SPEED_BONUS, 0.6, 0));
        design.addReward(createReward(GeneratorDesignReward.RewardType.AUTOMATION_UNLOCK, 1.0, 1));
        
        return design;
    }
    
    /**
     * Creates the Crystal design (Tier 4).
     */
    @NotNull
    public static GeneratorDesign createCrystalDesign() {
        GeneratorDesign design = new GeneratorDesign("crystal", EGeneratorDesignType.CRYSTAL);
        design.setDifficulty(4);
        design.setSpeedMultiplier(1.8);
        design.setXpMultiplier(1.5);
        design.setParticleEffect("crystal_sparkle");
        
        // Requirements
        design.addRequirement(createRequirement(new EvolutionLevelRequirement(20), 0));
        design.addRequirement(createRequirement(new BlocksBrokenRequirement(10000), 1));
        design.addRequirement(createRequirement(new GeneratorTierRequirement(EGeneratorDesignType.VOLCANIC), 2));
        
        // Rewards
        design.addReward(createReward(GeneratorDesignReward.RewardType.SPEED_BONUS, 0.8, 0));
        design.addReward(createReward(GeneratorDesignReward.RewardType.XP_BONUS, 0.5, 1));
        
        return design;
    }
    
    /**
     * Creates the Mechanical design (Tier 5).
     */
    @NotNull
    public static GeneratorDesign createMechanicalDesign() {
        GeneratorDesign design = new GeneratorDesign("mechanical", EGeneratorDesignType.MECHANICAL);
        design.setDifficulty(5);
        design.setSpeedMultiplier(2.0);
        design.setXpMultiplier(1.6);
        design.setParticleEffect("mechanical_gears");
        
        // Requirements
        design.addRequirement(createRequirement(new EvolutionLevelRequirement(30), 0));
        design.addRequirement(createRequirement(new BlocksBrokenRequirement(25000), 1));
        design.addRequirement(createRequirement(new GeneratorTierRequirement(EGeneratorDesignType.CRYSTAL), 2));
        
        // Rewards
        design.addReward(createReward(GeneratorDesignReward.RewardType.SPEED_BONUS, 1.0, 0));
        design.addReward(createReward(GeneratorDesignReward.RewardType.AUTOMATION_UNLOCK, 2.0, 1));
        
        return design;
    }
    
    /**
     * Creates the Nature design (Tier 6).
     */
    @NotNull
    public static GeneratorDesign createNatureDesign() {
        GeneratorDesign design = new GeneratorDesign("nature", EGeneratorDesignType.NATURE);
        design.setDifficulty(6);
        design.setSpeedMultiplier(2.2);
        design.setXpMultiplier(1.8);
        design.setParticleEffect("nature_leaves");
        
        // Requirements
        design.addRequirement(createRequirement(new EvolutionLevelRequirement(40), 0));
        design.addRequirement(createRequirement(new BlocksBrokenRequirement(50000), 1));
        design.addRequirement(createRequirement(new GeneratorTierRequirement(EGeneratorDesignType.MECHANICAL), 2));
        
        // Rewards
        design.addReward(createReward(GeneratorDesignReward.RewardType.SPEED_BONUS, 1.2, 0));
        design.addReward(createReward(GeneratorDesignReward.RewardType.DROP_CHANCE, 0.1, 1));
        
        return design;
    }
    
    /**
     * Creates the Nether design (Tier 7).
     */
    @NotNull
    public static GeneratorDesign createNetherDesign() {
        GeneratorDesign design = new GeneratorDesign("nether", EGeneratorDesignType.NETHER);
        design.setDifficulty(7);
        design.setSpeedMultiplier(2.5);
        design.setXpMultiplier(2.0);
        design.setParticleEffect("nether_flames");
        
        // Requirements
        design.addRequirement(createRequirement(new EvolutionLevelRequirement(50), 0));
        design.addRequirement(createRequirement(new BlocksBrokenRequirement(100000), 1));
        design.addRequirement(createRequirement(new PrestigeLevelRequirement(1), 2));
        design.addRequirement(createRequirement(new GeneratorTierRequirement(EGeneratorDesignType.NATURE), 3));
        
        // Rewards
        design.addReward(createReward(GeneratorDesignReward.RewardType.SPEED_BONUS, 1.5, 0));
        design.addReward(createReward(GeneratorDesignReward.RewardType.SPECIAL_DROP, 1.0, 1));
        
        return design;
    }
    
    /**
     * Creates the End design (Tier 8).
     */
    @NotNull
    public static GeneratorDesign createEndDesign() {
        GeneratorDesign design = new GeneratorDesign("end", EGeneratorDesignType.END);
        design.setDifficulty(8);
        design.setSpeedMultiplier(3.0);
        design.setXpMultiplier(2.5);
        design.setParticleEffect("end_particles");
        
        // Requirements
        design.addRequirement(createRequirement(new EvolutionLevelRequirement(75), 0));
        design.addRequirement(createRequirement(new BlocksBrokenRequirement(200000), 1));
        design.addRequirement(createRequirement(new PrestigeLevelRequirement(3), 2));
        design.addRequirement(createRequirement(new GeneratorTierRequirement(EGeneratorDesignType.NETHER), 3));
        
        // Rewards
        design.addReward(createReward(GeneratorDesignReward.RewardType.SPEED_BONUS, 2.0, 0));
        design.addReward(createReward(GeneratorDesignReward.RewardType.SPECIAL_DROP, 2.0, 1));
        
        return design;
    }
    
    /**
     * Creates the Ancient design (Tier 9).
     */
    @NotNull
    public static GeneratorDesign createAncientDesign() {
        GeneratorDesign design = new GeneratorDesign("ancient", EGeneratorDesignType.ANCIENT);
        design.setDifficulty(9);
        design.setSpeedMultiplier(4.0);
        design.setXpMultiplier(3.0);
        design.setParticleEffect("ancient_sculk");
        
        // Requirements
        design.addRequirement(createRequirement(new EvolutionLevelRequirement(100), 0));
        design.addRequirement(createRequirement(new BlocksBrokenRequirement(500000), 1));
        design.addRequirement(createRequirement(new PrestigeLevelRequirement(5), 2));
        design.addRequirement(createRequirement(new GeneratorTierRequirement(EGeneratorDesignType.END), 3));
        
        // Rewards
        design.addReward(createReward(GeneratorDesignReward.RewardType.SPEED_BONUS, 3.0, 0));
        design.addReward(createReward(GeneratorDesignReward.RewardType.FORTUNE_BONUS, 0.5, 1));
        
        return design;
    }
    
    /**
     * Creates the Celestial design (Tier 10).
     */
    @NotNull
    public static GeneratorDesign createCelestialDesign() {
        GeneratorDesign design = new GeneratorDesign("celestial", EGeneratorDesignType.CELESTIAL);
        design.setDifficulty(10);
        design.setSpeedMultiplier(5.0);
        design.setXpMultiplier(4.0);
        design.setFortuneBonus(1.0);
        design.setParticleEffect("celestial_stars");
        
        // Requirements
        design.addRequirement(createRequirement(new EvolutionLevelRequirement(150), 0));
        design.addRequirement(createRequirement(new BlocksBrokenRequirement(1000000), 1));
        design.addRequirement(createRequirement(new PrestigeLevelRequirement(10), 2));
        design.addRequirement(createRequirement(new GeneratorTierRequirement(EGeneratorDesignType.ANCIENT), 3));
        
        // Rewards
        design.addReward(createReward(GeneratorDesignReward.RewardType.SPEED_BONUS, 4.0, 0));
        design.addReward(createReward(GeneratorDesignReward.RewardType.XP_BONUS, 3.0, 1));
        design.addReward(createReward(GeneratorDesignReward.RewardType.FORTUNE_BONUS, 1.0, 2));
        
        return design;
    }
    
    // Helper methods
    
    private static GeneratorDesignRequirement createRequirement(
            com.raindropcentral.rplatform.requirement.AbstractRequirement requirement, 
            int displayOrder
    ) {
        GeneratorDesignRequirement req = new GeneratorDesignRequirement(requirement, displayOrder);
        return req;
    }
    
    private static GeneratorDesignReward createReward(
            GeneratorDesignReward.RewardType type, 
            double value, 
            int displayOrder
    ) {
        GeneratorDesignReward reward = new GeneratorDesignReward(type, value);
        reward.setDisplayOrder(displayOrder);
        return reward;
    }
    
    private static GeneratorDesignLayer createFoundryLayer(int index, boolean isFoundation, boolean isCoreLayer) {
        GeneratorDesignLayer layer = new GeneratorDesignLayer(index, 5, 5);
        layer.setIsFoundation(isFoundation);
        layer.setIsCoreLayer(isCoreLayer);
        layer.setNameKey("generator.design.foundry.layer." + index);
        
        // Simple pattern for foundry
        Material[][] pattern = layer.getPattern();
        Material primary = isFoundation ? Material.COBBLESTONE : Material.IRON_BLOCK;
        Material secondary = isCoreLayer ? Material.FURNACE : Material.HOPPER;
        
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                if (x == 0 || x == 4 || z == 0 || z == 4) {
                    pattern[x][z] = primary;
                } else if (x == 2 && z == 2) {
                    pattern[x][z] = isCoreLayer ? Material.AIR : secondary;
                } else {
                    pattern[x][z] = secondary;
                }
            }
        }
        
        if (isCoreLayer) {
            layer.setCoreOffsetX(2);
            layer.setCoreOffsetZ(2);
        }
        
        return layer;
    }
}
