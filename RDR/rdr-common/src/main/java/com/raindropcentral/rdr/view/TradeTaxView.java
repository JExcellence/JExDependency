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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.configs.ConfigSection;
import com.raindropcentral.rdr.service.TradeService;
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

/**
 * Paginated read-only view listing configured per-currency trade-tax rules.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class TradeTaxView extends APaginatedView<TradeTaxView.TradeTaxEntry> {

    private final State<RDR> rdr = initialState("plugin");

    /**
     * Creates the trade-tax overview view.
     */
    public TradeTaxView() {
        super(TradeHubView.class);
    }

    /**
     * Returns the translation namespace used by this view.
     *
     * @return trade-tax translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "trade_tax_ui";
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
     * Resolves trade-tax definitions for pagination.
     *
     * @param context active menu context
     * @return async list of configured trade-tax entries
     */
    @Override
    protected @NotNull CompletableFuture<List<TradeTaxEntry>> getAsyncPaginationSource(
        final @NotNull Context context
    ) {
        final RDR plugin = this.rdr.get(context);
        final List<TradeTaxEntry> entries = new ArrayList<>();
        for (final Map.Entry<String, ConfigSection.TradeTaxCurrencyDefinition> entry
            : plugin.getDefaultConfig().getTradeTaxationCurrencies().entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }

            final String normalizedCurrencyId = entry.getKey().trim().toLowerCase(Locale.ROOT);
            entries.add(new TradeTaxEntry(normalizedCurrencyId, entry.getValue()));
        }
        entries.sort(Comparator.comparing(TradeTaxEntry::currencyId, String.CASE_INSENSITIVE_ORDER));
        return CompletableFuture.completedFuture(entries);
    }

    /**
     * Renders one configured trade-tax entry.
     *
     * @param context menu context
     * @param builder item component builder
     * @param index rendered index
     * @param entry trade-tax entry
     */
    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull TradeTaxEntry entry
    ) {
        final RDR plugin = this.rdr.get(context);
        final TradeService tradeService = plugin.getTradeService();
        builder.withItem(this.createEntryItem(context.getPlayer(), tradeService, entry))
            .onClick(clickContext -> clickContext.setCancelled(true));
    }

    /**
     * Renders static controls for this paginated trade-tax view.
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
        final boolean taxationEnabled = plugin.getDefaultConfig().isTradeTaxationEnabled();
        final int entryCount = this.getPagination(render).source() == null
            ? 0
            : this.getPagination(render).source().size();

        render.layoutSlot('s', this.createSummaryItem(player, taxationEnabled, entryCount));
        if (entryCount < 1) {
            render.slot(22).renderWith(() -> this.createEmptyItem(player));
        }
    }

    /**
     * Cancels vanilla click handling so this menu behaves as a read-only UI.
     *
     * @param click click context for the current inventory interaction
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final boolean taxationEnabled,
        final int currencyCount
    ) {
        final String enabledState = this.i18n(
            taxationEnabled ? "summary.tax_enabled.on" : "summary.tax_enabled.off",
            player
        ).build().getI18nVersionWrapper().asPlaceholder();
        return UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "tax_enabled", enabledState,
                    "currency_count", currencyCount
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createEntryItem(
        final @NotNull Player player,
        final TradeService tradeService,
        final @NotNull TradeTaxEntry entry
    ) {
        final ConfigSection.TradeTaxCurrencyDefinition definition = entry.definition();
        final String mode = this.i18n(
            definition.mode() == ConfigSection.TradeTaxMode.GROWTH ? "entry.mode.growth" : "entry.mode.flat",
            player
        ).build().getI18nVersionWrapper().asPlaceholder();
        return UnifiedBuilderFactory.item(this.resolveCurrencyMaterial(entry.currencyId()))
            .setName(this.i18n("entry.name", player)
                .withPlaceholders(Map.of(
                    "currency", this.resolveCurrencyDisplayName(tradeService, entry.currencyId()),
                    "currency_id", entry.currencyId()
                ))
                .build()
                .component())
            .setLore(this.i18n("entry.lore", player)
                .withPlaceholders(Map.of(
                    "currency", this.resolveCurrencyDisplayName(tradeService, entry.currencyId()),
                    "currency_id", entry.currencyId(),
                    "mode", mode,
                    "flat_amount", this.formatCurrencyAmount(tradeService, entry.currencyId(), definition.flatAmount()),
                    "growth_per_currency", this.formatCurrencyAmount(
                        tradeService,
                        entry.currencyId(),
                        definition.growthPerCurrencyAmount()
                    ),
                    "growth_per_item", String.format(Locale.US, "%.2f", definition.growthPerItem()),
                    "example_tax", this.formatCurrencyAmount(
                        tradeService,
                        entry.currencyId(),
                        definition.calculateTax(100.0D, 5)
                    )
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull String resolveCurrencyDisplayName(
        final TradeService tradeService,
        final @NotNull String currencyId
    ) {
        if (tradeService == null) {
            return currencyId;
        }
        return tradeService.getCurrencyDisplayName(currencyId);
    }

    private @NotNull String formatCurrencyAmount(
        final TradeService tradeService,
        final @NotNull String currencyId,
        final double amount
    ) {
        if (tradeService == null) {
            return String.format(Locale.US, "%.2f %s", amount, currencyId);
        }
        return tradeService.formatCurrency(currencyId, amount);
    }

    private @NotNull Material resolveCurrencyMaterial(final @NotNull String currencyId) {
        if ("vault".equalsIgnoreCase(currencyId)) {
            return Material.EMERALD;
        }
        if ("raindrops".equalsIgnoreCase(currencyId)) {
            return Material.PRISMARINE_CRYSTALS;
        }
        return Material.GOLD_NUGGET;
    }

    private @NotNull ItemStack createEmptyItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("empty.name", player).build().component())
            .setLore(this.i18n("empty.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    /**
     * Immutable trade-tax list entry payload.
     *
     * @param currencyId normalized currency identifier
     * @param definition normalized tax definition
     */
    protected record TradeTaxEntry(
        @NotNull String currencyId,
        @NotNull ConfigSection.TradeTaxCurrencyDefinition definition
    ) {
    }
}
