package com.raindropcentral.rplatform.reward.config;

import com.raindropcentral.rplatform.reward.AbstractReward;
import com.raindropcentral.rplatform.reward.RewardRegistry;
import com.raindropcentral.rplatform.reward.impl.*;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

public final class RewardFactory<T> {

    private static final Logger LOGGER = Logger.getLogger(RewardFactory.class.getName());
    private static final RewardFactory<Map<String, Object>> INSTANCE = new RewardFactory<>();

    private final Map<String, Function<Map<String, Object>, AbstractReward>> converters = new HashMap<>();
    private final Map<Class<?>, RewardSectionAdapter<?>> sectionAdapters = new HashMap<>();

    private RewardFactory() {
        registerDefaultConverters();
    }

    public static RewardFactory<Map<String, Object>> getInstance() {
        return INSTANCE;
    }

    public void registerConverter(
        @NotNull String type,
        @NotNull Function<Map<String, Object>, AbstractReward> converter
    ) {
        converters.put(type.toUpperCase(), converter);
    }

    public void unregisterConverter(@NotNull String type) {
        converters.remove(type.toUpperCase());
    }

    public boolean hasConverter(@NotNull String type) {
        return converters.containsKey(type.toUpperCase());
    }

    public <S> void registerSectionAdapter(
        @NotNull Class<S> sectionClass,
        @NotNull RewardSectionAdapter<S> adapter
    ) {
        sectionAdapters.put(sectionClass, adapter);
    }

    public void unregisterSectionAdapter(@NotNull Class<?> sectionClass) {
        sectionAdapters.remove(sectionClass);
    }

    @SuppressWarnings("unchecked")
    public <S> RewardSectionAdapter<S> getSectionAdapter(@NotNull Class<S> sectionClass) {
        return (RewardSectionAdapter<S>) sectionAdapters.get(sectionClass);
    }

    public AbstractReward fromMap(@NotNull Map<String, Object> config) {
        String type = getString(config, "type", null);
        if (type == null) {
            throw new IllegalArgumentException("Reward type not specified");
        }

        Function<Map<String, Object>, AbstractReward> converter = converters.get(type.toUpperCase());
        if (converter == null) {
            throw new IllegalArgumentException("Unknown reward type: " + type);
        }

        return converter.apply(config);
    }

    public Optional<AbstractReward> tryFromMap(@NotNull Map<String, Object> config) {
        try {
            return Optional.of(fromMap(config));
        } catch (Exception e) {
            LOGGER.warning("Failed to parse reward: " + e.getMessage());
            return Optional.empty();
        }
    }

    public AbstractReward fromSection(@NotNull T section) {
        return fromSection(section, null);
    }

    @SuppressWarnings("unchecked")
    public AbstractReward fromSection(@NotNull T section, @Nullable Map<String, Object> context) {
        Class<?> sectionClass = section.getClass();
        RewardSectionAdapter<T> adapter = (RewardSectionAdapter<T>) sectionAdapters.get(sectionClass);
        
        if (adapter == null) {
            for (Map.Entry<Class<?>, RewardSectionAdapter<?>> entry : sectionAdapters.entrySet()) {
                if (entry.getKey().isAssignableFrom(sectionClass)) {
                    adapter = (RewardSectionAdapter<T>) entry.getValue();
                    break;
                }
            }
        }
        
        if (adapter == null) {
            throw new IllegalArgumentException("No adapter registered for: " + sectionClass.getName());
        }

        AbstractReward reward = adapter.convert(section, context);
        if (reward == null) {
            throw new IllegalArgumentException("Failed to convert section to reward");
        }

        return reward;
    }

    public Optional<AbstractReward> tryFromSection(@NotNull T section) {
        return tryFromSection(section, null);
    }

    public Optional<AbstractReward> tryFromSection(@NotNull T section, @Nullable Map<String, Object> context) {
        try {
            return Optional.of(fromSection(section, context));
        } catch (Exception e) {
            LOGGER.warning("Failed to parse reward from section: " + e.getMessage());
            return Optional.empty();
        }
    }

    public List<AbstractReward> parseRewards(@NotNull Map<String, T> sections) {
        return parseRewards(sections, null);
    }

    public List<AbstractReward> parseRewards(
        @NotNull Map<String, T> sections,
        @Nullable Map<String, Object> context
    ) {
        List<AbstractReward> rewards = new ArrayList<>();
        for (T section : sections.values()) {
            tryFromSection(section, context).ifPresent(rewards::add);
        }
        return rewards;
    }

    public Set<String> getRegisteredTypes() {
        return Set.copyOf(converters.keySet());
    }

