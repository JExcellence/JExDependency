/*
 * PremiumShopServiceTest.java
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests premium-edition shop service behaviour.
 */
class PremiumShopServiceTest {

    @Test
    void preservesConfiguredShopLimits() throws ReflectiveOperationException {
        final PremiumShopService service = new PremiumShopService();
        final ConfigSection config = this.createConfig(8);

        assertEquals(8, service.getMaximumShops(config));
    }

    @Test
    void allowsUnlimitedConfigValues() throws ReflectiveOperationException {
        final PremiumShopService service = new PremiumShopService();
        final ConfigSection config = this.createConfig(-1);

        assertEquals(-1, service.getMaximumShops(config));
    }

    @Test
    void allowsConfigChangesAndUnlimitedAdminShops() {
        final PremiumShopService service = new PremiumShopService();

        assertEquals(-1, service.getMaximumAdminShops());
        assertTrue(service.canChangeConfigs());
        assertTrue(service.isPremium());
    }

    private ConfigSection createConfig(final int maxShops) throws ReflectiveOperationException {
        final ConfigSection config = new ConfigSection(new EvaluationEnvironmentBuilder());
        final Field maxShopsField = ConfigSection.class.getDeclaredField("max_shops");
        maxShopsField.setAccessible(true);
        maxShopsField.set(config, maxShops);
        return config;
    }
}
