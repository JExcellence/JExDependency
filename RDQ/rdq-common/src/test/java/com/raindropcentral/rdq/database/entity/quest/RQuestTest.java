package com.raindropcentral.rdq.database.entity.quest;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RQuestTest {

    @Test
    void itInitializesDefaultsAndValidatesIdentifier() {
        final RecordingQuest quest = new RecordingQuest("quest.recording", 1, 5);

        assertEquals("quest.recording", quest.getIdentifier(), "Constructor should persist the identifier");
        assertEquals(1, quest.getInitialUpgradeLevel(), "Constructor should persist the initial upgrade level");
        assertEquals(5, quest.getMaximumUpgradeLevel(), "Constructor should persist the maximum upgrade level");
        assertEquals("quest.name", quest.getShowcaseI18nNameKey(), "Constructor should set the showcase name default");
        assertEquals("quest.lore", quest.getShowcaseI18nLoreKey(), "Constructor should set the showcase lore default");
        assertEquals(Material.DIAMOND, quest.getShowcase(), "initializeShowcase should provide the showcase material");
        assertEquals(1, quest.getInitializeInvocations(),
            "initializeShowcase should execute during construction");

        assertThrows(NullPointerException.class, () -> new RecordingQuest(null, 0, 0),
            "Constructor should reject a null identifier");
    }

    @Test
    void itManagesUpgradesAndExposesImmutableViews() {
        final RecordingQuest quest = new RecordingQuest("quest.upgrades", 0, 3);
        final RQuestUpgrade upgrade = new RQuestUpgrade(quest);

        quest.addUpgrade(upgrade);
        quest.addUpgrade(upgrade);
        assertEquals(List.of(upgrade), quest.getUpgrades(),
            "addUpgrade should register the upgrade once");
        assertThrows(UnsupportedOperationException.class, () -> quest.getUpgrades().add(upgrade),
            "getUpgrades should expose an immutable view");

        assertTrue(quest.removeUpgrade(upgrade), "removeUpgrade should remove the upgrade when present");
        assertTrue(quest.getUpgrades().isEmpty(), "removeUpgrade should leave the upgrades empty when removing the only entry");
        assertFalse(quest.removeUpgrade(upgrade), "removeUpgrade should report false when the upgrade is absent");
    }

    private static final class RecordingQuest extends RQuest {

        private int initializeInvocations;

        private RecordingQuest(final String identifier, final int initialUpgradeLevel, final int maximumUpgradeLevel) {
            super(identifier, initialUpgradeLevel, maximumUpgradeLevel);
        }

        @Override
        protected Material initializeShowcase() {
            this.initializeInvocations++;
            return Material.DIAMOND;
        }

        private int getInitializeInvocations() {
            return this.initializeInvocations;
        }
    }
}
