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

package com.raindropcentral.core.service.statistics.queue;

import com.raindropcentral.rplatform.type.EStatisticType.StatisticDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for QueuePersistenceManager, focusing on WAL corruption handling.
 *
 * @author JExcellence
 * @since 1.0.0
 */
class QueuePersistenceManagerTest {

    @TempDir
    Path tempDir;

    private QueuePersistenceManager manager;
    private Path walFile;

    @BeforeEach
    void setUp() {
        manager = new QueuePersistenceManager(tempDir);
        walFile = tempDir.resolve("statistics-queue.wal");
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up test files
        Files.deleteIfExists(walFile);
        Files.deleteIfExists(tempDir.resolve("statistics-queue.json"));
        Files.deleteIfExists(tempDir.resolve("statistics-queue.json.backup"));
    }

    /**
     * Tests that valid WAL entries are loaded correctly.
     */
    @Test
    void testLoadValidWalEntries() throws IOException {
        // Create valid WAL entries
        createWalFile(
            "{\"playerUuid\":\"550e8400-e29b-41d4-a716-446655440000\",\"statisticKey\":\"minecraft.blocks.mined.stone\",\"value\":100,\"dataType\":\"NUMBER\",\"collectionTimestamp\":1234567890,\"priority\":\"NORMAL\",\"isDelta\":true,\"sourcePlugin\":\"RCore\"}",
            "{\"playerUuid\":\"550e8400-e29b-41d4-a716-446655440001\",\"statisticKey\":\"minecraft.travel.walk\",\"value\":5000,\"dataType\":\"NUMBER\",\"collectionTimestamp\":1234567891,\"priority\":\"LOW\",\"isDelta\":false,\"sourcePlugin\":\"RCore\"}"
        );

        Map<DeliveryPriority, List<QueuedStatistic>> loaded = manager.load();

        assertEquals(1, loaded.get(DeliveryPriority.NORMAL).size());
        assertEquals(1, loaded.get(DeliveryPriority.LOW).size());
        
        QueuedStatistic stat = loaded.get(DeliveryPriority.NORMAL).get(0);
        assertEquals("minecraft.blocks.mined.stone", stat.statisticKey());
        assertEquals(100.0, stat.value());
    }

    /**
     * Tests that corrupted WAL entries are skipped gracefully.
     */
    @Test
    void testSkipCorruptedWalEntries() throws IOException {
        // Create WAL with mix of valid and corrupted entries
        createWalFile(
            "{\"playerUuid\":\"550e8400-e29b-41d4-a716-446655440000\",\"statisticKey\":\"minecraft.blocks.mined.stone\",\"value\":100,\"dataType\":\"NUMBER\",\"collectionTimestamp\":1234567890,\"priority\":\"NORMAL\",\"isDelta\":true,\"sourcePlugin\":\"RCore\"}",
            "invalid json line",
            "{incomplete json",
            "",
            "   ",
            "not a json object at all",
            "{\"playerUuid\":\"550e8400-e29b-41d4-a716-446655440001\",\"statisticKey\":\"minecraft.travel.walk\",\"value\":5000,\"dataType\":\"NUMBER\",\"collectionTimestamp\":1234567891,\"priority\":\"LOW\",\"isDelta\":false,\"sourcePlugin\":\"RCore\"}"
        );

        Map<DeliveryPriority, List<QueuedStatistic>> loaded = manager.load();

        // Should load only the 2 valid entries
        assertEquals(1, loaded.get(DeliveryPriority.NORMAL).size());
        assertEquals(1, loaded.get(DeliveryPriority.LOW).size());
        assertEquals(0, loaded.get(DeliveryPriority.HIGH).size());
    }

