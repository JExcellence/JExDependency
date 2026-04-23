package de.jexcellence.quests.machine;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Native-Bukkit storage inventory for a placed machine. Bypasses
 * InventoryFramework intentionally — the storage grid needs free
 * {@link org.bukkit.event.inventory.InventoryClickEvent} semantics
 * (pickup / place / swap / drop) that the framework cancels by
 * default. On close the {@link InventoryCloseEvent} listener matches
 * by {@link Holder} and persists.
 */
public final class MachineStorageInventory {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final int SIZE = 27;

    /** Map of open-inventory holders keyed by the viewing player's UUID. */
    private static final Map<UUID, Holder> OPEN = new HashMap<>();

    private MachineStorageInventory() {
    }

    /**
     * Builds and opens a storage inventory for {@code player},
     * populated from the machine's persisted {@code storage_data}.
     */
    public static void open(
            @NotNull org.bukkit.entity.Player player,
            @NotNull de.jexcellence.quests.database.entity.Machine machine,
            @NotNull MachineType type
    ) {
        final Holder holder = new Holder(machine.getId() != null ? machine.getId() : 0L);
        final Component title = MINI.deserialize(
                "<gradient:#fde047:#f59e0b>" + type.displayName() + " Storage</gradient>");
        final Inventory inventory = Bukkit.createInventory(holder, SIZE, title);
        holder.inventory = inventory;

        final ItemStack[] persisted = MachineStorageCodec.decode(machine.getStorageData(), SIZE);
        for (int slot = 0; slot < Math.min(persisted.length, SIZE); slot++) {
            if (persisted[slot] != null) inventory.setItem(slot, persisted[slot]);
        }

        OPEN.put(player.getUniqueId(), holder);
        player.openInventory(inventory);
    }

    /**
     * Hook invoked from a Bukkit {@link InventoryCloseEvent} listener.
     * Returns {@code true} when the close corresponds to a tracked
     * machine storage inventory and the contents were persisted.
     */
    public static boolean onClose(
            @NotNull HumanEntity who,
            @NotNull Inventory inventory,
            @NotNull BiConsumer<Long, String> persist
    ) {
        if (!(inventory.getHolder() instanceof Holder holder)) return false;
        OPEN.remove(who.getUniqueId());
        final ItemStack[] snapshot = new ItemStack[inventory.getSize()];
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            final ItemStack held = inventory.getItem(slot);
            snapshot[slot] = held != null ? held.clone() : null;
        }
        final String encoded = MachineStorageCodec.encode(snapshot);
        persist.accept(holder.machineId, encoded);
        return true;
    }

    /** Tag object so the close listener can identify a managed inventory. */
    static final class Holder implements InventoryHolder {
        private final long machineId;
        private Inventory inventory;

        private Holder(long machineId) {
            this.machineId = machineId;
        }

        long machineId() {
            return this.machineId;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return this.inventory;
        }
    }
}
