package me.devnatan.inventoryframework.runtime.thirdparty;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test stub that mimics the minimal reflective hooks used by
 * {@link com.raindropcentral.rplatform.view.anvil.CustomAnvilInputNMS}.
 */
public final class ReflectionUtils {

    public static final Class<?> ENTITY_PLAYER = FakeEntityPlayer.class;

    private static final Map<String, Class<?>> NMS_CLASSES = new HashMap<>();
    private static final Map<Player, FakeEntityPlayer> ENTITIES = new ConcurrentHashMap<>();
    private static final Map<Player, Object> SENT_PACKETS = new ConcurrentHashMap<>();

    private static boolean supportsMc1202;

    private ReflectionUtils() {}

    public static void reset() {
        SENT_PACKETS.clear();
        ENTITIES.clear();
        supportsMc1202 = false;
    }

    public static void mapNMSClass(String path, Class<?> type) {
        NMS_CLASSES.put(path, type);
    }

    public static void mapNMSClass(String pkg, String name, Class<?> type) {
        mapNMSClass(pkg + '.' + name, type);
    }

    public static Class<?> getNMSClass(String path) {
        final Class<?> type = NMS_CLASSES.get(path);
        if (type == null) {
            throw new IllegalStateException("No mapping registered for " + path);
        }
        return type;
    }

    public static Class<?> getNMSClass(String pkg, String name) {
        return getNMSClass(pkg + '.' + name);
    }

    public static FakeEntityPlayer associate(Player player) {
        final FakeEntityPlayer entity = new FakeEntityPlayer();
        ENTITIES.put(player, entity);
        return entity;
    }

    public static Object getEntityPlayer(Player player) {
        return Objects.requireNonNull(ENTITIES.get(player), "Player has not been associated with a fake entity");
    }

    public static void sendPacketSync(Player player, Object packet) {
        SENT_PACKETS.put(player, packet);
    }

    public static Object getLastPacket(Player player) {
        return SENT_PACKETS.get(player);
    }

    public static boolean supportsMC1202() {
        return supportsMc1202;
    }

    public static void setSupportsMc1202(boolean value) {
        supportsMc1202 = value;
    }

    public static String getVersionInformation() {
        return "TEST";
    }

    public static final class FakeEntityPlayer {

        private final FakePlayerInventory inventory = new FakePlayerInventory();
        private final InventoryUpdate.InventoryContainer inventoryMenu = new FakeContainerPlayer();
        private InventoryUpdate.InventoryContainer activeContainer = inventoryMenu;
        private int nextContainerId;
        private InventoryUpdate.InventoryContainer lastInitializedMenu;

        public int nextContainerCounter() {
            return ++nextContainerId;
        }

        public FakePlayerInventory fN() {
            return inventory;
        }

        public FakePlayerInventory fR() {
            return inventory;
        }

        public FakePlayerInventory getInventory() {
            return inventory;
        }

        public InventoryUpdate.InventoryContainer getInventoryMenu() {
            return inventoryMenu;
        }

        public InventoryUpdate.InventoryContainer getActiveContainer() {
            return activeContainer;
        }

        public void setActiveContainer(InventoryUpdate.InventoryContainer container) {
            this.activeContainer = container;
        }

        public InventoryUpdate.InventoryContainer getLastInitializedMenu() {
            return lastInitializedMenu;
        }

        public void a(InventoryUpdate.InventoryContainer container) {
            lastInitializedMenu = container;
        }

        public void setContainerMenu(InventoryUpdate.InventoryContainer container) {
            activeContainer = container;
        }

        public InventoryUpdate.InventoryContainer inventoryMenu() {
            return inventoryMenu;
        }
    }

    public static final class FakePlayerInventory {}

    public static class FakeContainerPlayer extends InventoryUpdate.InventoryContainer {}

    public static class FakeICrafting {}
}
