package de.jexcellence.oneblock.database.repository;

import de.jexcellence.hibernate.repository.CachedRepository;
import de.jexcellence.hibernate.repository.InjectRepository;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIslandBan;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository providing cached CRUD access to {@link OneblockIslandBan} entities.
 * Handles island ban data persistence with time-based queries and asynchronous operations.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
@InjectRepository
public class OneblockIslandBanRepository extends CachedRepository<OneblockIslandBan, Long, Long> {

    public OneblockIslandBanRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<OneblockIslandBan> entityClass,
            @NotNull Function<OneblockIslandBan, Long> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
    }

    // ========== Synchronous Methods ==========

    /**
     * Finds an active ban for a player on an island
     */
    @Nullable
    public OneblockIslandBan findActiveBan(@NotNull Long islandId, @NotNull UUID playerUuid) {
        return findAll().stream()
            .filter(b -> b.getIsland() != null && islandId.equals(b.getIsland().getId()))
            .filter(b -> b.getBannedPlayer() != null && playerUuid.equals(b.getBannedPlayer().getUniqueId()))
            .filter(OneblockIslandBan::isActive)
            .findFirst()
            .orElse(null);
    }

    /**
     * Finds all bans for an island
     */
    @NotNull
    public List<OneblockIslandBan> findByIsland(@NotNull Long islandId) {
        return findAll().stream()
            .filter(b -> b.getIsland() != null && islandId.equals(b.getIsland().getId()))
            .toList();
    }

    /**
     * Finds all bans for a player
     */
    @NotNull
    public List<OneblockIslandBan> findByPlayer(@NotNull UUID playerUuid) {
        return findAll().stream()
            .filter(b -> b.getBannedPlayer() != null && playerUuid.equals(b.getBannedPlayer().getUniqueId()))
            .toList();
    }

    /**
     * Finds all active bans for an island
     */
    @NotNull
    public List<OneblockIslandBan> findActiveByIsland(@NotNull Long islandId) {
        return findAll().stream()
            .filter(b -> b.getIsland() != null && islandId.equals(b.getIsland().getId()))
            .filter(OneblockIslandBan::isActive)
            .toList();
    }

    /**
     * Finds all expired bans that are still active
     */
    @NotNull
    public List<OneblockIslandBan> findExpiredBans() {
        LocalDateTime now = LocalDateTime.now();
        return findAll().stream()
            .filter(OneblockIslandBan::isActive)
            .filter(b -> b.getExpiresAt() != null && b.getExpiresAt().isBefore(now))
            .toList();
    }

    /**
     * Finds permanent bans for an island
     */
    @NotNull
    public List<OneblockIslandBan> findPermanentBans(@NotNull Long islandId) {
        return findAll().stream()
            .filter(b -> b.getIsland() != null && islandId.equals(b.getIsland().getId()))
            .filter(OneblockIslandBan::isActive)
            .filter(b -> b.getExpiresAt() == null)
            .toList();
    }

    /**
     * Checks if a player is banned from an island
     */
    public boolean isBanned(@NotNull Long islandId, @NotNull UUID playerUuid) {
        return findActiveBan(islandId, playerUuid) != null;
    }

    /**
     * Counts active bans for an island
     */
    public long countActiveByIsland(@NotNull Long islandId) {
        return findActiveByIsland(islandId).size();
    }

    /**
     * Deactivates expired bans
     */
    public int deactivateExpiredBans() {
        List<OneblockIslandBan> expired = findExpiredBans();
        expired.forEach(ban -> {
            ban.setActive(false);
            updateAsync(ban);
        });
        return expired.size();
    }

    // ========== Asynchronous Methods ==========

    /**
     * Asynchronously finds bans for an island
     */
    @NotNull
    public CompletableFuture<List<OneblockIslandBan>> findByIslandAsync(@NotNull Long islandId) {
        return CompletableFuture.supplyAsync(
            () -> findByIsland(islandId),
            getExecutorService()
        );
    }
    
    /**
     * Asynchronously finds bans for an island entity
     */
    @NotNull
    public CompletableFuture<List<OneblockIslandBan>> findByIslandAsync(
            @NotNull de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland island) {
        return findByIslandAsync(island.getId());
    }
    
    /**
     * Asynchronously finds active bans for an island entity
     */
    @NotNull
    public CompletableFuture<List<OneblockIslandBan>> findActiveByIslandAsync(
            @NotNull de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland island) {
        return CompletableFuture.supplyAsync(
            () -> findActiveByIsland(island.getId()),
            getExecutorService()
        );
    }
    
    /**
     * Asynchronously finds an active ban by island and player entities
     */
    @NotNull
    public CompletableFuture<java.util.Optional<OneblockIslandBan>> findActiveByIslandAndPlayerAsync(
            @NotNull de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland island,
            @NotNull de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer player) {
        return CompletableFuture.supplyAsync(
            () -> java.util.Optional.ofNullable(findActiveBan(island.getId(), player.getUniqueId())),
            getExecutorService()
        );
    }
    
    /**
     * Asynchronously finds all bans by island and player entities
     */
    @NotNull
    public CompletableFuture<List<OneblockIslandBan>> findByIslandAndPlayerAsync(
            @NotNull de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland island,
            @NotNull de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer player) {
        return CompletableFuture.supplyAsync(
            () -> findAll().stream()
                .filter(b -> b.getIsland() != null && island.getId().equals(b.getIsland().getId()))
                .filter(b -> b.getBannedPlayer() != null && player.getUniqueId().equals(b.getBannedPlayer().getUniqueId()))
                .toList(),
            getExecutorService()
        );
    }
    
    /**
     * Asynchronously finds expired bans
     */
    @NotNull
    public CompletableFuture<List<OneblockIslandBan>> findExpiredBansAsync() {
        return CompletableFuture.supplyAsync(
            this::findExpiredBans,
            getExecutorService()
        );
    }

    /**
     * Asynchronously finds an active ban
     */
    @NotNull
    public CompletableFuture<Optional<OneblockIslandBan>> findActiveBanAsync(@NotNull Long islandId, @NotNull UUID playerUuid) {
        return CompletableFuture.supplyAsync(
            () -> Optional.ofNullable(findActiveBan(islandId, playerUuid)),
            getExecutorService()
        );
    }

    /**
     * Asynchronously checks if a player is banned
     */
    @NotNull
    public CompletableFuture<Boolean> isBannedAsync(@NotNull Long islandId, @NotNull UUID playerUuid) {
        return CompletableFuture.supplyAsync(
            () -> isBanned(islandId, playerUuid),
            getExecutorService()
        );
    }

    /**
     * Asynchronously deactivates expired bans
     */
    @NotNull
    public CompletableFuture<Integer> deactivateExpiredBansAsync() {
        return CompletableFuture.supplyAsync(
            this::deactivateExpiredBans,
            getExecutorService()
        );
    }

    /**
     * Asynchronously deletes all bans for an island
     */
    @NotNull
    public CompletableFuture<Void> deleteByIslandAsync(@NotNull Long islandId) {
        return CompletableFuture.runAsync(() -> {
            findByIsland(islandId).forEach(ban -> deleteAsync(ban.getId()));
        }, getExecutorService());
    }
}
