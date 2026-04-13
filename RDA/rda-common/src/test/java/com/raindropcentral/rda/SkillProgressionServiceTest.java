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
import com.raindropcentral.rda.database.entity.RDASkillState;
import com.raindropcentral.rda.database.repository.RRDAPlayer;
import com.raindropcentral.rda.database.repository.RRDASkillState;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link SkillProgressionService}.
 */
class SkillProgressionServiceTest {

    @Test
    void awardsConfiguredXpAndLevelsUpForMining() {
        final SkillConfig skillConfig = this.createTestConfig(SkillType.MINING, Material.DIAMOND_PICKAXE, 3, 1, 10);
        final RDASkillState skillState = new RDASkillState(new RDAPlayer(UUID.randomUUID()), SkillType.MINING);

        SkillProgressionService.applyXpAward(skillState, 10L, skillConfig);

        assertEquals(1, skillState.getLevel());
        assertEquals(0L, skillState.getXp());
    }

    @Test
    void appliesPrestigeXpBonus() {
        final SkillConfig skillConfig = this.createTestConfig(SkillType.FIGHTING, Material.DIAMOND_SWORD, 3, 10, 10);

        assertEquals(11L, SkillProgressionService.calculateAwardedXp(2, 5.0D, 1, skillConfig));
    }

    @Test
    void appliesPrestigeBonusToSharedBaseXp() {
        final SkillConfig skillConfig = this.createTestConfig(SkillType.FIGHTING, Material.DIAMOND_SWORD, 3, 10, 10);

        assertEquals(22L, SkillProgressionService.applyPrestigeBonus(20L, 1, skillConfig));
    }

    @Test
    void capsAtSoftMaxBeforeFinalPrestige() {
        final SkillConfig skillConfig = this.createTestConfig(SkillType.WOODCUTTING, Material.DIAMOND_AXE, 3, 1, 10);
        final RDASkillState skillState = new RDASkillState(new RDAPlayer(UUID.randomUUID()), SkillType.WOODCUTTING);

        SkillProgressionService.applyXpAward(skillState, 40L, skillConfig);
        final SkillProfileSnapshot snapshot = SkillProgressionService.createSnapshot(
            skillState,
            SkillType.WOODCUTTING,
            skillConfig
        );

        assertEquals(3, skillState.getLevel());
        assertEquals(10L, skillState.getXp());
        assertEquals("3", snapshot.displayLevelText());
        assertEquals(0L, snapshot.xpToNextLevel());
        assertTrue(snapshot.prestigeAvailable());
    }

    @Test
    void allowsOverlevelsAfterFinalPrestige() {
        final SkillConfig skillConfig = this.createTestConfig(SkillType.WOODCUTTING, Material.DIAMOND_AXE, 3, 1, 10);
        final RDASkillState skillState = new RDASkillState(new RDAPlayer(UUID.randomUUID()), SkillType.WOODCUTTING);
        skillState.setPrestige(1);

        SkillProgressionService.applyXpAward(skillState, 40L, skillConfig);
        final SkillProfileSnapshot snapshot = SkillProgressionService.createSnapshot(
            skillState,
            SkillType.WOODCUTTING,
            skillConfig
        );

        assertEquals(4, skillState.getLevel());
        assertEquals("3 (1)", snapshot.displayLevelText());
        assertEquals(10L, snapshot.xpToNextLevel());
        assertTrue(snapshot.maxPrestigeReached());
    }

    @Test
    void rebuildsStateFromTotalProgressXp() {
        final SkillConfig skillConfig = this.createTestConfig(SkillType.MINING, Material.DIAMOND_PICKAXE, 3, 1, 10);
        final RDASkillState skillState = new RDASkillState(new RDAPlayer(UUID.randomUUID()), SkillType.MINING);

        SkillProgressionService.applyXpAward(skillState, 25L, skillConfig);
        assertEquals(25L, SkillProgressionService.resolveTotalProgressXp(skillState, skillConfig));

        SkillProgressionService.applyTotalProgressXp(skillState, 12L, skillConfig);

        assertEquals(1, skillState.getLevel());
        assertEquals(2L, skillState.getXp());
        assertEquals(12L, SkillProgressionService.resolveTotalProgressXp(skillState, skillConfig));
    }

    @Test
    void seedsChildStateFromLegacyColumns() {
        final RDA rda = mock(RDA.class);
        final RRDAPlayer playerRepository = mock(RRDAPlayer.class);
        final RRDASkillState skillStateRepository = mock(RRDASkillState.class);
        final RDAPlayer playerProfile = new RDAPlayer(UUID.randomUUID());
        playerProfile.setMiningLevel(25);
        playerProfile.setMiningXp(99L);
        playerProfile.setMiningPrestige(2);
        when(skillStateRepository.findByPlayerAndSkill(playerProfile.getPlayerUuid(), SkillType.MINING)).thenReturn(null);
        when(skillStateRepository.create(any(RDASkillState.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final SkillProgressionService progressionService = new SkillProgressionService(
            rda,
            playerRepository,
            skillStateRepository,
            SkillType.MINING,
            this.createTestConfig(SkillType.MINING, Material.DIAMOND_PICKAXE, 3, 1, 10)
        );

        final RDASkillState seededState = progressionService.ensureState(playerProfile);

        assertEquals(25, seededState.getLevel());
        assertEquals(99L, seededState.getXp());
        assertEquals(2, seededState.getPrestige());
    }

    private SkillConfig createTestConfig(
        final SkillType skillType,
        final Material displayIcon,
        final int softMaxLevel,
        final int maxPrestiges,
        final int prestigeBonusPercent
    ) {
        return new SkillConfig(
            skillType,
            true,
            displayIcon,
            softMaxLevel,
            maxPrestiges,
            SkillConfig.PrestigeTrigger.MANUAL,
            prestigeBonusPercent,
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
}
