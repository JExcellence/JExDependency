package com.raindropcentral.rdq.command.player.rq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class EPRQActionTest {

    private static Stream<Arguments> actionNameSource() {
        return Stream.of(
                Arguments.of(EPRQAction.ADMIN, "ADMIN"),
                Arguments.of(EPRQAction.BOUNTY, "BOUNTY"),
                Arguments.of(EPRQAction.MAIN, "MAIN"),
                Arguments.of(EPRQAction.QUESTS, "QUESTS"),
                Arguments.of(EPRQAction.RANKS, "RANKS"),
                Arguments.of(EPRQAction.PERKS, "PERKS"),
                Arguments.of(EPRQAction.HELP, "HELP")
        );
    }

    @ParameterizedTest
    @MethodSource("actionNameSource")
    void valueOfReturnsSameInstance(final EPRQAction expected, final String name) {
        assertSame(expected, Enum.valueOf(EPRQAction.class, name));
    }

    @Test
    void valuesMirrorTabCompletionOptions() {
        final List<String> options = Arrays.stream(EPRQAction.values())
                .map(Enum::name)
                .map(value -> value.toLowerCase(Locale.ENGLISH))
                .toList();

        final List<String> expected = List.of(
                "admin",
                "bounty",
                "main",
                "quests",
                "ranks",
                "perks",
                "help"
        );

        assertEquals(expected, options);
    }
}
