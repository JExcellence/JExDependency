package com.raindropcentral.rdq.database.entity.quest;

import com.raindropcentral.rdq.requirement.AbstractRequirement;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RQuestUpgradeTest {

    @Test
    void itRequiresQuestAndManagesRequirementsWithBidirectionalUpdates() {
        final RecordingQuest quest = new RecordingQuest("quest.upgrade", 0, 5);
        final RQuestUpgrade upgrade = new RQuestUpgrade(quest);

        assertThrows(NullPointerException.class, () -> new RQuestUpgrade(null),
            "Constructor should reject a null quest");

        final RQuestUpgrade anotherUpgrade = new RQuestUpgrade(quest);
        final RQuestUpgradeRequirement requirement = new RQuestUpgradeRequirement(
            anotherUpgrade, new RecordingRequirement(), 2);

        assertTrue(upgrade.addUpgradeRequirement(requirement),
            "addUpgradeRequirement should register new requirements");
        assertSame(upgrade, requirement.getQuestUpgrade(),
            "addUpgradeRequirement should update the requirement's quest upgrade reference");
        assertFalse(upgrade.addUpgradeRequirement(requirement),
            "addUpgradeRequirement should not add duplicates");

        final RQuestUpgradeRequirement differentLevel = new RQuestUpgradeRequirement(
            upgrade, new RecordingRequirement(), 3);
        assertTrue(upgrade.addUpgradeRequirement(differentLevel),
            "addUpgradeRequirement should append additional requirements");

        final List<RQuestUpgradeRequirement> levelTwo = upgrade.getRequirementsForLevel(2);
        assertEquals(List.of(requirement), levelTwo,
            "getRequirementsForLevel should return the requirements for the requested level");
        assertNotSame(levelTwo, upgrade.getRequirementsForLevel(2),
            "getRequirementsForLevel should provide a defensive copy");

        assertTrue(upgrade.removeUpgradeRequirement(requirement),
            "removeUpgradeRequirement should remove present requirements");
        assertTrue(upgrade.getRequirementsForLevel(2).isEmpty(),
            "removeUpgradeRequirement should leave no requirements for the removed level");
        assertFalse(upgrade.removeUpgradeRequirement(requirement),
            "removeUpgradeRequirement should return false when the requirement is absent");
    }

    @Test
    void itReliesOnIdentifierForEqualityAndHashCode() {
        final RecordingQuest quest = new RecordingQuest("quest.identity", 0, 1);
        final RQuestUpgrade upgradeA = new RQuestUpgrade(quest);
        final RQuestUpgrade upgradeB = new RQuestUpgrade(quest);
        final RQuestUpgrade upgradeC = new RQuestUpgrade(quest);

        assignId(upgradeA, 5L);
        assignId(upgradeB, 5L);
        assignId(upgradeC, 6L);

        assertEquals(upgradeA, upgradeB, "Entities with the same identifier should be equal");
        assertEquals(upgradeA.hashCode(), upgradeB.hashCode(),
            "Entities with the same identifier should share hash codes");
        assertNotEquals(upgradeA, upgradeC,
            "Entities with different identifiers should not be equal");
        assertNotEquals(upgradeA, new Object(),
            "Entities should not be equal to an arbitrary object");
    }

    private static void assignId(final RQuestUpgrade upgrade, final long value) {
        try {
            final Field idField = RQuestUpgrade.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(upgrade, value);
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
            return Material.DIAMOND;
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
            // no-op for testing purposes
        }

        @Override
        public String getDescriptionKey() {
            return "requirement.description";
        }
    }
}
