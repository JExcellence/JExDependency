package com.raindropcentral.rdt.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests premium-edition behavior reported by {@link PremiumTownService}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class PremiumTownServiceTest {

    @Test
    void reportsPremiumEditionCapabilities() {
        final PremiumTownService service = new PremiumTownService();

        assertTrue(service.isPremium());
        assertTrue(service.canChangeConfigs());
    }
}
