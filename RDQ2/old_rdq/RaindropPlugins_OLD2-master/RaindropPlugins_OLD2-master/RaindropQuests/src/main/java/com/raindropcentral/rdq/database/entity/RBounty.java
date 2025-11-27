package com.raindropcentral.rdq.database.entity;

import com.raindropcentral.rdq.view.bounty.RewardItem;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Entity representing a bounty placed on a player in the RaindropQuests system.
 * <p>
 * This entity links a {@link RDQPlayer} to a bounty, tracks the commissioner (the player who placed the bounty),
 * and manages the rewards (items and currencies) as well as the reward history.
 * </p>
 * <p>
 * Mapped to the {@code r_bounty} table in the database.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@Entity
@Table(
    name = "r_bounty",
    uniqueConstraints = @UniqueConstraint(columnNames = {"player_id"})
)
public class RBounty extends AbstractEntity {
    
    /**
     * The player who is the target of this bounty.
     */
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(
        name = "player_id",
        nullable = false,
        unique = true
    )
    private RDQPlayer player;
    
    /**
     * The UUID of the player who commissioned (placed) the bounty.
     */
    @Column(
        name = "commissioner",
        nullable = false
    )
    private UUID commissioner;
    
    /**
     * The set of item rewards associated with this bounty.
     * <p>
     * Each {@link RewardItem} represents an item contributed as a reward for completing the bounty.
     * </p>
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "rbounty_reward_items",
        joinColumns = @JoinColumn(name = "rbounty_id")
    )
    private Set<RewardItem> rewardItems = new HashSet<>();
    
    /**
     * The set of reward history entries for this bounty.
     * <p>
     * Each entry is a string describing a reward event (e.g., item added, currency contributed).
     * </p>
     */
    @ElementCollection(
        targetClass = String.class,
        fetch = FetchType.EAGER
    )
    @CollectionTable(
        name = "r_reward_bounty_history",
        joinColumns = @JoinColumn(name = "entry_id")
    )
    @Column(name = "reward_history")
    private Set<String> rewardHistory = new HashSet<>();
    
    /**
     * The map of currency rewards for this bounty, keyed by currency name.
     * <p>
     * Each entry maps a currency name to the amount offered as a reward.
     * </p>
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(
        name = "rbounty_reward_currencies",
        joinColumns = @JoinColumn(name = "rbounty_id")
    )
    @MapKeyColumn(name = "currency_name")
    @Column(name = "amount")
    private Map<String, Double> rewardCurrencies = new HashMap<>();
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected RBounty() {
    }
    
    /**
     * Constructs a new {@code RBounty} for the given player and commissioner.
     *
     * @param player    the player who is the target of the bounty
     * @param commissioner the player who commissioned (placed) the bounty
     */
    public RBounty(
        final @NotNull RDQPlayer player,
        final @NotNull Player commissioner
    ) {
        this.player = player;
        this.commissioner = commissioner.getUniqueId();
    }
    
    /**
     * Constructs a new {@code RBounty} for the given player, commissioner, item rewards, and currency rewards.
     *
     * @param player       the player who is the target of the bounty; must not be {@code null}
     * @param commissioner    the player who commissioned (placed) the bounty; must not be {@code null}
     * @param rewardItems     the set of item rewards associated with this bounty; must not be {@code null}
     * @param rewardCurrencies the map of currency rewards for this bounty, keyed by currency name; must not be {@code null}
     */
    public RBounty(
        final @NotNull RDQPlayer player,
        final @NotNull Player commissioner,
        final @NotNull Set<RewardItem> rewardItems,
        final @NotNull Map<String, Double> rewardCurrencies
    ) {
        this.player = player;
        this.commissioner = commissioner.getUniqueId();
        this.rewardItems = rewardItems;
        this.rewardCurrencies = rewardCurrencies;
    }
    
    /**
     * Gets the player who is the target of this bounty.
     *
     * @return the {@link RDQPlayer} associated with this bounty
     */
    public RDQPlayer getPlayer() {
        return this.player;
    }
    
    /**
     * Sets the player who is the target of this bounty.
     *
     * @param player the {@link RDQPlayer} to associate with this bounty
     */
    public void setRdqPlayer(final RDQPlayer player) {
        this.player = player;
    }
    
    /**
     * Gets the set of item rewards associated with this bounty.
     *
     * @return a set of {@link RewardItem} objects representing the item rewards
     */
    public Set<RewardItem> getRewardItems() {
        return this.rewardItems;
    }
    
    /**
     * Gets the UUID of the player who commissioned (placed) the bounty.
     *
     * @return the commissioner's UUID
     */
    public UUID getCommissioner() {
        return this.commissioner;
    }
    
}