package com.raindropcentral.rdq.manager.perk;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerkManagerContractTest {

    /**
     * The PerkManager type currently acts as a marker interface. This contract test documents
     * that expectation so that adding new API surface requires an explicit update to the test.
     */
    @Test
    void perkManagerRemainsPublicMarkerInterface() {
        assertTrue(PerkManager.class.isInterface(), "PerkManager should stay an interface to preserve existing API contracts");
        assertTrue(Modifier.isPublic(PerkManager.class.getModifiers()), "PerkManager should remain publicly accessible");
        assertEquals(0, PerkManager.class.getDeclaredMethods().length, "PerkManager is expected to remain a marker interface; update this test deliberately when adding methods");
    }
}
