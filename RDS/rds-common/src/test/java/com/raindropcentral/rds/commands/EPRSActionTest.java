package com.raindropcentral.rds.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link EPRSAction} command action definitions.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class EPRSActionTest {

    @Test
    void exposesExpectedActionsInStableOrder() {
        assertArrayEquals(
            new EPRSAction[]{
                EPRSAction.ADMIN,
                EPRSAction.BAR,
                EPRSAction.GIVE,
                EPRSAction.INFO,
                EPRSAction.SCOREBOARD,
                EPRSAction.SEARCH,
                EPRSAction.STORE,
                EPRSAction.TAXES
            },
            EPRSAction.values()
        );
    }

    @Test
    void resolvesEnumValueByCanonicalName() {
        assertEquals(EPRSAction.STORE, EPRSAction.valueOf("STORE"));
    }
}
