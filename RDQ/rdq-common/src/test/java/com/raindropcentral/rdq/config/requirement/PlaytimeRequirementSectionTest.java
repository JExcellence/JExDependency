package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaytimeRequirementSectionTest {

    @Test
    void itConvertsRequiredPlaytimeAcrossSupportedUnits() throws Exception {
        final PlaytimeRequirementSection secondsSection = newSection();
        setField(secondsSection, "requiredPlaytimeSeconds", 120L);
        assertEquals(120L, secondsSection.getRequiredPlaytimeSeconds(),
                "requiredPlaytimeSeconds should return the configured seconds value");

        final PlaytimeRequirementSection minutesSection = newSection();
        setField(minutesSection, "requiredPlaytimeMinutes", 2L);
        assertEquals(TimeUnit.MINUTES.toSeconds(2L), minutesSection.getRequiredPlaytimeSeconds(),
                "requiredPlaytimeMinutes should be converted to seconds");

        final PlaytimeRequirementSection hoursSection = newSection();
        setField(hoursSection, "requiredPlaytimeHours", 3L);
        assertEquals(TimeUnit.HOURS.toSeconds(3L), hoursSection.getRequiredPlaytimeSeconds(),
                "requiredPlaytimeHours should be converted to seconds");

        final PlaytimeRequirementSection daysSection = newSection();
        setField(daysSection, "requiredPlaytimeDays", 4L);
        assertEquals(TimeUnit.DAYS.toSeconds(4L), daysSection.getRequiredPlaytimeSeconds(),
                "requiredPlaytimeDays should be converted to seconds");

        final PlaytimeRequirementSection timeSectionHours = newSection();
        setField(timeSectionHours, "time", 5L);
        setField(timeSectionHours, "timeUnit", "H");
        assertEquals(TimeUnit.HOURS.toSeconds(5L), timeSectionHours.getRequiredPlaytimeSeconds(),
                "time with unit hours should be converted using the provided unit");

        final PlaytimeRequirementSection timeSectionWeeks = newSection();
        setField(timeSectionWeeks, "time", 1L);
        setField(timeSectionWeeks, "timeUnit", "weeks");
        assertEquals(TimeUnit.DAYS.toSeconds(7L), timeSectionWeeks.getRequiredPlaytimeSeconds(),
                "time with unit weeks should be converted using seven days");

        final PlaytimeRequirementSection defaultSection = newSection();
        assertEquals(0L, defaultSection.getRequiredPlaytimeSeconds(),
                "When no values are configured the default should be 0 seconds");
    }

    @Test
    void itConvertsWorldPlaytimeAcrossSupportedUnits() throws Exception {
        final PlaytimeRequirementSection secondsSection = newSection();
        setField(secondsSection, "worldPlaytimeSeconds", 45L);
        assertEquals(45L, secondsSection.getWorldPlaytimeSeconds(),
                "worldPlaytimeSeconds should return the configured seconds value");

        final PlaytimeRequirementSection minutesSection = newSection();
        setField(minutesSection, "worldPlaytimeMinutes", 10L);
        assertEquals(TimeUnit.MINUTES.toSeconds(10L), minutesSection.getWorldPlaytimeSeconds(),
                "worldPlaytimeMinutes should be converted to seconds");

        final PlaytimeRequirementSection hoursSection = newSection();
        setField(hoursSection, "worldPlaytimeHours", 2L);
        assertEquals(TimeUnit.HOURS.toSeconds(2L), hoursSection.getWorldPlaytimeSeconds(),
                "worldPlaytimeHours should be converted to seconds");

        final PlaytimeRequirementSection daysSection = newSection();
        setField(daysSection, "worldPlaytimeDays", 1L);
        assertEquals(TimeUnit.DAYS.toSeconds(1L), daysSection.getWorldPlaytimeSeconds(),
                "worldPlaytimeDays should be converted to seconds");

        final PlaytimeRequirementSection defaultSection = newSection();
        assertEquals(0L, defaultSection.getWorldPlaytimeSeconds(),
                "When no world values are configured the default should be 0 seconds");
    }

    @Test
    void itDetectsWorldSpecificConfigurationsAndAutoChoosesTotalPlaytime() throws Exception {
        final PlaytimeRequirementSection worldMapSection = newSection();
        final Map<String, Long> worldMap = new HashMap<>();
        worldMap.put("overworld", 120L);
        setField(worldMapSection, "worldPlaytimeRequirements", worldMap);

        assertTrue(worldMapSection.hasWorldSpecificConfiguration(),
                "A configured world map should flag world-specific configuration");
        assertFalse(worldMapSection.getUseTotalPlaytime(),
                "World-specific configuration should auto-disable total playtime");

        final PlaytimeRequirementSection worldsListSection = newSection();
        setField(worldsListSection, "worlds", List.of("alpha", " beta "));
        setField(worldsListSection, "worldPlaytimeMinutes", 5L);

        assertTrue(worldsListSection.hasWorldSpecificConfiguration(),
                "A configured worlds list with playtime should flag world-specific configuration");
        assertEquals(TimeUnit.MINUTES.toSeconds(5L), worldsListSection.getWorldPlaytimeSeconds(),
                "World playtime minutes should be converted when supplied via the worlds list");
        assertFalse(worldsListSection.getUseTotalPlaytime(),
                "Derived world configuration should auto-disable total playtime");

        final PlaytimeRequirementSection explicitToggleSection = newSection();
        assertTrue(explicitToggleSection.getUseTotalPlaytime(),
                "When no configuration exists total playtime should be used by default");

        setField(explicitToggleSection, "useTotalPlaytime", Boolean.FALSE);
        assertFalse(explicitToggleSection.getUseTotalPlaytime(),
                "Explicitly disabling total playtime should be honored");

        setField(explicitToggleSection, "useTotalPlaytime", Boolean.TRUE);
        assertTrue(explicitToggleSection.getUseTotalPlaytime(),
                "Explicitly enabling total playtime should be honored");
    }

    @Test
    void itValidatesConfigurationAndSurfacesErrorStates() throws Exception {
        final PlaytimeRequirementSection missingRequirementSection = newSection();
        assertThrows(IllegalStateException.class, missingRequirementSection::validate,
                "validate should require at least one playtime declaration");

        final PlaytimeRequirementSection inconsistentSection = newSection();
        setField(inconsistentSection, "requiredPlaytimeSeconds", 30L);
        setField(inconsistentSection, "useTotalPlaytime", Boolean.FALSE);
        assertThrows(IllegalStateException.class, inconsistentSection::validate,
                "validate should reject useTotalPlaytime=false without world-specific configuration");

        final PlaytimeRequirementSection blankWorldSection = newSection();
        final Map<String, Long> blankWorldMap = new HashMap<>();
        blankWorldMap.put("  ", 90L);
        setField(blankWorldSection, "worldPlaytimeRequirements", blankWorldMap);
        assertThrows(IllegalStateException.class, blankWorldSection::validate,
                "validate should reject blank world names");

        final PlaytimeRequirementSection nonPositiveSection = newSection();
        final Map<String, Long> nonPositiveMap = new HashMap<>();
        nonPositiveMap.put("nether", 0L);
        setField(nonPositiveSection, "worldPlaytimeRequirements", nonPositiveMap);
        assertThrows(IllegalStateException.class, nonPositiveSection::validate,
                "validate should reject non-positive world playtime values");
    }

    private static PlaytimeRequirementSection newSection() {
        return new PlaytimeRequirementSection(new EvaluationEnvironmentBuilder());
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
