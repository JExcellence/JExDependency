package com.raindropcentral.rplatform.view.anvil;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import me.devnatan.inventoryframework.runtime.thirdparty.InventoryUpdate;
import me.devnatan.inventoryframework.runtime.thirdparty.McVersion;
import me.devnatan.inventoryframework.runtime.thirdparty.ReflectionUtils;
import me.devnatan.inventoryframework.runtime.thirdparty.ReflectionUtils.FakeEntityPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomAnvilInputNMSTest {

    private ServerMock server;
    private Player player;
    private FakeEntityPlayer entityPlayer;

    @BeforeAll
    static void registerFakeNmsClasses() {
        ReflectionUtils.mapNMSClass("world.inventory", "ContainerAnvil", FakeAnvilContainer.class);
        ReflectionUtils.mapNMSClass("world.entity.player", "PlayerInventory", ReflectionUtils.FakePlayerInventory.class);
        ReflectionUtils.mapNMSClass("world.inventory", "ContainerPlayer", ReflectionUtils.FakeContainerPlayer.class);
        ReflectionUtils.mapNMSClass("world.inventory.ICrafting", ReflectionUtils.FakeICrafting.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionUtils.reset();
        InventoryUpdate.reset();
        McVersion.reset();
        FakeAnvilContainer.reset();

        server = MockBukkit.mock();
        player = server.addPlayer();
        entityPlayer = ReflectionUtils.associate(player);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void openUsesContainerPacketWhenAvailable() throws Throwable {
        final PacketRecorder recorder = new PacketRecorder();
        InventoryUpdate.setPacketHandle(recorder.modernHandle());
        InventoryUpdate.setUseContainers(true);

        final AnvilInventory anvilInventory = mock(AnvilInventory.class);
        FakeAnvilContainer.setViewSupplier(() -> new FakeBukkitView(anvilInventory));

        final Inventory inventory = CustomAnvilInputNMS.open(player, "Title", "Initial");

        assertSame(anvilInventory, inventory);

        final FakeAnvilContainer container = FakeAnvilContainer.lastCreated();
        assertNotNull(container);
        assertFalse(container.isCheckReachable());
        assertEquals(1, container.getWindowId());
        assertSame(container, entityPlayer.getActiveContainer());
        assertSame(container, entityPlayer.getLastInitializedMenu());

        final ArgumentCaptor<ItemStack> slotCaptor = ArgumentCaptor.forClass(ItemStack.class);
        verify(anvilInventory).setMaximumRepairCost(0);
        verify(anvilInventory).setItem(eq(0), slotCaptor.capture());

        final ItemStack paper = slotCaptor.getValue();
        assertEquals(Material.PAPER, paper.getType());
        final ItemMeta meta = paper.getItemMeta();
        assertNotNull(meta);
        assertEquals("Initial", meta.getDisplayName());

        final Object[] packetArgs = recorder.getArguments();
        assertEquals(3, packetArgs.length);
        assertEquals(1, packetArgs[0]);
        assertSame(InventoryUpdate.Containers.ANVIL, packetArgs[1]);
        assertEquals("Title", packetArgs[2]);
        assertSame(recorder.getPacket(), ReflectionUtils.getLastPacket(player));
    }

    @Test
    void openFallsBackToLegacyPacketWhenContainersUnavailable() throws Throwable {
        final PacketRecorder recorder = new PacketRecorder();
        InventoryUpdate.setPacketHandle(recorder.legacyHandle());
        InventoryUpdate.setUseContainers(false);

        final AnvilInventory anvilInventory = mock(AnvilInventory.class);
        FakeAnvilContainer.setViewSupplier(() -> new FakeBukkitView(anvilInventory));

        final Inventory inventory = CustomAnvilInputNMS.open(player, null, "Fallback");

        assertSame(anvilInventory, inventory);

        final Object[] packetArgs = recorder.getArguments();
        assertEquals(4, packetArgs.length);
        assertEquals(1, packetArgs[0]);
        assertSame(InventoryType.ANVIL, packetArgs[1]);
        assertEquals("", packetArgs[2]);
        assertEquals(InventoryType.ANVIL.getDefaultSize(), packetArgs[3]);
    }

    @Test
    void openUsesSlotListenerWhenModernInitUnsupported() throws Throwable {
        final PacketRecorder recorder = new PacketRecorder();
        InventoryUpdate.setPacketHandle(recorder.modernHandle());
        InventoryUpdate.setUseContainers(true);
        McVersion.setSupportsModern(false);

        final AnvilInventory anvilInventory = mock(AnvilInventory.class);
        FakeAnvilContainer.setViewSupplier(() -> new FakeBukkitView(anvilInventory));

        CustomAnvilInputNMS.open(player, "Title", "Listener");

        final FakeAnvilContainer container = FakeAnvilContainer.lastCreated();
        assertNotNull(container);
        assertSame(player, container.getLastListener());
        assertNull(entityPlayer.getLastInitializedMenu());
    }

    private static final class PacketRecorder {

        private Object[] arguments = new Object[0];
        private Object packet = new Object();

        MethodHandle modernHandle() throws NoSuchMethodException, IllegalAccessException {
            return MethodHandles.lookup()
                                .findVirtual(PacketRecorder.class,
                                    "recordModern",
                                    MethodType.methodType(Object.class, int.class, Object.class, Object.class))
                                .bindTo(this);
        }

        MethodHandle legacyHandle() throws NoSuchMethodException, IllegalAccessException {
            return MethodHandles.lookup()
                                .findVirtual(PacketRecorder.class,
                                    "recordLegacy",
                                    MethodType.methodType(Object.class, int.class, Object.class, Object.class, int.class))
                                .bindTo(this);
        }

        private Object recordModern(int windowId, Object container, Object title) {
            arguments = new Object[] {windowId, container, title};
            packet = new Object();
            return packet;
        }

        private Object recordLegacy(int windowId, Object container, Object title, int size) {
            arguments = new Object[] {windowId, container, title, size};
            packet = new Object();
            return packet;
        }

        Object[] getArguments() {
            return arguments;
        }

        Object getPacket() {
            return packet;
        }
    }

    private static final class FakeBukkitView {

        private final AnvilInventory inventory;

        private FakeBukkitView(AnvilInventory inventory) {
            this.inventory = inventory;
        }

        public AnvilInventory getTopInventory() {
            return inventory;
        }
    }

    private static final class FakeAnvilContainer extends InventoryUpdate.InventoryContainer {

        private static Supplier<Object> supplier = () -> null;
        private static FakeAnvilContainer last;

        private final ReflectionUtils.FakePlayerInventory inventory;

        private FakeAnvilContainer(int windowId, ReflectionUtils.FakePlayerInventory inventory) {
            this.inventory = inventory;
            setWindowId(windowId);
            setBukkitView(supplier.get());
            last = this;
        }

        static void setViewSupplier(Supplier<Object> viewSupplier) {
            supplier = viewSupplier;
        }

        static FakeAnvilContainer lastCreated() {
            return last;
        }

        static void reset() {
            supplier = () -> null;
            last = null;
        }

        public ReflectionUtils.FakePlayerInventory getInventory() {
            return inventory;
        }
    }
}
