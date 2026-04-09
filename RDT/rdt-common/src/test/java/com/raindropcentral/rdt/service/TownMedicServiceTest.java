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
import com.raindropcentral.rdt.configs.MedicConfigSection;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.utils.ChunkType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

class TownMedicServiceTest {

    @Test
    void memberEnteringHighLevelMedicChunkReceivesConfiguredEntryEffects() throws ReflectiveOperationException {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        final RTownChunk medicChunk = new RTownChunk(town, "world", 2, 3, ChunkType.MEDIC);
        medicChunk.setChunkLevel(5);
        town.addChunk(medicChunk);

        final World world = Mockito.mock(World.class);
        when(world.getName()).thenReturn("world");
        final Location medicLocation = new Location(world, 33.0D, 64.0D, 49.0D);
        final PlayerContext playerContext = this.createPlayerContext(UUID.randomUUID(), medicLocation, 8.0D, 8, 0.0F);
        final UUID playerUuid = playerContext.player().getUniqueId();
        final TownRuntimeService runtimeService = Mockito.mock(TownRuntimeService.class);
        when(runtimeService.getChunkAt(any(Location.class))).thenAnswer(invocation -> {
            final Location location = invocation.getArgument(0, Location.class);
            return TownRuntimeService.toChunkCoordinate(location.getBlockX()) == 2
                && TownRuntimeService.toChunkCoordinate(location.getBlockZ()) == 3
                ? medicChunk
                : null;
        });
        when(runtimeService.getPlayerData(playerUuid)).thenReturn(new RDTPlayer(playerUuid, town.getTownUUID()));

        final TownMedicService service = new TownMedicService(this.createPlugin(MedicConfigSection.createDefault(), runtimeService));

        service.handlePlayerJoin(playerContext.player());

        assertEquals(40.0D, playerContext.maxHealth().get(), 0.0001D);
        assertEquals(40.0D, playerContext.health().get(), 0.0001D);
        assertEquals(20, playerContext.foodLevel().get());
        assertEquals(20.0F, playerContext.saturation().get(), 0.0001F);
    }

    @Test
    void nonMembersDoNotReceiveMedicBuffs() {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        final RTownChunk medicChunk = new RTownChunk(town, "world", 2, 3, ChunkType.MEDIC);
        medicChunk.setChunkLevel(5);
        town.addChunk(medicChunk);

        final World world = Mockito.mock(World.class);
        when(world.getName()).thenReturn("world");
        final Location medicLocation = new Location(world, 33.0D, 64.0D, 49.0D);
        final PlayerContext playerContext = this.createPlayerContext(UUID.randomUUID(), medicLocation, 8.0D, 8, 0.0F);
        final UUID playerUuid = playerContext.player().getUniqueId();

        final TownRuntimeService runtimeService = Mockito.mock(TownRuntimeService.class);
        when(runtimeService.getChunkAt(any(Location.class))).thenReturn(medicChunk);
        when(runtimeService.getPlayerData(playerUuid)).thenReturn(new RDTPlayer(playerUuid));

        final TownMedicService service = new TownMedicService(this.createPlugin(MedicConfigSection.createDefault(), runtimeService));

        service.handlePlayerJoin(playerContext.player());

        assertEquals(20.0D, playerContext.maxHealth().get(), 0.0001D);
        assertEquals(8.0D, playerContext.health().get(), 0.0001D);
        assertEquals(8, playerContext.foodLevel().get());
        assertTrue(playerContext.removedEffects().get().isEmpty());
    }

    @Test
    void sameChunkMoveDoesNotRetriggerEntryEffects() {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        final RTownChunk medicChunk = new RTownChunk(town, "world", 2, 3, ChunkType.MEDIC);
        medicChunk.setChunkLevel(5);
        town.addChunk(medicChunk);

        final World world = Mockito.mock(World.class);
        when(world.getName()).thenReturn("world");
        final Location firstLocation = new Location(world, 33.0D, 64.0D, 49.0D);
        final Location sameChunkLocation = new Location(world, 34.0D, 64.0D, 50.0D);
        final PlayerContext playerContext = this.createPlayerContext(UUID.randomUUID(), firstLocation, 6.0D, 6, 0.0F);
        final UUID playerUuid = playerContext.player().getUniqueId();

        final TownRuntimeService runtimeService = Mockito.mock(TownRuntimeService.class);
        when(runtimeService.getChunkAt(any(Location.class))).thenReturn(medicChunk);
        when(runtimeService.getPlayerData(playerUuid)).thenReturn(new RDTPlayer(playerUuid, town.getTownUUID()));

        final TownMedicService service = new TownMedicService(this.createPlugin(MedicConfigSection.createDefault(), runtimeService));

        service.handlePlayerJoin(playerContext.player());
        playerContext.health().set(5.0D);
        playerContext.foodLevel().set(5);
        playerContext.location().set(sameChunkLocation);

        service.handlePlayerMove(playerContext.player(), firstLocation, sameChunkLocation);

        assertEquals(5.0D, playerContext.health().get(), 0.0001D);
        assertEquals(5, playerContext.foodLevel().get());
    }

