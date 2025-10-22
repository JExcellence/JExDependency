package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrencyRequirementSectionTest {

    @Test
    void itProvidesSafeDefaultsForUnsetFields() {
        final CurrencyRequirementSection section = new CurrencyRequirementSection(new EvaluationEnvironmentBuilder());

        assertTrue(section.getConsumeOnComplete(),
            "getConsumeOnComplete should default to true when unset");
        assertEquals("money", section.getCurrencyType(),
            "getCurrencyType should default to 'money' when unset");
        assertEquals(0.0, section.getCurrencyAmount(),
            "getCurrencyAmount should default to 0.0 when unset");
        assertEquals("vault", section.getCurrencyPlugin(),
            "getCurrencyPlugin should default to 'vault' when unset");
    }

    @Test
    void itMergesRequiredCurrencySourcesWithoutDuplicates() throws Exception {
        final CurrencyRequirementSection section = new CurrencyRequirementSection(new EvaluationEnvironmentBuilder());

        final Map<String, Double> multiCurrency = new HashMap<>();
        multiCurrency.put("gold", 10.0);
        multiCurrency.put("silver", 5.0);

        setField(section, "requiredCurrencies", multiCurrency);
        setField(section, "currency", "gold");
        setField(section, "amount", 99.9);

        final Map<String, Double> result = section.getRequiredCurrencies();

        assertEquals(2, result.size(),
            "getRequiredCurrencies should merge sources without duplicating keys");
        assertEquals(99.9, result.get("gold"),
            "Alias fields should overwrite the existing entry for the same currency");
        assertEquals(5.0, result.get("silver"),
            "Existing entries should remain intact when merging alias fields");
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
