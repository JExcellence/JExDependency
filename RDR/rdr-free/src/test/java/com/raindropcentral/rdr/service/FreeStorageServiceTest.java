/*
 * FreeStorageServiceTest.java
 *
 * @author ItsRainingHP
 * @version 5.0.0
 */

package com.raindropcentral.rdr.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.raindropcentral.rdr.configs.ConfigSection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests free-edition storage service limits.
 */
class FreeStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void capsMaximumStoragesAtThree() throws IOException {
        final FreeStorageService service = new FreeStorageService();
        final ConfigSection config = this.createConfig(5, 10);

        assertEquals(3, service.getMaximumStorages(config));
        assertEquals(3, service.getInitialProvisionedStorages(config));
    }

    @Test
    void blocksConfigChanges() {
        final FreeStorageService service = new FreeStorageService();

        assertFalse(service.canChangeStorageSettings());
        assertFalse(service.isPremium());
    }

    private ConfigSection createConfig(
        final int startingStorages,
        final int maxStorages
    ) throws IOException {
        final Path configFile = this.tempDir.resolve("config.yml");
        Files.writeString(
            configFile,
            """
            starting_storages: %d
            max_storages: %d
            """.formatted(startingStorages, maxStorages)
        );
        return ConfigSection.fromFile(configFile.toFile());
    }
}
