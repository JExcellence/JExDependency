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

package com.raindropcentral.rda;

import com.raindropcentral.rda.database.entity.RDAPlayer;
import com.raindropcentral.rda.database.entity.RDAPlayerBuild;
import com.raindropcentral.rda.database.entity.RDASkillState;
import com.raindropcentral.rda.database.entity.RDAStatAllocation;
import com.raindropcentral.rda.database.repository.RRDAPlayer;
import com.raindropcentral.rda.database.repository.RRDAPlayerBuild;
import com.raindropcentral.rda.database.repository.RRDASkillPreference;
import com.raindropcentral.rda.database.repository.RRDASkillState;
import com.raindropcentral.rda.database.repository.RRDAStatAllocation;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.scheduler.CancellableTaskHandle;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.EnumMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests {@link PlayerBuildService}.
 */
class PlayerBuildServiceTest {

    @Test
    void initializesWithPlatformSchedulerAndCancelsOnShutdown() {
        final JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getName()).thenReturn("RDA");
        when(plugin.namespace()).thenReturn("rda");

        final RDA rda = mock(RDA.class);
        final RPlatform platform = mock(RPlatform.class);
        final ISchedulerAdapter scheduler = mock(ISchedulerAdapter.class);
        final CancellableTaskHandle taskHandle = mock(CancellableTaskHandle.class);
        when(rda.getPlatform()).thenReturn(platform);
        when(platform.getScheduler()).thenReturn(scheduler);
        when(scheduler.runRepeating(any(Runnable.class), eq(20L), eq(20L))).thenReturn(taskHandle);

        final PlayerBuildService service = new PlayerBuildService(
            plugin,
            rda,
            mock(RRDAPlayer.class),
            mock(RRDASkillState.class),
            mock(RRDAPlayerBuild.class),
            mock(RRDAStatAllocation.class),
            mock(RRDASkillPreference.class),
            mock(StatsConfig.class)
        );

        service.initialize();

