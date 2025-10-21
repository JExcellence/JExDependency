package com.raindropcentral.rplatform.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class LogLevelTest {

    @ParameterizedTest
    @MethodSource("levelMappings")
    @DisplayName("toJavaLevel should map enum constants to expected JUL levels")
    void toJavaLevelShouldMatchExpectedLevels(final LogLevel logLevel, final Level expectedLevel) {
        assertSame(expectedLevel, logLevel.toJavaLevel(),
            () -> logLevel.name() + " should map to JUL level " + expectedLevel);
    }

    private static Stream<Arguments> levelMappings() {
        return Stream.of(
            Arguments.of(LogLevel.CRITICAL, Level.SEVERE),
            Arguments.of(LogLevel.ERROR, Level.SEVERE),
            Arguments.of(LogLevel.WARN, Level.WARNING),
            Arguments.of(LogLevel.WARNING, Level.WARNING),
            Arguments.of(LogLevel.INFO, Level.INFO),
            Arguments.of(LogLevel.CONFIG, Level.CONFIG),
            Arguments.of(LogLevel.DEBUG, Level.FINE),
            Arguments.of(LogLevel.TRACE, Level.FINER),
            Arguments.of(LogLevel.FINE, Level.FINE),
            Arguments.of(LogLevel.FINER, Level.FINER),
            Arguments.of(LogLevel.FINEST, Level.FINEST)
        );
    }

    @ParameterizedTest
    @MethodSource("levelNames")
    @DisplayName("fromString should resolve level names regardless of case or whitespace")
    void fromStringShouldResolveLevelNames(final String input, final LogLevel expectedLevel) {
        assertEquals(expectedLevel, LogLevel.fromString(input));
    }

    private static Stream<Arguments> levelNames() {
        return Stream.of(
            Arguments.of("critical", LogLevel.CRITICAL),
            Arguments.of(" ERROR ", LogLevel.ERROR),
            Arguments.of("Warn", LogLevel.WARN),
            Arguments.of("warning", LogLevel.WARNING),
            Arguments.of("info", LogLevel.INFO),
            Arguments.of("config", LogLevel.CONFIG),
            Arguments.of("debug", LogLevel.DEBUG),
            Arguments.of("trace", LogLevel.TRACE),
            Arguments.of("fine", LogLevel.FINE),
            Arguments.of("finer", LogLevel.FINER),
            Arguments.of("finest", LogLevel.FINEST)
        );
    }

    @Test
    @DisplayName("fromString should default to INFO when input is null, blank, or unknown")
    void fromStringShouldDefaultToInfoWhenInputIsNullBlankOrUnknown() {
        assertEquals(LogLevel.INFO, LogLevel.fromString(null));
        assertEquals(LogLevel.INFO, LogLevel.fromString(""));
        assertEquals(LogLevel.INFO, LogLevel.fromString("   "));
        assertEquals(LogLevel.INFO, LogLevel.fromString("not-a-real-level"));
    }

    @Test
    @DisplayName("fromString should map JUL level aliases to documented defaults")
    void fromStringShouldMapJulLevelAliases() {
        assertEquals(LogLevel.ERROR, LogLevel.fromString(Level.SEVERE.getName()));
        assertEquals(LogLevel.INFO, LogLevel.fromString(Level.INFO.getName()));
    }
}
