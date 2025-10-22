package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeBasedRequirementSectionTest {

    @Test
    void itProvidesDefaultValuesWhenUnset() {
        final TimeBasedRequirementSection section = new TimeBasedRequirementSection(new EvaluationEnvironmentBuilder());

        assertEquals(0L, section.getTimeConstraintSeconds(), "getTimeConstraintSeconds should default to 0L when unset");
        assertEquals(0L, section.getCooldownSeconds(), "getCooldownSeconds should default to 0L when unset");
        assertEquals("UTC", section.getTimeZone(), "getTimeZone should default to UTC when unset");
        assertFalse(section.getRecurring(), "getRecurring should default to false when unset");
        assertTrue(section.getUseRealTime(), "getUseRealTime should default to true when unset");

        final List<Integer> activeDays = section.getActiveDays();
        final List<String> activeDates = section.getActiveDates();
        assertNotNull(activeDays, "getActiveDays should never return null");
        assertNotNull(activeDates, "getActiveDates should never return null");
        assertTrue(activeDays.isEmpty(), "getActiveDays should return an empty list by default");
        assertTrue(activeDates.isEmpty(), "getActiveDates should return an empty list by default");
        assertNotSame(activeDays, section.getActiveDays(), "getActiveDays should create defensive copies when unset");
        assertNotSame(activeDates, section.getActiveDates(), "getActiveDates should create defensive copies when unset");
    }

    @Test
    void itReturnsConfiguredValuesAndRetainsListMutability() throws Exception {
        final TimeBasedRequirementSection section = new TimeBasedRequirementSection(new EvaluationEnvironmentBuilder());

        setField(section, "timeConstraintSeconds", 90L);
        setField(section, "cooldownSeconds", 45L);
        setField(section, "startTime", "08:00");
        setField(section, "endTime", "18:30");
        setField(section, "timeZone", "America/New_York");
        setField(section, "recurring", true);

        final List<Integer> configuredDays = new ArrayList<>(List.of(1, 3, 5));
        final List<String> configuredDates = new ArrayList<>(List.of("2024-12-24"));
        setField(section, "activeDays", configuredDays);
        setField(section, "activeDates", configuredDates);
        setField(section, "useRealTime", false);

        assertEquals(90L, section.getTimeConstraintSeconds(), "getTimeConstraintSeconds should return the configured value");
        assertEquals(45L, section.getCooldownSeconds(), "getCooldownSeconds should return the configured value");
        assertEquals("08:00", section.getStartTime(), "getStartTime should return the configured value");
        assertEquals("18:30", section.getEndTime(), "getEndTime should return the configured value");
        assertEquals("America/New_York", section.getTimeZone(), "getTimeZone should return the configured value");
        assertTrue(section.getRecurring(), "getRecurring should return the configured value");
        assertFalse(section.getUseRealTime(), "getUseRealTime should return the configured value");

        final List<Integer> returnedDays = section.getActiveDays();
        final List<String> returnedDates = section.getActiveDates();
        assertSame(configuredDays, returnedDays, "getActiveDays should return the configured list instance");
        assertSame(configuredDates, returnedDates, "getActiveDates should return the configured list instance");

        returnedDays.add(7);
        returnedDates.add("2025-01-01");
        assertTrue(configuredDays.contains(7), "Configured activeDays list should remain mutable");
        assertTrue(configuredDates.contains("2025-01-01"), "Configured activeDates list should remain mutable");
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
