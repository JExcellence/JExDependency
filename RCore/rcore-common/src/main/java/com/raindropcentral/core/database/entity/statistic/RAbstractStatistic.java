package com.raindropcentral.core.database.entity.statistic;

import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Objects;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "statistic_type")
@Table(
    name = "r_statistic",
    uniqueConstraints = @UniqueConstraint(columnNames = {"identifier", "player_statistic_id"})
)
public abstract class RAbstractStatistic extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "identifier", nullable = false, length = 100)
    protected String identifier;
    
    @Column(name = "plugin", nullable = false, length = 50)
    private String plugin;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_statistic_id")
    private RPlayerStatistic playerStatistic;
    
    protected RAbstractStatistic() {}
    
    protected RAbstractStatistic(final @NotNull String identifier, final @NotNull String plugin) {
        this.identifier = Objects.requireNonNull(identifier, "identifier cannot be null");
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
    }
    
    public abstract @NotNull Object getValue();
    
    public @NotNull String getIdentifier() {
        return this.identifier;
    }
    
    public @NotNull String getPlugin() {
        return this.plugin;
    }
    
    public @Nullable RPlayerStatistic getPlayerStatistic() {
        return this.playerStatistic;
    }
    
    public void setPlayerStatistic(final @Nullable RPlayerStatistic playerStatistic) {
        this.playerStatistic = playerStatistic;
    }
    
    public boolean matches(final @NotNull String identifier, final @NotNull String plugin) {
        return this.identifier.equals(identifier) && this.plugin.equals(plugin);
    }
}