    private void registerDefaultConverters() {
        converters.put("ITEM", this::createItemReward);
        converters.put("CURRENCY", this::createCurrencyReward);
        converters.put("EXPERIENCE", this::createExperienceReward);
        converters.put("COMMAND", this::createCommandReward);
        converters.put("COMPOSITE", this::createCompositeReward);
        converters.put("CHOICE", this::createChoiceReward);
        converters.put("PERMISSION", this::createPermissionReward);
    }

    private ItemReward createItemReward(Map<String, Object> config) {
        ItemStack item = getItemStack(config, "item");
        if (item == null) {
            throw new IllegalArgumentException("Item reward requires 'item' field");
        }
        return new ItemReward(item);
    }

    private CurrencyReward createCurrencyReward(Map<String, Object> config) {
        String currencyId = getString(config, "currencyId", "vault");
        double amount = getDouble(config, "amount", 0.0);
        return new CurrencyReward(currencyId, amount);
    }

    private ExperienceReward createExperienceReward(Map<String, Object> config) {
        int amount = getInt(config, "amount", 0);
        String typeStr = getString(config, "experienceType", "POINTS");
        ExperienceReward.ExperienceType type = ExperienceReward.ExperienceType.valueOf(typeStr.toUpperCase());
        return new ExperienceReward(amount, type);
    }

    private CommandReward createCommandReward(Map<String, Object> config) {
        String command = getString(config, "command", null);
        if (command == null) {
            throw new IllegalArgumentException("Command reward requires 'command' field");
        }
        boolean executeAsPlayer = getBoolean(config, "executeAsPlayer", false);
        long delayTicks = getLong(config, "delayTicks", 0L);
        return new CommandReward(command, executeAsPlayer, delayTicks);
    }

    @SuppressWarnings("unchecked")
    private CompositeReward createCompositeReward(Map<String, Object> config) {
        List<Map<String, Object>> rewardMaps = (List<Map<String, Object>>) config.get("rewards");
        if (rewardMaps == null || rewardMaps.isEmpty()) {
            throw new IllegalArgumentException("Composite reward requires 'rewards' list");
        }

        List<AbstractReward> rewards = new ArrayList<>();
        for (Map<String, Object> rewardMap : rewardMaps) {
            rewards.add(fromMap(rewardMap));
        }

        boolean continueOnError = getBoolean(config, "continueOnError", false);
        return new CompositeReward(rewards, continueOnError);
    }

    @SuppressWarnings("unchecked")
    private ChoiceReward createChoiceReward(Map<String, Object> config) {
        List<Map<String, Object>> choiceMaps = (List<Map<String, Object>>) config.get("choices");
        if (choiceMaps == null || choiceMaps.isEmpty()) {
            throw new IllegalArgumentException("Choice reward requires 'choices' list");
        }

        List<AbstractReward> choices = new ArrayList<>();
        for (Map<String, Object> choiceMap : choiceMaps) {
            choices.add(fromMap(choiceMap));
        }

        int minimumRequired = getInt(config, "minimumRequired", 1);
        Integer maximumRequired = config.containsKey("maximumRequired") 
            ? getInt(config, "maximumRequired", 1) 
            : null;
        boolean allowMultipleSelections = getBoolean(config, "allowMultipleSelections", false);

        return new ChoiceReward(choices, minimumRequired, maximumRequired, allowMultipleSelections);
    }

    @SuppressWarnings("unchecked")
    private PermissionReward createPermissionReward(Map<String, Object> config) {
        Object permsObj = config.get("permissions");
        List<String> permissions = new ArrayList<>();

        if (permsObj instanceof String) {
            permissions.add((String) permsObj);
        } else if (permsObj instanceof List) {
            permissions.addAll((List<String>) permsObj);
        } else {
            throw new IllegalArgumentException("Permission reward requires 'permissions' field");
        }

        Long durationSeconds = config.containsKey("durationSeconds") 
            ? getLong(config, "durationSeconds", 0L) 
            : null;
        boolean temporary = getBoolean(config, "temporary", false);

        return new PermissionReward(permissions, durationSeconds, temporary);
    }

    @SuppressWarnings("unchecked")
    private ItemStack getItemStack(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value == null) return null;

        if (value instanceof ItemStack) {
            return (ItemStack) value;
        }

        if (value instanceof Map) {
            Map<String, Object> itemMap = (Map<String, Object>) value;
            String materialStr = getString(itemMap, "material", "STONE");
            int amount = getInt(itemMap, "amount", 1);
            
            Material material = Material.matchMaterial(materialStr);
            if (material == null) {
                material = Material.STONE;
            }
            
            return new ItemStack(material, amount);
        }

        return null;
    }

    private String getString(Map<String, Object> config, String key, @Nullable String defaultValue) {
        Object value = config.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private int getInt(Map<String, Object> config, String key, int defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private long getLong(Map<String, Object> config, String key, long defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private double getDouble(Map<String, Object> config, String key, double defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private boolean getBoolean(Map<String, Object> config, String key, boolean defaultValue) {
        Object value = config.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }
}
