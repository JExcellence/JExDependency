package de.jexcellence.core.stats;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bounded priority queue keyed by {@link StatisticPriority#rank()} + FIFO
 * tiebreaker. When full, the lowest-priority tail entry is evicted and
 * counted in {@link DeliveryMetrics#dropped()}.
 */
final class StatisticsQueue {

    private final int maxSize;
    private final DeliveryMetrics metrics;
    private final PriorityQueue<Node> heap;
    private final AtomicLong sequence = new AtomicLong();

    StatisticsQueue(int maxSize, @NotNull DeliveryMetrics metrics) {
        this.maxSize = maxSize;
        this.metrics = metrics;
        this.heap = new PriorityQueue<>((a, b) -> {
            final int byPriority = Integer.compare(a.entry().priority().rank(), b.entry().priority().rank());
            return byPriority != 0 ? byPriority : Long.compare(a.seq(), b.seq());
        });
    }

    synchronized void enqueue(@NotNull StatisticEntry entry) {
        if (this.heap.size() >= this.maxSize) {
            final Node worst = findLowestPriority();
            if (worst == null || worst.entry().priority().rank() <= entry.priority().rank()) {
                this.metrics.onDropped();
                return;
            }
            this.heap.remove(worst);
            this.metrics.onDropped();
        }
        this.heap.add(new Node(this.sequence.incrementAndGet(), entry));
        this.metrics.onEnqueued();
    }

    synchronized @NotNull List<StatisticEntry> drain(int limit) {
        final int n = Math.min(limit, this.heap.size());
        final StatisticEntry[] out = new StatisticEntry[n];
        for (int i = 0; i < n; i++) out[i] = this.heap.poll().entry();
        return List.of(out);
    }

    synchronized int size() {
        return this.heap.size();
    }

    private @Nullable Node findLowestPriority() {
        Node worst = null;
        for (final Node node : this.heap) {
            if (worst == null || node.entry().priority().rank() > worst.entry().priority().rank()) {
                worst = node;
            }
        }
        return worst;
    }

    private record Node(long seq, @NotNull StatisticEntry entry) {
    }
}
