package com.raindropcentral.rdq.database.entity.player;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.perk.RPlayerPerk;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRank;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRankPath;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.*;

@Entity
@Table(name = "rdq_player")
public final class RDQPlayer extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "unique_id", unique = true, nullable = false)
    private UUID uniqueId;

    @Column(name = "player_name", nullable = false, length = 16)
    private String playerName;

    // Bounty relationship removed - bounties are managed by UUID references

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "player")
    private List<RPlayerRank> playerRanks = new ArrayList<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "player")
    private List<RPlayerRankPath> playerRankPaths = new ArrayList<>();

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<RPlayerPerk> playerPerks = new HashSet<>();

    @Embedded
    private BountyStatistics bountyStatistics = new BountyStatistics();

    protected RDQPlayer() {}

    public RDQPlayer(@NotNull UUID uniqueId, @NotNull String playerName) {
        this.uniqueId = Objects.requireNonNull(uniqueId);
        this.playerName = Objects.requireNonNull(playerName);
    }

    public RDQPlayer(@NotNull Player player) {
        this(player.getUniqueId(), player.getName());
    }

    public @NotNull UUID getUniqueId() { return uniqueId; }
    public @NotNull String getPlayerName() { return playerName; }
    // Bounty getter removed - use BountyService to get bounties by player UUID
    
    public void setUniqueId(@NotNull UUID uniqueId) { this.uniqueId = Objects.requireNonNull(uniqueId); }
    public void setPlayerName(@NotNull String playerName) { this.playerName = Objects.requireNonNull(playerName); }
    
    // Bounty setter removed - use BountyService to manage bounties

    public List<RPlayerRank> getPlayerRanks() { return Collections.unmodifiableList(playerRanks); }
    public List<RPlayerRankPath> getPlayerRankPaths() { return Collections.unmodifiableList(playerRankPaths); }
    public Set<RPlayerPerk> getPlayerPerks() { return Collections.unmodifiableSet(playerPerks); }
    public BountyStatistics getBountyStatistics() { return bountyStatistics; }
    
    public void setPlayerRanks(@NotNull List<RPlayerRank> playerRanks) {
        this.playerRanks.clear();
        if (playerRanks != null) this.playerRanks.addAll(playerRanks);
    }
    
    public void setPlayerRankPaths(@NotNull List<RPlayerRankPath> playerRankPaths) {
        this.playerRankPaths.clear();
        if (playerRankPaths != null) this.playerRankPaths.addAll(playerRankPaths);
    }

    public void addPlayerRank(@NotNull RPlayerRank playerRank) {
        Objects.requireNonNull(playerRank);
        if (!playerRanks.contains(playerRank)) {
            playerRanks.add(playerRank);
            playerRank.setRdqPlayer(this);
        }
    }

    public void removePlayerRank(@NotNull RPlayerRank playerRank) {
        Objects.requireNonNull(playerRank);
        if (playerRanks.remove(playerRank)) {
            playerRank.setRdqPlayer(null);
        }
    }

    public Optional<RPlayerRank> getPlayerRankForTree(@NotNull String rankTreeIdentifier) {
        return playerRanks.stream()
            .filter(rank -> rank.belongsToRankTree(rankTreeIdentifier))
            .findFirst();
    }

    public Optional<RPlayerRank> getActivePlayerRank() {
        return playerRanks.stream()
            .filter(RPlayerRank::isActive)
            .findFirst();
    }

    public boolean hasAnyRanks() { return !playerRanks.isEmpty(); }
    public boolean hasRankInTree(@NotNull String rankTreeIdentifier) {
        return getPlayerRankForTree(rankTreeIdentifier).isPresent();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RDQPlayer other)) return false;
        return uniqueId.equals(other.uniqueId);
    }

    @Override
    public int hashCode() {
        return uniqueId.hashCode();
    }

    @Override
    public String toString() {
        return "RDQPlayer[id=%d, uuid=%s, name=%s, ranks=%d]"
            .formatted(getId(), uniqueId, playerName, playerRanks.size());
    }
}