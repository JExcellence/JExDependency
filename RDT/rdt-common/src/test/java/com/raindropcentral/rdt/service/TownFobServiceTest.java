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
import com.raindropcentral.rplatform.scheduler.CancellableTaskHandle;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TownFobServiceTest {

    @Mock
    private JavaPlugin javaPlugin;

    @Mock
    private TownService townService;

    @Mock
    private TownRuntimeService runtimeService;

    @Mock
    private ISchedulerAdapter scheduler;

    @Mock
    private Player player;

    @Mock
    private World world;

    @Test
    void teleportToTownFobReturnsFalseWhenThePlayerHasNoTown() {
        final UUID playerUuid = UUID.randomUUID();
        when(this.player.getUniqueId()).thenReturn(playerUuid);
        when(this.runtimeService.getTownFor(playerUuid)).thenReturn(null);

        final TownFobService service = new TownFobService(this.createPlugin(3));

        assertFalse(service.teleportToTownFob(this.player));
        verifyNoInteractions(this.scheduler);
    }

    @Test
    void teleportToTownFobReturnsFalseWhenTheTargetCannotBeResolved() {
        final UUID playerUuid = UUID.randomUUID();
        final RTown town = new RTown(UUID.randomUUID(), playerUuid, "Town", null);
        when(this.player.getUniqueId()).thenReturn(playerUuid);
        when(this.runtimeService.getTownFor(playerUuid)).thenReturn(town);
        when(this.runtimeService.resolveFobTeleportLocation(town)).thenReturn(null);

        final TownFobService service = new TownFobService(this.createPlugin(3));

        assertFalse(service.teleportToTownFob(this.player));
        verifyNoInteractions(this.scheduler);
    }

    @Test
    void teleportToTownFobSchedulesTheConfiguredWarmupAndTeleportsOnlinePlayers() {
        final UUID playerUuid = UUID.randomUUID();
        final RTown town = new RTown(UUID.randomUUID(), playerUuid, "Town", null);
        final Location target = new Location(this.world, 10.0D, 64.0D, 20.0D);
        final ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(this.player.getUniqueId()).thenReturn(playerUuid);
        when(this.player.isOnline()).thenReturn(true);
        when(this.runtimeService.getTownFor(playerUuid)).thenReturn(town);
        when(this.runtimeService.resolveFobTeleportLocation(town)).thenReturn(target);
        when(this.scheduler.runDelayed(runnableCaptor.capture(), eq(140L))).thenReturn(CancellableTaskHandle.noop());

        final TownFobService service = new TownFobService(this.createPlugin(7));

        assertTrue(service.teleportToTownFob(this.player));
        verify(this.scheduler).runDelayed(org.mockito.ArgumentMatchers.any(Runnable.class), eq(140L));
        verify(this.player, never()).teleport(target);

        runnableCaptor.getValue().run();

        verify(this.player).teleport(target);
    }

    private RDT createPlugin(final int fobDelaySeconds) {
        final ConfigSection config = ConfigSection.fromInputStream(
            new ByteArrayInputStream(("town_fob_teleport_delay_seconds: " + fobDelaySeconds).getBytes(StandardCharsets.UTF_8))
        );
        return new RDT(this.javaPlugin, "test", this.townService) {
            @Override
            public ConfigSection getDefaultConfig() {
                return config;
            }

            @Override
            public TownRuntimeService getTownRuntimeService() {
                return TownFobServiceTest.this.runtimeService;
            }

            @Override
            public ISchedulerAdapter getScheduler() {
                return TownFobServiceTest.this.scheduler;
            }
        };
    }
}
