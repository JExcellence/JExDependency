package com.raindropcentral.rdq.rank.service;

import com.raindropcentral.rdq.fixtures.TestData;
import com.raindropcentral.rdq.database.entity.rank.PlayerRank;
import com.raindropcentral.rdq.database.entity.rank.PlayerRankPath;
import com.raindropcentral.rdq.rank.repository.PlayerRankPathRepository;
import com.raindropcentral.rdq.rank.repository.PlayerRankRepository;
import com.raindropcentral.rdq.rank.repository.RankRepository;
import com.raindropcentral.rdq.rank.repository.RankTreeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DefaultFreeRankService")
class DefaultFreeRankServiceTest {

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

    private DefaultFreeRankService service;

    @BeforeEach
    void setUp() {
        service = new DefaultFreeRankService(
            treeRepository,
            rankRepository,
            playerRankRepository,
            pathRepository,
            requirementChecker
        );
    }

    @Nested
    @DisplayName("getPlayerRanks()")
    class GetPlayerRanksTests {

        @Test
        @DisplayName("should return empty when player has no rank data")
        void shouldReturnEmptyWhenNoRankData() {
            var playerId = UUID.randomUUID();

            when(pathRepository.findByPlayerIdAsync(playerId))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
            when(playerRankRepository.findByPlayerIdAsync(playerId))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

            var result = service.getPlayerRanks(playerId).join();

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return player rank data when exists")
        void shouldReturnPlayerRankDataWhenExists() {
            var playerId = UUID.randomUUID();
            var path = PlayerRankPath.create(playerId, "warrior", "warrior_1");
            var rank = PlayerRank.create(playerId, "warrior_1", "warrior");

            when(pathRepository.findByPlayerIdAsync(playerId))
                .thenReturn(CompletableFuture.completedFuture(List.of(path)));
            when(playerRankRepository.findByPlayerIdAsync(playerId))
                .thenReturn(CompletableFuture.completedFuture(List.of(rank)));

            var result = service.getPlayerRanks(playerId).join();

            assertTrue(result.isPresent());
            assertEquals(1, result.get().activePathCount());
            assertTrue(result.get().hasUnlockedRank("warrior_1"));
        }
    }


    @Nested
    @DisplayName("unlockRank()")
    class UnlockRankTests {

        @Test
        @DisplayName("should return false when rank does not exist")
        void shouldReturnFalseWhenRankNotExists() {
            var playerId = UUID.randomUUID();

            when(rankRepository.findById("nonexistent")).thenReturn(Optional.empty());

            var result = service.unlockRank(playerId, "nonexistent").join();

            assertFalse(result);
        }

        @Test
        @DisplayName("should return false when requirements not met")
        void shouldReturnFalseWhenRequirementsNotMet() {
            var playerId = UUID.randomUUID();
            var rank = TestData.rank("warrior", "warrior_2", 2);

            when(rankRepository.findById("warrior_2")).thenReturn(Optional.of(rank));
            when(requirementChecker.checkAll(eq(playerId), any()))
                .thenReturn(CompletableFuture.completedFuture(false));

            var result = service.unlockRank(playerId, "warrior_2").join();

            assertFalse(result);
            verify(playerRankRepository, never()).hasUnlockedRankAsync(any(), any());
        }

        @Test
        @DisplayName("should return false when rank already unlocked")
        void shouldReturnFalseWhenAlreadyUnlocked() {
            var playerId = UUID.randomUUID();
            var rank = TestData.rank("warrior", "warrior_1", 1);

            when(rankRepository.findById("warrior_1")).thenReturn(Optional.of(rank));
            when(requirementChecker.checkAll(eq(playerId), any()))
                .thenReturn(CompletableFuture.completedFuture(true));
            when(playerRankRepository.hasUnlockedRankAsync(playerId, "warrior_1"))
                .thenReturn(CompletableFuture.completedFuture(true));

            var result = service.unlockRank(playerId, "warrior_1").join();

            assertFalse(result);
            verify(rankRepository, never()).findEnabledByTreeId(any());
        }

        @Test
        @DisplayName("should unlock first rank in tree successfully")
        void shouldUnlockFirstRankSuccessfully() {
            var playerId = UUID.randomUUID();
            var rank = TestData.rank("warrior", "warrior_1", 1);

            when(rankRepository.findById("warrior_1")).thenReturn(Optional.of(rank));
            when(requirementChecker.checkAll(eq(playerId), any()))
                .thenReturn(CompletableFuture.completedFuture(true));
            when(playerRankRepository.hasUnlockedRankAsync(playerId, "warrior_1"))
                .thenReturn(CompletableFuture.completedFuture(false));
            when(rankRepository.findEnabledByTreeId("warrior"))
                .thenReturn(List.of(rank));
            when(playerRankRepository.createAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(PlayerRank.create(playerId, "warrior_1", "warrior")));
            when(pathRepository.findByPlayerIdAndTreeIdAsync(playerId, "warrior"))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
            when(pathRepository.countActiveByPlayerIdAsync(playerId))
                .thenReturn(CompletableFuture.completedFuture(0L));
            when(pathRepository.createAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(PlayerRankPath.create(playerId, "warrior", "warrior_1")));

            var result = service.unlockRank(playerId, "warrior_1").join();

            assertTrue(result);
            verify(playerRankRepository).createAsync(any());
            verify(pathRepository).createAsync(any());
        }
    }


    @Nested
    @DisplayName("checkRequirements()")
    class CheckRequirementsTests {

        @Test
        @DisplayName("should return false when rank does not exist")
        void shouldReturnFalseWhenRankNotExists() {
            var playerId = UUID.randomUUID();

            when(rankRepository.findById("nonexistent")).thenReturn(Optional.empty());

            var result = service.checkRequirements(playerId, "nonexistent").join();

            assertFalse(result);
        }

        @Test
        @DisplayName("should return true when rank has no requirements")
        void shouldReturnTrueWhenNoRequirements() {
            var playerId = UUID.randomUUID();
            var rank = TestData.rank("warrior", "warrior_1", 1);

            when(rankRepository.findById("warrior_1")).thenReturn(Optional.of(rank));
            when(requirementChecker.checkAll(eq(playerId), any()))
                .thenReturn(CompletableFuture.completedFuture(true));

            var result = service.checkRequirements(playerId, "warrior_1").join();

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("getAvailableRankTrees()")
    class GetAvailableRankTreesTests {

        @Test
        @DisplayName("should return all enabled rank trees")
        void shouldReturnAllEnabledRankTrees() {
            var tree1 = TestData.rankTree("warrior");
            var tree2 = TestData.rankTree("mage");

            when(treeRepository.findAllEnabled()).thenReturn(List.of(tree1, tree2));

            var result = service.getAvailableRankTrees().join();

            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("getRank()")
    class GetRankTests {

        @Test
        @DisplayName("should return rank when exists")
        void shouldReturnRankWhenExists() {
            var rank = TestData.rank("warrior", "warrior_1", 1);

            when(rankRepository.findById("warrior_1")).thenReturn(Optional.of(rank));

            var result = service.getRank("warrior_1").join();

            assertTrue(result.isPresent());
            assertEquals("warrior_1", result.get().id());
        }

        @Test
        @DisplayName("should return empty when rank does not exist")
        void shouldReturnEmptyWhenRankNotExists() {
            when(rankRepository.findById("nonexistent")).thenReturn(Optional.empty());

            var result = service.getRank("nonexistent").join();

            assertTrue(result.isEmpty());
        }
    }
}
