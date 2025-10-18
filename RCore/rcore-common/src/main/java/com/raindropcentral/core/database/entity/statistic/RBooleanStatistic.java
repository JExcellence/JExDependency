package com.raindropcentral.core.database.entity.statistic;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Entity
@DiscriminatorValue("BOOLEAN")
public class RBooleanStatistic extends RAbstractStatistic {
    
    @Column(name = "statistic_value", nullable = false)
    private Boolean value;
    
    protected RBooleanStatistic() {}
    
    public RBooleanStatistic(
        final @NotNull String identifier,
        final @NotNull String plugin,
        final @NotNull Boolean value
    ) {
        super(identifier, plugin);
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }
    
    @Override
    public @NotNull Boolean getValue() {
        return this.value;
    }
    
    public void toggle() {
        this.value = !this.value;
    }
    
    public void setValue(final @NotNull Boolean value) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }
    
    @Override
    public String toString() {
        return "RBooleanStatistic[id=%d, identifier=%s, plugin=%s, value=%b]"
            .formatted(getId(), identifier, getPlugin(), value);
    }
}
