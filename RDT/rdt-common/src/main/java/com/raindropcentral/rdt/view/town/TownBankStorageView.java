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

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.service.TownBankService;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Interactive shared bank or cache storage view.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownBankStorageView extends BaseView {

    private final State<RDT> plugin = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");
    private final State<String> storageMode = initialState(TownBankViewSupport.STORAGE_MODE_KEY);
    private final MutableState<Boolean> lockHeld = mutableState(false);
    private final MutableState<Boolean> proxyBacked = mutableState(false);
    private final MutableState<String> hostServerId = mutableState("");

    /**
     * Creates the bank storage view.
     */
    public TownBankStorageView() {
        super(TownBankRootView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "town_bank_storage_ui";
    }

    @Override
    protected int getSize() {
        return 54;
    }

    @Override
    protected boolean shouldAutoFill() {
        return false;
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext open) {
        return Map.of("storage_name", this.resolveStorageName(open));
    }

    @Override
    public void onOpen(final @NotNull OpenContext open) {
        super.onOpen(open);
        open.modifyConfig().size(this.resolveInventorySize(open));

        final RDT plugin = this.plugin.get(open);
        final TownBankService bankService = plugin.getTownBankService();
        final UUID resolvedTownUuid = this.townUuid.get(open);
        if (bankService == null || resolvedTownUuid == null) {
            open.getPlayer().closeInventory();
            return;
        }

        final TownBankStorageMode mode = this.resolveMode(open);
        if (mode == TownBankStorageMode.REMOTE_CACHE_DEPOSIT) {
            final TownBankService.RemoteCacheSessionStartResult result = bankService.openRemoteCacheDepositSession(
                open.getPlayer(),
                resolvedTownUuid
            );
            if (!result.opened()) {
                new I18n.Builder("town_bank_shared.messages.in_use", open.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
                open.getPlayer().closeInventory();
                return;
            }
            this.proxyBacked.set(result.proxyBacked(), open);
            this.hostServerId.set(result.hostServerId(), open);
            this.lockHeld.set(true, open);
            return;
        }

        if (!bankService.acquireBankAccess(resolvedTownUuid, open.getPlayer().getUniqueId())) {
            new I18n.Builder("town_bank_shared.messages.in_use", open.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            open.getPlayer().closeInventory();
            return;
        }
        this.lockHeld.set(true, open);
    }

    @Override
    public void renderNavigationButtons(final @NotNull RenderContext render, final @NotNull Player player) {
        // Storage slots consume the full chest UI.
    }

    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RTown town = this.resolveTown(render);
        final TownBankService bankService = this.plugin.get(render).getTownBankService();
        if (town == null || bankService == null) {
            return;
        }

        final ItemStack[] contents = switch (this.resolveMode(render)) {
            case SHARED_STORAGE -> bankService.expandInventory(town.getSharedBankStorage(), this.resolveInventorySize(render));
            case LOCAL_CACHE -> bankService.expandInventory(town.getBankCacheContents(), this.resolveInventorySize(render));
            case REMOTE_CACHE_DEPOSIT -> new ItemStack[this.resolveInventorySize(render)];
        };
        for (int slot = 0; slot < contents.length; slot++) {
            if (contents[slot] != null && !contents[slot].isEmpty()) {
                render.slot(slot, contents[slot].clone());
            }
        }
    }

    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        if (this.resolveMode(click) != TownBankStorageMode.REMOTE_CACHE_DEPOSIT) {
            click.setCancelled(false);
            return;
        }

        if (click.getClickedContainer().isEntityContainer()) {
            click.setCancelled(false);
            return;
        }

        if (click.isKeyboardClick() || click.isMiddleClick()) {
            click.setCancelled(true);
            return;
        }

        final ItemStack cursorItem = click.getClickOrigin().getCursor();
        final ItemStack currentSlotItem = click.getClickOrigin().getCurrentItem();
        if ((cursorItem == null || cursorItem.isEmpty()) && currentSlotItem != null && !currentSlotItem.isEmpty()) {
            click.setCancelled(true);
            return;
        }

        click.setCancelled(false);
    }

    @Override
    public void onClose(final @NotNull CloseContext close) {
        final RDT plugin = this.plugin.get(close);
        final TownBankService bankService = plugin.getTownBankService();
        final UUID resolvedTownUuid = this.townUuid.get(close);
        if (bankService == null || resolvedTownUuid == null || !Boolean.TRUE.equals(this.lockHeld.get(close))) {
            super.onClose(close);
            return;
        }

        final ItemStack[] contents = close.getParent().getInventory().getContents();
        final TownBankStorageMode mode = this.resolveMode(close);
        switch (mode) {
            case SHARED_STORAGE -> {
                bankService.saveSharedStorage(resolvedTownUuid, contents);
                bankService.releaseBankAccess(resolvedTownUuid, close.getPlayer().getUniqueId());
            }
            case LOCAL_CACHE -> {
                bankService.saveCacheStorage(resolvedTownUuid, contents);
                bankService.releaseBankAccess(resolvedTownUuid, close.getPlayer().getUniqueId());
            }
            case REMOTE_CACHE_DEPOSIT -> {
                final TownBankService.RemoteCacheDepositResult result = bankService.closeRemoteCacheDepositSession(
                    close.getPlayer(),
                    resolvedTownUuid,
                    contents,
                    Boolean.TRUE.equals(this.proxyBacked.get(close)),
                    this.hostServerId.get(close) == null ? "" : this.hostServerId.get(close)
                );
                this.returnLeftovers(close.getPlayer(), result.leftoverContents());
            }
        }
        super.onClose(close);
    }

    private void returnLeftovers(final @NotNull Player player, final ItemStack @NotNull [] leftovers) {
        for (final ItemStack leftover : leftovers) {
            if (leftover == null || leftover.isEmpty()) {
                continue;
            }
            final Map<Integer, ItemStack> remaining = player.getInventory().addItem(leftover);
            for (final ItemStack overflow : remaining.values()) {
                if (overflow != null && !overflow.isEmpty()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), overflow);
                }
            }
        }
    }

    private @Nullable RTown resolveTown(final @NotNull Context context) {
        final UUID resolvedTownUuid = this.townUuid.get(context);
        return resolvedTownUuid == null || this.plugin.get(context).getTownBankService() == null
            ? null
            : this.plugin.get(context).getTownBankService().getTown(resolvedTownUuid);
    }

    private int resolveInventorySize(final @NotNull Context context) {
        final TownBankService bankService = this.plugin.get(context).getTownBankService();
        if (bankService == null) {
            return 27;
        }
        return switch (this.resolveMode(context)) {
            case SHARED_STORAGE -> bankService.getSharedStorageSize();
            case LOCAL_CACHE, REMOTE_CACHE_DEPOSIT -> bankService.getCacheSize();
        };
    }

    private @NotNull String resolveStorageName(final @NotNull Context context) {
        return switch (this.resolveMode(context)) {
            case SHARED_STORAGE -> "Shared Storage";
            case LOCAL_CACHE -> "Town Cache";
            case REMOTE_CACHE_DEPOSIT -> "Remote Cache Deposit";
        };
    }

    private @NotNull TownBankStorageMode resolveMode(final @NotNull Context context) {
        final String rawMode = this.storageMode.get(context);
        if (rawMode == null) {
            return TownBankStorageMode.SHARED_STORAGE;
        }
        try {
            return TownBankStorageMode.valueOf(rawMode.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ignored) {
            return TownBankStorageMode.SHARED_STORAGE;
        }
    }
}
