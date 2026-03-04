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
