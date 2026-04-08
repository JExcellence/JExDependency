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

package com.raindropcentral.rdt.view.town;
import com.raindropcentral.rplatform.view.AbstractAnvilView;
import me.devnatan.inventoryframework.BukkitViewContainer;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.IFRenderContext;
import me.devnatan.inventoryframework.context.RenderContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Town-creation specific anvil base that keeps the result slot visible without modifying the shared
 * platform anvil behavior.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
abstract class AbstractTownCreationAnvilView extends AbstractAnvilView {

    /**
     * Creates the town-creation anvil with town-creation hub back navigation.
     */
    protected AbstractTownCreationAnvilView() {
        super(TownCreationProgressView.class);
    }

    /**
     * Schedules regular updates so the physical anvil result mirrors the live rename text.
     *
     * @param config view configuration builder
     */
    @Override
    public void onInit(final @NotNull ViewConfigBuilder config) {
        super.onInit(config);
        config.scheduleUpdate(2);
    }

    /**
     * Renders the standard name slot, a return button, and a custom clickable result slot.
     *
     * @param render render context
     */
    @Override
    public void onFirstRender(final @NotNull RenderContext render) {
        final Player player = render.getPlayer();
        this.setupFirstSlot(render, player);
        this.setupMiddleSlot(render, player);
        this.setupTownResultSlot(render);
        this.syncTownResultSlot(render);
    }

    /**
     * Keeps the anvil result slot updated while the player types.
     *
     * @param context active view context
     */
    @Override
    public void onUpdate(final @NotNull Context context) {
        this.syncTownResultSlot(context);
    }

    @Override
    protected void setupMiddleSlot(final @NotNull RenderContext render, final @NotNull Player player) {
        // Leave the second ingredient slot empty so the client still renders the anvil output.
        // Modern 1.21.x clients suppress the result slot when this slot holds an incompatible item.
    }

    private void setupTownResultSlot(final @NotNull RenderContext render) {
        render.resultSlot().onClick(clickContext -> {
            clickContext.setCancelled(true);

            final String input = this.resolveCurrentInput(clickContext);
            if (!this.isValidInput(input, clickContext)) {
                this.onValidationFailed(input, clickContext);
                return;
            }

            try {
                final Object result = this.processInput(input, clickContext);
                final Map<String, Object> resultData = this.prepareResultData(result, input, clickContext);
                clickContext.back(resultData);
            } catch (final Exception exception) {
                this.onProcessingFailed(input, clickContext, exception);
            }
        });
    }

    private void syncTownResultSlot(final @NotNull Context context) {
        if (!(context instanceof final IFRenderContext renderContext)) {
            return;
        }
        if (!(renderContext.getContainer() instanceof final BukkitViewContainer container)) {
            return;
        }
        if (!(container.getInventory() instanceof final AnvilInventory anvilInventory)) {
            return;
        }

        anvilInventory.setResult(this.createResultItem(context, anvilInventory));
    }

    private @NotNull ItemStack createResultItem(
        final @NotNull Context context,
        final @NotNull AnvilInventory anvilInventory
    ) {
        final String input = this.resolveCurrentInput(context, anvilInventory);
        final boolean valid = this.isValidInput(input, context);

        return com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory.item(
                valid ? Material.LIME_DYE : Material.BARRIER)
            .setName(Component.text(input.isEmpty() ? " " : input))
            .build();
    }

    private @NotNull String resolveCurrentInput(final @NotNull Context context) {
        return this.resolveCurrentInput(context, null);
    }

    private @NotNull String resolveCurrentInput(
        final @NotNull Context context,
        final @Nullable AnvilInventory anvilInventory
    ) {
        if (context.getPlayer().getOpenInventory() instanceof final AnvilView anvilView) {
            final String renameText = anvilView.getRenameText();
            if (renameText != null) {
                return renameText;
            }
        }

        if (anvilInventory != null) {
            final ItemStack firstItem = anvilInventory.getFirstItem();
            if (firstItem != null) {
                final ItemMeta itemMeta = firstItem.getItemMeta();
                if (itemMeta != null && itemMeta.displayName() != null) {
                    return PlainTextComponentSerializer.plainText().serialize(itemMeta.displayName());
                }
            }
        }

        return "";
    }
}
