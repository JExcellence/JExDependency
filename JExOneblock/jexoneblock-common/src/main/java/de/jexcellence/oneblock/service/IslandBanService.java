package de.jexcellence.oneblock.service;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIslandBan;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer;
import de.jexcellence.oneblock.database.repository.OneblockIslandBanRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class IslandBanService {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    private final OneblockIslandBanRepository banRepository;
    private final ScheduledExecutorService scheduler;
    
    public IslandBanService(@NotNull OneblockIslandBanRepository banRepository) {
        this.banRepository = banRepository;
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        startExpiredBanCleanup();
    }

    public @NotNull CompletableFuture<Boolean> banPlayer(
            @NotNull OneblockIsland island,
            @NotNull OneblockPlayer target,
            @NotNull OneblockPlayer bannedBy,
            @NotNull String reason,
            @Nullable Duration duration
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (island.isOwner(target)) {
                    LOGGER.warning("Cannot ban island owner " + target.getPlayerName() + " from island " + island.getIdentifier());
                    return false;
                }
                
                if (island.isBanned(target)) {
                    LOGGER.warning("Player " + target.getPlayerName() + " is already banned from island " + island.getIdentifier());
                    return false;
                }
                
                island.removeMember(target);
                
                var expiresAt = duration != null ? LocalDateTime.now().plus(duration) : null;
                
                var ban = duration != null 
                    ? new OneblockIslandBan(island, target, bannedBy, reason, expiresAt)
                    : new OneblockIslandBan(island, target, bannedBy, reason);
                
                banRepository.create(ban);
                
                island.banPlayer(target);
                
                var banType = duration != null ? "temporarily" : "permanently";
                LOGGER.info("Banned player " + target.getPlayerName() + " " + banType + " from island " + island.getIdentifier() + ". Reason: " + reason);
                
                return true;
                
            } catch (Exception e) {
                LOGGER.severe("Failed to ban player from island: " + e.getMessage());
                return false;
            }
        });
    }

    public @NotNull CompletableFuture<Boolean> unbanPlayer(
            @NotNull OneblockIsland island,
            @NotNull OneblockPlayer target,
            @NotNull OneblockPlayer unbannedBy
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!island.isBanned(target)) {
                    LOGGER.warning("Player " + target.getPlayerName() + " is not banned from island " + island.getIdentifier());
                    return false;
                }
                
                island.unbanPlayerEntity(target);
                
                island.unbanPlayer(target);
                
                banRepository.findActiveByIslandAndPlayerAsync(island, target)
                    .thenAccept(opt -> opt.ifPresent(ban -> {
                        ban.unban(unbannedBy);
                        banRepository.create(ban);
                    }));
                
                LOGGER.info("Unbanned player " + target.getPlayerName() + " from island " + island.getIdentifier());
                return true;
                
            } catch (Exception e) {
                LOGGER.severe("Failed to unban player from island: " + e.getMessage());
                return false;
            }
        });
    }
    
    public @NotNull CompletableFuture<List<OneblockIslandBan>> getActiveBans(@NotNull OneblockIsland island) {
        return banRepository.findActiveByIslandAsync(island);
    }
    
    public @NotNull CompletableFuture<List<OneblockIslandBan>> getAllBans(@NotNull OneblockIsland island) {
        return banRepository.findByIslandAsync(island);
    }
    
    public boolean isBanned(@NotNull OneblockIsland island, @NotNull OneblockPlayer player) {
        return island.isBanned(player);
    }
    
    public @NotNull CompletableFuture<java.util.Optional<OneblockIslandBan>> getActiveBan(
            @NotNull OneblockIsland island,
            @NotNull OneblockPlayer player
    ) {
        return banRepository.findActiveByIslandAndPlayerAsync(island, player);
    }
    
    public @NotNull CompletableFuture<List<OneblockIslandBan>> getBanHistory(
            @NotNull OneblockIsland island,
            @NotNull OneblockPlayer player
    ) {
        return banRepository.findByIslandAndPlayerAsync(island, player);
    }
    
    public void cleanupExpiredBans() {
        banRepository.findExpiredBansAsync()
            .thenAccept(expiredBans -> {
                if (!expiredBans.isEmpty()) {
                    LOGGER.info("Cleaning up " + expiredBans.size() + " expired bans");
                    
                    for (var ban : expiredBans) {
                        ban.setActive(false);
                        banRepository.create(ban);
                        
                        ban.getIsland().unbanPlayer(ban.getBannedPlayer());
                        
                        LOGGER.fine("Expired ban removed for player " + ban.getBannedPlayer().getPlayerName() + 
                                   " on island " + ban.getIsland().getIdentifier());
                    }
                }
            })
            .exceptionally(throwable -> {
                LOGGER.severe("Failed to cleanup expired bans: " + throwable.getMessage());
                return null;
            });
    }
    
    public @NotNull CompletableFuture<Integer> getBanCount(@NotNull OneblockIsland island) {
        return getActiveBans(island)
            .thenApply(List::size);
    }
    
    public @NotNull CompletableFuture<Boolean> isAtBanLimit(@NotNull OneblockIsland island) {
        return getBanCount(island)
            .thenApply(count -> {
                var plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("JExOneblock");
                int banLimit = 50;
                
                if (plugin instanceof de.jexcellence.oneblock.JExOneblock jexPlugin) {
                    var upgradeService = new de.jexcellence.oneblock.service.UpgradeService();
                    var memberSlotLevel = upgradeService.getCurrentLevel(island, 
                        de.jexcellence.oneblock.service.UpgradeService.UpgradeType.MEMBER_SLOTS);
                    banLimit = 50 + (memberSlotLevel * 10);
                }
                
                return count >= banLimit;
            });
    }
    
    private void startExpiredBanCleanup() {
        scheduler.scheduleAtFixedRate(
            this::cleanupExpiredBans,
            5,
            5,
            TimeUnit.MINUTES
        );
        
        LOGGER.info("Started expired ban cleanup scheduler (runs every 5 minutes)");
    }
    
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        LOGGER.info("Island ban service shut down");
    }
}