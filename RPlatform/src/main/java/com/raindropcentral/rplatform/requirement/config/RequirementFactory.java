package com.raindropcentral.rplatform.requirement.config;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.impl.*;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory for creating AbstractRequirement instances from configuration data.
 *
 * <p>This factory provides a centralized, extensible way to create requirements from
 * various configuration formats. Plugins can register custom converters for their
 * own requirement types.
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * // Create from a map (e.g., parsed YAML)
 * Map<String, Object> config = Map.of(
 *     "type", "ITEM",
 *     "items", List.of(Map.of("type", "DIAMOND", "amount", 10)),
 *     "consumeOnComplete", true
 * );
 * AbstractRequirement req = RequirementFactory.getInstance().fromMap(config);
 *
 * // Or use the builder API
 * AbstractRequirement req = RequirementBuilder.item()
 *     .addItem(new ItemStack(Material.DIAMOND, 10))
 *     .consumeOnComplete(true)
 *     .build();
 * }</pre>
 *
 * @author ItsRainingHP, JExcellence
 * @since 2.0.0
 * @version 1.0.0
 */
public final class RequirementFactory {

    private static final Logger LOGGER = Logger.getLogger(RequirementFactory.class.getName());
    private static final RequirementFactory INSTANCE = new RequirementFactory();

    private final Map<String, Function<Map<String, Object>, AbstractRequirement>> converters = new ConcurrentHashMap<>();
    private final Map<Class<?>, RequirementSectionAdapter<?>> sectionAdapters = new ConcurrentHashMap<>();

    private RequirementFactory() {
        registerDefaultConverters();
    }

    /**
     * Gets instance.
     */
    @NotNull
    public static RequirementFactory getInstance() {
        return INSTANCE;
    }

    // ==================== Converter Registration ====================

    /**
     * Registers a custom converter for a requirement type.
     *
     * @param type the requirement type name (e.g., "EVOLUTION_LEVEL")
     * @param converter function that converts a config map to a requirement
     */
    public void registerConverter(
            @NotNull String type,
            @NotNull Function<Map<String, Object>, AbstractRequirement> converter
    ) {
        converters.put(type.toUpperCase(), converter);
        LOGGER.info("Registered requirement converter for type: " + type);
    }

    /**
     * Unregisters a converter for a requirement type.
     *
     * @param type the requirement type name
     */
    public void unregisterConverter(@NotNull String type) {
        converters.remove(type.toUpperCase());
    }

    /**
     * Registers a section adapter for a specific config section class.
 *
 * <p>Section adapters allow plugins to convert their custom config section types
     * directly to AbstractRequirement instances.
     * 
     * <p><b>Note:</b> Duplicate registrations are ignored to prevent infinite loops
     * and CPU spikes during initialization.</p>
     *
     * @param sectionClass the config section class
     * @param adapter the adapter that converts sections to requirements
     * @param <T> the config section type
     */
    public <T> void registerSectionAdapter(
            @NotNull Class<T> sectionClass,
            @NotNull RequirementSectionAdapter<T> adapter
    ) {
        // Prevent duplicate registration to avoid infinite loops and CPU spikes
        if (sectionAdapters.containsKey(sectionClass)) {
            LOGGER.fine("Section adapter already registered for: " + sectionClass.getSimpleName());
            return;
        }
        
        sectionAdapters.put(sectionClass, adapter);
        LOGGER.info("Registered section adapter for: " + sectionClass.getSimpleName());
    }

    /**
     * Unregisters a section adapter.
     *
     * @param sectionClass the config section class
     */
    public void unregisterSectionAdapter(@NotNull Class<?> sectionClass) {
        sectionAdapters.remove(sectionClass);
    }

    /**
     * Gets the registered section adapter for a class.
     *
     * @param sectionClass the config section class
     * @param <T> the config section type
     * @return the adapter, or null if not registered
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> RequirementSectionAdapter<T> getSectionAdapter(@NotNull Class<T> sectionClass) {
        return (RequirementSectionAdapter<T>) sectionAdapters.get(sectionClass);
    }

    /**
     * Converts a config section to an AbstractRequirement using the registered adapter.
     *
     * @param section the config section to convert
     * @param <T> the config section type
     * @return the created requirement
     * @throws IllegalArgumentException if no adapter is registered for the section type
     */
    @NotNull
    public <T> AbstractRequirement fromSection(@NotNull T section) {
        return fromSection(section, null);
    }

