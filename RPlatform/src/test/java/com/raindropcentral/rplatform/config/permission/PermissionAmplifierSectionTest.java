package com.raindropcentral.rplatform.config.permission;

import com.raindropcentral.rplatform.testutil.ReflectionTestUtils;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests amplifier resolution and bounds handling in {@link PermissionAmplifierSection}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class PermissionAmplifierSectionTest {

    @Test
    void resolvesHighestConfiguredAmplifierThenAppliesBounds() {
        final PermissionAmplifierSection section = new PermissionAmplifierSection(new EvaluationEnvironmentBuilder());
        ReflectionTestUtils.setField(section, "defaultAmplifier", 1);
        ReflectionTestUtils.setField(
            section,
            "permissionAmplifiers",
            Map.of(
                "perm.basic", 2,
                "perm.vip", 5
            )
        );
        ReflectionTestUtils.setField(section, "minAmplifier", 1);
        ReflectionTestUtils.setField(section, "maxAmplifier", 4);

        assertEquals(4, section.getEffectiveAmplifier(Set.of("perm.basic", "perm.vip")));
        assertEquals(2, section.getAmplifierForPermission("perm.basic"));
    }

    @Test
    void clampAndBoundsChecksBehaveAsExpected() {
        final PermissionAmplifierSection section = new PermissionAmplifierSection(new EvaluationEnvironmentBuilder());
        ReflectionTestUtils.setField(section, "defaultAmplifier", 1);
        ReflectionTestUtils.setField(section, "minAmplifier", 2);
        ReflectionTestUtils.setField(section, "maxAmplifier", 6);

        assertEquals(2, section.clampAmplifier(1));
        assertEquals(6, section.clampAmplifier(9));
        assertEquals(1, section.clampAmplifier(null));

        assertTrue(section.isAmplifierWithinBounds(4));
        assertFalse(section.isAmplifierWithinBounds(1));
        assertFalse(section.isAmplifierWithinBounds(8));
    }

    @Test
    void validateRejectsInvertedBounds() {
        final PermissionAmplifierSection section = new PermissionAmplifierSection(new EvaluationEnvironmentBuilder());
        ReflectionTestUtils.setField(section, "minAmplifier", 5);
        ReflectionTestUtils.setField(section, "maxAmplifier", 2);

        assertThrows(IllegalStateException.class, section::validate);
    }
}
