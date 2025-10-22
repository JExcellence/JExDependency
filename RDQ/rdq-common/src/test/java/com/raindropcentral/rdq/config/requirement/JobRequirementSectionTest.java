package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobRequirementSectionTest {

    @Test
    void itMergesAliasFieldsWhenPopulated() throws Exception {
        final JobRequirementSection section = new JobRequirementSection(new EvaluationEnvironmentBuilder());

        final Map<String, Integer> requiredJobs = new HashMap<>();
        requiredJobs.put("farmer", 2);
        requiredJobs.put("miner", 3);

        final Map<String, Integer> jobs = new HashMap<>();
        jobs.put("miner", 5);
        jobs.put("builder", 4);

        setField(section, "requiredJob", "hunter");
        setField(section, "job", "ignoredJob");
        setField(section, "requiredJobLevel", 7);
        setField(section, "jobLevel", 3);
        setField(section, "requiredJobs", requiredJobs);
        setField(section, "jobs", jobs);

        assertEquals("hunter", section.getRequiredJob(), "requiredJob field should take precedence over job alias");
        assertEquals(7, section.getRequiredJobLevel(), "requiredJobLevel field should take precedence over jobLevel alias");

        final Map<String, Integer> mergedJobs = section.getRequiredJobs();
        assertEquals(4, mergedJobs.size(), "Merged jobs should include entries from maps and single job");
        assertEquals(2, mergedJobs.get("farmer"));
        assertEquals(5, mergedJobs.get("miner"), "jobs alias map should override entries from requiredJobs map");
        assertEquals(4, mergedJobs.get("builder"));
        assertEquals(7, mergedJobs.get("hunter"), "Single job should be added using resolved level");
    }

    @Test
    void itProvidesFallbackValuesWhenUnset() {
        final JobRequirementSection section = new JobRequirementSection(new EvaluationEnvironmentBuilder());

        assertEquals("", section.getRequiredJob(), "getRequiredJob should default to an empty string");
        assertEquals(1, section.getRequiredJobLevel(), "getRequiredJobLevel should default to 1");
        assertTrue(section.getRequiredJobs().isEmpty(), "getRequiredJobs should return an empty map when unset");
    }

    @Test
    void itResolvesPluginConsumptionAndRequireAllDefaults() throws Exception {
        final JobRequirementSection section = new JobRequirementSection(new EvaluationEnvironmentBuilder());

        assertEquals("jobs", section.getJobPlugin(), "Default job plugin should be jobs");
        assertFalse(section.getConsumeOnComplete(), "Default consumeOnComplete should be false");
        assertTrue(section.getRequireAll(), "Default requireAll should be true");

        setField(section, "jobPlugin", "jobsreborn");
        setField(section, "consumeOnComplete", true);
        setField(section, "requireAll", false);

        assertEquals("jobsreborn", section.getJobPlugin(), "Configured job plugin should override default");
        assertTrue(section.getConsumeOnComplete(), "Configured consumeOnComplete should override default");
        assertFalse(section.getRequireAll(), "Configured requireAll should override default");
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
