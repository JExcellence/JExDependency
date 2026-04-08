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

package com.raindropcentral.rdt.view.town;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.service.TownService;
import com.raindropcentral.rdt.utils.ChunkType;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownChunkViewTest {

    @Test
    void securityChunksSupportChunkScopedProtectionEditing() {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        final RTownChunk securityChunk = new RTownChunk(town, "world", 2, 3, ChunkType.SECURITY);

        assertTrue(TownChunkView.supportsChunkScopedProtections(securityChunk));
    }

    @Test
    void supportedChunkTypesExposeChunkProgression() {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);

        assertTrue(TownChunkView.supportsChunkProgression(new RTownChunk(town, "world", 2, 3, ChunkType.SECURITY)));
        assertTrue(TownChunkView.supportsChunkProgression(new RTownChunk(town, "world", 2, 4, ChunkType.BANK)));
        assertTrue(TownChunkView.supportsChunkProgression(new RTownChunk(town, "world", 2, 5, ChunkType.FARM)));
        assertTrue(TownChunkView.supportsChunkProgression(new RTownChunk(town, "world", 2, 6, ChunkType.OUTPOST)));
        assertFalse(TownChunkView.supportsChunkProgression(new RTownChunk(town, "world", 2, 7, ChunkType.DEFAULT)));
        assertFalse(TownChunkView.supportsChunkProgression(new RTownChunk(town, "world", 2, 8, ChunkType.MEDIC)));
    }

    @Test
    void nonSecurityChunksDoNotSupportChunkScopedProtectionEditing() {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        final RTownChunk defaultChunk = new RTownChunk(town, "world", 2, 3, ChunkType.DEFAULT);

        assertFalse(TownChunkView.supportsChunkScopedProtections(defaultChunk));
    }

    @Test
    void fuelControlsOnlyRenderForSecurityChunks() {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);

        assertTrue(TownChunkView.supportsFuelFeatures(new RTownChunk(town, "world", 2, 3, ChunkType.SECURITY)));
        assertFalse(TownChunkView.supportsFuelFeatures(new RTownChunk(town, "world", 2, 4, ChunkType.DEFAULT)));
    }

    @Test
    void estimateFuelDurationMillisUsesPooledFuelAndDrainRate() {
        assertEquals(Duration.ofHours(2L).toMillis(), TownChunkView.estimateFuelDurationMillis(50.0D, 25.0D));
        assertEquals(0L, TownChunkView.estimateFuelDurationMillis(0.0D, 25.0D));
        assertEquals(0L, TownChunkView.estimateFuelDurationMillis(50.0D, 0.0D));
    }

    @Test
    void createProtectionNavigationDataOpensTownGlobalProtectionsFromSecurityChunkEntry() {
        final RDT plugin = new RDT(Mockito.mock(JavaPlugin.class), "test", Mockito.mock(TownService.class));
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        final RTownChunk securityChunk = new RTownChunk(town, "world", 2, 3, ChunkType.SECURITY);

        final Map<String, Object> data = TownChunkView.createProtectionNavigationData(plugin, securityChunk);

        assertSame(plugin, data.get("plugin"));
        assertEquals(town.getTownUUID(), data.get("town_uuid"));
        assertEquals("world", data.get("origin_world_name"));
        assertEquals(2, data.get("origin_chunk_x"));
        assertEquals(3, data.get("origin_chunk_z"));
        assertFalse(data.containsKey("world_name"));
        assertFalse(data.containsKey("chunk_x"));
        assertFalse(data.containsKey("chunk_z"));
    }
}
