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

package com.raindropcentral.rdt.service;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.configs.ConfigSection;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.database.repository.RRTown;
import com.raindropcentral.rdt.database.repository.RRTownChunk;
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.TownArchetype;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rdt.utils.TownProtections;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TownRuntimeServiceTest {

    @Mock
    private JavaPlugin javaPlugin;

    @Mock
    private RRTownChunk townChunkRepository;

    @Mock
    private RRDTPlayer playerRepository;

    @Mock
    private RRTown townRepository;

    @Mock
    private TownService townService;

    @Mock
    private Location location;

    @Mock
    private World world;

    @Mock
    private Chunk chunk;

    @Mock
    private Chunk loadedChunk;

    @Mock
    private Chunk secondaryLoadedChunk;

    @Mock
    private Player player;

    @Mock
    private RTownChunk townChunk;

    @Mock
    private Server server;

    @Mock
    private Monster hostileMonster;

    @Mock
    private Animals passiveAnimal;

    @Mock
    private Animals overriddenPassiveAnimal;

    @Test
    void getChunkAtUsesBlockCoordinatesWithoutChunkLookup() throws ReflectiveOperationException {
        when(location.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(location.getBlockX()).thenReturn(-5);
        when(location.getBlockZ()).thenReturn(1);
        when(townChunkRepository.findByChunk("world", -1, 0)).thenReturn(townChunk);

        final RDT plugin = new RDT(javaPlugin, "test", townService);
        setField(plugin, "townChunkRepository", townChunkRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertSame(townChunk, service.getChunkAt(location));
        verify(townChunkRepository).findByChunk("world", -1, 0);
        verify(location, never()).getChunk();
    }

    @Test
    void isChunkClaimableRejectsDiagonalAdjacencyWhenCornersAreExcluded() {
        final RTown town = createTownWithClaim("world", 0, 0);
        final TownRuntimeService service = new TownRuntimeService(this.createPluginWithConfig("""
            exclude_corner_claim_adjacency: true
            global_max_chunk_limit: 64
            """));

        assertTrue(service.isChunkClaimable(town, "world", 1, 0));
        assertFalse(service.isChunkClaimable(town, "world", 1, 1));
    }

    @Test
    void isChunkClaimableAllowsDiagonalAdjacencyWhenCornersAreIncluded() {
        final RTown town = createTownWithClaim("world", 0, 0);
        final TownRuntimeService service = new TownRuntimeService(this.createPluginWithConfig("""
            exclude_corner_claim_adjacency: false
            global_max_chunk_limit: 64
            """));

        assertTrue(service.isChunkClaimable(town, "world", 1, 1));
    }

    @Test
    void claimChunkCreatesDefaultChunkTypeWhenPlacementIsValid() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID playerUuid = UUID.randomUUID();
        final RTown town = new RTown(townUuid, playerUuid, "Test Town", null);
        final RDTPlayer playerData = new RDTPlayer(playerUuid, townUuid, RTown.MAYOR_ROLE_ID);
        playerData.grantTownPermission(TownPermissions.CLAIM_CHUNK);

        when(player.getUniqueId()).thenReturn(playerUuid);
        when(location.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(location.getChunk()).thenReturn(chunk);
        when(chunk.getX()).thenReturn(2);
        when(chunk.getZ()).thenReturn(3);
        when(townRepository.findByTownUUID(townUuid)).thenReturn(town);
        when(playerRepository.findByPlayer(playerUuid)).thenReturn(playerData);

        final RDT plugin = this.createPluginWithConfig("""
            exclude_corner_claim_adjacency: true
            global_max_chunk_limit: 64
            """);
        setField(plugin, "townRepository", townRepository);
        setField(plugin, "playerRepository", playerRepository);

        final TownRuntimeService service = new TownRuntimeService(plugin);

        final RTownChunk claimedChunk = service.claimChunk(player, location, townUuid, "world", 2, 3);

        assertNotNull(claimedChunk);
        assertEquals(ChunkType.DEFAULT, claimedChunk.getChunkType());
        verify(townRepository).update(town);
    }

    @Test
    void setTownProtectionRoleIdRefreshesTheLiveTownBeforeSaving() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown staleTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);

        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);

        final RDT plugin = this.createPluginWithConfig("""
            town:
              archetype_change_cooldown_seconds: 86400
            """);
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.setTownProtectionRoleId(staleTown, TownProtections.BREAK_BLOCK, RTown.MEMBER_ROLE_ID));
        assertEquals(RTown.MEMBER_ROLE_ID, liveTown.getProtectionRoleId(TownProtections.BREAK_BLOCK));
        assertEquals(TownProtections.BREAK_BLOCK.getDefaultRoleId(), staleTown.getProtectionRoleId(TownProtections.BREAK_BLOCK));
        verify(townRepository).update(liveTown);
    }

    @Test
    void setTownArchetypeRefreshesTheLiveTownBeforeSaving() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown staleTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);

        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);

        final RDT plugin = this.createPluginWithConfig("""
            town:
              archetype_change_cooldown_seconds: 86400
            """);
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.setTownArchetype(staleTown, TownArchetype.COMMUNIST));
        assertEquals(TownArchetype.COMMUNIST, liveTown.getArchetype());
        assertEquals(TownArchetype.COMMUNIST, staleTown.getArchetype());
        verify(townRepository).update(liveTown);
    }

    @Test
    void setTownArchetypeHonorsConfiguredCooldown() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown staleTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);

        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);

        final RDT plugin = this.createPluginWithConfig("""
            town:
              archetype_change_cooldown_seconds: 86400
            """);
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.setTownArchetype(staleTown, TownArchetype.COMMUNIST));
        assertFalse(service.setTownArchetype(staleTown, TownArchetype.SOCIALIST));
        assertTrue(service.getRemainingTownArchetypeChangeCooldownMillis(staleTown) > 0L);
        assertEquals(TownArchetype.COMMUNIST, liveTown.getArchetype());
        verify(townRepository).update(liveTown);
    }

    @Test
    void setChunkProtectionRoleIdRefreshesTheLiveChunkBeforeSaving() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown staleTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk staleChunk = new RTownChunk(staleTown, "world", 3, 4, ChunkType.SECURITY);
        staleTown.addChunk(staleChunk);

        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk liveChunk = new RTownChunk(liveTown, "world", 3, 4, ChunkType.SECURITY);
        liveTown.addChunk(liveChunk);

        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);

        final RDT plugin = new RDT(javaPlugin, "test", townService);
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.setChunkProtectionRoleId(staleChunk, TownProtections.BREAK_BLOCK, RTown.MEMBER_ROLE_ID));
        assertEquals(RTown.MEMBER_ROLE_ID, liveChunk.getProtectionRoleId(TownProtections.BREAK_BLOCK));
        assertNull(staleChunk.getProtectionRoleId(TownProtections.BREAK_BLOCK));
        verify(townRepository).update(liveTown);
    }

    @Test
    void setChunkProtectionRoleIdRemovesLoadedHostileEntitiesWhenChunkBecomesRestricted() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown staleTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk staleChunk = new RTownChunk(staleTown, "world", 3, 4, ChunkType.SECURITY);
        staleChunk.setProtectionRoleId(TownProtections.TOWN_HOSTILE_ENTITIES, RTown.PUBLIC_ROLE_ID);
        staleTown.addChunk(staleChunk);

        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk liveChunk = new RTownChunk(liveTown, "world", 3, 4, ChunkType.SECURITY);
        liveChunk.setProtectionRoleId(TownProtections.TOWN_HOSTILE_ENTITIES, RTown.PUBLIC_ROLE_ID);
        liveTown.addChunk(liveChunk);

        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);
        when(javaPlugin.getServer()).thenReturn(server);
        when(server.getWorld("world")).thenReturn(world);
        when(world.isChunkLoaded(3, 4)).thenReturn(true);
        when(world.getChunkAt(3, 4)).thenReturn(loadedChunk);
        when(loadedChunk.getEntities()).thenReturn(new Entity[]{hostileMonster, passiveAnimal});

        final RDT plugin = new RDT(javaPlugin, "test", townService);
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.setChunkProtectionRoleId(staleChunk, TownProtections.TOWN_HOSTILE_ENTITIES, RTown.MEMBER_ROLE_ID));
        assertEquals(RTown.MEMBER_ROLE_ID, liveChunk.getProtectionRoleId(TownProtections.TOWN_HOSTILE_ENTITIES));
        verify(hostileMonster).remove();
        verify(passiveAnimal, never()).remove();
        verify(townRepository).update(liveTown);
    }

    @Test
    void setTownProtectionRoleIdRemovesLoadedPassiveEntitiesOnlyFromAffectedChunks() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown staleTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk staleInheritingChunk = new RTownChunk(staleTown, "world", 1, 2, ChunkType.DEFAULT);
        final RTownChunk stalePublicOverrideChunk = new RTownChunk(staleTown, "world", 5, 6, ChunkType.DEFAULT);
        stalePublicOverrideChunk.setProtectionRoleId(TownProtections.TOWN_PASSIVE_ENTITIES, RTown.PUBLIC_ROLE_ID);
        staleTown.addChunk(staleInheritingChunk);
        staleTown.addChunk(stalePublicOverrideChunk);

        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk liveInheritingChunk = new RTownChunk(liveTown, "world", 1, 2, ChunkType.DEFAULT);
        final RTownChunk livePublicOverrideChunk = new RTownChunk(liveTown, "world", 5, 6, ChunkType.DEFAULT);
        livePublicOverrideChunk.setProtectionRoleId(TownProtections.TOWN_PASSIVE_ENTITIES, RTown.PUBLIC_ROLE_ID);
        liveTown.addChunk(liveInheritingChunk);
        liveTown.addChunk(livePublicOverrideChunk);

        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);
        when(javaPlugin.getServer()).thenReturn(server);
        when(server.getWorld("world")).thenReturn(world);
        when(world.isChunkLoaded(1, 2)).thenReturn(true);
        when(world.getChunkAt(1, 2)).thenReturn(loadedChunk);
        when(loadedChunk.getEntities()).thenReturn(new Entity[]{passiveAnimal, hostileMonster});

        final RDT plugin = new RDT(javaPlugin, "test", townService);
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.setTownProtectionRoleId(staleTown, TownProtections.TOWN_PASSIVE_ENTITIES, RTown.MEMBER_ROLE_ID));
        assertEquals(RTown.MEMBER_ROLE_ID, liveTown.getProtectionRoleId(TownProtections.TOWN_PASSIVE_ENTITIES));
        verify(passiveAnimal).remove();
        verify(hostileMonster, never()).remove();
        verify(overriddenPassiveAnimal, never()).remove();
        verify(townRepository).update(liveTown);
    }

    private RDT createPluginWithConfig(final String yaml) {
        final ConfigSection config = ConfigSection.fromInputStream(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))
        );
        return new RDT(this.javaPlugin, "test", this.townService) {
            @Override
            public ConfigSection getDefaultConfig() {
                return config;
            }
        };
    }

    private static RTown createTownWithClaim(final String worldName, final int chunkX, final int chunkZ) {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Test Town", null);
        town.addChunk(new RTownChunk(town, worldName, chunkX, chunkZ, ChunkType.DEFAULT));
        return town;
    }

    private static void setField(final RDT target, final String fieldName, final Object value) throws ReflectiveOperationException {
        final Field field = RDT.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
