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
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

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

    @Test
    void townCreationProgressNormalizesKeysClonesItemsAndClearsByPrefix() {
        final RDTPlayer player = new RDTPlayer(UUID.randomUUID());
        final ItemStack contributionItem = Mockito.mock(ItemStack.class);
        final ItemStack storedItem = Mockito.mock(ItemStack.class);
        final ItemStack retrievedItem = Mockito.mock(ItemStack.class);

        Mockito.when(contributionItem.isEmpty()).thenReturn(false);
        Mockito.when(contributionItem.clone()).thenReturn(storedItem);
        Mockito.when(storedItem.clone()).thenReturn(retrievedItem);
        Mockito.when(retrievedItem.getAmount()).thenReturn(4);

        player.setTownCreationCurrencyProgress(" Nexus.Level.1.Charter ", 250.0D);
        player.setTownCreationItemProgress(" Nexus.Level.1.Materials#0 ", contributionItem);

        assertEquals(250.0D, player.getTownCreationCurrencyProgress("nexus.level.1.charter"));
        assertEquals(1, player.getTownCreationCurrencyProgress().size());
        assertEquals(4, player.getTownCreationItemProgress("nexus.level.1.materials#0").getAmount());
        assertNotSame(contributionItem, player.getTownCreationItemProgress("nexus.level.1.materials#0"));
        verify(contributionItem).clone();
        verify(storedItem, atLeastOnce()).clone();

        player.clearTownCreationRequirementProgress("nexus.level.1");

        assertTrue(player.getTownCreationCurrencyProgress().isEmpty());
        assertTrue(player.getTownCreationItemProgress().isEmpty());
    }

    @Test
    void clearTownCreationProgressRemovesAllStoredEntries() {
        final RDTPlayer player = new RDTPlayer(UUID.randomUUID());
        final ItemStack contributionItem = Mockito.mock(ItemStack.class);
        final ItemStack storedItem = Mockito.mock(ItemStack.class);

        Mockito.when(contributionItem.isEmpty()).thenReturn(false);
        Mockito.when(contributionItem.clone()).thenReturn(storedItem);

        player.setTownCreationCurrencyProgress("nexus.level.1.charter", 500.0D);
        player.setTownCreationItemProgress("nexus.level.1.materials#0", contributionItem);

        player.clearTownCreationProgress();

        assertTrue(player.getTownCreationCurrencyProgress().isEmpty());
        assertTrue(player.getTownCreationItemProgress().isEmpty());
        assertEquals(0.0D, player.getTownCreationCurrencyProgress("nexus.level.1.charter"));
        assertNull(player.getTownCreationItemProgress("nexus.level.1.materials#0"));
    }
}
