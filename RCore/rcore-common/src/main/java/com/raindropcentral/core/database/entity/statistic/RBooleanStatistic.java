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
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Entity
@DiscriminatorValue("BOOLEAN")
public class RBooleanStatistic extends RAbstractStatistic {

    /**
     * Column mapping for the boolean {@code statistic_value}. Stored as a non-null column and
     * toggled in place for lifecycle transitions.
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
     * Inverts the stored boolean value. Should be invoked inside a managed transaction so the
     * update is flushed appropriately.
     */
    public void toggle() {
        this.value = !this.value;
    }

    /**
     * Updates the persisted boolean payload.
     *
     * @param value new boolean value
     */
    public void setValue(final @NotNull Boolean value) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }
    
    @Override
    public String toString() {
        return "RBooleanStatistic[id=%d, identifier=%s, plugin=%s, value=%b]"
            .formatted(getId(), identifier, getPlugin(), value);
    }
}
