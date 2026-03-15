package com.raindropcentral.rplatform.utility.map;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests map merge builder behavior in {@link Maps}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class MapsTest {

    @Test
    void mergeBuilderAppliesMutationsAndConditions() {
        final Map<String, Integer> result = Maps.<String, Integer>merge(Map.of("a", 1, "b", 2))
            .with("c", 3)
            .remove("b")
            .onlyIf(false, "d", 4)
            .onlyIf(true, Map.of("e", 5))
            .immutable();

        assertEquals(Map.of("a", 1, "c", 3, "e", 5), result);
        assertThrows(UnsupportedOperationException.class, () -> result.put("x", 9));
    }

    @Test
    void immutableSnapshotDoesNotTrackFutureBuilderChanges() {
        final Maps.Builder<String, Integer> builder = Maps.<String, Integer>merge(Map.of("base", 1));
        final Map<String, Integer> immutableSnapshot = builder.immutable();

        builder.with("later", 2);

        assertFalse(immutableSnapshot.containsKey("later"));
    }

    @Test
    void mergeObjectRejectsNonMapInputs() {
        assertThrows(
            ClassCastException.class,
            () -> Maps.<String, Integer>merge(new Object())
        );
    }
}
