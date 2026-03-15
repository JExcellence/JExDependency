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
