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
 * Represents the type API type.
 */
/**
 * Represents the RDateStatistic API type.
 */
@Entity
@DiscriminatorValue("DATE")
public class RDateStatistic extends RAbstractStatistic {
    
    @Column(name = "statistic_date")
    private Long value;
    
    protected RDateStatistic() {}
    
    public RDateStatistic(
        final @NotNull String identifier,
        final @NotNull String plugin,
        final @NotNull Long value
    ) {
        super(identifier, plugin);
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }
    
    /**
     * Gets value.
     */
    @Override
    public @NotNull Long getValue() {
        return this.value;
    }
    
    /**
     * Sets value.
     */
    public void setValue(final @NotNull Long value) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }
    
    /**
     * Gets asInstant.
     */
    public @NotNull Instant getAsInstant() {
        return Instant.ofEpochMilli(this.value);
    }
    
    /**
     * Gets asLocalDateTime.
     */
    public @NotNull LocalDateTime getAsLocalDateTime() {
        return LocalDateTime.ofInstant(getAsInstant(), ZoneOffset.UTC);
    }
    
    /**
     * Returns whether before.
     */
    public boolean isBefore(final @NotNull Instant other) {
        return getAsInstant().isBefore(other);
    }
    
    /**
     * Returns whether after.
     */
    public boolean isAfter(final @NotNull Instant other) {
        return getAsInstant().isAfter(other);
    }
    
    /**
     * Performs updateToNow.
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
