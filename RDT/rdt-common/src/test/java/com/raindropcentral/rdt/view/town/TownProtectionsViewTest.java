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
import com.raindropcentral.rdt.utils.TownProtectionCategory;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class TownProtectionsViewTest {

    @Test
    void routesEachProtectionCategoryToItsDedicatedEditor() {
        assertSame(TownRoleProtectionsView.class, TownProtectionsView.resolveCategoryViewClass(TownProtectionCategory.ROLE_BASED));
        assertSame(TownToggleProtectionsView.class, TownProtectionsView.resolveCategoryViewClass(TownProtectionCategory.BINARY_TOGGLE));
    }

    @Test
    void createOriginChunkNavigationDataBuildsChunkViewReturnDataFromSecurityChunkEntry() {
        final RDT plugin = new RDT(Mockito.mock(JavaPlugin.class), "test", Mockito.mock(TownService.class));
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        final RTownChunk securityChunk = new RTownChunk(town, "world", 2, 3, ChunkType.SECURITY);
        final Map<String, Object> initialData = TownChunkView.createProtectionNavigationData(plugin, securityChunk);

        final Map<String, Object> returnData = AbstractTownProtectionView.createOriginChunkNavigationData(
            plugin,
            town.getTownUUID(),
            initialData
        );

        assertNotNull(returnData);
        assertEquals(plugin, returnData.get("plugin"));
        assertEquals(town.getTownUUID(), returnData.get("town_uuid"));
        assertEquals("world", returnData.get("world_name"));
        assertEquals(2, returnData.get("chunk_x"));
        assertEquals(3, returnData.get("chunk_z"));
        assertFalse(returnData.containsKey("origin_world_name"));
    }

    @Test
    void createOriginChunkNavigationDataReturnsNullWithoutAStoredSecurityChunkOrigin() {
        final RDT plugin = new RDT(Mockito.mock(JavaPlugin.class), "test", Mockito.mock(TownService.class));

        assertNull(AbstractTownProtectionView.createOriginChunkNavigationData(plugin, UUID.randomUUID(), Map.of()));
    }
}
