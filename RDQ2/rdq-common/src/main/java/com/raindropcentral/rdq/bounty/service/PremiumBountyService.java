package com.raindropcentral.rdq.bounty.service;

import com.raindropcentral.rdq.bounty.dto.*;
import com.raindropcentral.rdq.bounty.exception.*;
import com.raindropcentral.rdq.bounty.type.BountyStatus;
import com.raindropcentral.rdq.bounty.type.ClaimMode;
import com.raindropcentral.rdq.bounty.type.HunterSortOrder;
import com.raindropcentral.rdq.database.entity.bounty.BountyHunterStats;
import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.repository.BountyHunterStatsRepository;
import com.raindropcentral.rdq.database.repository.RBountyRepository;
import com.raindropcentral.rdq.database.repository.RDQPlayerRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Premium edition implementation of BountyService.
 * <p>
 * Provides full database persistence with unlimited bounties per player.
 * Supports dynamic bounty creation with configurable limits and expiration.
 * All operations are asynchronous to avoid blocking the main server thread.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 2.0.0
 */
public final class PremiumBountyService implements BountyService {
    
    private final RBountyRepository bountyRepository;
    private final BountyHunterStatsRepository hunterStatsRepository;
    private final RDQPlayerRepository playerRepository;
    
    // Configuration values (will be loaded from config in task 17)
    private final int maxBountiesPerPlayer;
    private final int maxRewardItems;
    private final int expiryDays;
    
    /**
     * Creates a new PremiumBountyService with repository dependencies.
     *
     * @param bountyRepository the bounty repository
     * @param hunterStatsRepository the hunter statistics repository
     * @param playerRepository the player repository
     */
    public PremiumBountyService(
            @NotNull RBountyRepository bountyRepository,
            @NotNull BountyHunterStatsRepository hunterStatsRepository,
            @NotNull RDQPlayerRepository playerRepository
    ) {
        this(bountyRepository, hunterStatsRepository, playerRepository, -1, 54, 7);
    }
    
    /**
     * Creates a new PremiumBountyService with repository dependencies and configuration.
     *
     * @param bountyRepository the bounty repository
     * @param hunterStatsRepository the hunter statistics repository
     * @param playerRepository the player repository
     * @param maxBountiesPerPlayer maximum bounties per player (-1 for unlimited)
     * @param maxRewardItems maximum reward items per bounty
     * @param expiryDays number of days until bounty expires
     */
    public PremiumBountyService(
            @NotNull RBountyRepository bountyRepository,
            @NotNull BountyHunterStatsRepository hunterStatsRepository,
            @NotNull RDQPlayerRepository playerRepository,
            int maxBountiesPerPlayer,
            int maxRewardItems,
            int expiryDays
    ) {
        this.bountyRepository = Objects.requireNonNull(bountyRepository, "bountyRepository cannot be null");
        this.hunterStatsRepository = Objects.requireNonNull(hunterStatsRepository, "hunterStatsRepository cannot be null");
        this.playerRepository = Objects.requireNonNull(playerRepository, "playerRepository cannot be null");
        this.maxBountiesPerPlayer = maxBountiesPerPlayer;
        this.maxRewardItems = maxRewardItems;
        this.expiryDays = expiryDays;
    }
    
    // ========== Edition Capabilities ==========
    
    @Override
    public boolean isPremium() {
        return true;
    }
    
    @Override
    public int getMaxBountiesPerPlayer() {
        return maxBountiesPerPlayer;
    }
    
    @Override
    public int getMaxRewardItems() {
        return maxRewardItems;
    }
    
    @Override
    public boolean canCreateBounty(@NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        
        // Premium edition: check if player has reached their bounty limit
        if (maxBountiesPerPlayer < 0) {
            return true; // Unlimited
        }
        
        // Count active bounties by this commissioner
        try {
            List<RBounty> bounties = bountyRepository.findByCommissionerAsync(player.getUniqueId()).join();
            long activeBounties = bounties.stream()
                    .filter(RBounty::isActive)
                    .count();
            return activeBounties < maxBountiesPerPlayer;
        } catch (Exception e) {
            return false;
        }
    }
    
