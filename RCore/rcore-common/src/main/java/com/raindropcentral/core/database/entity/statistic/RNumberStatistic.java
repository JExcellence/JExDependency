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
     * Column mapping for numeric {@code statistic_value}. Persisted as a non-null double precision
     * column with the {@link #value} field managed directly by Hibernate.
     *
     * <p>Although the entity accepts any {@link Double} that can be serialized by the JDBC driver,
     * callers should provide finite values within domain-specific ranges. Database constraints and
     * the {@code nullable = false} contract guarantee the column always contains a value.</p>
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
     * Replaces the numeric payload with the exact value supplied.
     *
     * <p>No clamping or rounding is applied; callers are expected to normalize their domain values
     * prior to invocation. While the field is mapped as {@code nullable = false}, the setter
     * accepts {@code null} so Hibernate can surface the resulting persistence violation when
     * invoked incorrectly.</p>
     *
     * @param value new numeric value to persist. Prefer finite doubles within the supported domain.
     */
    public void setValue(final Double value) { this.value = value; }

    /**
     * Increments the current value by the provided amount without applying any rounding.
     *
     * <p>The result may exceed previously established bounds; callers should pass deltas that keep
     * the final value within their acceptable domain. Negative amounts effectively decrement the
     * value.</p>
     *
     * @param amount amount to add to the current value; can be negative for manual reductions
     */
    public void increment(final double amount) {
        this.value += amount;
    }

    /**
     * Decrements the value while clamping the result to a minimum of {@code 0.0}.
     *
     * <p>Any subtraction that would drive the value below zero is snapped back to zero, preventing
     * negative persisted totals. Positive amounts reduce the value, while negative amounts behave
     * like an increment.</p>
     *
     * @param amount amount to subtract from the current value; negative amounts trigger a raise
     */
    public void decrement(final double amount) {
        this.value = Math.max(0.0, this.value - amount);
    }

    /**
     * Multiplies the current value by the provided factor.
     *
     * <p>No rounding or clamping is applied to the product. Factors less than {@code 0.0} will
     * invert the sign of the stored value, and fractional factors will scale the value according to
     * standard floating-point rules.</p>
     *
     * @param factor scaling factor applied to the stored value
     */
    public void multiply(final double factor) {
        this.value *= factor;
    }

    /**
     * Determines whether the stored value is strictly greater than zero.
     *
     * <p>No tolerance window is applied—values such as {@code 0.0000001} are considered positive.</p>
     *
     * @return {@code true} when the stored value is greater than zero
     */
    public boolean isPositive() {
        return this.value > 0.0;
    }

    /**
     * Determines whether the stored value is effectively zero within a small epsilon.
     *
     * <p>The comparison uses {@code |value| < 0.0001} to absorb minor floating-point noise that may
     * accumulate during repeated arithmetic operations.</p>
     *
     * @return {@code true} when the stored value is effectively zero within a small epsilon
     */
    public boolean isZero() {
        return Math.abs(this.value) < 0.0001;
    }

    /**
     * Generates a concise debug representation of the statistic.
     *
     * <p>The numeric component is formatted to two decimal places via {@code %.2f}, rounding the
     * displayed value while leaving the persisted {@link #value} untouched.</p>
     *
     * @return string snapshot containing the identifier, owning plugin, and rounded value
     */
    @Override
    public String toString() {
        return "RNumberStatistic[id=%d, identifier=%s, plugin=%s, value=%.2f]"
            .formatted(getId(), identifier, getPlugin(), value);
    }
}
