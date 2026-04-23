package de.jexcellence.core.stats;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Counters maintained by {@link DeliveryEngine}. Thread-safe; intended for
 * monitoring only — not a general-purpose metrics facade.
 */
public final class DeliveryMetrics {

    private final AtomicLong enqueued = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private final AtomicLong batches = new AtomicLong();
    private final AtomicLong delivered = new AtomicLong();
    private final AtomicLong retries = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();
    private final AtomicLong spooled = new AtomicLong();

    void onEnqueued() {
        this.enqueued.incrementAndGet();
    }

    void onDropped() {
        this.dropped.incrementAndGet();
    }

    void onBatchBuilt() {
        this.batches.incrementAndGet();
    }

    void onDelivered(int entries) {
        this.delivered.addAndGet(entries);
    }

    void onRetry() {
        this.retries.incrementAndGet();
    }

    void onFailure() {
        this.failures.incrementAndGet();
    }

    void onSpooled() {
        this.spooled.incrementAndGet();
    }

    public long enqueued() {
        return this.enqueued.get();
    }

    public long dropped() {
        return this.dropped.get();
    }

    public long batches() {
        return this.batches.get();
    }

    public long delivered() {
        return this.delivered.get();
    }

    public long retries() {
        return this.retries.get();
    }

    public long failures() {
        return this.failures.get();
    }

    public long spooled() {
        return this.spooled.get();
    }
}
