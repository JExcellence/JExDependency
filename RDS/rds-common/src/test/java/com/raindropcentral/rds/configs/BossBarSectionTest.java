package com.raindropcentral.rds.configs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link BossBarSection} parsing and default behavior.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class BossBarSectionTest {

    @Test
    void returnsExpectedDefaultsWhenSectionIsMissing(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, "max_shops: 5");

        final BossBarSection section = BossBarSection.fromFile(configFile.toFile());

        assertEquals(10L, section.getUpdatePeriodTicks());
        assertEquals(12, section.getViewDistance());
    }

    @Test
    void parsesConfiguredValues(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            boss_bar:
              update_period_ticks: 60
              view_distance: 20
            """);

        final BossBarSection section = BossBarSection.fromFile(configFile.toFile());

        assertEquals(60L, section.getUpdatePeriodTicks());
        assertEquals(20, section.getViewDistance());
    }

    @Test
    void clampsNonPositiveConfiguredValues(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            boss_bar:
              update_period_ticks: -1
              view_distance: 0
            """);

        final BossBarSection section = BossBarSection.fromFile(configFile.toFile());

        assertEquals(1L, section.getUpdatePeriodTicks());
        assertEquals(1, section.getViewDistance());
    }
}
