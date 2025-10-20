package com.raindropcentral.rplatform.config.permission;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionCooldownSectionTest {

    private PermissionCooldownSection section;

    @BeforeEach
    void setUp() {
        section = new PermissionCooldownSection(mock(EvaluationEnvironmentBuilder.class));
    }

    @Test
    void accessorsExposeConfiguredCooldowns() {
        setDefaultCooldownSeconds(90L);
        Map<String, Long> configuredCooldowns = new LinkedHashMap<>();
        configuredCooldowns.put("rdc.cooldown.standard", 45L);
        configuredCooldowns.put("rdc.cooldown.fast", 15L);
        configuredCooldowns.put("rdc.cooldown.instant", 0L);
        setPermissionCooldowns(configuredCooldowns);

        assertEquals(90L, section.getDefaultCooldownSeconds());
        Map<String, Long> cooldowns = section.getPermissionCooldowns();
        assertEquals(3, cooldowns.size());
        assertEquals(15L, section.getCooldownForPermission("rdc.cooldown.fast"));
        assertEquals(0L, section.getCooldownForPermission("rdc.cooldown.instant"));
        assertNull(section.getCooldownForPermission("rdc.cooldown.missing"));
        assertNull(section.getCooldownForPermission(" "));
        assertTrue(section.hasPermissionCooldowns());

        cooldowns.put("rdc.cooldown.new", 5L);
        assertFalse(section.getPermissionCooldowns().containsKey("rdc.cooldown.new"));
    }

    @Test
    void effectiveCooldownResolvesOverridesForPlayersAndPermissionSets() {
        setDefaultCooldownSeconds(120L);
        Map<String, Long> configuredCooldowns = new LinkedHashMap<>();
        configuredCooldowns.put("rdc.cooldown.standard", 45L);
        configuredCooldowns.put("rdc.cooldown.fast", 15L);
        configuredCooldowns.put("rdc.cooldown.instant", 0L);
        setPermissionCooldowns(configuredCooldowns);

        Player player = mock(Player.class);
        Set<PermissionAttachmentInfo> attachments = new LinkedHashSet<>();
        attachments.add(attachment("rdc.cooldown.standard", true));
        attachments.add(attachment("rdc.cooldown.instant", true));
        attachments.add(attachment("rdc.cooldown.revoked", false));
        when(player.getEffectivePermissions()).thenReturn(attachments);

        assertEquals(0L, section.getEffectiveCooldown(player));

        Set<String> permissionSet = Set.of("rdc.cooldown.fast");
        assertEquals(15L, section.getEffectiveCooldown(permissionSet));
        assertEquals(120L, section.getEffectiveCooldown(Set.of("rdc.cooldown.unknown")));
    }

    @Test
    void defaultCooldownFallsBackToZeroWhenUnset() {
        assertEquals(0L, section.getDefaultCooldownSeconds());
        assertFalse(section.hasPermissionCooldowns());
        assertNull(section.getCooldownForPermission("rdc.cooldown.fast"));
        assertEquals(0L, section.getEffectiveCooldown(Set.of("rdc.cooldown.fast")));
    }

    @Test
    void validateRejectsNegativeDurations() {
        setDefaultCooldownSeconds(-10L);
        assertThrows(IllegalStateException.class, section::validate);

        setDefaultCooldownSeconds(30L);
        setPermissionCooldowns(Map.of("rdc.cooldown.invalid", -5L));
        assertThrows(IllegalStateException.class, section::validate);
    }

    private void setDefaultCooldownSeconds(final Long value) {
        setField("defaultCooldownSeconds", value);
    }

    private void setPermissionCooldowns(final Map<String, Long> values) {
        setField("permissionCooldowns", values);
    }

    private void setField(final String fieldName, final Object value) {
        try {
            Field field = PermissionCooldownSection.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(section, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to set field " + fieldName, exception);
        }
    }

    private PermissionAttachmentInfo attachment(final String permission, final boolean value) {
        PermissionAttachmentInfo info = mock(PermissionAttachmentInfo.class);
        when(info.getPermission()).thenReturn(permission);
        when(info.getValue()).thenReturn(value);
        return info;
    }
}
