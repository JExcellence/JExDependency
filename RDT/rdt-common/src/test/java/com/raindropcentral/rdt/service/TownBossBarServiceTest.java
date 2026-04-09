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
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.TownColorUtil;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TownBossBarServiceTest {

    @Mock
    private JavaPlugin javaPlugin;

    @Mock
    private ISchedulerAdapter scheduler;

    @Mock
    private TownService townService;

    @Mock
    private Player firstPlayer;

    @Mock
    private Player secondPlayer;

    @Mock
    private Player player;

    @Mock
    private TownRuntimeService runtimeService;

    @Mock
    private Location location;

    @Mock
    private Block targetBlock;

    @Mock
    private Location targetLocation;

    @Test
    void refreshPlayersUsesEntitySchedulerContexts() throws ReflectiveOperationException {
        final RDT plugin = this.createPluginWithConfig("""
            global_max_chunk_limit: 10
            """);
        setField(plugin, "scheduler", scheduler);

        final TownBossBarService service = new TownBossBarService(plugin);

        service.refreshPlayers(List.of(firstPlayer, secondPlayer));

        verify(scheduler).runAtEntity(eq(firstPlayer), any(Runnable.class));
        verify(scheduler).runAtEntity(eq(secondPlayer), any(Runnable.class));
        verifyNoMoreInteractions(scheduler);
    }

    @Test
    void resolveRenderStateUsesClaimedTerritoryWhenNotTargetingNexus() throws ReflectiveOperationException {
        final RDT plugin = this.createPluginWithConfig("""
            global_max_chunk_limit: 10
            """);
        setField(plugin, "townRuntimeService", runtimeService);
        final TownBossBarService service = new TownBossBarService(plugin);
        final RTown town = createTownWithChunk("Alpha", "world", 2, 3);

        when(player.getLocation()).thenReturn(location);
        when(location.getBlockX()).thenReturn(34);
        when(location.getBlockZ()).thenReturn(49);
        when(player.getTargetBlockExact(16)).thenReturn(null);
        when(runtimeService.getTownAt(location)).thenReturn(town);

        final TownBossBarService.BossBarRenderState renderState = service.resolveRenderState(player);

        assertEquals("town_boss_bar.territory.claimed", renderState.translationKey());
        assertEquals("Alpha", renderState.placeholders().get("town_name"));
        assertEquals(2, renderState.placeholders().get("chunk_x"));
        assertEquals(3, renderState.placeholders().get("chunk_z"));
        assertEquals(0.1F, renderState.progress(), 0.000_1F);
        assertEquals(TownColorUtil.toBossBarColor(town.getTownColorHex()), renderState.color());
    }

    @Test
    void resolveRenderStateUsesWildernessWhenNoTownIsPresent() throws ReflectiveOperationException {
        final RDT plugin = this.createPluginWithConfig("""
            global_max_chunk_limit: 10
            """);
        setField(plugin, "townRuntimeService", runtimeService);
        final TownBossBarService service = new TownBossBarService(plugin);

        when(player.getLocation()).thenReturn(location);
        when(location.getBlockX()).thenReturn(-1);
        when(location.getBlockZ()).thenReturn(-17);
        when(player.getTargetBlockExact(16)).thenReturn(null);
        when(runtimeService.getTownAt(location)).thenReturn(null);

        final TownBossBarService.BossBarRenderState renderState = service.resolveRenderState(player);

        assertEquals("town_boss_bar.territory.wilderness", renderState.translationKey());
        assertEquals(-1, renderState.placeholders().get("chunk_x"));
        assertEquals(-2, renderState.placeholders().get("chunk_z"));
        assertEquals(1.0F, renderState.progress(), 0.000_1F);
        assertEquals(BossBar.Color.WHITE, renderState.color());
    }

    @Test
    void resolveRenderStateUsesTargetedNexusCombatState() throws ReflectiveOperationException {
        final RDT plugin = this.createPluginWithConfig("""
            global_max_chunk_limit: 10
            """);
        setField(plugin, "townRuntimeService", runtimeService);
        final TownBossBarService service = new TownBossBarService(plugin);
        final RTown nexusTown = createTownWithChunk("Citadel", "world", 10, 12);
        final NexusCombatSnapshot snapshot = new NexusCombatSnapshot(4, 725.0D, 1500.0D, 12.0D);

        when(player.getTargetBlockExact(16)).thenReturn(targetBlock);
        when(targetBlock.getLocation()).thenReturn(targetLocation);
        when(runtimeService.findNexusTown(targetLocation)).thenReturn(nexusTown);
        when(runtimeService.getNexusCombatSnapshot(nexusTown)).thenReturn(snapshot);

        final TownBossBarService.BossBarRenderState renderState = service.resolveRenderState(player);

        assertEquals("town_boss_bar.nexus.title", renderState.translationKey());
        assertEquals("Citadel", renderState.placeholders().get("town_name"));
        assertEquals("725", renderState.placeholders().get("current_health"));
        assertEquals("1500", renderState.placeholders().get("max_health"));
        assertEquals("12", renderState.placeholders().get("defense"));
        assertEquals(4, renderState.placeholders().get("nexus_level"));
        assertEquals(snapshot.progress(), renderState.progress(), 0.000_1F);
        assertEquals(TownColorUtil.toBossBarColor(nexusTown.getTownColorHex()), renderState.color());
    }

    private RDT createPluginWithConfig(final String configYaml) {
        final ConfigSection config = ConfigSection.fromInputStream(
            new ByteArrayInputStream(configYaml.getBytes(StandardCharsets.UTF_8))
        );
        return new RDT(this.javaPlugin, "test", this.townService) {
            @Override
            public ConfigSection getDefaultConfig() {
                return config;
            }
        };
    }

    private static RTown createTownWithChunk(
        final String townName,
        final String worldName,
        final int chunkX,
        final int chunkZ
    ) {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), townName, null);
        town.addChunk(new RTownChunk(town, worldName, chunkX, chunkZ, ChunkType.DEFAULT));
        return town;
    }

    private static void setField(final RDT target, final String fieldName, final Object value) throws ReflectiveOperationException {
        final Field field = RDT.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
