package de.jexcellence.core.database.entity.statistic;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.jetbrains.annotations.NotNull;

@Entity
@DiscriminatorValue("NUMBER")
public class NumberStatistic extends AbstractStatistic {

    @Column(name = "statistic_number")
    private Double value;

    protected NumberStatistic() {
    }

    public NumberStatistic(@NotNull String identifier, @NotNull String plugin, double value) {
        super(identifier, plugin);
        this.value = value;
    }

    @Override
    public @NotNull Double getValue() {
        return this.value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
