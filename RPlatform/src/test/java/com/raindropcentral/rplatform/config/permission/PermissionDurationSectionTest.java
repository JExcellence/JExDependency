package com.raindropcentral.rplatform.config.permission;

import com.raindropcentral.rplatform.config.DurationSection;
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
import static org.mockito.Mockito.*;

class PermissionDurationSectionTest {

    private PermissionDurationSection section;

    @BeforeEach
    void setUp() {
        section = new PermissionDurationSection(mock(EvaluationEnvironmentBuilder.class));
    }

    @Test
    void accessorsExposeConfiguredDurationsAndSections() {
        DurationSection defaultNode = mockDurationNode(600L, true, "10 minutes");
        DurationSection minNode = mockDurationNode(120L, true, "2 minutes");
        DurationSection maxNode = mockDurationNode(3600L, true, "1 hour");
        DurationSection standardNode = mockDurationNode(300L, true, "5 minutes");
        DurationSection eliteNode = mockDurationNode(1800L, true, "30 minutes");

        Map<String, DurationSection> overrides = new LinkedHashMap<>();
        overrides.put("rdc.perk.standard", standardNode);
        overrides.put("rdc.perk.elite", eliteNode);
        overrides.put("rdc.perk.null", null);

        setField(section, "defaultDuration", defaultNode);
        setField(section, "permissionDurations", overrides);
        setField(section, "minDuration", minNode);
        setField(section, "maxDuration", maxNode);

        assertEquals(600L, section.getDefaultDurationSeconds());
        assertSame(defaultNode, section.getDefaultDuration());

        Map<String, Long> seconds = section.getPermissionDurationsSeconds();
        assertEquals(2, seconds.size());
        assertEquals(300L, seconds.get("rdc.perk.standard"));
        assertEquals(1800L, seconds.get("rdc.perk.elite"));

        seconds.put("rdc.perk.new", 45L);
        assertFalse(section.getPermissionDurationsSeconds().containsKey("rdc.perk.new"));

        Map<String, DurationSection> durationSections = section.getPermissionDurations();
        assertEquals(3, durationSections.size());
        durationSections.put("rdc.perk.extra", mockDurationNode(45L, true, "45 seconds"));
        assertNull(section.getDurationSectionForPermission("rdc.perk.extra"));

        assertEquals(3600L, section.getMaxDurationSeconds());
        assertSame(maxNode, section.getMaxDuration());
        assertEquals(120L, section.getMinDurationSeconds());
        assertSame(minNode, section.getMinDuration());

        assertEquals(300L, section.getDurationForPermission("rdc.perk.standard"));
        assertNull(section.getDurationForPermission("rdc.perk.null"));
        assertNull(section.getDurationForPermission(" "));
        assertSame(eliteNode, section.getDurationSectionForPermission("rdc.perk.elite"));
        assertNull(section.getDurationSectionForPermission(null));
        assertTrue(section.hasPermissionDurations());

        PermissionDurationSection empty = new PermissionDurationSection(mock(EvaluationEnvironmentBuilder.class));
        assertEquals(0L, empty.getDefaultDurationSeconds());
        assertNotNull(empty.getDefaultDuration());
        assertTrue(empty.getPermissionDurationsSeconds().isEmpty());
        assertFalse(empty.hasPermissionDurations());
        assertNull(empty.getMaxDurationSeconds());
        assertNull(empty.getMinDurationSeconds());
    }

