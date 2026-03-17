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

package com.raindropcentral.rdr.view;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.database.entity.RTownStorageBank;
import com.raindropcentral.rdr.database.repository.RRTownStorageBank;
import com.raindropcentral.rdr.service.StorageTownTaxBankService;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Mayor-only paginated view for transferring collected storage taxes into a town bank.
 *
 * <p>Each entry represents one currency currently stored in the plugin-managed town storage-tax
 * ledger. Transfer currently supports {@code vault} currency because supported protection plugins
 * expose a single-value town bank API path.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageTaxTownBankView extends APaginatedView<RTownStorageBank> {

    private static final double EPSILON = 1.0E-6D;
    private final State<RDR> rdr = initialState("plugin");

    /**
     * Creates the town-bank transfer view.
     */
    public StorageTaxTownBankView() {
        super(StorageTaxView.class);
    }

    /**
     * Returns the translation namespace used by this view.
     *
     * @return town-bank transfer translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "storage_tax_town_bank_ui";
    }

    /**
     * Returns the menu layout for this view.
     *
     * @return rendered layout with pagination controls
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            "  < p >  "
        };
    }

    /**
     * Resolves town-bank ledger entries for pagination.
     *
     * @param context active menu context
     * @return async list of transferable town-bank entries
     */
    @Override
    protected @NotNull CompletableFuture<List<RTownStorageBank>> getAsyncPaginationSource(
        final @NotNull Context context
    ) {
        final RDR plugin = this.rdr.get(context);
        final Player player = context.getPlayer();
        return CompletableFuture.completedFuture(this.findTownEntries(plugin, player));
    }

    /**
     * Renders a single town-bank ledger entry.
     *
     * @param context menu context
     * @param builder item component builder
     * @param index rendered index
     * @param entry town-bank ledger entry
     */
    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull RTownStorageBank entry
    ) {
        final RDR plugin = this.rdr.get(context);
        builder.withItem(this.createEntryItem(context.getPlayer(), plugin, entry))
            .onClick(clickContext -> this.handleTransferClick(clickContext, plugin, entry));
    }

    /**
     * Renders static controls for the town-bank transfer view.
     *
     * @param render render context
     * @param player player viewing the menu
     */
    @Override
    protected void onPaginatedRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        final RDR plugin = this.rdr.get(render);
        final StorageTownTaxBankService.TownScope townScope = StorageTownTaxBankService.resolveTownScope(player);
        final boolean canTransfer = townScope != null && StorageTownTaxBankService.canTransferToTownBank(player);

        final int entryCount = canTransfer ? this.findTownEntries(plugin, player).size() : 0;
        final String townName = townScope == null ? "-" : townScope.townDisplayName();
        final String protectionPlugin = townScope == null ? "-" : townScope.protectionPlugin();

        render.layoutSlot('s', this.createSummaryItem(player, townName, protectionPlugin, entryCount));
        if (!canTransfer) {
            render.slot(22).renderWith(() -> this.createLockedItem(player));
            return;
        }

        if (entryCount < 1) {
            render.slot(22).renderWith(() -> this.createEmptyItem(player));
        }
    }

    /**
     * Cancels vanilla click handling so the menu behaves as an action UI.
     *
     * @param click click context for the current inventory interaction
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private @NotNull List<RTownStorageBank> findTownEntries(
        final @NotNull RDR plugin,
        final @NotNull Player player
    ) {
        final StorageTownTaxBankService.TownScope townScope = StorageTownTaxBankService.resolveTownScope(player);
        final RRTownStorageBank repository = plugin.getTownStorageBankRepository();
        if (townScope == null || repository == null || !StorageTownTaxBankService.canTransferToTownBank(player)) {
            return List.of();
        }

        return repository.findByTown(townScope.protectionPlugin(), townScope.townIdentifier()).stream()
            .filter(entry -> entry.getAmount() > EPSILON)
            .toList();
    }

    private void handleTransferClick(
        final @NotNull SlotClickContext clickContext,
        final @NotNull RDR plugin,
        final @NotNull RTownStorageBank entry
    ) {
        clickContext.setCancelled(true);

        final String currencyType = entry.getCurrencyType().trim().toLowerCase(Locale.ROOT);
        if (!"vault".equals(currencyType)) {
            this.i18n("feedback.currency_unsupported", clickContext.getPlayer())
                .withPlaceholder("currency", StorageStorePricingSupport.getCurrencyDisplayName(currencyType))
                .build()
                .sendMessage();
            return;
        }

        final double transferredAmount = StorageTownTaxBankService.transferToTownBank(
            plugin,
            clickContext.getPlayer(),
            currencyType
        );
        if (transferredAmount <= EPSILON) {
            this.i18n("feedback.transfer_failed", clickContext.getPlayer())
                .withPlaceholder("currency", StorageStorePricingSupport.getCurrencyDisplayName(currencyType))
                .build()
                .sendMessage();
            return;
        }

        this.i18n("feedback.transfer_success", clickContext.getPlayer())
            .withPlaceholders(Map.of(
                "currency", StorageStorePricingSupport.getCurrencyDisplayName(currencyType),
                "amount", plugin.formatVaultCurrency(transferredAmount)
            ))
            .build()
            .sendMessage();

        clickContext.openForPlayer(
            StorageTaxTownBankView.class,
            Map.of("plugin", plugin)
        );
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final @NotNull String townName,
        final @NotNull String protectionPlugin,
        final int entryCount
    ) {
        return UnifiedBuilderFactory.item(Material.EMERALD_BLOCK)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "town_name", townName,
                    "protection_plugin", protectionPlugin,
                    "entry_count", entryCount
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createEntryItem(
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final @NotNull RTownStorageBank entry
    ) {
        final String currencyType = entry.getCurrencyType();
        final boolean transferable = "vault".equalsIgnoreCase(currencyType);
        return UnifiedBuilderFactory.item(transferable ? Material.GOLD_BLOCK : Material.COPPER_BLOCK)
            .setName(this.i18n("entry.name", player)
                .withPlaceholder("currency", StorageStorePricingSupport.getCurrencyDisplayName(currencyType))
                .build()
                .component())
            .setLore(this.i18n("entry.lore", player)
                .withPlaceholders(Map.of(
                    "amount", StorageStorePricingSupport.formatCurrency(plugin, currencyType, entry.getAmount()),
                    "transfer_status", transferable
                        ? this.i18n("entry.status.transferable", player).build().getI18nVersionWrapper().asPlaceholder()
                        : this.i18n("entry.status.unavailable", player).build().getI18nVersionWrapper().asPlaceholder()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createLockedItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("locked.name", player).build().component())
            .setLore(this.i18n("locked.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createEmptyItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
            .setName(this.i18n("empty.name", player).build().component())
            .setLore(this.i18n("empty.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
}
