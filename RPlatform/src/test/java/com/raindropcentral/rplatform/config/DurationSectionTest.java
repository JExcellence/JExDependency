package com.raindropcentral.rplatform.config;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class DurationSectionTest {

    private DurationSection section;

    @BeforeEach
    void setUp() {
        section = new DurationSection(Mockito.mock(EvaluationEnvironmentBuilder.class));
    }

    @Test
    void rawDurationPrefersPrimaryKeyAndTrimsWhitespace() {
        setField("duration", " 15m ");
        setField("time", "30m");
        setField("period", "45m");

        assertEquals("15m", section.getRawDuration());
    }

    @Test
    void rawDurationFallsBackToAliasesWhenPrimaryMissing() {
        setField("duration", null);
        setField("time", " 10s ");
        setField("period", "5s");

        assertEquals("10s", section.getRawDuration());

        setField("time", "   ");
        assertEquals("5s", section.getRawDuration());
    }

    @Test
    void getSecondsParsesCompoundAndNumericDurations() {
        setField("duration", "1w 2d 3h 4m 5s");
        assertEquals(604800L + 172800L + 10800L + 240L + 5L, section.getSeconds());

        setField("duration", " 900 ");
        assertEquals(900L, section.getSeconds());
    }

    @Test
    void getSecondsParsesSimpleValueAndUnitPairs() {
        setField("duration", "15min");
        assertEquals(900L, section.getSeconds());
    }

    @Test
    void getSecondsFallsBackToStructuredOverrides() {
        setField("duration", "invalid");
        setField("seconds", 5L);
        assertEquals(5L, section.getSeconds());

        setField("seconds", null);
        setField("minutes", 2L);
        assertEquals(120L, section.getSeconds());

        setField("minutes", null);
        setField("hours", 1L);
        assertEquals(3600L, section.getSeconds());

        setField("hours", null);
        setField("days", 1L);
        assertEquals(86400L, section.getSeconds());
    }

    @Test
    void getSecondsDefaultsToZeroWhenUnconfigured() {
        assertEquals(0L, section.getSeconds());
    }

    @Test
    void derivedUnitConversionsUseResolvedSeconds() {
        setField("seconds", 120L);

        assertEquals(120000L, section.getMilliseconds());
        assertEquals(2L, section.getMinutes());
        assertEquals(0L, section.getHours());
        assertEquals(0L, section.getDays());
    }

    @Test
    void hasDurationReflectsPositiveSeconds() {
        assertFalse(section.hasDuration());

        setField("seconds", 30L);
        assertTrue(section.hasDuration());
    }

    @Test
    void formattedDurationProducesHumanReadableOutput() {
        setField("duration", "1d 2h 30m 15s");
        assertEquals("1 day 2 hours 30 minutes 15 seconds", section.getFormattedDuration());
    }

    @Test
    void formattedDurationReturnsZeroSecondsWhenEmpty() {
        assertEquals("0 seconds", section.getFormattedDuration());
    }

    @Test
    void validatePassesForValidConfiguration() {
        setField("duration", "45s");
        assertDoesNotThrow(section::validate);
    }

    @Test
    void validateThrowsForInvalidRawDuration() {
        setField("duration", "not-a-duration");
        assertThrows(IllegalStateException.class, section::validate);
    }

    @Test
    void validateThrowsForNegativeDuration() {
        setField("seconds", -5L);
        assertThrows(IllegalStateException.class, section::validate);
    }

    private void setField(final String fieldName, final Object value) {
        try {
            Field field = DurationSection.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(section, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to set field " + fieldName, e);
        }
    }
}
