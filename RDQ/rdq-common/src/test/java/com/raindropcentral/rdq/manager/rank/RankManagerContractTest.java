package com.raindropcentral.rdq.manager.rank;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankManagerContractTest {

    @Test
    void rankManagerRemainsPublicInterfaceWithoutDeclaredMethods() {
        final Class<RankManager> contract = RankManager.class;

        assertTrue(contract.isInterface(), "RankManager must remain an interface");
        assertTrue(Modifier.isPublic(contract.getModifiers()), "RankManager must remain public");
        assertEquals(0, contract.getDeclaredMethods().length, "RankManager must not declare methods");
    }
}
