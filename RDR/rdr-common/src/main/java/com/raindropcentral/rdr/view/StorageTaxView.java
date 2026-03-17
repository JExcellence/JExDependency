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
import com.raindropcentral.rdr.database.entity.RStorage;
import com.raindropcentral.rdr.database.repository.RRStorage;
import com.raindropcentral.rdr.service.StorageTownTaxBankService;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tax-management landing view for player storage taxes.
 *
 * <p>This view summarizes outstanding frozen-storage debt, links to frozen storage entries for
 * repayment/unfreezing, and exposes mayor-only controls for transferring collected storage tax
 * balances into a supported protection-plugin town bank.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageTaxView extends BaseView {

    private static final double EPSILON = 1.0E-6D;
    private final State<RDR> rdr = initialState("plugin");

    /**
     * Creates the storage tax landing view.
     */
    public StorageTaxView() {
        super(StorageOverviewView.class);
    }

    /**
     * Returns the translation namespace used by this view.
     *
     * @return storage tax translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "storage_tax_ui";
    }

    /**
     * Returns the menu layout for this view.
     *
     * @return rendered layout with summary and tax actions
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            "   i f m ",
            "         "
        };
    }

    /**
     * Renders the summary and action controls for storage tax management.
     *
     * @param render render context for this menu
     * @param player player viewing the menu
     */
    @Override
    public void onFirstRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        final RDR plugin = this.rdr.get(render);
        final List<RStorage> ownerStorages = this.findOwnerStorages(plugin, player);
        final Map<String, Double> debtByCurrency = summarizeDebtByCurrency(ownerStorages);
        final int frozenStorageCount = countFrozenStorages(ownerStorages);

        render.layoutSlot('s', this.createSummaryItem(player, frozenStorageCount, debtByCurrency.size()));
        render.layoutSlot('i', this.createDebtInfoItem(player, plugin, frozenStorageCount, debtByCurrency));
        render.layoutSlot('f', this.createFrozenStoragesItem(player, frozenStorageCount))
            .onClick(clickContext -> clickContext.openForPlayer(
                StorageFrozenStorageView.class,
                Map.of("plugin", plugin)
            ));

        if (StorageTownTaxBankService.canTransferToTownBank(player)) {
            render.layoutSlot('m', this.createMayorTransferItem(player))
                .onClick(clickContext -> clickContext.openForPlayer(
                    StorageTaxTownBankView.class,
                    Map.of("plugin", plugin)
                ));
        } else {
            render.layoutSlot('m', this.createMayorLockedItem(player));
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

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final int frozenStorageCount,
        final int debtCurrencyCount
    ) {
        return UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "frozen_storage_count", frozenStorageCount,
                    "debt_currency_count", debtCurrencyCount
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createDebtInfoItem(
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final int frozenStorageCount,
        final @NotNull Map<String, Double> debtByCurrency
    ) {
        final List<Component> lore = new ArrayList<>(this.i18n("actions.info.lore", player)
            .withPlaceholders(Map.of("frozen_storage_count", frozenStorageCount))
            .build()
            .children());

        if (debtByCurrency.isEmpty()) {
            lore.add(this.i18n("actions.info.no_debt_line", player).build().component());
        } else {
            lore.add(this.i18n("actions.info.debt_header_line", player).build().component());
            for (final Map.Entry<String, Double> debtEntry : debtByCurrency.entrySet()) {
                final String currencyId = debtEntry.getKey();
                final double amount = debtEntry.getValue() == null ? 0.0D : Math.max(0.0D, debtEntry.getValue());
                if (amount <= EPSILON) {
                    continue;
                }

                lore.add(this.i18n("actions.info.debt_line", player)
                    .withPlaceholders(Map.of(
                        "currency", StorageStorePricingSupport.getCurrencyDisplayName(currencyId),
                        "amount", StorageStorePricingSupport.formatCurrency(plugin, currencyId, amount)
                    ))
                    .build()
                    .component());
            }
        }

        return UnifiedBuilderFactory.item(Material.PAPER)
            .setName(this.i18n("actions.info.name", player).build().component())
            .setLore(lore)
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createFrozenStoragesItem(
        final @NotNull Player player,
        final int frozenStorageCount
    ) {
        return UnifiedBuilderFactory.item(Material.ICE)
            .setName(this.i18n("actions.frozen.name", player).build().component())
            .setLore(this.i18n("actions.frozen.lore", player)
                .withPlaceholder("frozen_storage_count", frozenStorageCount)
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createMayorTransferItem(
        final @NotNull Player player
    ) {
        final StorageTownTaxBankService.TownScope townScope = StorageTownTaxBankService.resolveTownScope(player);
        final String townName = townScope == null ? "-" : townScope.townDisplayName();
        return UnifiedBuilderFactory.item(Material.EMERALD_BLOCK)
            .setName(this.i18n("actions.transfer.name", player).build().component())
            .setLore(this.i18n("actions.transfer.lore", player)
                .withPlaceholder("town_name", townName)
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createMayorLockedItem(
        final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("actions.transfer_locked.name", player).build().component())
            .setLore(this.i18n("actions.transfer_locked.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull List<RStorage> findOwnerStorages(
        final @NotNull RDR plugin,
        final @NotNull Player player
    ) {
        final RRStorage storageRepository = plugin.getStorageRepository();
        if (storageRepository == null) {
            return List.of();
        }
        return storageRepository.findByPlayerUuid(player.getUniqueId());
    }

    private static int countFrozenStorages(final @NotNull List<RStorage> storages) {
        int frozenCount = 0;
        for (final RStorage storage : storages) {
            if (storage != null && storage.hasTaxDebt()) {
                frozenCount++;
            }
        }
        return frozenCount;
    }

    private static @NotNull Map<String, Double> summarizeDebtByCurrency(
        final @NotNull List<RStorage> storages
    ) {
        final Map<String, Double> summary = new LinkedHashMap<>();
        for (final RStorage storage : storages) {
            if (storage == null || !storage.hasTaxDebt()) {
                continue;
            }

            for (final Map.Entry<String, Double> debtEntry : storage.getTaxDebtEntries().entrySet()) {
                if (debtEntry.getKey() == null || debtEntry.getKey().isBlank() || debtEntry.getValue() == null) {
                    continue;
                }
                final String normalizedCurrency = debtEntry.getKey().trim().toLowerCase(Locale.ROOT);
                summary.merge(normalizedCurrency, Math.max(0.0D, debtEntry.getValue()), Double::sum);
            }
        }
        return summary;
    }
}
