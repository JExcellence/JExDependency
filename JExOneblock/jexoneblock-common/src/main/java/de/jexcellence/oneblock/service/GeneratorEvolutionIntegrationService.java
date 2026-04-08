package de.jexcellence.oneblock.service;

import de.jexcellence.oneblock.database.entity.generator.EGeneratorDesignType;
import de.jexcellence.oneblock.database.entity.generator.GeneratorDesign;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service that integrates the generator structure system with the evolution system.
 * <p>
 * Handles:
 * - Generator unlock checks based on evolution progression
 * - Generator rewards at evolution milestones
 * - Updating evolution views to show generator unlocks
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class GeneratorEvolutionIntegrationService {

    private static final Logger LOGGER = Logger.getLogger("JExOneblock");

    private final GeneratorStructureManager structureManager;
    private final GeneratorDesignRegistry designRegistry;

    // Cache for evolution-to-generator mappings
    private final Map<Integer, List<EGeneratorDesignType>> evolutionUnlocks = new ConcurrentHashMap<>();
    
    // Cache for milestone rewards
    private final Map<Integer, GeneratorMilestoneReward> milestoneRewards = new ConcurrentHashMap<>();

    private boolean initialized = false;

    public GeneratorEvolutionIntegrationService(
            @NotNull GeneratorStructureManager structureManager,
            @NotNull GeneratorDesignRegistry designRegistry
    ) {
        this.structureManager = structureManager;
        this.designRegistry = designRegistry;
    }

    /**
     * Initializes the integration service.
     */
    public void initialize() {
        if (initialized) return;

        LOGGER.info("Initializing Generator-Evolution Integration Service...");

        // Set up default evolution unlock mappings
        setupDefaultEvolutionUnlocks();
        
        // Set up milestone rewards
        setupMilestoneRewards();

        initialized = true;
        LOGGER.info("Generator-Evolution Integration Service initialized");
    }

    /**
     * Sets up default evolution level to generator unlock mappings.
     */
    private void setupDefaultEvolutionUnlocks() {
        // Tier 1: Foundry - Available from start (evolution level 1)
        evolutionUnlocks.put(1, List.of(EGeneratorDesignType.FOUNDRY));
        
        // Tier 2: Aquatic - Evolution level 5
        evolutionUnlocks.put(5, List.of(EGeneratorDesignType.AQUATIC));
        
        // Tier 3: Volcanic - Evolution level 10
        evolutionUnlocks.put(10, List.of(EGeneratorDesignType.VOLCANIC));
        
        // Tier 4: Crystal - Evolution level 15
        evolutionUnlocks.put(15, List.of(EGeneratorDesignType.CRYSTAL));
        
        // Tier 5: Mechanical - Evolution level 20
        evolutionUnlocks.put(20, List.of(EGeneratorDesignType.MECHANICAL));
        
        // Tier 6: Nature - Evolution level 25
        evolutionUnlocks.put(25, List.of(EGeneratorDesignType.NATURE));
        
        // Tier 7: Nether - Evolution level 30
        evolutionUnlocks.put(30, List.of(EGeneratorDesignType.NETHER));
        
        // Tier 8: End - Evolution level 35
        evolutionUnlocks.put(35, List.of(EGeneratorDesignType.END));
        
        // Tier 9: Ancient - Evolution level 40
        evolutionUnlocks.put(40, List.of(EGeneratorDesignType.ANCIENT));
        
        // Tier 10: Celestial - Evolution level 50
        evolutionUnlocks.put(50, List.of(EGeneratorDesignType.CELESTIAL));
    }

    /**
     * Sets up milestone rewards for evolution progression.
     */
    private void setupMilestoneRewards() {
        // Every 10 evolution levels, grant a speed bonus
        milestoneRewards.put(10, new GeneratorMilestoneReward(
                "Speed Boost I", 
                "All generators gain +5% speed",
                MilestoneRewardType.SPEED_BONUS, 
                0.05
        ));
        
        milestoneRewards.put(20, new GeneratorMilestoneReward(
                "XP Boost I",
                "All generators gain +10% XP",
                MilestoneRewardType.XP_BONUS,
                0.10
        ));
        
        milestoneRewards.put(30, new GeneratorMilestoneReward(
                "Speed Boost II",
                "All generators gain +10% speed",
                MilestoneRewardType.SPEED_BONUS,
                0.10
        ));
        
        milestoneRewards.put(40, new GeneratorMilestoneReward(
                "Fortune Boost I",
                "All generators gain +5% fortune",
                MilestoneRewardType.FORTUNE_BONUS,
                0.05
        ));
        
        milestoneRewards.put(50, new GeneratorMilestoneReward(
                "Master Generator",
                "All generators gain +15% to all stats",
                MilestoneRewardType.ALL_BONUS,
                0.15
        ));
    }

    /**
     * Checks if a player can unlock a generator design based on their evolution level.
     *
     * @param player the player
     * @param evolutionLevel the player's current evolution level
     * @param design the generator design to check
     * @return true if the player meets the evolution requirement
     */
    public boolean canUnlockByEvolution(@NotNull Player player, int evolutionLevel, @NotNull GeneratorDesign design) {
        int requiredLevel = getRequiredEvolutionLevel(design.getDesignType());
        return evolutionLevel >= requiredLevel;
    }

    /**
     * Gets the required evolution level for a generator design type.
     *
     * @param type the generator design type
     * @return the required evolution level
     */
    public int getRequiredEvolutionLevel(@NotNull EGeneratorDesignType type) {
        for (Map.Entry<Integer, List<EGeneratorDesignType>> entry : evolutionUnlocks.entrySet()) {
            if (entry.getValue().contains(type)) {
                return entry.getKey();
            }
        }
        return 1; // Default to level 1 if not found
    }

    /**
     * Gets all generator designs unlocked at a specific evolution level.
     *
     * @param evolutionLevel the evolution level
     * @return list of unlocked generator design types
     */
    @NotNull
    public List<EGeneratorDesignType> getUnlocksAtLevel(int evolutionLevel) {
        return evolutionUnlocks.getOrDefault(evolutionLevel, Collections.emptyList());
    }

    /**
     * Gets all generator designs available up to a specific evolution level.
     *
     * @param evolutionLevel the evolution level
     * @return list of all available generator design types
     */
    @NotNull
    public List<EGeneratorDesignType> getAvailableDesignsForLevel(int evolutionLevel) {
        List<EGeneratorDesignType> available = new ArrayList<>();
        
        for (Map.Entry<Integer, List<EGeneratorDesignType>> entry : evolutionUnlocks.entrySet()) {
            if (entry.getKey() <= evolutionLevel) {
                available.addAll(entry.getValue());
            }
        }
        
        return available;
    }

    /**
     * Gets the milestone reward for a specific evolution level.
     *
     * @param evolutionLevel the evolution level
     * @return the milestone reward, or null if none
     */
    @Nullable
    public GeneratorMilestoneReward getMilestoneReward(int evolutionLevel) {
        return milestoneRewards.get(evolutionLevel);
    }

    /**
     * Gets all milestone rewards up to a specific evolution level.
     *
     * @param evolutionLevel the evolution level
     * @return map of level to milestone rewards
     */
    @NotNull
    public Map<Integer, GeneratorMilestoneReward> getMilestoneRewardsUpTo(int evolutionLevel) {
        Map<Integer, GeneratorMilestoneReward> rewards = new LinkedHashMap<>();
        
        for (Map.Entry<Integer, GeneratorMilestoneReward> entry : milestoneRewards.entrySet()) {
            if (entry.getKey() <= evolutionLevel) {
                rewards.put(entry.getKey(), entry.getValue());
            }
        }
        
        return rewards;
    }

    /**
     * Calculates the total bonus multiplier for a player based on their evolution level.
     *
     * @param evolutionLevel the player's evolution level
     * @param bonusType the type of bonus to calculate
     * @return the total multiplier (1.0 = no bonus)
     */
    public double calculateTotalBonus(int evolutionLevel, @NotNull MilestoneRewardType bonusType) {
        double totalBonus = 1.0;
        
        for (Map.Entry<Integer, GeneratorMilestoneReward> entry : milestoneRewards.entrySet()) {
            if (entry.getKey() <= evolutionLevel) {
                GeneratorMilestoneReward reward = entry.getValue();
                if (reward.type() == bonusType || reward.type() == MilestoneRewardType.ALL_BONUS) {
                    totalBonus += reward.value();
                }
            }
        }
        
        return totalBonus;
    }

    /**
     * Called when a player advances to a new evolution level.
     * Handles unlocking new generators and granting milestone rewards.
     *
     * @param player the player
     * @param island the player's island
     * @param newLevel the new evolution level
     * @return future containing the unlock result
     */
    @NotNull
    public CompletableFuture<EvolutionAdvanceResult> onEvolutionAdvance(
            @NotNull Player player,
            @NotNull OneblockIsland island,
            int newLevel
    ) {
        return CompletableFuture.supplyAsync(() -> {
            List<EGeneratorDesignType> newUnlocks = getUnlocksAtLevel(newLevel);
            GeneratorMilestoneReward milestone = getMilestoneReward(newLevel);
            
            List<String> unlockedDesigns = new ArrayList<>();
            
            // Process new unlocks
            for (EGeneratorDesignType type : newUnlocks) {
                GeneratorDesign design = designRegistry.getDesign(type);
                if (design != null) {
                    unlockedDesigns.add(design.getNameKey());
                }
            }
            
            return new EvolutionAdvanceResult(
                    newLevel,
                    unlockedDesigns,
                    milestone
            );
        }).exceptionally(ex -> {
            LOGGER.log(Level.WARNING, "Error processing evolution advance", ex);
            return new EvolutionAdvanceResult(newLevel, Collections.emptyList(), null);
        });
    }

    /**
     * Gets generator unlock information for display in evolution views.
     *
     * @param evolutionLevel the evolution level to display
     * @return list of generator unlock info
     */
    @NotNull
    public List<GeneratorUnlockInfo> getUnlockInfoForEvolutionView(int evolutionLevel) {
        List<GeneratorUnlockInfo> unlockInfo = new ArrayList<>();
        
        for (Map.Entry<Integer, List<EGeneratorDesignType>> entry : evolutionUnlocks.entrySet()) {
            int level = entry.getKey();
            boolean unlocked = evolutionLevel >= level;
            
            for (EGeneratorDesignType type : entry.getValue()) {
                GeneratorDesign design = designRegistry.getDesign(type);
                if (design != null) {
                    unlockInfo.add(new GeneratorUnlockInfo(
                            design.getDesignKey(),
                            design.getNameKey(),
                            design.getTier(),
                            level,
                            unlocked
                    ));
                }
            }
        }
        
        // Sort by required level
        unlockInfo.sort(Comparator.comparingInt(GeneratorUnlockInfo::requiredLevel));
        
        return unlockInfo;
    }

    /**
     * Registers a custom evolution unlock mapping.
     *
     * @param evolutionLevel the evolution level
     * @param designType the generator design type to unlock
     */
    public void registerEvolutionUnlock(int evolutionLevel, @NotNull EGeneratorDesignType designType) {
        evolutionUnlocks.computeIfAbsent(evolutionLevel, k -> new ArrayList<>()).add(designType);
    }

    /**
     * Registers a custom milestone reward.
     *
     * @param evolutionLevel the evolution level
     * @param reward the milestone reward
     */
    public void registerMilestoneReward(int evolutionLevel, @NotNull GeneratorMilestoneReward reward) {
        milestoneRewards.put(evolutionLevel, reward);
    }

    public boolean isInitialized() {
        return initialized;
    }

    // ==================== Inner Classes ====================

    /**
     * Types of milestone rewards.
     */
    public enum MilestoneRewardType {
        SPEED_BONUS,
        XP_BONUS,
        FORTUNE_BONUS,
        ALL_BONUS
    }

    /**
     * Represents a milestone reward for evolution progression.
     */
    public record GeneratorMilestoneReward(
            @NotNull String name,
            @NotNull String description,
            @NotNull MilestoneRewardType type,
            double value
    ) {}

    /**
     * Result of an evolution advance event.
     */
    public record EvolutionAdvanceResult(
            int newLevel,
            @NotNull List<String> unlockedDesigns,
            @Nullable GeneratorMilestoneReward milestone
    ) {
        public boolean hasNewUnlocks() {
            return !unlockedDesigns.isEmpty();
        }

        public boolean hasMilestone() {
            return milestone != null;
        }
    }

    /**
     * Information about a generator unlock for display.
     */
    public record GeneratorUnlockInfo(
            @NotNull String designKey,
            @NotNull String nameKey,
            int tier,
            int requiredLevel,
            boolean unlocked
    ) {}
}
