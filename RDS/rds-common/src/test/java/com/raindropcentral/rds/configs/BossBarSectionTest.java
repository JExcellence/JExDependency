/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

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
