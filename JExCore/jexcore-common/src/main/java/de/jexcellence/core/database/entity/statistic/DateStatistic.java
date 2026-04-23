package de.jexcellence.core.database.entity.statistic;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

@Entity
@DiscriminatorValue("DATE")
public class DateStatistic extends AbstractStatistic {

    @Column(name = "statistic_date")
    private Long value;

    protected DateStatistic() {
    }

    public DateStatistic(@NotNull String identifier, @NotNull String plugin, long epochMillis) {
        super(identifier, plugin);
        this.value = epochMillis;
    }

    @Override
    public @NotNull Long getValue() {
        return this.value;
    }

    public void setValue(long epochMillis) {
        this.value = epochMillis;
    }

    public @NotNull Instant asInstant() {
        return Instant.ofEpochMilli(this.value);
    }
}
