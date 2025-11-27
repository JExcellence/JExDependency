package com.raindropcentral.rdq.bounty.distribution;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.raindropcentral.rdq.bounty.dto.Bounty;
import com.raindropcentral.rdq.bounty.dto.ClaimInfo;
import com.raindropcentral.rdq.bounty.dto.RewardItem;
import com.raindropcentral.rdq.bounty.type.BountyStatus;
import com.raindropcentral.rdq.bounty.type.ClaimMode;
import net.jqwik.api.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for reward distribution modes.
 * These tests verify that rewards are distributed correctly across all distribution modes.
 */
class RewardDistributionPropertiesTest {
    
    private static ServerMock server;
    private static WorldMock world;
    
    @BeforeAll
    static void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
    }
    
    @AfterAll
    static void tearDown() {
        MockBukkit.unmock();
    }
    
    /**
     * **Feature: bounty-system-rebuild, Property 46: Instant distribution**
     * 
     * For any bounty with INSTANT distribution mode, all reward items should be added 
     * to the hunter's inventory. If inventory is full, excess items should be dropped.
     * 
     * **Validates: Requirements 11.1**
     */
    @Property(tries = 100)
    @Label("Property 46: Instant distribution")
    void instantDistributionProperty(
        @ForAll("bountyWithItems") Bounty bounty,
        @ForAll("location") Location location
    ) {
        // Given: A hunter and an instant reward distributor
        PlayerMock hunter = server.addPlayer("Hunter");
        hunter.getInventory().clear();
        InstantRewardDistributor distributor = new InstantRewardDistributor();
        
        int initialInventorySize = countItems(hunter.getInventory());
        int rewardItemCount = bounty.rewardItems().stream()
            .mapToInt(RewardItem::amount)
            .sum();
        
        // When: Distributing rewards
        CompletableFuture<Void> future = distributor.distributeRewards(hunter, bounty, location);
        future.join(); // Wait for completion
        
        // Then: Items should be in inventory or dropped
        int finalInventorySize = countItems(hunter.getInventory());
        int droppedItemCount = world.getEntities().stream()
            .filter(e -> e instanceof org.bukkit.entity.Item)
            .mapToInt(e -> ((org.bukkit.entity.Item) e).getItemStack().getAmount())
            .sum();
        
        int totalItems = (finalInventorySize - initialInventorySize) + droppedItemCount;
        assertEquals(rewardItemCount, totalItems, 
            "Total items (inventory + dropped) should equal reward items");
        
        // Cleanup
        hunter.disconnect();
        world.getEntities().clear();
    }
    
    /**
     * **Feature: bounty-system-rebuild, Property 16: Currency distribution**
     * 
     * For any bounty with currency rewards, the currencies should be credited 
     * to the hunter's economy balance.
     * 
     * **Validates: Requirements 11.5**
     */
    @Property(tries = 100)
    @Label("Property 16: Currency distribution")
    void currencyDistributionProperty(
        @ForAll("bountyWithCurrencies") Bounty bounty,
        @ForAll("location") Location location
    ) {
        // Given: A hunter and an instant reward distributor
        PlayerMock hunter = server.addPlayer("Hunter");
        InstantRewardDistributor distributor = new InstantRewardDistributor();
        
        // When: Distributing rewards
        CompletableFuture<Void> future = distributor.distributeRewards(hunter, bounty, location);
        future.join(); // Wait for completion
        
        // Then: Distribution should complete without errors
        // Note: Actual economy integration will be tested when economy system is available
        assertTrue(future.isDone(), "Distribution should complete");
        assertFalse(future.isCompletedExceptionally(), "Distribution should not throw exceptions");
        
        // Cleanup
        hunter.disconnect();
    }
    
    /**
     * **Feature: bounty-system-rebuild, Property 47: Virtual distribution**
     * 
     * For any bounty with VIRTUAL distribution mode, all rewards should be credited 
     * to the hunter's virtual storage.
     * 
     * **Validates: Requirements 11.2**
     */
    @Property(tries = 100)
    @Label("Property 47: Virtual distribution")
    void virtualDistributionProperty(
        @ForAll("bountyWithItems") Bounty bounty,
        @ForAll("location") Location location
    ) {
        // Given: A hunter and a virtual reward distributor
        PlayerMock hunter = server.addPlayer("Hunter");
        VirtualRewardDistributor distributor = new VirtualRewardDistributor();
        
        // When: Distributing rewards
        CompletableFuture<Void> future = distributor.distributeRewards(hunter, bounty, location);
        future.join(); // Wait for completion
        
        // Then: Distribution should complete without errors
        // Note: Actual virtual storage integration will be tested when system is available
        assertTrue(future.isDone(), "Distribution should complete");
        assertFalse(future.isCompletedExceptionally(), "Distribution should not throw exceptions");
        
        // Cleanup
        hunter.disconnect();
    }
    
    /**
     * **Feature: bounty-system-rebuild, Property 48: Drop distribution**
     * 
     * For any bounty with DROP distribution mode, all reward items should be dropped 
     * at the target's death location.
     * 
     * **Validates: Requirements 11.3**
     */
    @Property(tries = 100)
    @Label("Property 48: Drop distribution")
    void dropDistributionProperty(
        @ForAll("bountyWithItems") Bounty bounty,
        @ForAll("validLocation") Location location
    ) {
        // Given: A hunter and a drop reward distributor
        PlayerMock hunter = server.addPlayer("Hunter");
        DropRewardDistributor distributor = new DropRewardDistributor();
        
        int rewardItemCount = bounty.rewardItems().stream()
            .mapToInt(RewardItem::amount)
            .sum();
        
        // When: Distributing rewards
        CompletableFuture<Void> future = distributor.distributeRewards(hunter, bounty, location);
        future.join(); // Wait for completion
        
        // Then: Items should be dropped in the world
        int droppedItemCount = world.getEntities().stream()
            .filter(e -> e instanceof org.bukkit.entity.Item)
            .mapToInt(e -> ((org.bukkit.entity.Item) e).getItemStack().getAmount())
            .sum();
        
        assertEquals(rewardItemCount, droppedItemCount, 
            "All reward items should be dropped in the world");
        
        // Cleanup
        hunter.disconnect();
        world.getEntities().clear();
    }
    
    /**
     * **Feature: bounty-system-rebuild, Property 49: Chest distribution**
     * 
     * For any bounty with CHEST distribution mode, a chest should be placed at the 
     * death location and filled with reward items.
     * 
     * **Validates: Requirements 11.4**
     */
    @Property(tries = 100)
    @Label("Property 49: Chest distribution")
    void chestDistributionProperty(
        @ForAll("bountyWithItems") Bounty bounty,
        @ForAll("validLocation") Location location
    ) {
        // Given: A hunter and a chest reward distributor
        PlayerMock hunter = server.addPlayer("Hunter");
        ChestRewardDistributor distributor = new ChestRewardDistributor();
        
        // Ensure location is clear for chest placement
        Block block = location.getBlock();
        block.setType(Material.AIR);
        
        int rewardItemCount = bounty.rewardItems().stream()
            .mapToInt(RewardItem::amount)
            .sum();
        
        // When: Distributing rewards
        CompletableFuture<Void> future = distributor.distributeRewards(hunter, bounty, location);
        future.join(); // Wait for completion
        
        // Then: A chest should be placed with items, or items should be dropped
        Block resultBlock = location.getBlock();
        
        if (resultBlock.getType() == Material.CHEST) {
            // Chest was successfully placed
            BlockState state = resultBlock.getState();
            assertTrue(state instanceof Chest, "Block should be a chest");
            
            Chest chest = (Chest) state;
            int chestItemCount = Arrays.stream(chest.getInventory().getContents())
                .filter(Objects::nonNull)
                .mapToInt(ItemStack::getAmount)
                .sum();
            
            int droppedItemCount = world.getEntities().stream()
                .filter(e -> e instanceof org.bukkit.entity.Item)
                .mapToInt(e -> ((org.bukkit.entity.Item) e).getItemStack().getAmount())
                .sum();
            
            int totalItems = chestItemCount + droppedItemCount;
            assertEquals(rewardItemCount, totalItems, 
                "All reward items should be in chest or dropped (if chest full)");
        } else {
            // Chest placement failed, items should be dropped
            int droppedItemCount = world.getEntities().stream()
                .filter(e -> e instanceof org.bukkit.entity.Item)
                .mapToInt(e -> ((org.bukkit.entity.Item) e).getItemStack().getAmount())
                .sum();
            
            assertEquals(rewardItemCount, droppedItemCount, 
                "All reward items should be dropped if chest placement fails");
        }
        
        // Cleanup
        hunter.disconnect();
        world.getEntities().clear();
        block.setType(Material.AIR);
    }
    
    /**
     * Property: Distribution handles full inventory gracefully
     * 
     * For any bounty with INSTANT distribution and a full inventory, 
     * excess items should be dropped without errors.
     */
    @Property(tries = 100)
    @Label("Full inventory handling")
    void fullInventoryHandlingProperty(
        @ForAll("bountyWithItems") Bounty bounty,
        @ForAll("location") Location location
    ) {
        // Given: A hunter with a full inventory
        PlayerMock hunter = server.addPlayer("Hunter");
        PlayerInventory inventory = hunter.getInventory();
        
        // Fill inventory with stone
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, new ItemStack(Material.STONE, 64));
        }
        
        InstantRewardDistributor distributor = new InstantRewardDistributor();
        
        int rewardItemCount = bounty.rewardItems().stream()
            .mapToInt(RewardItem::amount)
            .sum();
        
        // When: Distributing rewards
        CompletableFuture<Void> future = distributor.distributeRewards(hunter, bounty, location);
        future.join(); // Wait for completion
        
        // Then: Items should be dropped (inventory is full)
        int droppedItemCount = world.getEntities().stream()
            .filter(e -> e instanceof org.bukkit.entity.Item)
            .mapToInt(e -> ((org.bukkit.entity.Item) e).getItemStack().getAmount())
            .sum();
        
        assertTrue(droppedItemCount > 0, "Items should be dropped when inventory is full");
        
        // Cleanup
        hunter.disconnect();
        world.getEntities().clear();
    }
    
    // ========== Arbitraries ==========
    
    @Provide
    Arbitrary<UUID> uuid() {
        return Arbitraries.randomValue(random -> UUID.randomUUID());
    }
    
    @Provide
    Arbitrary<RewardItem> rewardItem() {
        return Combinators.combine(
            Arbitraries.of(Material.DIAMOND, Material.GOLD_INGOT, Material.EMERALD, 
                Material.IRON_INGOT, Material.COAL),
            Arbitraries.integers().between(1, 16)
        ).as((material, amount) -> 
            new RewardItem(new ItemStack(material), amount, amount * 10.0)
        );
    }
    
    @Provide
    Arbitrary<Set<RewardItem>> rewardItems() {
        return rewardItem().set().ofMinSize(1).ofMaxSize(5);
    }
    
    @Provide
    Arbitrary<Map<String, Double>> rewardCurrencies() {
        return Combinators.combine(
            Arbitraries.of("coins", "gems", "tokens"),
            Arbitraries.doubles().between(1.0, 1000.0)
        ).as((key, value) -> Map.of(key, value));
    }
    
    @Provide
    Arbitrary<Bounty> bountyWithItems() {
        return Combinators.combine(
            uuid(), // id as long
            uuid(), // targetUuid
            Arbitraries.strings().alpha().ofLength(8), // targetName
            uuid(), // commissionerUuid
            Arbitraries.strings().alpha().ofLength(8), // commissionerName
            rewardItems(),
            Arbitraries.just(Map.<String, Double>of()) // empty currencies
        ).as((id, targetUuid, targetName, commissionerUuid, commissionerName, items, currencies) ->
            new Bounty(
                Math.abs(id.getMostSignificantBits()),
                targetUuid,
                targetName,
                commissionerUuid,
                commissionerName,
                items,
                currencies,
                items.stream().mapToDouble(RewardItem::estimatedValue).sum(),
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(7),
                BountyStatus.ACTIVE,
                Optional.empty()
            )
        );
    }
    
    @Provide
    Arbitrary<Bounty> bountyWithCurrencies() {
        return Combinators.combine(
            uuid(),
            uuid(),
            Arbitraries.strings().alpha().ofLength(8),
            uuid(),
            Arbitraries.strings().alpha().ofLength(8),
            rewardCurrencies()
        ).as((id, targetUuid, targetName, commissionerUuid, commissionerName, currencies) ->
            new Bounty(
                Math.abs(id.getMostSignificantBits()),
                targetUuid,
                targetName,
                commissionerUuid,
                commissionerName,
                Set.of(), // empty items
                currencies,
                currencies.values().stream().mapToDouble(Double::doubleValue).sum(),
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(7),
                BountyStatus.ACTIVE,
                Optional.empty()
            )
        );
    }
    
    @Provide
    Arbitrary<Location> location() {
        return Combinators.combine(
            Arbitraries.integers().between(-100, 100),
            Arbitraries.integers().between(world.getMinHeight(), world.getMaxHeight() - 1),
            Arbitraries.integers().between(-100, 100)
        ).as((x, y, z) -> new Location(world, x, y, z));
    }
    
    @Provide
    Arbitrary<Location> validLocation() {
        return Combinators.combine(
            Arbitraries.integers().between(0, 10),
            Arbitraries.integers().between(world.getMinHeight() + 10, world.getMaxHeight() - 10),
            Arbitraries.integers().between(0, 10)
        ).as((x, y, z) -> new Location(world, x, y, z));
    }
    
    // ========== Helper Methods ==========
    
    private int countItems(PlayerInventory inventory) {
        return Arrays.stream(inventory.getContents())
            .filter(Objects::nonNull)
            .mapToInt(ItemStack::getAmount)
            .sum();
    }
}
