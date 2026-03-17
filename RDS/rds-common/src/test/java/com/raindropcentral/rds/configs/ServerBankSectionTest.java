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

/*
 * ServerBankSectionTest.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rds.configs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests server bank section parsing and defaults.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class ServerBankSectionTest {

    @Test
    void usesDefaultsWhenSectionIsMissing(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            max_shops: 10
            """);

        final ServerBankSection section = ServerBankSection.fromFile(configFile.toFile());
        assertTrue(section.isEnabled());
        assertEquals(1200L, section.getTransferIntervalTicks());
    }

    @Test
    void readsConfiguredValues(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            server_bank:
              enabled: false
              transfer_interval_ticks: 2400
            """);

        final ServerBankSection section = ServerBankSection.fromFile(configFile.toFile());
        assertFalse(section.isEnabled());
        assertEquals(2400L, section.getTransferIntervalTicks());
    }

    @Test
    void clampsTransferIntervalToMinimum(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            server_bank:
              enabled: true
              transfer_interval_ticks: 5
            """);

        final ServerBankSection section = ServerBankSection.fromFile(configFile.toFile());
        assertTrue(section.isEnabled());
        assertEquals(20L, section.getTransferIntervalTicks());
    }
}
