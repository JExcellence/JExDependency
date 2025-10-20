package com.raindropcentral.rplatform.config.permission;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionAmplifierSectionTest {

    private PermissionAmplifierSection section;

    @BeforeEach
    void setUp() {
        section = new PermissionAmplifierSection(new EvaluationEnvironmentBuilder());
    }

    @Test
    void defaultAmplifierAndEmptyConfigurationUseFallbacks() {
        assertEquals(0, section.getDefaultAmplifier());
        assertFalse(section.hasPermissionAmplifiers());
        assertNull(section.getAmplifierForPermission("perm.test"));
        assertEquals(0, section.getEffectiveAmplifier((Player) null));
        assertEquals(0, section.getEffectiveAmplifier(Set.<String>of()));
    }

    @Test
    void configuredAmplifiersResolveStrongestValueAndClamp() {
        setField("defaultAmplifier", 1);
        setField("minAmplifier", 0);
        setField("maxAmplifier", 5);

        Map<String, Integer> configured = new LinkedHashMap<>();
        configured.put("perm.base", 2);
        configured.put("perm.vip", 4);
        configured.put("perm.vip.legend", 7);
        setField("permissionAmplifiers", configured);

        Player player = mock(Player.class);
        Set<PermissionAttachmentInfo> attachments = new LinkedHashSet<>();
        attachments.add(attachment("perm.base", true));
        attachments.add(attachment("perm.vip.legend", true));
        when(player.getEffectivePermissions()).thenReturn(attachments);

        assertTrue(section.hasPermissionAmplifiers());
        assertEquals(configured, section.getPermissionAmplifiers());
        assertEquals(7, section.getAmplifierForPermission("perm.vip.legend"));
        assertEquals(5, section.getEffectiveAmplifier(player));

        Set<String> directPermissions = new LinkedHashSet<>();
        directPermissions.add("perm.base");
        directPermissions.add("perm.vip.legend");
        assertEquals(5, section.getEffectiveAmplifier(directPermissions));

        Map<String, Integer> copy = section.getPermissionAmplifiers();
        copy.put("perm.extra", 99);
        assertNull(section.getAmplifierForPermission("perm.extra"));

        assertEquals(0, section.getMinAmplifier());
        assertEquals(5, section.getMaxAmplifier());
        assertEquals(1, section.clampAmplifier(null));
        assertEquals(0, section.clampAmplifier(-5));
        assertEquals(5, section.clampAmplifier(9));
        assertEquals(3, section.clampAmplifier(3));

        assertTrue(section.isAmplifierWithinBounds(3));
        assertFalse(section.isAmplifierWithinBounds(-1));
        assertFalse(section.isAmplifierWithinBounds(9));
        assertFalse(section.isAmplifierWithinBounds(null));
    }

    @Test
    void missingPermissionsFallBackToDefaultAmplifier() {
        setField("defaultAmplifier", 2);

        Map<String, Integer> configured = new LinkedHashMap<>();
        configured.put("perm.valid", 4);
        setField("permissionAmplifiers", configured);

        Player player = mock(Player.class);
        Set<PermissionAttachmentInfo> attachments = new LinkedHashSet<>();
        attachments.add(attachment("perm.invalid", true));
        when(player.getEffectivePermissions()).thenReturn(attachments);

        assertEquals(2, section.getEffectiveAmplifier(player));
        assertEquals(2, section.getEffectiveAmplifier(Set.of("perm.unknown")));
        assertNull(section.getAmplifierForPermission(" "));
        assertNull(section.getAmplifierForPermission(null));
    }

    @Test
    void additionalValidationRejectsNegativeOrInvertedBounds() {
        setField("minAmplifier", -1);
        setField("maxAmplifier", 5);
        assertThrows(IllegalStateException.class, this::invokeValidation);

        setField("minAmplifier", null);
        setField("maxAmplifier", -2);
        assertThrows(IllegalStateException.class, this::invokeValidation);

        setField("minAmplifier", 5);
        setField("maxAmplifier", 3);
        assertThrows(IllegalStateException.class, this::invokeValidation);

        setField("minAmplifier", 0);
        setField("maxAmplifier", 5);
        assertDoesNotThrow(this::invokeValidation);
    }

    private PermissionAttachmentInfo attachment(final String permission, final boolean value) {
        PermissionAttachmentInfo info = mock(PermissionAttachmentInfo.class);
        when(info.getPermission()).thenReturn(permission);
        when(info.getValue()).thenReturn(value);
        return info;
    }

    private void setField(final String fieldName, final Object value) {
        try {
            Field field = PermissionAmplifierSection.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(section, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to set field " + fieldName, exception);
        }
    }

    private void invokeValidation() {
        try {
            Method method = PermissionAmplifierSection.class.getDeclaredMethod("performAdditionalValidation");
            method.setAccessible(true);
            method.invoke(section);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to invoke performAdditionalValidation", exception);
        }
    }
}
