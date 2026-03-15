package com.raindropcentral.rdq.bounty.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies announcement scope parsing defaults for bounty broadcasts.
 *
 * @author ItsRainingHP
 * @since 6.0.0
 * @version 6.0.0
 */
class EAnnouncementScopeTest {

    @Test
    void returnsMatchingScopeForValidValue() {
        assertEquals(EAnnouncementScope.SERVER, EAnnouncementScope.of("SERVER"));
        assertEquals(EAnnouncementScope.NEARBY, EAnnouncementScope.of("NEARBY"));
        assertEquals(EAnnouncementScope.TARGET, EAnnouncementScope.of("TARGET"));
    }

    @Test
    void fallsBackToServerForInvalidValue() {
        assertEquals(EAnnouncementScope.SERVER, EAnnouncementScope.of("NOT_A_SCOPE"));
    }
}
