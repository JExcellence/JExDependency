package com.raindropcentral.rdq.manager.perk;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerkManagerContractTest {

    @Test
    void perkManagerExposesUnifiedLifecycleContract() {
        assertTrue(PerkManager.class.isInterface(), "PerkManager should stay an interface to preserve existing API contracts");
        assertTrue(Modifier.isPublic(PerkManager.class.getModifiers()), "PerkManager should remain publicly accessible");
        final Set<String> expectedMethods = Set.of(
                "getPerkRegistry",
                "getPerkStateService",
                "getPerkTriggerService",
                "initialize",
                "shutdown",
                "getCooldownService",
                "clearPlayerState"
        );
        final Set<String> actualMethods = Arrays.stream(PerkManager.class.getDeclaredMethods())
                .map(method -> method.getName())
                .collect(Collectors.toSet());
        assertEquals(expectedMethods, actualMethods, "PerkManager methods changed unexpectedly; update contract test accordingly");
    }
}
