package com.raindropcentral.rdq.command.player.rq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ERQPermissionTest {

    private static final Map<ERQPermission, ExpectedValues> EXPECTATIONS;

    static {
        final Map<ERQPermission, ExpectedValues> expectations = new EnumMap<>(ERQPermission.class);
        expectations.put(ERQPermission.COMMAND, new ExpectedValues("command", "raindropquests.command"));
        expectations.put(ERQPermission.ADMIN, new ExpectedValues("commandAdmin", "raindropquests.command.admin"));
        expectations.put(ERQPermission.BOUNTY, new ExpectedValues("commandBounty", "raindropquests.command.bounty"));
        expectations.put(ERQPermission.MAIN, new ExpectedValues("commandMain", "raindropquests.command.main"));
        expectations.put(ERQPermission.QUESTS, new ExpectedValues("commandQuests", "raindropquests.command.quests"));
        expectations.put(ERQPermission.RANKS, new ExpectedValues("commandRanks", "raindropquests.command.ranks"));
        expectations.put(ERQPermission.PERKS, new ExpectedValues("commandPerks", "raindropquests.command.perks"));
        EXPECTATIONS = Map.copyOf(expectations);
    }

    @ParameterizedTest
    @EnumSource(ERQPermission.class)
    void permissionAccessorsExposeEnumLiteralValues(final ERQPermission permission) {
        final ExpectedValues expected = EXPECTATIONS.get(permission);

        assertNotNull(expected, () -> "Missing expectation for " + permission.name());
        assertEquals(expected.internalName, permission.getInternalName());
        assertEquals(expected.fallbackNode, permission.getFallbackNode());
    }

    @Test
    void internalNamesAreUnique() {
        final Set<String> internalNames = Arrays.stream(ERQPermission.values())
                .map(ERQPermission::getInternalName)
                .collect(Collectors.toSet());

        assertEquals(ERQPermission.values().length, internalNames.size());
    }

    private static final class ExpectedValues {

        private final String internalName;
        private final String fallbackNode;

        private ExpectedValues(final String internalName, final String fallbackNode) {
            this.internalName = internalName;
            this.fallbackNode = fallbackNode;
        }
    }
}