    /**
     * Converts a config section to an AbstractRequirement using the registered adapter.
     *
     * @param section the config section to convert
     * @param context optional context data for conversion
     * @param <T> the config section type
     * @return the created requirement
     * @throws IllegalArgumentException if no adapter is registered or conversion fails
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public <T> AbstractRequirement fromSection(@NotNull T section, @Nullable Map<String, Object> context) {
        Class<?> sectionClass = section.getClass();
        RequirementSectionAdapter<T> adapter = (RequirementSectionAdapter<T>) sectionAdapters.get(sectionClass);

        if (adapter == null) {
            throw new IllegalArgumentException("No adapter registered for section type: " + sectionClass.getName());
        }

        AbstractRequirement result = adapter.convert(section, context);
        if (result == null) {
            throw new IllegalArgumentException("Adapter returned null for section: " + sectionClass.getName());
        }

        return result;
    }

    /**
     * Attempts to convert a config section to an AbstractRequirement.
     *
     * @param section the config section to convert
     * @param <T> the config section type
     * @return the created requirement, or empty if conversion fails
     */
    @NotNull
    public <T> Optional<AbstractRequirement> tryFromSection(@NotNull T section) {
        return tryFromSection(section, null);
    }

    /**
     * Attempts to convert a config section to an AbstractRequirement.
     *
     * @param section the config section to convert
     * @param context optional context data for conversion
     * @param <T> the config section type
     * @return the created requirement, or empty if conversion fails
     */
    @NotNull
    public <T> Optional<AbstractRequirement> tryFromSection(@NotNull T section, @Nullable Map<String, Object> context) {
        try {
            return Optional.of(fromSection(section, context));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to convert section to requirement", e);
            return Optional.empty();
        }
    }

    /**
     * Parses multiple config sections into a list of AbstractRequirements.
     *
     * @param sections map of section key to config section
     * @param <T> the config section type
     * @return list of created requirements (skips failed conversions)
     */
    @NotNull
    public <T> List<AbstractRequirement> parseRequirements(@NotNull Map<String, T> sections) {
        return parseRequirements(sections, null);
    }

    /**
     * Parses multiple config sections into a list of AbstractRequirements.
     *
     * @param sections map of section key to config section
     * @param context optional context data for conversion
     * @param <T> the config section type
     * @return list of created requirements (skips failed conversions)
     */
    @NotNull
    public <T> List<AbstractRequirement> parseRequirements(
            @NotNull Map<String, T> sections,
            @Nullable Map<String, Object> context
    ) {
        if (sections.isEmpty()) {
            return List.of();
        }

        List<AbstractRequirement> requirements = new ArrayList<>();

        for (var entry : sections.entrySet()) {
            String key = entry.getKey();
            T section = entry.getValue();

            try {
                Optional<AbstractRequirement> req = tryFromSection(section, context);
                req.ifPresent(requirements::add);
            } catch (Exception e) {
                LOGGER.warning("Failed to parse requirement '" + key + "': " + e.getMessage());
            }
        }

        return requirements;
    }

    // ==================== Factory Methods ====================

    /**
     * Creates a requirement from a configuration map.
     *
     * @param config the configuration map containing requirement data
     * @return the created requirement
     * @throws IllegalArgumentException if the type is unknown or config is invalid
     */
    @NotNull
    public AbstractRequirement fromMap(@NotNull Map<String, Object> config) {
        String type = getString(config, "type", "UNKNOWN").toUpperCase();

        Function<Map<String, Object>, AbstractRequirement> converter = converters.get(type);
        if (converter == null) {
            throw new IllegalArgumentException("Unknown requirement type: " + type);
        }

        try {
            AbstractRequirement req = converter.apply(config);
            if (req == null) {
                throw new IllegalArgumentException("Converter returned null for type: " + type);
            }
            return req;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create requirement of type: " + type, e);
            throw new IllegalArgumentException("Failed to create requirement: " + e.getMessage(), e);
        }
    }

    /**
     * Attempts to create a requirement from a configuration map.
     *
     * @param config the configuration map
     * @return the created requirement, or empty if creation fails
     */
    @NotNull
    public Optional<AbstractRequirement> tryFromMap(@NotNull Map<String, Object> config) {
        try {
            return Optional.of(fromMap(config));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to create requirement from config", e);
            return Optional.empty();
        }
    }

    /**
     * Checks if a converter exists for the given type.
     *
     * @param type the requirement type
     * @return true if a converter is registered
     */
    public boolean hasConverter(@NotNull String type) {
        return converters.containsKey(type.toUpperCase());
    }

