package com.raindropcentral.rdq.bounty.service;

import com.raindropcentral.rdq.bounty.dto.*;
import com.raindropcentral.rdq.bounty.exception.*;
import com.raindropcentral.rdq.bounty.type.BountyStatus;
import com.raindropcentral.rdq.bounty.type.HunterSortOrder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FreeBountyService.
 * Tests in-memory storage operations, static bounty loading, and 1 bounty limit enforcement.
 */
class FreeBountyServiceTest {
    
    @TempDir
    File tempDir;
    
    private FreeBountyService service;
    private File configFile;
    
    private UUID targetUuid;
    private UUID commissionerUuid;
    private UUID hunterUuid;
    
    @BeforeEach
    void setUp() {
        targetUuid = UUID.randomUUID();
        commissionerUuid = UUID.randomUUID();
        hunterUuid = UUID.randomUUID();
        
        configFile = new File(tempDir, "bounty.yml");
        service = new FreeBountyService(configFile, 27, 7);
    }
    
    // ========== Edition Capabilities Tests ==========
    
    @Test
    void isPremium_shouldReturnFalse() {
        assertFalse(service.isPremium());
    }
    
    @Test
    void getMaxBountiesPerPlayer_shouldReturnOne() {
        assertEquals(1, service.getMaxBountiesPerPlayer());
    }
    
    @Test
    void getMaxRewardItems_shouldReturnConfiguredValue() {
        assertEquals(27, service.getMaxRewardItems());
    }
    
    // ========== Bounty Creation Tests ==========
    
    @Test
    void createBounty_withValidRequest_shouldSucceed() throws Exception {
        BountyCreationRequest request = createValidRequest(targetUuid, commissionerUuid);
        
        Bounty bounty = service.createBounty(request).get();
        
        assertNotNull(bounty);
        assertNotNull(bounty.id());
        assertEquals(targetUuid, bounty.targetUuid());
        assertEquals(commissionerUuid, bounty.commissionerUuid());
        assertEquals(BountyStatus.ACTIVE, bounty.status());
        assertFalse(bounty.rewardItems().isEmpty());
        assertFalse(bounty.rewardCurrencies().isEmpty());
    }
    
    @Test
    void createBounty_withSelfTargeting_shouldFail() {
        BountyCreationRequest request = createValidRequest(commissionerUuid, commissionerUuid);
        
        ExecutionException exception = assertThrows(ExecutionException.class, () -> {
            service.createBounty(request).get();
        });
        
        assertTrue(exception.getCause() instanceof SelfTargetingException);
    }
    
    @Test
    void createBounty_withExistingActiveBounty_shouldEnforceLimit() throws Exception {
        // Create first bounty
        BountyCreationRequest request1 = createValidRequest(targetUuid, commissionerUuid);
        service.createBounty(request1).get();
        
        // Attempt to create second bounty by same commissioner
        UUID target2Uuid = UUID.randomUUID();
        BountyCreationRequest request2 = createValidRequest(target2Uuid, commissionerUuid);
        
        ExecutionException exception = assertThrows(ExecutionException.class, () -> {
            service.createBounty(request2).get();
        });
        
        assertTrue(exception.getCause() instanceof BountyAlreadyExistsException);
    }
    
    @Test
    void createBounty_afterClaimingFirst_shouldAllowNewBounty() throws Exception {
        // Create first bounty
        BountyCreationRequest request1 = createValidRequest(targetUuid, commissionerUuid);
        Bounty bounty1 = service.createBounty(request1).get();
        
        // Claim the first bounty
        service.claimBounty(bounty1.id(), hunterUuid).get();
        
        // Create second bounty by same commissioner
        UUID target2Uuid = UUID.randomUUID();
        BountyCreationRequest request2 = createValidRequest(target2Uuid, commissionerUuid);
        Bounty bounty2 = service.createBounty(request2).get();
        
        assertNotNull(bounty2);
        assertEquals(BountyStatus.ACTIVE, bounty2.status());
    }
    
    // ========== Bounty Query Tests ==========
    
    @Test
    void getAllBounties_withNoBounties_shouldReturnEmptyList() throws Exception {
        List<Bounty> bounties = service.getAllBounties(0, 10).get();
        
        assertTrue(bounties.isEmpty());
    }
    
    @Test
    void getAllBounties_withActiveBounties_shouldReturnThem() throws Exception {
        BountyCreationRequest request = createValidRequest(targetUuid, commissionerUuid);
        service.createBounty(request).get();
        
        List<Bounty> bounties = service.getAllBounties(0, 10).get();
        
        assertEquals(1, bounties.size());
        assertEquals(BountyStatus.ACTIVE, bounties.get(0).status());
    }
    
    @Test
    void getBountyByTarget_withExistingBounty_shouldReturnIt() throws Exception {
        BountyCreationRequest request = createValidRequest(targetUuid, commissionerUuid);
        service.createBounty(request).get();
        
        Optional<Bounty> bounty = service.getBountyByTarget(targetUuid).get();
        
        assertTrue(bounty.isPresent());
        assertEquals(targetUuid, bounty.get().targetUuid());
    }
    
    @Test
    void getBountyByTarget_withNoBounty_shouldReturnEmpty() throws Exception {
        Optional<Bounty> bounty = service.getBountyByTarget(targetUuid).get();
        
        assertTrue(bounty.isEmpty());
    }
    
    @Test
    void getBountiesByCommissioner_shouldReturnCommissionerBounties() throws Exception {
        BountyCreationRequest request = createValidRequest(targetUuid, commissionerUuid);
        service.createBounty(request).get();
        
        List<Bounty> bounties = service.getBountiesByCommissioner(commissionerUuid).get();
        
        assertEquals(1, bounties.size());
        assertEquals(commissionerUuid, bounties.get(0).commissionerUuid());
    }
    
