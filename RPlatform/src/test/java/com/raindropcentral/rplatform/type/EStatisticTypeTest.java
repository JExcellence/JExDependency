package com.raindropcentral.rplatform.type;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests statistics lookup and helper methods in {@link EStatisticType}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class EStatisticTypeTest {

    @Test
    void resolvesStatisticsByKey() {
        assertEquals(EStatisticType.JOIN_DATE, EStatisticType.getByKey("join_date"));
        assertNull(EStatisticType.getByKey("does_not_exist"));
    }

    @Test
    void filtersByDataTypeAndCategory() {
        assertTrue(
            EStatisticType.getByDataType(EStatisticType.StatisticDataType.NUMBER)
                .contains(EStatisticType.CURRENT_BALANCE)
        );
        assertFalse(
            EStatisticType.getByDataType(EStatisticType.StatisticDataType.BOOLEAN)
                .contains(EStatisticType.CURRENT_BALANCE)
        );
        assertTrue(
            EStatisticType.getByCategory(EStatisticType.StatisticCategory.PERKS)
                .contains(EStatisticType.TOTAL_PERKS_ACTIVATED)
        );
    }

    @Test
    void categoryDefaultsContainExpectedKeysAndValues() {
        final Map<String, Object> coreDefaults = EStatisticType.getCoreDefaults();
        final Map<String, Object> perkDefaults = EStatisticType.getPerkDefaults();

        assertEquals(0L, coreDefaults.get("join_date"));
        assertEquals(0.0D, perkDefaults.get("total_perks_activated"));
        assertFalse(coreDefaults.containsKey("total_perks_activated"));
    }

    @Test
    void buildsDynamicPerkStatisticKeysInLowerCase() {
        assertEquals(
            "perk_activation_count_speedboost",
            EStatisticType.getPerkActivationCountKey("SpeedBoost")
        );
        assertEquals(
            "perk_last_used_speedboost",
            EStatisticType.getPerkLastUsedKey("SpeedBoost")
        );
        assertEquals(
            "perk_usage_time_speedboost",
            EStatisticType.getPerkUsageTimeKey("SpeedBoost")
        );
    }
}
