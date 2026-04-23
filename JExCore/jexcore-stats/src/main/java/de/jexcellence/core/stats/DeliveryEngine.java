package de.jexcellence.core.stats;

import de.jexcellence.jexplatform.logging.JExLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Drains the queue, builds batches, sends over the transport, retries,
 * and spools to disk on fatal failure. Runs on a single asynchronous
 * Bukkit task; bookkeeping is thread-safe.
 */
final class DeliveryEngine {

    private final JavaPlugin plugin;
    private final StatisticsConfig config;
    private final StatisticsQueue queue;
    private final BatchBuilder builder;
    private final UUID serverUuid;
    private final CentralTransport transport;
    private final RateLimiter rateLimiter;
    private final RetryPolicy retryPolicy;
    private final OfflineSpool spool;
    private final DeliveryMetrics metrics;
    private final JExLogger logger;

    private BukkitTask task;

    DeliveryEngine(
            @NotNull JavaPlugin plugin,
            @NotNull StatisticsConfig config,
            @NotNull UUID serverUuid,
            @NotNull StatisticsQueue queue,
            @NotNull CentralTransport transport,
            @NotNull DeliveryMetrics metrics,
            @NotNull JExLogger logger
    ) throws IOException {
        this.plugin = plugin;
        this.config = config;
        this.serverUuid = serverUuid;
        this.queue = queue;
        this.transport = transport;
        this.rateLimiter = new RateLimiter(config.rateLimitPerSecond(), config.rateLimitPerSecond());
        this.retryPolicy = new RetryPolicy(config.retryInitial(), config.retryMax(), config.retryMaxAttempts());
        this.metrics = metrics;
        this.logger = logger;

        final PayloadSigner signer = config.hmacSecret() != null ? new PayloadSigner(config.hmacSecret()) : null;
        this.builder = new BatchBuilder(serverUuid, signer);
        this.spool = new OfflineSpool(plugin.getDataFolder().toPath().resolve(config.spoolDir()));
    }

    void start() {
        if (this.task != null) return;
        final long period = Math.max(1L, this.config.flushInterval().toMillis() / 50L);
        this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, this::tick, period, period);
    }

    void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
        flushAll();
        try {
            this.transport.close();
        } catch (final Exception ignored) {
        }
    }

    void flushAll() {
        while (this.queue.size() > 0) tick();
        replaySpool();
    }

    private void tick() {
        replaySpool();

        if (this.queue.size() == 0) return;
        if (!this.rateLimiter.tryAcquire()) return;

        final List<StatisticEntry> drained = this.queue.drain(this.config.maxBatchEntries());
        if (drained.isEmpty()) return;

        final BatchPayload batch = this.builder.build(drained);
        if (batch.bytes() > this.config.maxBatchBytes() && batch.size() > 1) {
            this.logger.warn("batch body {}B over limit {}B — splitting", batch.bytes(), this.config.maxBatchBytes());
            splitAndResend(drained);
            return;
        }
        this.metrics.onBatchBuilt();
        attempt(batch, 0);
    }

    private void splitAndResend(@NotNull List<StatisticEntry> entries) {
        final int mid = Math.max(1, entries.size() / 2);
        final BatchPayload first = this.builder.build(entries.subList(0, mid));
        final BatchPayload second = this.builder.build(entries.subList(mid, entries.size()));
        this.metrics.onBatchBuilt();
        this.metrics.onBatchBuilt();
        attempt(first, 0);
        attempt(second, 0);
    }

    private void attempt(@NotNull BatchPayload batch, int attempt) {
        final DeliveryResult result = this.transport.send(batch);
        if (result instanceof DeliveryResult.Success s) {
            this.metrics.onDelivered(batch.size());
            this.logger.debug("delivered batch {} ({} entries, HTTP {})", batch.batchId(), batch.size(), s.statusCode());
            return;
        }
        if (result instanceof DeliveryResult.Retryable r) {
            if (this.retryPolicy.hasAttemptsLeft(attempt)) {
                this.metrics.onRetry();
                final long delayTicks = Math.max(1L, this.retryPolicy.nextDelay(attempt).toMillis() / 50L);
                Bukkit.getScheduler().runTaskLaterAsynchronously(this.plugin, () -> attempt(batch, attempt + 1), delayTicks);
            } else {
                spoolBatch(batch, "retries exhausted: " + r.reason());
            }
            return;
        }
        if (result instanceof DeliveryResult.Fatal f) {
            spoolBatch(batch, "fatal: " + f.reason());
        }
    }

    private void spoolBatch(@NotNull BatchPayload batch, @NotNull String reason) {
        this.metrics.onFailure();
        try {
            this.spool.write(batch);
            this.metrics.onSpooled();
            this.logger.warn("batch {} spooled to disk ({})", batch.batchId(), reason);
        } catch (final IOException ex) {
            this.logger.error("batch {} lost — spool write failed: {}", batch.batchId(), ex.getMessage());
        }
    }

    private void replaySpool() {
        final List<Path> pending;
        try {
            pending = this.spool.pending();
        } catch (final IOException ex) {
            this.logger.error("spool list failed: {}", ex.getMessage());
            return;
        }
        for (final Path path : pending) {
            if (!this.rateLimiter.tryAcquire()) return;
            try {
                final byte[] body = this.spool.read(path);
                final BatchPayload replay = new BatchPayload(
                        UUID.randomUUID(),
                        this.serverUuid,
                        java.time.Instant.now(),
                        List.of(),
                        body,
                        null
                );
                final DeliveryResult result = this.transport.send(replay);
                if (result instanceof DeliveryResult.Success) {
                    this.spool.delete(path);
                    this.logger.info("replayed spooled batch {}", path.getFileName());
                }
            } catch (final IOException | RuntimeException ex) {
                this.logger.debug("spool replay skipped for {}: {}", path.getFileName(), ex.getMessage());
            }
        }
    }
}
