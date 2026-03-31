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

package com.raindropcentral.rdr.view;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that important config paths are exposed as editable entries by the config UI rules.
 */
class StorageConfigViewCoverageTest {

    @Test
    void includesProtectionTaxPathsFromBundledConfig() throws IOException {
        final YamlConfiguration configuration = this.loadBundledConfiguration();
        final Set<String> editablePaths = this.resolveEditablePaths(configuration);

        assertTrue(editablePaths.contains("protection.restricted_storages"));
        assertTrue(editablePaths.contains("protection.taxed_storages"));
        assertTrue(editablePaths.contains("protection.open_storage_taxes.vault"));
        assertTrue(editablePaths.contains("protection.open_storage_taxes.raindrops"));
        assertTrue(editablePaths.contains("protection.filled_storage_taxes.interval_ticks"));
        assertTrue(editablePaths.contains("protection.filled_storage_taxes.maximum_freeze"));
        assertTrue(editablePaths.contains("protection.filled_storage_taxes.maximum_debt.vault"));
        assertTrue(editablePaths.contains("protection.filled_storage_taxes.maximum_debt.raindrops"));
        assertTrue(editablePaths.contains("protection.filled_storage_taxes.currencies.vault"));
        assertTrue(editablePaths.contains("protection.filled_storage_taxes.currencies.raindrops"));
    }

    @Test
    void exposesAllLeafConfigPathsAsEditableEntries() throws IOException {
        final YamlConfiguration configuration = this.loadBundledConfiguration();
        final Set<String> leafPaths = this.resolveLeafPaths(configuration);
        final Set<String> editablePaths = this.resolveEditablePaths(configuration);

        final Map<String, Set<String>> differences = new LinkedHashMap<>();
        final Set<String> missingEditablePaths = new LinkedHashSet<>(leafPaths);
        missingEditablePaths.removeAll(editablePaths);
        final Set<String> unexpectedEditablePaths = new LinkedHashSet<>(editablePaths);
        unexpectedEditablePaths.removeAll(leafPaths);
        differences.put("missing", missingEditablePaths);
        differences.put("unexpected", unexpectedEditablePaths);

        assertEquals(leafPaths, editablePaths, "StorageConfigView editable path coverage mismatch: " + differences);
    }

    private @NotNull YamlConfiguration loadBundledConfiguration() throws IOException {
        final YamlConfiguration configuration;
        try (InputStream configStream = StorageConfigViewCoverageTest.class
            .getClassLoader()
            .getResourceAsStream("config/config.yml")) {
            assertNotNull(configStream, "Bundled config/config.yml was not found on test classpath");
            configuration = YamlConfiguration.loadConfiguration(
                new InputStreamReader(configStream, StandardCharsets.UTF_8)
            );
        }
        return configuration;
    }

    private @NotNull Set<String> resolveEditablePaths(final @NotNull YamlConfiguration configuration) {
        final Set<String> editablePaths = new LinkedHashSet<>();
        for (final Map.Entry<String, Object> entry : configuration.getValues(true).entrySet()) {
            final String path = entry.getKey();
            final Object value = entry.getValue();
            if (path == null || path.isBlank()) {
                continue;
            }
            if (value instanceof ConfigurationSection || value instanceof MemorySection) {
                continue;
            }
            if (!StorageConfigEditSupport.isEditableValue(value)) {
                continue;
            }
            editablePaths.add(path);
        }

        return editablePaths;
    }

    private @NotNull Set<String> resolveLeafPaths(final @NotNull YamlConfiguration configuration) {
        final Set<String> leafPaths = new LinkedHashSet<>();
        for (final Map.Entry<String, Object> entry : configuration.getValues(true).entrySet()) {
            final String path = entry.getKey();
            final Object value = entry.getValue();
            if (path == null || path.isBlank()) {
                continue;
            }
            if (value instanceof ConfigurationSection || value instanceof MemorySection) {
                continue;
            }
            leafPaths.add(path);
        }
        return leafPaths;
    }
}
