package com.raindropcentral.rdq.type;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EPerkTypeTest {

    private static Stream<Arguments> perkCharacteristics() {
        return Stream.of(
            Arguments.of(EPerkType.TOGGLEABLE_PASSIVE, false, true, false, "Toggleable passive effect without cooldown"),
            Arguments.of(EPerkType.EVENT_TRIGGERED, true, false, true, "Automatically triggered by events with cooldown"),
            Arguments.of(EPerkType.INSTANT_USE, true, false, false, "Immediate effect with cooldown"),
            Arguments.of(EPerkType.DURATION_BASED, true, false, true, "Temporary effect with duration and cooldown")
        );
    }

    @ParameterizedTest(name = "{0} flags match expected values")
    @MethodSource("perkCharacteristics")
    void flagsMatchExpectations(
        final EPerkType perkType,
        final boolean hasCooldown,
        final boolean isToggleable,
        final boolean isEventBased,
        final String description
    ) {
        assertEquals(hasCooldown, perkType.hasCooldown(), "Cooldown flag mismatch");
        assertEquals(isToggleable, perkType.isToggleable(), "Toggleable flag mismatch");
        assertEquals(isEventBased, perkType.isEventBased(), "Event-based flag mismatch");
        assertEquals(description, perkType.getDescription(), "Description mismatch");
    }
}
