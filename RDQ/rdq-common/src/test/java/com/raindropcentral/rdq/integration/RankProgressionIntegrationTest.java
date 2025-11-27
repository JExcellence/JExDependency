package com.raindropcentral.rdq.integration;

import com.raindropcentral.rdq.api.FreeRankService;
import com.raindropcentral.rdq.fixtures.TestData;
import com.raindropcentral.rdq.database.entity.rank.PlayerRank;
import com.raindropcentral.rdq.database.entity.rank.PlayerRankPath;
import com.raindropcentral.rdq.rank.repository.PlayerRankPathRepository;
import com.raindropcentral.rdq.rank.repository.PlayerRankRepository;
import com.raindropcentral.rdq.rank.repository.RankRepository;
import com.raindropcentral.rdq.rank.repository.RankTreeRepository;
import com.raindropcentral.rdq.rank.service.DefaultFreeRankService;
import com.raindropcentral.rdq.rank.service.RankRequirementChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Rank Progression Integration Tests")
class RankProgressionIntegrationTest {

    @Mock
    private RankTreeRepository treeRepository;

    @Mock
    private RankRepository rankRepository;

    @Mock
    private PlayerRankRepository playerRankRepository;

    @Mock
    private PlayerRankPathRepository pathRepository;

    @Mock
    private RankRequirementChecker requirementChecker;

    private FreeRankService rankService;

    @BeforeEach
    void setUp() {
        rankService = new DefaultFreeRankService(
            treeRepository,
            rankRepository,
            playerRankRepository,
            pathRepository,
            requirementChecker
        );
    }

    @Test
    @DisplayName("Full rank progression flow: start tree -> unlock ranks -> progress")
    void fullRankProgressionFlow() {
        var playerId = UUID.randomUUID();
        var tree = TestData.rankTree("warrior");
        var rank1 = TestData.rank("warrior", "warrior_1", 1);
        var rank2 = TestData.rank("warrior", "warrior_2", 2);
        var rank3 = TestData.rank("warrior", "warrior_3", 3);

        when(treeRepository.findAllEnabled()).thenReturn(List.of(tree));
        when(treeRepository.findById("warrior")).thenReturn(Optional.of(tree));
        when(rankRepository.findById("warrior_1")).thenReturn(Optional.of(rank1));
        when(rankRepository.findById("warrior_2")).thenReturn(Optional.of(rank2));
        when(rankRepository.findById("warrior_3")).thenReturn(Optional.of(rank3));
        when(rankRepository.findEnabledByTreeId("warrior")).thenReturn(List.of(rank1, rank2, rank3));

        when(requirementChecker.checkAll(eq(playerId), any()))
            .thenReturn(CompletableFuture.completedFuture(true));

        when(playerRankRepository.hasUnlockedRankAsync(playerId, "warrior_1"))
            .thenReturn(CompletableFuture.completedFuture(false));
        when(playerRankRepository.createAsync(any()))
            .thenAnswer(inv -> CompletableFuture.completedFuture(inv.getArgument(0)));

        when(pathRepository.findByPlayerIdAndTreeIdAsync(playerId, "warrior"))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(pathRepository.countActiveByPlayerIdAsync(playerId))
            .thenReturn(CompletableFuture.completedFuture(0L));
        when(pathRepository.createAsync(any()))
            .thenAnswer(inv -> CompletableFuture.completedFuture(inv.getArgument(0)));

        var trees = rankService.getAvailableRankTrees().join();
        assertEquals(1, trees.size());
        assertEquals("warrior", trees.get(0).id());

        var unlockResult = rankService.unlockRank(playerId, "warrior_1").join();
        assertTrue(unlockResult);

        verify(playerRankRepository).createAsync(any(PlayerRank.class));
        verify(pathRepository).createAsync(any(PlayerRankPath.class));
    }

    @Test
    @DisplayName("Cannot skip ranks in linear progression")
    void cannotSkipRanksInLinearProgression() {
        var playerId = UUID.randomUUID();
        var rank2 = TestData.rank("warrior", "warrior_2", 2);

        when(rankRepository.findById("warrior_2")).thenReturn(Optional.of(rank2));
        when(requirementChecker.checkAll(eq(playerId), any()))
            .thenReturn(CompletableFuture.completedFuture(false));

        var result = rankService.unlockRank(playerId, "warrior_2").join();

        assertFalse(result);
        verify(playerRankRepository, never()).createAsync(any());
    }

    @Test
    @DisplayName("Get player rank progress after unlocking ranks")
    void getPlayerRankProgressAfterUnlocking() {
        var playerId = UUID.randomUUID();
        var path = PlayerRankPath.create(playerId, "warrior", "warrior_2");
        var rank1 = PlayerRank.create(playerId, "warrior_1", "warrior");
        var rank2 = PlayerRank.create(playerId, "warrior_2", "warrior");

        when(pathRepository.findByPlayerIdAsync(playerId))
            .thenReturn(CompletableFuture.completedFuture(List.of(path)));
        when(playerRankRepository.findByPlayerIdAsync(playerId))
            .thenReturn(CompletableFuture.completedFuture(List.of(rank1, rank2)));

        var result = rankService.getPlayerRanks(playerId).join();

        assertTrue(result.isPresent());
        var data = result.get();
        assertEquals(1, data.activePathCount());
        assertTrue(data.hasUnlockedRank("warrior_1"));
        assertTrue(data.hasUnlockedRank("warrior_2"));
        assertFalse(data.hasUnlockedRank("warrior_3"));
    }

    @Test
    @DisplayName("Check requirements returns correct status")
    void checkRequirementsReturnsCorrectStatus() {
        var playerId = UUID.randomUUID();
        var rank = TestData.rank("warrior", "warrior_1", 1);

        when(rankRepository.findById("warrior_1")).thenReturn(Optional.of(rank));
        when(requirementChecker.checkAll(eq(playerId), any()))
            .thenReturn(CompletableFuture.completedFuture(true));

        var result = rankService.checkRequirements(playerId, "warrior_1").join();

        assertTrue(result);
    }

    @Test
    @DisplayName("Get rank tree returns tree with all ranks")
    void getRankTreeReturnsTreeWithAllRanks() {
        var tree = TestData.rankTree("warrior");

        when(treeRepository.findById("warrior")).thenReturn(Optional.of(tree));

        var result = rankService.getRankTree("warrior").join();

        assertTrue(result.isPresent());
        assertEquals("warrior", result.get().id());
        assertEquals(3, result.get().ranks().size());
    }
}
