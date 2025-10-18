package com.raindropcentral.rdq.database.entity.bounty;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.*;

@Entity
@Table(name = "r_bounty", uniqueConstraints = @UniqueConstraint(columnNames = {"player_id"}))
public class RBounty extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id", nullable = false, unique = true)
    private RDQPlayer player;

    @Column(name = "commissioner", nullable = false)
    private UUID commissioner;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rbounty_reward_items", joinColumns = @JoinColumn(name = "rbounty_id"))
    private Set<RewardItem> rewardItems = new HashSet<>();

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "r_reward_bounty_history", joinColumns = @JoinColumn(name = "entry_id"))
    @Column(name = "reward_history")
    private Set<String> rewardHistory = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "rbounty_reward_currencies", joinColumns = @JoinColumn(name = "rbounty_id"))
    @MapKeyColumn(name = "currency_name")
    @Column(name = "amount")
    private Map<String, Double> rewardCurrencies = new HashMap<>();

    protected RBounty() {}

    public RBounty(final @NotNull RDQPlayer player, final @NotNull Player commissioner) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(commissioner, "commissioner cannot be null");
        this.commissioner = commissioner.getUniqueId();
    }

    public RBounty(final @NotNull RDQPlayer player, final @NotNull Player commissioner,
                   final @NotNull Set<RewardItem> rewardItems, final @NotNull Map<String, Double> rewardCurrencies) {
        this(player, commissioner);
        this.rewardItems = new HashSet<>(Objects.requireNonNull(rewardItems, "rewardItems cannot be null"));
        this.rewardCurrencies = new HashMap<>(Objects.requireNonNull(rewardCurrencies, "rewardCurrencies cannot be null"));
    }

    public @NotNull RDQPlayer getPlayer() {
        return this.player;
    }

    public void setPlayer(final @NotNull RDQPlayer player) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    public void setCommissioner(UUID commissioner) {
        this.commissioner = commissioner;
    }

    public void setRewardItems(Set<RewardItem> rewardItems) {
        this.rewardItems = rewardItems;
    }

    public void setRewardHistory(Set<String> rewardHistory) {
        this.rewardHistory = rewardHistory;
    }

    public void setRewardCurrencies(Map<String, Double> rewardCurrencies) {
        this.rewardCurrencies = rewardCurrencies;
    }

    public @NotNull UUID getCommissioner() {
        return this.commissioner;
    }

    public @NotNull Set<RewardItem> getRewardItems() {
        return Collections.unmodifiableSet(this.rewardItems);
    }

    public void addRewardItem(final @NotNull RewardItem item) {
        this.rewardItems.add(Objects.requireNonNull(item, "item cannot be null"));
    }

    public @NotNull Set<String> getRewardHistory() {
        return Collections.unmodifiableSet(this.rewardHistory);
    }

    public void addRewardHistoryEntry(final @NotNull String entry) {
        this.rewardHistory.add(Objects.requireNonNull(entry, "entry cannot be null"));
    }

    public @NotNull Map<String, Double> getRewardCurrencies() {
        return Collections.unmodifiableMap(this.rewardCurrencies);
    }

    public void addRewardCurrency(final @NotNull String currencyName, final double amount) {
        Objects.requireNonNull(currencyName, "currencyName cannot be null");
        this.rewardCurrencies.merge(currencyName, amount, Double::sum);
    }

    public double getTotalRewardValue() {
        return this.rewardCurrencies.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RBounty other)) return false;
        return Objects.equals(this.player, other.player);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.player);
    }

    @Override
    public String toString() {
        return "RBounty[id=%d, player=%s, commissioner=%s, items=%d, currencies=%d]"
                .formatted(getId(), player != null ? player.getPlayerName() : "null",
                        commissioner, rewardItems.size(), rewardCurrencies.size());
    }
}