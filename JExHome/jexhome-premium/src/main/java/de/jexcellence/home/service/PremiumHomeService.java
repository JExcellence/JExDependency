package de.jexcellence.home.service;

import de.jexcellence.home.config.HomeSystemConfig;
import de.jexcellence.home.database.entity.Home;
import de.jexcellence.home.database.repository.HomeRepository;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Premium version of the Home Service with full functionality.
 * <p>
 * Features:
 * - Configurable home limits via permissions
 * - Category support
 * - Favorites support
 * - Full statistics tracking
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PremiumHomeService implements IHomeService {

    private static PremiumHomeService instance;
    private final HomeRepository repository;
    private final HomeSystemConfig config;

    private PremiumHomeService(@NotNull HomeRepository repository, @NotNull HomeSystemConfig config) {
        this.repository = repository;
        this.config = config;
    }

    /**
     * Initializes the Premium Home Service.
     *
     * @param repository the home repository
     * @param config     the home system configuration
     * @return the initialized service instance
     */
    public static PremiumHomeService initialize(@NotNull HomeRepository repository, @NotNull HomeSystemConfig config) {
        if (instance == null) {
            instance = new PremiumHomeService(repository, config);
        }
        return instance;
    }

    /**
     * Gets the initialized instance.
     *
     * @return the service instance
     * @throws IllegalStateException if not initialized
     */
    public static PremiumHomeService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PremiumHomeService not initialized. Call initialize() first.");
        }
        return instance;
    }

    @Override
    public @NotNull CompletableFuture<Home> createHome(@NotNull UUID playerId, @NotNull String name, @NotNull Location location) {
        return repository.findByPlayerAndName(playerId, name)
            .thenCompose(existing -> {
                if (existing.isPresent()) {
                    var home = existing.get();
                    home.setLocation(location);
                    return repository.saveHome(home);
                }
                var home = new Home(name, playerId, location);
                return repository.saveHome(home);
            });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> deleteHome(@NotNull UUID playerId, @NotNull String name) {
        return repository.deleteByPlayerAndName(playerId, name);
    }

    @Override
    public @NotNull CompletableFuture<Optional<Home>> findHome(@NotNull UUID playerId, @NotNull String name) {
        return repository.findByPlayerAndName(playerId, name);
    }

    @Override
    public @NotNull CompletableFuture<List<Home>> getPlayerHomes(@NotNull UUID playerId) {
        return repository.findByPlayerUuid(playerId);
    }

    @Override
    public @NotNull CompletableFuture<Home> updateHome(@NotNull Home home) {
        return repository.saveHome(home);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> teleportToHome(@NotNull Player player, @NotNull String homeName) {
        return findHome(player.getUniqueId(), homeName)
            .thenApply(optionalHome -> {
                if (optionalHome.isEmpty()) {
                    return false;
                }
                var home = optionalHome.get();
                var location = home.toLocation();
                if (location == null) {
                    return false;
                }
                home.recordVisit();
                repository.saveHome(home);
                player.teleport(location);
                return true;
            });
    }

    @Override
    public boolean canCreateHome(@NotNull Player player) {
        var maxHomes = getMaxHomesForPlayer(player);
        if (maxHomes == -1) return true;
        var count = repository.countByPlayerUuid(player.getUniqueId()).join();
        return count < maxHomes;
    }

    @Override
    public int getMaxHomesForPlayer(@NotNull Player player) {
        return config.getMaxHomesForPlayer(player);
    }

    @Override
    public @NotNull CompletableFuture<Long> getHomeCount(@NotNull UUID playerId) {
        return repository.countByPlayerUuid(playerId);
    }

    @Override
    public boolean isPremium() {
        return true;
    }

    // Premium features - fully enabled

    @Override
    public @NotNull CompletableFuture<List<Home>> getHomesByCategory(@NotNull UUID playerId, @NotNull String category) {
        return repository.findByPlayerUuid(playerId)
            .thenApply(homes -> homes.stream()
                .filter(h -> category.equalsIgnoreCase(h.getCategory()))
                .collect(Collectors.toList()));
    }

    @Override
    public @NotNull CompletableFuture<List<Home>> getFavoriteHomes(@NotNull UUID playerId) {
        return repository.findByPlayerUuid(playerId)
            .thenApply(homes -> homes.stream()
                .filter(Home::isFavorite)
                .collect(Collectors.toList()));
    }

    @Override
    public @NotNull CompletableFuture<Boolean> setHomeCategory(@NotNull UUID playerId, @NotNull String homeName, @NotNull String category) {
        return findHome(playerId, homeName)
            .thenCompose(optionalHome -> {
                if (optionalHome.isEmpty()) {
                    return CompletableFuture.completedFuture(false);
                }
                var home = optionalHome.get();
                home.setCategory(category);
                return repository.saveHome(home).thenApply(h -> true);
            });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> toggleFavorite(@NotNull UUID playerId, @NotNull String homeName) {
        return findHome(playerId, homeName)
            .thenCompose(optionalHome -> {
                if (optionalHome.isEmpty()) {
                    return CompletableFuture.completedFuture(false);
                }
                var home = optionalHome.get();
                home.setFavorite(!home.isFavorite());
                return repository.saveHome(home).thenApply(h -> true);
            });
    }
}
