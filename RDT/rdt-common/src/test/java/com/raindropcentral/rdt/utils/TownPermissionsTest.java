package com.raindropcentral.rdt.utils;

import java.util.Set;

import org.junit.jupiter.api.Test;

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
        assertFalse(memberPermissions.contains(TownPermissions.CLAIM_CHUNK.getPermissionKey()));

        assertTrue(mayorPermissions.contains(TownPermissions.TOWN_INFO.getPermissionKey()));
        assertTrue(mayorPermissions.contains(TownPermissions.TOWN_INVITE.getPermissionKey()));
        assertTrue(mayorPermissions.contains(TownPermissions.CLAIM_CHUNK.getPermissionKey()));
        assertTrue(mayorPermissions.contains(TownPermissions.UPGRADE_CHUNK.getPermissionKey()));
    }

    @Test
    void reportsPermissionDefaultOwnership() {
        assertTrue(TownPermissions.CLAIM_CHUNK.isDefaultForRole("mayor"));
        assertFalse(TownPermissions.CLAIM_CHUNK.isDefaultForRole("member"));
    }
}
