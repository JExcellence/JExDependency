package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChoiceRequirementSectionTest {

    @Test
    void itAggregatesChoicesFromAllSources() throws Exception {
        final ChoiceRequirementSection section = new ChoiceRequirementSection(new EvaluationEnvironmentBuilder());

        final BaseRequirementSection first = new BaseRequirementSection(new EvaluationEnvironmentBuilder());
        final BaseRequirementSection second = new BaseRequirementSection(new EvaluationEnvironmentBuilder());
        final BaseRequirementSection third = new BaseRequirementSection(new EvaluationEnvironmentBuilder());
        final BaseRequirementSection fourth = new BaseRequirementSection(new EvaluationEnvironmentBuilder());

        final List<BaseRequirementSection> primaryChoices = new ArrayList<>();
        primaryChoices.add(first);
        primaryChoices.add(second);

        final List<BaseRequirementSection> alternateChoices = new ArrayList<>();
        alternateChoices.add(third);

        final Map<String, BaseRequirementSection> namedChoices = new LinkedHashMap<>();
        namedChoices.put("fourth", fourth);

        setField(section, "choices", primaryChoices);
        setField(section, "choiceList", alternateChoices);
        setField(section, "choiceMap", namedChoices);

        final List<BaseRequirementSection> combined = section.getChoices();
        assertEquals(List.of(first, second, third, fourth), combined, "getChoices should aggregate choices, choiceList, and choiceMap values");
        assertSame(namedChoices, section.getChoiceMap(), "getChoiceMap should return the configured map when present");
    }

    @Test
    void itProvidesExpectedDefaultsForFlagsDescriptionsAndMaximums() throws Exception {
        final ChoiceRequirementSection section = new ChoiceRequirementSection(new EvaluationEnvironmentBuilder());

        setField(section, "allowPartialProgress", null);
        assertTrue(section.getAllowPartialProgress(), "allowPartialProgress should default to true when unset");
        setField(section, "allowPartialProgress", Boolean.FALSE);
        assertFalse(section.getAllowPartialProgress(), "allowPartialProgress should return configured value when set");

        setField(section, "mutuallyExclusive", null);
        assertFalse(section.getMutuallyExclusive(), "mutuallyExclusive should default to false when unset");
        setField(section, "mutuallyExclusive", Boolean.TRUE);
        assertTrue(section.getMutuallyExclusive(), "mutuallyExclusive should return configured value when set");

        setField(section, "allowChoiceChange", null);
        assertTrue(section.getAllowChoiceChange(), "allowChoiceChange should default to true when unset");
        setField(section, "allowChoiceChange", Boolean.FALSE);
        assertFalse(section.getAllowChoiceChange(), "allowChoiceChange should return configured value when set");

        setField(section, "description", null);
        setField(section, "choiceDescription", null);
        assertEquals("", section.getDescription(), "getDescription should default to an empty string when no description provided");

        setField(section, "choiceDescription", "Fallback description");
        assertEquals("Fallback description", section.getDescription(), "getDescription should prefer choiceDescription when primary description missing");

        setField(section, "description", "Primary description");
        assertEquals("Primary description", section.getDescription(), "getDescription should prefer primary description when available");

        final BaseRequirementSection first = new BaseRequirementSection(new EvaluationEnvironmentBuilder());
        final BaseRequirementSection second = new BaseRequirementSection(new EvaluationEnvironmentBuilder());
        final BaseRequirementSection third = new BaseRequirementSection(new EvaluationEnvironmentBuilder());
        final BaseRequirementSection fourth = new BaseRequirementSection(new EvaluationEnvironmentBuilder());

        final List<BaseRequirementSection> primaryChoices = new ArrayList<>();
        primaryChoices.add(first);
        primaryChoices.add(second);

        final List<BaseRequirementSection> alternateChoices = new ArrayList<>();
        alternateChoices.add(third);

        final Map<String, BaseRequirementSection> namedChoices = new LinkedHashMap<>();
        namedChoices.put("fourth", fourth);

        setField(section, "choices", primaryChoices);
        setField(section, "choiceList", alternateChoices);
        setField(section, "choiceMap", namedChoices);

        setField(section, "maximumRequired", null);
        setField(section, "mutuallyExclusive", Boolean.TRUE);
        assertEquals(1, section.getMaximumRequired(), "getMaximumRequired should limit to one when mutuallyExclusive is true");

        setField(section, "mutuallyExclusive", Boolean.FALSE);
        assertEquals(4, section.getMaximumRequired(), "getMaximumRequired should default to total choices when not mutually exclusive");

        setField(section, "maximumRequired", 2);
        assertEquals(2, section.getMaximumRequired(), "getMaximumRequired should respect explicitly configured values");
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final Field field = locateField(target.getClass(), name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Field locateField(final Class<?> type, final String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (final NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
