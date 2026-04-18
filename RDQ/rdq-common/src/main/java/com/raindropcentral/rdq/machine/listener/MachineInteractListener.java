/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdq.machine.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.machine.Machine;
import com.raindropcentral.rdq.machine.IMachineService;
import com.raindropcentral.rdq.machine.MachineManager;
import com.raindropcentral.rdq.machine.structure.MultiBlockStructure;
import com.raindropcentral.rdq.machine.structure.StructureDetector;
import com.raindropcentral.rdq.machine.structure.StructureValidator;
import com.raindropcentral.rdq.machine.view.MachineMainView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

/**
 * Listens to player interaction events for machine GUI opening.
 *
 * <p>This listener monitors right-click interactions with machine core blocks.
 * When a player interacts with a machine, it validates trust permissions and
 * opens the appropriate GUI based on the machine type and interaction context.
 *
 * <p>The listener ensures that only authorized players (owners and trusted players)
 * can access machine GUIs, preventing unauthorized access to machine resources
 * and configuration.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.0
 */
public class MachineInteractListener implements Listener {

    private final RDQ rdq;
    private final IMachineService machineService;
    private final MachineManager machineManager;
    private final StructureDetector structureDetector;
    private final StructureValidator structureValidator;
    private final ViewFrame viewFrame;

    /**
     * Creates a machine interact listener and automatically registers it.
     *
     * @param rdq the RDQ instance providing access to all dependencies
     */
    public MachineInteractListener(@NotNull final RDQ rdq) {
        this.rdq = rdq;
        this.machineService = rdq.getMachineService();
        this.machineManager = rdq.getMachineManager();
        this.structureDetector = rdq.getStructureDetector();
        this.structureValidator = new StructureValidator();
        this.viewFrame = rdq.getViewFrame();
    }

    /**
     * Handles player interaction events for machine GUI opening.
     *
     * <p>This method checks if the player right-clicked a machine core block.
     * If so, it validates trust permissions and opens the appropriate GUI.
     *
     * @param event the player interact event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(@NotNull final PlayerInteractEvent event) {
        // Only handle right-click on blocks
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        final Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        final Player player = event.getPlayer();
        final Location location = clickedBlock.getLocation();

        // Check if this is a machine core block
        if (!structureDetector.isCoreBlock(clickedBlock.getType())) {
            return;
        }

        // Check if there's a machine at this location
        final Optional<Machine> machineOpt = machineManager.getActiveMachine(location);
        if (machineOpt.isEmpty()) {
            return;
        }

        final Machine machine = machineOpt.get();

        // Cancel the event to prevent default block interaction
        event.setCancelled(true);

        // Validate trust permissions
        if (!machineService.canInteract(player, machine)) {
            new  I18n.Builder("machine.interaction.no_permission", player)
                .build()
                .sendMessage();
            return;
        }

        // Open the appropriate GUI based on machine type
        openMachineGUI(player, machine);
    }

    /**
     * Opens the appropriate GUI for a machine.
     *
     * <p>This method determines which GUI to open based on the machine type
     * and player interaction context. Opens the main machine view with the
     * machine data passed as initial context.
     *
     * @param player  the player to open the GUI for
     * @param machine the machine to open the GUI for
     */
    private void openMachineGUI(@NotNull final Player player, @NotNull final Machine machine) {
        // Detect and get the structure for this machine
        final StructureDetector.DetectionResult detectionResult = 
            structureDetector.detectAndValidate(machine.getLocation());
        
        if (!detectionResult.isValid() || detectionResult.getStructure() == null) {
            rdq.getPlugin().getLogger().warning("No valid structure found for machine at: " + machine.getLocation());
            new I18n.Builder("machine.error.no_structure", player)
                .build()
                .sendMessage();
            return;
        }
        
        final MultiBlockStructure structure = detectionResult.getStructure();
        
        switch (machine.getMachineType()) {
            case FABRICATOR -> {
                viewFrame.open(MachineMainView.class, player, Map.of(
                    "machine", machine,
                    "machineService", machineService,
                    "structure", structure,
                    "validator", structureValidator,
                    "plugin", rdq
                ));
            }
        }
    }
}
