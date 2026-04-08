package de.jexcellence.oneblock.bonus;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.factory.EvolutionFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages evolution bonuses for islands.
 * Calculates and applies bonuses based on current evolution and level.
 * 
 * @author JExcellence
 * @since 2.0.0
 */
public class BonusManager {
    
    private final EvolutionFactory evolutionFactory;
    private final Map<String, List<Bonus>> cachedBonuses = new ConcurrentHashMap<>();
    
    public BonusManager(@NotNull EvolutionFactory evolutionFactory) {
        this.evolutionFactory = evolutionFactory;
    }

    /**
     * Gets all active bonuses for an island.
     * @param island the island
     * @return list of active bonuses
     */
    @NotNull
    public List<Bonus> getActiveBonuses(@NotNull OneblockIsland island) {
        String evolutionName = island.getCurrentEvolution();
        int evolutionLevel = island.getOneblock() != null ? island.getOneblock().getEvolutionLevel() : 1;
        
        String cacheKey = evolutionName + ":" + evolutionLevel;
        
        return cachedBonuses.computeIfAbsent(cacheKey, k -> calculateBonuses(evolutionName, evolutionLevel));
    }

    /**
     * Gets a specific bonus type for an island.
     * @param island the island
     * @param bonusType the bonus type to get
     * @return the bonus, or null if not found
     */
    @Nullable
    public Bonus getBonus(@NotNull OneblockIsland island, @NotNull Bonus.Type bonusType) {
        return getActiveBonuses(island).stream()
            .filter(bonus -> bonus.getType() == bonusType)
            .findFirst()
            .orElse(null);
    }

    /**
     * Gets the total multiplier for a specific bonus type.
     * @param island the island
     * @param bonusType the bonus type
     * @return the total multiplier (1.0 = no bonus)
     */
    public double getBonusMultiplier(@NotNull OneblockIsland island, @NotNull Bonus.Type bonusType) {
        return getActiveBonuses(island).stream()
            .filter(bonus -> bonus.getType() == bonusType)
            .filter(Bonus::isActive)
            .mapToDouble(Bonus::getValue)
            .reduce(1.0, (a, b) -> a * b); // Multiply bonuses together
    }

    /**
     * Gets the total additive bonus for a specific bonus type.
     * @param island the island
     * @param bonusType the bonus type
     * @return the total additive bonus (0.0 = no bonus)
     */
    public double getBonusAdditive(@NotNull OneblockIsland island, @NotNull Bonus.Type bonusType) {
        return getActiveBonuses(island).stream()
            .filter(bonus -> bonus.getType() == bonusType)
            .filter(Bonus::isActive)
            .mapToDouble(Bonus::getValue)
            .sum();
    }

    /**
     * Checks if an island has a specific bonus type.
     * @param island the island
     * @param bonusType the bonus type to check
     * @return true if the island has this bonus type
     */
    public boolean hasBonus(@NotNull OneblockIsland island, @NotNull Bonus.Type bonusType) {
        return getBonus(island, bonusType) != null;
    }

