package com.raindropcentral.rdq.database.entity.perk;

import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.type.EPerkType;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RPerkTest {

    @Test
    void itDerivesDisplayKeysAndEnforcesMandatoryFields() {
        final RecordingPerk perk = new RecordingPerk("perk.speed");

        assertEquals("perk.speed", perk.getIdentifier(), "Identifier should match the provided value");
        assertEquals("perk.speed.name", perk.getDisplayNameKey(), "Display name key should derive from identifier");
        assertEquals("perk.speed.description", perk.getDescriptionKey(),
            "Description key should derive from identifier");
        assertNotNull(perk.getPerkSection(), "Constructor should persist the provided perk section");
        assertEquals(EPerkType.TOGGLEABLE_PASSIVE, perk.getPerkType(), "Constructor should persist the perk type");

        assertThrows(NullPointerException.class,
            () -> new RecordingPerk(null, createSection(), EPerkType.TOGGLEABLE_PASSIVE),
            "Constructor should reject a null identifier");
        assertThrows(NullPointerException.class,
            () -> new RecordingPerk("perk.section.null", null, EPerkType.TOGGLEABLE_PASSIVE),
            "Constructor should reject a null perk section");
        assertThrows(NullPointerException.class,
            () -> new RecordingPerk("perk.type.null", createSection(), null),
            "Constructor should reject a null perk type");
    }

    @Test
    void itAllowsMutatingEnablementPriorityAndConcurrencyLimits() {
        final RecordingPerk perk = new RecordingPerk("perk.toggle");

        assertTrue(perk.isEnabled(), "Perks should default to enabled");
        perk.setEnabled(false);
        assertFalse(perk.isEnabled(), "setEnabled should update the enabled flag");

        assertEquals(0, perk.getPriority(), "Perks should default to zero priority");
        perk.setPriority(7);
        assertEquals(7, perk.getPriority(), "setPriority should update the priority value");

        assertNull(perk.getMaxConcurrentUsers(), "Perks should default to unlimited concurrent users");
        perk.setMaxConcurrentUsers(5);
        assertEquals(5, perk.getMaxConcurrentUsers(),
            "setMaxConcurrentUsers should persist the provided limit");
        perk.setMaxConcurrentUsers(null);
        assertNull(perk.getMaxConcurrentUsers(), "setMaxConcurrentUsers should allow clearing the limit");
    }

    @Test
    void itProvidesImmutablePlayerPerkViewAndMaintainsBidirectionalIntegrity() {
        final RecordingPerk original = new RecordingPerk("perk.original");
        final RecordingPerk replacement = new RecordingPerk("perk.replacement");
        final RPlayerPerk playerPerk = new RPlayerPerk(createPlayer("Bidirectional"), original);

        final Set<RPlayerPerk> view = original.getPlayerPerks();
        assertThrows(UnsupportedOperationException.class, () -> view.add(null),
            "playerPerks view should be immutable");

        assertEquals(1, original.getAddInvocations(),
            "Constructor should delegate to addPlayerPerk once");
        assertTrue(original.getPlayerPerks().contains(playerPerk),
            "Original perk should track its associations");
        assertSame(original, playerPerk.getPerk(),
            "Player perk should point back to the original perk");

        replacement.addPlayerPerk(playerPerk);

        assertSame(replacement, playerPerk.getPerk(),
            "addPlayerPerk should redirect the player perk to the new perk");
        assertFalse(original.getPlayerPerks().contains(playerPerk),
            "Original perk should remove reassigned player perks");
        assertTrue(replacement.getPlayerPerks().contains(playerPerk),
            "Replacement perk should include the reassigned player perk");
        assertEquals(1, replacement.getAddInvocations(),
            "Overridden addPlayerPerk should record invocations");

        assertTrue(replacement.removePlayerPerk(playerPerk),
            "removePlayerPerk should report that the association was removed");
        assertNull(playerPerk.getPerk(),
            "removePlayerPerk should clear the reverse association when removing");
        assertEquals(1, replacement.getRemoveInvocations(),
            "Overridden removePlayerPerk should record invocations");
        assertFalse(replacement.getPlayerPerks().contains(playerPerk),
            "Replacement perk should no longer track the player perk after removal");
    }

    private static RDQPlayer createPlayer(final String name) {
        return new RDQPlayer(UUID.nameUUIDFromBytes(name.getBytes()), name);
    }

    private static PerkSection createSection() {
        return new PerkSection(new EvaluationEnvironmentBuilder());
    }

    private static final class RecordingPerk extends RPerk {

        private int addInvocations;
        private int removeInvocations;

        private RecordingPerk(final String identifier) {
            this(identifier, createSection(), EPerkType.TOGGLEABLE_PASSIVE);
        }

        private RecordingPerk(final @Nullable String identifier, final @Nullable PerkSection perkSection,
                              final @Nullable EPerkType perkType) {
            super(identifier, perkSection, perkType);
        }

        @Override
        public boolean addPlayerPerk(final @NotNull RPlayerPerk playerPerk) {
            this.addInvocations++;
            return super.addPlayerPerk(playerPerk);
        }

        @Override
        public boolean removePlayerPerk(final @NotNull RPlayerPerk playerPerk) {
            this.removeInvocations++;
            return super.removePlayerPerk(playerPerk);
        }

        @Override
        public boolean performActivation() {
            return true;
        }

        @Override
        public boolean performDeactivation() {
            return true;
        }

        @Override
        public boolean canPerformActivation() {
            return true;
        }

        @Override
        public void performTrigger() {
            // no-op for tests
        }

        private int getAddInvocations() {
            return this.addInvocations;
        }

        private int getRemoveInvocations() {
            return this.removeInvocations;
        }
    }
}
