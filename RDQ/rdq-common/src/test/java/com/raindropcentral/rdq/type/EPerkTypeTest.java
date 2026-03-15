package com.raindropcentral.rdq.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies capability flags and descriptions for {@link EPerkType}.
 *
 * @author ItsRainingHP
 * @since 6.0.0
 * @version 6.0.0
 */
class EPerkTypeTest {

    @Test
    void exposesExpectedBehaviorFlagsByType() {
        assertFalse(EPerkType.TOGGLEABLE_PASSIVE.hasCooldown());
        assertTrue(EPerkType.TOGGLEABLE_PASSIVE.isToggleable());
        assertFalse(EPerkType.TOGGLEABLE_PASSIVE.isEventBased());

        assertTrue(EPerkType.EVENT_TRIGGERED.hasCooldown());
        assertFalse(EPerkType.EVENT_TRIGGERED.isToggleable());
        assertTrue(EPerkType.EVENT_TRIGGERED.isEventBased());

        assertTrue(EPerkType.INSTANT_USE.hasCooldown());
        assertFalse(EPerkType.INSTANT_USE.isToggleable());
        assertFalse(EPerkType.INSTANT_USE.isEventBased());

        assertTrue(EPerkType.DURATION_BASED.hasCooldown());
        assertFalse(EPerkType.DURATION_BASED.isToggleable());
        assertTrue(EPerkType.DURATION_BASED.isEventBased());
    }

    @Test
    void exposesStableDescriptionsForEachPerkType() {
        assertEquals(
            "Toggleable passive effect without cooldown",
            EPerkType.TOGGLEABLE_PASSIVE.getDescription()
        );
        assertEquals(
            "Automatically triggered by events with cooldown",
            EPerkType.EVENT_TRIGGERED.getDescription()
        );
        assertEquals(
            "Immediate effect with cooldown",
            EPerkType.INSTANT_USE.getDescription()
        );
        assertEquals(
            "Temporary effect with duration and cooldown",
            EPerkType.DURATION_BASED.getDescription()
        );
    }
}
