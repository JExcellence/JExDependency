package com.raindropcentral.rdr.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.raindropcentral.rdr.configs.ConfigSection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests premium-edition storage service behaviour.
 */
class PremiumStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void preservesConfiguredStorageLimits() throws IOException {
        final PremiumStorageService service = new PremiumStorageService();
        final ConfigSection config = this.createConfig(2, 8);

        assertEquals(8, service.getMaximumStorages(config));
        assertEquals(2, service.getInitialProvisionedStorages(config));
    }

    @Test
    void allowsConfigChanges() {
        final PremiumStorageService service = new PremiumStorageService();

        assertTrue(service.canChangeStorageSettings());
        assertTrue(service.isPremium());
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
