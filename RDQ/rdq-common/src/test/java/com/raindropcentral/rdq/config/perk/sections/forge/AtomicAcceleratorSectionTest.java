package com.raindropcentral.rdq.config.perk.sections.forge;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AtomicAcceleratorSectionTest {

    @Test
    void itDefaultsRateToZeroBeforeInjection() {
        final AtomicAcceleratorSection acceleratorSection = new AtomicAcceleratorSection(new EvaluationEnvironmentBuilder());

        assertEquals(0.0D, acceleratorSection.getRate(),
            "getRate should return zero when configuration has not injected a value");
    }

    @Test
    void itExposesInjectedRateThroughGetter() throws Exception {
        final AtomicAcceleratorSection acceleratorSection = new AtomicAcceleratorSection(new EvaluationEnvironmentBuilder());

        setRateField(acceleratorSection, 2.5D);

        assertEquals(2.5D, acceleratorSection.getRate(),
            "getRate should return the value injected into the rate field");
    }

    private static void setRateField(final AtomicAcceleratorSection section, final double value) throws Exception {
        final Field rateField = AtomicAcceleratorSection.class.getDeclaredField("rate");
        rateField.setAccessible(true);
        rateField.set(section, value);
    }
}
