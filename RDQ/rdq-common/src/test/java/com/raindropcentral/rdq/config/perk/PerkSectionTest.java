package com.raindropcentral.rdq.config.perk;

import com.raindropcentral.rdq.config.requirement.RequirementSection;
import com.raindropcentral.rdq.config.reward.RewardSection;
import com.raindropcentral.rplatform.config.permission.PermissionAmplifierSection;
import com.raindropcentral.rplatform.config.permission.PermissionCooldownSection;
import com.raindropcentral.rplatform.config.permission.PermissionDurationSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerkSectionTest {

    @Test
    void itProvidesDefaultSectionsAndMapsWhenUnset() {
        final PerkSection section = new PerkSection(new EvaluationEnvironmentBuilder());

        final PerkSettingsSection firstSettings = section.getPerkSettings();
        final PerkSettingsSection secondSettings = section.getPerkSettings();
        assertNotNull(firstSettings, "getPerkSettings should never return null when unset");
        assertNotSame(firstSettings, secondSettings, "getPerkSettings should create new defaults when unset");

        final PermissionCooldownSection firstCooldowns = section.getPermissionCooldowns();
        final PermissionCooldownSection secondCooldowns = section.getPermissionCooldowns();
        assertNotNull(firstCooldowns, "getPermissionCooldowns should never return null when unset");
        assertNotSame(firstCooldowns, secondCooldowns, "getPermissionCooldowns should create new defaults when unset");

        final PermissionAmplifierSection firstAmplifiers = section.getPermissionAmplifiers();
        final PermissionAmplifierSection secondAmplifiers = section.getPermissionAmplifiers();
        assertNotNull(firstAmplifiers, "getPermissionAmplifiers should never return null when unset");
        assertNotSame(firstAmplifiers, secondAmplifiers, "getPermissionAmplifiers should create new defaults when unset");

        final PermissionDurationSection firstDurations = section.getPermissionDurations();
        final PermissionDurationSection secondDurations = section.getPermissionDurations();
        assertNotNull(firstDurations, "getPermissionDurations should never return null when unset");
        assertNotSame(firstDurations, secondDurations, "getPermissionDurations should create new defaults when unset");

        final Map<String, PluginCurrencySection> firstCosts = section.getCosts();
        final Map<String, PluginCurrencySection> secondCosts = section.getCosts();
        assertNotNull(firstCosts, "getCosts should never return null when unset");
        assertTrue(firstCosts.isEmpty(), "getCosts should default to an empty map when unset");
        assertNotSame(firstCosts, secondCosts, "getCosts should create new maps when unset");

        final Map<String, RequirementSection> firstRequirements = section.getRequirements();
        final Map<String, RequirementSection> secondRequirements = section.getRequirements();
        assertNotNull(firstRequirements, "getRequirements should never return null when unset");
        assertTrue(firstRequirements.isEmpty(), "getRequirements should default to an empty map when unset");
        assertNotSame(firstRequirements, secondRequirements, "getRequirements should create new maps when unset");

        final Map<String, RewardSection> firstRewards = section.getRewards();
        final Map<String, RewardSection> secondRewards = section.getRewards();
        assertNotNull(firstRewards, "getRewards should never return null when unset");
        assertTrue(firstRewards.isEmpty(), "getRewards should default to an empty map when unset");
        assertNotSame(firstRewards, secondRewards, "getRewards should create new maps when unset");
    }

    @Test
    void itReturnsExistingInstancesWhenFieldsPopulated() throws Exception {
        final PerkSection section = new PerkSection(new EvaluationEnvironmentBuilder());

        final PerkSettingsSection expectedSettings = new PerkSettingsSection(new EvaluationEnvironmentBuilder());
        final PermissionCooldownSection expectedCooldowns = new PermissionCooldownSection(new EvaluationEnvironmentBuilder());
        final PermissionAmplifierSection expectedAmplifiers = new PermissionAmplifierSection(new EvaluationEnvironmentBuilder());
        final PermissionDurationSection expectedDurations = new PermissionDurationSection(new EvaluationEnvironmentBuilder());
        final Map<String, PluginCurrencySection> expectedCosts = new HashMap<>();
        final Map<String, RequirementSection> expectedRequirements = new HashMap<>();
        final Map<String, RewardSection> expectedRewards = new HashMap<>();

        setField(section, "perkSettings", expectedSettings);
        setField(section, "permissionCooldowns", expectedCooldowns);
        setField(section, "permissionAmplifiers", expectedAmplifiers);
        setField(section, "permissionDurations", expectedDurations);
        setField(section, "costs", expectedCosts);
        setField(section, "requirements", expectedRequirements);
        setField(section, "rewards", expectedRewards);

        assertSame(expectedSettings, section.getPerkSettings(), "getPerkSettings should return the configured instance");
        assertSame(expectedCooldowns, section.getPermissionCooldowns(), "getPermissionCooldowns should return the configured instance");
        assertSame(expectedAmplifiers, section.getPermissionAmplifiers(), "getPermissionAmplifiers should return the configured instance");
        assertSame(expectedDurations, section.getPermissionDurations(), "getPermissionDurations should return the configured instance");
        assertSame(expectedCosts, section.getCosts(), "getCosts should return the configured map");
        assertSame(expectedRequirements, section.getRequirements(), "getRequirements should return the configured map");
        assertSame(expectedRewards, section.getRewards(), "getRewards should return the configured map");
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
