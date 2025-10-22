package com.raindropcentral.rdq.database.entity.quest;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class RPlayerQuestTest {

    @Test
    void itSeedsUpgradeLevelAndRequirementProgressDuringConstruction() {
        final RDQPlayer player = new RDQPlayer(UUID.randomUUID(), "QuestPlayer");
        final RecordingQuest quest = new RecordingQuest("quest.recording", 1, 3);
        final RQuestUpgrade upgrade = quest.addUpgrade();

        final RQuestUpgradeRequirement requirementLevelOne = addRequirement(upgrade, 1);
        final RQuestUpgradeRequirement requirementLevelTwo = addRequirement(upgrade, 2);
        final RQuestUpgradeRequirement requirementLevelThree = addRequirement(upgrade, 3);

        final RPlayerQuest playerQuest = new RPlayerQuest(player, quest, 2);

        assertEquals(2, playerQuest.getCurrentUpgradeLevel(),
            "Constructor should honour upgrade levels within the quest's range");

        final List<RPlayerQuestRequirementProgress> progressRecords = playerQuest.getRequirementProgressRecords();
        assertEquals(3, progressRecords.size(),
            "Constructor should create progress records for each quest requirement");
        assertThrows(UnsupportedOperationException.class, () -> progressRecords.add(null),
            "Progress records should be exposed as an immutable view");

        final Set<RQuestUpgradeRequirement> expectedRequirements = Set.of(
            requirementLevelOne, requirementLevelTwo, requirementLevelThree
        );
        final Set<RQuestUpgradeRequirement> actualRequirements = progressRecords.stream()
            .map(RPlayerQuestRequirementProgress::getUpgradeRequirement)
            .collect(Collectors.toSet());
        assertEquals(expectedRequirements, actualRequirements,
            "Each progress record should target a quest requirement");

        progressRecords.forEach(progress -> {
            assertSame(playerQuest, progress.getPlayerQuest(),
                "Progress entries should reference the owning player quest");
            assertEquals(0.0, progress.getProgress(),
                "Progress entries should start at zero completion");
        });

        final RPlayerQuest belowRange = new RPlayerQuest(player, quest, 0);
        assertEquals(quest.getInitialUpgradeLevel(), belowRange.getCurrentUpgradeLevel(),
            "Constructor should default to the initial level when provided a below-range level");

        final RPlayerQuest aboveRange = new RPlayerQuest(player, quest, 5);
        assertEquals(quest.getInitialUpgradeLevel(), aboveRange.getCurrentUpgradeLevel(),
            "Constructor should default to the initial level when provided an above-range level");
    }

    @Test
    void itExposesAndUpdatesStateThroughAccessors() {
        final RDQPlayer originalPlayer = new RDQPlayer(UUID.randomUUID(), "Original");
        final RecordingQuest originalQuest = new RecordingQuest("quest.original", 0, 2);
        final RQuestUpgrade upgrade = originalQuest.addUpgrade();
        addRequirement(upgrade, 1);

        final RPlayerQuest playerQuest = new RPlayerQuest(originalPlayer, originalQuest, 0);

        assertSame(originalPlayer, playerQuest.getPlayer(),
            "getPlayer should return the player supplied during construction");
        assertSame(originalQuest, playerQuest.getQuest(),
            "getQuest should return the quest supplied during construction");

        final RDQPlayer updatedPlayer = new RDQPlayer(UUID.randomUUID(), "Updated");
        final RecordingQuest updatedQuest = new RecordingQuest("quest.updated", 0, 4);
        addRequirement(updatedQuest.addUpgrade(), 1);

        playerQuest.setPlayer(updatedPlayer);
        playerQuest.setQuest(updatedQuest);
        playerQuest.setCurrentUpgradeLevel(3);

        assertSame(updatedPlayer, playerQuest.getPlayer(),
            "setPlayer should replace the owning player");
        assertSame(updatedQuest, playerQuest.getQuest(),
            "setQuest should replace the backing quest");
        assertEquals(3, playerQuest.getCurrentUpgradeLevel(),
            "setCurrentUpgradeLevel should adjust the quest level without clamping");
    }

    @Test
    void itDeterminesUpgradeEligibility() {
        final RDQPlayer player = new RDQPlayer(UUID.randomUUID(), "Progressive");
        final RecordingQuest quest = new RecordingQuest("quest.progressive", 0, 2);
        addRequirement(quest.addUpgrade(), 0);

        final RPlayerQuest playerQuest = new RPlayerQuest(player, quest, 0);

        assertTrue(playerQuest.canUpgrade(), "canUpgrade should be true when below the maximum level");

        playerQuest.incrementUpgradeLevel();
        assertEquals(1, playerQuest.getCurrentUpgradeLevel(),
            "incrementUpgradeLevel should increase the level when upgrades remain");
        assertTrue(playerQuest.canUpgrade(),
            "canUpgrade should remain true until the maximum level is reached");

        playerQuest.incrementUpgradeLevel();
        assertEquals(2, playerQuest.getCurrentUpgradeLevel(),
            "incrementUpgradeLevel should reach the configured maximum level");
        assertFalse(playerQuest.canUpgrade(),
            "canUpgrade should be false once the maximum level is achieved");

        playerQuest.incrementUpgradeLevel();
        assertEquals(2, playerQuest.getCurrentUpgradeLevel(),
            "incrementUpgradeLevel should not exceed the maximum level");
    }

    @Test
    void itImplementsEqualityUsingPlayerAndQuestIdentity() {
        final UUID playerId = UUID.randomUUID();
        final RDQPlayer playerA = new RDQPlayer(playerId, "Equality");
        final RDQPlayer playerB = new RDQPlayer(playerId, "Equality");

        final RecordingQuest questA = new RecordingQuest("quest.shared", 0, 1);
        addRequirement(questA.addUpgrade(), 0);
        final RecordingQuest questB = new RecordingQuest("quest.shared", 0, 1);
        addRequirement(questB.addUpgrade(), 0);

        final RPlayerQuest left = new RPlayerQuest(playerA, questA, 0);
        final RPlayerQuest right = new RPlayerQuest(playerB, questB, 0);

        assertEquals(left, right, "RPlayerQuest equality should consider player and quest identity");
        assertEquals(left.hashCode(), right.hashCode(),
            "Equal player quests should compute matching hash codes");

        final RPlayerQuest differentPlayer = new RPlayerQuest(
            new RDQPlayer(UUID.randomUUID(), "Different"), questA, 0);
        assertNotEquals(left, differentPlayer,
            "RPlayerQuest should differ when the owning player changes");

        final RecordingQuest otherQuest = new RecordingQuest("quest.other", 0, 1);
        addRequirement(otherQuest.addUpgrade(), 0);
        final RPlayerQuest differentQuest = new RPlayerQuest(playerA, otherQuest, 0);
        assertNotEquals(left, differentQuest,
            "RPlayerQuest should differ when the backing quest changes");
    }

    private static final class RecordingQuest extends RQuest {

        private RecordingQuest(final String identifier, final int initialUpgradeLevel, final int maximumUpgradeLevel) {
            super(identifier, initialUpgradeLevel, maximumUpgradeLevel);
        }

        private RQuestUpgrade addUpgrade() {
            final RQuestUpgrade upgrade = new RQuestUpgrade(this);
            super.addUpgrade(upgrade);
            return upgrade;
        }

        @Override
        protected Material initializeShowcase() {
            return Material.DIAMOND;
        }
    }

    private static final class StubRequirement extends AbstractRequirement {

        private StubRequirement() {
            super(Type.CUSTOM);
        }

        @Override
        public boolean isMet(final @NotNull Player player) {
            return false;
        }

        @Override
        public double calculateProgress(final @NotNull Player player) {
            return 0.0D;
        }

        @Override
        public void consume(final @NotNull Player player) {
        }

        @Override
        public @NotNull String getDescriptionKey() {
            return "stub.requirement";
        }
    }

    private static RQuestUpgradeRequirement addRequirement(final RQuestUpgrade upgrade, final int level) {
        final RQuestUpgradeRequirement requirement = new RQuestUpgradeRequirement(
            upgrade, new StubRequirement(), level
        );
        upgrade.addUpgradeRequirement(requirement);
        return requirement;
    }
}
