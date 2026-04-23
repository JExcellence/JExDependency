package de.jexcellence.core.database.entity.statistic;

import de.jexcellence.core.database.entity.CentralServer;
import de.jexcellence.core.database.entity.CorePlayer;
import de.jexcellence.jehibernate.entity.base.LongIdEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Aggregate root for the set of statistic rows belonging to one
 * {@link CorePlayer}. Optionally scoped to a {@link CentralServer}.
 */
@Entity
@Table(name = "jexcore_player_statistic")
public class PlayerStatistic extends LongIdEntity {

    @OneToOne(mappedBy = "playerStatistic")
    private CorePlayer player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id")
    private CentralServer server;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "player_statistic_id")
    private Set<AbstractStatistic> statistics = new HashSet<>();

    protected PlayerStatistic() {
    }

    public PlayerStatistic(@NotNull CorePlayer player) {
        this.player = player;
    }

    public @NotNull CorePlayer getPlayer() {
        return this.player;
    }

    public void setPlayer(@NotNull CorePlayer player) {
        this.player = player;
    }

    public @Nullable CentralServer getServer() {
        return this.server;
    }

    public void setServer(@Nullable CentralServer server) {
        this.server = server;
    }

    public @NotNull Set<AbstractStatistic> getStatistics() {
        return this.statistics;
    }

    @Override
    public String toString() {
        return "PlayerStatistic[player=" + (this.player != null ? this.player.getPlayerName() : "null")
                + ", count=" + this.statistics.size() + "]";
    }
}
