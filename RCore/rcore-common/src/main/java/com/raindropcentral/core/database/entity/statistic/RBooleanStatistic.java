package com.raindropcentral.core.database.entity.statistic;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Boolean-backed statistic persisted through the {@code BOOLEAN} discriminator value in
 * the {@code r_statistic} table. Encodes true/false flags such as tutorial completion
 * markers.
 * <p>
 * Toggle and assignment operations should emit {@link com.raindropcentral.rplatform.logging.CentralLogger
 * CentralLogger} entries so downstream services can audit why a flag changed. Log at debug when a
 * cached value is read due to a cache miss and at info when state transitions occur; escalate
 * failures (for example, null assignments) to error logs containing the statistic identifier.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Entity
@DiscriminatorValue("BOOLEAN")
public class RBooleanStatistic extends RAbstractStatistic {

    /**
     * Column mapping for the boolean {@code statistic_value}. Persisted as a non-null column to
     * guarantee transactional toggles never interact with {@code null} state despite Hibernate's
     * boxed {@link Boolean} requirement.
     */
    @Column(name = "statistic_value", nullable = false)
    private Boolean value;

    /**
     * No-args constructor reserved for Hibernate.
     */
    protected RBooleanStatistic() {}

    /**
     * Constructs a boolean statistic row.
     *
     * @param identifier statistic identifier shared with the aggregate constraint
     * @param plugin     plugin namespace creating the statistic
     * @param value      persisted boolean payload
     */
    public RBooleanStatistic(
        final @NotNull String identifier,
        final @NotNull String plugin,
        final @NotNull Boolean value
    ) {
        super(identifier, plugin);
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }

    /**
     * @return persisted boolean value associated with this statistic
     */
    @Override
    public @NotNull Boolean getValue() {
        return this.value;
    }

    /**
     * Inverts the stored boolean value.
     *
     * <p>This mutator is expected to run inside an active persistence transaction so that the
     * change is detected and flushed by the entity manager without exposing a transient
     * {@code null} intermediary.</p>
     */
    public void toggle() {
        this.value = !this.value;
    }

    /**
     * Updates the persisted boolean payload, validating the provided value.
     *
     * @param value new boolean value that must be non-null
     * @throws NullPointerException when {@code value} is {@code null}
     */
    public void setValue(final @NotNull Boolean value) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }

    /**
     * Builds a debug-friendly representation of the statistic, including identifier metadata.
     *
     * @return formatted string suitable for logging and diagnostics
     */
    @Override
    public String toString() {
        return "RBooleanStatistic[id=%d, identifier=%s, plugin=%s, value=%b]"
            .formatted(getId(), identifier, getPlugin(), value);
    }
}
