package de.jexcellence.oneblock.bonus;

import org.jetbrains.annotations.NotNull;

// All remaining bonus classes in one file for brevity

/**
 * Tool durability bonus - increases tool durability.
 */
final class ToolDurabilityBonus extends AbstractBonus {
    public ToolDurabilityBonus(double multiplier) { super(Type.TOOL_DURABILITY, multiplier); }
    public ToolDurabilityBonus(double multiplier, boolean active, long duration) { super(Type.TOOL_DURABILITY, multiplier, active, duration); }
}

/**
 * Armor protection bonus - increases armor protection.
 */
final class ArmorProtectionBonus extends AbstractBonus {
    public ArmorProtectionBonus(double multiplier) { super(Type.ARMOR_PROTECTION, multiplier); }
    public ArmorProtectionBonus(double multiplier, boolean active, long duration) { super(Type.ARMOR_PROTECTION, multiplier, active, duration); }
}

/**
 * Fuel efficiency bonus - increases fuel burning time.
 */
final class FuelEfficiencyBonus extends AbstractBonus {
    public FuelEfficiencyBonus(double multiplier) { super(Type.FUEL_EFFICIENCY, multiplier); }
    public FuelEfficiencyBonus(double multiplier, boolean active, long duration) { super(Type.FUEL_EFFICIENCY, multiplier, active, duration); }
}

/**
 * Trading bonus - improves villager trade rates.
 */
final class TradingBonus extends AbstractBonus {
    public TradingBonus(double multiplier) { super(Type.TRADING, multiplier); }
    public TradingBonus(double multiplier, boolean active, long duration) { super(Type.TRADING, multiplier, active, duration); }
}

/**
 * Fire resistance bonus - provides fire damage resistance.
 */
final class FireResistanceBonus extends AbstractBonus {
    public FireResistanceBonus(double resistance) { super(Type.FIRE_RESISTANCE, resistance); }
    public FireResistanceBonus(double resistance, boolean active, long duration) { super(Type.FIRE_RESISTANCE, resistance, active, duration); }
}

/**
 * Teleportation bonus - improves teleportation abilities.
 */
final class TeleportationBonus extends AbstractBonus {
    public TeleportationBonus(double efficiency) { super(Type.TELEPORTATION, efficiency); }
    public TeleportationBonus(double efficiency, boolean active, long duration) { super(Type.TELEPORTATION, efficiency, active, duration); }
}

/**
 * All stats bonus - increases all bonus effects.
 */
final class AllStatsBonus extends AbstractBonus {
    public AllStatsBonus(double multiplier) { super(Type.ALL_STATS, multiplier); }
    public AllStatsBonus(double multiplier, boolean active, long duration) { super(Type.ALL_STATS, multiplier, active, duration); }
}

/**
 * Ultimate bonus - ultimate tier bonus with multiple effects.
 */
final class UltimateBonus extends AbstractBonus {
    public UltimateBonus(double multiplier) { super(Type.ULTIMATE, multiplier); }
    public UltimateBonus(double multiplier, boolean active, long duration) { super(Type.ULTIMATE, multiplier, active, duration); }
}

/**
 * Farming bonus - increases crop yield and growth speed.
 */
final class FarmingBonus extends AbstractBonus {
    public FarmingBonus(double multiplier) { super(Type.FARMING, multiplier); }
    public FarmingBonus(double multiplier, boolean active, long duration) { super(Type.FARMING, multiplier, active, duration); }
}

/**
 * Fishing bonus - improves fishing luck and speed.
 */
final class FishingBonus extends AbstractBonus {
    public FishingBonus(double multiplier) { super(Type.FISHING, multiplier); }
    public FishingBonus(double multiplier, boolean active, long duration) { super(Type.FISHING, multiplier, active, duration); }
}

/**
 * Smelting bonus - increases smelting speed and efficiency.
 */
final class SmeltingBonus extends AbstractBonus {
    public SmeltingBonus(double multiplier) { super(Type.SMELTING, multiplier); }
    public SmeltingBonus(double multiplier, boolean active, long duration) { super(Type.SMELTING, multiplier, active, duration); }
}

/**
 * Speed bonus - increases movement speed.
 */
final class SpeedBonus extends AbstractBonus {
    public SpeedBonus(double speedIncrease) { super(Type.SPEED, speedIncrease); }
    public SpeedBonus(double speedIncrease, boolean active, long duration) { super(Type.SPEED, speedIncrease, active, duration); }
    
    @Override
    public @NotNull String getFormattedDescription() {
        return String.format("Speed +%.1f", value);
    }
}

/**
 * Energy bonus - increases energy generation/capacity.
 */
final class EnergyBonus extends AbstractBonus {
    public EnergyBonus(double multiplier) { super(Type.ENERGY, multiplier); }
    public EnergyBonus(double multiplier, boolean active, long duration) { super(Type.ENERGY, multiplier, active, duration); }
}

/**
 * Probability bonus - increases chance-based events.
 */
final class ProbabilityBonus extends AbstractBonus {
    public ProbabilityBonus(double multiplier) { super(Type.PROBABILITY, multiplier); }
    public ProbabilityBonus(double multiplier, boolean active, long duration) { super(Type.PROBABILITY, multiplier, active, duration); }
}

/**
 * Rare drops bonus - increases rare item drop chances.
 */
final class RareDropBonus extends AbstractBonus {
    public RareDropBonus(double multiplier) { super(Type.RARE_DROPS, multiplier); }
    public RareDropBonus(double multiplier, boolean active, long duration) { super(Type.RARE_DROPS, multiplier, active, duration); }
}

/**
 * Enchanting bonus - improves enchanting results.
 */
