package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillRequirementSectionTest {

    @Test
    void itMergesAliasFieldsWhenPopulated() throws Exception {
        final SkillRequirementSection section = new SkillRequirementSection(new EvaluationEnvironmentBuilder());

        final Map<String, Integer> requiredSkills = new HashMap<>();
        requiredSkills.put("archery", 2);
        requiredSkills.put("mining", 3);

        final Map<String, Integer> skills = new HashMap<>();
        skills.put("mining", 5);
        skills.put("fishing", 4);

        setField(section, "requiredSkill", "alchemy");
        setField(section, "skill", "ignoredSkill");
        setField(section, "requiredSkillLevel", 8);
        setField(section, "skillLevel", 3);
        setField(section, "requiredSkills", requiredSkills);
        setField(section, "skills", skills);

        assertEquals("alchemy", section.getRequiredSkill(), "requiredSkill field should take precedence over skill alias");
        assertEquals(8, section.getRequiredSkillLevel(), "requiredSkillLevel field should take precedence over skillLevel alias");

        final Map<String, Integer> mergedSkills = section.getRequiredSkills();
        assertEquals(4, mergedSkills.size(), "Merged skills should include entries from maps and single skill");
        assertEquals(2, mergedSkills.get("archery"));
        assertEquals(5, mergedSkills.get("mining"), "skills alias map should override entries from requiredSkills map");
        assertEquals(4, mergedSkills.get("fishing"));
        assertEquals(8, mergedSkills.get("alchemy"), "Single skill should be added using resolved level");
    }

    @Test
    void itProvidesFallbackValuesWhenUnset() {
        final SkillRequirementSection section = new SkillRequirementSection(new EvaluationEnvironmentBuilder());

        assertEquals("", section.getRequiredSkill(), "getRequiredSkill should default to an empty string");
        assertEquals(1, section.getRequiredSkillLevel(), "getRequiredSkillLevel should default to 1");
        assertTrue(section.getRequiredSkills().isEmpty(), "getRequiredSkills should return an empty map when unset");
    }

    @Test
    void itResolvesPluginAndConsumptionDefaults() throws Exception {
        final SkillRequirementSection section = new SkillRequirementSection(new EvaluationEnvironmentBuilder());

        assertEquals("mcmmo", section.getSkillPlugin(), "Default skill plugin should be mcmmo");
        assertFalse(section.getConsumeOnComplete(), "Default consumeOnComplete should be false");

        setField(section, "skillPlugin", "skillapi");
        setField(section, "consumeOnComplete", true);

        assertEquals("skillapi", section.getSkillPlugin(), "Configured skill plugin should override default");
        assertTrue(section.getConsumeOnComplete(), "Configured consumeOnComplete should override default");
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