    @Test
    void effectiveDurationAndFormattingRespectsOverrides() {
        DurationSection defaultNode = mockDurationNode(600L, true, "10 minutes");
        DurationSection minNode = mockDurationNode(120L, true, "2 minutes");
        DurationSection maxNode = mockDurationNode(1200L, true, "20 minutes");
        DurationSection standardNode = mockDurationNode(300L, true, "5 minutes");
        DurationSection eliteNode = mockDurationNode(1800L, true, "30 minutes");

        Map<String, DurationSection> overrides = new LinkedHashMap<>();
        overrides.put("rdc.perk.elite", eliteNode);
        overrides.put("rdc.perk.standard", standardNode);

        setField(section, "defaultDuration", defaultNode);
        setField(section, "permissionDurations", overrides);
        setField(section, "minDuration", minNode);
        setField(section, "maxDuration", maxNode);

        Player player = mock(Player.class);
        Set<PermissionAttachmentInfo> attachments = new LinkedHashSet<>();
        attachments.add(attachment("rdc.perk.elite", true));
        attachments.add(attachment("rdc.perk.standard", true));
        attachments.add(attachment("rdc.perk.disabled", false));
        when(player.getEffectivePermissions()).thenReturn(attachments);

        assertEquals(1200L, section.getEffectiveDuration(player));
        assertEquals(300L, section.getEffectiveDuration(Set.of("rdc.perk.standard")));
        assertEquals(600L, section.getEffectiveDuration(Set.of("rdc.perk.unknown")));
        assertEquals(600L, section.getEffectiveDuration((Player) null));

        assertEquals("30 minutes", section.getFormattedEffectiveDuration(player));
        assertEquals("5 minutes", section.getFormattedEffectiveDuration(Set.of("rdc.perk.standard")));
        assertEquals("10 minutes", section.getFormattedEffectiveDuration((Player) null));

        DurationSection numericOverride = mockDurationNode(3600L, false, "ignored");
        Map<String, DurationSection> extendedOverrides = new LinkedHashMap<>(overrides);
        extendedOverrides.put("rdc.perk.numeric", numericOverride);
        setField(section, "permissionDurations", extendedOverrides);

        assertEquals(1200L, section.getEffectiveDuration(Set.of("rdc.perk.numeric")));
        assertEquals("10 minutes", section.getFormattedEffectiveDuration(Set.of("rdc.perk.numeric")));

        PermissionDurationSection fallbackSection = new PermissionDurationSection(mock(EvaluationEnvironmentBuilder.class));
        DurationSection zeroDefault = mockDurationNode(0L, false, "ignored");
        DurationSection rawNode = mockDurationNode(3600L, false, "ignored");
        DurationSection zeroNode = mockDurationNode(0L, true, "0 seconds");
        Map<String, DurationSection> fallbackOverrides = new LinkedHashMap<>();
        fallbackOverrides.put("rdc.perk.numeric", rawNode);
        fallbackOverrides.put("rdc.perk.zero", zeroNode);

        setField(fallbackSection, "defaultDuration", zeroDefault);
        setField(fallbackSection, "permissionDurations", fallbackOverrides);

        assertEquals(3600L, fallbackSection.getEffectiveDuration(Set.of("rdc.perk.numeric")));
        assertEquals("1 hour", fallbackSection.getFormattedEffectiveDuration(Set.of("rdc.perk.numeric")));
        assertEquals("Permanent", fallbackSection.getFormattedEffectiveDuration(Set.of("rdc.perk.zero")));
        assertEquals("Permanent", fallbackSection.getFormattedEffectiveDuration((Player) null));
    }

    @Test
    void boundsChecksAndClampingHandleEdgeCases() {
        DurationSection defaultNode = mockDurationNode(90L, true, "1 minute 30 seconds");
        DurationSection minNode = mockDurationNode(60L, true, "1 minute");
        DurationSection maxNode = mockDurationNode(300L, true, "5 minutes");

        setField(section, "defaultDuration", defaultNode);
        setField(section, "minDuration", minNode);
        setField(section, "maxDuration", maxNode);

        assertFalse(section.hasPermissionDurations());

        assertFalse(section.isDurationWithinBounds(null));
        assertFalse(section.isDurationWithinBounds(30L));
        assertFalse(section.isDurationWithinBounds(600L));
        assertTrue(section.isDurationWithinBounds(120L));

        assertEquals(90L, section.clampDuration(null));
        assertEquals(60L, section.clampDuration(30L));
        assertEquals(300L, section.clampDuration(600L));
        assertEquals(120L, section.clampDuration(120L));
    }

