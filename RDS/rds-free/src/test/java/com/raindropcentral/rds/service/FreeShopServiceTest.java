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

package com.raindropcentral.rds.service;

import com.raindropcentral.rds.configs.ConfigSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests free-edition shop service limits.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class FreeShopServiceTest {

    @Test
    void capsMaximumShopsAtThree() throws ReflectiveOperationException {
        final FreeShopService service = new FreeShopService();
        final ConfigSection config = this.createConfig(10);

        assertEquals(3, service.getMaximumShops(config));
    }

    @Test
    void capsUnlimitedConfigAtThree() throws ReflectiveOperationException {
        final FreeShopService service = new FreeShopService();
        final ConfigSection config = this.createConfig(-1);

        assertEquals(3, service.getMaximumShops(config));
    }

    @Test
    void blocksConfigChangesAndAdminExpansion() {
        final FreeShopService service = new FreeShopService();

        assertEquals(2, service.getMaximumAdminShops());
        assertFalse(service.canChangeConfigs());
        assertFalse(service.isPremium());
    }

    private ConfigSection createConfig(final int maxShops) throws ReflectiveOperationException {
        final ConfigSection config = new ConfigSection(new EvaluationEnvironmentBuilder());
        final Field maxShopsField = ConfigSection.class.getDeclaredField("max_shops");
        maxShopsField.setAccessible(true);
        maxShopsField.set(config, maxShops);
        return config;
    }
}
