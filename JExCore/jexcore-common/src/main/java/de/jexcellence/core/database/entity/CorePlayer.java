package de.jexcellence.core.database.entity;

import de.jexcellence.core.database.entity.statistic.PlayerStatistic;
import de.jexcellence.jehibernate.entity.base.LongIdEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Canonical player record tracked by JExCore. Other Raindrop plugins reference
 * this entity by foreign key when they need to hang data off a tracked
 * identity.
 */
@Entity
@Table(name = "jexcore_player")
public class CorePlayer extends LongIdEntity {

    @Column(name = "unique_id", unique = true, nullable = false)
    private UUID uniqueId;

    @Column(name = "player_name", nullable = false, length = 16)
    private String playerName;

    @Column(name = "first_seen", nullable = false)
    private LocalDateTime firstSeen;

    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen;

    @ManyToMany
    @JoinTable(
            name = "jexcore_player_servers",
            joinColumns = @JoinColumn(name = "player_id"),
            inverseJoinColumns = @JoinColumn(name = "server_id")
    )
    private Set<CentralServer> serversJoined = new HashSet<>();

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private PlayerStatistic playerStatistic;

    protected CorePlayer() {
    }

    public CorePlayer(@NotNull UUID uniqueId, @NotNull String playerName) {
        final LocalDateTime now = LocalDateTime.now();
        this.uniqueId = uniqueId;
        this.playerName = playerName;
        this.firstSeen = now;
        this.lastSeen = now;
    }

    public @NotNull UUID getUniqueId() {
        return this.uniqueId;
    }

    public @NotNull String getPlayerName() {
        return this.playerName;
    }

    public void setPlayerName(@NotNull String playerName) {
        this.playerName = playerName;
    }

    public @NotNull LocalDateTime getFirstSeen() {
        return this.firstSeen;
    }

    public @NotNull LocalDateTime getLastSeen() {
        return this.lastSeen;
    }

    public void setLastSeen(@NotNull LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public @NotNull Set<CentralServer> getServersJoined() {
        return this.serversJoined;
    }

    public @Nullable PlayerStatistic getPlayerStatistic() {
        return this.playerStatistic;
    }

    public void setPlayerStatistic(@Nullable PlayerStatistic playerStatistic) {
        this.playerStatistic = playerStatistic;
    }

    @Override
    public String toString() {
        return "CorePlayer[" + this.playerName + "/" + this.uniqueId + "]";
    }
}
