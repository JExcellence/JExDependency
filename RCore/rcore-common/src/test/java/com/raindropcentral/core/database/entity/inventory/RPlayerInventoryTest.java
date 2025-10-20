package com.raindropcentral.core.database.entity.inventory;

import com.raindropcentral.core.database.entity.player.RPlayer;
import com.raindropcentral.core.database.entity.server.RServer;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RPlayerInventoryTest {

    private RServer rServer;
    private RPlayer rPlayer;

    @BeforeEach
    void setUp() {
        this.rServer = new RServer(UUID.randomUUID(), "alpha");
        this.rPlayer = new RPlayer(UUID.randomUUID(), "TestUser");
    }

    @Test
    void constructorSurvivalModeCopiesInventorySnapshots() {
        final Player player = mock(Player.class);
        final PlayerInventory playerInventory = mock(PlayerInventory.class);
        final Inventory enderChest = mock(Inventory.class);

        final ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.setAmount(3);
        final ItemStack pickaxe = new ItemStack(Material.DIAMOND_PICKAXE);
        pickaxe.setAmount(2);
        final ItemStack[] mainContents = new ItemStack[36];
        mainContents[0] = sword;
        mainContents[5] = new ItemStack(Material.AIR);
        mainContents[9] = pickaxe;

        final ItemStack helmet = new ItemStack(Material.NETHERITE_HELMET);
        final ItemStack[] armorContents = new ItemStack[4];
        armorContents[0] = helmet;

        final ItemStack shulker = new ItemStack(Material.SHULKER_BOX);
        final ItemStack[] enderContents = new ItemStack[27];
        enderContents[10] = shulker;

        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(player.getInventory()).thenReturn(playerInventory);
        when(playerInventory.getContents()).thenReturn(mainContents);
        when(playerInventory.getArmorContents()).thenReturn(armorContents);
        when(player.getEnderChest()).thenReturn(enderChest);
        when(enderChest.getContents()).thenReturn(enderContents);

        final RPlayerInventory snapshot = new RPlayerInventory(this.rServer, this.rPlayer, player);

        final Map<Integer, ItemStack> storedInventory = snapshot.getInventory();
        assertEquals(2, storedInventory.size());
        assertEquals(Material.DIAMOND_SWORD, storedInventory.get(0).getType());
        assertEquals(3, storedInventory.get(0).getAmount());
        assertEquals(Material.DIAMOND_PICKAXE, storedInventory.get(9).getType());
        assertEquals(2, storedInventory.get(9).getAmount());
        assertFalse(storedInventory.containsKey(5), "AIR entries should be filtered out");

        sword.setAmount(7);
        assertEquals(3, storedInventory.get(0).getAmount(), "Stored snapshot should be a clone");

        final Map<Integer, ItemStack> storedArmor = snapshot.getArmor();
        assertEquals(1, storedArmor.size());
        assertEquals(Material.NETHERITE_HELMET, storedArmor.get(0).getType());

        final Map<Integer, ItemStack> storedEnder = snapshot.getEnderchest();
        assertEquals(1, storedEnder.size());
        assertEquals(Material.SHULKER_BOX, storedEnder.get(10).getType());

        assertEquals(4, snapshot.getTotalItemCount());
        assertFalse(snapshot.isEmpty());

        assertThrows(UnsupportedOperationException.class, () -> storedInventory.put(1, new ItemStack(Material.STONE)),
                "Inventory map should be immutable");
    }

    @Test
    void constructorCreativeModeSkipsSnapshotsAndLogs() {
        final Player player = mock(Player.class);
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
        when(player.getName()).thenReturn("CreativeUser");

        final Logger logger = CentralLogger.getLogger(RPlayerInventory.class.getName());
        final TestHandler handler = new TestHandler();
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);

        try {
            final RPlayerInventory snapshot = new RPlayerInventory(this.rServer, this.rPlayer, player);
            assertTrue(snapshot.getInventory().isEmpty());
            assertTrue(snapshot.getArmor().isEmpty());
            assertTrue(snapshot.getEnderchest().isEmpty());
            assertEquals(0, snapshot.getTotalItemCount());
            assertTrue(snapshot.isEmpty());
        } finally {
            logger.removeHandler(handler);
            handler.close();
        }

        assertTrue(handler.messages.stream()
                        .anyMatch(record -> record.getLevel() == Level.INFO
                                && record.getMessage().contains("CreativeUser")
                                && record.getMessage().contains("creative mode")),
                "Creative players should trigger an info log entry");
    }

    @Test
    void settersUseDefensiveCopiesForMaps() {
        final Player player = mockPlayerWithContents(
                GameMode.SURVIVAL,
                new ItemStack[9],
                new ItemStack[4],
                new ItemStack[3]
        );

        final RPlayerInventory snapshot = new RPlayerInventory(this.rServer, this.rPlayer, player);

        final Map<Integer, ItemStack> newInventory = new HashMap<>();
        newInventory.put(0, new ItemStack(Material.GOLD_BLOCK));
        snapshot.setInventory(newInventory);
        newInventory.put(1, new ItemStack(Material.EMERALD_BLOCK));

        final Map<Integer, ItemStack> storedInventory = snapshot.getInventory();
        assertEquals(1, storedInventory.size());
        assertFalse(storedInventory.containsKey(1));
        assertThrows(UnsupportedOperationException.class,
                () -> storedInventory.put(2, new ItemStack(Material.DIRT)));

        final Map<Integer, ItemStack> armorMap = new HashMap<>();
        armorMap.put(1, new ItemStack(Material.DIAMOND_CHESTPLATE));
        snapshot.setArmor(armorMap);
        armorMap.clear();

        final Map<Integer, ItemStack> storedArmor = snapshot.getArmor();
        assertEquals(1, storedArmor.size());
        assertThrows(UnsupportedOperationException.class, storedArmor::clear);

        final Map<Integer, ItemStack> enderMap = new HashMap<>();
        enderMap.put(5, new ItemStack(Material.ENDER_EYE));
        snapshot.setEnderchest(enderMap);
        enderMap.put(6, new ItemStack(Material.ENDER_PEARL));

        final Map<Integer, ItemStack> storedEnder = snapshot.getEnderchest();
        assertEquals(1, storedEnder.size());
        assertFalse(storedEnder.containsKey(6));
        assertThrows(UnsupportedOperationException.class,
                () -> storedEnder.put(7, new ItemStack(Material.SLIME_BALL)));

        final RPlayer replacementPlayer = new RPlayer(UUID.randomUUID(), "SecondUser");
        snapshot.setRPlayer(replacementPlayer);
        assertSame(replacementPlayer, snapshot.getRPlayer());

        final RServer replacementServer = new RServer(UUID.randomUUID(), "beta");
        snapshot.setRServer(replacementServer);
        assertSame(replacementServer, snapshot.getRServer());
    }

    @Test
    void updateFromPlayerRefreshesSnapshots() {
        final Player player = mock(Player.class);
        final PlayerInventory playerInventory = mock(PlayerInventory.class);
        final Inventory enderChest = mock(Inventory.class);

        final ItemStack[] initialMain = new ItemStack[9];
        initialMain[1] = new ItemStack(Material.STONE);
        final ItemStack[] initialArmor = new ItemStack[4];
        initialArmor[0] = new ItemStack(Material.LEATHER_HELMET);
        final ItemStack[] initialEnder = new ItemStack[3];
        initialEnder[1] = new ItemStack(Material.DIRT);

        final ItemStack emeraldBlock = new ItemStack(Material.EMERALD_BLOCK);
        emeraldBlock.setAmount(4);
        final ItemStack[] updatedMain = new ItemStack[9];
        updatedMain[3] = emeraldBlock;
        final ItemStack[] updatedArmor = new ItemStack[4];
        updatedArmor[2] = new ItemStack(Material.NETHERITE_LEGGINGS);
        final ItemStack[] updatedEnder = new ItemStack[3];
        updatedEnder[2] = new ItemStack(Material.DRAGON_EGG);

        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(player.getInventory()).thenReturn(playerInventory);
        when(playerInventory.getContents()).thenReturn(initialMain, updatedMain);
        when(playerInventory.getArmorContents()).thenReturn(initialArmor, updatedArmor);
        when(player.getEnderChest()).thenReturn(enderChest);
        when(enderChest.getContents()).thenReturn(initialEnder, updatedEnder);

        final RPlayerInventory snapshot = new RPlayerInventory(this.rServer, this.rPlayer, player);
        assertTrue(snapshot.getInventory().containsKey(1));

        snapshot.updateFromPlayer(player);

        final Map<Integer, ItemStack> storedInventory = snapshot.getInventory();
        assertFalse(storedInventory.containsKey(1));
        assertEquals(Material.EMERALD_BLOCK, storedInventory.get(3).getType());

        emeraldBlock.setAmount(1);
        assertEquals(4, storedInventory.get(3).getAmount());

        final Map<Integer, ItemStack> storedArmor = snapshot.getArmor();
        assertEquals(1, storedArmor.size());
        assertEquals(Material.NETHERITE_LEGGINGS, storedArmor.get(2).getType());

        final Map<Integer, ItemStack> storedEnder = snapshot.getEnderchest();
        assertEquals(1, storedEnder.size());
        assertEquals(Material.DRAGON_EGG, storedEnder.get(2).getType());
    }

    @Test
    void applyToPlayerRestoresSnapshots() {
        final Player source = mock(Player.class);
        final PlayerInventory sourceInventory = mock(PlayerInventory.class);
        final Inventory sourceEnder = mock(Inventory.class);

        final ItemStack[] sourceMain = new ItemStack[5];
        sourceMain[0] = new ItemStack(Material.BREAD);
        final ItemStack[] sourceArmor = new ItemStack[4];
        sourceArmor[1] = new ItemStack(Material.IRON_CHESTPLATE);
        final ItemStack[] sourceEnderContents = new ItemStack[3];
        sourceEnderContents[2] = new ItemStack(Material.ENDER_PEARL);

        when(source.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(source.getInventory()).thenReturn(sourceInventory);
        when(sourceInventory.getContents()).thenReturn(sourceMain);
        when(sourceInventory.getArmorContents()).thenReturn(sourceArmor);
        when(source.getEnderChest()).thenReturn(sourceEnder);
        when(sourceEnder.getContents()).thenReturn(sourceEnderContents);

        final RPlayerInventory snapshot = new RPlayerInventory(this.rServer, this.rPlayer, source);

        final Player target = mock(Player.class);
        final PlayerInventory targetInventory = mock(PlayerInventory.class);
        final Inventory targetEnder = mock(Inventory.class);

        when(target.getInventory()).thenReturn(targetInventory);
        when(targetInventory.getSize()).thenReturn(sourceMain.length);
        when(targetInventory.getArmorContents()).thenReturn(new ItemStack[sourceArmor.length]);
        when(target.getEnderChest()).thenReturn(targetEnder);
        when(targetEnder.getSize()).thenReturn(sourceEnderContents.length);

        snapshot.applyToPlayer(target);

        final ArgumentCaptor<ItemStack[]> mainCaptor = ArgumentCaptor.forClass(ItemStack[].class);
        verify(targetInventory).setContents(mainCaptor.capture());
        assertEquals(Material.BREAD, mainCaptor.getValue()[0].getType());
        assertNull(mainCaptor.getValue()[1]);

        final ArgumentCaptor<ItemStack[]> armorCaptor = ArgumentCaptor.forClass(ItemStack[].class);
        verify(targetInventory).setArmorContents(armorCaptor.capture());
        assertEquals(Material.IRON_CHESTPLATE, armorCaptor.getValue()[1].getType());

        final ArgumentCaptor<ItemStack[]> enderCaptor = ArgumentCaptor.forClass(ItemStack[].class);
        verify(targetEnder).setContents(enderCaptor.capture());
        assertEquals(Material.ENDER_PEARL, enderCaptor.getValue()[2].getType());
    }

    private static Player mockPlayerWithContents(
            final GameMode mode,
            final ItemStack[] inventory,
            final ItemStack[] armor,
            final ItemStack[] ender
    ) {
        final Player player = mock(Player.class);
        final PlayerInventory playerInventory = mock(PlayerInventory.class);
        final Inventory enderChest = mock(Inventory.class);

        when(player.getGameMode()).thenReturn(mode);
        when(player.getInventory()).thenReturn(playerInventory);
        when(playerInventory.getContents()).thenReturn(inventory);
        when(playerInventory.getArmorContents()).thenReturn(armor);
        when(player.getEnderChest()).thenReturn(enderChest);
        when(enderChest.getContents()).thenReturn(ender);

        return player;
    }

    private static final class TestHandler extends Handler {

        private final List<LogRecord> messages = new ArrayList<>();

        @Override
        public void publish(final LogRecord record) {
            this.messages.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
            this.messages.clear();
        }
    }
}
