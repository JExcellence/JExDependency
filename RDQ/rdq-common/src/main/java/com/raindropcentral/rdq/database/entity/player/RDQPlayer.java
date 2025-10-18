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

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "player")
    private RBounty bounty;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "player")
    private List<RPlayerRank> playerRanks = new ArrayList<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "player")
    private List<RPlayerRankPath> playerRankPaths = new ArrayList<>();

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<RPlayerPerk> playerPerks = new HashSet<>();

    protected RDQPlayer() {}

    public RDQPlayer(final @NotNull UUID uniqueId, final @NotNull String playerName) {
        this.uniqueId = Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        this.playerName = Objects.requireNonNull(playerName, "playerName cannot be null");
    }

    public RDQPlayer(final @NotNull Player player) {
        this(player.getUniqueId(), player.getName());
    }

    public @NotNull UUID getUniqueId() {
        return this.uniqueId;
    }

    public void setUniqueId(final @NotNull UUID uniqueId) {
        this.uniqueId = Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
    }

    public @NotNull String getPlayerName() {
        return this.playerName;
    }

    public void setPlayerName(final @NotNull String playerName) {
        this.playerName = Objects.requireNonNull(playerName, "playerName cannot be null");
    }

    public @Nullable RBounty getBounty() {
        return this.bounty;
    }

    public void setBounty(final @Nullable RBounty bounty) {
        this.bounty = bounty;
        if (bounty != null) {
            bounty.setPlayer(this);
        }
    }

    public @NotNull List<RPlayerRank> getPlayerRanks() {
        return Collections.unmodifiableList(this.playerRanks);
    }

    public void setPlayerRanks(final @NotNull List<RPlayerRank> playerRanks) {
        this.playerRanks.clear();
        if (playerRanks != null) {
            this.playerRanks.addAll(playerRanks);
        }
    }

    public @NotNull List<RPlayerRankPath> getPlayerRankPaths() {
        return Collections.unmodifiableList(this.playerRankPaths);
    }

    public void setPlayerRankPaths(final @NotNull List<RPlayerRankPath> playerRankPaths) {
        this.playerRankPaths.clear();
        if (playerRankPaths != null) {
            this.playerRankPaths.addAll(playerRankPaths);
        }
    }

    public @NotNull Set<RPlayerPerk> getPlayerPerks() {
        return Collections.unmodifiableSet(this.playerPerks);
    }

    public void addPlayerRank(final @NotNull RPlayerRank playerRank) {
        Objects.requireNonNull(playerRank, "playerRank cannot be null");
        if (!this.playerRanks.contains(playerRank)) {
            this.playerRanks.add(playerRank);
            playerRank.setRdqPlayer(this);
        }
    }

    public void removePlayerRank(final @NotNull RPlayerRank playerRank) {
        Objects.requireNonNull(playerRank, "playerRank cannot be null");
        if (this.playerRanks.remove(playerRank)) {
            playerRank.setRdqPlayer(null);
        }
    }

    public @NotNull Optional<RPlayerRank> getPlayerRankForTree(final @NotNull String rankTreeIdentifier) {
        Objects.requireNonNull(rankTreeIdentifier, "rankTreeIdentifier cannot be null");
        return this.playerRanks.stream()
                .filter(rank -> rank.belongsToRankTree(rankTreeIdentifier))
                .findFirst();
    }

    public @NotNull Optional<RPlayerRank> getActivePlayerRank() {
        return this.playerRanks.stream()
                .filter(RPlayerRank::isActive)
                .findFirst();
    }

    public boolean hasAnyRanks() {
        return !this.playerRanks.isEmpty();
    }

    public boolean hasRankInTree(final @NotNull String rankTreeIdentifier) {
        Objects.requireNonNull(rankTreeIdentifier, "rankTreeIdentifier cannot be null");
        return getPlayerRankForTree(rankTreeIdentifier).isPresent();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RDQPlayer other)) return false;
        return this.uniqueId.equals(other.uniqueId);
    }

    @Override
    public int hashCode() {
        return this.uniqueId.hashCode();
    }

    @Override
    public String toString() {
        return "RDQPlayer[id=%d, uuid=%s, name=%s, ranks=%d]"
                .formatted(getId(), uniqueId, playerName, playerRanks.size());
    }
}