package com.raindropcentral.rplatform.statistic;

import com.raindropcentral.rplatform.type.EStatisticType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatisticTypeTest {

    @Test
    @DisplayName("values should mirror legacy EStatisticType metadata")
    void valuesShouldMirrorLegacyMetadata() {
        assertEquals(EStatisticType.values().length, StatisticType.values().length,
                "StatisticType should expose the same number of constants as EStatisticType");

        for (EStatisticType legacy : EStatisticType.values()) {
            final StatisticType modern = StatisticType.valueOf(legacy.name());
            assertAll(
                    () -> assertEquals(legacy.getKey(), modern.getKey(),
                            () -> legacy.name() + " should expose identical storage key"),
                    () -> assertEquals(legacy.getDefaultValue(), modern.getDefaultValue(),
                            () -> legacy.name() + " should expose identical default value"),
                    () -> assertEquals(legacy.getDataType().name(), modern.getDataType().name(),
                            () -> legacy.name() + " should expose identical data type"),
                    () -> assertEquals(legacy.getCategory().name(), modern.getCategory().name(),
                            () -> legacy.name() + " should expose identical category"),
                    () -> assertEquals(legacy.getDescription(), modern.getDescription(),
                            () -> legacy.name() + " should expose identical description")
            );
        }
    }

    @Test
    @DisplayName("getByKey should resolve statistics via storage aliases")
    void getByKeyShouldResolveStatisticsByAlias() {
        for (StatisticType type : StatisticType.values()) {
            assertSame(type, StatisticType.getByKey(type.getKey()),
                    () -> type.name() + " should resolve via getByKey");
            assertSame(EStatisticType.valueOf(type.name()),
                    EStatisticType.getByKey(type.getKey()),
                    () -> type.name() + " alias should stay compatible with legacy enumeration");
        }

        assertNull(StatisticType.getByKey("unknown"),
                "Unknown aliases should return null to match documented contract");
    }

    @Test
    @DisplayName("lookup helpers should stay aligned with legacy filters")
    void lookupHelpersShouldMirrorLegacyBehaviour() {
        for (StatisticType.DataType dataType : StatisticType.DataType.values()) {
            final Set<String> modern = StatisticType.getByDataType(dataType).stream()
                    .map(StatisticType::name)
                    .collect(TreeSet::new, TreeSet::add, TreeSet::addAll);
            final Set<String> legacy = EStatisticType.getByDataType(EStatisticType.StatisticDataType.valueOf(dataType.name()))
                    .stream()
                    .map(EStatisticType::name)
                    .collect(TreeSet::new, TreeSet::add, TreeSet::addAll);

            assertEquals(legacy, modern,
                    () -> "DataType " + dataType + " should match legacy filtering");
        }

        for (StatisticType.Category category : StatisticType.Category.values()) {
            final Set<String> modern = StatisticType.getByCategory(category).stream()
                    .map(StatisticType::name)
                    .collect(TreeSet::new, TreeSet::add, TreeSet::addAll);
            final Set<String> legacy = EStatisticType.getByCategory(EStatisticType.StatisticCategory.valueOf(category.name()))
                    .stream()
                    .map(EStatisticType::name)
                    .collect(TreeSet::new, TreeSet::add, TreeSet::addAll);

            assertEquals(legacy, modern,
                    () -> "Category " + category + " should match legacy filtering");
        }
    }

    @Test
    @DisplayName("getDefaultValuesForCategory should mirror legacy defaults")
    void getDefaultValuesForCategoryShouldMatchLegacyDefaults() {
        for (StatisticType.Category category : StatisticType.Category.values()) {
            final Map<String, Object> modernDefaults = StatisticType.getDefaultValuesForCategory(category);
            final Map<String, Object> legacyDefaults = EStatisticType.getDefaultValuesForCategory(
                    EStatisticType.StatisticCategory.valueOf(category.name()));

            assertEquals(legacyDefaults, modernDefaults,
                    () -> "Default map for category " + category + " should match legacy defaults");
        }
    }

    @Test
    @DisplayName("dynamic perk statistic helpers should namespace and lowercase identifiers")
    void dynamicPerkHelpersShouldNamespaceAndLowercase() {
        final String identifier = "SpeedBoost";

        assertEquals("perk_activation_count_speedboost",
                StatisticType.getPerkActivationCountKey(identifier));
        assertEquals(EStatisticType.getPerkActivationCountKey(identifier),
                StatisticType.getPerkActivationCountKey(identifier));

        assertEquals("perk_last_used_speedboost",
                StatisticType.getPerkLastUsedKey(identifier));
        assertEquals(EStatisticType.getPerkLastUsedKey(identifier),
                StatisticType.getPerkLastUsedKey(identifier));

        assertEquals("perk_usage_time_speedboost",
                StatisticType.getPerkUsageTimeKey(identifier));
        assertEquals(EStatisticType.getPerkUsageTimeKey(identifier),
                StatisticType.getPerkUsageTimeKey(identifier));
    }

    @Test
    @DisplayName("invalid enum lookups should throw IllegalArgumentException as documented")
    void invalidEnumLookupsShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> StatisticType.valueOf("NOT_A_STAT"));
        assertThrows(IllegalArgumentException.class, () -> EStatisticType.valueOf("NOT_A_STAT"));
    }

    @Test
    @DisplayName("dynamic perk helpers should reject null identifiers")
    void dynamicPerkHelpersShouldRejectNull() {
        assertThrows(NullPointerException.class, () -> StatisticType.getPerkActivationCountKey(null));
        assertThrows(NullPointerException.class, () -> StatisticType.getPerkLastUsedKey(null));
        assertThrows(NullPointerException.class, () -> StatisticType.getPerkUsageTimeKey(null));
    }

    @Test
    @DisplayName("enum constants should be retrievable via valueOf")
    void enumConstantsShouldBeRetrievableViaValueOf() {
        for (StatisticType type : StatisticType.values()) {
            assertNotNull(StatisticType.valueOf(type.name()));
        }
    }
}
