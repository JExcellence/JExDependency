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

/*
 * ShopTaxSupportTest.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rds.service.tax;

import com.raindropcentral.rds.configs.ProtectionSection;
import com.raindropcentral.rds.database.entity.Shop;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests tax support shop taxability rules.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class ShopTaxSupportTest {

    @Test
    void excludesAdminShopsFromTaxableSet() {
        final UUID ownerId = UUID.randomUUID();
        final Shop adminShop = this.createShop(ownerId, true);
        final Shop playerShop = this.createShop(ownerId, false);

        assertFalse(ShopTaxSupport.isTaxableShop(adminShop));
        assertTrue(ShopTaxSupport.isTaxableShop(playerShop));
    }

    @Test
    void requiresMatchingOwnerForTaxableOwnerCheck() {
        final UUID ownerId = UUID.randomUUID();
        final UUID otherOwnerId = UUID.randomUUID();
        final Shop playerShop = this.createShop(ownerId, false);
        final Shop adminShop = this.createShop(ownerId, true);

        assertTrue(ShopTaxSupport.isTaxableShopForOwner(playerShop, ownerId));
        assertFalse(ShopTaxSupport.isTaxableShopForOwner(playerShop, otherOwnerId));
        assertFalse(ShopTaxSupport.isTaxableShopForOwner(adminShop, ownerId));
    }

    @Test
    void resolvesProtectionTaxCurrencies() {
        final ProtectionSection protectionSection = new ProtectionSection(new EvaluationEnvironmentBuilder());
        final Map<String, Double> configuredTaxes = new LinkedHashMap<>();
        configuredTaxes.put("vault", -1.0D);
        configuredTaxes.put("coins", 2500.0D);
        protectionSection.setContext(false, configuredTaxes);

        assertTrue(ShopTaxSupport.usesProtectionTax(protectionSection, "vault"));
        assertTrue(ShopTaxSupport.usesProtectionTax(protectionSection, "COINS"));
        assertFalse(ShopTaxSupport.usesProtectionTax(protectionSection, "gems"));
    }

    @Test
    void exposesProtectionFallbackToPlayerSetting() {
        final ProtectionSection protectionSection = new ProtectionSection(new EvaluationEnvironmentBuilder());
        protectionSection.setContext(false, true, Map.of());

        assertTrue(protectionSection.isShopTaxesFallbackToPlayer());
    }

    private Shop createShop(
            final UUID ownerId,
            final boolean adminShop
    ) {
        final Shop shop = new Shop(ownerId, null);
        shop.setAdminShop(adminShop);
        return shop;
    }
}
