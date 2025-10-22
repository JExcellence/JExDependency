package com.raindropcentral.rdq.config.bounty;

import com.raindropcentral.rdq.type.EBountyClaimMode;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BountySectionTest {

    @Test
    void itDefaultsToLastHitWhenClaimModeUnset() {
        final BountySection section = new BountySection(new EvaluationEnvironmentBuilder());

        assertEquals(EBountyClaimMode.LAST_HIT, section.getClaimMode(),
            "getClaimMode should default to LAST_HIT when no configuration is provided");
    }

    @Test
    void itParsesConfiguredEnumNameAndRejectsInvalidValues() throws Exception {
        final BountySection section = new BountySection(new EvaluationEnvironmentBuilder());

        setField(section, "claimMode", EBountyClaimMode.MOST_DAMAGE.name());

        assertEquals(EBountyClaimMode.MOST_DAMAGE, section.getClaimMode(),
            "getClaimMode should return the configured enum constant when valid");

        setField(section, "claimMode", "NOT_A_MODE");

        assertThrows(IllegalArgumentException.class, section::getClaimMode,
            "getClaimMode should throw when the configured value is not a valid enum constant");
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