    // ========== Query Operations ==========
    
    @Override
    public @NotNull CompletableFuture<List<Bounty>> getAllBounties(int page, int pageSize) {
        return bountyRepository.findAllActiveAsync(page, pageSize)
                .thenCompose(entities -> {
                    // Convert entities to DTOs asynchronously
                    List<CompletableFuture<Bounty>> futures = entities.stream()
                            .map(this::entityToDto)
                            .toList();
                    
                    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .thenApply(v -> futures.stream()
                                    .map(CompletableFuture::join)
                                    .collect(Collectors.toList()));
                });
    }
    
    @Override
    public @NotNull CompletableFuture<Optional<Bounty>> getBountyByTarget(@NotNull UUID targetUuid) {
        Objects.requireNonNull(targetUuid, "targetUuid cannot be null");
        
        return bountyRepository.findActiveByTargetAsync(targetUuid)
                .thenCompose(entityOpt -> {
                    if (entityOpt.isEmpty()) {
                        return CompletableFuture.completedFuture(Optional.empty());
                    }
                    
                    return entityToDto(entityOpt.get())
                            .thenApply(Optional::of);
                });
    }
    
    @Override
    public @NotNull CompletableFuture<List<Bounty>> getBountiesByCommissioner(@NotNull UUID commissionerUuid) {
        Objects.requireNonNull(commissionerUuid, "commissionerUuid cannot be null");
        
        return bountyRepository.findByCommissionerAsync(commissionerUuid)
                .thenCompose(entities -> {
                    // Convert entities to DTOs asynchronously
                    List<CompletableFuture<Bounty>> futures = entities.stream()
                            .map(this::entityToDto)
                            .toList();
                    
                    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .thenApply(v -> futures.stream()
                                    .map(CompletableFuture::join)
                                    .collect(Collectors.toList()));
                });
    }
    
    @Override
    public @NotNull CompletableFuture<Integer> getTotalBountyCount() {
        return bountyRepository.countActiveAsync();
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
        
        // Check if target already has an active bounty
        return bountyRepository.findActiveByTargetAsync(request.targetUuid())
                .thenCompose(existingBounty -> {
                    if (existingBounty.isPresent()) {
                        return CompletableFuture.failedFuture(
                                new BountyAlreadyExistsException("Target already has an active bounty")
                        );
                    }
                    
                    // Calculate total estimated value from rewards
                    double totalValue = calculateTotalValue(request);
                    
                    // TODO: Validate commissioner has sufficient currency balance
                    // This will be implemented when economy integration is added
                    
                    // Create bounty entity
                    RBounty entity = new RBounty(
                            request.targetUuid(),
                            request.commissionerUuid()
                    );
                    
                    // Add reward items (convert DTO to entity)
                    for (com.raindropcentral.rdq.bounty.dto.RewardItem dtoItem : request.rewardItems()) {
                        com.raindropcentral.rdq.database.entity.reward.RewardItem entityItem = 
                                new com.raindropcentral.rdq.database.entity.reward.RewardItem(dtoItem.item());
                        entityItem.setAmount(dtoItem.amount());
                        entity.addRewardItem(entityItem);
                    }
                    
                    // Add reward currencies
                    for (Map.Entry<String, Double> entry : request.rewardCurrencies().entrySet()) {
                        entity.addRewardCurrency(entry.getKey(), entry.getValue());
                    }
                    
                    // Set expiration time based on configuration or custom expiration
                    LocalDateTime expiresAt = request.customExpiration()
                            .orElse(LocalDateTime.now().plusDays(expiryDays));
                    entity.setExpiresAt(Optional.of(expiresAt));
                    
                    // Set total estimated value
                    entity.setTotalEstimatedValue(totalValue);
                    
                    // Save bounty entity asynchronously
                    return bountyRepository.createAsync(entity)
                            .thenCompose(this::entityToDto);
                });
    }
    
