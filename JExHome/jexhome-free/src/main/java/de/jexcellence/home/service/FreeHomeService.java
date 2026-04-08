package de.jexcellence.home.service;

import de.jexcellence.home.database.entity.Home;
import de.jexcellence.home.database.repository.HomeRepository;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Free version of the Home Service with limited functionality.
 * <p>
 * Limitations:
 * - Maximum 3 homes per player
 * - No category support
 * - No favorites support
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class FreeHomeService implements IHomeService {

    private static final int MAX_HOMES_FREE = 3;

    private static FreeHomeService instance;
    private final HomeRepository repository;

    private FreeHomeService(@NotNull HomeRepository repository) {
        this.repository = repository;
    }

    /**
     * Initializes the Free Home Service.
     *
     * @param repository the home repository
     * @return the initialized service instance
     */
    public static FreeHomeService initialize(@NotNull HomeRepository repository) {
        if (instance == null) {
            instance = new FreeHomeService(repository);
        }
        return instance;
    }

    /**
     * Gets the initialized instance.
     *
     * @return the service instance
     * @throws IllegalStateException if not initialized
     */
    public static FreeHomeService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("FreeHomeService not initialized. Call initialize() first.");
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
                
                return repository.countByPlayerUuid(playerId)
                    .thenCompose(count -> {
                        if (count >= MAX_HOMES_FREE) {
                            return CompletableFuture.failedFuture(
                                new IllegalStateException("Free version: Maximum " + MAX_HOMES_FREE + " homes. Upgrade to Premium!")
                            );
                        }
                        var home = new Home(name, playerId, location);
                        return repository.saveHome(home);
                    });
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
        var count = repository.countByPlayerUuid(player.getUniqueId()).join();
        return count < MAX_HOMES_FREE;
    }

    @Override
    public int getMaxHomesForPlayer(@NotNull Player player) {
        return MAX_HOMES_FREE;
    }

    @Override
    public @NotNull CompletableFuture<Long> getHomeCount(@NotNull UUID playerId) {
        return repository.countByPlayerUuid(playerId);
    }

    @Override
    public boolean isPremium() {
        return false;
    }

    // Premium features - disabled in free version

    @Override
    public @NotNull CompletableFuture<List<Home>> getHomesByCategory(@NotNull UUID playerId, @NotNull String category) {
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    @Override
    public @NotNull CompletableFuture<List<Home>> getFavoriteHomes(@NotNull UUID playerId) {
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    @Override
    public @NotNull CompletableFuture<Boolean> setHomeCategory(@NotNull UUID playerId, @NotNull String homeName, @NotNull String category) {
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> toggleFavorite(@NotNull UUID playerId, @NotNull String homeName) {
        return CompletableFuture.completedFuture(false);
    }
}
