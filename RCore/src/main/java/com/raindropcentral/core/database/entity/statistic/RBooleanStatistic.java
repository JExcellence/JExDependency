package com.raindropcentral.core.database.entity.statistic;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents the type API type.
 */
/**
 * Represents the RBooleanStatistic API type.
 */
@Entity
@DiscriminatorValue("BOOLEAN")
public class RBooleanStatistic extends RAbstractStatistic {
    
    @Column(name = "statistic_boolean")
    private Boolean value;
    
    protected RBooleanStatistic() {}
    
    /**
     * Executes RBooleanStatistic.
     */
    public RBooleanStatistic(
        final @NotNull String identifier,
        final @NotNull String plugin,
        final @NotNull Boolean value
    ) {
        /**
         * Executes super.
         */
        super(identifier, plugin);
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }
    /**
     * Executes this member.
     */
    
    /**
     * Gets value.
     */
    @Override
    public @NotNull Boolean getValue() {
        return this.value;
    }
    
    /**
     * Executes toggle.
     */
    public void toggle() {
        this.value = !this.value;
    }
    
    /**
     * Sets value.
     */
    public void setValue(final @NotNull Boolean value) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }
    
    /**
     * Executes toString.
     */
    @Override
    public String toString() {
        return "RBooleanStatistic[id=%d, identifier=%s, plugin=%s, value=%b]"
            .formatted(getId(), identifier, getPlugin(), value);
    }
}
