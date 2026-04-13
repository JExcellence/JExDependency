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
import com.raindropcentral.rdt.service.ServerBankService;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Interactive shared-storage view for the admin-only server bank.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class ServerBankStorageView extends BaseView {

    private final State<RDT> plugin = initialState("plugin");
    private final MutableState<Boolean> lockHeld = mutableState(false);

    /**
     * Creates the server-bank storage view.
     */
    public ServerBankStorageView() {
        super(ServerBankRootView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "server_bank_storage_ui";
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
    public void onOpen(final @NotNull OpenContext open) {
        super.onOpen(open);
        final ServerBankService bankService = this.plugin.get(open).getServerBankService();
        if (bankService == null || !bankService.acquireBankAccess(open.getPlayer().getUniqueId())) {
            new I18n.Builder("server_bank_shared.messages.in_use", open.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            open.getPlayer().closeInventory();
            return;
        }
        this.lockHeld.set(true, open);
    }

    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final ServerBankService bankService = this.plugin.get(render).getServerBankService();
        if (bankService == null) {
            return;
        }

        final ItemStack[] contents = bankService.expandInventory(bankService.getSharedStorage(), bankService.getSharedStorageSize());
        for (int slot = 0; slot < contents.length; slot++) {
            if (contents[slot] != null && !contents[slot].isEmpty()) {
                render.slot(slot, contents[slot].clone());
            }
        }
    }

    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(false);
    }

    @Override
    public void onClose(final @NotNull CloseContext close) {
        final ServerBankService bankService = this.plugin.get(close).getServerBankService();
        if (bankService != null && Boolean.TRUE.equals(this.lockHeld.get(close))) {
            bankService.saveSharedStorage(close.getParent().getInventory().getContents());
            bankService.releaseBankAccess(close.getPlayer().getUniqueId());
        }
        super.onClose(close);
    }
}
