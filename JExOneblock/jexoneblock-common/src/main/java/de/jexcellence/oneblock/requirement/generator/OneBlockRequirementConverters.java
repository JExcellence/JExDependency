package de.jexcellence.oneblock.requirement.generator;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.config.RequirementFactory;
import de.jexcellence.oneblock.requirement.EvolutionCurrencyRequirement;
import de.jexcellence.oneblock.requirement.EvolutionCustomRequirement;
import de.jexcellence.oneblock.requirement.EvolutionExperienceRequirement;
import de.jexcellence.oneblock.requirement.EvolutionItemRequirement;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Registers all OneBlock requirement converters with RPlatform's RequirementFactory.
 * <p>
 * This allows OneBlock requirements to be created from configuration maps,
 * enabling YAML-based requirement definitions for both generators and evolutions.
 * </p>
 *
 * @author JExcellence
 * @version 2.1.0
 * @since 2.0.0
 */
public final class OneBlockRequirementConverters {

    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    private static boolean registered = false;

    private OneBlockRequirementConverters() {
    }

    /**
     * Registers all OneBlock requirement converters with the factory.
     */
    public static void register() {
        if (registered) return;

        RequirementFactory factory = RequirementFactory.getInstance();

        // Generator requirements
        factory.registerConverter("EVOLUTION_LEVEL", OneBlockRequirementConverters::createEvolutionLevel);
        factory.registerConverter("BLOCKS_BROKEN", OneBlockRequirementConverters::createBlocksBroken);
        factory.registerConverter("PRESTIGE_LEVEL", OneBlockRequirementConverters::createPrestigeLevel);
        factory.registerConverter("ISLAND_LEVEL", OneBlockRequirementConverters::createIslandLevel);
        factory.registerConverter("GENERATOR_TIER", OneBlockRequirementConverters::createGeneratorTier);

        // Evolution requirements
        factory.registerConverter("EVOLUTION_CURRENCY", OneBlockRequirementConverters::createEvolutionCurrency);
        factory.registerConverter("EVOLUTION_EXPERIENCE", OneBlockRequirementConverters::createEvolutionExperience);
        factory.registerConverter("EVOLUTION_ITEM", OneBlockRequirementConverters::createEvolutionItem);
        factory.registerConverter("EVOLUTION_CUSTOM", OneBlockRequirementConverters::createEvolutionCustom);

        registered = true;
        LOGGER.info("Registered OneBlock requirement converters (9 types)");
    }

    /**
     * Unregisters all OneBlock requirement converters.
     */
    public static void unregister() {
        if (!registered) return;

        RequirementFactory factory = RequirementFactory.getInstance();

        // Generator requirements
        factory.unregisterConverter("EVOLUTION_LEVEL");
        factory.unregisterConverter("BLOCKS_BROKEN");
        factory.unregisterConverter("PRESTIGE_LEVEL");
        factory.unregisterConverter("ISLAND_LEVEL");
        factory.unregisterConverter("GENERATOR_TIER");

        // Evolution requirements
        factory.unregisterConverter("EVOLUTION_CURRENCY");
        factory.unregisterConverter("EVOLUTION_EXPERIENCE");
        factory.unregisterConverter("EVOLUTION_ITEM");
        factory.unregisterConverter("EVOLUTION_CUSTOM");

        registered = false;
        LOGGER.info("Unregistered OneBlock requirement converters");
    }

    // ==================== Generator Requirement Converters ====================

    @NotNull
    private static AbstractRequirement createEvolutionLevel(@NotNull Map<String, Object> config) {
        int level = getInt(config, "requiredLevel", 1);
        String evolutionName = getString(config, "evolutionName", null);
        return new EvolutionLevelRequirement(level, evolutionName);
    }

    @NotNull
    private static AbstractRequirement createBlocksBroken(@NotNull Map<String, Object> config) {
        long blocks = getLong(config, "requiredBlocks", 100);
        return new BlocksBrokenRequirement(blocks);
    }

    @NotNull
    private static AbstractRequirement createPrestigeLevel(@NotNull Map<String, Object> config) {
        int prestige = getInt(config, "requiredPrestige", 1);
        return new PrestigeLevelRequirement(prestige);
    }

    @NotNull
    private static AbstractRequirement createIslandLevel(@NotNull Map<String, Object> config) {
        int level = getInt(config, "requiredLevel", 1);
        return new IslandLevelRequirement(level);
    }

    @NotNull
    private static AbstractRequirement createGeneratorTier(@NotNull Map<String, Object> config) {
        String tier = getString(config, "requiredTier", "BASIC");
        return new GeneratorTierRequirement(tier);
    }

