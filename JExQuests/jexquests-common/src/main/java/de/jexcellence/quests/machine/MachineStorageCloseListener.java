package de.jexcellence.quests.machine;

import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.database.entity.Machine;
import de.jexcellence.quests.service.MachineService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Persists machine-storage changes when the player closes the native
 * {@link org.bukkit.inventory.Inventory} opened by
 * {@link MachineStorageInventory}. The heavy lifting lives in the
 * inventory class — this listener just bridges the Bukkit close event
 * onto it with the plugin's machine service and logger.
 */
public final class MachineStorageCloseListener implements Listener {

    private final MachineService machines;
    private final JExLogger logger;

    public MachineStorageCloseListener(@NotNull JExQuests quests) {
        this.machines = quests.machineService();
        this.logger = quests.logger();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(@NotNull InventoryCloseEvent event) {
        MachineStorageInventory.onClose(event.getPlayer(), event.getInventory(), (machineId, encoded) -> {
            this.machines.findByIdAsync(machineId).thenAccept(opt -> {
                if (opt.isEmpty()) return;
                final Machine machine = opt.get();
                this.machines.writeStorageAsync(machine, encoded).exceptionally(ex -> {
                    this.logger.error("storage save failed for machine {}: {}", machineId, ex.getMessage());
                    return machine;
                });
            });
        });
    }
}
