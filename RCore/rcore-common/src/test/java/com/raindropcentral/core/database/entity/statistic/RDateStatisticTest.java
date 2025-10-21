package com.raindropcentral.core.database.entity.statistic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RDateStatisticTest {

    @Test
    @DisplayName("Constructor guards against null epoch values")
    void constructorNullGuard() {
        final NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> new RDateStatistic("identifier", "plugin", null)
        );

        assertEquals("value cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("setValue updates epoch milliseconds and derived temporal views")
    void setValueUpdatesTemporalRepresentations() {
        final Instant initialInstant = Instant.parse("2024-03-10T12:00:00Z");
        final Instant updatedInstant = Instant.parse("2024-03-15T09:30:15Z");
        final RDateStatistic statistic = new RDateStatistic(
            "cooldown",
            "rcore",
            initialInstant.toEpochMilli()
        );

        statistic.setValue(updatedInstant.toEpochMilli());

        assertEquals(updatedInstant.toEpochMilli(), statistic.getValue());
        assertEquals(updatedInstant, statistic.getAsInstant());
        assertEquals(
            LocalDateTime.ofInstant(updatedInstant, ZoneOffset.UTC),
            statistic.getAsLocalDateTime()
        );
    }

    @Test
    @DisplayName("setValue enforces null guard on epoch milliseconds")
    void setValueNullGuard() {
        final RDateStatistic statistic = new RDateStatistic(
            "cooldown",
            "rcore",
            Instant.parse("2024-03-10T12:00:00Z").toEpochMilli()
        );

        final NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> statistic.setValue(null)
        );

        assertEquals("value cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("isBefore and isAfter compare using UTC instants")
    void comparisonOperatorsUseUtcInstants() {
        final Instant baseInstant = Instant.parse("2024-04-01T18:25:30Z");
        final RDateStatistic statistic = new RDateStatistic(
            "lockout",
            "rcore",
            baseInstant.toEpochMilli()
        );

        assertTrue(statistic.isBefore(baseInstant.plusSeconds(5)));
        assertFalse(statistic.isBefore(baseInstant.minusSeconds(5)));

        assertTrue(statistic.isAfter(baseInstant.minusSeconds(5)));
        assertFalse(statistic.isAfter(baseInstant.plusSeconds(5)));
    }

    @Test
    @DisplayName("updateToNow leverages injected clock for deterministic tests")
    void updateToNowUsesInjectedClock() {
        final AtomicLong clockValue = new AtomicLong(Instant.parse("2024-05-10T11:20:30Z").toEpochMilli());
        final TestableRDateStatistic statistic = new TestableRDateStatistic(
            "timestamp",
            "rcore",
            0L,
            clockValue::get
        );

        statistic.updateToNow();
        assertEquals(clockValue.get(), statistic.getValue());

        clockValue.set(Instant.parse("2024-05-10T12:20:30Z").toEpochMilli());
        statistic.updateToNow();
        assertEquals(clockValue.get(), statistic.getValue());
    }

    @Test
    @DisplayName("toString renders the UTC local date-time representation")
    void toStringRendersUtcTimestamp() {
        final Instant instant = Instant.parse("2024-06-01T06:45:00Z");
        final RDateStatistic statistic = new RDateStatistic(
            "report",
            "rcore",
            instant.toEpochMilli()
        );

        final String expected = "RDateStatistic[id=null, identifier=report, plugin=rcore, value=%s]".formatted(
            LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
        );

        assertEquals(expected, statistic.toString());
    }

    private static final class TestableRDateStatistic extends RDateStatistic {

        private final LongSupplier clock;

        private TestableRDateStatistic(
            final String identifier,
            final String plugin,
            final long value,
            final LongSupplier clock
        ) {
            super(identifier, plugin, value);
            this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        }

        @Override
        public void updateToNow() {
            setValue(this.clock.getAsLong());
        }
    }
}