    @Test
    void fortifiedRecoveryPersistsOutsideChunkAndExpires() throws ReflectiveOperationException {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        final RTownChunk medicChunk = new RTownChunk(town, "world", 2, 3, ChunkType.MEDIC);
        medicChunk.setChunkLevel(4);
        town.addChunk(medicChunk);

        final MedicConfigSection config = MedicConfigSection.fromInputStream(new ByteArrayInputStream("""
            fortified_recovery:
              duration_seconds: 2
              upkeep_interval_ticks: 20
            levels:
              1:
                requirements: {}
                rewards: {}
              2:
                requirements: {}
                rewards: {}
              3:
                requirements: {}
                rewards: {}
              4:
                requirements: {}
                rewards: {}
            """.getBytes(StandardCharsets.UTF_8)));

        final World world = Mockito.mock(World.class);
        when(world.getName()).thenReturn("world");
        final Location medicLocation = new Location(world, 33.0D, 64.0D, 49.0D);
        final Location wildernessLocation = new Location(world, 80.0D, 64.0D, 80.0D);
        final PlayerContext playerContext = this.createPlayerContext(UUID.randomUUID(), medicLocation, 10.0D, 10, 0.0F);
        final UUID playerUuid = playerContext.player().getUniqueId();

        final TownRuntimeService runtimeService = Mockito.mock(TownRuntimeService.class);
        when(runtimeService.getChunkAt(any(Location.class))).thenAnswer(invocation -> {
            final Location location = invocation.getArgument(0, Location.class);
            return TownRuntimeService.toChunkCoordinate(location.getBlockX()) == 2
                && TownRuntimeService.toChunkCoordinate(location.getBlockZ()) == 3
                ? medicChunk
                : null;
        });
        when(runtimeService.getPlayerData(playerUuid)).thenReturn(new RDTPlayer(playerUuid, town.getTownUUID()));

        final TownMedicService service = new TownMedicService(this.createPlugin(config, runtimeService));

        service.handlePlayerJoin(playerContext.player());
        playerContext.location().set(wildernessLocation);
        service.handlePlayerMove(playerContext.player(), medicLocation, wildernessLocation);

        this.advanceTicks(service, 39);
        assertEquals(40.0D, playerContext.maxHealth().get(), 0.0001D);

        this.advanceTicks(service, 1);
        assertEquals(20.0D, playerContext.maxHealth().get(), 0.0001D);
    }

    @Test
    void emergencyRefillCooldownPersistsAcrossReentryAndTriggersAgainWhenReady() throws ReflectiveOperationException {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        final RTownChunk medicChunk = new RTownChunk(town, "world", 2, 3, ChunkType.MEDIC);
        medicChunk.setChunkLevel(5);
        town.addChunk(medicChunk);

        final MedicConfigSection config = MedicConfigSection.fromInputStream(new ByteArrayInputStream("""
            emergency_refill:
              cooldown_seconds: 2
            """.getBytes(StandardCharsets.UTF_8)));

        final World world = Mockito.mock(World.class);
        when(world.getName()).thenReturn("world");
        final Location medicLocation = new Location(world, 33.0D, 64.0D, 49.0D);
        final Location wildernessLocation = new Location(world, 80.0D, 64.0D, 80.0D);
        final PlayerContext playerContext = this.createPlayerContext(UUID.randomUUID(), medicLocation, 10.0D, 10, 0.0F);
        final UUID playerUuid = playerContext.player().getUniqueId();

        final TownRuntimeService runtimeService = Mockito.mock(TownRuntimeService.class);
        when(runtimeService.getChunkAt(any(Location.class))).thenAnswer(invocation -> {
            final Location location = invocation.getArgument(0, Location.class);
            return TownRuntimeService.toChunkCoordinate(location.getBlockX()) == 2
                && TownRuntimeService.toChunkCoordinate(location.getBlockZ()) == 3
                ? medicChunk
                : null;
        });
        when(runtimeService.getPlayerData(playerUuid)).thenReturn(new RDTPlayer(playerUuid, town.getTownUUID()));

        final TownMedicService service = new TownMedicService(this.createPlugin(config, runtimeService));

        service.handlePlayerJoin(playerContext.player());
        assertEquals(40.0D, playerContext.health().get(), 0.0001D);

        playerContext.health().set(5.0D);
        playerContext.location().set(wildernessLocation);
        service.handlePlayerMove(playerContext.player(), medicLocation, wildernessLocation);
        playerContext.location().set(medicLocation);
        service.handlePlayerMove(playerContext.player(), wildernessLocation, medicLocation);

        assertEquals(6.0D, playerContext.health().get(), 0.0001D);

        this.advanceTicks(service, 40);
        assertEquals(40.0D, playerContext.health().get(), 0.0001D);
    }

