package com.raindropcentral.rdr.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link EPRRAction} command action definitions.
 */
class EPRRActionTest {

    @Test
    void exposesExpectedActionsInStableOrder() {
        assertArrayEquals(
            new EPRRAction[]{
                EPRRAction.ADMIN,
                EPRRAction.INFO,
                EPRRAction.SCOREBOARD,
                EPRRAction.STORAGE,
                EPRRAction.TRADE,
                EPRRAction.TAXES
            },
            EPRRAction.values()
        );
    }

    @Test
    void resolvesEnumValueByCanonicalName() {
        assertEquals(EPRRAction.STORAGE, EPRRAction.valueOf("STORAGE"));
    }
}
