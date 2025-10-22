package com.raindropcentral.rdq.database.entity.quest;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import de.jexcellence.hibernate.entity.AbstractEntity;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RPlayerQuestRequirementProgressTest {

    @Test
    void constructorInitializesProgressAndValidatesArguments() {
        final TestContext context = createContext("quest.construction");

        final RPlayerQuestRequirementProgress progress = new RPlayerQuestRequirementProgress(
            context.playerQuest(), context.requirement()
        );

        assertSame(context.playerQuest(), progress.getPlayerQuest(),
            "Constructor should retain the supplied player quest reference");
        assertSame(context.requirement(), progress.getUpgradeRequirement(),
            "Constructor should retain the supplied requirement reference");
        assertEquals(0.0, progress.getProgress(), 1.0e-10,
            "Constructor should initialize progress to zero");

        assertThrows(NullPointerException.class,
            () -> new RPlayerQuestRequirementProgress(null, context.requirement()),
            "Constructor should reject a null player quest");
        assertThrows(NullPointerException.class,
            () -> new RPlayerQuestRequirementProgress(context.playerQuest(), null),
            "Constructor should reject a null requirement");
    }

    @Test
    void setProgressClampsToNormalizedRange() {
        final TestContext context = createContext("quest.progress.clamp");
        final RPlayerQuestRequirementProgress progress = new RPlayerQuestRequirementProgress(
            context.playerQuest(), context.requirement()
        );

        progress.setProgress(0.4);
        assertEquals(0.4, progress.getProgress(), 1.0e-10,
            "setProgress should accept values inside the normalized range");

        progress.setProgress(-2.5);
        assertEquals(0.0, progress.getProgress(), 1.0e-10,
            "setProgress should clamp negative values to zero");

        progress.setProgress(3.0);
        assertEquals(1.0, progress.getProgress(), 1.0e-10,
            "setProgress should clamp values above one to the maximum");
    }

    @Test
    void incrementResetAndCompletionRespectBoundaries() {
        final TestContext context = createContext("quest.progress.increment");
        final RPlayerQuestRequirementProgress progress = new RPlayerQuestRequirementProgress(
            context.playerQuest(), context.requirement()
        );

        progress.incrementProgress(0.5);
        assertEquals(0.5, progress.getProgress(), 1.0e-10,
            "incrementProgress should add to the current progress");
        assertFalse(progress.isCompleted(),
            "isCompleted should remain false until full progress is reached");

        progress.incrementProgress(0.75);
        assertEquals(1.0, progress.getProgress(), 1.0e-10,
            "incrementProgress should clamp sums above one");
        assertTrue(progress.isCompleted(),
            "isCompleted should report true when progress reaches one");

        progress.incrementProgress(-2.0);
        assertEquals(0.0, progress.getProgress(), 1.0e-10,
            "incrementProgress should not permit the value to drop below zero");
        assertFalse(progress.isCompleted(),
            "isCompleted should return false when progress falls below one");

        progress.resetProgress();
        assertEquals(0.0, progress.getProgress(), 1.0e-10,
            "resetProgress should restore the progress to zero");
        assertFalse(progress.isCompleted(),
            "resetProgress should reset the completion flag");
    }

    @Test
    void equalityAndHashCodeDependOnQuestAndRequirement() {
        final TestContext sharedContext = createContext("quest.shared");
        assignIdentifier(sharedContext.requirement(), 1L);

        final RPlayerQuestRequirementProgress left = new RPlayerQuestRequirementProgress(
            sharedContext.playerQuest(), sharedContext.requirement()
        );
        left.setProgress(0.75);

        final RPlayerQuestRequirementProgress right = new RPlayerQuestRequirementProgress(
            sharedContext.playerQuest(), sharedContext.requirement()
        );
        right.setProgress(0.25);

        assertEquals(left, right,
            "Equality should ignore progress values when quest and requirement references match");
        assertEquals(left.hashCode(), right.hashCode(),
            "Hash codes should align when quest and requirement references match");

        final TestContext otherContext = createContext("quest.other");
        assignIdentifier(otherContext.requirement(), 2L);
        final RPlayerQuestRequirementProgress differentQuest = new RPlayerQuestRequirementProgress(
            otherContext.playerQuest(), otherContext.requirement()
        );
        assertNotEquals(left, differentQuest,
            "Progress entries should differ when the player quest reference changes");

        final RQuestUpgradeRequirement alternateRequirement = new RQuestUpgradeRequirement(
            sharedContext.requirement().getQuestUpgrade(), new StubRequirement(), 0
        );
        assignIdentifier(alternateRequirement, 3L);
        sharedContext.requirement().getQuestUpgrade().addUpgradeRequirement(alternateRequirement);

        final RPlayerQuestRequirementProgress differentRequirement = new RPlayerQuestRequirementProgress(
            sharedContext.playerQuest(), alternateRequirement
        );
        assertNotEquals(left, differentRequirement,
            "Progress entries should differ when the requirement reference changes");
    }

    private static TestContext createContext(final String questIdentifier) {
        final RDQPlayer player = new RDQPlayer(UUID.randomUUID(), "Player-" + questIdentifier);
        final StubQuest quest = new StubQuest(questIdentifier, 0, 1);
        final RQuestUpgrade upgrade = quest.addUpgrade();
        final RQuestUpgradeRequirement requirement = new RQuestUpgradeRequirement(
            upgrade, new StubRequirement(), 0
        );
        upgrade.addUpgradeRequirement(requirement);
        final RPlayerQuest playerQuest = new RPlayerQuest(player, quest, 0);
        return new TestContext(playerQuest, requirement);
    }

    private static void assignIdentifier(final AbstractEntity entity, final long identifier) {
        try {
            final Field idField = AbstractEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, identifier);
        } catch (final ReflectiveOperationException exception) {
            fail("Failed to assign identifier for test isolation", exception);
        }
    }

    private record TestContext(RPlayerQuest playerQuest, RQuestUpgradeRequirement requirement) {
    }

    private static final class StubQuest extends RQuest {

        private StubQuest(final String identifier, final int initialUpgradeLevel, final int maximumUpgradeLevel) {
            super(identifier, initialUpgradeLevel, maximumUpgradeLevel);
        }

        private RQuestUpgrade addUpgrade() {
            final RQuestUpgrade upgrade = new RQuestUpgrade(this);
            super.addUpgrade(upgrade);
            return upgrade;
        }

        @Override
        protected Material initializeShowcase() {
            return Material.BOOK;
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
}