    /**
     * Gets all bonus types that an island currently has.
     * @param island the island
     * @return set of bonus types
     */
    @NotNull
    public Set<Bonus.Type> getBonusTypes(@NotNull OneblockIsland island) {
        return getActiveBonuses(island).stream()
            .map(Bonus::getType)
            .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Clears the bonus cache for a specific evolution.
     * @param evolutionName the evolution name
     */
    public void clearCache(@NotNull String evolutionName) {
        cachedBonuses.entrySet().removeIf(entry -> entry.getKey().startsWith(evolutionName + ":"));
    }

    /**
     * Clears all cached bonuses.
     */
    public void clearAllCache() {
        cachedBonuses.clear();
    }

    /**
     * Calculates bonuses for a specific evolution and level.
     * @param evolutionName the evolution name
     * @param evolutionLevel the evolution level
     * @return list of bonuses
     */
    @NotNull
    private List<Bonus> calculateBonuses(@NotNull String evolutionName, int evolutionLevel) {
        List<Bonus> bonuses = new ArrayList<>();
        
        // Get evolution from factory
        OneblockEvolution evolution = evolutionFactory.getCachedEvolution(evolutionName);
        if (evolution == null) {
            return bonuses; // Return empty list if evolution not found
        }

        // Add evolution-specific bonuses based on evolution name and level
        addEvolutionBonuses(bonuses, evolutionName, evolutionLevel);
        
        // Add level-based bonuses
        addLevelBonuses(bonuses, evolutionLevel);
        
        return bonuses;
    }

    /**
     * Adds evolution-specific bonuses.
     * @param bonuses the bonus list to add to
     * @param evolutionName the evolution name
     * @param evolutionLevel the evolution level
     */
    private void addEvolutionBonuses(@NotNull List<Bonus> bonuses, @NotNull String evolutionName, int evolutionLevel) {
        switch (evolutionName.toLowerCase()) {
            case "genesis" -> {
                // Genesis: Basic bonuses
                bonuses.add(new DropRateBonus(1.0 + (evolutionLevel * 0.01))); // 1% per level
            }
            case "terra" -> {
                // Terra: Farming bonuses
                bonuses.add(new FarmingBonus(1.0 + (evolutionLevel * 0.05))); // 5% per level
                bonuses.add(new BlockBreakSpeedBonus(1.0 + (evolutionLevel * 0.02))); // 2% per level
            }
            case "aqua" -> {
                // Aqua: Fishing and water bonuses
                bonuses.add(new FishingBonus(1.0 + (evolutionLevel * 0.04))); // 4% per level
                bonuses.add(new WaterBreathingBonus(true, -1L)); // Permanent water breathing
            }
            case "ignis" -> {
                // Ignis: Fire and smelting bonuses
                bonuses.add(new SmeltingBonus(1.0 + (evolutionLevel * 0.03))); // 3% per level
                bonuses.add(new FireResistanceBonus(0.5 + (evolutionLevel * 0.1))); // Fire resistance
            }
            case "ventus" -> {
                // Ventus: Speed and flight bonuses
                bonuses.add(new SpeedBonus(evolutionLevel * 0.1)); // Speed increase
                if (evolutionLevel >= 10) {
                    bonuses.add(new FlightBonus(true, -1L)); // Flight at level 10+
                }
            }
            case "stone" -> {
                // Stone: Tool durability and mining
                bonuses.add(new ToolDurabilityBonus(1.0 + (evolutionLevel * 0.15))); // 15% per level
                bonuses.add(new BlockBreakSpeedBonus(1.0 + (evolutionLevel * 0.05))); // 5% per level
            }
            case "copper" -> {
                // Copper: Tool efficiency and durability
                bonuses.add(new ToolDurabilityBonus(1.0 + (evolutionLevel * 0.20))); // 20% per level
                bonuses.add(new EnergyBonus(1.0 + (evolutionLevel * 0.03))); // 3% energy per level
            }
            case "iron" -> {
                // Iron: Armor and defense
                bonuses.add(new ArmorProtectionBonus(1.0 + (evolutionLevel * 0.10))); // 10% per level
                bonuses.add(new DefenseBonus(evolutionLevel * 0.5)); // Defense increase
            }
            case "gold" -> {
                // Gold: Experience and trading
                bonuses.add(new ExperienceBonus(1.0 + (evolutionLevel * 0.25))); // 25% per level
                bonuses.add(new TradingBonus(1.0 + (evolutionLevel * 0.15))); // 15% per level
            }
            case "diamond" -> {
                // Diamond: Rare drops and luck
                bonuses.add(new RareDropBonus(1.0 + (evolutionLevel * 0.30))); // 30% per level
                bonuses.add(new LuckBonus(evolutionLevel * 1.0)); // Luck increase
            }
            case "emerald" -> {
                // Emerald: Trading and enchanting
                bonuses.add(new TradingBonus(1.0 + (evolutionLevel * 0.35))); // 35% per level
                bonuses.add(new EnchantingBonus(1.0 + (evolutionLevel * 0.20))); // 20% per level
            }
            case "nether" -> {
                // Nether: Fire resistance and nether bonuses
                bonuses.add(new FireResistanceBonus(1.0)); // Full fire resistance
                bonuses.add(new DamageBonus(evolutionLevel * 1.0)); // Damage increase
            }
            case "end" -> {
                // End: Teleportation and void protection
                bonuses.add(new TeleportationBonus(1.0 + (evolutionLevel * 0.25))); // 25% per level
                bonuses.add(new NightVisionBonus(true, -1L)); // Permanent night vision
            }
            case "cosmic" -> {
                // Cosmic: All stats bonus
                bonuses.add(new AllStatsBonus(1.0 + (evolutionLevel * 0.50))); // 50% per level
                bonuses.add(new FlightBonus(true, -1L)); // Permanent flight
            }
            case "infinity" -> {
                // Infinity: Ultimate bonuses
                bonuses.add(new UltimateBonus(1.0 + (evolutionLevel * 1.00))); // 100% per level
                bonuses.add(new AllStatsBonus(2.0)); // Double all stats
            }
        }
    }

    /**
     * Adds level-based bonuses that apply to all evolutions.
     * @param bonuses the bonus list to add to
     * @param evolutionLevel the evolution level
     */
    private void addLevelBonuses(@NotNull List<Bonus> bonuses, int evolutionLevel) {
        // Every 5 levels: small experience bonus
        if (evolutionLevel % 5 == 0) {
            bonuses.add(new ExperienceBonus(1.0 + (evolutionLevel / 5 * 0.05))); // 5% per 5 levels
        }
        
        // Every 10 levels: health bonus
        if (evolutionLevel % 10 == 0) {
            bonuses.add(new HealthBonus(evolutionLevel / 10 * 2.0)); // +2 health per 10 levels
        }
        
        // Every 25 levels: regeneration bonus
        if (evolutionLevel % 25 == 0) {
            bonuses.add(new RegenerationBonus(evolutionLevel / 25 * 0.1)); // Regeneration increase
        }
    }
}