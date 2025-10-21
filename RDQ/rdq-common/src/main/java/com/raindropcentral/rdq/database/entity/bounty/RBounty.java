package com.raindropcentral.rdq.database.entity.bounty;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.*;

/**
 * Represents a bounty placed on a {@link RDQPlayer} including the commissioner, item rewards, and
 * currency payouts.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
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

    /**
     * Creates a bounty instance for JPA.
     */
    protected RBounty() {}

    /**
     * Creates a bounty for the provided player and commissioner.
     *
     * @param player        the player who the bounty is targeting
     * @param commissioner  the online player placing the bounty
     */
    public RBounty(final @NotNull RDQPlayer player, final @NotNull Player commissioner) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(commissioner, "commissioner cannot be null");
        this.commissioner = commissioner.getUniqueId();
    }

    /**
     * Creates a bounty with initial reward items and currency rewards.
     *
     * @param player           the player who the bounty is targeting
     * @param commissioner     the online player placing the bounty
     * @param rewardItems      the collection of items promised as a reward
     * @param rewardCurrencies the currency rewards associated with the bounty
     */
    public RBounty(final @NotNull RDQPlayer player, final @NotNull Player commissioner,
                   final @NotNull Set<RewardItem> rewardItems, final @NotNull Map<String, Double> rewardCurrencies) {
        this(player, commissioner);
        this.rewardItems = new HashSet<>(Objects.requireNonNull(rewardItems, "rewardItems cannot be null"));
        this.rewardCurrencies = new HashMap<>(Objects.requireNonNull(rewardCurrencies, "rewardCurrencies cannot be null"));
    }

    /**
     * Gets the player that is the target of the bounty.
     *
     * @return the player associated with this bounty
     */
    public @NotNull RDQPlayer getPlayer() {
        return this.player;
    }

    /**
     * Updates the player associated with this bounty.
     *
     * @param player the new player for this bounty
     */
    public void setPlayer(final @NotNull RDQPlayer player) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    /**
     * Updates the commissioner who placed the bounty.
     *
     * @param commissioner the unique identifier of the commissioner
     */
    public void setCommissioner(UUID commissioner) {
        this.commissioner = commissioner;
    }

    /**
     * Replaces the reward items associated with this bounty.
     *
     * @param rewardItems the reward items to set
     */
    public void setRewardItems(Set<RewardItem> rewardItems) {
        this.rewardItems = rewardItems;
    }

    /**
     * Replaces the reward history entries for this bounty.
     *
     * @param rewardHistory the reward history entries to set
     */
    public void setRewardHistory(Set<String> rewardHistory) {
        this.rewardHistory = rewardHistory;
    }

    /**
     * Replaces the currency rewards associated with this bounty.
     *
     * @param rewardCurrencies the map of currency name to reward amount
     */
    public void setRewardCurrencies(Map<String, Double> rewardCurrencies) {
        this.rewardCurrencies = rewardCurrencies;
    }

    /**
     * Gets the commissioner who placed the bounty.
     *
     * @return the unique identifier of the commissioner
     */
    public @NotNull UUID getCommissioner() {
        return this.commissioner;
    }

    /**
     * Retrieves the reward items associated with the bounty.
     *
     * @return an unmodifiable view of the reward items
     */
    public @NotNull Set<RewardItem> getRewardItems() {
        return Collections.unmodifiableSet(this.rewardItems);
    }

    /**
     * Adds a new reward item to the bounty.
     *
     * @param item the reward item to add
     */
    public void addRewardItem(final @NotNull RewardItem item) {
        this.rewardItems.add(Objects.requireNonNull(item, "item cannot be null"));
    }

    /**
     * Retrieves the historical reward entries distributed for this bounty.
     *
     * @return an unmodifiable view of the reward history
     */
    public @NotNull Set<String> getRewardHistory() {
        return Collections.unmodifiableSet(this.rewardHistory);
    }

    /**
     * Adds a new entry to the reward history log.
     *
     * @param entry the history entry describing a reward payout
     */
    public void addRewardHistoryEntry(final @NotNull String entry) {
        this.rewardHistory.add(Objects.requireNonNull(entry, "entry cannot be null"));
    }

    /**
     * Retrieves the currency rewards associated with this bounty.
     *
     * @return an unmodifiable map of currency name to reward amount
     */
    public @NotNull Map<String, Double> getRewardCurrencies() {
        return Collections.unmodifiableMap(this.rewardCurrencies);
    }

    /**
     * Adds or increases a currency reward entry.
     *
     * @param currencyName the currency to modify
     * @param amount       the amount to add to the reward
     */
    public void addRewardCurrency(final @NotNull String currencyName, final double amount) {
        Objects.requireNonNull(currencyName, "currencyName cannot be null");
        this.rewardCurrencies.merge(currencyName, amount, Double::sum);
    }

    /**
     * Calculates the total currency value associated with the bounty.
     *
     * @return the sum of all currency rewards
     */
    public double getTotalRewardValue() {
        return this.rewardCurrencies.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    /**
     * Compares this bounty to another object based on the associated player.
     *
     * @param obj the object to compare against
     * @return {@code true} if the other object refers to the same player, otherwise {@code false}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RBounty other)) return false;
        return Objects.equals(this.player, other.player);
    }

    /**
     * Generates a hash code based on the associated player.
     *
     * @return the hash code for this bounty
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.player);
    }

    /**
     * Returns a concise textual representation of the bounty for logging purposes.
     *
     * @return the string representation of the bounty
     */
    @Override
    public String toString() {
        return "RBounty[id=%d, player=%s, commissioner=%s, items=%d, currencies=%d]"
                .formatted(getId(), player != null ? player.getPlayerName() : "null",
                        commissioner, rewardItems.size(), rewardCurrencies.size());
    }
}