    @Test
    void validationCatchesInvalidConfigurations() {
        DurationSection defaultNode = mockDurationNode(600L, true, "10 minutes");
        DurationSection minNode = mockDurationNode(120L, true, "2 minutes");
        DurationSection maxNode = mockDurationNode(1800L, true, "30 minutes");
        DurationSection overrideNode = mockDurationNode(900L, true, "15 minutes");

        Map<String, DurationSection> overrides = new LinkedHashMap<>();
        overrides.put("rdc.perk.standard", overrideNode);

        setField(section, "defaultDuration", defaultNode);
        setField(section, "permissionDurations", overrides);
        setField(section, "minDuration", minNode);
        setField(section, "maxDuration", maxNode);

        assertDoesNotThrow(section::validate);

        DurationSection negativeMin = mockDurationNode(-5L, true, "invalid");
        setField(section, "minDuration", negativeMin);
        IllegalStateException minException = assertThrows(IllegalStateException.class, section::validate);
        assertTrue(minException.getMessage().contains("Minimum duration"));

        DurationSection negativeMax = mockDurationNode(-10L, true, "invalid");
        setField(section, "minDuration", minNode);
        setField(section, "maxDuration", negativeMax);
        IllegalStateException maxException = assertThrows(IllegalStateException.class, section::validate);
        assertTrue(maxException.getMessage().contains("Maximum duration"));

        DurationSection largeMin = mockDurationNode(1200L, true, "20 minutes");
        DurationSection smallMax = mockDurationNode(600L, true, "10 minutes");
        setField(section, "minDuration", largeMin);
        setField(section, "maxDuration", smallMax);
        IllegalStateException invertedBounds = assertThrows(IllegalStateException.class, section::validate);
        assertTrue(invertedBounds.getMessage().contains("Minimum duration"));

        DurationSection invalidOverride = mock(DurationSection.class);
        when(invalidOverride.getSeconds()).thenReturn(300L);
        when(invalidOverride.hasDuration()).thenReturn(true);
        doThrow(new IllegalStateException("Invalid format")).when(invalidOverride).validate();

        Map<String, DurationSection> invalidOverrides = new LinkedHashMap<>();
        invalidOverrides.put("rdc.perk.invalid", invalidOverride);
        setField(section, "minDuration", minNode);
        setField(section, "maxDuration", maxNode);
        setField(section, "permissionDurations", invalidOverrides);

        IllegalStateException invalidSection = assertThrows(IllegalStateException.class, section::validate);
        assertTrue(invalidSection.getMessage().contains("Invalid format"));

        DurationSection negativeDefault = mockDurationNode(600L, true, "10 minutes");
        doThrow(new IllegalStateException("Negative default")).when(negativeDefault).validate();
        setField(section, "defaultDuration", negativeDefault);
        IllegalStateException defaultException = assertThrows(IllegalStateException.class, section::validate);
        assertTrue(defaultException.getMessage().contains("Negative default"));
    }

    private DurationSection mockDurationNode(final long seconds, final boolean hasDuration, final String formatted) {
        DurationSection node = mock(DurationSection.class);
        when(node.getSeconds()).thenReturn(seconds);
        when(node.hasDuration()).thenReturn(hasDuration);
        when(node.getFormattedDuration()).thenReturn(formatted);
        doNothing().when(node).validate();
        return node;
    }

    private PermissionAttachmentInfo attachment(final String permission, final boolean value) {
        PermissionAttachmentInfo info = mock(PermissionAttachmentInfo.class);
        when(info.getPermission()).thenReturn(permission);
        when(info.getValue()).thenReturn(value);
        return info;
    }

    private void setField(
        final PermissionDurationSection target,
        final String fieldName,
        final Object value
    ) {
        try {
            Field field = PermissionDurationSection.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to set field " + fieldName, exception);
        }
    }
}
