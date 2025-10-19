package com.raindropcentral.core.database.entity.statistic;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Date-backed statistic persisted with the {@code DATE} discriminator. Stores timestamps as
 * epoch milliseconds and exposes helper conversions for temporal comparisons.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Entity
@DiscriminatorValue("DATE")
public class RDateStatistic extends RAbstractStatistic {

    /**
     * Column mapping for epoch-millisecond {@code statistic_value}. Non-null to guarantee
     * consistent conversion to {@link Instant} and {@link LocalDateTime}.
     */
    @Column(name = "statistic_value", nullable = false)
    private Long value;

    /**
     * No-args constructor reserved for Hibernate use.
     */
    protected RDateStatistic() {}

    /**
     * Constructs a date statistic.
     *
     * @param identifier unique identifier within the owning aggregate
     * @param plugin     plugin namespace managing the statistic
     * @param value      epoch milliseconds to persist
     */
    public RDateStatistic(
        final @NotNull String identifier,
        final @NotNull String plugin,
        final @NotNull Long value
    ) {
        super(identifier, plugin);
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }

    /**
     * @return persisted epoch-millisecond value
     */
    @Override
    public @NotNull Long getValue() {
        return this.value;
    }

    /**
     * Updates the stored timestamp.
     *
     * @param value epoch milliseconds to persist
     */
    public void setValue(final @NotNull Long value) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }

    /**
     * @return timestamp as an {@link Instant}
     */
    public @NotNull Instant getAsInstant() {
        return Instant.ofEpochMilli(this.value);
    }

    /**
     * @return timestamp converted to UTC {@link LocalDateTime}
     */
    public @NotNull LocalDateTime getAsLocalDateTime() {
        return LocalDateTime.ofInstant(getAsInstant(), ZoneOffset.UTC);
    }

    /**
     * Determines if this timestamp occurs before the provided instant.
     *
     * @param other instant to compare against
     * @return {@code true} when this timestamp is before {@code other}
     */
    public boolean isBefore(final @NotNull Instant other) {
        return getAsInstant().isBefore(other);
    }

    /**
     * Determines if this timestamp occurs after the provided instant.
     *
     * @param other instant to compare against
     * @return {@code true} when this timestamp is after {@code other}
     */
    public boolean isAfter(final @NotNull Instant other) {
        return getAsInstant().isAfter(other);
    }

    /**
     * Updates the statistic to reflect the current system time. Invoke inside a managed
     * transaction when persisting the change.
     */
    public void updateToNow() {
        this.value = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return "RDateStatistic[id=%d, identifier=%s, plugin=%s, value=%s]"
            .formatted(getId(), identifier, getPlugin(), getAsLocalDateTime());
    }
}
