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
import com.raindropcentral.rdt.database.entity.RNation;
import com.raindropcentral.rdt.service.NationBankService;
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

import java.util.UUID;

/**
 * Interactive shared-storage view for the nation bank.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class NationBankStorageView extends BaseView {

    private final State<RDT> plugin = initialState("plugin");
    private final State<UUID> nationUuid = initialState("nation_uuid");
    private final MutableState<Boolean> lockHeld = mutableState(false);

    /**
     * Creates the nation-bank storage view.
     */
    public NationBankStorageView() {
        super(NationBankRootView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "nation_bank_storage_ui";
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
        final RDT plugin = this.plugin.get(open);
        final NationBankService bankService = plugin.getNationBankService();
        final UUID resolvedNationUuid = this.nationUuid.get(open);
        final RNation nation = resolvedNationUuid == null || bankService == null ? null : bankService.getNation(resolvedNationUuid);
        if (bankService == null || nation == null || !bankService.canViewBank(open.getPlayer(), nation)) {
            new I18n.Builder("nation_bank_shared.messages.no_permission", open.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            open.getPlayer().closeInventory();
            return;
        }
        if (!bankService.acquireBankAccess(resolvedNationUuid, open.getPlayer().getUniqueId())) {
            new I18n.Builder("nation_bank_shared.messages.in_use", open.getPlayer())
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
        final RNation nation = this.resolveNation(render);
        final NationBankService bankService = this.plugin.get(render).getNationBankService();
        if (nation == null || bankService == null) {
            return;
        }

        final ItemStack[] contents = bankService.expandInventory(nation.getSharedBankStorage(), bankService.getSharedStorageSize());
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
        final RDT plugin = this.plugin.get(close);
        final NationBankService bankService = plugin.getNationBankService();
        final UUID resolvedNationUuid = this.nationUuid.get(close);
        if (bankService != null && resolvedNationUuid != null && Boolean.TRUE.equals(this.lockHeld.get(close))) {
            bankService.saveSharedStorage(resolvedNationUuid, close.getParent().getInventory().getContents());
            bankService.releaseBankAccess(resolvedNationUuid, close.getPlayer().getUniqueId());
        }
        super.onClose(close);
    }

    private @Nullable RNation resolveNation(final @NotNull Context context) {
        final UUID resolvedNationUuid = this.nationUuid.get(context);
        return resolvedNationUuid == null || this.plugin.get(context).getNationBankService() == null
            ? null
            : this.plugin.get(context).getNationBankService().getNation(resolvedNationUuid);
    }
}
