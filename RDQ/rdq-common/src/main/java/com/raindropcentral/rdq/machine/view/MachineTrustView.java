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
import com.raindropcentral.rdq.database.entity.machine.MachineTrust;
import com.raindropcentral.rdq.machine.IMachineService;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * View for managing machine trust list.
 *
 * <p>This view allows the machine owner to:
 * <ul>
 *   <li>View all trusted players</li>
 *   <li>Add new trusted players</li>
 *   <li>Remove trusted players</li>
 *   <li>View owner information</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class MachineTrustView extends BaseView {

    private final State<IMachineService> machineService = initialState("machineService");
    private final State<Machine> machine = initialState("machine");

    /**
     * Constructs a new {@code MachineTrustView}.
     */
    public MachineTrustView() {
        super(MachineMainView.class);
    }

    @Override
    protected String getKey() {
        return "view.machine.trust";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "XXXXXXXXX",
            "XoTTTTTXX",
            "XTTTTTTXX",
            "XTTTTTTXX",
            "XXXXXXXXX",
            "    a    "
        };
    }

    @Override
    public void onFirstRender(
        @NotNull final RenderContext render,
        @NotNull final Player player
    ) {
        // Render decoration
        render.layoutSlot('X', createFillItem(player));

        // Render owner information
        renderOwnerInfo(render, player);

        // Render trusted players
        renderTrustedPlayers(render, player);

        // Render add button
        renderAddButton(render, player);
    }

    /**
     * Renders the owner information display.
     *
     * @param render the render context
     * @param player the viewing player
     */
    private void renderOwnerInfo(
        @NotNull final RenderContext render,
        @NotNull final Player player
    ) {
        final OfflinePlayer owner = Bukkit.getOfflinePlayer(machine.get(render).getOwnerUuid());

        render.layoutSlot('o')
            .withItem(
                UnifiedBuilderFactory.item(Material.PLAYER_HEAD)
                    .setName(
                        i18n("items.owner.name", player)
                            .withPlaceholder("owner", owner.getName() != null ? owner.getName() : "Unknown")
                            .build()
                            .component()
                    )
                    .setLore(
                        i18n("items.owner.lore", player)
                            .withPlaceholder("owner", owner.getName() != null ? owner.getName() : "Unknown")
                            .build()
                            .children()
                    )
                    .build()
            );
    }

    /**
     * Renders the list of trusted players.
     *
     * @param render the render context
     * @param player the viewing player
     */
    private void renderTrustedPlayers(
        @NotNull final RenderContext render,
        @NotNull final Player player
    ) {
        final Set<MachineTrust> trustedPlayers = machine.get(render).getTrustedPlayers();
        final MachineTrust[] trustArray = trustedPlayers.toArray(new MachineTrust[0]);
        
        render.layoutSlot('T', (index, item) -> {
            if (index < trustArray.length) {
                // Render trusted player
                final MachineTrust trust = trustArray[index];
                final OfflinePlayer trustedPlayer = Bukkit.getOfflinePlayer(trust.getTrustedUuid());
                
                item.withItem(
                    UnifiedBuilderFactory.item(Material.PLAYER_HEAD)
                        .setName(
                            i18n("items.trusted-player.name", player)
                                .withPlaceholder("player", trustedPlayer.getName() != null ? trustedPlayer.getName() : "Unknown")
                                .build()
                                .component()
                        )
                        .setLore(
                            i18n("items.trusted-player.lore", player)
                                .withPlaceholder("player", trustedPlayer.getName() != null ? trustedPlayer.getName() : "Unknown")
                                .build()
                                .children()
                        )
                        .build()
                );
                item.onClick(click -> {
                    // Only owner can remove trusted players
                    if (!player.getUniqueId().equals(machine.get(click).getOwnerUuid())) {
                        return;
                    }

                    // Remove trusted player
                    machineService.get(click).removeTrustedPlayer(machine.get(click).getId(), trust.getTrustedUuid())
                        .thenAccept(success -> {
                            if (success) {
                                machine.get(click).removeTrustedPlayer(trust);
                                click.update();
                            }
                        });
                });
            } else {
                // Fill empty slot
                item.withItem(
                    UnifiedBuilderFactory.item(Material.GRAY_STAINED_GLASS_PANE)
                        .setName(
                            i18n("items.empty-slot.name", player)
                                .build()
                                .component()
                        )
                        .build()
                );
            }
        });
    }

    /**
     * Renders the add trusted player button.
     *
     * @param render the render context
     * @param player the viewing player
     */
    private void renderAddButton(
        @NotNull final RenderContext render,
        @NotNull final Player player
    ) {
        // Only show add button to owner
        if (!player.getUniqueId().equals(machine.get(render).getOwnerUuid())) {
            return;
        }

        render.layoutSlot('a')
            .withItem(
                UnifiedBuilderFactory.item(Material.GREEN_WOOL)
                    .setName(
                        i18n("items.add-player.name", player)
                            .build()
                            .component()
                    )
                    .setLore(
                        i18n("items.add-player.lore", player)
                            .build()
                            .children()
                    )
                    .build()
            )
            .onClick(click -> {
                // TODO: Implement player name input mechanism
                // For now, just send a message
                i18n("messages.add-player-prompt", player)
                    .build()
                            .sendMessage();
                click.closeForPlayer();
            });
    }
}
