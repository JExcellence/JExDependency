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
 * Represents the RStringStatistic API type.
 */
@Entity
@DiscriminatorValue("STRING")
public class RStringStatistic extends RAbstractStatistic {
    
    @Column(name = "statistic_string", columnDefinition = "TEXT")
    private String value;
    
    protected RStringStatistic() {}
    
    /**
     * Executes RStringStatistic.
     */
    public RStringStatistic(
        final @NotNull String identifier,
        final @NotNull String plugin,
        final @NotNull String value
    ) {
        super(identifier, plugin);
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }
    
    /**
     * Gets value.
     */
    @Override
    /**
     * Executes getValue.
     */
    public @NotNull String getValue() {
        return this.value;
    }
    /**
     * Executes this member.
     */
    
    /**
     * Sets value.
     */
    public void setValue(final @NotNull String value) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }
    
    /**
     * Returns whether empty.
     */
    public boolean isEmpty() {
        return this.value.isEmpty();
    }
    
    /**
     * Executes length.
     */
    public int length() {
        return this.value.length();
    }
    
    /**
     * Executes toString.
     */
    @Override
    public String toString() {
        final String displayValue = value.length() > 50 
            ? value.substring(0, 47) + "..." 
            : value;
        return "RStringStatistic[id=%d, identifier=%s, plugin=%s, value=%s]"
            .formatted(getId(), identifier, getPlugin(), displayValue);
    }
}
