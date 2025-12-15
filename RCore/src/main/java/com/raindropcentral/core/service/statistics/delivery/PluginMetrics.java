package com.raindropcentral.core.service.statistics.delivery;

/**
 * Plugin-specific metrics included with batch payloads.
 * Provides activity information about RCore and related plugins.
 *
 * @param activeQuestCount          number of currently active quests
 * @param completedQuestsInPeriod   quests completed since last delivery
 * @param economyTransactionCount   economy transactions since last delivery
 * @param economyTransactionVolume  total economy volume since last delivery
 * @param perkActivationCount       perk activations since last delivery
 * @param activePerkCount           currently active perks
 * @param activeBountyCount         currently active bounties (RDQ)
 * @param completedBountiesInPeriod bounties completed since last delivery (RDQ)
 *
 * @author JExcellence
 * @since 1.0.0
 */
public record PluginMetrics(
    int activeQuestCount,
    int completedQuestsInPeriod,
    int economyTransactionCount,
    double economyTransactionVolume,
    int perkActivationCount,
    int activePerkCount,
    int activeBountyCount,
    int completedBountiesInPeriod
) {

    /**
     * Creates a builder for PluginMetrics.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for PluginMetrics.
     */
    public static class Builder {
        private int activeQuestCount = 0;
        private int completedQuestsInPeriod = 0;
        private int economyTransactionCount = 0;
        private double economyTransactionVolume = 0.0;
        private int perkActivationCount = 0;
        private int activePerkCount = 0;
        private int activeBountyCount = 0;
        private int completedBountiesInPeriod = 0;

        public Builder activeQuestCount(int count) { this.activeQuestCount = count; return this; }
        public Builder completedQuestsInPeriod(int count) { this.completedQuestsInPeriod = count; return this; }
        public Builder economyTransactionCount(int count) { this.economyTransactionCount = count; return this; }
        public Builder economyTransactionVolume(double volume) { this.economyTransactionVolume = volume; return this; }
        public Builder perkActivationCount(int count) { this.perkActivationCount = count; return this; }
        public Builder activePerkCount(int count) { this.activePerkCount = count; return this; }
        public Builder activeBountyCount(int count) { this.activeBountyCount = count; return this; }
        public Builder completedBountiesInPeriod(int count) { this.completedBountiesInPeriod = count; return this; }

        public PluginMetrics build() {
            return new PluginMetrics(
                activeQuestCount, completedQuestsInPeriod,
                economyTransactionCount, economyTransactionVolume,
                perkActivationCount, activePerkCount,
                activeBountyCount, completedBountiesInPeriod
            );
        }
    }
}
