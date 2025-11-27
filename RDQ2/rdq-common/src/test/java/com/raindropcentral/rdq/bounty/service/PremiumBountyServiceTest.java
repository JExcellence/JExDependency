package com.raindropcentral.rdq.bounty.service;

import com.raindropcentral.rdq.bounty.dto.*;
import com.raindropcentral.rdq.bounty.exception.*;
import com.raindropcentral.rdq.bounty.type.BountyStatus;
import com.raindropcentral.rdq.bounty.type.HunterSortOrder;
import com.raindropcentral.rdq.database.entity.bounty.BountyHunterStats;
import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.repository.BountyHunterStatsRepository;
import com.raindropcentral.rdq.database.repository.RBountyRepository;
import com.raindropcentral.rdq.database.repository.RDQPlayerRepository;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PremiumBountyService.
 * Tests all service methods with mock repositories.
 */
@ExtendWith(MockitoExtension.class)
class PremiumBountyServiceTest {
    
    @Mock
    private RBountyRepository bountyRepository;
    
    @Mock
    private BountyHunterStatsRepository hunterStatsRepository;
    
    @Mock
    private RDQPlayerRepository playerRepository;
    
    private PremiumBountyService service;
    
    private UUID targetUuid;
    private UUID commissionerUuid;
    private UUID hunterUuid;
    
    @BeforeEach
    void setUp() {
        service = new PremiumBountyService(
                bountyRepository,
                hunterStatsRepository,
                playerRepository,
                -1, // unlimited bounties
                54, // max reward items
                7   // expiry days
        );
        
        targetUuid = UUID.randomUUID();
        commissionerUuid = UUID.randomUUID();
        hunterUuid = UUID.randomUUID();
    }
    
    // ========== Edition Capabilities Tests ==========
    
    @Test
    void testIsPremium() {
        assertTrue(service.isPremium());
    }
    
    @Test
    void testGetMaxBountiesPerPlayer() {
        assertEquals(-1, service.getMaxBountiesPerPlayer());
    }
    
    @Test
    void testGetMaxRewardItems() {
        assertEquals(54, service.getMaxRewardItems());
    }
    
    // ========== Query Operations Tests ==========
    
