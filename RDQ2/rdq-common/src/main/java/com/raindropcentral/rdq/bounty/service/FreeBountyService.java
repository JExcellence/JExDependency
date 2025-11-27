package com.raindropcentral.rdq.bounty.service;

import com.raindropcentral.rdq.bounty.dto.*;
import com.raindropcentral.rdq.bounty.exception.*;
import com.raindropcentral.rdq.bounty.type.BountyStatus;
import com.raindropcentral.rdq.bounty.type.ClaimMode;
import com.raindropcentral.rdq.bounty.type.HunterSortOrder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Free edition implementation of BountyService.
 * <p>
 * Provides in-memory storage with a limit of one active bounty per player.
 * Supports static pre-configured bounties loaded from configuration files.
 * All operations use in-memory data structures for fast access without database overhead.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 2.0.0
 */
public final class FreeBountyService implements BountyService {
    
    // In-memory storage using ConcurrentHashMap for thread-safety
    private final ConcurrentHashMap<Long, Bounty> bounties = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, InMemoryHunterStats> hunterStats = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    // Configuration values
    private final int maxRewardItems;
    private final int expiryDays;
    private final File configFile;
    
    // Static bounties loaded from configuration
    private final List<StaticBountyConfig> staticBounties = new ArrayList<>();
    
    /**
     * Creates a new FreeBountyService with default configuration.
     *
     * @param configFile the bounty configuration file
     */
    public FreeBountyService(@NotNull File configFile) {
        this(configFile, 27, 7); // Default: 27 items (3 rows), 7 days expiry
    }
    
    /**
     * Creates a new FreeBountyService with custom configuration.
     *
     * @param configFile the bounty configuration file
     * @param maxRewardItems maximum reward items per bounty
     * @param expiryDays number of days until bounty expires
     */
    public FreeBountyService(
            @NotNull File configFile,
            int maxRewardItems,
            int expiryDays
    ) {
        this.configFile = Objects.requireNonNull(configFile, "configFile cannot be null");
        this.maxRewardItems = maxRewardItems;
        this.expiryDays = expiryDays;
        
        // Load static bounties from configuration
        loadStaticBounties();
    }
    
    // ========== Edition Capabilities ==========
    
    @Override
    public boolean isPremium() {
        return false;
    }
    
    @Override
    public int getMaxBountiesPerPlayer() {
        return 3; // Free edition limit: 3 total active bounties globally
    }
    
    @Override
    public int getMaxRewardItems() {
        return maxRewardItems;
    }
    
    @Override
    public boolean canCreateBounty(@NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        
        // Free edition: check global limit of 3 total active bounties
        long activeBountyCount = bounties.values().stream()
                .filter(Bounty::isActive)
                .count();
        
        return activeBountyCount < 3; // Allow creation if less than 3 active bounties
    }
    
    // ========== Query Operations ==========
    
    @Override
    public @NotNull CompletableFuture<List<Bounty>> getAllBounties(int page, int pageSize) {
        return CompletableFuture.supplyAsync(() -> {
            // Filter active bounties and apply pagination
            List<Bounty> activeBounties = bounties.values().stream()
                    .filter(Bounty::isActive)
                    .sorted(Comparator.comparing(Bounty::createdAt).reversed())
                    .skip((long) page * pageSize)
                    .limit(pageSize)
                    .collect(Collectors.toList());
            
            return activeBounties;
        });
    }
    
    @Override
    public @NotNull CompletableFuture<Optional<Bounty>> getBountyByTarget(@NotNull UUID targetUuid) {
        Objects.requireNonNull(targetUuid, "targetUuid cannot be null");
        
        return CompletableFuture.supplyAsync(() -> 
            bounties.values().stream()
                    .filter(b -> b.targetUuid().equals(targetUuid))
                    .filter(Bounty::isActive)
                    .findFirst()
        );
    }
    
