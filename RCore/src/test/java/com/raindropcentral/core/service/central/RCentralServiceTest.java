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

package com.raindropcentral.core.service.central;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RCentralServiceTest {

    @Test
    void normalizeAllowlistItemCodesTrimsLowercasesAndDeduplicates() {
        assertEquals(
                List.of("skill-level-cookie", "job-level-cookie"),
                RCentralService.normalizeAllowlistItemCodes(List.of(
                        " Skill-Level-Cookie ",
                        "job-level-cookie",
                        "skill-level-cookie",
                        "   "
                ))
        );
    }

    @Test
    void marksAllowlistAsStaleOnceSixHoursHaveElapsed() {
        final LocalDateTime now = LocalDateTime.of(2026, 4, 4, 12, 0);

        assertFalse(RCentralService.isAllowlistStale(now.minusHours(5).minusMinutes(59), now));
        assertTrue(RCentralService.isAllowlistStale(now.minusHours(6), now));
        assertTrue(RCentralService.isAllowlistStale(null, now));
    }
}
