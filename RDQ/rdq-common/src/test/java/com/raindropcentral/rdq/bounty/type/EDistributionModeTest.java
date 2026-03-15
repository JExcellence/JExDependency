package com.raindropcentral.rdq.bounty.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies distribution mode parsing for bounty reward delivery settings.
 *
 * @author ItsRainingHP
 * @since 6.0.0
 * @version 6.0.0
 */
class EDistributionModeTest {

    @Test
    void parsesConfiguredModeCaseInsensitively() {
        assertEquals(EDistributionMode.INSTANT, EDistributionMode.of("instant"));
        assertEquals(EDistributionMode.DROP, EDistributionMode.of("DROP"));
        assertEquals(EDistributionMode.CHEST, EDistributionMode.of("ChEsT"));
        assertEquals(EDistributionMode.VIRTUAL, EDistributionMode.of("virtual"));
    }

    @Test
    void fallsBackToInstantForUnknownValues() {
        assertEquals(EDistributionMode.INSTANT, EDistributionMode.of("unknown-mode"));
    }
}
