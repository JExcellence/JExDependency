package de.jexcellence.glow.service;

import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.glow.database.entity.PlayerGlow;
import de.jexcellence.glow.database.repository.GlowRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service implementation for managing player glow effects.
 * <p>
 * Handles business logic for enabling/disabling glow, checking glow status,
 * and applying/removing visual effects. All database operations are asynchronous,
 * and Bukkit API calls are scheduled on the main thread.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class GlowService implements IGlowService {

    private final GlowRepository repository;
    private final JavaPlugin plugin;
    private final Logger logger;

    /**
     * Constructs a new GlowService.
     *
     * @param repository the glow repository
     * @param plugin     the plugin instance for scheduling
     */
    public GlowService(@NotNull GlowRepository repository, @NotNull JavaPlugin plugin) {
        this.repository = repository;
        this.plugin = plugin;
        this.logger = CentralLogger.getLoggerByName("GlowService");
    }

    @Override
    public @NotNull CompletableFuture<Boolean> enableGlow(@NotNull UUID playerId, @Nullable UUID adminId) {
        return repository.findByPlayerUuid(playerId)
            .thenCompose(optionalGlow -> {
                PlayerGlow playerGlow;
                if (optionalGlow.isPresent()) {
                    playerGlow = optionalGlow.get();
                    playerGlow.enableGlow(adminId);
                } else {
                    playerGlow = new PlayerGlow(playerId, true, adminId);
                }
                
                return repository.saveGlowState(playerGlow)
                    .thenCompose(savedGlow -> {
                        // Apply visual effect if player is online
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null && player.isOnline()) {
                            return applyGlowEffect(player).thenApply(v -> true);
                        }
                        return CompletableFuture.completedFuture(true);
                    });
            })
            .exceptionally(throwable -> {
                logger.log(Level.SEVERE, "Failed to enable glow for player " + playerId, throwable);
                return false;
            });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> disableGlow(@NotNull UUID playerId) {
        return repository.findByPlayerUuid(playerId)
            .thenCompose(optionalGlow -> {
                if (optionalGlow.isEmpty()) {
                    // Even if no database record exists, try to remove the visual effect
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null && player.isOnline()) {
                        return removeGlowEffect(player).thenApply(v -> false);
                    }
                    return CompletableFuture.completedFuture(false);
                }
                
                PlayerGlow playerGlow = optionalGlow.get();
                playerGlow.disableGlow();
                
                return repository.saveGlowState(playerGlow)
                    .thenCompose(savedGlow -> {
                        // Remove visual effect if player is online
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null && player.isOnline()) {
                            // Ensure the glow is actually removed from the player
                            return removeGlowEffect(player)
                                .thenApply(v -> {
                                    logger.log(Level.FINE, "Successfully removed glow effect for player: " + player.getName());
                                    return true;
                                });
                        }
                        return CompletableFuture.completedFuture(true);
                    });
            })
            .exceptionally(throwable -> {
                logger.log(Level.SEVERE, "Failed to disable glow for player " + playerId, throwable);
                return false;
            });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> isGlowEnabled(@NotNull UUID playerId) {
        return repository.isGlowEnabled(playerId)
            .exceptionally(throwable -> {
                logger.log(Level.WARNING, "Failed to check glow status for player " + playerId, throwable);
                return false;
            });
    }

    @Override
    public @NotNull CompletableFuture<Void> applyGlowEffect(@NotNull Player player) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        // Schedule on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (player.isOnline() && player.isValid()) {
                    player.setGlowing(true);
                    future.complete(null);
                } else {
                    logger.log(Level.FINE, "Skipped applying glow to offline/invalid player: " + player.getName());
                    future.complete(null);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to apply glow effect to player " + player.getName(), e);
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }

    @Override
    public @NotNull CompletableFuture<Void> removeGlowEffect(@NotNull Player player) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        // Schedule on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (player.isOnline() && player.isValid()) {
                    player.setGlowing(false);
                    future.complete(null);
                } else {
                    logger.log(Level.FINE, "Skipped removing glow from offline/invalid player: " + player.getName());
                    future.complete(null);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to remove glow effect from player " + player.getName(), e);
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
}
