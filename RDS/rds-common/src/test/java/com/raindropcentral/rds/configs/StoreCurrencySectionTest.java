package com.raindropcentral.rds.configs;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link StoreCurrencySection} default and normalization behavior.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class StoreCurrencySectionTest {

    @Test
    void returnsExpectedDefaults() {
        final StoreCurrencySection section = new StoreCurrencySection(new EvaluationEnvironmentBuilder());

        assertEquals("vault", section.getType());
        assertEquals(1000.0D, section.getInitialCost());
        assertEquals(1.125D, section.getGrowthRate());
    }

    @Test
    void normalizesConfiguredValues() {
        final StoreCurrencySection section = new StoreCurrencySection(new EvaluationEnvironmentBuilder());
        section.setContext("  Coins  ", 250.5D, 1.33D);

        assertEquals("coins", section.getType());
        assertEquals(250.5D, section.getInitialCost());
        assertEquals(1.33D, section.getGrowthRate());
    }

    @Test
    void fallsBackToVaultWhenConfiguredTypeIsBlank() {
        final StoreCurrencySection section = new StoreCurrencySection(new EvaluationEnvironmentBuilder());
        section.setContext("   ", 100.0D, 1.2D);

        assertEquals("vault", section.getType());
    }
}
