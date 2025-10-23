package com.raindropcentral.rdq.utility.rank;

import com.raindropcentral.rdq.config.ranks.rank.RankSection;
import com.raindropcentral.rdq.config.ranks.ranktree.RankTreeSection;
import com.raindropcentral.rdq.config.ranks.system.RankSystemSection;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class RankSystemStateTest {

    @Test
    void emptyStateProvidesIsolatedCollections() {
        final RankSystemState first = RankSystemState.empty();

        assertTrue(first.rankTreeSections().isEmpty(), "Empty state must expose empty rank tree sections");
        assertTrue(first.rankSections().isEmpty(), "Empty state must expose empty rank sections");
        assertTrue(first.rankTrees().isEmpty(), "Empty state must expose empty rank trees");
        assertTrue(first.ranks().isEmpty(), "Empty state must expose empty ranks");
        assertNull(first.defaultRank(), "Empty state must not expose a default rank");

        final RankSystemState second = RankSystemState.empty();

        assertNotSame(first.rankTreeSections(), second.rankTreeSections(), "Rank tree maps must not be shared across empty states");
        assertNotSame(first.rankSections(), second.rankSections(), "Rank section maps must not be shared across empty states");
        assertNotSame(first.rankTrees(), second.rankTrees(), "Rank tree entity maps must not be shared across empty states");
        assertNotSame(first.ranks(), second.ranks(), "Rank entity maps must not be shared across empty states");

        first.rankTreeSections().put("alpha", mock(RankTreeSection.class));
        first.rankSections().put("alpha", new HashMap<>());
        first.rankTrees().put("alpha", mock(RRankTree.class));
        first.ranks().put("alpha", new HashMap<>());

        assertEquals(1, first.rankTreeSections().size(), "Mutating the retrieved rank tree map should affect only that instance");
        assertEquals(1, first.rankSections().size(), "Mutating the retrieved rank section map should affect only that instance");
        assertEquals(1, first.rankTrees().size(), "Mutating the retrieved rank tree entity map should affect only that instance");
        assertEquals(1, first.ranks().size(), "Mutating the retrieved rank entity map should affect only that instance");

        assertTrue(second.rankTreeSections().isEmpty(), "Mutations must not leak into separately created empty states");
        assertTrue(second.rankSections().isEmpty(), "Mutations must not leak into separately created empty states");
        assertTrue(second.rankTrees().isEmpty(), "Mutations must not leak into separately created empty states");
        assertTrue(second.ranks().isEmpty(), "Mutations must not leak into separately created empty states");
        assertNull(second.defaultRank(), "Mutating another state must not assign a default rank here");
    }

    @Test
    void builderUsesProvidedReferencesAndAllowsDefaultRankMutation() {
        final RankSystemSection systemSection = mock(RankSystemSection.class);

        final Map<String, RankTreeSection> rankTreeSections = new HashMap<>();
        final RankTreeSection treeSection = mock(RankTreeSection.class);
        rankTreeSections.put("ascension", treeSection);

        final Map<String, Map<String, RankSection>> rankSections = new HashMap<>();
        final Map<String, RankSection> nestedRankSections = new HashMap<>();
        final RankSection rankSection = mock(RankSection.class);
        nestedRankSections.put("novice", rankSection);
        rankSections.put("ascension", nestedRankSections);

        final Map<String, RRankTree> rankTrees = new HashMap<>();
        final RRankTree rankTree = mock(RRankTree.class);
        rankTrees.put("ascension", rankTree);

        final Map<String, Map<String, RRank>> ranks = new HashMap<>();
        final Map<String, RRank> nestedRanks = new HashMap<>();
        final RRank rank = mock(RRank.class);
        nestedRanks.put("novice", rank);
        ranks.put("ascension", nestedRanks);

        final RRank defaultRank = mock(RRank.class);

        final RankSystemState state = RankSystemState.builder()
                .rankSystemSection(systemSection)
                .rankTreeSections(rankTreeSections)
                .rankSections(rankSections)
                .rankTrees(rankTrees)
                .ranks(ranks)
                .defaultRank(defaultRank)
                .build();

        assertSame(systemSection, state.rankSystemSection(), "Builder must retain the provided system section reference");
        assertSame(rankTreeSections, state.rankTreeSections(), "Builder must retain the provided rank tree sections map");
        assertSame(rankSections, state.rankSections(), "Builder must retain the provided rank sections map");
        assertSame(rankTrees, state.rankTrees(), "Builder must retain the provided rank tree entities map");
        assertSame(ranks, state.ranks(), "Builder must retain the provided rank entities map");
        assertSame(defaultRank, state.defaultRank(), "Builder must retain the provided default rank");

        final RRank newDefaultRank = mock(RRank.class);
        state.setDefaultRank(newDefaultRank);

        assertSame(newDefaultRank, state.defaultRank(), "setDefaultRank should update the stored default rank reference");
    }

    @Test
    void builderResetsCollectionsWhenNullInputsProvided() {
        final RankSystemState state = RankSystemState.builder()
                .rankTreeSections(Map.of("tree", mock(RankTreeSection.class)))
                .rankSections(Map.of("tree", Map.of("rank", mock(RankSection.class))))
                .rankTrees(Map.of("tree", mock(RRankTree.class)))
                .ranks(Map.of("tree", Map.of("rank", mock(RRank.class))))
                .rankTreeSections(null)
                .rankSections(null)
                .rankTrees(null)
                .ranks(null)
                .build();

        assertTrue(state.rankTreeSections().isEmpty(), "Null input should reset rank tree sections to an empty map");
        assertTrue(state.rankSections().isEmpty(), "Null input should reset rank sections to an empty map");
        assertTrue(state.rankTrees().isEmpty(), "Null input should reset rank trees to an empty map");
        assertTrue(state.ranks().isEmpty(), "Null input should reset ranks to an empty map");
        assertNull(state.defaultRank(), "Default rank should remain unset when not provided");
    }
}
