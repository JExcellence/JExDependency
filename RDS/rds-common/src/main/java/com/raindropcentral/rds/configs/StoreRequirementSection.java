package com.raindropcentral.rds.configs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Raw configuration wrapper for a single shop-store requirement definition.
 *
 * <p>The wrapped map mirrors the YAML structure consumed by the RPlatform requirement factory.
 * RDS keeps the original definition intact so store requirements can use any valid RPlatform
 * requirement type while still exposing the local config key and icon metadata.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class StoreRequirementSection {

    private final String key;
    private final String type;
    private final StoreRequirementIconSection icon;
    private final Map<String, Object> definition;

    /**
     * Creates a requirement section wrapper.
     *
     * @param key requirement identifier used in config order and diagnostics
     * @param type configured requirement type identifier
     * @param icon configured icon metadata
     * @param definition raw requirement definition
     * @throws NullPointerException if {@code key}, {@code icon}, or {@code definition} is {@code null}
     */
    public StoreRequirementSection(
        final @NotNull String key,
        final @NotNull String type,
        final @NotNull StoreRequirementIconSection icon,
        final @NotNull Map<String, Object> definition
    ) {
        final Map<String, Object> requirementDefinition = Objects.requireNonNull(definition, "definition");
        this.key = Objects.requireNonNull(key, "key");
        this.type = resolveType(type, requirementDefinition);
        this.icon = Objects.requireNonNull(icon, "icon");
        this.definition = deepCopyMap(requirementDefinition);
    }

    /**
     * Creates a requirement section from a YAML configuration section.
     *
     * @param key config key for the requirement
     * @param section YAML section containing the requirement definition
     * @return parsed requirement section
     * @throws NullPointerException if {@code key} or {@code section} is {@code null}
     */
    public static @NotNull StoreRequirementSection fromConfigurationSection(
        final @NotNull String key,
        final @NotNull ConfigurationSection section
    ) {
        final Map<String, Object> definition = convertSection(section);
        final String type = section.getString("type", "UNKNOWN");
        final ConfigurationSection iconSection = section.getConfigurationSection("icon");
        final StoreRequirementIconSection icon = iconSection == null
            ? new StoreRequirementIconSection("PAPER")
            : StoreRequirementIconSection.fromConfigurationSection(iconSection);
        return new StoreRequirementSection(key, type, icon, definition);
    }

    /**
     * Creates a currency requirement section.
     *
     * @param key config key for the requirement
     * @param currencyType configured economy identifier
     * @param amount required currency amount
     * @param iconType configured icon material
     * @return currency requirement section
     */
    public static @NotNull StoreRequirementSection currency(
        final @NotNull String key,
        final @NotNull String currencyType,
        final double amount,
        final @NotNull String iconType
    ) {
        final Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("type", "CURRENCY");
        final Map<String, Object> icon = new LinkedHashMap<>();
        icon.put("type", iconType);
        definition.put("icon", icon);
        definition.put("currency", normalizeCurrencyType(currencyType));
        definition.put("amount", Math.max(0D, amount));
        definition.put("consumable", true);
        return new StoreRequirementSection(
            key,
            "CURRENCY",
            new StoreRequirementIconSection(iconType),
            definition
        );
    }

    /**
     * Returns the config key assigned to this requirement.
     *
     * @return requirement config key
     */
    public @NotNull String getKey() {
        return this.key;
    }

    /**
     * Returns the configured requirement type identifier.
     *
     * @return normalized requirement type identifier, or {@code "UNKNOWN"} when no type exists
     */
    public @NotNull String getType() {
        return this.type;
    }

    /**
     * Returns the configured icon metadata for this requirement.
     *
     * @return icon section for this requirement
     */
    public @NotNull StoreRequirementIconSection getIcon() {
        return this.icon;
    }

    /**
     * Returns a defensive copy of the wrapped requirement map.
     *
     * @return copied requirement definition
     */
    public @NotNull Map<String, Object> toRequirementMap() {
        final Map<String, Object> copy = normalizeRequirementDefinition(
            deepCopyMap(this.definition),
            this.type
        );
        copy.remove("icon");
        return copy;
    }

    /**
     * Returns the configured description when the requirement provides one.
     *
     * @return configured description, or {@code null} when absent
     */
    public @Nullable String getDescription() {
        final Object description = this.definition.get("description");
        return description == null ? null : description.toString();
    }

    private static @NotNull Map<String, Object> convertSection(final @NotNull ConfigurationSection section) {
        final Map<String, Object> values = new LinkedHashMap<>();
        for (final String key : section.getKeys(false)) {
            values.put(key, normalizeValue(section.get(key)));
        }
        return values;
    }

    private static @Nullable Object normalizeValue(final @Nullable Object value) {
        if (value instanceof ConfigurationSection nestedSection) {
            return convertSection(nestedSection);
        }

        if (value instanceof Map<?, ?> map) {
            final Map<String, Object> normalized = new LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    normalized.put(entry.getKey().toString(), normalizeValue(entry.getValue()));
                }
            }
            return normalized;
        }

        if (value instanceof List<?> list) {
            final List<Object> normalized = new ArrayList<>(list.size());
            for (final Object entry : list) {
                normalized.add(normalizeValue(entry));
            }
            return normalized;
        }

        return value;
    }

    private static @NotNull Map<String, Object> deepCopyMap(final @NotNull Map<String, Object> source) {
        final Map<String, Object> copy = new LinkedHashMap<>();
        for (final Map.Entry<String, Object> entry : source.entrySet()) {
            copy.put(entry.getKey(), deepCopyValue(entry.getValue()));
        }
        return copy;
    }

    private static @Nullable Object deepCopyValue(final @Nullable Object value) {
        if (value instanceof Map<?, ?> map) {
            final Map<String, Object> nested = new LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    nested.put(entry.getKey().toString(), deepCopyValue(entry.getValue()));
                }
            }
            return nested;
        }

        if (value instanceof List<?> list) {
            final List<Object> nested = new ArrayList<>(list.size());
            for (final Object entry : list) {
                nested.add(deepCopyValue(entry));
            }
            return nested;
        }

        return value;
    }

    private static @NotNull String normalizeCurrencyType(final @Nullable String currencyType) {
        if (currencyType == null || currencyType.isBlank()) {
            return "vault";
        }

        return currencyType.trim().toLowerCase(Locale.ROOT);
    }

    private static @NotNull String normalizeType(final @Nullable String type) {
        if (type == null || type.isBlank()) {
            return "UNKNOWN";
        }

        return type.trim().toUpperCase(Locale.ROOT);
    }

    private static @NotNull String resolveType(
        final @Nullable String configuredType,
        final @NotNull Map<String, Object> definition
    ) {
        final String normalizedConfiguredType = normalizeType(configuredType);
        if (!"UNKNOWN".equals(normalizedConfiguredType)) {
            return normalizedConfiguredType;
        }

        if (definition.containsKey("itemRequirement")) {
            return "ITEM";
        }
        if (definition.containsKey("currencyRequirement")) {
            return "CURRENCY";
        }
        if (definition.containsKey("experienceRequirement")) {
            return "EXPERIENCE_LEVEL";
        }
        if (definition.containsKey("permissionRequirement")) {
            return "PERMISSION";
        }
        if (definition.containsKey("locationRequirement")) {
            return "LOCATION";
        }
        if (definition.containsKey("playtimeRequirement")) {
            return "PLAYTIME";
        }
        if (definition.containsKey("compositeRequirement")) {
            return "COMPOSITE";
        }
        if (definition.containsKey("choiceRequirement")) {
            return "CHOICE";
        }
        if (definition.containsKey("timeBasedRequirement")) {
            return "TIME_BASED";
        }
        if (definition.containsKey("pluginRequirement")) {
            return "PLUGIN";
        }

        return normalizedConfiguredType;
    }

    private static @NotNull Map<String, Object> normalizeRequirementDefinition(
        final @NotNull Map<String, Object> source,
        final @NotNull String type
    ) {
        Map<String, Object> normalized = flattenNestedRequirementSection(source, type);
        normalized.put("type", type);

        return switch (type) {
            case "ITEM" -> normalizeItemRequirement(normalized);
            case "CURRENCY" -> normalizeCurrencyRequirement(normalized);
            case "EXPERIENCE_LEVEL" -> normalizeExperienceRequirement(normalized);
            case "PERMISSION" -> normalizePermissionRequirement(normalized);
            case "LOCATION" -> normalizeLocationRequirement(normalized);
            case "PLAYTIME" -> normalizePlaytimeRequirement(normalized);
            case "COMPOSITE" -> normalizeCompositeRequirement(normalized);
            case "CHOICE" -> normalizeChoiceRequirement(normalized);
            case "TIME_BASED" -> normalizeTimedRequirement(normalized);
            case "PLUGIN" -> normalizePluginRequirement(normalized);
            default -> normalized;
        };
    }

    private static @NotNull Map<String, Object> flattenNestedRequirementSection(
        final @NotNull Map<String, Object> source,
        final @NotNull String type
    ) {
        final String nestedKey = switch (type) {
            case "ITEM" -> "itemRequirement";
            case "CURRENCY" -> "currencyRequirement";
            case "EXPERIENCE_LEVEL" -> "experienceRequirement";
            case "PERMISSION" -> "permissionRequirement";
            case "LOCATION" -> "locationRequirement";
            case "PLAYTIME" -> "playtimeRequirement";
            case "COMPOSITE" -> "compositeRequirement";
            case "CHOICE" -> "choiceRequirement";
            case "TIME_BASED" -> "timeBasedRequirement";
            case "PLUGIN" -> "pluginRequirement";
            default -> null;
        };

        if (nestedKey == null) {
            return source;
        }

        final Object nestedValue = source.get(nestedKey);
        if (!(nestedValue instanceof Map<?, ?> nestedMapValue)) {
            return source;
        }

        final Map<String, Object> flattened = toStringObjectMap(nestedMapValue);
        for (final Map.Entry<String, Object> entry : source.entrySet()) {
            if (nestedKey.equals(entry.getKey()) || flattened.containsKey(entry.getKey())) {
                continue;
            }
            flattened.put(entry.getKey(), deepCopyValue(entry.getValue()));
        }
        return flattened;
    }

    private static @NotNull Map<String, Object> normalizeItemRequirement(final @NotNull Map<String, Object> source) {
        if (source.containsKey("item") && !source.containsKey("requiredItems")) {
            source.put("requiredItems", List.of(deepCopyValue(source.get("item"))));
        } else if (source.containsKey("requiredItem") && !source.containsKey("requiredItems")) {
            source.put("requiredItems", List.of(deepCopyValue(source.get("requiredItem"))));
        }

        if (source.get("requiredItems") instanceof Map<?, ?> namedItems) {
            source.put("requiredItems", convertNamedEntriesToList(namedItems));
        }
        if (source.get("items") instanceof Map<?, ?> namedItems) {
            source.put("items", convertNamedEntriesToList(namedItems));
        }
        if (!source.containsKey("consumeOnComplete") && source.containsKey("consumable")) {
            source.put("consumeOnComplete", source.get("consumable"));
        }

        source.remove("item");
        source.remove("requiredItem");
        return source;
    }

    private static @NotNull Map<String, Object> normalizeCurrencyRequirement(final @NotNull Map<String, Object> source) {
        if (source.get("requiredCurrencies") instanceof Map<?, ?> currencies && !source.containsKey("currency")) {
            for (final Map.Entry<String, Object> entry : toStringObjectMap(currencies).entrySet()) {
                if (entry.getValue() instanceof Number amount) {
                    source.put("currency", entry.getKey());
                    source.put("amount", amount.doubleValue());
                    break;
                }
            }
        }
        if (!source.containsKey("currency") && source.containsKey("currencyId")) {
            source.put("currency", source.get("currencyId"));
        }
        if (!source.containsKey("consumable") && source.containsKey("consumeOnComplete")) {
            source.put("consumable", source.get("consumeOnComplete"));
        }
        source.remove("requiredCurrencies");
        source.remove("currencyId");
        return source;
    }

    private static @NotNull Map<String, Object> normalizeExperienceRequirement(final @NotNull Map<String, Object> source) {
        if (!source.containsKey("experienceType") && source.containsKey("requiredType")) {
            source.put("experienceType", source.get("requiredType"));
        }
        return source;
    }

    private static @NotNull Map<String, Object> normalizePermissionRequirement(final @NotNull Map<String, Object> source) {
        if (!source.containsKey("permissionMode") && source.containsKey("requireAll")) {
            final boolean requireAll = Boolean.TRUE.equals(source.get("requireAll"));
            source.put("permissionMode", requireAll ? "ALL" : "ANY");
        }
        if (!source.containsKey("checkNegated") && source.containsKey("checkNegation")) {
            source.put("checkNegated", source.get("checkNegation"));
        }
        return source;
    }

    private static @NotNull Map<String, Object> normalizeLocationRequirement(final @NotNull Map<String, Object> source) {
        if (!source.containsKey("requiredWorld") && source.containsKey("world")) {
            source.put("requiredWorld", source.get("world"));
        }
        if (!source.containsKey("requiredRegion") && source.containsKey("region")) {
            source.put("requiredRegion", source.get("region"));
        }
        if (!source.containsKey("requiredCoordinates")) {
            if (source.get("coordinates") instanceof Map<?, ?> coordinates) {
                source.put("requiredCoordinates", toDoubleMap(coordinates));
            } else {
                final Map<String, Double> coordinates = new LinkedHashMap<>();
                putDoubleIfPresent(coordinates, "x", source.get("x"));
                putDoubleIfPresent(coordinates, "y", source.get("y"));
                putDoubleIfPresent(coordinates, "z", source.get("z"));
                if (coordinates.size() == 3) {
                    source.put("requiredCoordinates", coordinates);
                }
            }
        } else if (source.get("requiredCoordinates") instanceof Map<?, ?> coordinates) {
            source.put("requiredCoordinates", toDoubleMap(coordinates));
        }
        if (!source.containsKey("requiredDistance") && source.containsKey("distance")) {
            source.put("requiredDistance", source.get("distance"));
        }

        source.remove("world");
        source.remove("region");
        source.remove("coordinates");
        source.remove("distance");
        source.remove("x");
        source.remove("y");
        source.remove("z");
        source.remove("exactLocation");
        return source;
    }

    private static @NotNull Map<String, Object> normalizePlaytimeRequirement(final @NotNull Map<String, Object> source) {
        if (!source.containsKey("requiredPlaytimeSeconds")) {
            final Long requiredPlaytimeSeconds = resolveDurationSeconds(
                source,
                "requiredPlaytimeSeconds",
                "requiredPlaytimeMinutes",
                "requiredPlaytimeHours",
                "requiredPlaytimeDays",
                "time",
                "timeUnit"
            );
            if (requiredPlaytimeSeconds != null) {
                source.put("requiredPlaytimeSeconds", requiredPlaytimeSeconds);
            }
        }

        if (!source.containsKey("worldPlaytimeRequirements")
            && source.get("worlds") instanceof List<?> worlds
        ) {
            final Long worldPlaytimeSeconds = resolveDurationSeconds(
                source,
                "worldPlaytimeSeconds",
                "worldPlaytimeMinutes",
                "worldPlaytimeHours",
                "worldPlaytimeDays",
                null,
                null
            );
            if (worldPlaytimeSeconds != null && worldPlaytimeSeconds > 0L) {
                final Map<String, Long> worldRequirements = new LinkedHashMap<>();
                for (final Object world : worlds) {
                    if (world == null) {
                        continue;
                    }
                    final String worldName = world.toString().trim();
                    if (!worldName.isEmpty()) {
                        worldRequirements.put(worldName, worldPlaytimeSeconds);
                    }
                }
                if (!worldRequirements.isEmpty()) {
                    source.put("worldPlaytimeRequirements", worldRequirements);
                }
            }
        } else if (source.get("worldPlaytimeRequirements") instanceof Map<?, ?> worldRequirements) {
            source.put("worldPlaytimeRequirements", toLongMap(worldRequirements));
        }

        if (!source.containsKey("useTotalPlaytime") && source.containsKey("worldPlaytimeRequirements")) {
            source.put("useTotalPlaytime", false);
        }

        source.remove("requiredPlaytimeMinutes");
        source.remove("requiredPlaytimeHours");
        source.remove("requiredPlaytimeDays");
        source.remove("time");
        source.remove("timeUnit");
        source.remove("worlds");
        source.remove("worldPlaytimeSeconds");
        source.remove("worldPlaytimeMinutes");
        source.remove("worldPlaytimeHours");
        source.remove("worldPlaytimeDays");
        return source;
    }

    private static @NotNull Map<String, Object> normalizeCompositeRequirement(final @NotNull Map<String, Object> source) {
        if (source.get("compositeRequirements") instanceof List<?> compositeRequirements) {
            source.put("requirements", normalizeRequirementList(compositeRequirements));
        } else if (source.get("subRequirements") instanceof Map<?, ?> namedRequirements) {
            source.put("requirements", normalizeRequirementList(new ArrayList<>(toStringObjectMap(namedRequirements).values())));
        } else if (source.get("requirements") instanceof Map<?, ?> namedRequirements) {
            source.put("requirements", normalizeRequirementList(new ArrayList<>(toStringObjectMap(namedRequirements).values())));
        }
        if (!source.containsKey("operator") && source.containsKey("compositeOperator")) {
            source.put("operator", source.get("compositeOperator"));
        }
        source.remove("compositeRequirements");
        source.remove("subRequirements");
        source.remove("compositeOperator");
        return source;
    }

    private static @NotNull Map<String, Object> normalizeChoiceRequirement(final @NotNull Map<String, Object> source) {
        if (source.get("choices") instanceof Map<?, ?> namedChoices) {
            source.put("choices", normalizeRequirementList(new ArrayList<>(toStringObjectMap(namedChoices).values())));
        } else if (source.get("choices") instanceof List<?> choices) {
            source.put("choices", normalizeRequirementList(choices));
        }
        if (!source.containsKey("minimumChoicesRequired") && source.containsKey("minimumRequired")) {
            source.put("minimumChoicesRequired", source.get("minimumRequired"));
        }
        return source;
    }

    private static @NotNull Map<String, Object> normalizeTimedRequirement(final @NotNull Map<String, Object> source) {
        if (!source.containsKey("timeLimitMillis")) {
            final Long timeLimitSeconds = resolveDurationSeconds(
                source,
                "timeConstraintSeconds",
                "timeLimitMinutes",
                "timeLimitHours",
                "timeLimitDays",
                "timeLimitSeconds",
                null
            );
            if (timeLimitSeconds != null) {
                source.put("timeLimitMillis", timeLimitSeconds * 1000L);
            }
        }
        if (!source.containsKey("delegate") && source.get("requirement") instanceof Map<?, ?> requirement) {
            source.put("delegate", requirement);
        }
        if (source.get("delegate") instanceof Map<?, ?> delegateRequirement) {
            source.put("delegate", normalizeRequirementMap(delegateRequirement));
        }
        source.remove("timeConstraintSeconds");
        source.remove("timeLimitSeconds");
        source.remove("timeLimitMinutes");
        source.remove("timeLimitHours");
        source.remove("timeLimitDays");
        source.remove("requirement");
        return source;
    }

    private static @NotNull Map<String, Object> normalizePluginRequirement(final @NotNull Map<String, Object> source) {
        if (!source.containsKey("plugin") && source.containsKey("pluginId")) {
            source.put("plugin", source.get("pluginId"));
        }
        if (!source.containsKey("plugin") && source.containsKey("integrationId")) {
            source.put("plugin", source.get("integrationId"));
        }
        if (!source.containsKey("plugin") && source.containsKey("skillPlugin")) {
            source.put("plugin", source.get("skillPlugin"));
            source.putIfAbsent("category", "SKILLS");
        }
        if (!source.containsKey("plugin") && source.containsKey("jobPlugin")) {
            source.put("plugin", source.get("jobPlugin"));
            source.putIfAbsent("category", "JOBS");
        }
        if (!source.containsKey("values") && source.get("requiredValues") instanceof Map<?, ?> requiredValues) {
            source.put("values", toDoubleMap(requiredValues));
        }
        if (!source.containsKey("values") && source.get("skills") instanceof Map<?, ?> skills) {
            source.put("values", toDoubleMap(skills));
            source.putIfAbsent("category", "SKILLS");
        }
        if (!source.containsKey("values") && source.get("jobs") instanceof Map<?, ?> jobs) {
            source.put("values", toDoubleMap(jobs));
            source.putIfAbsent("category", "JOBS");
        }
        if (!source.containsKey("values")
            && source.containsKey("key")
            && source.get("value") != null
        ) {
            final Double value = toDouble(source.get("value"));
            if (value != null) {
                source.put("values", Map.of(source.get("key").toString(), value));
            }
        }
        if (!source.containsKey("values")
            && source.containsKey("skill")
            && source.get("level") != null
        ) {
            final Double value = toDouble(source.get("level"));
            if (value != null) {
                source.put("values", Map.of(source.get("skill").toString(), value));
                source.putIfAbsent("category", "SKILLS");
            }
        }
        if (!source.containsKey("values")
            && source.containsKey("job")
            && source.get("level") != null
        ) {
            final Double value = toDouble(source.get("level"));
            if (value != null) {
                source.put("values", Map.of(source.get("job").toString(), value));
                source.putIfAbsent("category", "JOBS");
            }
        }
        if (!source.containsKey("consumable") && source.containsKey("consumeOnComplete")) {
            source.put("consumable", source.get("consumeOnComplete"));
        }
        source.remove("pluginId");
        source.remove("integrationId");
        source.remove("skillPlugin");
        source.remove("jobPlugin");
        source.remove("requiredValues");
        source.remove("skills");
        source.remove("jobs");
        source.remove("skill");
        source.remove("job");
        source.remove("level");
        source.remove("key");
        source.remove("value");
        return source;
    }

    private static @NotNull List<Object> normalizeRequirementList(final @NotNull List<?> source) {
        final List<Object> normalized = new ArrayList<>(source.size());
        for (final Object entry : source) {
            if (entry instanceof Map<?, ?> entryMap) {
                normalized.add(normalizeRequirementMap(entryMap));
            } else {
                normalized.add(deepCopyValue(entry));
            }
        }
        return normalized;
    }

    private static @NotNull Map<String, Object> normalizeRequirementMap(final @NotNull Map<?, ?> source) {
        final Map<String, Object> requirement = toStringObjectMap(source);
        final Object type = requirement.get("type");
        return normalizeRequirementDefinition(
            requirement,
            resolveType(type == null ? null : type.toString(), requirement)
        );
    }

    private static @NotNull List<Object> convertNamedEntriesToList(final @NotNull Map<?, ?> namedEntries) {
        final List<Object> entries = new ArrayList<>(namedEntries.size());
        for (final Object value : namedEntries.values()) {
            entries.add(deepCopyValue(value));
        }
        return entries;
    }

    private static @NotNull Map<String, Object> toStringObjectMap(final @NotNull Map<?, ?> source) {
        final Map<String, Object> normalized = new LinkedHashMap<>();
        for (final Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            normalized.put(entry.getKey().toString(), deepCopyValue(entry.getValue()));
        }
        return normalized;
    }

    private static void putDoubleIfPresent(
        final @NotNull Map<String, Double> target,
        final @NotNull String key,
        final @Nullable Object value
    ) {
        final Double normalizedValue = toDouble(value);
        if (normalizedValue != null) {
            target.put(key, normalizedValue);
        }
    }

    private static @Nullable Long resolveDurationSeconds(
        final @NotNull Map<String, Object> source,
        final @Nullable String secondsKey,
        final @Nullable String minutesKey,
        final @Nullable String hoursKey,
        final @Nullable String daysKey,
        final @Nullable String genericKey,
        final @Nullable String genericUnitKey
    ) {
        final Long seconds = secondsKey == null ? null : toLong(source.get(secondsKey));
        if (seconds != null && seconds > 0L) {
            return seconds;
        }

        final Long minutes = minutesKey == null ? null : toLong(source.get(minutesKey));
        if (minutes != null && minutes > 0L) {
            return minutes * 60L;
        }

        final Long hours = hoursKey == null ? null : toLong(source.get(hoursKey));
        if (hours != null && hours > 0L) {
            return hours * 3600L;
        }

        final Long days = daysKey == null ? null : toLong(source.get(daysKey));
        if (days != null && days > 0L) {
            return days * 86400L;
        }

        if (genericKey == null) {
            return null;
        }

        final Long genericDuration = toLong(source.get(genericKey));
        if (genericDuration == null || genericDuration <= 0L) {
            return null;
        }

        final String unit = genericUnitKey == null || source.get(genericUnitKey) == null
            ? "seconds"
            : source.get(genericUnitKey).toString().trim().toLowerCase(Locale.ROOT);
        return switch (unit) {
            case "minutes", "minute", "min", "m" -> genericDuration * 60L;
            case "hours", "hour", "hr", "h" -> genericDuration * 3600L;
            case "days", "day", "d" -> genericDuration * 86400L;
            case "weeks", "week", "w" -> genericDuration * 604800L;
            default -> genericDuration;
        };
    }

    private static @Nullable Double toDouble(final @Nullable Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static @Nullable Long toLong(final @Nullable Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static @NotNull Map<String, Double> toDoubleMap(final @NotNull Map<?, ?> source) {
        final Map<String, Double> normalized = new LinkedHashMap<>();
        for (final Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            final Double value = toDouble(entry.getValue());
            if (value != null) {
                normalized.put(entry.getKey().toString(), value);
            }
        }
        return normalized;
    }

    private static @NotNull Map<String, Long> toLongMap(final @NotNull Map<?, ?> source) {
        final Map<String, Long> normalized = new LinkedHashMap<>();
        for (final Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            final Long value = toLong(entry.getValue());
            if (value != null) {
                normalized.put(entry.getKey().toString(), value);
            }
        }
        return normalized;
    }
}
