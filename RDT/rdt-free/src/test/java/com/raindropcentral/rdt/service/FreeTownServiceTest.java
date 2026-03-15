package com.raindropcentral.rdt.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests free-edition behavior reported by {@link FreeTownService}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class FreeTownServiceTest {

    @Test
    void reportsFreeEditionCapabilities() {
        final FreeTownService service = new FreeTownService();

        assertFalse(service.isPremium());
        assertFalse(service.canChangeConfigs());
    }
}
