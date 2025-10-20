package com.raindropcentral.rplatform.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlatformTypeTest {

    @Test
    void valuesExposeDetectionPriorityOrdering() {
        PlatformType[] expectedOrder = {
            PlatformType.FOLIA,
            PlatformType.PAPER,
            PlatformType.SPIGOT
        };

        assertArrayEquals(expectedOrder, PlatformType.values(),
            "The enumeration order documents detection priority; update PlatformAPIFactory tests if this changes.");

        assertEquals(0, PlatformType.FOLIA.ordinal(), "FOLIA detection should run first.");
        assertEquals(1, PlatformType.PAPER.ordinal(), "PAPER detection should run after FOLIA.");
        assertEquals(2, PlatformType.SPIGOT.ordinal(), "SPIGOT detection should run last as the fallback platform.");
    }

    @Test
    void valueOfResolvesAllKnownConstants() {
        assertSame(PlatformType.FOLIA, PlatformType.valueOf("FOLIA"));
        assertSame(PlatformType.PAPER, PlatformType.valueOf("PAPER"));
        assertSame(PlatformType.SPIGOT, PlatformType.valueOf("SPIGOT"));
    }

    @Test
    void valueOfRejectsUnknownConstants() {
        assertThrows(IllegalArgumentException.class, () -> PlatformType.valueOf("UNKNOWN"));
    }
}
