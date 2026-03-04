/*
 * FreeShopServiceTest.java
 *
 * @author ItsRainingHP
 * @version 5.0.0
 */

package com.raindropcentral.rds.service;

import java.lang.reflect.Field;

import com.raindropcentral.rds.configs.ConfigSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests free-edition shop service limits.
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
