package com.raindropcentral.rdq.database.entity.perk;

import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.type.EPerkType;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

class RPlayerPerkTest {

    @Test
    void itInitializesStateThroughConstructors() {
        final RDQPlayer player = createPlayer("PlayerOne");
        final RPerk perk = createPerk("perk-alpha");
        final LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        final RPlayerPerk base = new RPlayerPerk(player, perk);

        final LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertSame(player, base.getPlayer(), "Constructor should store the provided player reference");
        assertSame(perk, base.getPerk(), "Constructor should store the provided perk reference");
        assertFalse(base.isEnabled(), "Default constructor should keep perks disabled");
        assertFalse(base.isActive(), "Default constructor should keep perks inactive");
        assertNull(base.getAssignmentSource(), "Base constructor should not set an assignment source");
        assertNotNull(base.getAcquiredAt(), "Constructor should record the acquisition timestamp");
        assertTrue(!base.getAcquiredAt().isBefore(before) && !base.getAcquiredAt().isAfter(after),
            "Acquired timestamp should fall between the creation bounds");

        final RPlayerPerk enabled = new RPlayerPerk(player, perk, true);
        assertTrue(enabled.isEnabled(), "Enabled constructor should honor the provided enabled flag");

        final RPlayerPerk withSource = new RPlayerPerk(player, perk, "quest-line");
        assertEquals("quest-line", withSource.getAssignmentSource(),
            "Constructor should persist the assignment source when supplied");
    }

    @Test
    void itRejectsNullDependenciesInConstructorsAndMutators() {
        final RPerk perk = createPerk("perk-null-guard");
        final RDQPlayer player = createPlayer("PlayerGuard");

        assertThrows(NullPointerException.class, () -> new RPlayerPerk(null, perk),
            "Constructor should reject a null player");
        assertThrows(NullPointerException.class, () -> new RPlayerPerk(player, null),
            "Constructor should reject a null perk");

        final RPlayerPerk playerPerk = new RPlayerPerk(player, perk);

        assertThrows(NullPointerException.class, () -> playerPerk.setPlayer(null),
            "setPlayer should reject null assignments");
        assertThrows(NullPointerException.class, () -> playerPerk.setPerk(null),
            "setPerk should reject null assignments");

        final RDQPlayer replacementPlayer = createPlayer("Replacement");
        final RPerk replacementPerk = createPerk("perk-beta");

        playerPerk.setPlayer(replacementPlayer);
        playerPerk.setPerk(replacementPerk);

        assertSame(replacementPlayer, playerPerk.getPlayer(), "setPlayer should update the player reference");
        assertSame(replacementPerk, playerPerk.getPerk(), "setPerk should update the perk reference");
    }

