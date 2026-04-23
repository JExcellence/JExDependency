package de.jexcellence.core.database.entity.statistic;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.jetbrains.annotations.NotNull;

@Entity
@DiscriminatorValue("BOOLEAN")
public class BooleanStatistic extends AbstractStatistic {

    @Column(name = "statistic_boolean")
    private Boolean value;

    protected BooleanStatistic() {
    }

    public BooleanStatistic(@NotNull String identifier, @NotNull String plugin, boolean value) {
        super(identifier, plugin);
        this.value = value;
    }

    @Override
    public @NotNull Boolean getValue() {
        return this.value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }
}
