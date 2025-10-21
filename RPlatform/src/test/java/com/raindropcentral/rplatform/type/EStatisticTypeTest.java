package com.raindropcentral.rplatform.type;

import com.raindropcentral.rplatform.statistic.StatisticType;
import com.raindropcentral.rplatform.type.EStatisticType.StatisticCategory;
import com.raindropcentral.rplatform.type.EStatisticType.StatisticDataType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class EStatisticTypeTest {

    @ParameterizedTest
    @MethodSource("statisticPairs")
    @DisplayName("Each EStatisticType constant should mirror the corresponding StatisticType metadata")
    void constantsShouldMirrorStatisticTypeMetadata(final EStatisticType type, final StatisticType reference) {
        assertEquals(reference.getKey(), type.getKey(),
            () -> type.name() + " key should equal StatisticType key");
        assertEquals(reference.getDataType().name(), type.getDataType().name(),
            () -> type.name() + " should share the same data type as StatisticType");
        assertEquals(reference.getDefaultValue(), type.getDefaultValue(),
            () -> type.name() + " should expose the same default value as StatisticType");
        assertEquals(reference.getCategory().name(), type.getCategory().name(),
            () -> type.name() + " category should match StatisticType category");
        assertEquals(reference.getDescription(), type.getDescription(),
            () -> type.name() + " description should match StatisticType description");
    }

    private static Stream<Arguments> statisticPairs() {
        return Stream.of(EStatisticType.values())
            .map(type -> Arguments.of(type, StatisticType.valueOf(type.name())));
    }

    @ParameterizedTest
    @MethodSource("dataTypeSamples")
    @DisplayName("isOfType should only return true for the matching data type")
    void isOfTypeShouldMatchExpectedDataType(final EStatisticType type, final StatisticDataType expected) {
        assertTrue(type.isOfType(expected), () -> type.name() + " should match " + expected);
        Stream.of(StatisticDataType.values())
            .filter(other -> other != expected)
            .forEach(other -> assertFalse(type.isOfType(other),
                () -> type.name() + " should not match data type " + other));
    }

    private static Stream<Arguments> dataTypeSamples() {
        return Stream.of(
            Arguments.of(EStatisticType.TUTORIAL_COMPLETED, StatisticDataType.BOOLEAN),
            Arguments.of(EStatisticType.TOTAL_KILLS, StatisticDataType.NUMBER),
            Arguments.of(EStatisticType.CLIENT_VERSION, StatisticDataType.STRING),
            Arguments.of(EStatisticType.LAST_PERK_ACTIVATION, StatisticDataType.TIMESTAMP)
        );
    }

    @ParameterizedTest
    @MethodSource("categorySamples")
    @DisplayName("isInCategory should only return true for the matching category")
    void isInCategoryShouldMatchExpectedCategory(final EStatisticType type, final StatisticCategory category) {
        assertTrue(type.isInCategory(category), () -> type.name() + " should belong to " + category);
        Stream.of(StatisticCategory.values())
            .filter(other -> other != category)
            .forEach(other -> assertFalse(type.isInCategory(other),
                () -> type.name() + " should not belong to " + other));
    }

    private static Stream<Arguments> categorySamples() {
        return Stream.of(
            Arguments.of(EStatisticType.LOGIN_COUNT, StatisticCategory.CORE),
            Arguments.of(EStatisticType.TOTAL_MONEY_EARNED, StatisticCategory.ECONOMY),
            Arguments.of(EStatisticType.PVP_KILLS, StatisticCategory.PVP),
            Arguments.of(EStatisticType.STRUCTURES_BUILT, StatisticCategory.BUILDING),
            Arguments.of(EStatisticType.QUESTS_COMPLETED, StatisticCategory.PROGRESSION),
            Arguments.of(EStatisticType.MESSAGES_SENT, StatisticCategory.SOCIAL),
            Arguments.of(EStatisticType.PERK_USAGE_TIME, StatisticCategory.PERKS)
        );
    }

    @Nested
    @DisplayName("Collection helpers")
    class CollectionHelperTests {

        @ParameterizedTest
        @MethodSource("com.raindropcentral.rplatform.type.EStatisticTypeTest#dataTypeCounts")
        @DisplayName("getByDataType should return statistics constrained to the requested data type")
        void getByDataTypeShouldFilterByType(final StatisticDataType dataType, final long expectedCount) {
            final List<EStatisticType> results = EStatisticType.getByDataType(dataType);
            assertEquals(expectedCount, results.size(),
                () -> "Unexpected statistic count for data type " + dataType);
            assertTrue(results.stream().allMatch(stat -> stat.getDataType() == dataType));
        }

        @ParameterizedTest
        @MethodSource("com.raindropcentral.rplatform.type.EStatisticTypeTest#categoryCounts")
        @DisplayName("getByCategory should return statistics constrained to the requested category")
        void getByCategoryShouldFilterByCategory(final StatisticCategory category, final long expectedCount) {
            final List<EStatisticType> results = EStatisticType.getByCategory(category);
            assertEquals(expectedCount, results.size(),
                () -> "Unexpected statistic count for category " + category);
            assertTrue(results.stream().allMatch(stat -> stat.getCategory() == category));
        }

        @Test
        @DisplayName("getByKey should resolve statistics by key and return null when missing")
        void getByKeyShouldResolveStatisticsByKey() {
            assertEquals(EStatisticType.LOGIN_COUNT, EStatisticType.getByKey("login_count"));
            assertNull(EStatisticType.getByKey("non_existent_statistic"));
        }

        @ParameterizedTest
        @MethodSource("com.raindropcentral.rplatform.type.EStatisticTypeTest#defaultValueExpectations")
        @DisplayName("getDefaultValuesForCategory should map keys to default values for the category")
        void getDefaultValuesForCategoryShouldReturnExpectedDefaults(
            final StatisticCategory category,
            final Map<String, Object> expectedDefaults
        ) {
            assertEquals(expectedDefaults, EStatisticType.getDefaultValuesForCategory(category));
        }

        @Test
        @DisplayName("Shortcut helpers should delegate to category default retrieval")
        void shortcutHelpersShouldDelegateToDefaultRetrieval() {
            assertEquals(EStatisticType.getDefaultValuesForCategory(StatisticCategory.CORE),
                EStatisticType.getCoreDefaults());
            assertEquals(EStatisticType.getDefaultValuesForCategory(StatisticCategory.GAMEPLAY),
                EStatisticType.getGameplayDefaults());
            assertEquals(EStatisticType.getDefaultValuesForCategory(StatisticCategory.PERKS),
                EStatisticType.getPerkDefaults());
            assertEquals(EStatisticType.getByCategory(StatisticCategory.PERKS),
                EStatisticType.getPerkStatistics());
        }
    }

    private static Stream<Arguments> dataTypeCounts() {
        final Map<StatisticDataType, Long> counts = Stream.of(EStatisticType.values())
            .collect(Collectors.groupingBy(EStatisticType::getDataType, Collectors.counting()));
        return Stream.of(StatisticDataType.values())
            .map(dataType -> Arguments.of(dataType, counts.getOrDefault(dataType, 0L)));
    }

    private static Stream<Arguments> categoryCounts() {
        final Map<StatisticCategory, Long> counts = Stream.of(EStatisticType.values())
            .collect(Collectors.groupingBy(EStatisticType::getCategory, Collectors.counting()));
        return Stream.of(StatisticCategory.values())
            .map(category -> Arguments.of(category, counts.getOrDefault(category, 0L)));
    }

    private static Stream<Arguments> defaultValueExpectations() {
        final Map<StatisticCategory, Map<String, Object>> defaults = new EnumMap<>(StatisticCategory.class);
        Stream.of(StatisticType.values()).forEach(stat ->
            defaults
                .computeIfAbsent(StatisticCategory.valueOf(stat.getCategory().name()), ignored -> new LinkedHashMap<>())
                .put(stat.getKey(), stat.getDefaultValue()));

        Stream.of(StatisticCategory.values())
            .filter(category -> !defaults.containsKey(category))
            .forEach(category -> defaults.put(category, Map.of()));

        return defaults.entrySet().stream()
            .map(entry -> Arguments.of(entry.getKey(), entry.getValue()));
    }

    @Nested
    @DisplayName("Dynamic perk statistic keys")
    class PerkDynamicKeyTests {

        @Test
        @DisplayName("Perk helper methods should normalise identifiers and append expected prefixes")
        void perkHelperMethodsShouldNormaliseIdentifiers() {
            final String identifier = "SpeedBoost";
            assertEquals("perk_activation_count_speedboost", EStatisticType.getPerkActivationCountKey(identifier));
            assertEquals("perk_last_used_speedboost", EStatisticType.getPerkLastUsedKey(identifier));
            assertEquals("perk_usage_time_speedboost", EStatisticType.getPerkUsageTimeKey(identifier));
        }

        @Test
        @DisplayName("Perk helper methods should respect already lowercase identifiers")
        void perkHelperMethodsShouldRespectLowercaseIdentifiers() {
            final String identifier = "regen";
            assertEquals("perk_activation_count_regen", EStatisticType.getPerkActivationCountKey(identifier));
            assertEquals("perk_last_used_regen", EStatisticType.getPerkLastUsedKey(identifier));
            assertEquals("perk_usage_time_regen", EStatisticType.getPerkUsageTimeKey(identifier));
        }
    }
}
