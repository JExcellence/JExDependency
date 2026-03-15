package com.raindropcentral.rdt.database.entity;

import com.raindropcentral.rdt.utils.TownPermissions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests permission mutation behavior on {@link TownRole}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class TownRoleTest {

    @Test
    void constructorNormalizesRoleIdAndPermissionKeys() {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        final TownRole role = new TownRole(
            town,
            " farmers ",
            " Farmers ",
            Set.of(" town_invite ", "view_town")
        );

        assertEquals("FARMERS", role.getRoleId());
        assertEquals("Farmers", role.getRoleName());
        assertTrue(role.hasPermission(TownPermissions.TOWN_INVITE));
        assertTrue(role.hasPermission("VIEW_TOWN"));
    }

    @Test
    void togglePermissionAddsThenRemovesPermission() {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        final TownRole role = new TownRole(
            town,
            "builders",
            "Builders",
            Set.of()
        );

        assertTrue(role.togglePermission("place_chunk"));
        assertTrue(role.hasPermission("PLACE_CHUNK"));

        assertFalse(role.togglePermission("PLACE_CHUNK"));
        assertFalse(role.hasPermission("PLACE_CHUNK"));
    }

    @Test
    void restrictedRoleCannotMutateOrResolvePermissions() {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        final TownRole restrictedRole = new TownRole(
            town,
            RTown.RESTRICTED_ROLE_ID,
            "Restricted",
            Set.of("TOWN_INFO")
        );

        assertFalse(restrictedRole.hasPermission(TownPermissions.TOWN_INFO));
        assertFalse(restrictedRole.togglePermission(TownPermissions.TOWN_INFO));

        restrictedRole.addPermission("TOWN_INFO");
        restrictedRole.removePermission("TOWN_INFO");
        restrictedRole.replacePermissions(Set.of("VIEW_TOWN"));

        assertTrue(restrictedRole.getPermissions().isEmpty());
    }
}
