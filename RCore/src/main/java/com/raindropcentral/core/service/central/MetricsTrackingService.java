package com.raindropcentral.core.service.central;

import com.raindropcentral.core.database.entity.central.RCentralServer;
import com.raindropcentral.core.database.repository.RCentralServerRepository;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for tracking metrics related to server authentication and heartbeats.
 *
 * <p>Provides counters and statistics for monitoring the health and performance of
 * the RaindropCentral integration. Metrics are tracked in-memory and can be
 * exported for monitoring systems.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.0
 */
public class MetricsTrackingService {

    private final RCentralServerRepository serverRepository;

    // Authentication metrics
    private final AtomicLong authSuccessCount = new AtomicLong(0);
    private final AtomicLong authFailureCount = new AtomicLong(0);
    private final AtomicLong authRateLimitCount = new AtomicLong(0);

    // Heartbeat metrics
    private final AtomicLong heartbeatProcessedCount = new AtomicLong(0);
    private final AtomicLong heartbeatFailureCount = new AtomicLong(0);
    private final AtomicLong totalHeartbeatProcessingTime = new AtomicLong(0);

    /**
     * Constructs a new MetricsTrackingService.
     *
     * @param serverRepository repository for querying server statistics
     * @throws NullPointerException if serverRepository is null
     */
    public MetricsTrackingService(final @NotNull RCentralServerRepository serverRepository) {
        this.serverRepository = Objects.requireNonNull(serverRepository, "serverRepository cannot be null");
    }

    /**
     * Records a successful authentication attempt.
     */
    public void recordAuthSuccess() {
        authSuccessCount.incrementAndGet();
    }

    /**
     * Records a failed authentication attempt.
     */
    public void recordAuthFailure() {
        authFailureCount.incrementAndGet();
    }

    /**
     * Records a rate-limited authentication attempt.
     */
    public void recordAuthRateLimit() {
        authRateLimitCount.incrementAndGet();
    }

    /**
     * Records a processed heartbeat with its processing time.
     *
     * @param processingTimeMillis the time taken to process the heartbeat in milliseconds
     */
    public void recordHeartbeatProcessed(final long processingTimeMillis) {
        heartbeatProcessedCount.incrementAndGet();
        totalHeartbeatProcessingTime.addAndGet(processingTimeMillis);
    }

    /**
     * Records a failed heartbeat processing attempt.
     */
    public void recordHeartbeatFailure() {
        heartbeatFailureCount.incrementAndGet();
    }

    /**
     * Gets the authentication success rate as a percentage.
     *
     * @return success rate between 0.0 and 1.0, or 0.0 if no attempts
     */
    public double getAuthSuccessRate() {
        final long total = authSuccessCount.get() + authFailureCount.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) authSuccessCount.get() / total;
    }

    /**
     * Gets the average heartbeat processing time in milliseconds.
     *
     * @return average processing time, or 0.0 if no heartbeats processed
     */
    public double getAverageHeartbeatProcessingTime() {
        final long count = heartbeatProcessedCount.get();
        if (count == 0) {
            return 0.0;
        }
        return (double) totalHeartbeatProcessingTime.get() / count;
    }

    /**
     * Gets the number of currently connected servers.
     *
     * @return CompletableFuture containing the count of connected servers
     */
    public CompletableFuture<Long> getConnectedServerCount() {
        return serverRepository.findAllAsync(0, Integer.MAX_VALUE)
                .thenApply(servers -> servers.stream()
                        .filter(server -> server.getConnectionStatus() == 
                                RCentralServer.ConnectionStatus.CONNECTED)
                        .count());
    }

    /**
     * Exports all metrics as a snapshot.
     *
     * @return MetricsSnapshot containing current metric values
     */
    public CompletableFuture<MetricsSnapshot> exportMetrics() {
        return getConnectedServerCount()
                .thenApply(connectedServers -> new MetricsSnapshot(
                        authSuccessCount.get(),
                        authFailureCount.get(),
                        authRateLimitCount.get(),
                        getAuthSuccessRate(),
                        heartbeatProcessedCount.get(),
                        heartbeatFailureCount.get(),
                        getAverageHeartbeatProcessingTime(),
                        connectedServers,
                        LocalDateTime.now()
                ));
    }

    /**
     * Resets all metrics counters.
 *
 * <p>Useful for periodic metric resets or testing.
     * </p>
     */
    public void resetMetrics() {
        authSuccessCount.set(0);
        authFailureCount.set(0);
        authRateLimitCount.set(0);
        heartbeatProcessedCount.set(0);
        heartbeatFailureCount.set(0);
        totalHeartbeatProcessingTime.set(0);
    }

    /**
     * Snapshot of metrics at a point in time.
     *
     * @param authSuccessCount              total successful authentications
     * @param authFailureCount              total failed authentications
     * @param authRateLimitCount            total rate-limited authentication attempts
     * @param authSuccessRate               authentication success rate (0.0 to 1.0)
     * @param heartbeatProcessedCount       total heartbeats processed
     * @param heartbeatFailureCount         total failed heartbeat processing attempts
     * @param averageHeartbeatProcessingTime average heartbeat processing time in milliseconds
     * @param connectedServerCount          number of currently connected servers
     * @param timestamp                     time when the snapshot was taken
     */
    public record MetricsSnapshot(
            long authSuccessCount,
            long authFailureCount,
            long authRateLimitCount,
            double authSuccessRate,
            long heartbeatProcessedCount,
            long heartbeatFailureCount,
            double averageHeartbeatProcessingTime,
            long connectedServerCount,
            @NotNull LocalDateTime timestamp
    ) {}
}
