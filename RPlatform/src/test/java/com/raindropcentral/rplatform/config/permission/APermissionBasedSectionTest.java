package com.raindropcentral.rplatform.config.permission;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class APermissionBasedSectionTest {

    private TestPermissionSection section;

    @BeforeEach
    void setUp() {
        section = new TestPermissionSection(Mockito.mock(EvaluationEnvironmentBuilder.class));
    }

    @Test
    void defaultsAreUsedWhenFlagsUnset() {
        assertTrue(section.getEnabled());
        assertTrue(section.getUseBestValue());

        section.setDefaultUseBestValue(false);
        assertFalse(section.getUseBestValue());
    }

    @Test
    void explicitFlagsOverrideDefaults() {
        setInternalFlag("enabled", Boolean.FALSE);
        setInternalFlag("useBestValue", Boolean.FALSE);

        assertFalse(section.getEnabled());
        assertFalse(section.getUseBestValue());
    }

    @Test
    void effectiveValueFallsBackToDefaultWhenPlayerMissingOrDisabled() {
        section.setDefaultValue(7);
        assertEquals(7, section.getEffectiveValue((Player) null));

        section.putPermissionValue("perm.test", 12);
        setInternalFlag("enabled", Boolean.FALSE);

        assertEquals(7, section.getEffectiveValue(Set.of("perm.test")));
    }

    @Test
    void effectiveValueSelectsBestPermissionAndAppliesBounds() {
        section.setDefaultValue(3);
        section.setBounds(0, 10);
        section.putPermissionValue("perm.alpha", 5);
        section.putPermissionValue("perm.beta", 15);

        Player player = mock(Player.class);
        Set<PermissionAttachmentInfo> attachments = new LinkedHashSet<>();
        attachments.add(attachment("perm.alpha", true));
        attachments.add(attachment("perm.beta", true));
        when(player.getEffectivePermissions()).thenReturn(attachments);

        assertEquals(10, section.getEffectiveValue(player));
        assertEquals("perm.beta", section.getEffectivePermission(player));
        assertEquals(1, section.getApplyBoundsInvocations());
    }

    @Test
    void effectiveValueUsesFirstMatchWhenBestValueDisabled() {
        section.setDefaultValue(2);
        section.putPermissionValue("perm.alpha", 4);
        section.putPermissionValue("perm.beta", 8);
        setInternalFlag("useBestValue", Boolean.FALSE);

        Player player = mock(Player.class);
        Set<PermissionAttachmentInfo> attachments = new LinkedHashSet<>();
        attachments.add(attachment("perm.beta", true));
        attachments.add(attachment("perm.alpha", true));
        when(player.getEffectivePermissions()).thenReturn(attachments);

        assertEquals(8, section.getEffectiveValue(player));
        assertEquals("perm.beta", section.getEffectivePermission(player));
    }

    @Test
    void directPermissionSetDelegatesToBoundsAndBestValueLogic() {
        section.setDefaultValue(1);
        section.setBounds(0, 5);
        section.putPermissionValue("perm.alpha", 2);
        section.putPermissionValue("perm.beta", 9);

        Set<String> permissions = new LinkedHashSet<>();
        permissions.add("perm.alpha");
        permissions.add("perm.beta");

        assertEquals(5, section.getEffectiveValue(permissions));
        assertEquals(1, section.getApplyBoundsInvocations());
    }

    @Test
    void permissionExtractionHonoursWildcardsAndIgnoresDisabledAttachments() {
        section.putPermissionValue("perm.exact", 3);
        section.putPermissionValue("perm.feature.*", 5);
        section.putPermissionValue("perm.feature.special", 7);

        Player player = mock(Player.class);
        Set<PermissionAttachmentInfo> attachments = new LinkedHashSet<>();
        attachments.add(attachment("perm.exact", true));
        attachments.add(attachment("perm.feature.extra", true));
        attachments.add(attachment("perm.feature.special", false));
        attachments.add(attachment("other.permission", true));
        when(player.getEffectivePermissions()).thenReturn(attachments);

        Set<String> extracted = section.extractPlayerPermissions(player);
        assertTrue(extracted.contains("perm.exact"));
        assertTrue(extracted.contains("perm.feature.*"));
        assertFalse(extracted.contains("perm.feature.special"));
        assertEquals(2, extracted.size());
    }

    @Test
    void matchesWildcardHandlesNullsAndSuffixes() {
        assertFalse(section.matchesWildcard(null, "perm.test"));
        assertFalse(section.matchesWildcard("perm.test", null));
        assertTrue(section.matchesWildcard("perm.test", "perm.test"));
        assertTrue(section.matchesWildcard("perm.feature.alpha", "perm.feature.*"));
        assertTrue(section.matchesWildcard("perm.feature.*", "perm.feature.beta"));
        assertFalse(section.matchesWildcard("perm.alpha", "perm.beta"));
    }

    @Test
    void helperMethodsReflectConfiguredPermissions() {
        assertFalse(section.hasPermissionValues());
        section.putPermissionValue("perm.alpha", 4);
        assertTrue(section.hasPermissionValues());
        assertEquals(4, section.getValueForPermission("perm.alpha"));
        assertNull(section.getValueForPermission("unknown"));
        assertNull(section.getValueForPermission(" "));
    }

    @Test
    void hasRelevantPermissionsRequiresConfiguredMatch() {
        section.putPermissionValue("perm.alpha", 5);
        Player player = mock(Player.class);
        when(player.getEffectivePermissions()).thenReturn(Set.of(attachment("perm.beta", true)));
        assertFalse(section.hasRelevantPermissions(player));

        when(player.getEffectivePermissions()).thenReturn(Set.of(attachment("perm.alpha", true)));
        assertTrue(section.hasRelevantPermissions(player));
    }

    @Test
    void effectivePermissionReturnsNullWhenNoOverridesApply() {
        section.setDefaultValue(3);
        Player player = mock(Player.class);
        when(player.getEffectivePermissions()).thenReturn(Set.of(attachment("perm.alpha", true)));
        assertNull(section.getEffectivePermission(player));
    }

    @Test
    void validateRejectsInvalidConfiguration() {
        section.setDefaultValue(3);
        section.putPermissionValue("perm.alpha", 4);
        section.validate();
        assertTrue(section.wasAdditionalValidationCalled());

        section.setDefaultValue(-1);
        assertThrows(IllegalStateException.class, section::validate);

        section.setDefaultValue(3);
        section.putPermissionValue(" ", 4);
        assertThrows(IllegalStateException.class, section::validate);

        section.clearPermissionValues();
        section.putPermissionValue("perm.alpha", 12);
        assertThrows(IllegalStateException.class, section::validate);

        section.clearPermissionValues();
        section.putPermissionValue("perm.alpha", 4);
        section.failAdditionalValidation();
        assertThrows(IllegalStateException.class, section::validate);
    }

    private PermissionAttachmentInfo attachment(final String permission, final boolean value) {
        PermissionAttachmentInfo info = mock(PermissionAttachmentInfo.class);
        when(info.getPermission()).thenReturn(permission);
        when(info.getValue()).thenReturn(value);
        return info;
    }

    private void setInternalFlag(final String fieldName, final Boolean value) {
        try {
            Field field = APermissionBasedSection.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(section, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to set field " + fieldName, exception);
        }
    }

    private static final class TestPermissionSection extends APermissionBasedSection<Integer> {

        private final Map<String, Integer> permissionValues = new LinkedHashMap<>();
        private int min = 0;
        private int max = 10;
        private boolean defaultUseBestValue = true;
        private Integer defaultValue = 0;
        private boolean additionalValidationCalled;
        private boolean failAdditionalValidation;
        private int applyBoundsInvocations;

        private TestPermissionSection(final EvaluationEnvironmentBuilder builder) {
            super(builder);
        }

        void setDefaultValue(final Integer defaultValue) {
            this.defaultValue = defaultValue;
        }

        void putPermissionValue(final String permission, final Integer value) {
            this.permissionValues.put(permission, value);
        }

        void clearPermissionValues() {
            this.permissionValues.clear();
        }

        void setBounds(final int min, final int max) {
            this.min = min;
            this.max = max;
        }

        void setDefaultUseBestValue(final boolean defaultUseBestValue) {
            this.defaultUseBestValue = defaultUseBestValue;
        }

        void failAdditionalValidation() {
            this.failAdditionalValidation = true;
        }

        int getApplyBoundsInvocations() {
            return this.applyBoundsInvocations;
        }

        boolean wasAdditionalValidationCalled() {
            return this.additionalValidationCalled;
        }

        @Override
        protected Integer getDefaultValue() {
            return this.defaultValue;
        }

        @Override
        protected Map<String, Integer> getPermissionValues() {
            return this.permissionValues;
        }

        @Override
        protected boolean getDefaultUseBestValue() {
            return this.defaultUseBestValue;
        }

        @Override
        protected Integer chooseBestValue(final Integer current, final Integer candidate) {
            return Math.max(current, candidate);
        }

        @Override
        protected boolean isBetterValue(final Integer candidate, final Integer current) {
            return candidate > current;
        }

        @Override
        protected Integer applyBounds(final Integer value) {
            this.applyBoundsInvocations++;
            if (value == null) {
                return null;
            }

            if (value < this.min) {
                return this.min;
            }

            if (value > this.max) {
                return this.max;
            }

            return value;
        }

        @Override
        protected boolean isValidValue(final Integer value) {
            return value != null && value >= this.min && value <= this.max;
        }

        @Override
        protected void performAdditionalValidation() {
            this.additionalValidationCalled = true;
            if (this.failAdditionalValidation) {
                throw new IllegalStateException("additional validation failed");
            }
        }
    }
}