    // ==================== Evolution Requirement Converters ====================

    @NotNull
    private static AbstractRequirement createEvolutionCurrency(@NotNull Map<String, Object> config) {
        long amount = getLong(config, "requiredAmount", 1000);
        String typeStr = getString(config, "currencyType", "ISLAND_COINS");
        String evolutionName = getString(config, "evolutionName", null);
        boolean consume = getBoolean(config, "consumeOnComplete", true);

        EvolutionCurrencyRequirement.CurrencyType type;
        try {
            type = EvolutionCurrencyRequirement.CurrencyType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = EvolutionCurrencyRequirement.CurrencyType.ISLAND_COINS;
        }

        return new EvolutionCurrencyRequirement(amount, type, evolutionName, consume);
    }

    @NotNull
    private static AbstractRequirement createEvolutionExperience(@NotNull Map<String, Object> config) {
        double xp = getDouble(config, "requiredExperience", 1000);
        String typeStr = getString(config, "experienceType", "EVOLUTION_XP");
        String evolutionName = getString(config, "evolutionName", null);
        boolean consume = getBoolean(config, "consumeOnComplete", false);

        EvolutionExperienceRequirement.ExperienceType type;
        try {
            type = EvolutionExperienceRequirement.ExperienceType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = EvolutionExperienceRequirement.ExperienceType.EVOLUTION_XP;
        }

        return new EvolutionExperienceRequirement(xp, type, evolutionName, consume);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private static AbstractRequirement createEvolutionItem(@NotNull Map<String, Object> config) {
        List<ItemStack> items = new ArrayList<>();

        // Parse items from config
        Object itemsObj = config.get("requiredItems");
        if (itemsObj instanceof List<?> itemList) {
            for (Object item : itemList) {
                if (item instanceof Map<?, ?> itemMap) {
                    String materialStr = getString((Map<String, Object>) itemMap, "type", "STONE");
                    int amount = getInt((Map<String, Object>) itemMap, "amount", 1);
                    try {
                        Material material = Material.valueOf(materialStr.toUpperCase());
                        items.add(new ItemStack(material, amount));
                    } catch (IllegalArgumentException e) {
                        LOGGER.warning("Invalid material: " + materialStr);
                    }
                } else if (item instanceof ItemStack is) {
                    items.add(is);
                }
            }
        }

        // Fallback to single item
        if (items.isEmpty()) {
            String materialStr = getString(config, "material", "STONE");
            int amount = getInt(config, "amount", 1);
            try {
                Material material = Material.valueOf(materialStr.toUpperCase());
                items.add(new ItemStack(material, amount));
            } catch (IllegalArgumentException e) {
                items.add(new ItemStack(Material.STONE, 1));
            }
        }

        boolean exactMatch = getBoolean(config, "exactMatch", false);
        boolean checkStorage = getBoolean(config, "checkStorage", false);
        String evolutionName = getString(config, "evolutionName", null);
        boolean consume = getBoolean(config, "consumeOnComplete", true);

        return new EvolutionItemRequirement(items, exactMatch, checkStorage, evolutionName, consume);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private static AbstractRequirement createEvolutionCustom(@NotNull Map<String, Object> config) {
        String typeStr = getString(config, "customType", "BLOCKS_BROKEN");
        double value = getDouble(config, "requiredValue", 100);
        String evolutionName = getString(config, "evolutionName", null);
        boolean consume = getBoolean(config, "consumeOnComplete", false);

        EvolutionCustomRequirement.CustomType type;
        try {
            type = EvolutionCustomRequirement.CustomType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = EvolutionCustomRequirement.CustomType.BLOCKS_BROKEN;
        }

        Map<String, Object> parameters = null;
        Object paramsObj = config.get("parameters");
        if (paramsObj instanceof Map<?, ?>) {
            parameters = (Map<String, Object>) paramsObj;
        }

        return new EvolutionCustomRequirement(type, value, parameters, evolutionName, consume);
    }

    // ==================== Helpers ====================

    private static int getInt(Map<String, Object> config, String key, int defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static long getLong(Map<String, Object> config, String key, long defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static double getDouble(Map<String, Object> config, String key, double defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static boolean getBoolean(Map<String, Object> config, String key, boolean defaultValue) {
        Object value = config.get(key);
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return Boolean.parseBoolean((String) value);
        return defaultValue;
    }

    private static String getString(Map<String, Object> config, String key, String defaultValue) {
        Object value = config.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
