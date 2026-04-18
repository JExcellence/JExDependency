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

package com.raindropcentral.rdq.machine.view;

import com.raindropcentral.rdq.database.entity.machine.Machine;
import com.raindropcentral.rdq.machine.IMachineService;
import com.raindropcentral.rdq.machine.structure.MultiBlockStructure;
import com.raindropcentral.rdq.machine.structure.StructureValidator;
import com.raindropcentral.rdq.machine.type.EMachineState;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Main machine GUI view displaying machine status, controls, and navigation.
 *
 * <p>This view provides:
 * <ul>
 *   <li>Machine state toggle (ON/OFF)</li>
 *   <li>Fuel level display</li>
 *   <li>Recipe preview</li>
 *   <li>Navigation buttons to other machine views</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class MachineMainView extends BaseView {

    private final State<IMachineService> machineService = initialState("machineService");
    private final State<Machine> machine = initialState("machine");
    private final State<MultiBlockStructure> structure = initialState("structure");
    private final State<StructureValidator> validator = initialState("validator");
    private final State<Plugin> plugin = initialState("plugin");

    /**
     * Constructs a new {@code MachineMainView}.
     */
    public MachineMainView() {
        super();
    }

    @Override
    protected String getKey() {
        return "view.machine.main";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "XXXXXXXXX",
            "XsTfRXXXX",
            "XXXXXXXXX",
            "XnNNNNXXX",
            "XXXXXXXXX",
            "         "
        };
    }

    @Override
    public void onFirstRender(
        @NotNull final RenderContext render,
        @NotNull final Player player
    ) {
        // Render decoration
        render.layoutSlot('X', createFillItem(player));

        // Render state toggle button
        renderStateToggle(render, player);

        // Render machine type display
        renderMachineType(render, player);

        // Render fuel display
        renderFuelDisplay(render, player);

        // Render recipe preview
        renderRecipePreview(render, player);

        // Render navigation buttons
        renderNavigationButtons(render, player);
    }

    /**
     * Renders the machine state toggle button.
     *
     * @param render the render context
     * @param player the viewing player
     */
    private void renderStateToggle(
        @NotNull final RenderContext render,
        @NotNull final Player player
    ) {
        final boolean isActive = machine.get(render).isActive();
        final String stateKey = isActive ? "on" : "off";

        render.layoutSlot('s')
            .withItem(
                UnifiedBuilderFactory.item(isActive ? Material.GREEN_WOOL : Material.RED_WOOL)
                    .setName(
                        i18n("items.state." + stateKey + ".name", player)
                            .build()
                            .component()
                    )
                    .setLore(
                        i18n("items.state." + stateKey + ".lore", player)
                            .build()
                            .children()
                    )
                    .build()
            )
            .onClick(click -> {
                final boolean newState = !machine.get(click).isActive();
                
                // If turning ON, validate structure first
                if (newState) {
                    // Check if validator and structure are available
                    if (validator.get(click) != null && structure.get(click) != null) {
                        // Validate structure
                        final StructureValidator.ValidationResult validationResult = 
                            validator.get(click).validate(
                                machine.get(click).getLocation(),
                                structure.get(click)
                            );
                        
                        if (validationResult.isFailure()) {
                            // Structure is invalid - send error message
                            i18n("messages.structure-invalid", player)
                                .withPlaceholder("error", validationResult.getErrorMessage())
                                .build()
                                .sendMessage();
                            return;
                        }
                    }
                    
                    // Check if recipe is set
                    if (machine.get(click).getRecipeData() == null || 
                        machine.get(click).getRecipeData().isEmpty()) {
                        i18n("messages.no-recipe", player)
                            .build()
                            .sendMessage();
                        return;
                    }
                    
                    // Check if fuel is available
                    if (machine.get(click).getFuelLevel() <= 0) {
                        i18n("messages.no-fuel", player)
                            .build()
                            .sendMessage();
                        return;
                    }
                }
                
                // Toggle machine state
                machineService.get(click).toggleMachine(machine.get(click).getId(), newState)
                    .thenAccept(success -> {
                        // Get plugin for scheduler, fallback to machineService's plugin
                        Plugin pluginInstance = plugin.get(click);
                        if (pluginInstance == null && machineService.get(click) instanceof org.bukkit.plugin.Plugin) {
                            pluginInstance = (Plugin) machineService.get(click);
                        }
                        
                        if (pluginInstance != null) {
                            Bukkit.getScheduler().runTask(
                                pluginInstance,
                                () -> handleToggleResult(click, player, newState, success)
                            );
                        } else {
                            // Fallback: run directly (not ideal but prevents NPE)
                            handleToggleResult(click, player, newState, success);
                        }
                    })
                    .exceptionally(ex -> {
                        Plugin pluginInstance = plugin.get(click);
                        if (pluginInstance != null) {
                            Bukkit.getScheduler().runTask(
                                pluginInstance,
                                () -> i18n("messages.toggle-error", player)
                                    .withPlaceholder("error", ex.getMessage())
                                    .build()
                                    .sendMessage()
                            );
                        } else {
                            i18n("messages.toggle-error", player)
                                .withPlaceholder("error", ex.getMessage())
                                .build()
                                .sendMessage();
                        }
                        return null;
                    });
            });
    }
    
    /**
     * Handles the result of a toggle operation.
     */
    private void handleToggleResult(
        @NotNull final SlotClickContext click,
        @NotNull final Player player,
        final boolean newState,
        final boolean success
    ) {
        if (success) {
            machine.get(click).setState(newState ? EMachineState.ACTIVE : EMachineState.INACTIVE);
            
            i18n("messages.state-" + (newState ? "on" : "off"), player)
                .build()
                .sendMessage();
            
            click.update();
        } else {
            i18n("messages.toggle-failed", player)
                .build()
                .sendMessage();
        }
    }

    /**
     * Renders the machine type display.
     *
     * @param render the render context
     * @param player the viewing player
     */
    private void renderMachineType(
        @NotNull final RenderContext render,
        @NotNull final Player player
    ) {
        render.layoutSlot('T')
            .withItem(
                UnifiedBuilderFactory.item(Material.DROPPER)
                    .setName(
                        i18n("items.type.name", player)
                            .withPlaceholder("type", machine.get(render).getMachineType().name())
                            .build()
                            .component()
                    )
                    .setLore(
                        i18n("items.type.lore", player)
                            .withPlaceholder("type", machine.get(render).getMachineType().name())
                            .build()
                            .children()
                    )
                    .build()
            );
    }

    /**
     * Renders the fuel level display.
     *
     * @param render the render context
     * @param player the viewing player
     */
    private void renderFuelDisplay(
        @NotNull final RenderContext render,
        @NotNull final Player player
    ) {
        render.layoutSlot('f')
            .withItem(
                UnifiedBuilderFactory.item(Material.COAL)
                    .setName(
                        i18n("items.fuel.name", player)
                            .withPlaceholder("current", machine.get(render).getFuelLevel())
                            .build()
                            .component()
                    )
                    .setLore(
                        i18n("items.fuel.lore", player)
                            .withPlaceholder("current", machine.get(render).getFuelLevel())
                            .build()
                            .children()
                    )
                    .build()
            )
            .onClick(click -> {
                // Open fuel management (could be a separate view or inline)
                // For now, just close
                click.getPlayer().closeInventory();
            });
    }

    /**
     * Renders the recipe preview.
     *
     * @param render the render context
     * @param player the viewing player
     */
    private void renderRecipePreview(
        @NotNull final RenderContext render,
        @NotNull final Player player
    ) {
        final boolean hasRecipe = machine.get(render).getRecipeData() != null && !machine.get(render).getRecipeData().isEmpty();

        render.layoutSlot('R')
            .withItem(
                UnifiedBuilderFactory.item(hasRecipe ? Material.CRAFTING_TABLE : Material.BARRIER)
                    .setName(
                        i18n("items.recipe." + (hasRecipe ? "set" : "not-set") + ".name", player)
                            .build()
                            .component()
                    )
                    .setLore(
                        i18n("items.recipe." + (hasRecipe ? "set" : "not-set") + ".lore", player)
                            .build()
                            .children()
                    )
                    .build()
            )
            .onClick(click -> {
                // Open recipe configuration view
                click.openForPlayer(MachineRecipeView.class, Map.of(
                    "machineService", machineService.get(click),
                    "machine", machine.get(click),
                    "insertedItems", new HashMap<UUID, Map<Integer, ItemStack>>()
                ));
            });
    }

    /**
     * Renders navigation buttons to other machine views.
     *
     * @param render the render context
     * @param player the viewing player
     */
    @Override
    public void renderNavigationButtons(
        @NotNull final RenderContext render,
        @NotNull final Player player
    ) {
        // Storage button
        render.layoutSlot('n')
            .withItem(
                UnifiedBuilderFactory.item(Material.CHEST)
                    .setName(
                        i18n("items.navigation.storage.name", player)
                            .build()
                            .component()
                    )
                    .setLore(
                        i18n("items.navigation.storage.lore", player)
                            .build()
                            .children()
                    )
                    .build()
            )
            .onClick(click -> {
                click.openForPlayer(MachineStorageView.class, Map.of(
                    "machineService", machineService.get(click),
                    "machine", machine.get(click)
                ));
            });

        // Navigation buttons for N slots
        render.layoutSlot('N', (index, item) -> {
            if (index == 0) {
                // Trust button
                item.withItem(
                    UnifiedBuilderFactory.item(Material.PLAYER_HEAD)
                        .setName(
                            i18n("items.navigation.trust.name", player)
                                .build()
                                .component()
                        )
                        .setLore(
                            i18n("items.navigation.trust.lore", player)
                                .withPlaceholder("count", machine.get(render).getTrustedPlayers().size())
                                .build()
                                .children()
                        )
                        .build()
                );
                item.onClick(click -> {
                    click.openForPlayer(MachineTrustView.class, Map.of(
                        "machineService", machineService.get(click),
                        "machine", machine.get(click)
                    ));
                });
            } else if (index == 1) {
                // Upgrades button
                item.withItem(
                    UnifiedBuilderFactory.item(Material.ENCHANTING_TABLE)
                        .setName(
                            i18n("items.navigation.upgrades.name", player)
                                .build()
                                .component()
                        )
                        .setLore(
                            i18n("items.navigation.upgrades.lore", player)
                                .build()
                                .children()
                        )
                        .build()
                );
                item.onClick(click -> {
                    click.openForPlayer(MachineUpgradeView.class, Map.of(
                        "machineService", machineService.get(click),
                        "machine", machine.get(click)
                    ));
                });
            } else {
                // Fill remaining slots
                item.withItem(createFillItem(player));
            }
        });
    }
}
