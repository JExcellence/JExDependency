package com.raindropcentral.rds.configs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link AdminShopSection} parsing and normalization behavior.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class AdminShopSectionTest {

    @Test
    void returnsExpectedDefaultsWhenSectionIsMissing(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, "max_shops: 5");

        final AdminShopSection section = AdminShopSection.fromFile(configFile.toFile());

        assertEquals(AdminShopRestockMode.GRADUAL, section.getRestockMode());
        assertEquals(20L, section.getRestockCheckPeriodTicks());
        assertEquals(1200L, section.getDefaultResetTimerTicks());
        assertEquals(LocalTime.MIDNIGHT, section.getFullRestockTime());
        assertEquals(ZoneId.systemDefault(), section.getTimeZoneId());
    }

    @Test
    void parsesAliasesAndClampsTickValues(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            admin_shops:
              restock_mode: "scheduled_full"
              restock_check_period_ticks: -20
              default_reset_timer_ticks: 0
              full_restock_time: "5:30pm"
              time_zone: "America/New_York"
            """);

        final AdminShopSection section = AdminShopSection.fromFile(configFile.toFile());

        assertEquals(AdminShopRestockMode.FULL_AT_TIME, section.getRestockMode());
        assertEquals(1L, section.getRestockCheckPeriodTicks());
        assertEquals(1L, section.getDefaultResetTimerTicks());
        assertEquals(LocalTime.of(17, 30), section.getFullRestockTime());
        assertEquals(ZoneId.of("America/New_York"), section.getTimeZoneId());
    }

    @Test
    void fallsBackForUnknownValuesAndParsesNoonAlias(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            admin_shops:
              restock_mode: "unsupported"
              full_restock_time: "noon"
              time_zone: "Not/AZone"
            """);

        final AdminShopSection section = AdminShopSection.fromFile(configFile.toFile());

        assertEquals(AdminShopRestockMode.GRADUAL, section.getRestockMode());
        assertEquals(LocalTime.NOON, section.getFullRestockTime());
        assertEquals(ZoneId.systemDefault(), section.getTimeZoneId());
    }
}
