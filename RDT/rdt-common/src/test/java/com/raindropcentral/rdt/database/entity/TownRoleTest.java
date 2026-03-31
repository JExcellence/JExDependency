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
