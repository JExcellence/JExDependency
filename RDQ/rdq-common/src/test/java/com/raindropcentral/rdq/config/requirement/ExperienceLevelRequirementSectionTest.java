package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperienceLevelRequirementSectionTest {

    @Test
    void defaultsAreReturnedWhenFieldsAreUnset() {
        ExperienceLevelRequirementSection section = new ExperienceLevelRequirementSection(new EvaluationEnvironmentBuilder());

        assertTrue(section.getConsumeOnComplete(), "consumeOnComplete should default to true");
        assertEquals(0, section.getRequiredLevel(), "required level should default to 0");
        assertEquals("LEVEL", section.getExperienceType(), "experience type should default to LEVEL");
    }

    @Test
    void reflectionInjectedValuesOverrideDefaults() throws Exception {
        ExperienceLevelRequirementSection section = new ExperienceLevelRequirementSection(new EvaluationEnvironmentBuilder());

        setField(section, "consumeOnComplete", Boolean.FALSE);
        setField(section, "requiredExperience", 12);
        setField(section, "requiredType", "POINTS");

        assertFalse(section.getConsumeOnComplete(), "consumeOnComplete should reflect injected value");
        assertEquals(12, section.getRequiredLevel(), "required level should reflect injected value");
        assertEquals("POINTS", section.getExperienceType(), "experience type should reflect injected value");
    }

    private void setField(ExperienceLevelRequirementSection section, String fieldName, Object value) throws Exception {
        Field field = ExperienceLevelRequirementSection.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(section, value);
    }
}
