package com.raindropcentral.rdq.manager.quest;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestManagerContractTest {

    @Test
    void questManagerRemainsMarkerInterface() {
        assertTrue(QuestManager.class.isInterface(), "QuestManager must remain an interface");
        assertTrue(Modifier.isPublic(QuestManager.class.getModifiers()), "QuestManager must remain public");
        assertEquals(0, QuestManager.class.getDeclaredMethods().length, "QuestManager must declare no methods");
    }
}