    @Test
    void itTracksLifecycleMutationsAndMetadata() throws Exception {
        final RPlayerPerk playerPerk = new RPlayerPerk(createPlayer("Lifecycle"), createPerk("perk-life"));

        assertEquals(0, playerPerk.getActivationCount(), "Activation count should start at zero");
        assertNull(playerPerk.getLastActivated(), "lastActivated should be null before activations");
        assertNull(playerPerk.getLastDeactivated(), "lastDeactivated should be null before deactivations");
        assertEquals(0, playerPerk.getTotalActiveDuration(), "Total active duration should start at zero");
        assertNull(playerPerk.getCooldownExpiry(), "Cooldown should be absent by default");

        playerPerk.recordActivation();
        assertEquals(1, playerPerk.getActivationCount(), "recordActivation should increment the activation counter");
        assertNotNull(playerPerk.getLastActivated(), "recordActivation should update lastActivated");

        final LocalDateTime adjustedActivation = LocalDateTime.now().minusSeconds(5);
        setField(playerPerk, "lastActivated", adjustedActivation);

        playerPerk.recordDeactivation();
        assertNotNull(playerPerk.getLastDeactivated(), "recordDeactivation should stamp lastDeactivated");
        assertTrue(playerPerk.getTotalActiveDuration() >= 5,
            "recordDeactivation should accumulate at least the elapsed seconds");

        playerPerk.setCooldown(30);
        assertNotNull(playerPerk.getCooldownExpiry(), "setCooldown should define a cooldown expiry when duration positive");
        assertTrue(playerPerk.isOnCooldown(), "isOnCooldown should reflect the configured cooldown");
        final long remaining = playerPerk.getRemainingCooldownSeconds();
        assertTrue(remaining > 0 && remaining <= 30,
            "getRemainingCooldownSeconds should return a bounded positive duration");

        playerPerk.clearCooldown();
        assertNull(playerPerk.getCooldownExpiry(), "clearCooldown should remove the cooldown expiry");
        assertFalse(playerPerk.isOnCooldown(), "isOnCooldown should be false after clearing cooldown");
        assertEquals(0, playerPerk.getRemainingCooldownSeconds(),
            "getRemainingCooldownSeconds should report zero when no cooldown exists");

        final LocalDateTime futureExpiry = LocalDateTime.now().plusDays(1);
        playerPerk.setExpiresAt(futureExpiry);
        assertEquals(futureExpiry, playerPerk.getExpiresAt(), "setExpiresAt should update the expiry timestamp");
        assertFalse(playerPerk.hasExpired(), "hasExpired should be false when expiry lies in the future");

        playerPerk.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        assertTrue(playerPerk.hasExpired(), "hasExpired should be true when expiry lies in the past");

        playerPerk.setTemporary(true);
        assertTrue(playerPerk.isTemporary(), "setTemporary should flag the perk as temporary");

        playerPerk.setMetadata("{\"granted\":true}");
        assertEquals("{\"granted\":true}", playerPerk.getMetadata(),
            "setMetadata should persist arbitrary metadata blobs");

        playerPerk.setActive(true);
        playerPerk.setEnabled(true);
        playerPerk.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        playerPerk.clearCooldown();
        playerPerk.setActive(false);

        assertTrue(playerPerk.canBeActivated(),
            "canBeActivated should be true when enabled, not expired, not on cooldown, and inactive");

        playerPerk.setCooldown(0);
        assertNull(playerPerk.getCooldownExpiry(), "setCooldown should clear expiry when provided zero duration");
    }

    @Test
    void itImplementsIdentityBasedEquality() {
        final UUID sharedId = UUID.randomUUID();
        final RDQPlayer playerA = new RDQPlayer(sharedId, "Alpha");
        final RDQPlayer playerB = new RDQPlayer(sharedId, "AlphaClone");
        final RPerk perkA = createPerk("perk-identity");
        final RPerk perkB = createPerk("perk-identity");

        final RPlayerPerk first = new RPlayerPerk(playerA, perkA);
        final RPlayerPerk second = new RPlayerPerk(playerB, perkB);
        final RPlayerPerk differentPlayer = new RPlayerPerk(createPlayer("Beta"), perkA);
        final RPlayerPerk differentPerk = new RPlayerPerk(playerA, createPerk("perk-other"));

        assertEquals(first, second, "Entities sharing the same player and perk identity should be equal");
        assertEquals(first.hashCode(), second.hashCode(),
            "Equal entities should produce identical hash codes");
        assertNotEquals(first, differentPlayer, "Different players should yield inequality");
        assertNotEquals(first, differentPerk, "Different perks should yield inequality");
        assertNotEquals(first, new Object(), "Equality should return false for unrelated types");
    }

    private static RDQPlayer createPlayer(final String name) {
        return new RDQPlayer(UUID.nameUUIDFromBytes(name.getBytes()), name);
    }

    private static RPerk createPerk(final String identifier) {
        return new TestPerk(identifier);
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class TestPerk extends RPerk {

        private TestPerk(final String identifier) {
            super(identifier, new PerkSection(new EvaluationEnvironmentBuilder()), EPerkType.TOGGLEABLE_PASSIVE);
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
            // no-op for testing purposes
        }
    }
}
