package com.raindropcentral.core.database.entity.player;

import com.raindropcentral.core.database.entity.statistic.RPlayerStatistic;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "r_player")
public class RPlayer extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 16;
    
    @Column(name = "unique_id", unique = true, nullable = false)
    private UUID uniqueId;
    
    @Column(name = "player_name", nullable = false, length = MAX_NAME_LENGTH)
    private String playerName;

    @OneToOne(
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private RPlayerStatistic playerStatistic;

    protected RPlayer() {}

    public RPlayer(final @NotNull UUID uniqueId, final @NotNull String playerName) {
        this.uniqueId = Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        this.playerName = validatePlayerName(playerName);
    }

    public RPlayer(final @NotNull Player bukkitPlayer) {
        this(bukkitPlayer.getUniqueId(), bukkitPlayer.getName());
    }

    public @NotNull UUID getUniqueId() {
        return this.uniqueId;
    }

    public @NotNull String getPlayerName() {
        return this.playerName;
    }

    public void updatePlayerName(final @NotNull String newName) {
        this.playerName = validatePlayerName(newName);
    }

    public @Nullable RPlayerStatistic getPlayerStatistic() {
        return this.playerStatistic;
    }

    public void setPlayerStatistic(final @NotNull RPlayerStatistic playerStatistic) {
        Objects.requireNonNull(playerStatistic, "playerStatistic cannot be null");
        this.playerStatistic = playerStatistic;
        playerStatistic.setPlayer(this);
    }

    public boolean hasStatistics() {
        return this.playerStatistic != null && !this.playerStatistic.getStatistics().isEmpty();
    }

    private static String validatePlayerName(final @NotNull String name) {
        Objects.requireNonNull(name, "playerName cannot be null");
        
        final String trimmed = name.trim();
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
