package com.raindropcentral.rdq.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class EBountyClaimModeTest {

    @Test
    void valuesShouldExposeAllClaimModes() {
        EBountyClaimMode[] expected = {
            EBountyClaimMode.MOST_DAMAGE,
            EBountyClaimMode.LAST_HIT
        };

        assertArrayEquals(expected, EBountyClaimMode.values(), "Enum values should remain stable");
        Arrays.stream(expected).forEach(mode -> assertSame(mode, EBountyClaimMode.valueOf(mode.name())));
    }

    @Test
    void helperMethodsShouldBeDocumentedWhenIntroduced() {
        long helperMethodCount = Arrays.stream(EBountyClaimMode.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> !("values".equals(method.getName()) && method.getParameterCount() == 0))
            .filter(method -> !("valueOf".equals(method.getName()) && method.getParameterCount() == 1))
            .count();

        assertEquals(0L, helperMethodCount, "EBountyClaimMode currently exposes no helper methods. If helper methods are added," +
            " document their metadata and update this test to reflect the additional API.");
    }
}
