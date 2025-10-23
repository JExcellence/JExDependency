package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.WorldMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class BountyRewardViewTest {

    private ServerMock server;
    private BountyRewardView view;
    private MockedStatic<TranslationService> translations;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.view = new BountyRewardView();
    }

    @AfterEach
    void tearDown() {
        if (this.translations != null) {
            this.translations.close();
            this.translations = null;
        }

        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    @Test
    void splitToMaxStacksRespectsItemStackLimits() throws Exception {
        ItemStack diamonds = new ItemStack(Material.DIAMOND);
        RewardItem rewardItem = new RewardItem(diamonds);
        int maxStack = diamonds.getMaxStackSize();
        rewardItem.setAmount(maxStack + 32);

        Method method = BountyRewardView.class.getDeclaredMethod("splitToMaxStacks", RewardItem.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<ItemStack> stacks = (List<ItemStack>) method.invoke(this.view, rewardItem);

        assertEquals(2, stacks.size());
        assertEquals(maxStack, stacks.get(0).getAmount());
        assertEquals(32, stacks.get(1).getAmount());
        stacks.forEach(stack -> assertTrue(stack.getAmount() <= maxStack));
    }

    @Test
    void findFirstPaneSlotIdentifiesFirstMatchingIndex() throws Exception {
        Inventory inventory = this.server.createInventory(null, 9);
        inventory.setItem(0, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        inventory.setItem(1, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        inventory.setItem(2, new ItemStack(Material.LIME_STAINED_GLASS_PANE));
        inventory.setItem(5, new ItemStack(Material.GREEN_STAINED_GLASS_PANE));

        Method method = BountyRewardView.class.getDeclaredMethod(
                "findFirstPaneSlot",
                Inventory.class,
                Set.class
        );
        method.setAccessible(true);

        int slot = (int) method.invoke(
                this.view,
                inventory,
                Set.of(Material.LIME_STAINED_GLASS_PANE, Material.GREEN_STAINED_GLASS_PANE)
        );

        assertEquals(2, slot);
    }

    @Test
    void refundInsertedItemsStacksInventoryAndDropsOverflow() throws Exception {
        PlayerMock player = this.server.addPlayer("Contributor");
        WorldMock world = Mockito.spy(this.server.addSimpleWorld("bounty-world"));
        player.teleport(world.getSpawnLocation());

        fillInventory(player);

        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 16));

        ItemStack diamonds = new ItemStack(Material.DIAMOND, 32);
        ItemStack emeralds = new ItemStack(Material.EMERALD, 5);
        Collection<ItemStack> items = List.of(diamonds, emeralds);

        mockTranslationService();

        AtomicReference<Location> droppedLocation = new AtomicReference<>();
        AtomicReference<ItemStack> droppedItem = new AtomicReference<>();
        doAnswer(invocation -> {
            Location location = invocation.getArgument(0);
            ItemStack dropped = invocation.getArgument(1);
            droppedLocation.set(location);
            droppedItem.set(dropped);
            return null;
        }).when(world).dropItem(any(Location.class), any(ItemStack.class));

        Method method = BountyRewardView.class.getDeclaredMethod(
                "refundInsertedItems",
                Player.class,
                Collection.class
        );
        method.setAccessible(true);
        method.invoke(this.view, player, items);

        ItemStack stackedDiamonds = player.getInventory().getItem(0);
        assertNotNull(stackedDiamonds);
        assertEquals(48, stackedDiamonds.getAmount());

        Mockito.verify(world).dropItem(any(Location.class), any(ItemStack.class));

        assertNotNull(droppedItem.get());
        assertEquals(Material.EMERALD, droppedItem.get().getType());
        assertEquals(5, droppedItem.get().getAmount());

        assertNotNull(droppedLocation.get());
        assertEquals(world, droppedLocation.get().getWorld());
        assertEquals(player.getLocation().getX(), droppedLocation.get().getX());
        assertEquals(player.getLocation().getY() + 0.5d, droppedLocation.get().getY());
        assertEquals(player.getLocation().getZ(), droppedLocation.get().getZ());
    }

    private void mockTranslationService() {
        this.translations = Mockito.mockStatic(TranslationService.class);
        this.translations.when(() -> TranslationService.create(any(TranslationKey.class), any(Player.class)))
                .thenAnswer(invocation -> createTranslationBuilder(
                        invocation.getArgument(0),
                        invocation.getArgument(1)
                ));
    }

    private TranslationService createTranslationBuilder(TranslationKey key, Player player) {
        TranslationService builder = mock(TranslationService.class);
        TranslatedMessage message = mock(TranslatedMessage.class);
        Mockito.when(builder.withAll(any())).thenReturn(builder);
        Mockito.when(builder.withPrefix()).thenReturn(builder);
        Mockito.when(builder.withPrefix(any(TranslationKey.class))).thenReturn(builder);
        Mockito.when(builder.build()).thenReturn(message);
        Mockito.doNothing().when(builder).send();
        Mockito.doNothing().when(builder).sendTitle();
        Mockito.doNothing().when(builder).sendActionBar();
        return builder;
    }

    private void fillInventory(PlayerMock player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Material.STONE, 64));
        }
    }
}
