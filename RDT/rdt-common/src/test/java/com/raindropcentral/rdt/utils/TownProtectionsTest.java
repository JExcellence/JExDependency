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

package com.raindropcentral.rdt.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests protection metadata and role normalization helpers in {@link TownProtections}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class TownProtectionsTest {

    @Test
    void normalizeRoleIdReturnsTrimmedUpperCaseValue() {
        assertEquals("MEMBER", TownProtections.normalizeRoleId(" member "));
        assertEquals("RESTRICTED", TownProtections.normalizeRoleId("restricted"));
    }

    @Test
    void exposesProtectionKeyAsEnumName() {
        assertEquals("CHEST", TownProtections.CHEST.getProtectionKey());
        assertEquals("BREAK_BLOCK", TownProtections.BREAK_BLOCK.getProtectionKey());
    }

    @Test
    void exposesConfiguredDefaultRoleIds() {
        assertEquals("PUBLIC", TownProtections.TOWN_WATER.getDefaultRoleId());
        assertEquals("RESTRICTED", TownProtections.TOWN_HOSTILE_ENTITIES.getDefaultRoleId());
        assertEquals("MEMBER", TownProtections.CHEST.getDefaultRoleId());
    }
}
