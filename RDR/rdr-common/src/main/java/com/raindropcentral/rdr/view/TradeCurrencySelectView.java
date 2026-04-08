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
import com.raindropcentral.rdr.database.entity.RTradeSession;
import com.raindropcentral.rdr.service.TradeService;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Currency selector view for trade-session escrow offers.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class TradeCurrencySelectView extends BaseView {

    private static final int[] ENTRY_SLOTS = {
        9, 10, 11, 12, 13, 14, 15, 16, 17,
        18, 19, 20, 21, 22, 23, 24, 25, 26,
        27, 28, 29, 30, 31, 32, 33, 34, 35,
        36, 37, 38, 39, 40, 41, 42, 43, 44
    };

    private final State<RDR> rdr = initialState("plugin");
    private final State<UUID> tradeUuid = initialState("trade_uuid");
    private final State<String> pendingCurrencyId = initialState("currency_id");
    private final State<String> pendingCurrencyDisplay = initialState("currency_display");
    private final State<Boolean> pendingAddMode = initialState("add_mode");
    private final State<Object> pendingCurrencyAmount = initialState("currency_amount");
    private final State<Object> pendingResult = initialState("result");
    private final State<String> pendingInput = initialState("input");

    /**
     * Creates a trade currency selector view.
     */
    public TradeCurrencySelectView() {
        super(TradeSessionView.class);
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return currency selector key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "trade_currency_select_ui";
    }

    /**
     * Returns the layout used for this view.
     *
     * @return six-row currency selector layout
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            "         ",
            "         ",
            "         ",
            "         ",
            "         "
        };
    }

    /**
     * Renders available currencies and applies pending anvil submissions.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    public void onFirstRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        final RDR plugin = this.rdr.get(render);
        final TradeService tradeService = plugin.getTradeService();
        final UUID activeTradeUuid = this.tradeUuid.get(render);
        if (tradeService == null || activeTradeUuid == null) {
            render.slot(22, this.createUnavailableItem(player));
            return;
        }

        final RTradeSession session = tradeService.findSession(activeTradeUuid);
        if (session == null || !session.hasParticipant(player.getUniqueId())) {
            render.slot(22, this.createUnavailableItem(player));
            return;
        }

        final PendingCurrencyMutation pendingMutation = this.extractPendingMutation(render);
        if (pendingMutation != null) {
            render.layoutSlot('s', this.createSummaryItem(player, 0, 0));
            render.slot(22, this.createApplyingItem(player, pendingMutation.currencyDisplay()));
            this.applyPendingMutation(player, plugin, tradeService, activeTradeUuid, pendingMutation);
            return;
        }

        final Map<String, Double> selfOfferCurrencies = session.getOfferCurrencyForParticipant(player.getUniqueId());
        final List<CurrencyEntry> currencyEntries = this.resolveCurrencyEntries(tradeService, selfOfferCurrencies);
        render.layoutSlot('s', this.createSummaryItem(player, currencyEntries.size(), selfOfferCurrencies.size()));

        if (currencyEntries.isEmpty()) {
            render.slot(22, this.createEmptyItem(player));
            return;
        }

        final int slotLimit = Math.min(ENTRY_SLOTS.length, currencyEntries.size());
        for (int index = 0; index < slotLimit; index++) {
            final CurrencyEntry currencyEntry = currencyEntries.get(index);
            final int slot = ENTRY_SLOTS[index];
            final double offeredAmount = Math.max(0.0D, selfOfferCurrencies.getOrDefault(currencyEntry.id(), 0.0D));
            render.slot(slot, this.createCurrencyEntryItem(player, tradeService, currencyEntry, offeredAmount))
                .onClick(clickContext -> {
                    clickContext.setCancelled(true);
                    clickContext.openForPlayer(
                        TradeCurrencyAmountAnvilView.class,
                        Map.of(
                            "plugin", plugin,
                            "trade_uuid", activeTradeUuid,
                            "currency_id", currencyEntry.id(),
                            "currency_display", currencyEntry.displayName(),
                            "add_mode", !clickContext.isRightClick(),
                            "current_offer", offeredAmount
                        )
                    );
                });
        }
    }

    /**
     * Cancels default inventory movement for this view.
     *
     * @param click click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    /**
     * Applies pending anvil mutation payloads when this view is resumed from the amount input view.
     *
     * @param origin previous context
     * @param target resumed context
     */
    @Override
    public void onResume(
        final @NotNull Context origin,
        final @NotNull Context target
    ) {
        final PendingCurrencyMutation targetMutation = this.extractPendingMutation(target);
        final PendingCurrencyMutation pendingMutation = targetMutation != null
            ? targetMutation
            : this.extractPendingMutation(origin);

        final RDR plugin = this.rdr.get(target) != null ? this.rdr.get(target) : this.rdr.get(origin);
        final UUID activeTradeUuid = this.tradeUuid.get(target) != null ? this.tradeUuid.get(target) : this.tradeUuid.get(origin);
        if (plugin == null || activeTradeUuid == null) {
            this.sendSessionResultMessage(target.getPlayer(), TradeService.SessionResult.UNAVAILABLE);
            target.update();
            return;
        }

        if (pendingMutation == null) {
            this.openFreshSelector(target.getPlayer(), plugin, activeTradeUuid);
            return;
        }

        final TradeService tradeService = plugin == null ? null : plugin.getTradeService();
        if (tradeService == null) {
            this.sendSessionResultMessage(target.getPlayer(), TradeService.SessionResult.UNAVAILABLE);
            this.openFreshSelector(target.getPlayer(), plugin, activeTradeUuid);
            return;
        }
        this.applyPendingMutation(target.getPlayer(), plugin, tradeService, activeTradeUuid, pendingMutation);
    }

    private void applyPendingMutation(
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final @NotNull TradeService tradeService,
        final @NotNull UUID tradeUuid,
        final @NotNull PendingCurrencyMutation pendingMutation
    ) {
        tradeService.adjustCurrencyOffer(
                player,
                tradeUuid,
                -1L,
                pendingMutation.currencyId(),
                pendingMutation.amount(),
                pendingMutation.addMode()
            )
            .thenAccept(result -> plugin.getScheduler().runSync(() -> {
                this.sendSessionResultMessage(player, result);
                if (!player.isOnline()) {
                    return;
                }
                this.openFreshSelector(player, plugin, tradeUuid);
            }));
    }

    private @Nullable PendingCurrencyMutation extractPendingMutation(final @NotNull Context context) {
        final String stateCurrencyId = this.readString(this.pendingCurrencyId.get(context));
        final Boolean stateAddMode = this.readBoolean(this.pendingAddMode.get(context));
        final Object stateAmount = this.pendingCurrencyAmount.get(context);
        final Object stateResult = this.pendingResult.get(context);
        final Double stateParsedAmount = this.parseAmount(
            stateAmount == null ? (stateResult == null ? this.pendingInput.get(context) : stateResult) : stateAmount
        );
        if (stateCurrencyId != null && !stateCurrencyId.isBlank() && stateAddMode != null && stateParsedAmount != null) {
            final String stateCurrencyDisplay = this.readString(this.pendingCurrencyDisplay.get(context));
            return new PendingCurrencyMutation(
                stateCurrencyId.trim().toLowerCase(Locale.ROOT),
                stateCurrencyDisplay == null || stateCurrencyDisplay.isBlank() ? stateCurrencyId : stateCurrencyDisplay,
                stateAddMode,
                Math.max(0.0D, stateParsedAmount)
            );
        }

        final Object initialData = context.getInitialData();
        if (!(initialData instanceof Map<?, ?> data)) {
            return null;
        }

        final String currencyId = this.readString(data.get("currency_id"));
        if (currencyId == null || currencyId.isBlank()) {
            return null;
        }

        final String currencyDisplayRaw = this.readString(data.get("currency_display"));
        final String currencyDisplay = currencyDisplayRaw == null || currencyDisplayRaw.isBlank()
            ? currencyId
            : currencyDisplayRaw;
        final Boolean addMode = this.readBoolean(data.get("add_mode"));
        if (addMode == null) {
            return null;
        }

        final Object directAmount = data.containsKey("currency_amount") ? data.get("currency_amount") : data.get("result");
        final Double parsedAmount = this.parseAmount(directAmount == null ? data.get("input") : directAmount);
        if (parsedAmount == null) {
            return null;
        }
        return new PendingCurrencyMutation(
            currencyId.trim().toLowerCase(Locale.ROOT),
            currencyDisplay,
            addMode,
            Math.max(0.0D, parsedAmount)
        );
    }

    private @Nullable Double parseAmount(final @Nullable Object amountObject) {
        if (amountObject instanceof Number numericAmount) {
            return numericAmount.doubleValue();
        }
        if (amountObject instanceof String textAmount) {
            final String normalizedTextAmount = textAmount.trim();
            if (normalizedTextAmount.isEmpty()) {
                return null;
            }
            try {
                return Double.parseDouble(normalizedTextAmount);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private @Nullable String readString(final @Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        return value.toString();
    }

    private @Nullable Boolean readBoolean(final @Nullable Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            if ("true".equalsIgnoreCase(text.trim())) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(text.trim())) {
                return Boolean.FALSE;
            }
        }
        return null;
    }

    private @NotNull Map<String, Object> buildSelectorOpenData(
        final @NotNull RDR plugin,
        final @NotNull UUID tradeUuid
    ) {
        final Map<String, Object> openData = new java.util.HashMap<>();
        openData.put("plugin", plugin);
        openData.put("trade_uuid", tradeUuid);
        openData.put("currency_id", "");
        openData.put("currency_display", "");
        openData.put("add_mode", Boolean.FALSE);
        openData.put("currency_amount", "");
        openData.put("result", "");
        openData.put("input", "");
        return openData;
    }

    private void openFreshSelector(
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final @NotNull UUID tradeUuid
    ) {
        player.closeInventory();
        if (plugin.getScheduler() != null) {
            plugin.getScheduler().runDelayed(() -> {
                if (!player.isOnline()) {
                    return;
                }
                plugin.getViewFrame().open(
                    TradeCurrencySelectView.class,
                    player,
                    this.buildSelectorOpenData(plugin, tradeUuid)
                );
            }, 1L);
            return;
        }
        if (!player.isOnline()) {
            return;
        }
        plugin.getViewFrame().open(
            TradeCurrencySelectView.class,
            player,
            this.buildSelectorOpenData(plugin, tradeUuid)
        );
    }

    private @NotNull List<CurrencyEntry> resolveCurrencyEntries(
        final @NotNull TradeService tradeService,
        final @NotNull Map<String, Double> selfOfferCurrencies
    ) {
        final Map<String, String> availableCurrencies = new java.util.LinkedHashMap<>(tradeService.getAvailableTradeCurrencies());
        for (final String offeredCurrencyId : selfOfferCurrencies.keySet()) {
            if (offeredCurrencyId == null || offeredCurrencyId.isBlank()) {
                continue;
            }
            final String normalizedCurrencyId = offeredCurrencyId.trim().toLowerCase(Locale.ROOT);
            availableCurrencies.putIfAbsent(normalizedCurrencyId, tradeService.getCurrencyDisplayName(normalizedCurrencyId));
        }

        final List<CurrencyEntry> entries = new ArrayList<>();
        for (final Map.Entry<String, String> entry : availableCurrencies.entrySet()) {
            final String currencyId = entry.getKey();
            if (currencyId == null || currencyId.isBlank()) {
                continue;
            }
            entries.add(new CurrencyEntry(
                currencyId,
                entry.getValue() == null || entry.getValue().isBlank() ? currencyId : entry.getValue()
            ));
        }
        entries.sort(Comparator.comparing(CurrencyEntry::displayName, String.CASE_INSENSITIVE_ORDER));
        return entries;
    }

    private void sendSessionResultMessage(
        final @NotNull Player player,
        final @NotNull TradeService.SessionResult result
    ) {
        final String key = switch (result) {
            case SUCCESS -> "trade.message.success";
            case WAITING_FOR_PARTNER -> "trade.message.waiting_for_partner";
            case COMPLETED -> "trade.message.completed";
            case NO_ITEM_IN_HAND -> "trade.message.no_item_in_hand";
            case OFFER_FULL -> "trade.message.offer_full";
            case INSUFFICIENT_FUNDS -> "trade.message.insufficient_funds";
            case MISSING -> "trade.message.missing";
            case FORBIDDEN -> "trade.message.forbidden";
            case INVALID_STATE -> "trade.message.invalid_state";
            case STALE -> "trade.message.stale";
            case EXPIRED -> "trade.message.expired";
            case UNAVAILABLE -> "trade.message.unavailable";
        };
        new I18n.Builder(key, player).build().sendMessage();
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final int availableCount,
        final int offeredCount
    ) {
        return UnifiedBuilderFactory.item(Material.GOLD_INGOT)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "available_count", availableCount,
                    "offered_count", offeredCount
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createCurrencyEntryItem(
        final @NotNull Player player,
        final @NotNull TradeService tradeService,
        final @NotNull CurrencyEntry currencyEntry,
        final double offeredAmount
    ) {
        return UnifiedBuilderFactory.item(this.resolveCurrencyMaterial(currencyEntry.id()))
            .setName(this.i18n("entry.name", player)
                .withPlaceholders(Map.of(
                    "currency", currencyEntry.displayName(),
                    "currency_id", currencyEntry.id()
                ))
                .build()
                .component())
            .setLore(this.i18n("entry.lore", player)
                .withPlaceholders(Map.of(
                    "currency", currencyEntry.displayName(),
                    "currency_id", currencyEntry.id(),
                    "offered_amount", tradeService.formatCurrency(currencyEntry.id(), offeredAmount)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createEmptyItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("empty.name", player).build().component())
            .setLore(this.i18n("empty.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createApplyingItem(
        final @NotNull Player player,
        final @NotNull String currencyDisplay
    ) {
        return UnifiedBuilderFactory.item(Material.CLOCK)
            .setName(this.i18n("applying.name", player).build().component())
            .setLore(this.i18n("applying.lore", player)
                .withPlaceholder("currency", currencyDisplay)
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createUnavailableItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("missing.name", player).build().component())
            .setLore(this.i18n("missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull Material resolveCurrencyMaterial(final @NotNull String currencyId) {
        return "vault".equalsIgnoreCase(currencyId) ? Material.EMERALD : Material.GOLD_NUGGET;
    }

    private record CurrencyEntry(
        String id,
        String displayName
    ) {
    }

    private record PendingCurrencyMutation(
        String currencyId,
        String currencyDisplay,
        boolean addMode,
        double amount
    ) {
    }
}
