package com.raindropcentral.rdq.database.entity.player;

import com.raindropcentral.rdq.database.entity.rank.RPlayerRank;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRankPath;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "rdq_player")
@Getter
@Setter
public class RDQPlayer extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "unqiue_id", nullable = false, unique = true)
    private UUID uniqueId;

    @Column(name = "player_name", nullable = false, length = 16)
    private String playerName;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<RPlayerRank> playerRanks = new ArrayList<>();


    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "player")
    private List<RPlayerRankPath> playerRankPaths = new ArrayList<>();

    protected RDQPlayer() {}

    public RDQPlayer(
            @NotNull UUID uniqueId,
            @NotNull String playerName
    ) {
        this.uniqueId = uniqueId;
        this.playerName = playerName;
    }

    public RDQPlayer(
            @NotNull Player player
    ) {
        this(player.getUniqueId(), player.getName());
    }

    /**
     * Adds a player rank to this player.
     * If adding a new active rank, deactivates all other ranks.
     *
     * @param playerRank the rank to add
     */
    public void addPlayerRank(@NotNull RPlayerRank playerRank) {
        if (playerRank.isActive()) {
            // Deactivate all other ranks when adding a new active rank
            playerRanks.forEach(rank -> rank.setActive(false));
        }
        playerRanks.add(playerRank);
        playerRank.setPlayer(this);
    }

    /**
     * Removes a player rank from this player.
     *
     * @param playerRank the rank to remove
     */
    public void removePlayerRank(@NotNull RPlayerRank playerRank) {
        playerRanks.remove(playerRank);
        playerRank.setPlayer(null);
    }

    /**
     * Gets the currently active rank for this player.
     *
     * @return the active rank, or empty if no rank is active
     */
    @NotNull
    public Optional<RPlayerRank> getActiveRank() {
        return playerRanks.stream()
                .filter(RPlayerRank::isActive)
                .findFirst();
    }

    /**
     * Sets a specific rank as the active rank.
     * Deactivates all other ranks.
     *
     * @param playerRank the rank to set as active
     */
    public void setActiveRank(@NotNull RPlayerRank playerRank) {
        if (!playerRanks.contains(playerRank)) {
            throw new IllegalArgumentException("Rank does not belong to this player");
        }
        
        // Deactivate all ranks
        playerRanks.forEach(rank -> rank.setActive(false));
        
        // Activate the specified rank
        playerRank.setActive(true);
    }

    /**
     * Gets all ranks for this player.
     *
     * @return list of all player ranks
     */
    @NotNull
    public List<RPlayerRank> getPlayerRanks() {
        return new ArrayList<>(playerRanks);
    }
}
