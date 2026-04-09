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
import com.raindropcentral.core.service.statistics.vanilla.VanillaStatisticCollectionService;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoint for vanilla statistics collection system.
 * <p>
 * Provides health status information for monitoring systems, including:
 * <ul>
 *   <li>Service initialization status</li>
 *   <li>Collection performance metrics</li>
 *   <li>Cache health indicators</li>
 *   <li>Overall system health status</li>
 * </ul>
 * <p>
 * Health status is determined by:
 * <ul>
 *   <li>HEALTHY: Service initialized, average collection time &lt; 100ms</li>
 *   <li>DEGRADED: Service initialized, average collection time &gt;= 100ms</li>
 *   <li>UNHEALTHY: Service not initialized</li>
 * </ul>
 */
public class VanillaStatisticsHealthCheck {
    
    /**
     * Health status levels.
     */
    public enum HealthStatus {
        /** System is operating normally. */
        HEALTHY,
        /** System is operational but performance is degraded. */
        DEGRADED,
        /** System is not operational. */
        UNHEALTHY
    }
    
    private static final long DEGRADED_THRESHOLD_MS = 100;
    
    private final VanillaStatisticCollectionService collectionService;
    
    /**
     * Creates a new health check endpoint.
     *
     * @param collectionService the vanilla statistic collection service
     */
    public VanillaStatisticsHealthCheck(VanillaStatisticCollectionService collectionService) {
        this.collectionService = collectionService;
    }
    
    /**
     * Performs a health check and returns the current status.
     *
     * @return a map containing health check results
     */
    public Map<String, Object> checkHealth() {
        Map<String, Object> health = new HashMap<>();
        
        // Check if service is initialized
        boolean initialized = collectionService.isInitialized();
        health.put("initialized", initialized);
        
        if (!initialized) {
            health.put("status", HealthStatus.UNHEALTHY.name());
            health.put("message", "Vanilla statistics collection service not initialized");
            return health;
        }
        
        // Get collection statistics
        CollectionStatistics stats = collectionService.getStatistics();
        
        // Determine health status based on performance
        HealthStatus status;
        String message;
        
        if (stats.averageDuration() < DEGRADED_THRESHOLD_MS) {
            status = HealthStatus.HEALTHY;
            message = "Collection performance is good";
        } else {
            status = HealthStatus.DEGRADED;
            message = "Collection performance is degraded (avg " + stats.averageDuration() + "ms)";
        }
        
        health.put("status", status.name());
        health.put("message", message);
        
        // Add detailed metrics
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalCollections", stats.totalCollections());
        metrics.put("totalStatistics", stats.totalStatistics());
        metrics.put("averageDurationMs", stats.averageDuration());
        metrics.put("cacheSize", stats.cacheSize());
        
        health.put("metrics", metrics);
        
        // Add performance indicators
        Map<String, Object> performance = new HashMap<>();
        performance.put("averageDurationMs", stats.averageDuration());
        performance.put("threshold", DEGRADED_THRESHOLD_MS);
        performance.put("withinThreshold", stats.averageDuration() < DEGRADED_THRESHOLD_MS);
        
        health.put("performance", performance);
        
        return health;
    }
    
    /**
     * Gets the current health status.
     *
     * @return the health status
     */
    public HealthStatus getStatus() {
        if (!collectionService.isInitialized()) {
            return HealthStatus.UNHEALTHY;
        }
        
        CollectionStatistics stats = collectionService.getStatistics();
        
        if (stats.averageDuration() < DEGRADED_THRESHOLD_MS) {
            return HealthStatus.HEALTHY;
        } else {
            return HealthStatus.DEGRADED;
        }
    }
    
    /**
     * Checks if the system is healthy.
     *
     * @return true if status is HEALTHY, false otherwise
     */
    public boolean isHealthy() {
        return getStatus() == HealthStatus.HEALTHY;
    }
    
    /**
     * Gets a simple health check result.
     *
     * @return a map with status and message
     */
    public Map<String, String> getSimpleHealth() {
        Map<String, String> health = new HashMap<>();
        
        HealthStatus status = getStatus();
        health.put("status", status.name());
        
        switch (status) {
            case HEALTHY -> health.put("message", "Vanilla statistics collection is healthy");
            case DEGRADED -> {
                CollectionStatistics stats = collectionService.getStatistics();
                health.put("message", "Performance degraded (avg " + stats.averageDuration() + "ms)");
            }
            case UNHEALTHY -> health.put("message", "Service not initialized");
        }
        
        return health;
    }
}
