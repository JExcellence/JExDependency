package com.raindropcentral.core.database.entity.player;

import com.raindropcentral.core.database.entity.central.RCentralServer;
import com.raindropcentral.core.database.entity.statistic.RPlayerStatistic;
import de.jexcellence.hibernate.converter.UuidBytesConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a persisted player profile stored in the {@code r_player} table.
 * Tracks all players who join connected servers for statistics on the web platform.
 */
@Entity
@Table(name = "r_player")
public class RPlayer extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 16;

    @Column(name = "unique_id", unique = true, nullable = false)
    @Convert(converter = UuidBytesConverter.class)
    private UUID uniqueId;

    @Column(name = "player_name", nullable = false, length = MAX_NAME_LENGTH)
    private String playerName;

    @Column(name = "first_seen")
    private LocalDateTime firstSeen;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @ManyToMany
    @JoinTable(
            name = "r_player_servers",
            joinColumns = @JoinColumn(name = "player_id"),
            inverseJoinColumns = @JoinColumn(name = "server_id")
    )
    private Set<RCentralServer> serversJoined = new HashSet<>();

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private RPlayerStatistic playerStatistic;

    protected RPlayer() {}

    public RPlayer(final @NotNull UUID uniqueId, final @NotNull String playerName) {
        this.uniqueId = Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        this.playerName = validatePlayerName(playerName);
        this.firstSeen = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
    }

    public RPlayer(final @NotNull Player bukkitPlayer) {
        this(bukkitPlayer.getUniqueId(), bukkitPlayer.getName());
    }

    public @NotNull UUID getUniqueId() {
        return this.uniqueId;
    }

    /**
     * Gets playerName.
     */
    public @NotNull String getPlayerName() {
        return this.playerName;
    }

    /**
     * Performs updatePlayerName.
     */
    public void updatePlayerName(final @NotNull String newName) {
        this.playerName = validatePlayerName(newName);
    }

    public @Nullable LocalDateTime getFirstSeen() {
        return this.firstSeen;
    }

    public @Nullable LocalDateTime getLastSeen() {
        return this.lastSeen;
    }

    /**
     * Performs updateLastSeen.
     */
    public void updateLastSeen() {
        this.lastSeen = LocalDateTime.now();
    }

    /**
     * Gets serversJoined.
     */
    public @NotNull Set<RCentralServer> getServersJoined() {
        return this.serversJoined;
    }

    /**
     * Performs addServerJoined.
     */
    public void addServerJoined(final @NotNull RCentralServer server) {
        this.serversJoined.add(server);
    }

    /**
     * Gets playerStatistic.
     */
    public @Nullable RPlayerStatistic getPlayerStatistic() {
        return this.playerStatistic;
    }

    /**
     * Sets playerStatistic.
     */
    public void setPlayerStatistic(final @NotNull RPlayerStatistic playerStatistic) {
        Objects.requireNonNull(playerStatistic, "playerStatistic cannot be null");
        this.playerStatistic = playerStatistic;
        playerStatistic.setPlayer(this);
    }

    /**
     * Returns whether statistics.
     */
    public boolean hasStatistics() {
        return this.playerStatistic != null && !this.playerStatistic.getStatistics().isEmpty();
    }

    private static String validatePlayerName(final @NotNull String name) {
        Objects.requireNonNull(name, "playerName cannot be null");
        var trimmed = name.trim();
        if (trimmed.length() < MIN_NAME_LENGTH || trimmed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Player name must be between %d and %d characters, got: %d"
                            .formatted(MIN_NAME_LENGTH, MAX_NAME_LENGTH, trimmed.length())
            );
        }
        return trimmed;
    }

    @Override
    public String toString() {
        return "RPlayer[id=%d, uuid=%s, name=%s, hasStats=%b]"
                .formatted(getId(), uniqueId, playerName, hasStatistics());
    }
}
