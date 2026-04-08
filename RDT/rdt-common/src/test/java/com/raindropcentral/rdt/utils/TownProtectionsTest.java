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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void exposesProtectionCategories() {
        assertEquals(TownProtectionCategory.BINARY_TOGGLE, TownProtections.TOWN_FIRE.getCategory());
        assertEquals(TownProtectionCategory.ROLE_BASED, TownProtections.PLACE_BLOCK.getCategory());
    }

    @Test
    void normalizesBinaryProtectionRolesToAllowedOrRestricted() {
        assertEquals("PUBLIC", TownProtections.TOWN_WATER.normalizeConfiguredRoleId("public"));
        assertEquals("RESTRICTED", TownProtections.TOWN_WATER.normalizeConfiguredRoleId("member"));
        assertEquals("RESTRICTED", TownProtections.normalizeBinaryRoleId(null));
    }

    @Test
    void editableValuesKeepSwitchSpecificEntriesOutOfTheRootRoleEditor() {
        assertIterableEquals(
            List.of(
                TownProtections.BREAK_BLOCK,
                TownProtections.PLACE_BLOCK,
                TownProtections.SWITCH_ACCESS,
                TownProtections.ITEM_USE
            ),
            TownProtections.editableValues(TownProtectionCategory.ROLE_BASED)
        );
        assertFalse(TownProtections.editableValues(TownProtectionCategory.ROLE_BASED).contains(TownProtections.CHEST));
        assertFalse(TownProtections.editableValues(TownProtectionCategory.ROLE_BASED).contains(TownProtections.LEVER));
        assertFalse(TownProtections.editableValues(TownProtectionCategory.ROLE_BASED).contains(TownProtections.ENDER_PEARL));
        assertTrue(TownProtections.editableValues(TownProtectionCategory.BINARY_TOGGLE).contains(TownProtections.TOWN_FIRE));
    }

    @Test
    void switchActionValuesExposeTheDedicatedSubmenuEntries() {
        assertTrue(TownProtections.switchActionValues().contains(TownProtections.CHEST));
        assertTrue(TownProtections.switchActionValues().contains(TownProtections.PRESSURE_PLATES));
        assertTrue(TownProtections.switchActionValues().contains(TownProtections.TARGET));
        assertFalse(TownProtections.switchActionValues().contains(TownProtections.MINECARTS));
        assertFalse(TownProtections.switchActionValues().contains(TownProtections.SWITCH_ACCESS));
        assertTrue(TownProtections.switchBulkValues().contains(TownProtections.SWITCH_ACCESS));
        assertTrue(TownProtections.switchBulkValues().contains(TownProtections.CONTAINER_ACCESS));
    }

    @Test
    void itemUseActionValuesExposeTheDedicatedSubmenuEntries() {
        assertTrue(TownProtections.itemUseActionValues().contains(TownProtections.MINECARTS));
        assertTrue(TownProtections.itemUseActionValues().contains(TownProtections.BOATS));
        assertTrue(TownProtections.itemUseActionValues().contains(TownProtections.ENDER_PEARL));
        assertTrue(TownProtections.itemUseActionValues().contains(TownProtections.FIREBALL));
        assertTrue(TownProtections.itemUseActionValues().contains(TownProtections.CHORUS_FRUIT));
        assertTrue(TownProtections.itemUseActionValues().contains(TownProtections.LEAD));
        assertFalse(TownProtections.itemUseActionValues().contains(TownProtections.ITEM_USE));
        assertTrue(TownProtections.itemUseBulkValues().contains(TownProtections.ITEM_USE));
    }

    @Test
    void interactionActionsFallBackToLegacyGenericProtectionKeys() {
        assertEquals(TownProtections.CONTAINER_ACCESS, TownProtections.CHEST.getFallbackProtection());
        assertEquals(TownProtections.SWITCH_ACCESS, TownProtections.LEVER.getFallbackProtection());
        assertEquals(TownProtections.SWITCH_ACCESS, TownProtections.ITEM_USE.getFallbackProtection());
        assertEquals(TownProtections.ITEM_USE, TownProtections.MINECARTS.getFallbackProtection());
        assertEquals(TownProtections.ITEM_USE, TownProtections.LEAD.getFallbackProtection());
        assertNull(TownProtections.SWITCH_ACCESS.getFallbackProtection());
    }
}
