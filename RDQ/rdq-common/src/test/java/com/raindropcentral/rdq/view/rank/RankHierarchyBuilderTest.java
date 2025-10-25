package com.raindropcentral.rdq.view.rank;

import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RankHierarchyBuilderTest {

    private static final String RANK_A = "rank-a";
    private static final String RANK_B = "rank-b";
    private static final String RANK_C = "rank-c";

    @Test
    void buildHierarchy_linksChildrenAndParentsBidirectionally() {
        final RRank rankA = mock(RRank.class);
        when(rankA.getIdentifier()).thenReturn(RANK_A);
        when(rankA.getNextRanks()).thenReturn(List.of(RANK_B, RANK_B));
        when(rankA.getPreviousRanks()).thenReturn(List.of());

        final RRank rankB = mock(RRank.class);
        when(rankB.getIdentifier()).thenReturn(RANK_B);
        when(rankB.getNextRanks()).thenReturn(List.of(RANK_C, RANK_C));
        when(rankB.getPreviousRanks()).thenReturn(List.of(RANK_A, RANK_A));

        final RRank rankC = mock(RRank.class);
        when(rankC.getIdentifier()).thenReturn(RANK_C);
        when(rankC.getNextRanks()).thenReturn(List.of());
        when(rankC.getPreviousRanks()).thenReturn(List.of(RANK_B, RANK_B, RANK_A, RANK_A));

        final RRankTree rankTree = mock(RRankTree.class);
        when(rankTree.getRanks()).thenReturn(List.of(rankA, rankB, rankC));

        final RankHierarchyBuilder builder = new RankHierarchyBuilder();
        final Map<String, RankNode> hierarchy = builder.buildHierarchy(rankTree);

        assertEquals(3, hierarchy.size(), "All ranks should be represented in the hierarchy");

        final RankNode nodeA = hierarchy.get(RANK_A);
        final RankNode nodeB = hierarchy.get(RANK_B);
        final RankNode nodeC = hierarchy.get(RANK_C);

        assertNotNull(nodeA, "Rank A node must be present");
        assertNotNull(nodeB, "Rank B node must be present");
        assertNotNull(nodeC, "Rank C node must be present");

        assertEquals(2, nodeA.getChildren().size(), "Rank A should link to ranks B and C once each");
        assertTrue(nodeA.getChildren().contains(nodeB), "Rank A should include rank B as a child");
        assertTrue(nodeA.getChildren().contains(nodeC), "Rank A should include rank C as a child");

        assertEquals(1, nodeB.getParents().size(), "Rank B should have a single parent (rank A)");
        assertSame(nodeA, nodeB.getParents().get(0), "Rank B's parent should be rank A");

        assertEquals(1, nodeB.getChildren().size(), "Rank B should have a single child (rank C)");
        assertSame(nodeC, nodeB.getChildren().get(0), "Rank B should link rank C as its child");

        assertEquals(2, nodeC.getParents().size(), "Rank C should be linked to both rank B and rank A");
        assertTrue(nodeC.getParents().contains(nodeB), "Rank C should include rank B as a parent");
        assertTrue(nodeC.getParents().contains(nodeA), "Rank C should include rank A as a parent");
    }

    @Test
    void buildHierarchy_skipsMissingReferencesGracefully() {
        final RRank rankA = mock(RRank.class);
        when(rankA.getIdentifier()).thenReturn(RANK_A);
        when(rankA.getNextRanks()).thenReturn(List.of("missing-child"));
        when(rankA.getPreviousRanks()).thenReturn(List.of("missing-parent"));

        final RRank rankB = mock(RRank.class);
        when(rankB.getIdentifier()).thenReturn(RANK_B);
        when(rankB.getNextRanks()).thenReturn(List.of());
        when(rankB.getPreviousRanks()).thenReturn(List.of("missing-parent"));

        final RRankTree rankTree = mock(RRankTree.class);
        when(rankTree.getRanks()).thenReturn(List.of(rankA, rankB));

        final RankHierarchyBuilder builder = new RankHierarchyBuilder();
        final Map<String, RankNode> hierarchy = builder.buildHierarchy(rankTree);

        assertEquals(2, hierarchy.size(), "Only the provided ranks should exist in the hierarchy");

        final RankNode nodeA = hierarchy.get(RANK_A);
        final RankNode nodeB = hierarchy.get(RANK_B);

        assertNotNull(nodeA, "Rank A node must be present");
        assertNotNull(nodeB, "Rank B node must be present");

        assertTrue(nodeA.getChildren().isEmpty(), "Rank A should not link to missing children");
        assertTrue(nodeA.getParents().isEmpty(), "Rank A should not link to missing parents");

        assertTrue(nodeB.getChildren().isEmpty(), "Rank B should not link to missing children");
        assertTrue(nodeB.getParents().isEmpty(), "Rank B should not link to missing parents");
    }
}