    @Override
    public @NotNull CompletableFuture<List<Bounty>> getBountiesByCommissioner(@NotNull UUID commissionerUuid) {
        Objects.requireNonNull(commissionerUuid, "commissionerUuid cannot be null");
        
        return CompletableFuture.supplyAsync(() -> 
            bounties.values().stream()
                    .filter(b -> b.commissionerUuid().equals(commissionerUuid))
                    .sorted(Comparator.comparing(Bounty::createdAt).reversed())
                    .collect(Collectors.toList())
        );
    }
    
    @Override
    public @NotNull CompletableFuture<Integer> getTotalBountyCount() {
        return CompletableFuture.supplyAsync(() -> 
            (int) bounties.values().stream()
                    .filter(Bounty::isActive)
                    .count()
        );
    }
    
    // ========== Static Bounty Loading ==========
    
    /**
     * Loads static bounty configurations from the bounty.yml file.
     * Static bounties are pre-configured bounties that can be used in free edition.
     */
    private void loadStaticBounties() {
        if (!configFile.exists()) {
            return; // No configuration file, skip loading
        }
        
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            ConfigurationSection staticSection = config.getConfigurationSection("static-bounties");
            
            if (staticSection == null) {
                return; // No static bounties configured
            }
            
            for (String key : staticSection.getKeys(false)) {
                ConfigurationSection bountySection = staticSection.getConfigurationSection(key);
                if (bountySection == null) {
                    continue;
                }
                
                try {
                    StaticBountyConfig staticBounty = parseStaticBounty(bountySection);
                    staticBounties.add(staticBounty);
                } catch (Exception e) {
                    // Log error and continue with other bounties
                    Bukkit.getLogger().warning("Failed to load static bounty '" + key + "': " + e.getMessage());
                }
            }
            
            Bukkit.getLogger().info("Loaded " + staticBounties.size() + " static bounties");
        } catch (Exception e) {
            Bukkit.getLogger().severe("Failed to load static bounties: " + e.getMessage());
        }
    }
    
    /**
     * Parses a static bounty configuration section.
     *
     * @param section the configuration section
     * @return the parsed static bounty configuration
     * @throws IllegalArgumentException if configuration is invalid
     */
    private @NotNull StaticBountyConfig parseStaticBounty(@NotNull ConfigurationSection section) {
        String targetUuidStr = section.getString("target-uuid");
        if (targetUuidStr == null) {
            throw new IllegalArgumentException("target-uuid is required");
        }
        
        UUID targetUuid;
        try {
            targetUuid = UUID.fromString(targetUuidStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid target-uuid format: " + targetUuidStr);
        }
        
        // Validate target exists (is online or has played before)
        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        if (targetPlayer == null && Bukkit.getOfflinePlayer(targetUuid).getName() == null) {
            throw new IllegalArgumentException("Target player does not exist: " + targetUuid);
        }
        
        // Parse reward items
        List<ItemStack> rewardItems = new ArrayList<>();
        if (section.contains("reward-items")) {
            @SuppressWarnings("unchecked")
            List<ItemStack> items = (List<ItemStack>) section.getList("reward-items");
            if (items != null) {
                rewardItems.addAll(items);
            }
        }
        
        // Parse reward currencies
        Map<String, Double> rewardCurrencies = new HashMap<>();
        ConfigurationSection currenciesSection = section.getConfigurationSection("reward-currencies");
        if (currenciesSection != null) {
            for (String currencyName : currenciesSection.getKeys(false)) {
                double amount = currenciesSection.getDouble(currencyName);
                rewardCurrencies.put(currencyName, amount);
            }
        }
        
        return new StaticBountyConfig(targetUuid, rewardItems, rewardCurrencies);
    }
    
    /**
     * Internal record for storing static bounty configuration.
     */
    private record StaticBountyConfig(
            @NotNull UUID targetUuid,
            @NotNull List<ItemStack> rewardItems,
            @NotNull Map<String, Double> rewardCurrencies
    ) {}
    
    /**
     * Internal class for storing hunter statistics in memory.
     */
    private static class InMemoryHunterStats {
        private final UUID playerUuid;
        private final String playerName;
        private int bountiesClaimed;
        private double totalRewardValue;
        private double highestBountyValue;
        private LocalDateTime lastClaimTime;
        
        public InMemoryHunterStats(@NotNull UUID playerUuid, @NotNull String playerName) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.bountiesClaimed = 0;
            this.totalRewardValue = 0.0;
            this.highestBountyValue = 0.0;
            this.lastClaimTime = null;
        }
        
        public synchronized void recordClaim(double rewardValue) {
            this.bountiesClaimed++;
            this.totalRewardValue += rewardValue;
            if (rewardValue > this.highestBountyValue) {
                this.highestBountyValue = rewardValue;
            }
            this.lastClaimTime = LocalDateTime.now();
        }
        
        public HunterStats toDto(int rank) {
            return new HunterStats(
                    playerUuid,
                    playerName,
                    bountiesClaimed,
                    totalRewardValue,
                    highestBountyValue,
                    Optional.ofNullable(lastClaimTime),
                    rank
            );
        }
    }
    
    // ========== Mutation Operations ==========
    
    @Override
    public @NotNull CompletableFuture<Bounty> createBounty(@NotNull BountyCreationRequest request) throws BountyException {
        Objects.requireNonNull(request, "request cannot be null");
        
        // Validate target is not commissioner (prevent self-targeting)
        if (request.targetUuid().equals(request.commissionerUuid())) {
            return CompletableFuture.failedFuture(
                    new SelfTargetingException(request.commissionerUuid())
            );
        }
        
        // Free edition: enforce global limit of 3 total active bounties
        long activeBountyCount = bounties.values().stream()
                .filter(Bounty::isActive)
                .count();
        
        if (activeBountyCount >= 3) {
            return CompletableFuture.failedFuture(
                    new BountyAlreadyExistsException("Maximum of 3 active bounties reached. Free edition limit.")
            );
        }
        
        // Check if target already has an active bounty
        Optional<Bounty> existingBounty = bounties.values().stream()
                .filter(b -> b.targetUuid().equals(request.targetUuid()))
                .filter(Bounty::isActive)
                .findFirst();
        
        if (existingBounty.isPresent()) {
            return CompletableFuture.failedFuture(
                    new BountyAlreadyExistsException("Target already has an active bounty")
            );
        }
        
        return CompletableFuture.supplyAsync(() -> {
            
            // Calculate total estimated value from rewards
            double totalValue = calculateTotalValue(request);
            
            // Generate unique ID
            Long bountyId = idGenerator.getAndIncrement();
            
            // Resolve player names
            String targetName = resolvePlayerName(request.targetUuid());
            String commissionerName = resolvePlayerName(request.commissionerUuid());
            
            // Set expiration time
            LocalDateTime expiresAt = request.customExpiration()
                    .orElse(LocalDateTime.now().plusDays(expiryDays));
            
            // Create bounty DTO
            Bounty bounty = new Bounty(
                    bountyId,
                    request.targetUuid(),
                    targetName,
                    request.commissionerUuid(),
                    commissionerName,
                    Set.copyOf(request.rewardItems()),
                    Map.copyOf(request.rewardCurrencies()),
                    totalValue,
                    LocalDateTime.now(),
                    expiresAt,
                    BountyStatus.ACTIVE,
                    Optional.empty()
            );
            
            // Store bounty in memory
            bounties.put(bountyId, bounty);
            
            return bounty;
        });
    }
    
    @Override
    public @NotNull CompletableFuture<Boolean> deleteBounty(@NotNull Long bountyId) {
        Objects.requireNonNull(bountyId, "bountyId cannot be null");
        
        return CompletableFuture.supplyAsync(() -> {
            Bounty bounty = bounties.get(bountyId);
            if (bounty == null) {
                return false;
            }
            
            // Mark bounty as expired by creating a new bounty with EXPIRED status
            Bounty expiredBounty = new Bounty(
                    bounty.id(),
                    bounty.targetUuid(),
                    bounty.targetName(),
                    bounty.commissionerUuid(),
                    bounty.commissionerName(),
                    bounty.rewardItems(),
                    bounty.rewardCurrencies(),
                    bounty.totalEstimatedValue(),
                    bounty.createdAt(),
                    bounty.expiresAt(),
                    BountyStatus.EXPIRED,
                    bounty.claimInfo()
            );
            
            bounties.put(bountyId, expiredBounty);
            
            return true;
        });
    }
    
    @Override
    public @NotNull CompletableFuture<Bounty> claimBounty(@NotNull Long bountyId, @NotNull UUID hunterUuid) throws BountyException {
        Objects.requireNonNull(bountyId, "bountyId cannot be null");
        Objects.requireNonNull(hunterUuid, "hunterUuid cannot be null");
        
        Bounty bounty = bounties.get(bountyId);
        
        if (bounty == null) {
            return CompletableFuture.failedFuture(
                    new BountyNotFoundException(bountyId)
            );
        }
        
        // Validate bounty can be claimed
        if (bounty.status() == BountyStatus.CLAIMED) {
            return CompletableFuture.failedFuture(
                    new BountyAlreadyClaimedException("Bounty has already been claimed")
            );
        }
        
        if (bounty.isExpired()) {
            return CompletableFuture.failedFuture(
                    new BountyExpiredException("Bounty has expired")
            );
        }
        
        return CompletableFuture.supplyAsync(() -> {
            
            // Resolve hunter name
            String hunterName = resolvePlayerName(hunterUuid);
            
            // Create claim info
            ClaimInfo claimInfo = new ClaimInfo(
                    hunterUuid,
                    hunterName,
                    LocalDateTime.now(),
                    ClaimMode.LAST_HIT // Default claim mode
            );
            
            // Create claimed bounty
            Bounty claimedBounty = new Bounty(
                    bounty.id(),
                    bounty.targetUuid(),
                    bounty.targetName(),
                    bounty.commissionerUuid(),
                    bounty.commissionerName(),
                    bounty.rewardItems(),
                    bounty.rewardCurrencies(),
                    bounty.totalEstimatedValue(),
                    bounty.createdAt(),
                    bounty.expiresAt(),
                    BountyStatus.CLAIMED,
                    Optional.of(claimInfo)
            );
            
            // Update bounty in storage
            bounties.put(bountyId, claimedBounty);
            
            // Update hunter statistics
            updateHunterStatistics(hunterUuid, hunterName, bounty.totalEstimatedValue());
            
            return claimedBounty;
        });
    }
    
    @Override
    public @NotNull CompletableFuture<Void> expireBounty(@NotNull Long bountyId) {
        Objects.requireNonNull(bountyId, "bountyId cannot be null");
        
        return CompletableFuture.supplyAsync(() -> {
            Bounty bounty = bounties.get(bountyId);
            
            if (bounty == null) {
                return null; // Bounty not found, consider it already expired
            }
            
            // Cannot expire a claimed bounty
            if (bounty.status() == BountyStatus.CLAIMED) {
                return null;
            }
            
            // Create expired bounty
            Bounty expiredBounty = new Bounty(
                    bounty.id(),
                    bounty.targetUuid(),
                    bounty.targetName(),
                    bounty.commissionerUuid(),
                    bounty.commissionerName(),
                    bounty.rewardItems(),
                    bounty.rewardCurrencies(),
                    bounty.totalEstimatedValue(),
                    bounty.createdAt(),
                    bounty.expiresAt(),
                    BountyStatus.EXPIRED,
                    bounty.claimInfo()
            );
            
            bounties.put(bountyId, expiredBounty);
            
            return null;
        });
    }
    
    // ========== Hunter Statistics ==========
    
    @Override
    public @NotNull CompletableFuture<Optional<HunterStats>> getHunterStats(@NotNull UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        
        return CompletableFuture.supplyAsync(() -> {
            InMemoryHunterStats stats = hunterStats.get(playerUuid);
            
            if (stats == null) {
                return Optional.empty();
            }
            
            // Calculate rank
            int rank = calculateRank(playerUuid);
            
            return Optional.of(stats.toDto(rank));
        });
    }
    
    @Override
    public @NotNull CompletableFuture<List<HunterStats>> getTopHunters(int limit, @NotNull HunterSortOrder sortOrder) {
        Objects.requireNonNull(sortOrder, "sortOrder cannot be null");
        
        return CompletableFuture.supplyAsync(() -> {
            Comparator<InMemoryHunterStats> comparator = switch (sortOrder) {
                case BOUNTIES_CLAIMED -> Comparator.comparingInt(s -> -s.bountiesClaimed);
                case TOTAL_REWARD_VALUE -> Comparator.comparingDouble(s -> -s.totalRewardValue);
                case HIGHEST_BOUNTY_VALUE -> Comparator.comparingDouble(s -> -s.highestBountyValue);
                case RECENT_CLAIMS -> Comparator.comparing(
                        s -> s.lastClaimTime,
                        Comparator.nullsLast(Comparator.reverseOrder())
                );
            };
            
            List<InMemoryHunterStats> sortedStats = hunterStats.values().stream()
                    .sorted(comparator)
                    .limit(limit)
                    .toList();
            
            // Convert to DTOs with ranking
            List<HunterStats> result = new ArrayList<>();
            int rank = 1;
            for (InMemoryHunterStats stats : sortedStats) {
                result.add(stats.toDto(rank++));
            }
            
            return result;
        });
    }
    
    @Override
    public @NotNull CompletableFuture<Integer> getHunterRank(@NotNull UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        
        return CompletableFuture.supplyAsync(() -> calculateRank(playerUuid));
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Resolves a player name from UUID.
     * Checks online players first, then falls back to offline player lookup.
     *
     * @param uuid the player UUID
     * @return the player name or "Unknown"
     */
    private @NotNull String resolvePlayerName(@NotNull UUID uuid) {
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }
        
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : "Unknown";
    }
    
    /**
     * Calculates the total estimated value of a bounty from its rewards.
     *
     * @param request the bounty creation request
     * @return the total estimated value
     */
    private double calculateTotalValue(@NotNull BountyCreationRequest request) {
        double itemValue = request.rewardItems().stream()
                .mapToDouble(RewardItem::estimatedValue)
                .sum();
        
        double currencyValue = request.rewardCurrencies().values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        
        return itemValue + currencyValue;
    }
    
    /**
     * Updates hunter statistics when a bounty is claimed.
     * Creates new statistics if this is the hunter's first claim.
     *
     * @param hunterUuid the UUID of the hunter
     * @param hunterName the name of the hunter
     * @param rewardValue the value of the bounty reward
     */
    private void updateHunterStatistics(
            @NotNull UUID hunterUuid,
            @NotNull String hunterName,
            double rewardValue
    ) {
        hunterStats.compute(hunterUuid, (uuid, stats) -> {
            if (stats == null) {
                stats = new InMemoryHunterStats(hunterUuid, hunterName);
            }
            stats.recordClaim(rewardValue);
            return stats;
        });
    }
    
    /**
     * Calculates the rank of a hunter based on bounties claimed.
     *
     * @param playerUuid the UUID of the player
     * @return the player's rank (1-indexed), or Integer.MAX_VALUE if not ranked
     */
    private int calculateRank(@NotNull UUID playerUuid) {
        InMemoryHunterStats playerStats = hunterStats.get(playerUuid);
        
        if (playerStats == null) {
            return Integer.MAX_VALUE; // Not ranked
        }
        
        // Count how many hunters have more bounties claimed
        long betterHunters = hunterStats.values().stream()
                .filter(s -> s.bountiesClaimed > playerStats.bountiesClaimed)
                .count();
        
        return (int) betterHunters + 1;
    }
}
