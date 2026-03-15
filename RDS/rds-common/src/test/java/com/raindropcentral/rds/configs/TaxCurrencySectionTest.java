package com.raindropcentral.rds.configs;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link TaxCurrencySection} default and normalization behavior.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class TaxCurrencySectionTest {

    @Test
    void returnsExpectedDefaults() {
        final TaxCurrencySection section = new TaxCurrencySection(new EvaluationEnvironmentBuilder());

        assertEquals("vault", section.getType());
        assertEquals(100.0D, section.getInitialCost());
        assertEquals(1.125D, section.getGrowthRate());
        assertEquals(-1.0D, section.getMaximumTax());
        assertFalse(section.hasTaxCap());
    }

    @Test
    void normalizesConfiguredValuesAndClampsNegativeCosts() {
        final TaxCurrencySection section = new TaxCurrencySection(new EvaluationEnvironmentBuilder());
        section.setContext("  Gems  ", -10.0D, -0.25D, 250.0D);

        assertEquals("gems", section.getType());
        assertEquals(0.0D, section.getInitialCost());
        assertEquals(0.0D, section.getGrowthRate());
        assertEquals(250.0D, section.getMaximumTax());
        assertTrue(section.hasTaxCap());
    }

    @Test
    void reportsUnlimitedTaxWhenMaximumIsNegative() {
        final TaxCurrencySection section = new TaxCurrencySection(new EvaluationEnvironmentBuilder());
        section.setContext("vault", 10.0D, 1.1D, -1.0D);

        assertFalse(section.hasTaxCap());
    }
}
