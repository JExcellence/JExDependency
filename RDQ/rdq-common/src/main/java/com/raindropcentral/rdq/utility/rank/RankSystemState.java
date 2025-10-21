package com.raindropcentral.rdq.utility.rank;

import com.raindropcentral.rdq.config.ranks.rank.RankSection;
import com.raindropcentral.rdq.config.ranks.ranktree.RankTreeSection;
import com.raindropcentral.rdq.config.ranks.system.RankSystemSection;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the cached state of the rank system, including both configuration sections and
 * materialized database entities required for runtime operations.
 *
 * <p>The state bundles the raw configuration definitions for rank trees and ranks alongside the
 * resolved {@link RRankTree} and {@link RRank} instances that are loaded from the database. The
 * combination allows the calling context to quickly evaluate rank metadata without repeatedly
 * querying the configuration or persistence layers.</p>
 *
 * <p>The builder should be used when consolidating state from multiple sources, while
 * {@link #empty()} provides a clean baseline for scenarios where the configuration is not
 * available.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
final class RankSystemState {

    private final @Nullable RankSystemSection rankSystemSection;
    private final @NotNull Map<String, RankTreeSection> rankTreeSections;
    private final @NotNull Map<String, Map<String, RankSection>> rankSections;

    private final @NotNull Map<String, RRankTree> rankTrees;
    private final @NotNull Map<String, Map<String, RRank>> ranks;

    private volatile @Nullable RRank defaultRank;

    /**
     * Creates a new state instance with explicit configuration sections and database projections.
     *
     * @param rankSystemSection the root system configuration, or {@code null} when no system is defined
     * @param rankTreeSections  the collection of configured rank trees keyed by tree identifier
     * @param rankSections      the configured rank definitions mapped by tree then rank identifier
     * @param rankTrees         the resolved rank tree entities keyed by tree identifier
     * @param ranks             the resolved rank entities mapped by tree then rank identifier
     * @param defaultRank       the default rank entity, or {@code null} when the system defines none
     */
    private RankSystemState(@Nullable RankSystemSection rankSystemSection,
                            @NotNull Map<String, RankTreeSection> rankTreeSections,
                            @NotNull Map<String, Map<String, RankSection>> rankSections,
                            @NotNull Map<String, RRankTree> rankTrees,
                            @NotNull Map<String, Map<String, RRank>> ranks,
                            @Nullable RRank defaultRank) {
        this.rankSystemSection = rankSystemSection;
        this.rankTreeSections = rankTreeSections;
        this.rankSections = rankSections;
        this.rankTrees = rankTrees;
        this.ranks = ranks;
        this.defaultRank = defaultRank;
    }

    /**
     * Creates an empty state with no configuration or rank data.
     *
     * @return a state instance with empty maps and no default rank
     */
    static @NotNull RankSystemState empty() {
        return new RankSystemState(null, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), null);
    }

    /**
     * Creates a builder for progressively constructing a state instance.
     *
     * @return a new builder instance
     */
    static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Gets the configuration for the rank system.
     *
     * @return the rank system section or {@code null} when the configuration is missing
     */
    @Nullable RankSystemSection rankSystemSection() {
        return rankSystemSection;
    }

    /**
     * Gets the configured rank trees.
     *
     * @return a map of rank tree identifiers to their configuration sections
     */
    @NotNull Map<String, RankTreeSection> rankTreeSections() {
        return rankTreeSections;
    }

    /**
     * Gets the configured rank definitions.
     *
     * @return a map of rank identifiers grouped by their rank tree
     */
    @NotNull Map<String, Map<String, RankSection>> rankSections() {
        return rankSections;
    }

    /**
     * Gets the resolved rank tree entities.
     *
     * @return a map of rank tree identifiers to their database entities
     */
    @NotNull Map<String, RRankTree> rankTrees() {
        return rankTrees;
    }

    /**
     * Gets the resolved rank entities grouped by rank tree.
     *
     * @return a map of rank entities keyed by tree and rank identifiers
     */
    @NotNull Map<String, Map<String, RRank>> ranks() {
        return ranks;
    }

    /**
     * Gets the default rank entity, if any.
     *
     * @return the default rank or {@code null} when not defined
     */
    @Nullable RRank defaultRank() {
        return defaultRank;
    }

    /**
     * Updates the default rank associated with this state.
     *
     * @param defaultRank the rank to set as default, or {@code null} to clear the default
     */
    void setDefaultRank(@Nullable RRank defaultRank) {
        this.defaultRank = defaultRank;
    }

    /**
     * Builder for aggregating configuration and database state into a {@link RankSystemState}.
     */
    static final class Builder {
        private RankSystemSection rankSystemSection;
        private Map<String, RankTreeSection> rankTreeSections = new HashMap<>();
        private Map<String, Map<String, RankSection>> rankSections = new HashMap<>();
        private Map<String, RRankTree> rankTrees = new HashMap<>();
        private Map<String, Map<String, RRank>> ranks = new HashMap<>();
        private RRank defaultRank;

        /**
         * Sets the root rank system configuration for the resulting state.
         *
         * @param section the rank system configuration
         * @return this builder instance
         */
        Builder rankSystemSection(RankSystemSection section) {
            this.rankSystemSection = section;
            return this;
        }

        /**
         * Sets the collection of rank tree configurations.
         *
         * @param sections the configured rank trees, or {@code null} to clear the collection
         * @return this builder instance
         */
        Builder rankTreeSections(Map<String, RankTreeSection> sections) {
            this.rankTreeSections = sections != null ? sections : new HashMap<>();
            return this;
        }

        /**
         * Sets the configured rank definitions keyed by tree and rank identifier.
         *
         * @param sections the configured ranks, or {@code null} to clear the collection
         * @return this builder instance
         */
        Builder rankSections(Map<String, Map<String, RankSection>> sections) {
            this.rankSections = sections != null ? sections : new HashMap<>();
            return this;
        }

        /**
         * Sets the resolved rank tree entities.
         *
         * @param map the rank tree entities, or {@code null} to clear the collection
         * @return this builder instance
         */
        Builder rankTrees(Map<String, RRankTree> map) {
            this.rankTrees = map != null ? map : new HashMap<>();
            return this;
        }

        /**
         * Sets the resolved rank entities grouped by their rank tree.
         *
         * @param map the rank entities, or {@code null} to clear the collection
         * @return this builder instance
         */
        Builder ranks(Map<String, Map<String, RRank>> map) {
            this.ranks = map != null ? map : new HashMap<>();
            return this;
        }

        /**
         * Sets the default rank entity for the resulting state.
         *
         * @param rank the default rank entity
         * @return this builder instance
         */
        Builder defaultRank(RRank rank) {
            this.defaultRank = rank;
            return this;
        }

        /**
         * Builds a new {@link RankSystemState} with the configured values.
         *
         * @return the constructed state
         */
        RankSystemState build() {
            return new RankSystemState(rankSystemSection, rankTreeSections, rankSections, rankTrees, ranks, defaultRank);
        }
    }
}