    /**
     * Gets all registered requirement types.
     *
     * @return set of registered type names
     */
    @NotNull
    public Set<String> getRegisteredTypes() {
        return Set.copyOf(converters.keySet());
    }

    // ==================== Default Converters ====================

    private void registerDefaultConverters() {
        converters.put("ITEM", this::createItemRequirement);
        converters.put("CURRENCY", this::createCurrencyRequirement);
        converters.put("EXPERIENCE_LEVEL", this::createExperienceRequirement);
        converters.put("PERMISSION", this::createPermissionRequirement);
        converters.put("LOCATION", this::createLocationRequirement);
        converters.put("PLAYTIME", this::createPlaytimeRequirement);
        converters.put("COMPOSITE", this::createCompositeRequirement);
        converters.put("CHOICE", this::createChoiceRequirement);
        converters.put("TIME_BASED", this::createTimedRequirement);
        converters.put("PLUGIN", this::createPluginRequirement);
        
        // Backward compatibility - map old types to new PLUGIN type
        converters.put("JOBS", this::createJobsRequirementCompat);
        converters.put("SKILLS", this::createSkillsRequirementCompat);
    }

    private ItemRequirement createItemRequirement(Map<String, Object> config) {
        List<ItemStack> items = getItemStacks(config, "requiredItems");
        if (items.isEmpty()) {
            items = getItemStacks(config, "items");
        }
        if (items.isEmpty()) {
            throw new IllegalArgumentException("No items specified for ITEM requirement");
        }

        return RequirementBuilder.item()
                .items(items)
                .consumeOnComplete(getBoolean(config, "consumeOnComplete", true))
                .exactMatch(getBoolean(config, "exactMatch", true))
                .description(getString(config, "description", null))
                .build();
    }

    private CurrencyRequirement createCurrencyRequirement(Map<String, Object> config) {
        String currency = getString(config, "currency", null);
        double amount = getDouble(config, "amount");
        Boolean consumable = getBoolean(config, "consumable", false);
        
        if (currency == null || currency.isEmpty()) {
            throw new IllegalArgumentException("Currency requirement must specify 'currency'");
        }
        
        if (amount <= 0) {
            throw new IllegalArgumentException("Currency requirement must specify a positive 'amount'");
        }

        return new CurrencyRequirement(currency, amount, consumable);
    }

    private ExperienceLevelRequirement createExperienceRequirement(Map<String, Object> config) {
        int level = getInt(config, "requiredLevel", 1);
        String typeStr = getString(config, "experienceType", "LEVEL");
        ExperienceLevelRequirement.ExperienceType type;
        try {
            type = ExperienceLevelRequirement.ExperienceType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = ExperienceLevelRequirement.ExperienceType.LEVEL;
        }

        return RequirementBuilder.experience()
                .level(level)
                .type(type)
                .consumeOnComplete(getBoolean(config, "consumeOnComplete", true))
                .description(getString(config, "description", null))
                .build();
    }

    private PermissionRequirement createPermissionRequirement(Map<String, Object> config) {
        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) config.get("requiredPermissions");
        if (permissions == null || permissions.isEmpty()) {
            throw new IllegalArgumentException("No permissions specified for PERMISSION requirement");
        }

        String modeStr = getString(config, "permissionMode", "ALL");
        PermissionRequirement.PermissionMode mode;
        try {
            mode = PermissionRequirement.PermissionMode.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            mode = PermissionRequirement.PermissionMode.ALL;
        }

