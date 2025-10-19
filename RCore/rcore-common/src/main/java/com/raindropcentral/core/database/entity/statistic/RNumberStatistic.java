package com.raindropcentral.core.database.entity.statistic;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Numeric statistic stored using the {@code NUMBER} discriminator. Supports arithmetic helpers
 * for counters tracked in the {@code r_statistic} table.
 *
 * <p>Values are persisted as {@link Double} to balance precision and storage. Callers should
 * apply domain-specific rounding when exposing results.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Entity
@DiscriminatorValue("NUMBER")
public class RNumberStatistic extends RAbstractStatistic {

    /**
     * Column mapping for numeric {@code statistic_value}. Stored as non-null double precision
     * values and updated in place for counter mutations.
     */
    @Column(name = "statistic_value", nullable = false)
    private Double value;

    /**
     * JPA-only constructor for proxy materialization.
     */
    protected RNumberStatistic() {}

    /**
     * Constructs a numeric statistic.
     *
     * @param identifier statistic identifier unique per player aggregate
     * @param plugin     plugin namespace that owns the value
     * @param value      numeric payload to persist
     */
    public RNumberStatistic(
        final @NotNull String identifier,
        final @NotNull String plugin,
        final @NotNull Double value
    ) {
        super(identifier, plugin);
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }

    /**
     * @return persisted numeric value for this statistic
     */
    @Override
    public @NotNull Double getValue() {
        return this.value;
    }

    /**
     * Replaces the numeric payload. Accepts {@code null} to allow Hibernate to manage
     * database-level not-null violations if invoked incorrectly.
     *
     * @param value new numeric value; callers should avoid {@code null}
     */
    public void setValue(final Double value) { this.value = value; }

    /**
     * Increments the value by the provided amount.
     *
     * @param amount amount to add to the current value
     */
    public void increment(final double amount) {
        this.value += amount;
    }

    /**
     * Decrements the value while preventing negative totals.
     *
     * @param amount amount to subtract
     */
    public void decrement(final double amount) {
        this.value = Math.max(0.0, this.value - amount);
    }

    /**
     * Multiplies the value by the provided factor.
     *
     * @param factor scaling factor
     */
    public void multiply(final double factor) {
        this.value *= factor;
    }

    /**
     * @return {@code true} when the stored value is greater than zero
     */
    public boolean isPositive() {
        return this.value > 0.0;
    }

    /**
     * @return {@code true} when the stored value is effectively zero within a small epsilon
     */
    public boolean isZero() {
        return Math.abs(this.value) < 0.0001;
    }
    
    @Override
    public String toString() {
        return "RNumberStatistic[id=%d, identifier=%s, plugin=%s, value=%.2f]"
            .formatted(getId(), identifier, getPlugin(), value);
    }
}
