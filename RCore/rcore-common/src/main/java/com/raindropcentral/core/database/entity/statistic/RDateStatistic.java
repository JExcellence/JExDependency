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
     * Column mapping for epoch-millisecond {@code statistic_value}. Persisted as a non-null
     * {@code BIGINT} to satisfy Hibernate's discriminator requirements and guarantee consistent
     * conversion to {@link Instant} and {@link LocalDateTime} representations.
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
     * Resolves the persisted epoch milliseconds into a timezone-agnostic {@link Instant}. Use
     * this when interacting with APIs that expect UTC-based instants or when forwarding the
     * timestamp across JVM boundaries.
     *
     * @return timestamp as an {@link Instant} derived from the stored epoch milliseconds
     */
    public @NotNull Instant getAsInstant() {
        return Instant.ofEpochMilli(this.value);
    }

    /**
     * Converts the persisted epoch milliseconds into a {@link LocalDateTime} using the UTC zone
     * offset. This ensures consistent rendering regardless of the server's default timezone and is
     * suitable for displaying timestamps in logs or user interfaces where UTC normalization is
     * required.
     *
     * @return timestamp converted to a UTC {@link LocalDateTime}
     */
    public @NotNull LocalDateTime getAsLocalDateTime() {
        return LocalDateTime.ofInstant(getAsInstant(), ZoneOffset.UTC);
    }

    /**
     * Determines if this statistic's UTC instant occurs before the provided instant. Useful for
     * enforcing expiration or cooldown logic where comparisons must remain consistent across
     * different server timezones.
     *
     * @param other instant to compare against, typically sourced from UTC or system clock values
     * @return {@code true} when this timestamp is before {@code other}
     */
    public boolean isBefore(final @NotNull Instant other) {
        return getAsInstant().isBefore(other);
    }

    /**
     * Determines if this statistic's UTC instant occurs after the provided instant. Applicable for
     * validating that an event happened after a stored cutoff without relying on local timezone
     * assumptions.
     *
     * @param other instant to compare against, typically sourced from UTC or system clock values
     * @return {@code true} when this timestamp is after {@code other}
     */
    public boolean isAfter(final @NotNull Instant other) {
        return getAsInstant().isAfter(other);
    }

    /**
     * Updates the statistic to reflect the current system time in epoch milliseconds. Invoke this
     * within a managed transaction so Hibernate can persist the UTC-compatible value on flush.
     */
    public void updateToNow() {
        this.value = System.currentTimeMillis();
    }

    /**
     * Formats a debugging-friendly summary that surfaces the UTC {@link LocalDateTime} derived from
     * the persisted epoch milliseconds. This aids in logging and auditing flows without ambiguity
     * around server-local timezones.
     *
     * @return formatted string representation including the UTC timestamp
     */
    @Override
    public String toString() {
        return "RDateStatistic[id=%d, identifier=%s, plugin=%s, value=%s]"
            .formatted(getId(), identifier, getPlugin(), getAsLocalDateTime());
    }
}
