package de.jexcellence.economy.type;

import de.jexcellence.economy.command.player.currencylog.PCurrencyLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ELogTypeTest {

    @Test
    @DisplayName("enum values align with documented log categories")
    void enumValuesMatchDocumentedCategories() {
        EnumSet<ELogType> documentedCategories = EnumSet.of(
                ELogType.TRANSACTION,
                ELogType.MANAGEMENT,
                ELogType.SYSTEM,
                ELogType.ERROR,
                ELogType.AUDIT,
                ELogType.DEBUG
        );

        assertEquals(
                documentedCategories,
                EnumSet.allOf(ELogType.class),
                "Enum declarations should remain in sync with the documented log categories"
        );
    }

    @Test
    @DisplayName("log filter builder accepts every log type")
    void logFilterBuilderHandlesAllLogTypes() {
        for (ELogType logType : ELogType.values()) {
            LogFilter filter = LogFilter.builder()
                    .withLogType(logType)
                    .build();

            assertSame(
                    logType,
                    filter.logType,
                    () -> "Builder should preserve the log type " + logType
            );
        }
    }

    @Test
    @DisplayName("player command helper exposes every log type for filtering")
    void playerCommandHelperStaysSynchronizedWithEnumValues() throws Exception {
        Field logTypesField = PCurrencyLog.class.getDeclaredField("LOG_TYPES");
        logTypesField.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> logTypes = (List<String>) logTypesField.get(null);

        List<String> expected = EnumSet.allOf(ELogType.class).stream()
                .map(Enum::name)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());

        assertEquals(
                expected,
                logTypes,
                "Command filtering helper should list every available log type"
        );
    }

    @Test
    @DisplayName("enum name lookups round-trip successfully and reject invalid values")
    void enumNameLookupsHandleInvalidValues() {
        for (ELogType logType : ELogType.values()) {
            String serialized = logType.name();
            assertSame(logType, ELogType.valueOf(serialized));

            String lowercase = serialized.toLowerCase(Locale.ROOT);
            assertSame(logType, ELogType.valueOf(lowercase.toUpperCase(Locale.ROOT)));
        }

        assertThrows(
                IllegalArgumentException.class,
                () -> ELogType.valueOf("UNKNOWN"),
                "Invalid log type names should be rejected"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ELogType.valueOf("   ".trim().toUpperCase(Locale.ROOT)),
                "Blank log type names should be rejected"
        );
    }
}
