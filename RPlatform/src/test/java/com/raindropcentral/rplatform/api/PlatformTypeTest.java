package com.raindropcentral.rplatform.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests enum contracts for {@link PlatformType}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class PlatformTypeTest {

    @Test
    void exposesExpectedValuesInStableOrder() {
        assertArrayEquals(
            new PlatformType[]{
                PlatformType.FOLIA,
                PlatformType.PAPER,
                PlatformType.SPIGOT
            },
            PlatformType.values()
        );
    }

    @Test
    void resolvesEnumConstantByName() {
        assertEquals(PlatformType.PAPER, PlatformType.valueOf("PAPER"));
    }
}
