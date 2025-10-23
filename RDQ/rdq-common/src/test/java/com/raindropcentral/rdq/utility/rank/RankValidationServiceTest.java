package com.raindropcentral.rdq.utility.rank;

import com.raindropcentral.rdq.config.ranks.rank.RankSection;
import com.raindropcentral.rdq.config.ranks.ranktree.RankTreeSection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RankValidationServiceTest {

    private final RankValidationService service = new RankValidationService();
    private final Executor executor = Runnable::run;

    @Test
    void validateConfigurationsAsyncCompletesForValidConfigurations() {
        final RankTreeSection firstTree = mock(RankTreeSection.class);
        when(firstTree.getPrerequisiteRankTrees()).thenReturn(List.of("tree-b"));
        when(firstTree.getUnlockedRankTrees()).thenReturn(List.of("tree-b"));
        when(firstTree.getConnectedRankTrees()).thenReturn(List.of());

        final RankTreeSection secondTree = mock(RankTreeSection.class);
        when(secondTree.getPrerequisiteRankTrees()).thenReturn(List.of());
        when(secondTree.getUnlockedRankTrees()).thenReturn(List.of("tree-a"));
        when(secondTree.getConnectedRankTrees()).thenReturn(List.of("tree-a"));

        final RankSection firstRank = mock(RankSection.class);
        when(firstRank.getPreviousRanks()).thenReturn(List.of());
        when(firstRank.getNextRanks()).thenReturn(List.of("rank-2"));

        final RankSection secondRank = mock(RankSection.class);
        when(secondRank.getPreviousRanks()).thenReturn(List.of("rank-1"));
        when(secondRank.getNextRanks()).thenReturn(List.of());

        final RankSection thirdRank = mock(RankSection.class);
        when(thirdRank.getPreviousRanks()).thenReturn(List.of());
        when(thirdRank.getNextRanks()).thenReturn(List.of());

        final RankSystemState state = RankSystemState.builder()
                .rankTreeSections(Map.of(
                        "tree-a", firstTree,
                        "tree-b", secondTree
                ))
                .rankSections(Map.of(
                        "tree-a", Map.of(
                                "rank-1", firstRank,
                                "rank-2", secondRank
                        ),
                        "tree-b", Map.of(
                                "rank-3", thirdRank
                        )
                ))
                .build();

        final CompletableFuture<Void> future = service.validateConfigurationsAsync(state, executor);

        assertDoesNotThrow(future::join);
    }

    @Test
    void validateConfigurationsAsyncFailsWhenPrerequisiteTreeMissing() {
        final RankTreeSection tree = mock(RankTreeSection.class);
        when(tree.getPrerequisiteRankTrees()).thenReturn(List.of("missing-tree"));
        when(tree.getUnlockedRankTrees()).thenReturn(List.of());
        when(tree.getConnectedRankTrees()).thenReturn(List.of());

        final RankSystemState state = RankSystemState.builder()
                .rankTreeSections(Map.of("tree-a", tree))
                .build();

        final CompletableFuture<Void> future = service.validateConfigurationsAsync(state, executor);

        final CompletionException exception = assertThrows(CompletionException.class, future::join);
        final Throwable cause = exception.getCause();
        assertInstanceOf(IllegalStateException.class, cause);
        assertEquals("Configuration validation failed", cause.getMessage());
    }

    @Test
    void validateConfigurationsAsyncFailsWhenRankReferencesMissing() {
        final RankTreeSection tree = mock(RankTreeSection.class);
        when(tree.getPrerequisiteRankTrees()).thenReturn(List.of());
        when(tree.getUnlockedRankTrees()).thenReturn(List.of());
        when(tree.getConnectedRankTrees()).thenReturn(List.of());

        final RankSection rank = mock(RankSection.class);
        when(rank.getPreviousRanks()).thenReturn(List.of("unknown-rank"));
        when(rank.getNextRanks()).thenReturn(List.of());

        final RankSystemState state = RankSystemState.builder()
                .rankTreeSections(Map.of("tree-a", tree))
                .rankSections(Map.of("tree-a", Map.of("rank-1", rank)))
                .build();

        final CompletableFuture<Void> future = service.validateConfigurationsAsync(state, executor);

        final CompletionException exception = assertThrows(CompletionException.class, future::join);
        final Throwable cause = exception.getCause();
        assertInstanceOf(IllegalStateException.class, cause);
        assertEquals("Configuration validation failed", cause.getMessage());
    }

    @Test
    void validateSystemAsyncFailsWhenCyclicPrerequisitesDetected() {
        final RankTreeSection firstTree = mock(RankTreeSection.class);
        when(firstTree.getPrerequisiteRankTrees()).thenReturn(List.of("tree-b"));
        when(firstTree.getUnlockedRankTrees()).thenReturn(List.of());
        when(firstTree.getConnectedRankTrees()).thenReturn(List.of());

        final RankTreeSection secondTree = mock(RankTreeSection.class);
        when(secondTree.getPrerequisiteRankTrees()).thenReturn(List.of("tree-a"));
        when(secondTree.getUnlockedRankTrees()).thenReturn(List.of());
        when(secondTree.getConnectedRankTrees()).thenReturn(List.of());

        final RankSystemState state = RankSystemState.builder()
                .rankTreeSections(Map.of(
                        "tree-a", firstTree,
                        "tree-b", secondTree
                ))
                .build();

        final CompletableFuture<Void> future = service.validateSystemAsync(state, executor);

        final CompletionException exception = assertThrows(CompletionException.class, future::join);
        final Throwable cause = exception.getCause();
        assertInstanceOf(IllegalStateException.class, cause);
        assertEquals("System validation failed", cause.getMessage());
    }
}
