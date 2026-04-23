package de.jexcellence.core.database.entity.statistic;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.jetbrains.annotations.NotNull;

@Entity
@DiscriminatorValue("STRING")
public class StringStatistic extends AbstractStatistic {

    @Column(name = "statistic_string", columnDefinition = "TEXT")
    private String value;

    protected StringStatistic() {
    }

    public StringStatistic(@NotNull String identifier, @NotNull String plugin, @NotNull String value) {
        super(identifier, plugin);
        this.value = value;
    }

    @Override
    public @NotNull String getValue() {
        return this.value;
    }

    public void setValue(@NotNull String value) {
        this.value = value;
    }
}
