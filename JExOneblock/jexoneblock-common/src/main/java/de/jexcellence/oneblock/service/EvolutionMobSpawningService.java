package de.jexcellence.oneblock.service;

import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockCore;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public class EvolutionMobSpawningService {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("JExOneblock");
    
    private static final Map<String, EvolutionMobConfig> EVOLUTION_MOBS = new HashMap<>();
    
    static {
        initializeEvolutionMobs();
    }

    public @Nullable Entity spawnEvolutionMob(@NotNull Player player, @NotNull OneblockCore core, 
                                   @NotNull Location spawnLocation, @NotNull EEvolutionRarityType rarity) {
        
        var evolutionName = core.getCurrentEvolution();
        int evolutionLevel = core.getEvolutionLevel();
        
        var mobType = getMobFromEvolution(evolutionName, rarity);
        
        if (mobType == null) {
            var config = EVOLUTION_MOBS.get(evolutionName.toLowerCase());
            if (config == null) {
                LOGGER.warning("No mob configuration found for evolution: " + evolutionName);
                return null;
            }
            
            boolean spawnHostile = shouldSpawnHostile(rarity, evolutionLevel);
            
            mobType = selectMobType(config, spawnHostile, rarity);
            if (mobType == null) {
                return null;
            }
        }
        
        var safeLocation = findSafeSpawnLocation(spawnLocation);
        if (safeLocation == null) {
            LOGGER.warning("Could not find safe spawn location for mob");
            return null;
        }
        
        var entity = safeLocation.getWorld().spawnEntity(safeLocation, mobType);
        
        configureMob(entity, evolutionName, evolutionLevel, rarity, isHostileMob(mobType));
        
        LOGGER.info("Spawned " + (isHostileMob(mobType) ? "hostile" : "friendly") + " mob " + mobType + 
                   " for evolution " + evolutionName + " at level " + evolutionLevel);
        
        return entity;
    }
    
    /**
     * Gets a mob from the actual evolution configuration
     */
    @Nullable
    private EntityType getMobFromEvolution(@NotNull String evolutionName, @NotNull EEvolutionRarityType rarity) {
        try {
            // Get the evolution from the factory
            var evolutionFactory = de.jexcellence.oneblock.factory.EvolutionFactory.getInstance();
            var evolution = evolutionFactory.getCachedEvolution(evolutionName);

            if (evolution != null) {
                // Get entities for the specific rarity
                var evolutionEntities = evolution.getEntities().stream()
                    .filter(entity -> entity.getRarity() == rarity && entity.isValid())
                    .findFirst();
                
                if (evolutionEntities.isPresent()) {
                    var entityConfig = evolutionEntities.get();
                    var spawnEggs = entityConfig.getSpawnEggs();
                    
                    if (spawnEggs != null && !spawnEggs.isEmpty()) {
                        // Convert spawn egg to entity type
                        ThreadLocalRandom random = ThreadLocalRandom.current();
                        Material spawnEgg = spawnEggs.get(random.nextInt(spawnEggs.size()));
                        return getEntityTypeFromSpawnEgg(spawnEgg);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to get mobs from evolution " + evolutionName + ": " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Converts a spawn egg material to entity type
     */
    @Nullable
    private EntityType getEntityTypeFromSpawnEgg(@NotNull Material spawnEgg) {
        String eggName = spawnEgg.name();
        if (eggName.endsWith("_SPAWN_EGG")) {
            String entityName = eggName.substring(0, eggName.length() - "_SPAWN_EGG".length());
            try {
                return EntityType.valueOf(entityName);
            } catch (IllegalArgumentException e) {
                LOGGER.warning("Could not convert spawn egg " + spawnEgg + " to entity type");
            }
        }
        return null;
    }
    
    /**
     * Checks if an entity type is hostile
     */
    private boolean isHostileMob(@NotNull EntityType entityType) {
        return switch (entityType) {
            case ZOMBIE, SKELETON, CREEPER, SPIDER, ENDERMAN, WITCH, BLAZE, GHAST, 
                 WITHER_SKELETON, ZOMBIFIED_PIGLIN, HOGLIN, ZOGLIN, PHANTOM, 
                 DROWNED, HUSK, STRAY, CAVE_SPIDER, SILVERFISH, ENDERMITE,
                 SHULKER, GUARDIAN, ELDER_GUARDIAN, WITHER, ENDER_DRAGON -> true;
            default -> false;
        };
    }
    
    /**
     * Determines whether to spawn a hostile or friendly mob
     */
    private boolean shouldSpawnHostile(@NotNull EEvolutionRarityType rarity, int evolutionLevel) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        // Base hostile chance increases with rarity
        double hostileChance = switch (rarity) {
            case COMMON, UNCOMMON -> 0.3;
            case RARE -> 0.4;
            case EPIC -> 0.5;
            case LEGENDARY -> 0.6;
            case SPECIAL -> 0.65;
            case UNIQUE -> 0.7;
            case MYTHICAL -> 0.75;
            case DIVINE -> 0.8;
            case CELESTIAL -> 0.85;
            default -> 0.9;
        };
        
        // Reduce hostile chance for early levels to help new players
        if (evolutionLevel < 5) {
            hostileChance *= 0.5;
        } else if (evolutionLevel < 10) {
            hostileChance *= 0.7;
        }
        
        return random.nextDouble() < hostileChance;
    }
    
    /**
     * Selects appropriate mob type based on configuration and parameters
     */
    @Nullable
    private EntityType selectMobType(@NotNull EvolutionMobConfig config, boolean hostile, @NotNull EEvolutionRarityType rarity) {
        List<EntityType> availableMobs = hostile ? config.hostileMobs : config.friendlyMobs;
        
        if (availableMobs.isEmpty()) {
            return null;
        }
        
        // For higher rarities, prefer more dangerous/rare mobs (later in the list)
        if (rarity.getTier() >= EEvolutionRarityType.LEGENDARY.getTier() && availableMobs.size() > 1) {
            // Bias towards later mobs in the list for high rarities
            int startIndex = Math.max(0, availableMobs.size() - 3);
            List<EntityType> rareMobs = availableMobs.subList(startIndex, availableMobs.size());
            return rareMobs.get(ThreadLocalRandom.current().nextInt(rareMobs.size()));
        }
        
        return availableMobs.get(ThreadLocalRandom.current().nextInt(availableMobs.size()));
    }
    
    /**
     * Finds a safe location to spawn the mob
     */
    @Nullable
    private Location findSafeSpawnLocation(@NotNull Location baseLocation) {
        World world = baseLocation.getWorld();
        if (world == null) return null;
        
        // Try locations around the base location
        for (int attempts = 0; attempts < 10; attempts++) {
            int offsetX = ThreadLocalRandom.current().nextInt(-3, 4);
            int offsetZ = ThreadLocalRandom.current().nextInt(-3, 4);
            
            Location testLocation = baseLocation.clone().add(offsetX, 1, offsetZ);
            
            // Check if location is safe (air above, solid below)
            if (world.getBlockAt(testLocation).getType() == Material.AIR &&
                world.getBlockAt(testLocation.clone().add(0, 1, 0)).getType() == Material.AIR &&
                world.getBlockAt(testLocation.clone().add(0, -1, 0)).getType().isSolid()) {
                
                return testLocation;
            }
        }
        
        // Fallback to base location + 1 Y
        return baseLocation.clone().add(0, 1, 0);
    }
    
    /**
     * Configures the spawned mob with appropriate stats and equipment
     */
    private void configureMob(@NotNull Entity entity, @NotNull String evolution, int evolutionLevel, 
                             @NotNull EEvolutionRarityType rarity, boolean hostile) {
        
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        
        // Set custom name
        living.customName(generateMobName(evolution, rarity, hostile));
        living.setCustomNameVisible(true);
        
        // Configure health and damage based on evolution level and rarity
        double healthMultiplier = 1.0 + (evolutionLevel * 0.1) + (rarity.getTier() * 0.2);
        if (living.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null) {
            double maxHealth = living.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getDefaultValue() * healthMultiplier;
            living.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(Math.min(maxHealth, 100.0));
            living.setHealth(Math.min(maxHealth, 100.0));
        }
        
        // Configure damage for hostile mobs
        if (hostile && living.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE) != null) {
            double damageMultiplier = 1.0 + (evolutionLevel * 0.05) + (rarity.getTier() * 0.1);
            double damage = living.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getDefaultValue() * damageMultiplier;
            living.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).setBaseValue(Math.min(damage, 20.0));
        }
        
        // Add evolution-specific effects
        addEvolutionEffects(living, evolution, rarity);
        
        // Configure equipment for certain mob types
        configureEquipment(living, evolution, evolutionLevel, rarity);
        
        // Set persistence and other properties
        living.setPersistent(false); // Despawn naturally
        living.setRemoveWhenFarAway(true);
    }
    
    /**
     * Generates appropriate mob name based on evolution and rarity
     */
    @NotNull
    private Component generateMobName(@NotNull String evolution, @NotNull EEvolutionRarityType rarity, boolean hostile) {
        String prefix = hostile ? "<red>" : "<green>";
        String type = hostile ? "Guardian" : "Spirit";
        
        return MiniMessage.miniMessage().deserialize(prefix + rarity.getColorCode() + evolution + " " + type);
    }
    
    /**
     * Adds evolution-specific potion effects to mobs
     */
    private void addEvolutionEffects(@NotNull LivingEntity entity, @NotNull String evolution, @NotNull EEvolutionRarityType rarity) {
        List<PotionEffect> effects = new ArrayList<>();
        
        // Evolution-specific effects
        switch (evolution.toLowerCase()) {
            case "genesis", "terra" -> {
                // Earth-based effects
                effects.add(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0));
            }
            case "aqua" -> {
                // Water-based effects
                effects.add(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0));
                effects.add(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, Integer.MAX_VALUE, 0));
            }
            case "ignis" -> {
                // Fire-based effects
                effects.add(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));
            }
            case "ventus" -> {
                // Air-based effects
                effects.add(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
                effects.add(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 1));
            }
            case "nether" -> {
                // Nether effects
                effects.add(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));
                effects.add(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0));
            }
            case "end" -> {
                // End effects
                effects.add(new PotionEffect(PotionEffectType.LEVITATION, 100, 0));
            }
            case "cosmic", "stellar", "galactic" -> {
                // Space effects
                effects.add(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));
                effects.add(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
            }
        }
        
        // Rarity-based effects
        if (rarity.getTier() >= EEvolutionRarityType.LEGENDARY.getTier()) {
            effects.add(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));
        }
        
        if (rarity.getTier() >= EEvolutionRarityType.MYTHICAL.getTier()) {
            effects.add(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0));
        }
        
        // Apply effects
        effects.forEach(entity::addPotionEffect);
    }
    
    /**
     * Configures equipment for mobs that can wear it
     */
    private void configureEquipment(@NotNull LivingEntity entity, @NotNull String evolution, 
                                   int evolutionLevel, @NotNull EEvolutionRarityType rarity) {
        
        if (!(entity instanceof Mob mob)) {
            return;
        }
        
        // Configure equipment based on evolution
        ItemStack helmet = getEvolutionHelmet(evolution, rarity);
        ItemStack weapon = getEvolutionWeapon(evolution, rarity);
        
        if (helmet != null) {
            mob.getEquipment().setHelmet(helmet);
            mob.getEquipment().setHelmetDropChance(0.1f);
        }
        
        if (weapon != null) {
            mob.getEquipment().setItemInMainHand(weapon);
            mob.getEquipment().setItemInMainHandDropChance(0.05f);
        }
    }
    
    /**
     * Gets evolution-specific helmet
     */
    @Nullable
    private ItemStack getEvolutionHelmet(@NotNull String evolution, @NotNull EEvolutionRarityType rarity) {
        Material helmetMaterial = switch (evolution.toLowerCase()) {
            case "genesis", "terra", "stone" -> Material.LEATHER_HELMET;
            case "iron", "copper", "bronze" -> Material.IRON_HELMET;
            case "gold" -> Material.GOLDEN_HELMET;
            case "diamond" -> Material.DIAMOND_HELMET;
            case "nether", "end" -> Material.NETHERITE_HELMET;
            default -> null;
        };
        
        return helmetMaterial != null ? new ItemStack(helmetMaterial) : null;
    }
    
    /**
     * Gets evolution-specific weapon
     */
    @Nullable
    private ItemStack getEvolutionWeapon(@NotNull String evolution, @NotNull EEvolutionRarityType rarity) {
        Material weaponMaterial = switch (evolution.toLowerCase()) {
            case "genesis", "terra", "stone" -> Material.STONE_SWORD;
            case "iron", "copper", "bronze" -> Material.IRON_SWORD;
            case "gold" -> Material.GOLDEN_SWORD;
            case "diamond" -> Material.DIAMOND_SWORD;
            case "nether", "end" -> Material.NETHERITE_SWORD;
            default -> Material.WOODEN_SWORD;
        };
        
        return new ItemStack(weaponMaterial);
    }
    
    /**
     * Initialize evolution-specific mob configurations
     */
    private static void initializeEvolutionMobs() {
        // Genesis Evolution (Levels 1-2)
        EVOLUTION_MOBS.put("genesis", new EvolutionMobConfig(
            Arrays.asList(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER),
            Arrays.asList(EntityType.PIG, EntityType.COW, EntityType.CHICKEN)
        ));
        
        // Terra Evolution (Level 2)
        EVOLUTION_MOBS.put("terra", new EvolutionMobConfig(
            Arrays.asList(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER),
            Arrays.asList(EntityType.PIG, EntityType.COW, EntityType.SHEEP)
        ));
        
        // Aqua Evolution (Level 3)
        EVOLUTION_MOBS.put("aqua", new EvolutionMobConfig(
            Arrays.asList(EntityType.DROWNED, EntityType.GUARDIAN, EntityType.SQUID),
            Arrays.asList(EntityType.DOLPHIN, EntityType.TROPICAL_FISH, EntityType.COD)
        ));
        
        // Ignis Evolution (Level 4)
        EVOLUTION_MOBS.put("ignis", new EvolutionMobConfig(
            Arrays.asList(EntityType.BLAZE, EntityType.MAGMA_CUBE, EntityType.GHAST),
            Arrays.asList(EntityType.STRIDER, EntityType.HOGLIN)
        ));
        
        // Ventus Evolution (Level 5)
        EVOLUTION_MOBS.put("ventus", new EvolutionMobConfig(
            Arrays.asList(EntityType.PHANTOM, EntityType.VEX, EntityType.PARROT),
            Arrays.asList(EntityType.PARROT, EntityType.BAT, EntityType.BEE)
        ));
        
        // Stone Evolution (Level 6)
        EVOLUTION_MOBS.put("stone", new EvolutionMobConfig(
            Arrays.asList(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.SILVERFISH),
            Arrays.asList(EntityType.IRON_GOLEM, EntityType.VILLAGER)
        ));
        
        // Continue with more evolutions...
        initializeAdvancedEvolutions();
    }
    
    /**
     * Initialize advanced evolution mob configurations
     */
    private static void initializeAdvancedEvolutions() {
        // Nether Evolution (Level 14)
        EVOLUTION_MOBS.put("nether", new EvolutionMobConfig(
            Arrays.asList(EntityType.BLAZE, EntityType.WITHER_SKELETON, EntityType.GHAST, EntityType.PIGLIN_BRUTE),
            Arrays.asList(EntityType.STRIDER, EntityType.PIGLIN, EntityType.ZOMBIFIED_PIGLIN)
        ));
        
        // End Evolution (Level 15)
        EVOLUTION_MOBS.put("end", new EvolutionMobConfig(
            Arrays.asList(EntityType.ENDERMAN, EntityType.ENDERMITE, EntityType.SHULKER),
            Arrays.asList(EntityType.ENDERMAN) // Neutral endermen
        ));
        
        // Dragon Evolution (Level 19)
        EVOLUTION_MOBS.put("dragon", new EvolutionMobConfig(
            Arrays.asList(EntityType.ENDER_DRAGON, EntityType.WITHER, EntityType.ENDERMAN),
            Arrays.asList(EntityType.HORSE, EntityType.LLAMA)
        ));
        
        // Cosmic Evolution (Level 45)
        EVOLUTION_MOBS.put("cosmic", new EvolutionMobConfig(
            Arrays.asList(EntityType.WITHER, EntityType.ENDER_DRAGON, EntityType.WARDEN),
            Arrays.asList(EntityType.ALLAY, EntityType.GLOW_SQUID)
        ));
        
        // Add more evolutions as needed...
        addRemainingEvolutions();
    }
    
    /**
     * Add remaining evolution configurations
     */
    private static void addRemainingEvolutions() {
        // Add default configurations for remaining evolutions
        String[] remainingEvolutions = {
            "copper", "iron", "coal", "wood", "bronze", "gold", "diamond",
            "knight", "castle", "artemis", "crusader", "explorer", "helium",
            "artist", "argon", "krypton", "hephaestus", "electric", "factory",
            "earth", "moon", "cyber", "nano", "bio", "quantum", "digital",
            "solar", "void", "nebula", "supernova", "black hole", "stellar",
            "galactic", "multiverse", "infinity", "eternity", "omnipotence",
            "dimensional", "universal", "eden"
        };
        
        for (String evolution : remainingEvolutions) {
            if (!EVOLUTION_MOBS.containsKey(evolution)) {
                // Default configuration with scaling difficulty
                EVOLUTION_MOBS.put(evolution, new EvolutionMobConfig(
                    Arrays.asList(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER),
                    Arrays.asList(EntityType.PIG, EntityType.COW, EntityType.SHEEP, EntityType.CHICKEN)
                ));
            }
        }
    }
    
    /**
     * Configuration class for evolution-specific mobs
     */
    private static class EvolutionMobConfig {
        final List<EntityType> hostileMobs;
        final List<EntityType> friendlyMobs;
        
        EvolutionMobConfig(List<EntityType> hostileMobs, List<EntityType> friendlyMobs) {
            this.hostileMobs = new ArrayList<>(hostileMobs);
            this.friendlyMobs = new ArrayList<>(friendlyMobs);
        }
    }
}