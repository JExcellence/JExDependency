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

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests role-default permission resolution for {@link TownPermissions}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class TownPermissionsTest {

    @Test
    void normalizeReturnsTrimmedUpperCaseValues() {
        assertEquals("MAYOR", TownPermissions.normalize(" mayor "));
        assertEquals("PUBLIC", TownPermissions.normalize("public"));
    }

    @Test
    void restrictedRoleCannotReceiveDefaultPermissions() {
        assertFalse(TownPermissions.canAssignToRole("restricted"));
        assertTrue(TownPermissions.defaultPermissionKeysForRole("restricted").isEmpty());
    }

    @Test
    void resolvesDefaultPermissionInheritanceByRole() {
        final Set<String> publicPermissions = TownPermissions.defaultPermissionKeysForRole("PUBLIC");
        final Set<String> memberPermissions = TownPermissions.defaultPermissionKeysForRole("member");
        final Set<String> mayorPermissions = TownPermissions.defaultPermissionKeysForRole("MaYoR");

        assertTrue(publicPermissions.contains(TownPermissions.TOWN_INFO.getPermissionKey()));
        assertFalse(publicPermissions.contains(TownPermissions.TOWN_INVITE.getPermissionKey()));
        assertFalse(publicPermissions.contains(TownPermissions.TOWN_WITHDRAW.getPermissionKey()));

        assertTrue(memberPermissions.contains(TownPermissions.TOWN_INFO.getPermissionKey()));
        assertTrue(memberPermissions.contains(TownPermissions.TOWN_INVITE.getPermissionKey()));
        assertTrue(memberPermissions.contains(TownPermissions.CONTRIBUTE.getPermissionKey()));
        assertTrue(memberPermissions.contains(TownPermissions.SUPPLY_TOWN_SHOPS.getPermissionKey()));
        assertFalse(memberPermissions.contains(TownPermissions.CLAIM_CHUNK.getPermissionKey()));
        assertFalse(memberPermissions.contains(TownPermissions.MANAGE_TOWN_SHOPS.getPermissionKey()));

        assertTrue(mayorPermissions.contains(TownPermissions.TOWN_INFO.getPermissionKey()));
        assertTrue(mayorPermissions.contains(TownPermissions.TOWN_INVITE.getPermissionKey()));
        assertTrue(mayorPermissions.contains(TownPermissions.CONTRIBUTE.getPermissionKey()));
        assertTrue(mayorPermissions.contains(TownPermissions.SUPPLY_TOWN_SHOPS.getPermissionKey()));
        assertTrue(mayorPermissions.contains(TownPermissions.MANAGE_TOWN_SHOPS.getPermissionKey()));
        assertTrue(mayorPermissions.contains(TownPermissions.MANAGE_RELATIONSHIPS.getPermissionKey()));
        assertTrue(mayorPermissions.contains(TownPermissions.CLAIM_CHUNK.getPermissionKey()));
        assertTrue(mayorPermissions.contains(TownPermissions.CHANGE_TOWN_COLOR.getPermissionKey()));
        assertTrue(mayorPermissions.contains(TownPermissions.UPGRADE_CHUNK.getPermissionKey()));
        assertFalse(publicPermissions.contains(TownPermissions.CONTRIBUTE.getPermissionKey()));
        assertFalse(memberPermissions.contains(TownPermissions.CHANGE_TOWN_COLOR.getPermissionKey()));
        assertFalse(memberPermissions.contains(TownPermissions.MANAGE_RELATIONSHIPS.getPermissionKey()));
    }

    @Test
    void reportsPermissionDefaultOwnership() {
        assertTrue(TownPermissions.CLAIM_CHUNK.isDefaultForRole("mayor"));
        assertFalse(TownPermissions.CLAIM_CHUNK.isDefaultForRole("member"));
        assertTrue(TownPermissions.CHANGE_TOWN_COLOR.isDefaultForRole("mayor"));
        assertFalse(TownPermissions.CHANGE_TOWN_COLOR.isDefaultForRole("member"));
        assertTrue(TownPermissions.CONTRIBUTE.isDefaultForRole("member"));
        assertFalse(TownPermissions.CONTRIBUTE.isDefaultForRole("public"));
        assertTrue(TownPermissions.SUPPLY_TOWN_SHOPS.isDefaultForRole("member"));
        assertFalse(TownPermissions.SUPPLY_TOWN_SHOPS.isDefaultForRole("public"));
        assertTrue(TownPermissions.MANAGE_TOWN_SHOPS.isDefaultForRole("mayor"));
        assertFalse(TownPermissions.MANAGE_TOWN_SHOPS.isDefaultForRole("member"));
        assertTrue(TownPermissions.MANAGE_RELATIONSHIPS.isDefaultForRole("mayor"));
        assertFalse(TownPermissions.MANAGE_RELATIONSHIPS.isDefaultForRole("member"));
    }
}