    @Test
    void quittingClearsFortifiedRecoveryModifier() {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        final RTownChunk medicChunk = new RTownChunk(town, "world", 2, 3, ChunkType.MEDIC);
        medicChunk.setChunkLevel(4);
        town.addChunk(medicChunk);

        final World world = Mockito.mock(World.class);
        when(world.getName()).thenReturn("world");
        final Location medicLocation = new Location(world, 33.0D, 64.0D, 49.0D);
        final PlayerContext playerContext = this.createPlayerContext(UUID.randomUUID(), medicLocation, 10.0D, 10, 0.0F);
        final UUID playerUuid = playerContext.player().getUniqueId();

        final TownRuntimeService runtimeService = Mockito.mock(TownRuntimeService.class);
        when(runtimeService.getChunkAt(any(Location.class))).thenReturn(medicChunk);
        when(runtimeService.getPlayerData(playerUuid)).thenReturn(new RDTPlayer(playerUuid, town.getTownUUID()));

        final TownMedicService service = new TownMedicService(this.createPlugin(MedicConfigSection.createDefault(), runtimeService));

        service.handlePlayerJoin(playerContext.player());
        assertEquals(40.0D, playerContext.maxHealth().get(), 0.0001D);

        service.handlePlayerQuit(playerContext.player());

        assertEquals(20.0D, playerContext.maxHealth().get(), 0.0001D);
    }

    private void advanceTicks(final TownMedicService service, final int ticks) throws ReflectiveOperationException {
        final Method tickMethod = TownMedicService.class.getDeclaredMethod("tick");
        tickMethod.setAccessible(true);
        for (int tick = 0; tick < ticks; tick++) {
            tickMethod.invoke(service);
        }
    }

    private RDT createPlugin(final MedicConfigSection config, final TownRuntimeService runtimeService) {
        final JavaPlugin javaPlugin = Mockito.mock(JavaPlugin.class);
        when(javaPlugin.namespace()).thenReturn("rdt-test");
        return new RDT(javaPlugin, "test", Mockito.mock(TownService.class)) {
            @Override
            public MedicConfigSection getMedicConfig() {
                return config;
            }

            @Override
            public TownRuntimeService getTownRuntimeService() {
                return runtimeService;
            }
        };
    }

    @SuppressWarnings("deprecation")
    private PlayerContext createPlayerContext(
        final UUID playerUuid,
        final Location initialLocation,
        final double initialHealth,
        final int initialFoodLevel,
        final float initialSaturation
    ) {
        final Player player = Mockito.mock(Player.class);
        final AtomicReference<Location> location = new AtomicReference<>(initialLocation);
        final AtomicReference<Double> health = new AtomicReference<>(initialHealth);
        final AtomicInteger foodLevel = new AtomicInteger(initialFoodLevel);
        final AtomicReference<Float> saturation = new AtomicReference<>(initialSaturation);
        final AtomicReference<Double> maxHealth = new AtomicReference<>(20.0D);
        final AtomicReference<Set<PotionEffectType>> removedEffects = new AtomicReference<>(new java.util.LinkedHashSet<>());

        when(player.getUniqueId()).thenReturn(playerUuid);
        when(player.isOnline()).thenReturn(true);
        when(player.getLocation()).thenAnswer(invocation -> location.get());
        when(player.getHealth()).thenAnswer(invocation -> health.get());
        when(player.getFoodLevel()).thenAnswer(invocation -> foodLevel.get());
        when(player.getSaturation()).thenAnswer(invocation -> saturation.get());
        when(player.getMaxHealth()).thenAnswer(invocation -> maxHealth.get());
        Mockito.doAnswer(invocation -> {
            health.set(invocation.getArgument(0, Double.class));
            return null;
        }).when(player).setHealth(anyDouble());
        Mockito.doAnswer(invocation -> {
            foodLevel.set(invocation.getArgument(0, Integer.class));
            return null;
        }).when(player).setFoodLevel(anyInt());
        Mockito.doAnswer(invocation -> {
            saturation.set(invocation.getArgument(0, Float.class));
            return null;
        }).when(player).setSaturation(anyFloat());
        Mockito.doAnswer(invocation -> {
            removedEffects.get().add(invocation.getArgument(0, PotionEffectType.class));
            return null;
        }).when(player).removePotionEffect(any(PotionEffectType.class));
        Mockito.doAnswer(invocation -> {
            maxHealth.set(invocation.getArgument(0, Double.class));
            return null;
        }).when(player).setMaxHealth(anyDouble());

        return new PlayerContext(player, location, health, foodLevel, saturation, maxHealth, removedEffects);
    }

    private record PlayerContext(
        Player player,
        AtomicReference<Location> location,
        AtomicReference<Double> health,
        AtomicInteger foodLevel,
        AtomicReference<Float> saturation,
        AtomicReference<Double> maxHealth,
        AtomicReference<Set<PotionEffectType>> removedEffects
    ) {
    }
}
