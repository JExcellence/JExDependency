package com.raindropcentral.rdq.perk.cache;

import com.raindropcentral.rdq.database.entity.perk.PlayerPerk;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Cache entry for a single player's perks.
 * Thread-safe storage for PlayerPerk entities with dirty tracking.
 *
 * @author JExcellence
 * @version 1.0.0
 */
class PlayerCacheEntry {
    
    // Map of perk ID -> PlayerPerk entity
    private final ConcurrentHashMap<Long, PlayerPerk> perks;
    
    // Set of dirty perk IDs that need persistence
    private final Set<Long> dirtyPerks;
    
    // Lock for atomic operations
    private final ReentrantReadWriteLock lock;
    
    // Timestamp when cache was loaded
    private final long loadedAt;
    
    /**
     * Creates a new cache entry.
     */
    public PlayerCacheEntry() {
        this.perks = new ConcurrentHashMap<>();
        this.dirtyPerks = ConcurrentHashMap.newKeySet();
        this.lock = new ReentrantReadWriteLock();
        this.loadedAt = System.currentTimeMillis();
    }
    
    /**
     * Gets a PlayerPerk by perk ID.
     *
     * @param perkId the perk ID
     * @return Optional containing the PlayerPerk, or empty if not found
     */
    public Optional<PlayerPerk> getPerk(@NotNull final Long perkId) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(perks.get(perkId));
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets all PlayerPerks.
     *
     * @return list of all PlayerPerks
     */
    public List<PlayerPerk> getAllPerks() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(perks.values());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets filtered PlayerPerks.
     *
     * @param filter the predicate to filter perks
     * @return list of filtered PlayerPerks
     */
    public List<PlayerPerk> getPerks(@NotNull final Predicate<PlayerPerk> filter) {
        lock.readLock().lock();
        try {
            return perks.values().stream()
                    .filter(filter)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Updates a PlayerPerk and marks it as dirty.
     *
     * @param playerPerk the PlayerPerk to update
     */
    public void updatePerk(@NotNull final PlayerPerk playerPerk) {
        lock.writeLock().lock();
        try {
            Long perkId = playerPerk.getPerk().getId();
            perks.put(perkId, playerPerk);
            dirtyPerks.add(perkId);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets all dirty PlayerPerks that need persistence.
     *
     * @return list of dirty PlayerPerks
     */
    public List<PlayerPerk> getDirtyPerks() {
        lock.readLock().lock();
        try {
            return dirtyPerks.stream()
                    .map(perks::get)
                    .filter(perk -> perk != null)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Clears dirty flags after successful persistence.
     */
    public void clearDirtyFlags() {
        lock.writeLock().lock();
        try {
            dirtyPerks.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Adds a new PlayerPerk to the cache.
     *
     * @param playerPerk the PlayerPerk to add
     */
    public void addPerk(@NotNull final PlayerPerk playerPerk) {
        lock.writeLock().lock();
        try {
            Long perkId = playerPerk.getPerk().getId();
            perks.put(perkId, playerPerk);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Removes a PlayerPerk from the cache.
     *
     * @param perkId the perk ID to remove
     */
    public void removePerk(@NotNull final Long perkId) {
        lock.writeLock().lock();
        try {
            perks.remove(perkId);
            dirtyPerks.remove(perkId);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets the timestamp when this cache was loaded.
     *
     * @return timestamp in milliseconds
     */
    public long getLoadedAt() {
        return loadedAt;
    }
    
    /**
     * Gets the number of perks in this cache.
     *
     * @return perk count
     */
    public int size() {
        return perks.size();
    }
    
    /**
     * Gets the number of dirty perks.
     *
     * @return dirty perk count
     */
    public int dirtyCount() {
        return dirtyPerks.size();
    }
}
