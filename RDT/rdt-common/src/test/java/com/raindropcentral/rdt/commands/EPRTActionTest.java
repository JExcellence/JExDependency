package com.raindropcentral.rdt.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link EPRTAction} command action definitions.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class EPRTActionTest {

    @Test
    void exposesExpectedActionsInStableOrder() {
        assertArrayEquals(
            new EPRTAction[]{
                EPRTAction.CREATE,
                EPRTAction.DELETE,
                EPRTAction.INFO,
                EPRTAction.INVITE,
                EPRTAction.JOIN,
                EPRTAction.ACCEPT,
                EPRTAction.CLAIM,
                EPRTAction.UNCLAIM,
                EPRTAction.DEBUG,
                EPRTAction.DEPOSIT,
                EPRTAction.WITHDRAW,
                EPRTAction.MAIN,
                EPRTAction.TOWN,
                EPRTAction.SPAWN,
                EPRTAction.HELP
            },
            EPRTAction.values()
        );
    }

    @Test
    void resolvesEnumByCanonicalName() {
        assertEquals(EPRTAction.SPAWN, EPRTAction.valueOf("SPAWN"));
    }
}
