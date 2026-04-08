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

package com.raindropcentral.rdr.service;

import com.raindropcentral.rdr.configs.ConfigSection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests shared storage service defaults.
 */
class StorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void clampsInitialProvisionedStoragesToEditionMaximum() throws IOException {
        final StorageService service = new FixedStorageService(4);
        final ConfigSection config = this.createConfig(6, 10);

        assertEquals(4, service.getInitialProvisionedStorages(config));
    }

    @Test
    void preservesInitialProvisionedStoragesWhenBelowEditionMaximum() throws IOException {
        final StorageService service = new FixedStorageService(4);
        final ConfigSection config = this.createConfig(2, 10);

        assertEquals(2, service.getInitialProvisionedStorages(config));
    }

    @Test
    void rejectsNullConfigWhenCalculatingInitialProvisioning() {
        final StorageService service = new FixedStorageService(4);

        assertThrows(NullPointerException.class, () -> service.getInitialProvisionedStorages(null));
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

    private static final class FixedStorageService implements StorageService {

        private final int maximumStorages;

        private FixedStorageService(final int maximumStorages) {
            this.maximumStorages = maximumStorages;
        }

        @Override
        public int getMaximumStorages(final ConfigSection config) {
            return this.maximumStorages;
        }

        @Override
        public boolean canChangeStorageSettings() {
            return false;
        }

        @Override
        public boolean isPremium() {
            return false;
        }
    }
}
