/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

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