        return RequirementBuilder.permission()
                .permissions(permissions)
                .mode(mode)
                .negated(getBoolean(config, "checkNegated", false))
                .description(getString(config, "description", null))
                .build();
    }

    private LocationRequirement createLocationRequirement(Map<String, Object> config) {
        var builder = RequirementBuilder.location();

        String world = getString(config, "requiredWorld", null);
        if (world != null) builder.world(world);

        String region = getString(config, "requiredRegion", null);
        if (region != null) builder.region(region);

        @SuppressWarnings("unchecked")
        Map<String, Double> coords = (Map<String, Double>) config.get("requiredCoordinates");
        if (coords != null && coords.containsKey("x") && coords.containsKey("y") && coords.containsKey("z")) {
            builder.coordinates(coords.get("x"), coords.get("y"), coords.get("z"));
        }

        double distance = getDouble(config, "requiredDistance");
        if (distance > 0) builder.distance(distance);

        builder.description(getString(config, "description", null));

        return builder.build();
    }

    private PlaytimeRequirement createPlaytimeRequirement(Map<String, Object> config) {
        var builder = RequirementBuilder.playtime();

        long seconds = getLong(config, "requiredPlaytimeSeconds", 0);
        if (seconds > 0) builder.seconds(seconds);

        @SuppressWarnings("unchecked")
        Map<String, Long> worldReqs = (Map<String, Long>) config.get("worldPlaytimeRequirements");
        if (worldReqs != null) {
            worldReqs.forEach(builder::worldPlaytime);
        }

        builder.description(getString(config, "description", null));

        return builder.build();
    }

    private CompositeRequirement createCompositeRequirement(Map<String, Object> config) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> reqConfigs = (List<Map<String, Object>>) config.get("requirements");
        if (reqConfigs == null || reqConfigs.isEmpty()) {
            throw new IllegalArgumentException("No requirements specified for COMPOSITE requirement");
        }

        var builder = RequirementBuilder.composite();

        for (Map<String, Object> reqConfig : reqConfigs) {
            builder.add(fromMap(reqConfig));
        }

        String opStr = getString(config, "operator", "AND");
        switch (opStr.toUpperCase()) {
            case "OR" -> builder.or();
            case "MINIMUM" -> builder.minimum(getInt(config, "minimumRequired", 1));
            default -> builder.and();
        }

        builder.allowPartialProgress(getBoolean(config, "allowPartialProgress", true));
        builder.description(getString(config, "description", null));

        return builder.build();
    }

    private ChoiceRequirement createChoiceRequirement(Map<String, Object> config) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choiceConfigs = (List<Map<String, Object>>) config.get("choices");
        if (choiceConfigs == null || choiceConfigs.isEmpty()) {
            throw new IllegalArgumentException("No choices specified for CHOICE requirement");
        }

        var builder = RequirementBuilder.choice();

        for (Map<String, Object> choiceConfig : choiceConfigs) {
            builder.add(fromMap(choiceConfig));
        }

        builder.minimumRequired(getInt(config, "minimumChoicesRequired", 1));
        builder.mutuallyExclusive(getBoolean(config, "mutuallyExclusive", false));
        builder.allowChoiceChange(getBoolean(config, "allowChoiceChange", true));
        builder.description(getString(config, "description", null));

        return builder.build();
    }

    private TimedRequirement createTimedRequirement(Map<String, Object> config) {
        @SuppressWarnings("unchecked")
        Map<String, Object> delegateConfig = (Map<String, Object>) config.get("delegate");
        if (delegateConfig == null) {
            throw new IllegalArgumentException("No delegate specified for TIME_BASED requirement");
        }

        return RequirementBuilder.timed()
                .delegate(fromMap(delegateConfig))
                .seconds(getLong(config, "timeLimitMillis", 60000) / 1000)
                .autoStart(getBoolean(config, "autoStart", true))
                .description(getString(config, "description", null))
                .build();
    }

    private PluginRequirement createPluginRequirement(Map<String, Object> config) {
        String plugin = getString(config, "plugin", null);
        String category = getString(config, "category", null);

        if ((plugin == null || plugin.isBlank()) && category != null) {
            if ("SKILLS".equalsIgnoreCase(category)) {
                plugin = getString(config, "skillPlugin", null);
            } else if ("JOBS".equalsIgnoreCase(category)) {
                plugin = getString(config, "jobPlugin", null);
            }
        }
        if (plugin == null || plugin.isBlank()) {
            plugin = getString(config, "pluginId", null);
        }
        if (plugin == null || plugin.isBlank()) {
            plugin = getString(config, "integrationId", null);
        }
        if (plugin == null || plugin.isBlank()) {
            plugin = "auto";
        }

        Map<String, Double> values = new HashMap<>();
        values.putAll(parseNumericMap(config.get("values")));

        if (values.isEmpty()) {
            if ("SKILLS".equalsIgnoreCase(category)) {
                values.putAll(parseNumericMap(config.get("skills")));
                if (values.isEmpty()) {
                    String skill = getString(config, "skill", null);
                    int level = getInt(config, "level", 1);
                    if (skill != null && !skill.isBlank()) {
                        values.put(skill, (double) level);
                    }
                }
            } else if ("JOBS".equalsIgnoreCase(category)) {
                values.putAll(parseNumericMap(config.get("jobs")));
                if (values.isEmpty()) {
                    String job = getString(config, "job", null);
                    int level = getInt(config, "level", 1);
                    if (job != null && !job.isBlank()) {
                        values.put(job, (double) level);
                    }
                }
            } else {
                values.putAll(parseNumericMap(config.get("requiredValues")));
                if (values.isEmpty()) {
                    String key = getString(config, "key", null);
                    double value = getDouble(config, "value");
                    if (key != null && !key.isBlank() && value > 0.0D) {
                        values.put(key, value);
                    }
                }
            }
        }

        boolean consumable = getBoolean(config, "consumable", false);
        String description = getString(config, "description", null);

        return new PluginRequirement(plugin, category, values, consumable, description);
    }

    /**
     * Backward compatibility: Convert old JOBS format to new PLUGIN format.
     */
    private PluginRequirement createJobsRequirementCompat(Map<String, Object> config) {
        String pluginStr = getString(config, "jobPlugin", "auto");
        Map<String, Double> values = new HashMap<>();
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> jobs = (Map<String, Integer>) config.get("jobs");
        if (jobs != null && !jobs.isEmpty()) {
            for (Map.Entry<String, Integer> entry : jobs.entrySet()) {
                values.put(entry.getKey(), entry.getValue().doubleValue());
            }
        } else {
            String job = getString(config, "job", null);
            int level = getInt(config, "level", 1);
            if (job != null) {
                values.put(job, (double) level);
            }
        }
        
        String description = getString(config, "description", null);
        return new PluginRequirement(pluginStr.toLowerCase(), "JOBS", values, false, description);
    }

    /**
     * Backward compatibility: Convert old SKILLS format to new PLUGIN format.
     */
    private PluginRequirement createSkillsRequirementCompat(Map<String, Object> config) {
        String pluginStr = getString(config, "skillPlugin", "auto");
        Map<String, Double> values = new HashMap<>();
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> skills = (Map<String, Integer>) config.get("skills");
        if (skills != null && !skills.isEmpty()) {
            for (Map.Entry<String, Integer> entry : skills.entrySet()) {
                values.put(entry.getKey(), entry.getValue().doubleValue());
            }
        } else {
            String skill = getString(config, "skill", null);
            int level = getInt(config, "level", 1);
            if (skill != null) {
                values.put(skill, (double) level);
            }
        }
        
        String description = getString(config, "description", null);
        return new PluginRequirement(pluginStr.toLowerCase(), "SKILLS", values, false, description);
    }

    // ==================== Helper Methods ====================

    @Nullable
    private String getString(Map<String, Object> config, String key, @Nullable String defaultValue) {
        Object value = config.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private int getInt(Map<String, Object> config, String key, int defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try { return Integer.parseInt((String) value); }
            catch (NumberFormatException e) { return defaultValue; }
        }
        return defaultValue;
    }

    private long getLong(Map<String, Object> config, String key, long defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            try { return Long.parseLong((String) value); }
            catch (NumberFormatException e) { return defaultValue; }
        }
        return defaultValue;
    }

    private double getDouble(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try { return Double.parseDouble((String) value); }
            catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    private boolean getBoolean(Map<String, Object> config, String key, boolean defaultValue) {
        Object value = config.get(key);
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return Boolean.parseBoolean((String) value);
        return defaultValue;
    }

    @NotNull
    private Map<String, Double> parseNumericMap(@Nullable Object rawMap) {
        if (!(rawMap instanceof Map<?, ?> map)) {
            return Map.of();
        }

        final Map<String, Double> values = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            if (entry.getValue() instanceof Number number) {
                values.put(entry.getKey().toString(), number.doubleValue());
                continue;
            }
            if (entry.getValue() instanceof String textValue) {
                try {
                    values.put(entry.getKey().toString(), Double.parseDouble(textValue));
                } catch (NumberFormatException ignored) {
                    // Ignore non-numeric map entries
                }
            }
        }
        return values;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private List<ItemStack> getItemStacks(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value == null) return List.of();

        if (value instanceof List<?> list) {
            List<ItemStack> items = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof ItemStack is) {
                    items.add(is);
                } else if (item instanceof Map<?, ?> map) {
                    // Parse from map format
                    ItemStack is = parseItemStackFromMap((Map<String, Object>) map);
                    if (is != null) items.add(is);
                }
            }
            return items;
        }
        return List.of();
    }

    @Nullable
    private ItemStack parseItemStackFromMap(Map<String, Object> map) {
        try {
            String typeStr = getString(map, "type", null);
            if (typeStr == null) return null;

            org.bukkit.Material material = org.bukkit.Material.valueOf(typeStr.toUpperCase());
            int amount = Math.max(1, getInt(map, "amount", 1));
            final ItemStack itemStack = new ItemStack(material, 1);
            itemStack.setAmount(amount);
            return itemStack;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse ItemStack from map", e);
            return null;
        }
    }
}
