package de.jexcellence.oneblock.database.repository;

import de.jexcellence.hibernate.repository.CachedRepository;
import de.jexcellence.hibernate.repository.InjectRepository;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIslandMember;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository providing cached CRUD access to {@link OneblockIslandMember} entities.
 * Handles island membership data persistence with asynchronous operations.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
@InjectRepository
public class OneblockIslandMemberRepository extends CachedRepository<OneblockIslandMember, Long, Long> {

    private final ExecutorService executor;

    public OneblockIslandMemberRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<OneblockIslandMember> entityClass,
            @NotNull Function<OneblockIslandMember, Long> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
        this.executor = executor;
    }

    // ========== Synchronous Methods ==========

    /**
     * Finds a member by island and player
     */
    @Nullable
    public OneblockIslandMember findByIslandAndPlayer(@NotNull Long islandId, @NotNull UUID playerUuid) {
        return findAll().stream()
            .filter(m -> m.getIsland() != null && islandId.equals(m.getIsland().getId()))
            .filter(m -> m.getPlayer() != null && playerUuid.equals(m.getPlayer().getUniqueId()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Finds a member by island entity and player entity
     */
    @NotNull
    public java.util.Optional<OneblockIslandMember> findByIslandAndPlayer(
            @NotNull de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland island,
            @NotNull de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer player) {
        return java.util.Optional.ofNullable(findByIslandAndPlayer(island.getId(), player.getUniqueId()));
    }

    /**
     * Finds all members for an island
     */
    @NotNull
    public List<OneblockIslandMember> findByIsland(@NotNull Long islandId) {
        return findAll().stream()
            .filter(m -> m.getIsland() != null && islandId.equals(m.getIsland().getId()))
            .toList();
    }

    /**
     * Finds all memberships for a player
     */
    @NotNull
    public List<OneblockIslandMember> findByPlayer(@NotNull UUID playerUuid) {
        return findAll().stream()
            .filter(m -> m.getPlayer() != null && playerUuid.equals(m.getPlayer().getUniqueId()))
            .toList();
    }

    /**
     * Finds active members for an island
     */
    @NotNull
    public List<OneblockIslandMember> findActiveByIsland(@NotNull Long islandId) {
        return findAll().stream()
            .filter(m -> m.getIsland() != null && islandId.equals(m.getIsland().getId()))
            .filter(OneblockIslandMember::isActive)
            .toList();
    }

    /**
     * Finds members by role for an island
     */
    @NotNull
    public List<OneblockIslandMember> findByIslandAndRole(@NotNull Long islandId, @NotNull String role) {
        return findAll().stream()
            .filter(m -> m.getIsland() != null && islandId.equals(m.getIsland().getId()))
            .filter(m -> role.equals(m.getRole()))
            .toList();
    }

    /**
     * Checks if a player is a member of an island
     */
    public boolean isMember(@NotNull Long islandId, @NotNull UUID playerUuid) {
        return findByIslandAndPlayer(islandId, playerUuid) != null;
    }

    /**
     * Counts active members for an island
     */
    public long countActiveByIsland(@NotNull Long islandId) {
        return findActiveByIsland(islandId).size();
    }

    /**
     * Counts members by island and active status
     */
    public long countByIslandAndIsActive(
            @NotNull de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland island,
            boolean isActive) {
        return findAll().stream()
            .filter(m -> m.getIsland() != null && island.getId().equals(m.getIsland().getId()))
            .filter(m -> m.isActive() == isActive)
            .count();
    }
    
    /**
     * Finds members by island and active status
     */
    @NotNull
    public List<OneblockIslandMember> findByIslandAndIsActive(
            @NotNull de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland island,
            boolean isActive) {
        return findAll().stream()
            .filter(m -> m.getIsland() != null && island.getId().equals(m.getIsland().getId()))
            .filter(m -> m.isActive() == isActive)
            .toList();
    }
    
    /**
     * Finds a member by island, player, and active status
     */
    @NotNull
    public java.util.Optional<OneblockIslandMember> findByIslandAndPlayerAndIsActive(
            @NotNull de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland island,
            @NotNull de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer player,
            boolean isActive) {
        return findAll().stream()
            .filter(m -> m.getIsland() != null && island.getId().equals(m.getIsland().getId()))
            .filter(m -> m.getPlayer() != null && player.getUniqueId().equals(m.getPlayer().getUniqueId()))
            .filter(m -> m.isActive() == isActive)
            .findFirst();
    }
    
    /**
     * Saves a member entity
     */
    public OneblockIslandMember saveMember(@NotNull OneblockIslandMember member) {
        if (member.getId() != null && member.getId() > 0) {
            return update(member);
        } else {
            return create(member);
        }
    }
    
    /**
     * Finds pending invites for a player (inactive memberships)
     */
    @NotNull
    public List<OneblockIslandMember> findPendingInvitesByPlayer(
            @NotNull de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer player) {
        return findAll().stream()
            .filter(m -> m.getPlayer() != null && player.getUniqueId().equals(m.getPlayer().getUniqueId()))
            .filter(m -> !m.isActive())
            .toList();
    }

    // ========== Asynchronous Methods ==========

    /**
     * Asynchronously finds members for an island
     */
    @NotNull
    public CompletableFuture<List<OneblockIslandMember>> findByIslandAsync(@NotNull Long islandId) {
        return CompletableFuture.supplyAsync(
            () -> findByIsland(islandId),
            this.executor
        );
    }
    
    /**
     * Asynchronously finds members for an island entity
     */
    @NotNull
    public CompletableFuture<List<OneblockIslandMember>> findByIslandAsync(
            @NotNull de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland island) {
        return findByIslandAsync(island.getId());
    }
    
    /**
     * Asynchronously finds a member by island and player
     */
    @NotNull
    public CompletableFuture<Optional<OneblockIslandMember>> findByIslandAndPlayerAsync(
            @NotNull de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland island,
            @NotNull de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer player) {
        return CompletableFuture.supplyAsync(
            () -> findByIslandAndPlayer(island, player),
            this.executor
        );
    }

    /**
     * Asynchronously finds memberships for a player
     */
    @NotNull
    public CompletableFuture<List<OneblockIslandMember>> findByPlayerAsync(@NotNull UUID playerUuid) {
        return CompletableFuture.supplyAsync(
            () -> findByPlayer(playerUuid),
            this.executor
        );
    }

    /**
     * Asynchronously checks if a player is a member
     */
    @NotNull
    public CompletableFuture<Boolean> isMemberAsync(@NotNull Long islandId, @NotNull UUID playerUuid) {
        return CompletableFuture.supplyAsync(
            () -> isMember(islandId, playerUuid),
            this.executor
        );
    }

    /**
     * Asynchronously deletes all members for an island
     */
    @NotNull
    public CompletableFuture<Void> deleteByIslandAsync(@NotNull Long islandId) {
        return CompletableFuture.runAsync(() -> {
            findByIsland(islandId).forEach(member -> delete(member.getId()));
        }, this.executor);
    }
}
