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

final class RankSystemState {

    private final @Nullable RankSystemSection rankSystemSection;
    private final @NotNull Map<String, RankTreeSection> rankTreeSections;
    private final @NotNull Map<String, Map<String, RankSection>> rankSections;

    private final @NotNull Map<String, RRankTree> rankTrees;
    private final @NotNull Map<String, Map<String, RRank>> ranks;

    private volatile @Nullable RRank defaultRank;

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

    static @NotNull RankSystemState empty() {
        return new RankSystemState(null, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), null);
    }

    static @NotNull Builder builder() {
        return new Builder();
    }

    @Nullable RankSystemSection rankSystemSection() {
        return rankSystemSection;
    }

    @NotNull Map<String, RankTreeSection> rankTreeSections() {
        return rankTreeSections;
    }

    @NotNull Map<String, Map<String, RankSection>> rankSections() {
        return rankSections;
    }

    @NotNull Map<String, RRankTree> rankTrees() {
        return rankTrees;
    }

    @NotNull Map<String, Map<String, RRank>> ranks() {
        return ranks;
    }

    @Nullable RRank defaultRank() {
        return defaultRank;
    }

    void setDefaultRank(@Nullable RRank defaultRank) {
        this.defaultRank = defaultRank;
    }

    static final class Builder {
        private RankSystemSection rankSystemSection;
        private Map<String, RankTreeSection> rankTreeSections = new HashMap<>();
        private Map<String, Map<String, RankSection>> rankSections = new HashMap<>();
        private Map<String, RRankTree> rankTrees = new HashMap<>();
        private Map<String, Map<String, RRank>> ranks = new HashMap<>();
        private RRank defaultRank;

        Builder rankSystemSection(RankSystemSection section) {
            this.rankSystemSection = section;
            return this;
        }

        Builder rankTreeSections(Map<String, RankTreeSection> sections) {
            this.rankTreeSections = sections != null ? sections : new HashMap<>();
            return this;
        }

        Builder rankSections(Map<String, Map<String, RankSection>> sections) {
            this.rankSections = sections != null ? sections : new HashMap<>();
            return this;
        }

        Builder rankTrees(Map<String, RRankTree> map) {
            this.rankTrees = map != null ? map : new HashMap<>();
            return this;
        }

        Builder ranks(Map<String, Map<String, RRank>> map) {
            this.ranks = map != null ? map : new HashMap<>();
            return this;
        }

        Builder defaultRank(RRank rank) {
            this.defaultRank = rank;
            return this;
        }

        RankSystemState build() {
            return new RankSystemState(rankSystemSection, rankTreeSections, rankSections, rankTrees, ranks, defaultRank);
        }
    }
}