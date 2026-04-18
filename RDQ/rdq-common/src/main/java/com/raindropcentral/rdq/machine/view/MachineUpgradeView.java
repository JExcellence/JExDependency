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

import com.raindropcentral.rdq.machine.IMachineService;
import com.raindropcentral.rdq.database.entity.machine.Machine;
import com.raindropcentral.rdq.machine.type.EUpgradeType;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * View for managing machine upgrades.
 *
 * <p>This view displays:
 * <ul>
 *   <li>Available upgrades with current levels</li>
 *   <li>Upgrade requirements</li>
 *   <li>Apply upgrade buttons</li>
 *   <li>Upgrade effects and benefits</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class MachineUpgradeView extends BaseView {

    private final State<IMachineService> machineService = initialState("machineService");
    private final State<Machine> machine = initialState("machine");

    /**
     * Constructs a new {@code MachineUpgradeView}.
     */
    public MachineUpgradeView() {
        super(MachineMainView.class);
    }

    @Override
    protected String getKey() {
        return "view.machine.upgrade";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "XXXXXXXXX",
            "XsefbXXXX",
            "XXXXXXXXX",
            "         ",
            "         ",
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

        // Render upgrade slots
        renderSpeedUpgrade(render, player);
        renderEfficiencyUpgrade(render, player);
        renderFuelReductionUpgrade(render, player);
        renderBonusOutputUpgrade(render, player);
    }

    /**
     * Renders the speed upgrade slot.
     *
     * @param render the render context
     * @param player the viewing player
     */
    private void renderSpeedUpgrade(
        @NotNull final RenderContext render,
        @NotNull final Player player
    ) {
        final int currentLevel = machine.get(render).getUpgradeLevel(EUpgradeType.SPEED);
        final int maxLevel = 5; // From config

        render.layoutSlot('s')
            .withItem(
                UnifiedBuilderFactory.item(Material.SUGAR)
                    .setName(
                        i18n("items.upgrade.speed.name", player)
                            .withPlaceholder("level", currentLevel)
                            .withPlaceholder("max", maxLevel)
                            .build()
                            .component()
                    )
                    .setLore(
                        i18n("items.upgrade.speed.lore", player)
                            .withPlaceholder("level", currentLevel)
                            .withPlaceholder("max", maxLevel)
                            .withPlaceholder("effect", currentLevel * 10)
                            .build()
                            .children()
                    )
                    .build()
            )
            .onClick(click -> {
                if (currentLevel >= maxLevel) {
                    return;
                }
                handleUpgradeApply(click, player, EUpgradeType.SPEED);
            });
    }

    /**
     * Renders the efficiency upgrade slot.
     *
     * @param render the render context
     * @param player the viewing player
     */
    private void renderEfficiencyUpgrade(
        @NotNull final RenderContext render,
        @NotNull final Player player
    ) {
        final int currentLevel = machine.get(render).getUpgradeLevel(EUpgradeType.EFFICIENCY);
        final int maxLevel = 5;

        render.layoutSlot('e')
            .withItem(
                UnifiedBuilderFactory.item(Material.GLOWSTONE_DUST)
                    .setName(
                        i18n("items.upgrade.efficiency.name", player)
                            .withPlaceholder("level", currentLevel)
                            .withPlaceholder("max", maxLevel)
                            .build()
                            .component()
                    )
                    .setLore(
                        i18n("items.upgrade.efficiency.lore", player)
                            .withPlaceholder("level", currentLevel)
                            .withPlaceholder("max", maxLevel)
                            .withPlaceholder("effect", currentLevel * 15)
                            .build()
                            .children()
                    )
                    .build()
            )
            .onClick(click -> {
                if (currentLevel >= maxLevel) {
                    return;
                }
                handleUpgradeApply(click, player, EUpgradeType.EFFICIENCY);
            });
    }

    /**
     * Renders the fuel reduction upgrade slot.
     *
     * @param render the render context
     * @param player the viewing player
     */
    private void renderFuelReductionUpgrade(
        @NotNull final RenderContext render,
        @NotNull final Player player
    ) {
        final int currentLevel = machine.get(render).getUpgradeLevel(EUpgradeType.FUEL_REDUCTION);
        final int maxLevel = 5;

        render.layoutSlot('f')
            .withItem(
                UnifiedBuilderFactory.item(Material.COAL)
                    .setName(
                        i18n("items.upgrade.fuel-reduction.name", player)
                            .withPlaceholder("level", currentLevel)
                            .withPlaceholder("max", maxLevel)
                            .build()
                            .component()
                    )
                    .setLore(
                        i18n("items.upgrade.fuel-reduction.lore", player)
                            .withPlaceholder("level", currentLevel)
                            .withPlaceholder("max", maxLevel)
                            .withPlaceholder("effect", currentLevel * 10)
                            .build()
                            .children()
                    )
                    .build()
            )
            .onClick(click -> {
                if (currentLevel >= maxLevel) {
                    return;
                }
                handleUpgradeApply(click, player, EUpgradeType.FUEL_REDUCTION);
            });
    }

    /**
     * Renders the bonus output upgrade slot.
     *
     * @param render the render context
     * @param player the viewing player
     */
    private void renderBonusOutputUpgrade(
        @NotNull final RenderContext render,
        @NotNull final Player player
    ) {
        final int currentLevel = machine.get(render).getUpgradeLevel(EUpgradeType.BONUS_OUTPUT);
        final int maxLevel = 3;

        render.layoutSlot('b')
            .withItem(
                UnifiedBuilderFactory.item(Material.EMERALD)
                    .setName(
                        i18n("items.upgrade.bonus-output.name", player)
                            .withPlaceholder("level", currentLevel)
                            .withPlaceholder("max", maxLevel)
                            .build()
                            .component()
                    )
                    .setLore(
                        i18n("items.upgrade.bonus-output.lore", player)
                            .withPlaceholder("level", currentLevel)
                            .withPlaceholder("max", maxLevel)
                            .withPlaceholder("effect", currentLevel * 10)
                            .build()
                            .children()
                    )
                    .build()
            )
            .onClick(click -> {
                if (currentLevel >= maxLevel) {
                    return;
                }
                handleUpgradeApply(click, player, EUpgradeType.BONUS_OUTPUT);
            });
    }

    /**
     * Handles applying an upgrade.
     *
     * @param player      the player applying the upgrade
     * @param upgradeType the type of upgrade to apply
     */
    private void handleUpgradeApply(
            @NotNull final SlotClickContext clickContext,
            @NotNull final Player player,
            @NotNull final EUpgradeType upgradeType
    ) {
        machineService.get(clickContext).applyUpgrade(machine.get(clickContext).getId(), upgradeType)
            .thenAccept(success -> {
                if (success) {
                    // Update machine upgrade level
                    machine.get(clickContext).getUpgrades().stream()
                        .filter(u -> u.getUpgradeType() == upgradeType)
                        .findFirst()
                        .ifPresent(upgrade -> upgrade.setLevel(upgrade.getLevel() + 1));

                    // Send success message
                    i18n("messages.upgrade-success", player)
                        .withPlaceholder("upgrade", upgradeType.name())
                        .build()
                        .component();
                } else {
                    // Send failure message
                    i18n("messages.upgrade-failed", player)
                        .withPlaceholder("upgrade", upgradeType.name())
                        .build()
                        .component();
                }
            });
    }
}
