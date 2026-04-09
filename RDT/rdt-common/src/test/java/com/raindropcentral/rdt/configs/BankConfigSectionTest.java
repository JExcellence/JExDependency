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

package com.raindropcentral.rdt.configs;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests parsing and default fallback behavior for {@link BankConfigSection}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class BankConfigSectionTest {

    @Test
    void createDefaultExposesExpectedBankLevelsAndFeatureDefaults() {
        final BankConfigSection section = BankConfigSection.createDefault();

        assertEquals(5, section.getHighestConfiguredLevel());
        assertEquals(2, section.getNextLevel(1));
        assertNull(section.getNextLevel(5));
        assertEquals(1, section.getCurrencyStorage().unlockLevel());
        assertEquals(java.util.List.of("vault", "raindrops"), section.getCurrencyStorage().currencies());
        assertEquals(2, section.getItemStorage().unlockLevel());
        assertEquals(54, section.getItemStorage().inventorySize());
        assertEquals(3, section.getRemoteAccess().unlockLevel());
        assertTrue(section.getRemoteAccess().requireOwnClaim());
        assertEquals(5, section.getRemoteAccess().crossClusterCacheDepositUnlockLevel());
        assertEquals(4, section.getCache().unlockLevel());
        assertEquals(27, section.getCache().inventorySize());
        assertEquals(10, section.getCache().placementRadiusBlocks());
        assertTrue(section.getLocking().singleViewer());

        final LevelDefinition levelTwo = section.getLevelDefinition(2);
        assertNotNull(levelTwo);
        assertTrue(levelTwo.getRequirements().containsKey("vault_upgrade"));
        assertTrue(levelTwo.getRequirements().containsKey("reserve_materials"));
        assertTrue(levelTwo.getRewards().containsKey("town_broadcast"));
    }

    @Test
    void fromInputStreamParsesSparseConfiguredLevelsAndFeatureOverrides() {
        final BankConfigSection section = BankConfigSection.fromInputStream(new ByteArrayInputStream("""
            currency_storage:
              unlock_level: 2
              currencies:
                - " Vault "
                - "RAINDROPS"
                - "vault"
            item_storage:
              unlock_level: 4
              rows: 5
            remote_access:
              unlock_level: 6
              require_own_claim: false
              cross_cluster_cache_deposit_unlock_level: 8
            cache:
              unlock_level: 7
              rows: 4
              placement_radius_blocks: 14
              item_material: BARREL
            locking:
              single_viewer: false
            levels:
              "3":
                requirements:
                  " Vault Req ":
                    type: CURRENCY
                    currencyId: vault
                    amount: 700
                rewards:
                  " Bonus ":
                    type: COMMAND
                    command: "rt broadcast {town_uuid} bank"
              "7":
                requirements:
                  " Gems ":
                    type: ITEM
                    requiredItems:
                      - type: EMERALD
                        amount: 4
                rewards:
                  " Vault Bonus ":
                    type: CURRENCY
                    currencyId: vault
                    amount: 50
            """.getBytes(StandardCharsets.UTF_8)));

        assertEquals(2, section.getLevels().size());
        assertEquals(7, section.getHighestConfiguredLevel());
        assertEquals(3, section.getNextLevel(1));
        assertEquals(7, section.getNextLevel(3));
        assertNull(section.getNextLevel(7));
        assertEquals(2, section.getCurrencyStorage().unlockLevel());
        assertEquals(java.util.List.of("vault", "raindrops"), section.getCurrencyStorage().currencies());
        assertEquals(4, section.getItemStorage().unlockLevel());
        assertEquals(45, section.getItemStorage().inventorySize());
        assertEquals(6, section.getRemoteAccess().unlockLevel());
        assertFalse(section.getRemoteAccess().requireOwnClaim());
        assertEquals(8, section.getRemoteAccess().crossClusterCacheDepositUnlockLevel());
        assertEquals(7, section.getCache().unlockLevel());
        assertEquals(36, section.getCache().inventorySize());
        assertEquals(14, section.getCache().placementRadiusBlocks());
        assertEquals(org.bukkit.Material.BARREL, section.getCache().itemMaterial());
        assertFalse(section.getLocking().singleViewer());
    }

    @Test
    void fromInputStreamFallsBackWhenFeatureSectionsAreSparseOrInvalid() {
        final BankConfigSection section = BankConfigSection.fromInputStream(new ByteArrayInputStream("""
            currency_storage:
              unlock_level: 0
              currencies: []
            item_storage:
              unlock_level: -2
              rows: 0
            remote_access:
              unlock_level: -3
              cross_cluster_cache_deposit_unlock_level: 1
            cache:
              unlock_level: -4
              rows: 9
              placement_radius_blocks: 0
              item_material: AIR
            locking: {}
            """.getBytes(StandardCharsets.UTF_8)));

        assertEquals(1, section.getCurrencyStorage().unlockLevel());
        assertEquals(java.util.List.of("vault", "raindrops"), section.getCurrencyStorage().currencies());
        assertEquals(2, section.getItemStorage().unlockLevel());
        assertEquals(54, section.getItemStorage().inventorySize());
        assertEquals(3, section.getRemoteAccess().unlockLevel());
        assertEquals(3, section.getRemoteAccess().crossClusterCacheDepositUnlockLevel());
        assertEquals(4, section.getCache().unlockLevel());
        assertEquals(54, section.getCache().inventorySize());
        assertEquals(10, section.getCache().placementRadiusBlocks());
        assertEquals(org.bukkit.Material.CHEST, section.getCache().itemMaterial());
        assertTrue(section.getLocking().singleViewer());
    }
}
