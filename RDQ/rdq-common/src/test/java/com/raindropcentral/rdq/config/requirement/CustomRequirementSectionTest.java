package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomRequirementSectionTest {

    @Test
    void itPrefersPrimaryFieldsWhenPresent() throws Exception {
        final CustomRequirementSection section = new CustomRequirementSection(new EvaluationEnvironmentBuilder());

        setField(section, "customScript", "console.log('primary');");
        setField(section, "script", "console.log('alias');");
        setField(section, "scriptLanguage", "groovy");
        setField(section, "language", "python");

        assertEquals("console.log('primary');", section.getCustomScript(),
                "getCustomScript should prioritize customScript over script");
        assertEquals("groovy", section.getScriptLanguage(),
                "getScriptLanguage should prioritize scriptLanguage over language");
    }

    @Test
    void itUsesAliasFieldsWhenPrimaryMissing() throws Exception {
        final CustomRequirementSection section = new CustomRequirementSection(new EvaluationEnvironmentBuilder());

        setField(section, "customScript", null);
        setField(section, "script", "console.log('alias');");
        setField(section, "scriptLanguage", null);
        setField(section, "language", "python");

        assertEquals("console.log('alias');", section.getCustomScript(),
                "getCustomScript should fall back to script when customScript unset");
        assertEquals("python", section.getScriptLanguage(),
                "getScriptLanguage should fall back to language when scriptLanguage unset");
    }

    @Test
    void itMergesCustomDataAndParametersWithAliasOverrides() throws Exception {
        final CustomRequirementSection section = new CustomRequirementSection(new EvaluationEnvironmentBuilder());

        final Map<String, Object> customData = new HashMap<>();
        customData.put("shared", "primary");
        customData.put("primaryOnly", 42);
        final Map<String, Object> data = new HashMap<>();
        data.put("shared", "alias");
        data.put("aliasOnly", true);

        final Map<String, String> parameters = new HashMap<>();
        parameters.put("shared", "base");
        parameters.put("baseOnly", "value");
        final Map<String, String> params = new HashMap<>();
        params.put("shared", "override");
        params.put("aliasOnly", "value");

        setField(section, "customData", customData);
        setField(section, "data", data);
        setField(section, "parameters", parameters);
        setField(section, "params", params);

        final Map<String, Object> mergedData = section.getCustomData();
        assertEquals(3, mergedData.size(), "getCustomData should merge primary and alias entries");
        assertEquals("alias", mergedData.get("shared"),
                "getCustomData should allow alias entries to override primary ones");
        assertEquals(42, mergedData.get("primaryOnly"));
        assertEquals(true, mergedData.get("aliasOnly"));

        final Map<String, String> mergedParams = section.getParameters();
        assertEquals(3, mergedParams.size(), "getParameters should merge primary and alias entries");
        assertEquals("override", mergedParams.get("shared"),
                "getParameters should allow alias entries to override primary ones");
        assertEquals("value", mergedParams.get("baseOnly"));
        assertEquals("value", mergedParams.get("aliasOnly"));
    }

    @Test
    void itProvidesAndOverridesCachingDefaults() throws Exception {
        final CustomRequirementSection section = new CustomRequirementSection(new EvaluationEnvironmentBuilder());

        assertFalse(section.getConsumeOnComplete(), "getConsumeOnComplete should default to false");
        assertFalse(section.getCacheResult(), "getCacheResult should default to false");
        assertEquals(300L, section.getCacheDuration(), "getCacheDuration should default to 300 seconds");

        setField(section, "consumeOnComplete", true);
        setField(section, "cacheResult", true);
        setField(section, "cacheDuration", 1200L);

        assertTrue(section.getConsumeOnComplete(), "getConsumeOnComplete should return overridden value");
        assertTrue(section.getCacheResult(), "getCacheResult should return overridden value");
        assertEquals(1200L, section.getCacheDuration(),
                "getCacheDuration should return overridden value when provided");
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
