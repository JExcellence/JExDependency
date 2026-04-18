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
import com.raindropcentral.rdq.database.entity.machine.MachineStorage;
import com.raindropcentral.rdq.machine.IMachineService;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated view for managing machine storage.
 *
 * <p>This view displays all items stored in the machine with pagination support,
 * allowing players to deposit and withdraw items.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class MachineStorageView extends APaginatedView<MachineStorage> {

    private final State<IMachineService> machineService = initialState("machineService");
    private final State<Machine> machine = initialState("machine");

    /**
     * Constructs a new {@code MachineStorageView}.
     */
    public MachineStorageView() {
        super(MachineMainView.class);
    }

    @Override
    protected String getKey() {
        return "view.machine.storage";
    }

    @Override
    protected CompletableFuture<List<MachineStorage>> getAsyncPaginationSource(
        @NotNull final Context context
    ) {
        // Load storage contents asynchronously
        return machineService.get(context).getStorageContents(machine.get(context).getId())
            .thenApply(storageMap -> {
                // Convert map to list of MachineStorage objects
                List<MachineStorage> storageList = new ArrayList<>();
                
                // Get existing storage entries from machine
                storageList.addAll(machine.get(context).getStorage());
                
                return storageList;
            });
    }

    @Override
    protected void renderEntry(
        @NotNull final Context context,
        @NotNull final BukkitItemComponentBuilder builder,
        final int index,
        @NotNull final MachineStorage entry
    ) {
        final Player player = context.getPlayer();
        
        // Deserialize item from storage
        final ItemStack storedItem = deserializeItem(entry.getItemData());
        if (storedItem == null) {
            return;
        }

        builder.withItem(
            UnifiedBuilderFactory.item(storedItem.getType())
                .setAmount(Math.min(entry.getQuantity(), 64))
                .setName(
                    i18n("items.storage-item.name", player)
                        .withPlaceholder("item", storedItem.getType().name())
                        .withPlaceholder("quantity", entry.getQuantity())
                        .build()
                        .component()
                )
                .setLore(
                    i18n("items.storage-item.lore", player)
                        .withPlaceholder("quantity", entry.getQuantity())
                        .withPlaceholder("type", entry.getStorageType().name())
                        .build()
                        .children()
                )
                .build()
        ).onClick(click -> {
            // Withdraw item on click
            handleWithdraw(click, click.getPlayer(), entry, storedItem.getType());
        });
    }

    @Override
    protected void onPaginatedRender(
        @NotNull final RenderContext render,
        @NotNull final Player player
    ) {
        // Add deposit button
        renderDepositButton(render, player);
        
        // Add filter buttons
        renderFilterButtons(render, player);
    }

    /**
     * Renders the deposit button.
     *
     * @param render the render context
     * @param player the viewing player
     */
    private void renderDepositButton(
        @NotNull final RenderContext render,
        @NotNull final Player player
    ) {
        render.slot(49)
            .withItem(
                UnifiedBuilderFactory.item(Material.HOPPER)
                    .setName(
                        i18n("items.deposit.name", player)
                            .build()
                            .component()
                    )
                    .setLore(
                        i18n("items.deposit.lore", player)
                            .build()
                            .children()
                    )
                    .build()
            )
            .onClick(click -> {
                // Get the item in the player's hand
                final ItemStack handItem = player.getInventory().getItemInMainHand();
                
                if (handItem.getType() == Material.AIR) {
                    i18n("messages.no-item-in-hand", player)
                        .build()
                        .sendMessage();
                    return;
                }
                
                // Deposit the item
                final ItemStack depositItem = handItem.clone();
                machineService.get(click).depositItems(
                    machine.get(click).getId(),
                    depositItem
                ).thenAccept(success -> {
                    if (success) {
                        // Remove item from player's hand
                        player.getInventory().setItemInMainHand(null);
                        
                        i18n("messages.item-deposited", player)
                            .withPlaceholder("amount", depositItem.getAmount())
                            .withPlaceholder("item", depositItem.getType().name())
                            .build()
                                    .sendMessage();
                        
                        // Refresh the view
                        click.update();
                    } else {
                        i18n("messages.storage-full", player)
                            .build()
                                .sendMessage();
                    }
                });
            });
    }

    /**
     * Renders filter buttons for storage types.
     *
     * @param render the render context
     * @param player the viewing player
     */
    private void renderFilterButtons(
        @NotNull final RenderContext render,
        @NotNull final Player player
    ) {
        // All items filter
        render.slot(46)
            .withItem(
                UnifiedBuilderFactory.item(Material.CHEST)
                    .setName(
                        i18n("items.filter.all.name", player)
                            .build()
                            .component()
                    )
                    .setLore(
                        i18n("items.filter.all.lore", player)
                            .build()
                            .children()
                    )
                    .build()
            )
            .onClick(click -> {
                // TODO: Implement filter functionality
                // For now, just update the view
                click.update();
            });

        // Input filter
        render.slot(47)
            .withItem(
                UnifiedBuilderFactory.item(Material.HOPPER)
                    .setName(
                        i18n("items.filter.input.name", player)
                            .build()
                            .component()
                    )
                    .setLore(
                        i18n("items.filter.input.lore", player)
                            .build()
                            .children()
                    )
                    .build()
            )
            .onClick(click -> {
                // TODO: Implement filter functionality
                // For now, just update the view
                click.update();
            });

        // Output filter
        render.slot(48)
            .withItem(
                UnifiedBuilderFactory.item(Material.DROPPER)
                    .setName(
                        i18n("items.filter.output.name", player)
                            .build()
                            .component()
                    )
                    .setLore(
                        i18n("items.filter.output.lore", player)
                            .build()
                            .children()
                    )
                    .build()
            )
            .onClick(click -> {
                // TODO: Implement filter functionality
                // For now, just update the view
                click.update();
            });
    }

    /**
     * Handles withdrawing an item from storage.
     *
     * @param player   the player withdrawing
     * @param entry    the storage entry
     * @param material the material to withdraw
     */
    private void handleWithdraw(
            @NotNull final SlotClickContext clickContext,
            @NotNull final Player player,
            @NotNull final MachineStorage entry,
            @NotNull final Material material
    ) {
        final int withdrawAmount = Math.min(entry.getQuantity(), 64);
        
        machineService.get(clickContext).withdrawItems(machine.get(clickContext).getId(), material, withdrawAmount)
            .thenAccept(withdrawnItem -> {
                if (withdrawnItem != null) {
                    // Give item to player
                    player.getInventory().addItem(withdrawnItem);
                    
                    // Update storage entry
                    entry.setQuantity(entry.getQuantity() - withdrawAmount);
                    if (entry.getQuantity() <= 0) {
                        machine.get(clickContext).removeStorage(entry);
                    }
                    
                    // Refresh view
                    // Note: In a real implementation, you'd want to update the view
                }
            });
    }

    /**
     * Deserializes an item from JSON data.
     *
     * @param itemData the JSON item data
     * @return the deserialized ItemStack, or null if deserialization fails
     */
    @Nullable
    private ItemStack deserializeItem(@Nullable final String itemData) {
        if (itemData == null || itemData.isEmpty()) {
            return null;
        }
        
        // TODO: Implement proper ItemStack deserialization from JSON
        // For now, return a placeholder
        try {
            // Parse material from JSON (simplified)
            final String materialName = itemData.replace("{", "")
                .replace("}", "")
                .replace("\"", "")
                .split(":")[0];
            return new ItemStack(Material.valueOf(materialName));
        } catch (Exception e) {
            return new ItemStack(Material.BARRIER);
        }
    }
}
