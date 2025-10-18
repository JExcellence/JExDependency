package com.raindropcentral.core.database.entity.statistic;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Entity
@DiscriminatorValue("STRING")
public class RStringStatistic extends RAbstractStatistic {
    
    @Column(name = "statistic_value", columnDefinition = "TEXT")
    private String value;
    
    protected RStringStatistic() {}
    
    public RStringStatistic(
        final @NotNull String identifier,
        final @NotNull String plugin,
        final @NotNull String value
    ) {
        super(identifier, plugin);
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }
    
    @Override
    public @NotNull String getValue() {
        return this.value;
    }
    
    public void setValue(final @NotNull String value) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }
    
    public boolean isEmpty() {
        return this.value.isEmpty();
    }
    
    public int length() {
        return this.value.length();
    }
    
    @Override
    public String toString() {
        final String displayValue = value.length() > 50 
            ? value.substring(0, 47) + "..." 
            : value;
        return "RStringStatistic[id=%d, identifier=%s, plugin=%s, value=%s]"
            .formatted(getId(), identifier, getPlugin(), displayValue);
    }
}
