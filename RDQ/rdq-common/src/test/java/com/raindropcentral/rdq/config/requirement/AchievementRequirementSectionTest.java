package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AchievementRequirementSectionTest {

    @Test
    void itPrioritizesSingleAchievementAndDeduplicatesCombinedSources() throws Exception {
        final AchievementRequirementSection section = new AchievementRequirementSection(new EvaluationEnvironmentBuilder());

        setField(section, "requiredAchievements", new ArrayList<>(List.of(
                "rdq.achievement.primary",
                "rdq.achievement.secondary"
        )));
        setField(section, "achievements", new ArrayList<>(List.of(
                "rdq.achievement.tertiary"
        )));
        setField(section, "requiredAchievement", "rdq.achievement.primary");
        setField(section, "achievement", "rdq.achievement.backup");

        final List<String> requiredAchievements = section.getRequiredAchievements();

        assertEquals(List.of(
                "rdq.achievement.primary",
                "rdq.achievement.secondary",
                "rdq.achievement.tertiary"
        ), requiredAchievements, "getRequiredAchievements should merge lists and exclude duplicate single entry");
        assertEquals("rdq.achievement.primary", section.getRequiredAchievement(), "getRequiredAchievement should prefer requiredAchievement over achievement");
    }

    @Test
    void itProvidesAndOverridesDefaultsForRequireAllAndPlugin() throws Exception {
        final AchievementRequirementSection section = new AchievementRequirementSection(new EvaluationEnvironmentBuilder());

        assertTrue(section.getRequireAll(), "getRequireAll should default to true when unset");
        assertEquals("advancedachievements", section.getAchievementPlugin(), "getAchievementPlugin should default to advancedachievements when unset");

        setField(section, "requireAll", Boolean.FALSE);
        setField(section, "achievementPlugin", "anotherachievements");

        assertEquals(Boolean.FALSE, section.getRequireAll(), "getRequireAll should reflect the configured value");
        assertEquals("anotherachievements", section.getAchievementPlugin(), "getAchievementPlugin should return the configured plugin identifier");
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