    /**
     * Tests that WAL cleanup removes corrupted entries.
     */
    @Test
    void testWalCleanupRemovesCorruptedEntries() throws IOException {
        // Create corrupted WAL
        createWalFile(
            "{\"playerUuid\":\"550e8400-e29b-41d4-a716-446655440000\",\"statisticKey\":\"minecraft.blocks.mined.stone\",\"value\":100,\"dataType\":\"NUMBER\",\"collectionTimestamp\":1234567890,\"priority\":\"NORMAL\",\"isDelta\":true,\"sourcePlugin\":\"RCore\"}",
            "corrupted line 1",
            "{incomplete",
            "{\"playerUuid\":\"550e8400-e29b-41d4-a716-446655440001\",\"statisticKey\":\"minecraft.travel.walk\",\"value\":5000,\"dataType\":\"NUMBER\",\"collectionTimestamp\":1234567891,\"priority\":\"LOW\",\"isDelta\":false,\"sourcePlugin\":\"RCore\"}"
        );

        // Run validation which triggers cleanup
        manager.validateAndRepair();

        // WAL should now contain only valid entries
        List<String> lines = Files.readAllLines(walFile);
        
        // Should have 2 valid entries (empty lines removed)
        long validLines = lines.stream()
            .filter(line -> !line.isBlank())
            .count();
        assertEquals(2, validLines);
    }

    /**
     * Tests that completely corrupted WAL is deleted.
     */
    @Test
    void testCompletelyCorruptedWalIsDeleted() throws IOException {
        // Create WAL with only corrupted entries
        createWalFile(
            "corrupted line 1",
            "{incomplete json",
            "not json",
            ""
        );

        manager.validateAndRepair();

        // WAL should be deleted
        assertFalse(Files.exists(walFile));
    }

    /**
     * Tests that empty WAL file is handled correctly.
     */
    @Test
    void testEmptyWalFile() throws IOException {
        // Create empty WAL
        Files.createFile(walFile);

        Map<DeliveryPriority, List<QueuedStatistic>> loaded = manager.load();

        // Should return empty queues
        for (DeliveryPriority priority : DeliveryPriority.values()) {
            assertTrue(loaded.get(priority).isEmpty());
        }
    }

    /**
     * Tests that WAL with only whitespace is handled correctly.
     */
    @Test
    void testWalWithOnlyWhitespace() throws IOException {
        createWalFile("", "   ", "\t", "\n");

        Map<DeliveryPriority, List<QueuedStatistic>> loaded = manager.load();

        // Should return empty queues
        for (DeliveryPriority priority : DeliveryPriority.values()) {
            assertTrue(loaded.get(priority).isEmpty());
        }
    }

    /**
     * Tests persist and load cycle.
     */
    @Test
    void testPersistAndLoad() {
        // Create test data
        Map<DeliveryPriority, Collection<QueuedStatistic>> queues = new EnumMap<>(DeliveryPriority.class);
        for (DeliveryPriority priority : DeliveryPriority.values()) {
            queues.put(priority, new ArrayList<>());
        }

        QueuedStatistic stat1 = new QueuedStatistic(
            UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
            "minecraft.blocks.mined.stone",
            100.0,
            StatisticDataType.NUMBER,
            System.currentTimeMillis(),
            DeliveryPriority.NORMAL,
            true,
            "RCore"
        );

        queues.get(DeliveryPriority.NORMAL).add(stat1);

        // Persist
        manager.persist(queues);

        // Load
        Map<DeliveryPriority, List<QueuedStatistic>> loaded = manager.load();

        assertEquals(1, loaded.get(DeliveryPriority.NORMAL).size());
        QueuedStatistic loadedStat = loaded.get(DeliveryPriority.NORMAL).get(0);
        assertEquals(stat1.statisticKey(), loadedStat.statisticKey());
        assertEquals(stat1.value(), loadedStat.value());
    }

    /**
     * Tests capacity checks.
     */
    @Test
    void testCapacityChecks() {
        int maxCapacity = manager.getOfflineMaxCapacity();
        
        assertFalse(manager.isAtWarningCapacity(100));
        assertFalse(manager.isAtMaxCapacity(100));
        
        assertTrue(manager.isAtWarningCapacity((int) (maxCapacity * 0.8)));
        assertTrue(manager.isAtMaxCapacity(maxCapacity));
    }

    /**
     * Helper method to create a WAL file with specified lines.
     */
    private void createWalFile(String... lines) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(walFile.toFile()), StandardCharsets.UTF_8))) {
            for (String line : lines) {
                writer.write(line);
                writer.write("\n");
            }
        }
    }
}
