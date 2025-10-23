package com.raindropcentral.rdq.view.bounty;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import com.raindropcentral.rdq.service.BountyService;
import com.raindropcentral.rdq.service.BountyServiceProvider;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class BountyCreationViewTest {

    private ServerMock server;
    private PlayerMock player;
    private BountyCreationView view;
    private BountyService bountyService;
    private MockedStatic<TranslationService> translations;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.player = this.server.addPlayer("BountyCreator");
        this.view = new BountyCreationView();
        this.bountyService = Mockito.mock(BountyService.class);
        BountyServiceProvider.setInstance(this.bountyService);
        mockTranslationService();
    }

    @AfterEach
    void tearDown() {
        if (this.translations != null) {
            this.translations.close();
            this.translations = null;
        }

        BountyServiceProvider.reset();
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    @Test
    void handleConfirmInvokesServiceAndClearsInsertedItems() throws Exception {
        final Map<State<?>, Object> stateBacking = new HashMap<>();
        final RewardItem rewardItem = new RewardItem(new ItemStack(Material.DIAMOND, 8));
        final Set<RewardItem> rewardItems = new HashSet<>();
        rewardItems.add(rewardItem);

        final Map<UUID, Map<Integer, ItemStack>> insertedItems = new HashMap<>();
        final Map<Integer, ItemStack> playerItems = new HashMap<>();
        playerItems.put(5, new ItemStack(Material.EMERALD, 3));
        insertedItems.put(this.player.getUniqueId(), playerItems);

        assignStateValue("target", Optional.of(this.player), stateBacking);
        assignStateValue("rewardItems", rewardItems, stateBacking);
        assignStateValue("insertedItems", insertedItems, stateBacking);

        final SlotClickContext clickContext = createSlotClickContext(stateBacking);

        Mockito.when(this.bountyService.getBountyByPlayer(this.player.getUniqueId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final Method handleConfirm = BountyCreationView.class.getDeclaredMethod(
                "handleConfirm",
                SlotClickContext.class,
                Player.class
        );
        handleConfirm.setAccessible(true);

        handleConfirm.invoke(this.view, clickContext, this.player);

        verify(this.bountyService).getBountyByPlayer(this.player.getUniqueId());
        assertFalse(insertedItems.containsKey(this.player.getUniqueId()),
                "Inserted item cache should be cleared once the bounty is confirmed");
    }

    @Test
    void handleConfirmStopsWhenTargetMissing() throws Exception {
        final Map<State<?>, Object> stateBacking = new HashMap<>();
        final RewardItem rewardItem = new RewardItem(new ItemStack(Material.GOLD_INGOT, 4));
        final Set<RewardItem> rewardItems = new HashSet<>();
        rewardItems.add(rewardItem);

        final Map<UUID, Map<Integer, ItemStack>> insertedItems = new HashMap<>();
        insertedItems.put(this.player.getUniqueId(), new HashMap<>());

        assignStateValue("target", Optional.empty(), stateBacking);
        assignStateValue("rewardItems", rewardItems, stateBacking);
        assignStateValue("insertedItems", insertedItems, stateBacking);

        final SlotClickContext clickContext = createSlotClickContext(stateBacking);

        final Method handleConfirm = BountyCreationView.class.getDeclaredMethod(
                "handleConfirm",
                SlotClickContext.class,
                Player.class
        );
        handleConfirm.setAccessible(true);

        handleConfirm.invoke(this.view, clickContext, this.player);

        verify(this.bountyService, never()).getBountyByPlayer(any(UUID.class));
        assertTrue(insertedItems.containsKey(this.player.getUniqueId()),
                "Inserted item cache should remain untouched when no target is selected");
    }

    @Test
    void handleConfirmStopsWhenRewardsMissing() throws Exception {
        final Map<State<?>, Object> stateBacking = new HashMap<>();
        final Map<UUID, Map<Integer, ItemStack>> insertedItems = new HashMap<>();
        insertedItems.put(this.player.getUniqueId(), new HashMap<>());

        assignStateValue("target", Optional.of(this.player), stateBacking);
        assignStateValue("rewardItems", new HashSet<RewardItem>(), stateBacking);
        assignStateValue("insertedItems", insertedItems, stateBacking);

        final SlotClickContext clickContext = createSlotClickContext(stateBacking);

        final Method handleConfirm = BountyCreationView.class.getDeclaredMethod(
                "handleConfirm",
                SlotClickContext.class,
                Player.class
        );
        handleConfirm.setAccessible(true);

        handleConfirm.invoke(this.view, clickContext, this.player);

        verify(this.bountyService, never()).getBountyByPlayer(any(UUID.class));
        assertTrue(insertedItems.containsKey(this.player.getUniqueId()),
                "Inserted item cache should remain untouched when no rewards are provided");
    }

    @Test
    void onCloseRefundsInsertedItemsToInventoryAndWorld() throws Exception {
        final Map<State<?>, Object> stateBacking = new HashMap<>();
        final Map<UUID, Map<Integer, ItemStack>> insertedItems = new HashMap<>();
        final Map<Integer, ItemStack> playerItems = new HashMap<>();
        playerItems.put(2, new ItemStack(Material.DIAMOND, 16));
        playerItems.put(7, new ItemStack(Material.EMERALD, 4));
        insertedItems.put(this.player.getUniqueId(), playerItems);

        assignStateValue("target", Optional.empty(), stateBacking);
        assignStateValue("rewardItems", new HashSet<RewardItem>(), stateBacking);
        assignStateValue("insertedItems", insertedItems, stateBacking);

        final CloseContext closeContext = createCloseContext(stateBacking);

        final WorldMock world = Mockito.spy(this.server.addSimpleWorld("bounty-world"));
        this.player.teleport(world.getSpawnLocation());

        this.player.getInventory().clear();
        this.player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 32));
        for (int slot = 1; slot < this.player.getInventory().getSize(); slot++) {
            this.player.getInventory().setItem(slot, new ItemStack(Material.STONE, 64));
        }

        final AtomicReference<Location> dropLocation = new AtomicReference<>();
        final AtomicReference<ItemStack> droppedItem = new AtomicReference<>();
        Mockito.doAnswer(invocation -> {
            dropLocation.set(invocation.getArgument(0));
            droppedItem.set(invocation.getArgument(1));
            return null;
        }).when(world).dropItem(any(Location.class), any(ItemStack.class));

        this.view.onClose(closeContext);

        final ItemStack stackedDiamonds = this.player.getInventory().getItem(0);
        assertNotNull(stackedDiamonds, "Refunded diamonds should stack onto the existing slot");
        assertEquals(48, stackedDiamonds.getAmount(), "Stacked diamonds should reflect the refunded quantity");

        assertNotNull(droppedItem.get(), "Overflow items should be dropped into the world");
        assertEquals(Material.EMERALD, droppedItem.get().getType(), "Dropped item should match the leftover reward");
        assertEquals(4, droppedItem.get().getAmount(), "Dropped item should preserve its original amount");

        assertNotNull(dropLocation.get(), "Dropped item location should be captured");
        assertEquals(world, dropLocation.get().getWorld(), "Dropped items should appear in the player's world");
        assertEquals(this.player.getLocation().getX(), dropLocation.get().getX(), 1.0e-6,
                "Dropped item should respect the player's X coordinate");
        assertEquals(this.player.getLocation().getY() + 0.5d, dropLocation.get().getY(), 1.0e-6,
                "Dropped item should appear slightly above the player's feet");
        assertEquals(this.player.getLocation().getZ(), dropLocation.get().getZ(), 1.0e-6,
                "Dropped item should respect the player's Z coordinate");
    }

    private SlotClickContext createSlotClickContext(final Map<State<?>, Object> stateBacking) {
        final SlotClickContext context = Mockito.mock(SlotClickContext.class);
        Mockito.when(context.getPlayer()).thenReturn(this.player);
        Mockito.when(context.getRawStateValue(Mockito.any())).thenAnswer(invocation ->
                stateBacking.get(invocation.getArgument(0))
        );
        return context;
    }

    private CloseContext createCloseContext(final Map<State<?>, Object> stateBacking) {
        final CloseContext context = Mockito.mock(CloseContext.class);
        Mockito.when(context.getPlayer()).thenReturn(this.player);
        Mockito.when(context.getRawStateValue(Mockito.any())).thenAnswer(invocation ->
                stateBacking.get(invocation.getArgument(0))
        );
        return context;
    }

    private void assignStateValue(
            final String fieldName,
            final Object value,
            final Map<State<?>, Object> stateBacking
    ) throws ReflectiveOperationException {
        final Field field = BountyCreationView.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        final State<?> state = (State<?>) field.get(this.view);
        stateBacking.put(state, value);
    }

    private void mockTranslationService() {
        this.translations = Mockito.mockStatic(TranslationService.class);
        this.translations.when(() -> TranslationService.create(any(TranslationKey.class), any(Player.class)))
                .thenAnswer(invocation -> createTranslationBuilder(
                        invocation.getArgument(0),
                        invocation.getArgument(1)
                ));
    }

    private TranslationService createTranslationBuilder(final TranslationKey key, final Player player) {
        final TranslationService builder = Mockito.mock(TranslationService.class);
        final TranslatedMessage message = Mockito.mock(TranslatedMessage.class);
        Mockito.when(builder.withPrefix()).thenReturn(builder);
        Mockito.when(builder.withPrefix(any(TranslationKey.class))).thenReturn(builder);
        Mockito.when(builder.withAll(any())).thenReturn(builder);
        Mockito.when(builder.with(any(String.class), any())).thenReturn(builder);
        Mockito.when(builder.build()).thenReturn(message);
        Mockito.doNothing().when(builder).send();
        Mockito.doNothing().when(builder).sendTitle();
        Mockito.doNothing().when(builder).sendActionBar();
        Mockito.when(message.component()).thenReturn(Component.empty());
        Mockito.when(message.splitLines()).thenReturn(List.of());
        return builder;
    }
}
