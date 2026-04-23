package de.jexcellence.core.stats;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatisticsQueueTest {

    private static StatisticEntry entry(String id, StatisticPriority priority) {
        return new StatisticEntry("test", id, null, 1.0, java.util.Map.of(), java.time.Instant.now(), priority);
    }

    @Test
    void drainsHighestPriorityFirst() {
        final DeliveryMetrics metrics = new DeliveryMetrics();
        final StatisticsQueue queue = new StatisticsQueue(10, metrics);

        queue.enqueue(entry("low", StatisticPriority.LOW));
        queue.enqueue(entry("critical", StatisticPriority.CRITICAL));
        queue.enqueue(entry("normal", StatisticPriority.NORMAL));
        queue.enqueue(entry("high", StatisticPriority.HIGH));

        final List<StatisticEntry> drained = queue.drain(4);
        assertEquals("critical", drained.get(0).identifier());
        assertEquals("high", drained.get(1).identifier());
        assertEquals("normal", drained.get(2).identifier());
        assertEquals("low", drained.get(3).identifier());
    }

    @Test
    void preservesFifoWithinSamePriority() {
        final DeliveryMetrics metrics = new DeliveryMetrics();
        final StatisticsQueue queue = new StatisticsQueue(10, metrics);

        for (int i = 0; i < 5; i++) {
            queue.enqueue(entry("n" + i, StatisticPriority.NORMAL));
        }

        final List<StatisticEntry> drained = queue.drain(5);
        for (int i = 0; i < 5; i++) {
            assertEquals("n" + i, drained.get(i).identifier());
        }
    }

    @Test
    void evictsLowerPriorityWhenFull() {
        final DeliveryMetrics metrics = new DeliveryMetrics();
        final StatisticsQueue queue = new StatisticsQueue(2, metrics);

        queue.enqueue(entry("low", StatisticPriority.LOW));
        queue.enqueue(entry("normal", StatisticPriority.NORMAL));
        queue.enqueue(entry("high", StatisticPriority.HIGH));

        assertEquals(2, queue.size());
        assertEquals(1L, metrics.dropped());
        final List<StatisticEntry> drained = queue.drain(2);
        assertEquals("high", drained.get(0).identifier());
        assertEquals("normal", drained.get(1).identifier());
    }

    @Test
    void dropsIncomingWhenNoLowerPriorityAvailable() {
        final DeliveryMetrics metrics = new DeliveryMetrics();
        final StatisticsQueue queue = new StatisticsQueue(1, metrics);

        queue.enqueue(entry("critical", StatisticPriority.CRITICAL));
        queue.enqueue(entry("low", StatisticPriority.LOW));

        assertEquals(1, queue.size());
        assertEquals(1L, metrics.dropped());
        assertEquals("critical", queue.drain(1).get(0).identifier());
    }
}
