package com.raindropcentral.rdq.reward;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import de.jexcellence.evaluable.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItemRewardTest {

    private ServerMock server;
    private WorldMock world;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.world = this.server.addSimpleWorld("reward_world");
        this.player = this.server.addPlayer("RewardTester");
        this.player.teleport(this.world.getSpawnLocation());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void constructorRejectsMissingConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new ItemReward(null, 0, null, null, true));
    }

    @Test
    void constructorRequiresPositiveAmountForMaterial() {
        assertThrows(IllegalArgumentException.class, () -> new ItemReward(Material.DIAMOND, 0, null, null, true));
        assertThrows(IllegalArgumentException.class, () -> new ItemReward(Material.DIAMOND, -4, null, null, true));
    }

    @Test
    void materialRewardAddsRequestedAmount() {
        ItemReward reward = new ItemReward(Material.GOLD_INGOT, 16);

        reward.apply(this.player);

        int total = this.player.getInventory().all(Material.GOLD_INGOT).values().stream()
                .mapToInt(ItemStack::getAmount)
                .sum();
        assertEquals(16, total, "Material rewards should add the configured amount");
    }

    @Test
    void stackRewardClonesConfiguredStack() {
        ItemStack configured = new ItemStack(Material.EMERALD, 5);
        ItemReward reward = new ItemReward(configured);

        configured.setAmount(2);

        ItemStack retrievedFirst = reward.getItemStack();
        ItemStack retrievedSecond = reward.getItemStack();

        assertNotNull(retrievedFirst, "getItemStack should return a stack clone");
        assertNotNull(retrievedSecond, "getItemStack should return a stack clone");
        assertNotSame(retrievedFirst, retrievedSecond, "Each call should return a new clone");
        assertEquals(5, retrievedFirst.getAmount(), "Cloned stacks should retain the configured amount");
        assertEquals(5, retrievedSecond.getAmount(), "Cloned stacks should retain the configured amount");

        retrievedFirst.setAmount(1);
        assertEquals(5, retrievedSecond.getAmount(), "Mutating one clone should not affect the other");

        reward.apply(this.player);

        int total = this.player.getInventory().all(Material.EMERALD).values().stream()
                .mapToInt(ItemStack::getAmount)
                .sum();
        assertEquals(5, total, "Applying the reward should use the stored stack clone");
    }

    @Test
    void builderRewardBuildsAndGivesStack() {
        ItemBuilder builder = mock(ItemBuilder.class);
        ItemStack builtStack = new ItemStack(Material.NETHERITE_SCRAP, 3);
        when(builder.build()).thenReturn(builtStack);

        ItemReward reward = new ItemReward(null, 0, null, builder, true);

        reward.apply(this.player);

        verify(builder).build();
        int total = this.player.getInventory().all(Material.NETHERITE_SCRAP).values().stream()
                .mapToInt(ItemStack::getAmount)
                .sum();
        assertEquals(3, total, "Builder rewards should add the built stack to the inventory");
    }

    @Test
    void dropsOverflowWhenInventoryFull() {
        fillInventory(this.player);
        ItemReward reward = new ItemReward(Material.DIAMOND, 1, null, null, true);

        reward.apply(this.player);

        assertTrue(this.player.getWorld().getEntitiesByClass(Item.class).stream()
                .anyMatch(item -> item.getItemStack().getType() == Material.DIAMOND),
                "Overflow items should drop into the world when configured");
        assertEquals(0, this.player.getInventory().all(Material.DIAMOND).size(),
                "No diamonds should fit into a completely full inventory");
    }

    @Test
    void retainsLeftoversWhenDropDisabled() {
        fillInventory(this.player);
        ItemReward reward = new ItemReward(Material.DIAMOND, 1, null, null, false);

        reward.apply(this.player);

        List<Item> droppedItems = this.player.getWorld().getEntitiesByClass(Item.class);
        assertTrue(droppedItems.isEmpty(), "Overflow items should not be dropped when dropIfFull is false");
        assertEquals(0, this.player.getInventory().all(Material.DIAMOND).size(),
                "Inventory should remain unchanged when no space is available");
    }

    @Test
    void largeMaterialAmountsSplitIntoStacks() {
        ItemReward reward = new ItemReward(Material.DIAMOND, 130);

        reward.apply(this.player);

        Map<Integer, ? extends ItemStack> stacks = this.player.getInventory().all(Material.DIAMOND);
        List<Integer> amounts = stacks.values().stream()
                .map(ItemStack::getAmount)
                .sorted()
                .toList();

        assertEquals(List.of(2, 64, 64), amounts,
                "Large material amounts should be distributed across multiple stacks");
    }

    private static void fillInventory(final PlayerMock player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Material.STONE));
        }
    }
}
