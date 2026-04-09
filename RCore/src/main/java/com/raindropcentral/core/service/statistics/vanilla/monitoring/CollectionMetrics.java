/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.core.service.statistics.vanilla.monitoring;

import com.raindropcentral.core.service.statistics.vanilla.CollectionStatistics;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks metrics for vanilla statistic collection operations.
 * <p>
 * This class maintains counters for collection operations, statistics collected,
 * collection duration, cache size, and cache hit rate. All operations are thread-safe
 * using atomic variables.
 * <p>
 * Metrics tracked:
 * <ul>
 *   <li>Total number of collection operations</li>
 *   <li>Total statistics collected</li>
 *   <li>Total collection duration in milliseconds</li>
 *   <li>Current cache size (number of cached players)</li>
 *   <li>Cache hits and misses for hit rate calculation</li>
 * </ul>
 */
public class CollectionMetrics {
    
    private final AtomicLong totalCollections = new AtomicLong(0);
    private final AtomicLong totalStatistics = new AtomicLong(0);
    private final AtomicLong totalDuration = new AtomicLong(0);
    private final AtomicInteger cacheSize = new AtomicInteger(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    
    /**
     * Records a completed collection operation.
     *
     * @param statisticsCollected the number of statistics collected in this operation
     * @param durationMs the duration of the collection in milliseconds
     */
    public void recordCollection(int statisticsCollected, long durationMs) {
        totalCollections.incrementAndGet();
        totalStatistics.addAndGet(statisticsCollected);
        totalDuration.addAndGet(durationMs);
    }
    
    /**
     * Updates the current cache size.
     *
     * @param size the current number of players in the cache
     */
    public void updateCacheSize(int size) {
        cacheSize.set(size);
    }
    
    /**
     * Records a cache hit (player data found in cache).
     */
    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }
    
    /**
     * Records a cache miss (player data not found in cache).
     */
    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }
    
    /**
     * Gets the total number of collection operations performed.
     *
     * @return the total collection count
     */
    public long getTotalCollections() {
        return totalCollections.get();
    }
    
    /**
     * Gets the total number of statistics collected across all operations.
     *
     * @return the total statistics count
     */
    public long getTotalStatistics() {
        return totalStatistics.get();
    }
    
    /**
     * Gets the total duration of all collection operations in milliseconds.
     *
     * @return the total duration in milliseconds
     */
    public long getTotalDuration() {
        return totalDuration.get();
    }
    
    /**
     * Gets the current cache size (number of cached players).
     *
     * @return the cache size
     */
    public int getCacheSize() {
        return cacheSize.get();
    }
    
    /**
     * Calculates the cache hit rate as a percentage.
     *
     * @return the cache hit rate (0.0 to 100.0), or 0.0 if no cache operations
     */
    public double getCacheHitRate() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        
        if (total == 0) {
            return 0.0;
        }
        
        return (hits * 100.0) / total;
    }
    
    /**
     * Calculates the average collection duration in milliseconds.
     *
     * @return the average duration, or 0 if no collections have been performed
     */
    public long getAverageDuration() {
        long collections = totalCollections.get();
        if (collections == 0) {
            return 0;
        }
        return totalDuration.get() / collections;
    }
    
    /**
     * Gets a snapshot of current collection statistics.
     *
     * @return a {@link CollectionStatistics} record containing current metrics
     */
    public CollectionStatistics getStatistics() {
        return new CollectionStatistics(
            totalCollections.get(),
            totalStatistics.get(),
            getAverageDuration(),
            cacheSize.get()
        );
    }
    
    /**
     * Resets all metrics to zero.
     * <p>
     * This method is primarily intended for testing purposes.
     */
    public void reset() {
        totalCollections.set(0);
        totalStatistics.set(0);
        totalDuration.set(0);
        cacheSize.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
    }
}
