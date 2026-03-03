/*
 * StorageStorePricingSupportTest.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rdr.view;

import com.raindropcentral.rdr.configs.StoreCurrencySection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StorageStorePricingSupportTest {

    @Test
    void calculatesScaledCostFromOwnedStorageCount() {
        final StoreCurrencySection section = new StoreCurrencySection(new EvaluationEnvironmentBuilder());
        section.setContext("vault", 1000.0D, 1.125D);

        assertEquals(1000.0D, StorageStorePricingSupport.calculateCost(section, 0));
        assertEquals(1125.0D, StorageStorePricingSupport.calculateCost(section, 1));
        assertEquals(1265.625D, StorageStorePricingSupport.calculateCost(section, 2));
    }

    @Test
    void fallsBackToInitialCostWhenGrowthProducesNonFiniteValues() {
        final StoreCurrencySection section = new StoreCurrencySection(new EvaluationEnvironmentBuilder());
        section.setContext("vault", 1000.0D, Double.POSITIVE_INFINITY);

        assertEquals(1000.0D, StorageStorePricingSupport.calculateCost(section, 2));
    }
}
