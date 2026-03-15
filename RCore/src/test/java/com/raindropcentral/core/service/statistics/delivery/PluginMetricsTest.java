package com.raindropcentral.core.service.statistics.delivery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests builder defaults and mapping for {@link PluginMetrics}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class PluginMetricsTest {

    @Test
    void builderDefaultsToZeroValues() {
        final PluginMetrics metrics = PluginMetrics.builder().build();

        assertEquals(0, metrics.activeQuestCount());
        assertEquals(0, metrics.completedQuestsInPeriod());
        assertEquals(0, metrics.economyTransactionCount());
        assertEquals(0.0D, metrics.economyTransactionVolume(), 0.000_1D);
        assertEquals(0, metrics.perkActivationCount());
        assertEquals(0, metrics.activePerkCount());
        assertEquals(0, metrics.activeBountyCount());
        assertEquals(0, metrics.completedBountiesInPeriod());
    }

    @Test
    void builderMapsProvidedValues() {
        final PluginMetrics metrics = PluginMetrics.builder()
            .activeQuestCount(2)
            .completedQuestsInPeriod(3)
            .economyTransactionCount(4)
            .economyTransactionVolume(5.5D)
            .perkActivationCount(6)
            .activePerkCount(7)
            .activeBountyCount(8)
            .completedBountiesInPeriod(9)
            .build();

        assertEquals(2, metrics.activeQuestCount());
        assertEquals(3, metrics.completedQuestsInPeriod());
        assertEquals(4, metrics.economyTransactionCount());
        assertEquals(5.5D, metrics.economyTransactionVolume(), 0.000_1D);
        assertEquals(6, metrics.perkActivationCount());
        assertEquals(7, metrics.activePerkCount());
        assertEquals(8, metrics.activeBountyCount());
        assertEquals(9, metrics.completedBountiesInPeriod());
    }
}
