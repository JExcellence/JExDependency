package de.jexcellence.quests.machine;

import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.database.entity.Machine;
import de.jexcellence.quests.service.MachineService;
import de.jexcellence.quests.view.MachineControllerView;
import de.jexcellence.jextranslate.R18nManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

/**
 * Handles the world-side machine lifecycle: place, break, interact.
 *
 * <ul>
 *   <li><b>Place</b> — a tagged item placed as a block registers a
 *       {@link Machine} row owned by the placer.</li>
 *   <li><b>Break</b> — only the owner (or {@code jexquests.machine.admin})
 *       can dismantle; the machine row is flagged {@code dismantled}
 *       and the block drops a fresh machine item (vanilla drops
 *       cancelled to prevent duplication).</li>
 *   <li><b>Interact</b> — right-clicking a placed-machine block opens
 *       the {@link MachineControllerView}; other interactions are left
 *       alone.</li>
 * </ul>
 */
public final class MachinePlacementListener implements Listener {

    private static final String ADMIN_PERM = "jexquests.machine.admin";

    private final JavaPlugin plugin;
    private final JExQuests quests;
    private final MachineService machines;
    private final MachineRegistry registry;
    private final JExLogger logger;

    public MachinePlacementListener(@NotNull JExQuests quests) {
        this.plugin = quests.getPlugin();
        this.quests = quests;
        this.machines = quests.machineService();
        this.registry = quests.machineRegistry();
        this.logger = quests.logger();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(@NotNull BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final String typeKey = MachineItem.typeKeyOf(this.plugin, event.getItemInHand());
        if (typeKey == null) return;

        final Optional<MachineType> type = this.registry.get(typeKey);
        if (type.isEmpty()) {
            event.setCancelled(true);
            r18n().msg("machine.not-found").prefix().with("type", typeKey).send(player);
            return;
        }

        final String facing = cardinal(player.getLocation().getYaw());
        this.machines.placeAsync(
                player.getUniqueId(),
                typeKey,
                event.getBlockPlaced().getLocation(),
                facing
        ).thenAccept(machine -> {
            if (machine == null) {
                this.logger.warn("machine place returned null for {}", typeKey);
                return;
            }
            r18n().msg("machine.place.success").prefix()
                    .with("type", typeKey)
                    .send(player);
        });
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(@NotNull BlockBreakEvent event) {
        final Block block = event.getBlock();
        final Location loc = block.getLocation();
        final Player player = event.getPlayer();
        this.machines.findAtAsync(loc).thenAccept(opt -> {
            if (opt.isEmpty()) return;
            final Machine machine = opt.get();

            if (!machine.getOwnerUuid().equals(player.getUniqueId())
                    && !player.hasPermission(ADMIN_PERM)) {
                // run on main thread so we can safely cancel the event
                this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                    event.setCancelled(true);
                    r18n().msg("error.no-permission").prefix().send(player);
                });
                return;
            }

            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                event.setDropItems(false);
                this.registry.get(machine.getMachineType()).ifPresent(type ->
                        block.getWorld().dropItemNaturally(
                                loc.add(0.5, 0.5, 0.5),
                                MachineItem.createFor(this.plugin, type)));
                this.machines.dismantleAsync(machine);
                r18n().msg("machine.destroyed").prefix()
                        .with("type", machine.getMachineType())
                        .send(player);
            });
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;
        if (!event.getAction().isRightClick()) return;

        final Location loc = event.getClickedBlock().getLocation();
        final Player player = event.getPlayer();
        this.machines.findAtAsync(loc).thenAccept(opt -> {
            if (opt.isEmpty()) return;
            final Machine machine = opt.get();
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                event.setCancelled(true);
                this.quests.viewFrame().open(
                        MachineControllerView.class,
                        player,
                        Map.of("plugin", this.quests, "machine", machine)
                );
            });
        });
    }

    private static @NotNull String cardinal(float yaw) {
        final float normalised = ((yaw % 360f) + 360f) % 360f;
        if (normalised < 45f || normalised >= 315f) return "SOUTH";
        if (normalised < 135f) return "WEST";
        if (normalised < 225f) return "NORTH";
        return "EAST";
    }

    private static R18nManager r18n() {
        return R18nManager.getInstance();
    }
}