    @Test
    void testGetAllBounties() throws ExecutionException, InterruptedException {
        // Arrange
        RBounty entity = createMockBounty();
        RDQPlayer targetPlayer = createMockPlayer(targetUuid, "Target");
        RDQPlayer commissionerPlayer = createMockPlayer(commissionerUuid, "Commissioner");
        
        when(bountyRepository.findAllActiveAsync(0, 10))
                .thenReturn(CompletableFuture.completedFuture(List.of(entity)));
        when(playerRepository.findByUuidAsync(targetUuid))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(targetPlayer)));
        when(playerRepository.findByUuidAsync(commissionerUuid))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(commissionerPlayer)));
        
        // Act
        List<Bounty> result = service.getAllBounties(0, 10).get();
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(targetUuid, result.get(0).targetUuid());
        verify(bountyRepository).findAllActiveAsync(0, 10);
    }
    
    @Test
    void testGetBountyByTarget() throws ExecutionException, InterruptedException {
        // Arrange
        RBounty entity = createMockBounty();
        RDQPlayer targetPlayer = createMockPlayer(targetUuid, "Target");
        RDQPlayer commissionerPlayer = createMockPlayer(commissionerUuid, "Commissioner");
        
        when(bountyRepository.findActiveByTargetAsync(targetUuid))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(entity)));
        when(playerRepository.findByUuidAsync(targetUuid))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(targetPlayer)));
        when(playerRepository.findByUuidAsync(commissionerUuid))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(commissionerPlayer)));
        
        // Act
        Optional<Bounty> result = service.getBountyByTarget(targetUuid).get();
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(targetUuid, result.get().targetUuid());
        verify(bountyRepository).findActiveByTargetAsync(targetUuid);
    }
    
    @Test
    void testGetBountiesByCommissioner() throws ExecutionException, InterruptedException {
        // Arrange
        RBounty entity = createMockBounty();
        RDQPlayer targetPlayer = createMockPlayer(targetUuid, "Target");
        RDQPlayer commissionerPlayer = createMockPlayer(commissionerUuid, "Commissioner");
        
        when(bountyRepository.findByCommissionerAsync(commissionerUuid))
                .thenReturn(CompletableFuture.completedFuture(List.of(entity)));
        when(playerRepository.findByUuidAsync(targetUuid))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(targetPlayer)));
        when(playerRepository.findByUuidAsync(commissionerUuid))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(commissionerPlayer)));
        
        // Act
        List<Bounty> result = service.getBountiesByCommissioner(commissionerUuid).get();
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(commissionerUuid, result.get(0).commissionerUuid());
        verify(bountyRepository).findByCommissionerAsync(commissionerUuid);
    }
    
    @Test
    void testGetTotalBountyCount() throws ExecutionException, InterruptedException {
        // Arrange
        when(bountyRepository.countActiveAsync())
                .thenReturn(CompletableFuture.completedFuture(5));
        
        // Act
        int result = service.getTotalBountyCount().get();
        
        // Assert
        assertEquals(5, result);
        verify(bountyRepository).countActiveAsync();
    }
    
    // ========== Mutation Operations Tests ==========
    
    @Test
    void testCreateBounty() throws ExecutionException, InterruptedException, BountyException {
        // Arrange
        BountyCreationRequest request = new BountyCreationRequest(
                targetUuid,
                commissionerUuid,
                Set.of(),
                Map.of("coins", 100.0),
                Optional.empty()
        );
        
        RBounty savedEntity = createMockBounty();
        RDQPlayer targetPlayer = createMockPlayer(targetUuid, "Target");
        RDQPlayer commissionerPlayer = createMockPlayer(commissionerUuid, "Commissioner");
        
        when(bountyRepository.findActiveByTargetAsync(targetUuid))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(bountyRepository.createAsync(any(RBounty.class)))
                .thenReturn(CompletableFuture.completedFuture(savedEntity));
        when(playerRepository.findByUuidAsync(targetUuid))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(targetPlayer)));
        when(playerRepository.findByUuidAsync(commissionerUuid))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(commissionerPlayer)));
        
        // Act
        Bounty result = service.createBounty(request).get();
        
        // Assert
        assertNotNull(result);
        assertEquals(targetUuid, result.targetUuid());
        assertEquals(commissionerUuid, result.commissionerUuid());
        verify(bountyRepository).createAsync(any(RBounty.class));
    }
    
    @Test
    void testCreateBounty_SelfTargeting() {
        // Arrange
        BountyCreationRequest request = new BountyCreationRequest(
                commissionerUuid,
                commissionerUuid,
                Set.of(),
                Map.of("coins", 100.0),
                Optional.empty()
        );
        
        // Act & Assert
        ExecutionException exception = assertThrows(ExecutionException.class, () -> {
            service.createBounty(request).get();
        });
        
        assertTrue(exception.getCause() instanceof SelfTargetingException);
    }
    
    @Test
    void testDeleteBounty() throws ExecutionException, InterruptedException {
        // Arrange
        Long bountyId = 1L;
        RBounty entity = createMockBounty();
        
        when(bountyRepository.findByIdAsync(bountyId))
                .thenReturn(CompletableFuture.completedFuture(entity));
        when(bountyRepository.updateAsync(entity))
                .thenReturn(CompletableFuture.completedFuture(entity));
        
        // Act
        boolean result = service.deleteBounty(bountyId).get();
        
        // Assert
        assertTrue(result);
        verify(bountyRepository).updateAsync(entity);
    }
    
    @Test
    void testClaimBounty() throws ExecutionException, InterruptedException, BountyException {
        // Arrange
        Long bountyId = 1L;
        RBounty entity = createMockBounty();
        RDQPlayer targetPlayer = createMockPlayer(targetUuid, "Target");
        RDQPlayer commissionerPlayer = createMockPlayer(commissionerUuid, "Commissioner");
        RDQPlayer hunterPlayer = createMockPlayer(hunterUuid, "Hunter");
        BountyHunterStats stats = new BountyHunterStats(hunterPlayer);
        
        when(bountyRepository.findByIdAsync(bountyId))
                .thenReturn(CompletableFuture.completedFuture(entity));
        when(bountyRepository.updateAsync(entity))
                .thenReturn(CompletableFuture.completedFuture(entity));
        when(playerRepository.findByUuidAsync(targetUuid))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(targetPlayer)));
        when(playerRepository.findByUuidAsync(commissionerUuid))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(commissionerPlayer)));
        when(playerRepository.findByUuidAsync(hunterUuid))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(hunterPlayer)));
        when(hunterStatsRepository.findByPlayerAsync(hunterPlayer))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(stats)));
        when(hunterStatsRepository.updateAsync(stats))
                .thenReturn(CompletableFuture.completedFuture(stats));
        
        // Act
        Bounty result = service.claimBounty(bountyId, hunterUuid).get();
        
        // Assert
        assertNotNull(result);
        verify(bountyRepository).updateAsync(entity);
        verify(hunterStatsRepository).updateAsync(stats);
    }
    
    @Test
    void testExpireBounty() throws ExecutionException, InterruptedException {
        // Arrange
        Long bountyId = 1L;
        RBounty entity = createMockBounty();
        
        when(bountyRepository.findByIdAsync(bountyId))
                .thenReturn(CompletableFuture.completedFuture(entity));
        when(bountyRepository.updateAsync(entity))
                .thenReturn(CompletableFuture.completedFuture(entity));
        
        // Act
        service.expireBounty(bountyId).get();
        
        // Assert
        verify(bountyRepository).updateAsync(entity);
        assertFalse(entity.isActive());
    }
    
    // ========== Hunter Statistics Tests ==========
    
    @Test
    void testGetHunterStats() throws ExecutionException, InterruptedException {
        // Arrange
        RDQPlayer hunterPlayer = createMockPlayer(hunterUuid, "Hunter");
        BountyHunterStats stats = new BountyHunterStats(hunterPlayer);
        stats.setBountiesClaimed(5);
        stats.setTotalRewardValue(500.0);
        
        when(hunterStatsRepository.findByPlayerUuidAsync(hunterUuid))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(stats)));
        when(hunterStatsRepository.getPlayerRankAsync(hunterUuid))
                .thenReturn(CompletableFuture.completedFuture(1));
        
        // Act
        Optional<HunterStats> result = service.getHunterStats(hunterUuid).get();
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(5, result.get().bountiesClaimed());
        assertEquals(500.0, result.get().totalRewardValue());
        assertEquals(1, result.get().rank());
    }
    
    @Test
    void testGetTopHunters() throws ExecutionException, InterruptedException {
        // Arrange
        RDQPlayer hunter1 = createMockPlayer(UUID.randomUUID(), "Hunter1");
        RDQPlayer hunter2 = createMockPlayer(UUID.randomUUID(), "Hunter2");
        BountyHunterStats stats1 = new BountyHunterStats(hunter1);
        BountyHunterStats stats2 = new BountyHunterStats(hunter2);
        stats1.setBountiesClaimed(10);
        stats2.setBountiesClaimed(5);
        
        when(hunterStatsRepository.findTopHuntersAsync(10, "bountiesClaimed"))
                .thenReturn(CompletableFuture.completedFuture(List.of(stats1, stats2)));
        
        // Act
        List<HunterStats> result = service.getTopHunters(10, HunterSortOrder.BOUNTIES_CLAIMED).get();
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).rank());
        assertEquals(2, result.get(1).rank());
    }
    
    @Test
    void testGetHunterRank() throws ExecutionException, InterruptedException {
        // Arrange
        when(hunterStatsRepository.getPlayerRankAsync(hunterUuid))
                .thenReturn(CompletableFuture.completedFuture(3));
        
        // Act
        int result = service.getHunterRank(hunterUuid).get();
        
        // Assert
        assertEquals(3, result);
    }
    
    // ========== Helper Methods ==========
    
    private RBounty createMockBounty() {
        RBounty bounty = new RBounty(targetUuid, commissionerUuid);
        bounty.setTotalEstimatedValue(100.0);
        bounty.setExpiresAt(Optional.of(LocalDateTime.now().plusDays(7)));
        return bounty;
    }
    
    private RDQPlayer createMockPlayer(UUID uuid, String name) {
        RDQPlayer player = mock(RDQPlayer.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getPlayerName()).thenReturn(name);
        return player;
    }
}