        verify(scheduler).runRepeating(any(Runnable.class), eq(20L), eq(20L));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of());
            service.shutdown();
        }
        verify(taskHandle).cancel();
    }

    @Test
    void respecAllTaxesTotalSkillProgressionBeforeRecalculatingPoints() {
        final JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getName()).thenReturn("RDA");
        when(plugin.namespace()).thenReturn("rda");

        final RDA rda = mock(RDA.class);
        final RPlatform platform = mock(RPlatform.class);
        final ISchedulerAdapter scheduler = mock(ISchedulerAdapter.class);
        when(rda.getPlatform()).thenReturn(platform);
        when(platform.getScheduler()).thenReturn(scheduler);

        final RRDAPlayer playerRepository = mock(RRDAPlayer.class);
        final RRDASkillState skillStateRepository = mock(RRDASkillState.class);
        final RRDAPlayerBuild playerBuildRepository = mock(RRDAPlayerBuild.class);
        final RRDAStatAllocation statAllocationRepository = mock(RRDAStatAllocation.class);
        final RRDASkillPreference skillPreferenceRepository = mock(RRDASkillPreference.class);
        final StatsConfig statsConfig = this.createTestStatsConfig(5, 0L, 50);

        final PlayerBuildService service = spy(new PlayerBuildService(
            plugin,
            rda,
            playerRepository,
            skillStateRepository,
            playerBuildRepository,
            statAllocationRepository,
            skillPreferenceRepository,
            statsConfig
        ));

        final UUID playerUuid = UUID.randomUUID();
        final Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(player.getAttribute(any(Attribute.class))).thenReturn(null);
        doNothing().when(service).refreshPassiveAttributes(player);

        final RDAPlayer playerProfile = new RDAPlayer(playerUuid);
        when(playerRepository.findOrCreateByPlayer(any(UUID.class))).thenReturn(playerProfile);

        final RDAPlayerBuild playerBuild = new RDAPlayerBuild(playerProfile);
        playerBuild.setUnspentPoints(1);
        when(playerBuildRepository.findByPlayer(any(UUID.class))).thenReturn(playerBuild);
        when(playerBuildRepository.update(any(RDAPlayerBuild.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(playerBuildRepository.create(any(RDAPlayerBuild.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final RDAStatAllocation strengthAllocation = new RDAStatAllocation(playerProfile, CoreStatType.STR);
        strengthAllocation.setAllocatedPoints(4);
        when(statAllocationRepository.findAllByPlayer(any(UUID.class))).thenReturn(List.of(strengthAllocation));

        final RDASkillState miningState = new RDASkillState(playerProfile, SkillType.MINING);
        miningState.setLevel(10);
        final RDASkillState woodcuttingState = new RDASkillState(playerProfile, SkillType.WOODCUTTING);
        woodcuttingState.setLevel(15);
        when(skillStateRepository.findByPlayerAndSkill(any(UUID.class), eq(SkillType.MINING))).thenReturn(miningState);
        when(skillStateRepository.findByPlayerAndSkill(any(UUID.class), eq(SkillType.WOODCUTTING))).thenReturn(
            woodcuttingState
        );

        final SkillConfig miningConfig = this.createTestSkillConfig(SkillType.MINING, Material.DIAMOND_PICKAXE);
        final SkillConfig woodcuttingConfig = this.createTestSkillConfig(SkillType.WOODCUTTING, Material.DIAMOND_AXE);
        when(rda.getSkillConfig(SkillType.MINING)).thenReturn(miningConfig);
        when(rda.getSkillConfig(SkillType.WOODCUTTING)).thenReturn(woodcuttingConfig);

        final RDASkillState expectedMiningState = new RDASkillState(playerProfile, SkillType.MINING);
        expectedMiningState.setLevel(miningState.getLevel());
        expectedMiningState.setXp(miningState.getXp());
        SkillProgressionService.applyTotalProgressXp(
            expectedMiningState,
            (long) Math.floor(SkillProgressionService.resolveTotalProgressXp(miningState, miningConfig) * 0.5D),
            miningConfig
        );

        final RDASkillState expectedWoodcuttingState = new RDASkillState(playerProfile, SkillType.WOODCUTTING);
        expectedWoodcuttingState.setLevel(woodcuttingState.getLevel());
        expectedWoodcuttingState.setXp(woodcuttingState.getXp());
        SkillProgressionService.applyTotalProgressXp(
            expectedWoodcuttingState,
            (long) Math.floor(SkillProgressionService.resolveTotalProgressXp(woodcuttingState, woodcuttingConfig) * 0.5D),
            woodcuttingConfig
        );

        final int expectedUnspentPoints =
            expectedMiningState.getLevel() / statsConfig.getAbilityPointInterval(SkillType.MINING)
                + expectedWoodcuttingState.getLevel() / statsConfig.getAbilityPointInterval(SkillType.WOODCUTTING);

        final boolean respecced = service.respecAll(player);

        assertTrue(respecced);
        assertEquals(0, strengthAllocation.getAllocatedPoints());
        assertEquals(expectedMiningState.getLevel(), miningState.getLevel());
        assertEquals(expectedMiningState.getXp(), miningState.getXp());
        assertEquals(expectedWoodcuttingState.getLevel(), woodcuttingState.getLevel());
        assertEquals(expectedWoodcuttingState.getXp(), woodcuttingState.getXp());
        assertEquals(expectedUnspentPoints, playerBuild.getUnspentPoints());
        verify(skillStateRepository, atLeastOnce()).update(miningState);
        verify(skillStateRepository, atLeastOnce()).update(woodcuttingState);
        verify(statAllocationRepository, atLeastOnce()).update(strengthAllocation);
        verify(playerBuildRepository, atLeastOnce()).update(playerBuild);
    }

    @Test
    void getBuildSnapshotUsesCentralizedManaDisplayModeWhenIntegrationIsAvailable() {
        final JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getName()).thenReturn("RDA");
        when(plugin.namespace()).thenReturn("rda");

        final RDA rda = mock(RDA.class);
        final RPlatform platform = mock(RPlatform.class);
        final ISchedulerAdapter scheduler = mock(ISchedulerAdapter.class);
        final RRDAPlayer playerRepository = mock(RRDAPlayer.class);
        final RRDASkillState skillStateRepository = mock(RRDASkillState.class);
        final RRDAPlayerBuild playerBuildRepository = mock(RRDAPlayerBuild.class);
        final RRDAStatAllocation statAllocationRepository = mock(RRDAStatAllocation.class);
        final RRDASkillPreference skillPreferenceRepository = mock(RRDASkillPreference.class);
        final ManaBossBarIntegration manaBossBarIntegration = mock(ManaBossBarIntegration.class);
        final StatsConfig statsConfig = this.createTestStatsConfig(5, 0L, 50);

        when(rda.getPlatform()).thenReturn(platform);
        when(platform.getScheduler()).thenReturn(scheduler);
        when(rda.getManaBossBarIntegration()).thenReturn(manaBossBarIntegration);

        final UUID playerUuid = UUID.randomUUID();
        final Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerUuid);

        final RDAPlayer playerProfile = new RDAPlayer(playerUuid);
        when(playerRepository.findOrCreateByPlayer(playerUuid)).thenReturn(playerProfile);

        final RDAPlayerBuild playerBuild = new RDAPlayerBuild(playerProfile);
        playerBuild.setManaDisplayMode(ManaDisplayMode.ACTION_BAR.name());
        when(playerBuildRepository.findByPlayer(playerUuid)).thenReturn(playerBuild);
        when(playerBuildRepository.update(any(RDAPlayerBuild.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statAllocationRepository.findAllByPlayer(playerUuid)).thenReturn(List.of());
        when(statAllocationRepository.findByPlayerAndStat(eq(playerUuid), any(CoreStatType.class))).thenReturn(null);
        when(statAllocationRepository.create(any(RDAStatAllocation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(skillPreferenceRepository.findByPlayerAndSkill(eq(playerUuid), any(SkillType.class))).thenReturn(null);
        when(skillPreferenceRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(manaBossBarIntegration.resolveDisplayMode(playerUuid, playerBuild)).thenReturn(ManaDisplayMode.BOSS_BAR);

        final PlayerBuildService service = new PlayerBuildService(
            plugin,
            rda,
            playerRepository,
            skillStateRepository,
            playerBuildRepository,
            statAllocationRepository,
            skillPreferenceRepository,
            statsConfig
        );

        final PlayerBuildSnapshot snapshot = service.getBuildSnapshot(player);

        assertEquals(ManaDisplayMode.BOSS_BAR, snapshot.manaDisplayMode());
    }

    @Test
    void getBuildSnapshotRecoversWhenConcurrentBuildInsertWinsRace() {
        final JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getName()).thenReturn("RDA");
        when(plugin.namespace()).thenReturn("rda");

        final RDA rda = mock(RDA.class);
        final RPlatform platform = mock(RPlatform.class);
        final ISchedulerAdapter scheduler = mock(ISchedulerAdapter.class);
        final RRDAPlayer playerRepository = mock(RRDAPlayer.class);
        final RRDASkillState skillStateRepository = mock(RRDASkillState.class);
        final RRDAPlayerBuild playerBuildRepository = mock(RRDAPlayerBuild.class);
        final RRDAStatAllocation statAllocationRepository = mock(RRDAStatAllocation.class);
        final RRDASkillPreference skillPreferenceRepository = mock(RRDASkillPreference.class);
        final StatsConfig statsConfig = this.createTestStatsConfig(5, 0L, 50);

        when(rda.getPlatform()).thenReturn(platform);
        when(platform.getScheduler()).thenReturn(scheduler);

        final UUID playerUuid = UUID.randomUUID();
        final Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerUuid);

        final RDAPlayer playerProfile = new RDAPlayer(playerUuid);
        when(playerRepository.findOrCreateByPlayer(playerUuid)).thenReturn(playerProfile);

        final RDAPlayerBuild concurrentBuild = new RDAPlayerBuild(playerProfile);
        when(playerBuildRepository.findByPlayer(playerUuid)).thenReturn(null, null, concurrentBuild);
        when(playerBuildRepository.create(any(RDAPlayerBuild.class))).thenThrow(new RuntimeException("duplicate"));
        when(playerBuildRepository.update(any(RDAPlayerBuild.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statAllocationRepository.findAllByPlayer(playerUuid)).thenReturn(List.of());
        when(statAllocationRepository.findByPlayerAndStat(eq(playerUuid), any(CoreStatType.class))).thenReturn(null);
        when(statAllocationRepository.create(any(RDAStatAllocation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(skillPreferenceRepository.findByPlayerAndSkill(eq(playerUuid), any(SkillType.class))).thenReturn(null);
        when(skillPreferenceRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final PlayerBuildService service = new PlayerBuildService(
            plugin,
            rda,
            playerRepository,
            skillStateRepository,
            playerBuildRepository,
            statAllocationRepository,
            skillPreferenceRepository,
            statsConfig
        );

        final PlayerBuildSnapshot snapshot = service.getBuildSnapshot(player);

        assertEquals(100.0D, snapshot.currentMana());
        verify(playerBuildRepository).create(any(RDAPlayerBuild.class));
        verify(playerBuildRepository, atLeastOnce()).update(any(RDAPlayerBuild.class));
    }

    private SkillConfig createTestSkillConfig(final SkillType skillType, final Material displayIcon) {
        return new SkillConfig(
            skillType,
            true,
            displayIcon,
            10,
            0,
            SkillConfig.PrestigeTrigger.MANUAL,
            0,
            new SkillConfig.LevelFormula(
                SkillConfig.LevelFormulaType.POWER,
                10.0D,
                0.0D,
                1.0D,
                SkillConfig.LevelFormulaRounding.FLOOR,
                "test"
            ),
            List.of(new SkillConfig.RateDefinition(
                "test",
                "Test",
                "Test rate",
                null,
                displayIcon,
                SkillTriggerType.BLOCK_BREAK,
                10,
                java.util.Set.of(Material.STONE),
                java.util.Set.of(),
                java.util.Set.of(),
                java.util.Set.of(),
                false,
                false,
                false,
                false,
                0.0D,
                SkillConfig.ToolRequirement.ANY
            )),
            0,
            List.of(),
            null
        );
    }

    private StatsConfig createTestStatsConfig(
        final int defaultAbilityPointInterval,
        final long respecCooldownSeconds,
        final int respecPointTaxPercent
    ) {
        final EnumMap<CoreStatType, StatsConfig.StatDefinition> statDefinitions = new EnumMap<>(CoreStatType.class);
        for (final CoreStatType coreStatType : CoreStatType.values()) {
            statDefinitions.put(coreStatType, new StatsConfig.StatDefinition(
                coreStatType,
                coreStatType.getFallbackIcon(),
                coreStatType.name(),
                null,
                coreStatType.name(),
                "x",
                new StatsConfig.LinearSoftCapFormula(0.0D, 1.0D, 0.5D, 0.25D, 10, 20, 30.0D)
            ));
        }

        return new StatsConfig(
            defaultAbilityPointInterval,
            new EnumMap<>(SkillType.class),
            statDefinitions,
            java.util.EnumSet.allOf(ActivationMode.class),
            new StatsConfig.RespecSettings(respecCooldownSeconds, respecPointTaxPercent),
            new StatsConfig.ManaSettings(100.0D, 5.0D, 1.0D, 0.3D, ManaDisplayMode.ACTION_BAR, 20.0D)
        );
    }
}
