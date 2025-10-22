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

class CompositeRequirementSectionTest {

    @Test
    void itNormalizesOperatorsAndDefaultsPartialProgress() throws Exception {
        final CompositeRequirementSection section = new CompositeRequirementSection(new EvaluationEnvironmentBuilder());

        setField(section, "allowPartialProgress", null);
        assertTrue(section.getAllowPartialProgress(), "allowPartialProgress should default to true when unset");

        setField(section, "allowPartialProgress", Boolean.FALSE);
        assertFalse(section.getAllowPartialProgress(), "allowPartialProgress should return configured value when set");

        setField(section, "operator", "xor");
        assertEquals("XOR", section.getOperator(), "getOperator should normalize operator field to upper-case");

        setField(section, "operator", null);
        setField(section, "compositeOperator", "or");
        assertEquals("OR", section.getOperator(), "getOperator should use compositeOperator alias when primary field unset");

        setField(section, "compositeOperator", null);
        assertEquals("AND", section.getOperator(), "getOperator should default to AND when no operator provided");
    }

    @Test
    void itAggregatesRequirementsAndComputesDefaultThresholds() throws Exception {
        final CompositeRequirementSection section = new CompositeRequirementSection(new EvaluationEnvironmentBuilder());

        final BaseRequirementSection first = new BaseRequirementSection(new EvaluationEnvironmentBuilder());
        final BaseRequirementSection second = new BaseRequirementSection(new EvaluationEnvironmentBuilder());
        final BaseRequirementSection third = new BaseRequirementSection(new EvaluationEnvironmentBuilder());

        final List<BaseRequirementSection> requirementList = new ArrayList<>();
        requirementList.add(first);
        requirementList.add(second);

        final Map<String, BaseRequirementSection> subRequirementMap = new LinkedHashMap<>();
        subRequirementMap.put("third", third);

        setField(section, "requirements", requirementList);
        setField(section, "subRequirements", subRequirementMap);
        setField(section, "operator", "and");

        final List<BaseRequirementSection> combined = section.getCompositeRequirements();
        assertEquals(List.of(first, second, third), combined, "getCompositeRequirements should merge list and map-backed requirements");
        assertSame(subRequirementMap, section.getSubRequirements(), "getSubRequirements should return configured map when present");

        assertEquals(3, section.getMinimumRequired(), "minimumRequired should default to size of requirements for AND operator");
        assertEquals(3, section.getMaximumRequired(), "maximumRequired should default to size of requirements for AND operator");

        setField(section, "operator", null);
        setField(section, "compositeOperator", "xor");

        assertEquals(1, section.getMinimumRequired(), "minimumRequired should default to 1 for XOR operator");
        assertEquals(1, section.getMaximumRequired(), "maximumRequired should default to 1 for XOR operator");
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