    @Override
    public @NotNull CompletableFuture<Boolean> deleteBounty(@NotNull Long bountyId) {
        Objects.requireNonNull(bountyId, "bountyId cannot be null");
        
        // Mark bounty as inactive instead of deleting
        return bountyRepository.findByIdAsync(bountyId)
                .thenCompose(bounty -> {
                    if (bounty == null) {
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    // Expire the bounty (marks it as inactive)
                    try {
                        bounty.expire();
                        return bountyRepository.updateAsync(bounty)
                                .thenApply(v -> true);
                    } catch (IllegalStateException e) {
                        return CompletableFuture.completedFuture(false);
                    }
                });
    }
    
    @Override
    public @NotNull CompletableFuture<Bounty> claimBounty(@NotNull Long bountyId, @NotNull UUID hunterUuid) throws BountyException {
        Objects.requireNonNull(bountyId, "bountyId cannot be null");
        Objects.requireNonNull(hunterUuid, "hunterUuid cannot be null");
        
        return bountyRepository.findByIdAsync(bountyId)
                .thenCompose(bounty -> {
                    if (bounty == null) {
                        return CompletableFuture.failedFuture(
                                new BountyNotFoundException(bountyId)
                        );
                    }
                    
                    // Validate bounty can be claimed
                    if (bounty.isClaimed()) {
                        return CompletableFuture.failedFuture(
                                new BountyAlreadyClaimedException("Bounty has already been claimed")
                        );
                    }
                    
                    if (bounty.isExpired()) {
                        return CompletableFuture.failedFuture(
                                new BountyExpiredException("Bounty has expired")
                        );
                    }
                    
                    // Mark bounty as claimed with hunter UUID and timestamp
                    try {
                        bounty.claim(hunterUuid);
                    } catch (IllegalStateException e) {
                        return CompletableFuture.failedFuture(
                                new InvalidBountyStateException(e.getMessage())
                        );
                    }
                    
                    // Update hunter statistics
                    CompletableFuture<Void> statsUpdateFuture = updateHunterStatistics(
                            hunterUuid,
                            bounty.getTotalEstimatedValue()
                    );
                    
                    // Save bounty and wait for stats update
                    return CompletableFuture.allOf(
                            bountyRepository.updateAsync(bounty),
                            statsUpdateFuture
                    ).thenCompose(v -> entityToDto(bounty));
                    
                    // TODO: Distribute rewards based on configured distribution mode
                    // This will be implemented in task 6
                });
    }
    
    @Override
    public @NotNull CompletableFuture<Void> expireBounty(@NotNull Long bountyId) {
        Objects.requireNonNull(bountyId, "bountyId cannot be null");
        
        return bountyRepository.findByIdAsync(bountyId)
                .thenCompose(bounty -> {
                    if (bounty == null) {
                        // Bounty not found, consider it already expired/removed
                        return CompletableFuture.completedFuture(null);
                    }
                    
                    // Mark bounty as expired (inactive)
                    try {
                        bounty.expire();
                    } catch (IllegalStateException e) {
                        // Bounty already claimed, cannot expire
                        return CompletableFuture.completedFuture(null);
                    }
                    
                    // Save the expired bounty
                    return bountyRepository.updateAsync(bounty)
                            .thenApply(v -> null);
                    
                    // TODO: Refund rewards to commissioner if configured
                    // This will be implemented when reward distribution is added in task 6
                });
    }
    
    // ========== Hunter Statistics ==========
    
    @Override
    public @NotNull CompletableFuture<Optional<HunterStats>> getHunterStats(@NotNull UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        
        return hunterStatsRepository.findByPlayerUuidAsync(playerUuid)
                .thenCompose(statsOpt -> {
                    if (statsOpt.isEmpty()) {
                        return CompletableFuture.completedFuture(Optional.empty());
                    }
                    
                    BountyHunterStats stats = statsOpt.get();
                    
                    // Get the hunter's rank
                    return hunterStatsRepository.getPlayerRankAsync(playerUuid)
                            .thenApply(rank -> {
                                // Convert timestamp to LocalDateTime
                                Optional<LocalDateTime> lastClaimTime = stats.getLastClaimTimestamp()
                                        .map(timestamp -> LocalDateTime.ofInstant(
                                                java.time.Instant.ofEpochMilli(timestamp),
                                                ZoneId.systemDefault()
                                        ));
                                
                                HunterStats dto = new HunterStats(
                                        stats.getPlayerUniqueId(),
                                        stats.getPlayerName(),
                                        stats.getBountiesClaimed(),
                                        stats.getTotalRewardValue(),
                                        stats.getHighestBountyValue(),
                                        lastClaimTime,
                                        rank
                                );
                                
                                return Optional.of(dto);
                            });
                });
    }
    
    @Override
    public @NotNull CompletableFuture<List<HunterStats>> getTopHunters(int limit, @NotNull HunterSortOrder sortOrder) {
        Objects.requireNonNull(sortOrder, "sortOrder cannot be null");
        
        // Map sort order to repository field name
        String orderByField = switch (sortOrder) {
            case BOUNTIES_CLAIMED -> "bountiesClaimed";
            case TOTAL_REWARD_VALUE -> "totalRewardValue";
            case HIGHEST_BOUNTY_VALUE -> "highestBountyValue";
            case RECENT_CLAIMS -> "lastClaimTimestamp";
        };
        
        return hunterStatsRepository.findTopHuntersAsync(limit, orderByField)
                .thenApply(statsList -> {
                    // Convert entities to DTOs with ranking
                    int rank = 1;
                    List<HunterStats> dtos = new java.util.ArrayList<>();
                    
                    for (BountyHunterStats stats : statsList) {
                        Optional<LocalDateTime> lastClaimTime = stats.getLastClaimTimestamp()
                                .map(timestamp -> LocalDateTime.ofInstant(
                                        java.time.Instant.ofEpochMilli(timestamp),
                                        ZoneId.systemDefault()
                                ));
                        
                        HunterStats dto = new HunterStats(
                                stats.getPlayerUniqueId(),
                                stats.getPlayerName(),
                                stats.getBountiesClaimed(),
                                stats.getTotalRewardValue(),
                                stats.getHighestBountyValue(),
                                lastClaimTime,
                                rank++
                        );
                        
                        dtos.add(dto);
                    }
                    
                    return dtos;
                });
    }
    
    @Override
    public @NotNull CompletableFuture<Integer> getHunterRank(@NotNull UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        
        return hunterStatsRepository.getPlayerRankAsync(playerUuid);
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Converts an RBounty entity to a Bounty DTO asynchronously.
     * This method resolves player names from UUIDs.
     *
     * @param entity the bounty entity
     * @return a future containing the bounty DTO
     */
    private @NotNull CompletableFuture<Bounty> entityToDto(@NotNull RBounty entity) {
        // Resolve player names asynchronously
        CompletableFuture<String> targetNameFuture = resolvePlayerName(entity.getTargetUniqueId());
        CompletableFuture<String> commissionerNameFuture = resolvePlayerName(entity.getCommissionerUniqueId());
        
        return CompletableFuture.allOf(targetNameFuture, commissionerNameFuture)
                .thenApply(v -> {
                    String targetName = targetNameFuture.join();
                    String commissionerName = commissionerNameFuture.join();
                    
                    // Determine bounty status
                    BountyStatus status;
                    if (entity.isClaimed()) {
                        status = BountyStatus.CLAIMED;
                    } else if (entity.isExpired()) {
                        status = BountyStatus.EXPIRED;
                    } else if (entity.isActive()) {
                        status = BountyStatus.ACTIVE;
                    } else {
                        status = BountyStatus.CANCELLED;
                    }
                    
                    // Build claim info if bounty is claimed
                    Optional<ClaimInfo> claimInfo = entity.getClaimedBy()
                            .flatMap(claimerUuid -> entity.getClaimedAt()
                                    .map(claimedAt -> {
                                        String hunterName = resolvePlayerNameSync(claimerUuid);
                                        return new ClaimInfo(
                                                claimerUuid,
                                                hunterName,
                                                claimedAt,
                                                ClaimMode.LAST_HIT // Default, will be configurable later
                                        );
                                    }));
                    
                    // Convert entity reward items to DTO reward items
                    Set<com.raindropcentral.rdq.bounty.dto.RewardItem> dtoRewardItems = entity.getRewardItems().stream()
                            .map(entityItem -> new com.raindropcentral.rdq.bounty.dto.RewardItem(
                                    entityItem.getItem(),
                                    entityItem.getAmount(),
                                    0.0 // Estimated value not stored in entity
                            ))
                            .collect(java.util.stream.Collectors.toSet());
                    
                    return new Bounty(
                            entity.getId(),
                            entity.getTargetUniqueId(),
                            targetName,
                            entity.getCommissionerUniqueId(),
                            commissionerName,
                            dtoRewardItems,
                            entity.getRewardCurrencies(),
                            entity.getTotalEstimatedValue(),
                            entity.getCreatedAt(),
                            entity.getExpiresAt().orElse(null),
                            status,
                            claimInfo
                    );
                });
    }
    
    /**
     * Resolves a player name from UUID asynchronously.
     * First checks online players, then falls back to database lookup.
     *
     * @param uuid the player UUID
     * @return a future containing the player name
     */
    private @NotNull CompletableFuture<String> resolvePlayerName(@NotNull UUID uuid) {
        // Check if player is online first
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            return CompletableFuture.completedFuture(onlinePlayer.getName());
        }
        
        // Fall back to database lookup
        return playerRepository.findByUuidAsync(uuid)
                .thenApply(playerOpt -> playerOpt
                        .map(RDQPlayer::getPlayerName)
                        .orElse("Unknown"));
    }
    
    /**
     * Resolves a player name from UUID synchronously.
     * This is a fallback for cases where async resolution is not practical.
     *
     * @param uuid the player UUID
     * @return the player name or "Unknown"
     */
    private @NotNull String resolvePlayerNameSync(@NotNull UUID uuid) {
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }
        
        try {
            return playerRepository.findByUuidAsync(uuid)
                    .thenApply(playerOpt -> playerOpt
                            .map(RDQPlayer::getPlayerName)
                            .orElse("Unknown"))
                    .join();
        } catch (Exception e) {
            return "Unknown";
        }
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
     * Creates a new statistics entity if this is the hunter's first claim.
     *
     * @param hunterUuid the UUID of the hunter
     * @param rewardValue the value of the bounty reward
     * @return a future that completes when statistics are updated
     */
    private @NotNull CompletableFuture<Void> updateHunterStatistics(
            @NotNull UUID hunterUuid,
            double rewardValue
    ) {
        return playerRepository.findByUuidAsync(hunterUuid)
                .thenCompose(playerOpt -> {
                    if (playerOpt.isEmpty()) {
                        // Player not found in database, skip stats update
                        return CompletableFuture.completedFuture(null);
                    }
                    
                    RDQPlayer player = playerOpt.get();
                    
                    return hunterStatsRepository.findByPlayerAsync(player)
                            .thenCompose(statsOpt -> {
                                BountyHunterStats stats;
                                CompletableFuture<BountyHunterStats> saveFuture;
                                
                                if (statsOpt.isEmpty()) {
                                    // Create new statistics entity for first-time hunter
                                    stats = new BountyHunterStats(player);
                                    // Update statistics atomically
                                    stats.recordClaim(rewardValue);
                                    saveFuture = hunterStatsRepository.createAsync(stats);
                                } else {
                                    stats = statsOpt.get();
                                    // Update statistics atomically
                                    stats.recordClaim(rewardValue);
                                    saveFuture = hunterStatsRepository.updateAsync(stats);
                                }
                                
                                return saveFuture.thenApply(v -> null);
                            });
                });
    }
}
