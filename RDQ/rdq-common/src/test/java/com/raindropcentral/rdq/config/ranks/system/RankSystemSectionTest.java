package com.raindropcentral.rdq.config.ranks.system;

import com.raindropcentral.rdq.config.ranks.rank.DefaultRankSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class RankSystemSectionTest {

    @Test
    void itReturnsFreshInstancesWhenSectionsAreUnset() {
        final RankSystemSection section = new RankSystemSection(new EvaluationEnvironmentBuilder());

        final SystemSettingsSection firstSettings = section.getSettings();
        final SystemSettingsSection secondSettings = section.getSettings();
        assertNotNull(firstSettings, "getSettings should provide a default when unset");
        assertNotNull(secondSettings, "getSettings should provide a default when unset");
        assertNotSame(firstSettings, secondSettings, "getSettings should return a new instance while unset");

        final FinalRankRuleSection firstFinalRankRule = section.getFinalRankRule();
        final FinalRankRuleSection secondFinalRankRule = section.getFinalRankRule();
        assertNotNull(firstFinalRankRule, "getFinalRankRule should provide a default when unset");
        assertNotNull(secondFinalRankRule, "getFinalRankRule should provide a default when unset");
        assertNotSame(firstFinalRankRule, secondFinalRankRule,
            "getFinalRankRule should return a new instance while unset");

        final ProgressionRuleSection firstProgressionRule = section.getProgressionRule();
        final ProgressionRuleSection secondProgressionRule = section.getProgressionRule();
        assertNotNull(firstProgressionRule, "getProgressionRule should provide a default when unset");
        assertNotNull(secondProgressionRule, "getProgressionRule should provide a default when unset");
        assertNotSame(firstProgressionRule, secondProgressionRule,
            "getProgressionRule should return a new instance while unset");

        final NotificationSection firstNotification = section.getNotification();
        final NotificationSection secondNotification = section.getNotification();
        assertNotNull(firstNotification, "getNotification should provide a default when unset");
        assertNotNull(secondNotification, "getNotification should provide a default when unset");
        assertNotSame(firstNotification, secondNotification,
            "getNotification should return a new instance while unset");

        final DefaultRankSection firstDefaultRank = section.getDefaultRank();
        final DefaultRankSection secondDefaultRank = section.getDefaultRank();
        assertNotNull(firstDefaultRank, "getDefaultRank should provide a default when unset");
        assertNotNull(secondDefaultRank, "getDefaultRank should provide a default when unset");
        assertNotSame(firstDefaultRank, secondDefaultRank,
            "getDefaultRank should return a new instance while unset");
    }

    @Test
    void itReturnsInjectedSectionsWhenPresent() throws Exception {
        final RankSystemSection section = new RankSystemSection(new EvaluationEnvironmentBuilder());

        final SystemSettingsSection injectedSettings = new SystemSettingsSection(new EvaluationEnvironmentBuilder());
        setField(section, "settings", injectedSettings);
        assertSame(injectedSettings, section.getSettings(), "getSettings should return the injected section");
        assertSame(injectedSettings, section.getSettings(), "getSettings should not replace the injected section");

        final FinalRankRuleSection injectedFinalRankRule = new FinalRankRuleSection(new EvaluationEnvironmentBuilder());
        setField(section, "finalRankRule", injectedFinalRankRule);
        assertSame(injectedFinalRankRule, section.getFinalRankRule(),
            "getFinalRankRule should return the injected section");
        assertSame(injectedFinalRankRule, section.getFinalRankRule(),
            "getFinalRankRule should not replace the injected section");

        final ProgressionRuleSection injectedProgressionRule = new ProgressionRuleSection(new EvaluationEnvironmentBuilder());
        setField(section, "progressionRule", injectedProgressionRule);
        assertSame(injectedProgressionRule, section.getProgressionRule(),
            "getProgressionRule should return the injected section");
        assertSame(injectedProgressionRule, section.getProgressionRule(),
            "getProgressionRule should not replace the injected section");

        final NotificationSection injectedNotification = new NotificationSection(new EvaluationEnvironmentBuilder());
        setField(section, "notification", injectedNotification);
        assertSame(injectedNotification, section.getNotification(),
            "getNotification should return the injected section");
        assertSame(injectedNotification, section.getNotification(),
            "getNotification should not replace the injected section");

        final DefaultRankSection injectedDefaultRank = new DefaultRankSection(new EvaluationEnvironmentBuilder());
        setField(section, "defaultRank", injectedDefaultRank);
        assertSame(injectedDefaultRank, section.getDefaultRank(),
            "getDefaultRank should return the injected section");
        assertSame(injectedDefaultRank, section.getDefaultRank(),
            "getDefaultRank should not replace the injected section");
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
