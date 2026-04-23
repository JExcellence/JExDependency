package de.jexcellence.core.database.entity.statistic;

import de.jexcellence.jehibernate.entity.base.LongIdEntity;
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

/**
 * Base row for every persisted statistic value. Subclasses select a
 * discriminator value stored in the {@code statistic_type} column.
 * Identifier + owning aggregate id form a unique key.
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "statistic_type")
@Table(
        name = "jexcore_statistic",
        uniqueConstraints = @UniqueConstraint(columnNames = {"identifier", "player_statistic_id"})
)
public abstract class AbstractStatistic extends LongIdEntity {

    @Column(name = "identifier", nullable = false, length = 100)
    protected String identifier;

    @Column(name = "plugin", nullable = false, length = 50)
    private String plugin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_statistic_id")
    private PlayerStatistic playerStatistic;

    protected AbstractStatistic() {
    }

    protected AbstractStatistic(@NotNull String identifier, @NotNull String plugin) {
        this.identifier = identifier;
        this.plugin = plugin;
    }

    public abstract @NotNull Object getValue();

    public @NotNull String getIdentifier() {
        return this.identifier;
    }

    public @NotNull String getPlugin() {
        return this.plugin;
    }

    public @Nullable PlayerStatistic getPlayerStatistic() {
        return this.playerStatistic;
    }

    public void setPlayerStatistic(@Nullable PlayerStatistic playerStatistic) {
        this.playerStatistic = playerStatistic;
    }

    public boolean matches(@NotNull String identifier, @NotNull String plugin) {
        return this.identifier.equals(identifier) && this.plugin.equals(plugin);
    }
}
