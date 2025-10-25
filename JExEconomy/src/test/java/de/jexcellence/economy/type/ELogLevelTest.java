package de.jexcellence.economy.type;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ELogLevelTest {

    @Test
    @DisplayName("each log level exposes its configured severity and display name")
    void logLevelsExposeSeverityAndDisplayName() {
        assertAll(
                "Severity values should map to the configured order",
                () -> assertEquals(10, ELogLevel.DEBUG.getSeverity()),
                () -> assertEquals(20, ELogLevel.INFO.getSeverity()),
                () -> assertEquals(30, ELogLevel.WARNING.getSeverity()),
                () -> assertEquals(40, ELogLevel.ERROR.getSeverity()),
                () -> assertEquals(50, ELogLevel.CRITICAL.getSeverity())
        );

        assertAll(
                "Display names should match the human readable variants",
                () -> assertEquals("Debug", ELogLevel.DEBUG.getDisplayName()),
                () -> assertEquals("Info", ELogLevel.INFO.getDisplayName()),
                () -> assertEquals("Warning", ELogLevel.WARNING.getDisplayName()),
                () -> assertEquals("Error", ELogLevel.ERROR.getDisplayName()),
                () -> assertEquals("Critical", ELogLevel.CRITICAL.getDisplayName())
        );
    }

    @Test
    @DisplayName("fromSeverity returns an empty optional when the severity is unknown")
    void fromSeverityRejectsUnknownValues() {
        assertFalse(ELogLevel.fromSeverity(999).isPresent(), "Unknown severities must return an empty Optional");
    }

    @Test
    @DisplayName("fromSeverity resolves known severities to their matching level")
    void fromSeverityResolvesKnownValues() {
        List.of(ELogLevel.values()).forEach(level ->
                assertEquals(level, ELogLevel.fromSeverity(level.getSeverity()).orElseThrow(),
                        "Expected severity " + level.getSeverity() + " to resolve to " + level)
        );
    }

    @Test
    @DisplayName("fromDisplayName resolves case-insensitive matches and trims surrounding whitespace")
    void fromDisplayNameHandlesFormatting() {
        assertEquals(ELogLevel.WARNING, ELogLevel.fromDisplayName("  warning  ").orElseThrow());
    }

    @Test
    @DisplayName("fromDisplayName returns an empty optional when the input is null or unknown")
    void fromDisplayNameRejectsInvalidInput() {
        assertTrue(ELogLevel.fromDisplayName(null).isEmpty(), "Null input should be rejected");
        assertTrue(ELogLevel.fromDisplayName("unknown").isEmpty(), "Unknown values should not resolve to a level");
    }

    @Test
    @DisplayName("fromName parses enum names in a case-insensitive manner")
    void fromNameParsesEnumNames() {
        assertEquals(ELogLevel.CRITICAL, ELogLevel.fromName("critical").orElseThrow());
    }

    @Test
    @DisplayName("fromName returns an empty optional when given blank or invalid values")
    void fromNameRejectsBlankOrUnknownValues() {
        assertTrue(ELogLevel.fromName("  ").isEmpty(), "Blank input should be rejected");
        assertTrue(ELogLevel.fromName("invalid").isEmpty(), "Unknown names should return empty Optional");
        assertTrue(ELogLevel.fromName(null).isEmpty(), "Null input should be rejected");
    }
}
