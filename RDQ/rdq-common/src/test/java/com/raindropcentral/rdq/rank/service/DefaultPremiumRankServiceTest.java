package com.raindropcentral.rdq.rank.service;

import com.raindropcentral.rdq.fixtures.TestData;
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

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DefaultPremiumRankService")
class DefaultPremiumRankServiceTest {

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

    private DefaultPremiumRankService service;

    @BeforeEach
    void setUp() {
        service = new DefaultPremiumRankService(
            treeRepository,
            rankRepository,
            playerRankRepository,
            pathRepository,
            requirementChecker,
            3
        );
    }

    @Nested
    @DisplayName("switchRankTree()")
    class SwitchRankTreeTests {

        @Test
        @DisplayName("should return false when switching to same tree")
        void shouldReturnFalseWhenSwitchingToSameTree() {
            var playerId = UUID.randomUUID();

            var result = service.switchRankTree(playerId, "warrior", "warrior").join();

            assertFalse(result);
        }

        @Test
        @DisplayName("should return false when target tree does not exist")
        void shouldReturnFalseWhenTargetTreeNotExists() {
            var playerId = UUID.randomUUID();

            when(treeRepository.exists("nonexistent")).thenReturn(false);

            var result = service.switchRankTree(playerId, "warrior", "nonexistent").join();

            assertFalse(result);
        }

        @Test
        @DisplayName("should return false when player has no active path in source tree")
        void shouldReturnFalseWhenNoActivePathInSourceTree() {
            var playerId = UUID.randomUUID();

            when(treeRepository.exists("mage")).thenReturn(true);
            when(pathRepository.findByPlayerIdAndTreeIdAsync(playerId, "warrior"))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

            var result = service.switchRankTree(playerId, "warrior", "mage").join();

            assertFalse(result);
        }


        @Test
        @DisplayName("should switch to existing path in target tree")
        void shouldSwitchToExistingPathInTargetTree() {
            var playerId = UUID.randomUUID();
            var sourcePath = PlayerRankPath.create(playerId, "warrior", "warrior_1");
            var targetPath = PlayerRankPath.create(playerId, "mage", "mage_1");
            targetPath.setActive(false);

            when(treeRepository.exists("mage")).thenReturn(true);
            when(pathRepository.findByPlayerIdAndTreeIdAsync(playerId, "warrior"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(sourcePath)));
            when(pathRepository.updateAsync(any()))
                .thenAnswer(inv -> CompletableFuture.completedFuture(inv.getArgument(0)));
            when(pathRepository.findByPlayerIdAndTreeIdAsync(playerId, "mage"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(targetPath)));

            var result = service.switchRankTree(playerId, "warrior", "mage").join();

            assertTrue(result);
            verify(pathRepository, times(2)).updateAsync(any());
        }

        @Test
        @DisplayName("should create new path when target tree has no existing path")
        void shouldCreateNewPathWhenNoExistingPath() {
            var playerId = UUID.randomUUID();
            var sourcePath = PlayerRankPath.create(playerId, "warrior", "warrior_1");
            var firstRank = TestData.rank("mage", "mage_1", 1);

            when(treeRepository.exists("mage")).thenReturn(true);
            when(pathRepository.findByPlayerIdAndTreeIdAsync(playerId, "warrior"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(sourcePath)));
            when(pathRepository.updateAsync(any()))
                .thenAnswer(inv -> CompletableFuture.completedFuture(inv.getArgument(0)));
            when(pathRepository.createAsync(any()))
                .thenAnswer(inv -> CompletableFuture.completedFuture(inv.getArgument(0)));
            when(pathRepository.findByPlayerIdAndTreeIdAsync(playerId, "mage"))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
            when(rankRepository.findFirstRankInTree("mage"))
                .thenReturn(Optional.of(firstRank));

            var result = service.switchRankTree(playerId, "warrior", "mage").join();

            assertTrue(result);
            verify(pathRepository).updateAsync(any());
            verify(pathRepository).createAsync(any());
        }
    }

    @Nested
    @DisplayName("canSwitchRankTree()")
    class CanSwitchRankTreeTests {

        @Test
        @DisplayName("should return true when under max active trees")
        void shouldReturnTrueWhenUnderMaxActiveTrees() {
            var playerId = UUID.randomUUID();

            when(pathRepository.countActiveByPlayerIdAsync(playerId))
                .thenReturn(CompletableFuture.completedFuture(2L));

            var result = service.canSwitchRankTree(playerId).join();

            assertTrue(result);
        }

        @Test
        @DisplayName("should return false when at max active trees")
        void shouldReturnFalseWhenAtMaxActiveTrees() {
            var playerId = UUID.randomUUID();

            when(pathRepository.countActiveByPlayerIdAsync(playerId))
                .thenReturn(CompletableFuture.completedFuture(3L));

            var result = service.canSwitchRankTree(playerId).join();

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("getMaxActiveRankTrees()")
    class GetMaxActiveRankTreesTests {

        @Test
        @DisplayName("should return configured max active trees")
        void shouldReturnConfiguredMaxActiveTrees() {
            assertEquals(3, service.getMaxActiveRankTrees());
        }
    }
}
