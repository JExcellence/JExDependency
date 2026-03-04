package com.raindropcentral.rds.service;

import com.raindropcentral.rds.configs.ConfigSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests shared shop service defaults.
 */
class ShopServiceTest {

    @Test
    void reportsFiniteShopLimitsOnlyForPositiveCaps() {
        final ConfigSection config = new ConfigSection(new EvaluationEnvironmentBuilder());

        assertTrue(new FixedShopService(3, 2).hasShopLimit(config));
        assertFalse(new FixedShopService(-1, 2).hasShopLimit(config));
        assertFalse(new FixedShopService(0, 2).hasShopLimit(config));
    }

    @Test
    void reportsFiniteAdminShopLimitsOnlyForPositiveCaps() {
        assertTrue(new FixedShopService(3, 2).hasAdminShopLimit());
        assertFalse(new FixedShopService(3, -1).hasAdminShopLimit());
        assertFalse(new FixedShopService(3, 0).hasAdminShopLimit());
    }

    @Test
    void rejectsNullConfigWhenEvaluatingShopLimits() {
        final ShopService service = new FixedShopService(3, 2);

        assertThrows(NullPointerException.class, () -> service.hasShopLimit(null));
    }

    private static final class FixedShopService implements ShopService {

        private final int maximumShops;
        private final int maximumAdminShops;

        private FixedShopService(
            final int maximumShops,
            final int maximumAdminShops
        ) {
            this.maximumShops = maximumShops;
            this.maximumAdminShops = maximumAdminShops;
        }

        @Override
        public int getMaximumShops(final ConfigSection config) {
            return this.maximumShops;
        }

        @Override
        public int getMaximumAdminShops() {
            return this.maximumAdminShops;
        }

        @Override
        public boolean canChangeConfigs() {
            return false;
        }

        @Override
        public boolean isPremium() {
            return false;
        }
    }
}
