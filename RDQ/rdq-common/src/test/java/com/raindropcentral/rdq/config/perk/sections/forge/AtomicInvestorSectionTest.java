package com.raindropcentral.rdq.config.perk.sections.forge;

import com.raindropcentral.rdq.config.perk.PluginCurrencySection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class AtomicInvestorSectionTest {

    @Test
    void itExposesInjectedTimerAndCurrencySections() throws Exception {
        final AtomicInvestorSection investorSection = new AtomicInvestorSection(new EvaluationEnvironmentBuilder());
        final int expectedTimer = 42;
        final Map<String, PluginCurrencySection> expectedCurrencyMap = Collections.singletonMap(
            "gold",
            new PluginCurrencySection(new EvaluationEnvironmentBuilder())
        );

        setField(investorSection, "timer", expectedTimer);
        setField(investorSection, "currencySections", expectedCurrencyMap);

        assertEquals(expectedTimer, investorSection.getTimer(),
            "getTimer should expose the configured timer value");
        assertSame(expectedCurrencyMap, investorSection.getCurrencySections(),
            "getCurrencySections should expose the injected currency map");
    }

    @Test
    void itDefaultsTimerToZeroWhenUnset() {
        final AtomicInvestorSection investorSection = new AtomicInvestorSection(new EvaluationEnvironmentBuilder());

        assertEquals(0, investorSection.getTimer(),
            "getTimer should default to zero when no timer is configured");
        assertNull(investorSection.getCurrencySections(),
            "getCurrencySections should return null when no currencies are configured");
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}

