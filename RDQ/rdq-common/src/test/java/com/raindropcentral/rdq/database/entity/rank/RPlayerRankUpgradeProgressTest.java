package com.raindropcentral.rdq.database.entity.rank;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RPlayerRankUpgradeProgressTest {

    @Test
    void constructorEnforcesNonNullAndInitializesProgress() {
        final RDQPlayer player = new RDQPlayer(UUID.randomUUID(), "TestPlayer");
        final RRankUpgradeRequirement requirement = mock(RRankUpgradeRequirement.class);

        final RPlayerRankUpgradeProgress progress = new RPlayerRankUpgradeProgress(player, requirement);

        assertSame(player, progress.getPlayer(), "Constructor should store the provided player reference");
        assertSame(requirement, progress.getUpgradeRequirement(),
            "Constructor should store the provided upgrade requirement reference");
        assertEquals(0.0, progress.getProgress(), "Constructor should initialize progress to zero");

        final NullPointerException playerException = assertThrows(NullPointerException.class,
            () -> new RPlayerRankUpgradeProgress(null, requirement),
            "Constructor should reject a null player");
        assertEquals("player cannot be null", playerException.getMessage());

        final NullPointerException requirementException = assertThrows(NullPointerException.class,
            () -> new RPlayerRankUpgradeProgress(player, null),
            "Constructor should reject a null upgrade requirement");
        assertEquals("upgradeRequirement cannot be null", requirementException.getMessage());
    }

    @Test
    void progressMutatorsClampAndReportCompletion() {
        final RDQPlayer player = new RDQPlayer(UUID.randomUUID(), "ProgressPlayer");
        final RRankUpgradeRequirement requirement = mock(RRankUpgradeRequirement.class);
        final RPlayerRankUpgradeProgress progress = new RPlayerRankUpgradeProgress(player, requirement);

        progress.setProgress(0.5D);
        assertEquals(0.5D, progress.getProgress(), "setProgress should store provided values below one");
        assertFalse(progress.isCompleted(), "Progress below one should not be considered completed");

        progress.setProgress(1.5D);
        assertEquals(1.0D, progress.getProgress(), "setProgress should clamp progress to one");
        assertTrue(progress.isCompleted(), "Progress at or above one should be considered completed");

        progress.resetProgress();
        assertEquals(0.0D, progress.getProgress(), "resetProgress should restore progress to zero");
        assertFalse(progress.isCompleted(), "Progress should no longer be marked as completed after reset");

        final double incremented = progress.incrementProgress(0.4D);
        assertEquals(0.4D, incremented, "incrementProgress should return the updated progress");
        assertEquals(0.4D, progress.getProgress(), "incrementProgress should update the stored progress");
        assertFalse(progress.isCompleted(), "Incrementing below one should not mark completion");

        final double capped = progress.incrementProgress(0.8D);
        assertEquals(1.0D, capped, "incrementProgress should clamp cumulative progress to one");
        assertEquals(1.0D, progress.getProgress(), "Progress should be capped at one after incrementing past the limit");
        assertTrue(progress.isCompleted(), "Progress capped at one should mark completion");
    }

    @Test
    void getTargetRankDelegatesToUpgradeRequirement() {
        final RDQPlayer player = new RDQPlayer(UUID.randomUUID(), "RankPlayer");
        final RRankUpgradeRequirement requirement = mock(RRankUpgradeRequirement.class);
        final RRank targetRank = mock(RRank.class);
        when(requirement.getRank()).thenReturn(targetRank);

        final RPlayerRankUpgradeProgress progress = new RPlayerRankUpgradeProgress(player, requirement);

        assertSame(targetRank, progress.getTargetRank(), "getTargetRank should delegate to the upgrade requirement");
        verify(requirement).getRank();
    }
}