    @Test
    void getTotalBountyCount_shouldReturnActiveCount() throws Exception {
        BountyCreationRequest request = createValidRequest(targetUuid, commissionerUuid);
        service.createBounty(request).get();
        
        int count = service.getTotalBountyCount().get();
        
        assertEquals(1, count);
    }
    
    // ========== Bounty Claiming Tests ==========
    
    @Test
    void claimBounty_withValidBounty_shouldSucceed() throws Exception {
        BountyCreationRequest request = createValidRequest(targetUuid, commissionerUuid);
        Bounty bounty = service.createBounty(request).get();
        
        Bounty claimedBounty = service.claimBounty(bounty.id(), hunterUuid).get();
        
        assertNotNull(claimedBounty);
        assertEquals(BountyStatus.CLAIMED, claimedBounty.status());
        assertTrue(claimedBounty.claimInfo().isPresent());
        assertEquals(hunterUuid, claimedBounty.claimInfo().get().hunterUuid());
    }
    
    @Test
    void claimBounty_withNonExistentBounty_shouldFail() {
        ExecutionException exception = assertThrows(ExecutionException.class, () -> {
            service.claimBounty(999L, hunterUuid).get();
        });
        
        assertTrue(exception.getCause() instanceof BountyNotFoundException);
    }
    
    @Test
    void claimBounty_withAlreadyClaimedBounty_shouldFail() throws Exception {
        BountyCreationRequest request = createValidRequest(targetUuid, commissionerUuid);
        Bounty bounty = service.createBounty(request).get();
        service.claimBounty(bounty.id(), hunterUuid).get();
        
        ExecutionException exception = assertThrows(ExecutionException.class, () -> {
            service.claimBounty(bounty.id(), UUID.randomUUID()).get();
        });
        
        assertTrue(exception.getCause() instanceof BountyAlreadyClaimedException);
    }
    
    // ========== Bounty Deletion/Expiration Tests ==========
    
    @Test
    void deleteBounty_withExistingBounty_shouldMarkExpired() throws Exception {
        BountyCreationRequest request = createValidRequest(targetUuid, commissionerUuid);
        Bounty bounty = service.createBounty(request).get();
        
        boolean deleted = service.deleteBounty(bounty.id()).get();
        
        assertTrue(deleted);
        
        // Verify bounty is no longer active
        Optional<Bounty> retrievedBounty = service.getBountyByTarget(targetUuid).get();
        assertTrue(retrievedBounty.isEmpty() || retrievedBounty.get().status() == BountyStatus.EXPIRED);
    }
    
    @Test
    void expireBounty_shouldMarkBountyExpired() throws Exception {
        BountyCreationRequest request = createValidRequest(targetUuid, commissionerUuid);
        Bounty bounty = service.createBounty(request).get();
        
        service.expireBounty(bounty.id()).get();
        
        // Verify bounty is no longer active
        Optional<Bounty> retrievedBounty = service.getBountyByTarget(targetUuid).get();
        assertTrue(retrievedBounty.isEmpty());
    }
    
    // ========== Hunter Statistics Tests ==========
    
    @Test
    void getHunterStats_withNoStats_shouldReturnEmpty() throws Exception {
        Optional<HunterStats> stats = service.getHunterStats(hunterUuid).get();
        
        assertTrue(stats.isEmpty());
    }
    
    @Test
    void getHunterStats_afterClaim_shouldReturnStats() throws Exception {
        BountyCreationRequest request = createValidRequest(targetUuid, commissionerUuid);
        Bounty bounty = service.createBounty(request).get();
        service.claimBounty(bounty.id(), hunterUuid).get();
        
        Optional<HunterStats> stats = service.getHunterStats(hunterUuid).get();
        
        assertTrue(stats.isPresent());
        assertEquals(1, stats.get().bountiesClaimed());
        assertTrue(stats.get().totalRewardValue() > 0);
    }
    
    @Test
    void getTopHunters_shouldReturnRankedList() throws Exception {
        // Create and claim multiple bounties with different hunters
        BountyCreationRequest request1 = createValidRequest(targetUuid, commissionerUuid);
        Bounty bounty1 = service.createBounty(request1).get();
        service.claimBounty(bounty1.id(), hunterUuid).get();
        
        List<HunterStats> topHunters = service.getTopHunters(10, HunterSortOrder.BOUNTIES_CLAIMED).get();
        
        assertFalse(topHunters.isEmpty());
        assertEquals(1, topHunters.get(0).rank());
    }
    
    @Test
    void getHunterRank_shouldReturnCorrectRank() throws Exception {
        BountyCreationRequest request = createValidRequest(targetUuid, commissionerUuid);
        Bounty bounty = service.createBounty(request).get();
        service.claimBounty(bounty.id(), hunterUuid).get();
        
        int rank = service.getHunterRank(hunterUuid).get();
        
        assertEquals(1, rank);
    }
    
    // ========== Helper Methods ==========
    
    private BountyCreationRequest createValidRequest(UUID target, UUID commissioner) {
        Set<RewardItem> rewardItems = Set.of(
                new RewardItem(new ItemStack(Material.DIAMOND, 10), 10, 100.0)
        );
        
        Map<String, Double> rewardCurrencies = Map.of("coins", 500.0);
        
        return new BountyCreationRequest(
                target,
                commissioner,
                rewardItems,
                rewardCurrencies,
                Optional.empty()
        );
    }
}
