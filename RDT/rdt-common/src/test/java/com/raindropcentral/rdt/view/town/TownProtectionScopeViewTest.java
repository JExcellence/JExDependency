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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class TownProtectionScopeViewTest {

    @Test
    void layoutKeepsPaginationControlsAndLeavesRoomForTheAutoBackButton() {
        final TownProtectionScopeView view = new TownProtectionScopeView();

        assertArrayEquals(
            new String[]{
                "XXXXXXXXX",
                "XOOOOOOOX",
                "XOOOOOOOX",
                "XOOOOOOOX",
                "XXXXXXXXX",
                "   <p>   "
            },
            view.getLayout()
        );
    }

    @Test
    void routesBackToTheCategorySpecificEditorWhenScopeChanges() {
        assertSame(TownProtectionsView.class, TownProtectionScopeView.resolveTargetViewClass(null));
        assertSame(TownRoleProtectionsView.class, TownProtectionScopeView.resolveTargetViewClass(TownProtectionCategory.ROLE_BASED));
        assertSame(TownToggleProtectionsView.class, TownProtectionScopeView.resolveTargetViewClass(TownProtectionCategory.BINARY_TOGGLE));
        assertSame(
            TownSwitchProtectionsView.class,
            TownProtectionScopeView.resolveTargetViewClass(TownProtectionCategory.ROLE_BASED, TownSwitchProtectionsView.VIEW_KEY)
        );
        assertSame(
            TownItemUseProtectionsView.class,
            TownProtectionScopeView.resolveTargetViewClass(TownProtectionCategory.ROLE_BASED, TownItemUseProtectionsView.VIEW_KEY)
        );
        assertSame(
            TownAlliedProtectionsView.class,
            TownProtectionScopeView.resolveTargetViewClass(TownProtectionCategory.ROLE_BASED, TownAlliedProtectionsView.VIEW_KEY)
        );
        assertSame(
            TownAlliedSwitchProtectionsView.class,
            TownProtectionScopeView.resolveTargetViewClass(TownProtectionCategory.ROLE_BASED, TownAlliedSwitchProtectionsView.VIEW_KEY)
        );
        assertSame(
            TownAlliedItemUseProtectionsView.class,
            TownProtectionScopeView.resolveTargetViewClass(TownProtectionCategory.ROLE_BASED, TownAlliedItemUseProtectionsView.VIEW_KEY)
        );
    }

    @Test
    void buildScopeOptionsOnlyIncludesTownGlobalAndSecurityChunks() {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        town.addChunk(new RTownChunk(town, "world", 4, 4, ChunkType.DEFAULT));
        town.addChunk(new RTownChunk(town, "world", 2, 2, ChunkType.SECURITY));
        town.addChunk(new RTownChunk(town, "world_nether", 1, 1, ChunkType.BANK));
        town.addChunk(new RTownChunk(town, "world_nether", 3, 3, ChunkType.SECURITY));

        final List<TownProtectionScopeView.ScopeOption> scopes = TownProtectionScopeView.buildScopeOptions(town);

        assertEquals(3, scopes.size());
        assertNull(scopes.getFirst().worldName());
        assertEquals("world", scopes.get(1).worldName());
        assertEquals(2, scopes.get(1).chunkX());
        assertEquals(2, scopes.get(1).chunkZ());
        assertEquals(ChunkType.SECURITY, scopes.get(1).chunkType());
        assertEquals("world_nether", scopes.get(2).worldName());
        assertEquals(3, scopes.get(2).chunkX());
        assertEquals(3, scopes.get(2).chunkZ());
        assertEquals(ChunkType.SECURITY, scopes.get(2).chunkType());
    }

    @Test
    void resolveSelectedChunkDefaultsToTownGlobalWhenSecurityChunkEntryDoesNotPassChunkScope() {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        final RTownChunk securityChunk = new RTownChunk(town, "world", 2, 3, ChunkType.SECURITY);
        final RTownChunk defaultChunk = new RTownChunk(town, "world", 4, 5, ChunkType.DEFAULT);
        town.addChunk(securityChunk);
        town.addChunk(defaultChunk);
        final RDT plugin = new RDT(Mockito.mock(JavaPlugin.class), "test", Mockito.mock(TownService.class));
        final Map<String, Object> navigationData = TownChunkView.createProtectionNavigationData(plugin, securityChunk);

        assertNull(
            TownProtectionScopeView.resolveSelectedChunk(
                town,
                (String) navigationData.get("world_name"),
                (Integer) navigationData.get("chunk_x"),
                (Integer) navigationData.get("chunk_z")
            )
        );
        assertEquals("world", navigationData.get("origin_world_name"));
        assertEquals(2, navigationData.get("origin_chunk_x"));
        assertEquals(3, navigationData.get("origin_chunk_z"));
        assertSame(securityChunk, TownProtectionScopeView.resolveSelectedChunk(town, "world", 2, 3));
        assertNull(TownProtectionScopeView.resolveSelectedChunk(town, "world", 4, 5));
    }
}
