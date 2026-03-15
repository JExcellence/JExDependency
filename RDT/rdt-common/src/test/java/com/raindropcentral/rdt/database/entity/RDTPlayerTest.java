package com.raindropcentral.rdt.database.entity;

import com.raindropcentral.rdt.utils.TownPermissions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests membership and town-permission behavior on {@link RDTPlayer}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class RDTPlayerTest {

    @Test
    void constructorWithTownAssignsMemberDefaults() {
        final RDTPlayer player = new RDTPlayer(UUID.randomUUID(), UUID.randomUUID());

        assertEquals(RTown.MEMBER_ROLE_ID, player.getTownRoleId());
        assertTrue(player.getTownPermissions().containsAll(
            TownPermissions.defaultPermissionKeysForRole(RTown.MEMBER_ROLE_ID)
        ));
    }

    @Test
    void clearingTownMembershipAlsoClearsRoleAndPermissions() {
        final RDTPlayer player = new RDTPlayer(UUID.randomUUID(), UUID.randomUUID());

        player.setTownUUID(null);

        assertNull(player.getTownUUID());
        assertNull(player.getTownRoleId());
        assertTrue(player.getTownPermissions().isEmpty());
    }

    @Test
    void settingRoleWithEmptyPermissionsBackfillsRoleDefaults() {
        final RDTPlayer player = new RDTPlayer(UUID.randomUUID(), UUID.randomUUID());
        player.getTownPermissions().clear();

        player.setTownRoleId("mayor");

        assertEquals(RTown.MAYOR_ROLE_ID, player.getTownRoleId());
        assertTrue(
            player.getTownPermissions().containsAll(
                TownPermissions.defaultPermissionKeysForRole(RTown.MAYOR_ROLE_ID)
            )
        );
    }

    @Test
    void replaceTownPermissionsNormalizesPermissionKeys() {
        final RDTPlayer player = new RDTPlayer(UUID.randomUUID(), UUID.randomUUID());

        player.replaceTownPermissions(Set.of(" town_invite ", "view_town"));

        assertTrue(player.hasTownPermission("TOWN_INVITE"));
        assertTrue(player.hasTownPermission("VIEW_TOWN"));
        assertFalse(player.hasTownPermission("CLAIM_CHUNK"));
    }

    @Test
    void toggleTownPermissionAddsAndRemovesPermission() {
        final RDTPlayer player = new RDTPlayer(UUID.randomUUID(), UUID.randomUUID());

        final boolean enabledAfterFirstToggle = player.toggleTownPermission(TownPermissions.CLAIM_CHUNK);
        final boolean enabledAfterSecondToggle = player.toggleTownPermission(TownPermissions.CLAIM_CHUNK);

        assertTrue(enabledAfterFirstToggle);
        assertFalse(enabledAfterSecondToggle);
    }
}
