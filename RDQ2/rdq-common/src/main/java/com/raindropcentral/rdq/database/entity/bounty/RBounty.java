package com.raindropcentral.rdq.database.entity.bounty;

import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(
    name = "r_bounty",
    indexes = {
        @Index(name = "idx_rbounty_target", columnList = "target_unique_id"),
        @Index(name = "idx_rbounty_commissioner", columnList = "commissioner_unique_id"),
        @Index(name = "idx_rbounty_active", columnList = "active"),
        @Index(name = "idx_rbounty_expires", columnList = "expires_at")
    }
)
public class RBounty extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "target_unique_id", unique = true, nullable = false)
    private UUID targetUniqueId;

    @Column(name = "commissioner_unique_id", nullable = false)
    private UUID commissionerUniqueId;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "claimed_by")
    private UUID claimedBy;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "total_estimated_value", nullable = false)
    private double totalEstimatedValue;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "bounty_id")
    private List<BountyReward> rewards = new ArrayList<>();

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

    public RBounty(@NotNull UUID targetUniqueId, @NotNull UUID commissionerUniqueId) {
        this.targetUniqueId = targetUniqueId;
        this.commissionerUniqueId = commissionerUniqueId;
        this.expiresAt = LocalDateTime.now().plusDays(7); // Default 7 day expiration
    }

    public RBounty(@NotNull UUID targetUniqueId, @NotNull UUID commissionerUniqueId, 
                   @NotNull Set<RewardItem> rewardItems, @NotNull Map<String, Double> rewardCurrencies) {
        this(targetUniqueId, commissionerUniqueId);
        this.rewardItems = new HashSet<>(rewardItems);
        this.rewardCurrencies = new HashMap<>(rewardCurrencies);
    }

    public @NotNull UUID getTargetUniqueId() { 
        return targetUniqueId; 
    }
    
    public @NotNull UUID getCommissionerUniqueId() { 
        return commissionerUniqueId; 
    }
    
    public @NotNull Optional<LocalDateTime> getExpiresAt() { 
        return Optional.ofNullable(expiresAt); 
    }
    
    public boolean isActive() { 
        return active; 
    }
    
    public @NotNull Optional<UUID> getClaimedBy() { 
        return Optional.ofNullable(claimedBy); 
    }
    
    public @NotNull Optional<LocalDateTime> getClaimedAt() { 
        return Optional.ofNullable(claimedAt); 
    }
    
    public double getTotalEstimatedValue() { 
        return totalEstimatedValue; 
    }
    
    public @NotNull List<BountyReward> getRewards() { 
        return Collections.unmodifiableList(rewards); 
    }
    
    public @NotNull Set<RewardItem> getRewardItems() { 
        return Collections.unmodifiableSet(rewardItems); 
    }
    
    public @NotNull Set<String> getRewardHistory() { 
        return Collections.unmodifiableSet(rewardHistory); 
    }
    
    public @NotNull Map<String, Double> getRewardCurrencies() { 
        return Collections.unmodifiableMap(rewardCurrencies); 
    }
    
    public void setTotalEstimatedValue(double totalEstimatedValue) { 
        this.totalEstimatedValue = Math.max(0.0, totalEstimatedValue); 
    }
    
    public void setExpiresAt(@NotNull Optional<LocalDateTime> expiresAt) { 
        this.expiresAt = expiresAt.orElse(null); 
    }
    
    public void addReward(@NotNull BountyReward reward) { 
        Objects.requireNonNull(reward, "reward cannot be null");
        rewards.add(reward); 
    }
    
    public void addRewardItem(@NotNull RewardItem item) { 
        Objects.requireNonNull(item, "item cannot be null");
        rewardItems.add(item); 
    }
    
    public void addRewardCurrency(@NotNull String currencyName, double amount) { 
        Objects.requireNonNull(currencyName, "currencyName cannot be null");
        if (amount > 0) {
            rewardCurrencies.merge(currencyName, amount, Double::sum); 
        }
    }
    
    public boolean isClaimed() { 
        return claimedBy != null; 
    }
    
    public boolean isExpired() { 
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt); 
    }
    
    /**
     * Marks this bounty as claimed by the specified hunter.
     * This is an atomic state transition from active to claimed.
     *
     * @param claimerUniqueId the UUID of the player claiming the bounty
     * @throws IllegalStateException if the bounty is already claimed or expired
     */
    public void claim(@NotNull UUID claimerUniqueId) {
        Objects.requireNonNull(claimerUniqueId, "claimerUniqueId cannot be null");
        if (isClaimed()) {
            throw new IllegalStateException("Bounty is already claimed");
        }
        if (isExpired()) {
            throw new IllegalStateException("Cannot claim expired bounty");
        }
        this.claimedBy = claimerUniqueId;
        this.claimedAt = LocalDateTime.now();
        this.active = false;
    }
    
    /**
     * Marks this bounty as expired.
     * This is an atomic state transition from active to expired.
     *
     * @throws IllegalStateException if the bounty is already claimed
     */
    public void expire() {
        if (isClaimed()) {
            throw new IllegalStateException("Cannot expire a claimed bounty");
        }
        this.active = false;
    }
    
    // Compatibility method that returns a simple player-like object
    public PlayerInfo getPlayer() {
        return new PlayerInfo(targetUniqueId, "Unknown"); // Name will be resolved by views
    }
    
    // Simple inner class for compatibility
    public static class PlayerInfo {
        private final UUID uniqueId;
        private final String playerName;
        
        public PlayerInfo(UUID uniqueId, String playerName) {
            this.uniqueId = uniqueId;
            this.playerName = playerName;
        }
        
        public UUID getUniqueId() { return uniqueId; }
        public String getPlayerName() { return playerName; }
    }
}