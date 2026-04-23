package de.jexcellence.core.stats;

/**
 * Delivery priority. Higher priorities jump the queue and are retained
 * longest when backpressure drops entries.
 */
public enum StatisticPriority {
    LOW(3),
    NORMAL(2),
    HIGH(1),
    CRITICAL(0);

    private final int rank;

    StatisticPriority(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return this.rank;
    }
}
