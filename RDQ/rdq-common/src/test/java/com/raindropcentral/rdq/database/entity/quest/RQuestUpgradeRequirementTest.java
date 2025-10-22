package com.raindropcentral.rdq.database.entity.quest;

import com.raindropcentral.rdq.requirement.AbstractRequirement;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RQuestUpgradeRequirementTest {

    @Test
    void constructorSetsDefaultsAndRejectsNullDependencies() {
        final RecordingQuest quest = new RecordingQuest("quest.requirement", 1, 3);
        final RQuestUpgrade upgrade = new RQuestUpgrade(quest);
        final RecordingRequirement requirementPayload = new RecordingRequirement();

        final RQuestUpgradeRequirement requirement = new RQuestUpgradeRequirement(upgrade, requirementPayload, 2);

        assertSame(upgrade, requirement.getQuestUpgrade(),
            "Constructor should retain the provided quest upgrade reference");
        assertSame(requirementPayload, requirement.getRequirement(),
            "Constructor should retain the provided requirement payload");
        assertEquals(2, requirement.getUpgradeLevel(),
            "Constructor should store the supplied upgrade level");
        assertEquals(Material.PAPER, requirement.getShowcase(),
            "Constructor should default the showcase to PAPER");

        assertThrows(NullPointerException.class,
            () -> new RQuestUpgradeRequirement(null, requirementPayload, 1),
            "Constructor should reject a null quest upgrade");
        assertThrows(NullPointerException.class,
            () -> new RQuestUpgradeRequirement(upgrade, null, 1),
            "Constructor should reject a null requirement payload");
    }

    @Test
    void settersUpdateStateAndEnforceNullChecks() {
        final RecordingQuest quest = new RecordingQuest("quest.setters", 0, 5);
        final RQuestUpgrade originalUpgrade = new RQuestUpgrade(quest);
        final RQuestUpgradeRequirement requirement = new RQuestUpgradeRequirement(
            originalUpgrade, new RecordingRequirement(), 1);

        final RecordingRequirement replacement = new RecordingRequirement();
        requirement.setRequirement(replacement);
        assertSame(replacement, requirement.getRequirement(),
            "setRequirement should replace the stored payload");

        requirement.setUpgradeLevel(4);
        assertEquals(4, requirement.getUpgradeLevel(),
            "setUpgradeLevel should update the requirement level");

        requirement.setShowcase(Material.DIAMOND);
        assertEquals(Material.DIAMOND, requirement.getShowcase(),
            "setShowcase should update the showcase material");

        final RQuestUpgrade newUpgrade = new RQuestUpgrade(quest);
        requirement.setQuestUpgrade(newUpgrade);
        assertSame(newUpgrade, requirement.getQuestUpgrade(),
            "setQuestUpgrade should update the quest upgrade reference");

        assertThrows(NullPointerException.class, () -> requirement.setQuestUpgrade(null),
            "setQuestUpgrade should reject null values");
        assertThrows(NullPointerException.class, () -> requirement.setRequirement(null),
            "setRequirement should reject null values");
        assertThrows(NullPointerException.class, () -> requirement.setShowcase(null),
            "setShowcase should reject null values");
    }

    @Test
    void setQuestUpgradeMaintainsBidirectionalRelationshipWhenAddedToUpgrade() {
        final RecordingQuest quest = new RecordingQuest("quest.bidirectional", 0, 2);
        final RQuestUpgrade firstUpgrade = new RQuestUpgrade(quest);
        final RQuestUpgrade secondUpgrade = new RQuestUpgrade(quest);
        final RQuestUpgradeRequirement requirement = new RQuestUpgradeRequirement(
            firstUpgrade, new RecordingRequirement(), 1);

        assertTrue(secondUpgrade.addUpgradeRequirement(requirement),
            "addUpgradeRequirement should accept new requirements");
        assertSame(secondUpgrade, requirement.getQuestUpgrade(),
            "addUpgradeRequirement should update the requirement's quest upgrade reference");
        assertTrue(secondUpgrade.getUpgradeRequirements().contains(requirement),
            "addUpgradeRequirement should retain the requirement in the upgrade's collection");
    }

    @Test
    void equalsHashCodeAndToStringRelyOnIdentifier() {
        final RecordingQuest quest = new RecordingQuest("quest.identity", 0, 1);
        final RQuestUpgrade upgrade = new RQuestUpgrade(quest);

        final RQuestUpgradeRequirement requirementA = new RQuestUpgradeRequirement(
            upgrade, new RecordingRequirement(), 3);
        final RQuestUpgradeRequirement requirementB = new RQuestUpgradeRequirement(
            upgrade, new RecordingRequirement(), 3);
        final RQuestUpgradeRequirement requirementC = new RQuestUpgradeRequirement(
            upgrade, new RecordingRequirement(), 3);

        assignId(requirementA, 10L);
        assignId(requirementB, 10L);
        assignId(requirementC, 11L);

        assertEquals(requirementA, requirementB,
            "Entities with the same identifier should be equal");
        assertEquals(requirementA.hashCode(), requirementB.hashCode(),
            "Entities with the same identifier should share hash codes");
        assertNotEquals(requirementA, requirementC,
            "Entities with different identifiers should not be equal");
        assertNotEquals(requirementA, new Object(),
            "Entities should not be equal to unrelated objects");

        requirementA.setUpgradeLevel(5);
        final String expectedToString = "RQuestUpgradeRequirement[id=10, level=5, requirement=RecordingRequirement]";
        assertEquals(expectedToString, requirementA.toString(),
            "toString should include the identifier, level, and requirement type");
    }

    private static void assignId(final RQuestUpgradeRequirement requirement, final long value) {
        try {
            final Field idField = RQuestUpgradeRequirement.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(requirement, value);
        } catch (final ReflectiveOperationException exception) {
            throw new AssertionError("Failed to assign entity identifier", exception);
        }
    }

    private static final class RecordingQuest extends RQuest {

        private RecordingQuest(final String identifier, final int initialUpgradeLevel,
                               final int maximumUpgradeLevel) {
            super(identifier, initialUpgradeLevel, maximumUpgradeLevel);
        }

        @Override
        protected Material initializeShowcase() {
            return Material.EMERALD;
        }
    }

    private static final class RecordingRequirement extends AbstractRequirement {

        private RecordingRequirement() {
            super(Type.CUSTOM);
        }

        @Override
        public boolean isMet(final Player player) {
            return true;
        }

        @Override
        public double calculateProgress(final Player player) {
            return 1.0D;
        }

        @Override
        public void consume(final Player player) {
            // no-op for testing
        }

        @Override
        public String getDescriptionKey() {
            return "requirement.description";
        }
    }
}
