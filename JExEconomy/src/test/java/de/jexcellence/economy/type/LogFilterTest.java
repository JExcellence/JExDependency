package de.jexcellence.economy.type;

import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.CurrencyLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogFilterTest {

    private Currency currency;
    private CurrencyLog referenceLog;
    private UUID playerUuid;
    private UUID initiatorUuid;
    private LocalDateTime timestamp;

    @BeforeEach
    void setUp() throws Exception {
        this.playerUuid = UUID.randomUUID();
        this.initiatorUuid = UUID.randomUUID();
        this.timestamp = LocalDateTime.of(2024, 1, 15, 12, 30);

        this.currency = new Currency("credits");
        assignEntityId(this.currency, 42L);

        this.referenceLog = createTransactionLog(EChangeType.DEPOSIT, true, this.initiatorUuid, this.timestamp);
    }

    @Test
    @DisplayName("matches returns true when no criteria are set")
    void matchesReturnsTrueWhenNoCriteriaSet() {
        LogFilter filter = LogFilter.builder().build();

        assertTrue(filter.matches(this.referenceLog), "Logs should match when no filters are configured");
    }

    @Test
    @DisplayName("builder and setter configured criteria must all match")
    void matchesEvaluatesBuilderAndSetterConfiguredCriteria() {
        LogFilter filter = LogFilter.builder()
                .withLogType(ELogType.TRANSACTION)
                .withLogLevel(ELogLevel.INFO)
                .withDateFrom(this.timestamp.minusHours(1))
                .withDateTo(this.timestamp.plusHours(1))
                .build()
                .setInitiatorUuid(this.initiatorUuid);

        assertTrue(filter.matches(this.referenceLog), "Expected the filter to match when every criterion aligns");

        LogFilter mismatched = LogFilter.builder()
                .withLogType(ELogType.SYSTEM)
                .withLogLevel(ELogLevel.INFO)
                .withDateFrom(this.timestamp.minusHours(1))
                .withDateTo(this.timestamp.plusHours(1))
                .build()
                .setInitiatorUuid(this.initiatorUuid);

        assertFalse(mismatched.matches(this.referenceLog), "Different log type should cause the filter to reject the log");
    }

    @Test
    @DisplayName("date filters treat the boundaries as inclusive")
    void matchesRespectsInclusiveDateBounds() {
        LogFilter inclusiveFilter = LogFilter.builder()
                .withDateFrom(this.timestamp)
                .withDateTo(this.timestamp)
                .build();

        assertTrue(inclusiveFilter.matches(this.referenceLog), "Log should match when occurring exactly on the boundary");

        inclusiveFilter.setDateTo(this.timestamp.minusNanos(1));
        assertFalse(inclusiveFilter.matches(this.referenceLog), "Log after the upper bound should not match");
    }

    @Test
    @DisplayName("all active criteria must be satisfied (logical AND)")
    void matchesRequiresAllCriteriaToBeSatisfied() throws Exception {
        LogFilter combinedFilter = LogFilter.builder()
                .withPlayerUuid(this.playerUuid)
                .withInitiatorUuid(this.initiatorUuid)
                .withCurrencyId(42L)
                .withOperationType(EChangeType.DEPOSIT)
                .withLogType(ELogType.TRANSACTION)
                .withLogLevel(ELogLevel.INFO)
                .withSuccessOnly(true)
                .withDateFrom(this.timestamp.minusDays(1))
                .withDateTo(this.timestamp.plusDays(1))
                .build();

        assertTrue(combinedFilter.matches(this.referenceLog), "Reference log should satisfy every configured criterion");

        CurrencyLog failingLog = createTransactionLog(EChangeType.WITHDRAW, true, this.initiatorUuid, this.timestamp);
        assertFalse(combinedFilter.matches(failingLog), "Changing any field should cause the combined filter to reject the log");
    }

    private CurrencyLog createTransactionLog(
            final EChangeType operationType,
            final boolean success,
            final UUID initiator,
            final LocalDateTime occurredAt
    ) throws Exception {
        CurrencyLog log = new CurrencyLog(
                this.playerUuid,
                "PlayerOne",
                this.currency,
                operationType,
                100.0,
                150.0,
                50.0,
                "Quest reward",
                "Daily quest"
        );
        assignEntityId(log, 100L);
        log.setTimestamp(occurredAt);
        log.setInitiatorUuid(initiator);
        log.setLogLevel(ELogLevel.INFO);
        log.setLogType(ELogType.TRANSACTION);
        log.setSuccess(success);
        return log;
    }

    private static void assignEntityId(final Object entity, final long identifier) throws Exception {
        Field idField = null;
        Class<?> current = entity.getClass();
        while (current != null && idField == null) {
            try {
                idField = current.getDeclaredField("id");
            } catch (NoSuchFieldException ex) {
                current = current.getSuperclass();
            }
        }

        if (idField == null) {
            throw new IllegalStateException("Unable to locate identifier field on entity");
        }

        idField.setAccessible(true);
        idField.set(entity, identifier);
    }
}
