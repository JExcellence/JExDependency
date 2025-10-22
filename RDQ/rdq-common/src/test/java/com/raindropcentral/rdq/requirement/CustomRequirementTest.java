package com.raindropcentral.rdq.requirement;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import be.seeseemelk.mockbukkit.plugin.MockPlugin;
import org.bukkit.Statistic;
import org.bukkit.permissions.PermissionAttachment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomRequirementTest {

    private ServerMock server;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.player = this.server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void itEvaluatesScriptTruthinessUsingPlayerContext() {
        final CustomRequirement requirement = new CustomRequirement(
                "return getLevel() >= 10 && getPlayTime() >= 60 && hasPermission(\"requirement.use\");"
        );

        this.player.setLevel(12);
        this.player.setStatistic(Statistic.PLAY_ONE_MINUTE, 20 * 120);
        assertFalse(requirement.isMet(this.player));

        final MockPlugin plugin = MockBukkit.createMockPlugin();
        final PermissionAttachment attachment = this.player.addAttachment(plugin, "requirement.use", true);
        assertTrue(requirement.isMet(this.player));

        this.player.setLevel(4);
        assertFalse(requirement.isMet(this.player));

        attachment.remove();
    }

    @Test
    void itCalculatesProgressFromScriptOrFallsBackToIsMet() {
        final CustomRequirement scriptedProgress = new CustomRequirement(
                CustomRequirement.CustomType.SCRIPT,
                "return getLevel() >= 5;",
                "return getLevel() / 10.0;",
                null,
                new HashMap<>(),
                null,
                true
        );

        this.player.setLevel(7);
        assertEquals(0.7D, scriptedProgress.calculateProgress(this.player), 1.0E-6);

        final CustomRequirement fallbackProgress = new CustomRequirement("return getLevel() >= 5;");

        assertEquals(1.0D, fallbackProgress.calculateProgress(this.player), 1.0E-6);

        this.player.setLevel(3);
        assertEquals(0.0D, fallbackProgress.calculateProgress(this.player), 1.0E-6);
    }

    @Test
    void itExecutesConsumeScriptWhenPresent() {
        final Map<String, Object> customData = new HashMap<>();
        customData.put("consumed", Boolean.FALSE);

        final CustomRequirement requirement = new CustomRequirement(
                CustomRequirement.CustomType.SCRIPT,
                "return true;",
                null,
                "customData.put(\"consumed\", Boolean.TRUE);",
                customData,
                null,
                true
        );

        requirement.consume(this.player);

        assertTrue((Boolean) requirement.getCustomData().get("consumed"));
    }

    @Test
    void itProvidesUtilityAccessorsAndValidation() throws Exception {
        final Map<String, Object> customData = new HashMap<>();
        customData.put("count", 5);
        customData.put("name", "value");

        final CustomRequirement requirement = new CustomRequirement(
                CustomRequirement.CustomType.SCRIPT,
                "return true;",
                null,
                null,
                customData,
                null,
                true
        );

        assertEquals(5, requirement.<Integer>getCustomDataValue("count", 0));
        assertEquals("value", requirement.<String>getCustomDataValue("name", "default"));
        assertEquals(42, requirement.<Integer>getCustomDataValue("missing", 42));
        assertEquals(7, requirement.<Integer>getCustomDataValue("name", 7));

        assertTrue(requirement.isScriptBased());
        assertFalse(requirement.isPluginBased());
        assertFalse(requirement.isDataBased());

        requirement.validate();

        setField(requirement, "customScript", "");
        assertThrows(IllegalStateException.class, requirement::validate);

        setField(requirement, "customScript", "return true;");
        setField(requirement, "customData", null);
        assertThrows(IllegalStateException.class, requirement::validate);
    }

    @Test
    void itCreatesNewScriptEnginesWhenCachingDisabled() {
        final CustomRequirement nonCached = new CustomRequirement(
                CustomRequirement.CustomType.SCRIPT,
                "return true;",
                "if (typeof invocationCount === 'undefined') { invocationCount = 0; } invocationCount++; return invocationCount;",
                null,
                new HashMap<>(),
                null,
                false
        );

        final double first = nonCached.calculateProgress(this.player);
        final double second = nonCached.calculateProgress(this.player);

        assertEquals(1.0D, first, 1.0E-6);
        assertEquals(1.0D, second, 1.0E-6);

        final CustomRequirement cached = new CustomRequirement(
                CustomRequirement.CustomType.SCRIPT,
                "return true;",
                "if (typeof invocationCount === 'undefined') { invocationCount = 0; } invocationCount++; return invocationCount;",
                null,
                new HashMap<>(),
                null,
                true
        );

        final double cachedFirst = cached.calculateProgress(this.player);
        final double cachedSecond = cached.calculateProgress(this.player);

        assertEquals(1.0D, cachedFirst, 1.0E-6);
        assertEquals(2.0D, cachedSecond, 1.0E-6);
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