final class EnchantingBonus extends AbstractBonus {
    public EnchantingBonus(double multiplier) { super(Type.ENCHANTING, multiplier); }
    public EnchantingBonus(double multiplier, boolean active, long duration) { super(Type.ENCHANTING, multiplier, active, duration); }
}

/**
 * Mob spawn bonus - affects mob spawn rates.
 */
final class MobSpawnBonus extends AbstractBonus {
    public MobSpawnBonus(double multiplier) { super(Type.MOB_SPAWN, multiplier); }
    public MobSpawnBonus(double multiplier, boolean active, long duration) { super(Type.MOB_SPAWN, multiplier, active, duration); }
}

/**
 * Block break speed bonus - increases block breaking speed.
 */
final class BlockBreakSpeedBonus extends AbstractBonus {
    public BlockBreakSpeedBonus(double multiplier) { super(Type.BLOCK_BREAK_SPEED, multiplier); }
    public BlockBreakSpeedBonus(double multiplier, boolean active, long duration) { super(Type.BLOCK_BREAK_SPEED, multiplier, active, duration); }
}

/**
 * Health bonus - increases maximum health.
 */
final class HealthBonus extends AbstractBonus {
    public HealthBonus(double healthIncrease) { super(Type.HEALTH, healthIncrease); }
    public HealthBonus(double healthIncrease, boolean active, long duration) { super(Type.HEALTH, healthIncrease, active, duration); }
    
    @Override
    public @NotNull String getFormattedDescription() {
        return String.format("Health +%.1f", value);
    }
}

/**
 * Mana bonus - increases mana capacity and regeneration.
 */
final class ManaBonus extends AbstractBonus {
    public ManaBonus(double manaIncrease) { super(Type.MANA, manaIncrease); }
    public ManaBonus(double manaIncrease, boolean active, long duration) { super(Type.MANA, manaIncrease, active, duration); }
}

/**
 * Luck bonus - increases general luck.
 */
final class LuckBonus extends AbstractBonus {
    public LuckBonus(double luckIncrease) { super(Type.LUCK, luckIncrease); }
    public LuckBonus(double luckIncrease, boolean active, long duration) { super(Type.LUCK, luckIncrease, active, duration); }
    
    @Override
    public @NotNull String getFormattedDescription() {
        return String.format("Luck +%.1f", value);
    }
}

/**
 * Critical hit bonus - increases critical hit chance.
 */
final class CriticalHitBonus extends AbstractBonus {
    public CriticalHitBonus(double critChance) { super(Type.CRITICAL_HIT, critChance); }
    public CriticalHitBonus(double critChance, boolean active, long duration) { super(Type.CRITICAL_HIT, critChance, active, duration); }
}

/**
 * Damage bonus - increases damage dealt.
 */
final class DamageBonus extends AbstractBonus {
    public DamageBonus(double damageIncrease) { super(Type.DAMAGE, damageIncrease); }
    public DamageBonus(double damageIncrease, boolean active, long duration) { super(Type.DAMAGE, damageIncrease, active, duration); }
    
    @Override
    public @NotNull String getFormattedDescription() {
        return String.format("Damage +%.1f", value);
    }
}

/**
 * Defense bonus - increases damage resistance.
 */
final class DefenseBonus extends AbstractBonus {
    public DefenseBonus(double defenseIncrease) { super(Type.DEFENSE, defenseIncrease); }
    public DefenseBonus(double defenseIncrease, boolean active, long duration) { super(Type.DEFENSE, defenseIncrease, active, duration); }
    
    @Override
    public @NotNull String getFormattedDescription() {
        return String.format("Defense +%.1f", value);
    }
}

/**
 * Regeneration bonus - increases health regeneration.
 */
final class RegenerationBonus extends AbstractBonus {
    public RegenerationBonus(double regenRate) { super(Type.REGENERATION, regenRate); }
    public RegenerationBonus(double regenRate, boolean active, long duration) { super(Type.REGENERATION, regenRate, active, duration); }
}

/**
 * Flight bonus - grants flight ability.
 */
final class FlightBonus extends AbstractBonus {
    public FlightBonus(boolean canFly, long duration) { super(Type.FLIGHT, canFly ? 1.0 : 0.0, canFly, duration); }
    
    @Override
    public @NotNull String getFormattedDescription() {
        return value > 0 ? "Flight Enabled" : "Flight Disabled";
    }
}

/**
 * Invisibility bonus - grants invisibility.
 */
final class InvisibilityBonus extends AbstractBonus {
    public InvisibilityBonus(boolean invisible, long duration) { super(Type.INVISIBILITY, invisible ? 1.0 : 0.0, invisible, duration); }
    
    @Override
    public @NotNull String getFormattedDescription() {
        return value > 0 ? "Invisibility Enabled" : "Invisibility Disabled";
    }
}

/**
 * Water breathing bonus - allows underwater breathing.
 */
final class WaterBreathingBonus extends AbstractBonus {
    public WaterBreathingBonus(boolean canBreathe, long duration) { super(Type.WATER_BREATHING, canBreathe ? 1.0 : 0.0, canBreathe, duration); }
    
    @Override
    public @NotNull String getFormattedDescription() {
        return value > 0 ? "Water Breathing Enabled" : "Water Breathing Disabled";
    }
}

/**
 * Night vision bonus - provides night vision.
 */
final class NightVisionBonus extends AbstractBonus {
    public NightVisionBonus(boolean hasVision, long duration) { super(Type.NIGHT_VISION, hasVision ? 1.0 : 0.0, hasVision, duration); }
    
    @Override
    public @NotNull String getFormattedDescription() {
        return value > 0 ? "Night Vision Enabled" : "Night Vision Disabled";
    }
}