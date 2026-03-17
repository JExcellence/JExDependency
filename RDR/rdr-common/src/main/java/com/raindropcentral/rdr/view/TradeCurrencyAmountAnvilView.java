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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.AbstractAnvilView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Anvil input view used to capture an explicit trade-currency add/remove amount.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class TradeCurrencyAmountAnvilView extends AbstractAnvilView {

    private final State<RDR> rdr = initialState("plugin");
    private final State<UUID> tradeUuid = initialState("trade_uuid");
    private final State<String> currencyId = initialState("currency_id");
    private final State<String> currencyDisplay = initialState("currency_display");
    private final State<Boolean> addMode = initialState("add_mode");
    private final State<Double> currentOffer = initialState("current_offer");

    /**
     * Creates the currency amount input anvil view.
     */
    public TradeCurrencyAmountAnvilView() {
        super(TradeCurrencySelectView.class);
    }

    /**
     * Returns the i18n key namespace for this view.
     *
     * @return trade currency amount anvil translation key
     */
    @Override
    protected @NotNull String getKey() {
        return "trade_currency_amount_anvil_ui";
    }

    /**
     * Cancels default inventory movement to prevent extracting display items from this anvil UI.
     *
     * @param click click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    /**
     * Parses the submitted amount as a non-negative double.
     *
     * @param input user-entered amount
     * @param context active anvil context
     * @return parsed non-negative amount
     */
    @Override
    protected @NotNull Object processInput(
        final @NotNull String input,
        final @NotNull Context context
    ) {
        final double parsedAmount = this.parseAmount(input);
        if (parsedAmount < 0.0D) {
            throw new IllegalArgumentException("Trade currency amount cannot be negative.");
        }
        return parsedAmount;
    }

    /**
     * Provides title placeholders for the selected currency and operation mode.
     *
     * @param context open context
     * @return title placeholder map
     */
    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(
        final @NotNull OpenContext context
    ) {
        return Map.of(
            "currency", this.resolveCurrencyDisplay(context),
            "mode", this.resolveModeLabel(context)
        );
    }

    /**
     * Returns the current offer amount as the initial input text.
     *
     * @param context open context
     * @return initial input value
     */
    @Override
    protected @NotNull String getInitialInputText(
        final @NotNull OpenContext context
    ) {
        final double amount = this.resolveDefaultInputAmount(context);
        return String.format(Locale.US, "%.2f", amount);
    }

    /**
     * Validates that the submitted input is a finite non-negative double value.
     *
     * @param input submitted input text
     * @param context active context
     * @return {@code true} when input is valid
     */
    @Override
    protected boolean isValidInput(
        final @NotNull String input,
        final @NotNull Context context
    ) {
        try {
            final double parsed = this.parseAmount(input);
            return parsed >= 0.0D && Double.isFinite(parsed);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    /**
     * Renders the first slot with currency-specific prompts.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    protected void setupFirstSlot(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        final ItemStack inputSlotItem = UnifiedBuilderFactory.item(Material.GOLD_INGOT)
            .setName(Component.text(String.format(Locale.US, "%.2f", this.resolveDefaultInputAmount(render))))
            .setLore(this.i18n("input.lore", player)
                .withPlaceholders(Map.of(
                    "currency", this.resolveCurrencyDisplay(render),
                    "mode", this.resolveModeLabel(render),
                    "current_offer", String.format(Locale.US, "%.2f", this.resolveCurrentOffer(render))
                ))
                .build()
                .children())
            .build();
        render.firstSlot(inputSlotItem);
    }

    /**
     * Sends a validation message for invalid amount input.
     *
     * @param input invalid user input
     * @param context active context
     */
    @Override
    protected void onValidationFailed(
        final @Nullable String input,
        final @NotNull Context context
    ) {
        this.i18n("error.invalid_amount", context.getPlayer())
            .includePrefix()
            .withPlaceholders(Map.of(
                "input", input == null ? "" : input,
                "currency", this.resolveCurrencyDisplay(context)
            ))
            .build()
            .sendMessage();
    }

    /**
     * Prepares result data consumed by {@link TradeCurrencySelectView}.
     *
     * @param processingResult parsed amount
     * @param input submitted input text
     * @param context active context
     * @return result data map for the parent view
     */
    @Override
    protected @NotNull Map<String, Object> prepareResultData(
        final @Nullable Object processingResult,
        final @NotNull String input,
        final @NotNull Context context
    ) {
        final Map<String, Object> resultData = new HashMap<>(super.prepareResultData(processingResult, input, context));
        resultData.put("plugin", this.rdr.get(context));
        resultData.put("trade_uuid", this.tradeUuid.get(context));
        resultData.put("currency_id", this.resolveCurrencyId(context));
        resultData.put("currency_display", this.resolveCurrencyDisplay(context));
        resultData.put("add_mode", this.isAddMode(context));
        resultData.put("currency_amount", processingResult instanceof Number amount ? amount.doubleValue() : 0.0D);
        return resultData;
    }

    private double parseAmount(final @NotNull String input) {
        final String normalizedInput = input.trim().replace(",", "");
        if (normalizedInput.isEmpty()) {
            throw new IllegalArgumentException("Trade currency amount cannot be empty.");
        }
        final double parsedAmount = Double.parseDouble(normalizedInput);
        if (!Double.isFinite(parsedAmount) || parsedAmount < 0.0D) {
            throw new IllegalArgumentException("Trade currency amount must be a non-negative number.");
        }
        return parsedAmount;
    }

    private @NotNull String resolveCurrencyId(final @NotNull Context context) {
        final String selectedCurrencyId = this.currencyId.get(context);
        return selectedCurrencyId == null ? "vault" : selectedCurrencyId;
    }

    private @NotNull String resolveCurrencyDisplay(final @NotNull Context context) {
        final String selectedCurrencyDisplay = this.currencyDisplay.get(context);
        if (selectedCurrencyDisplay == null || selectedCurrencyDisplay.isBlank()) {
            return this.resolveCurrencyId(context);
        }
        return selectedCurrencyDisplay;
    }

    private boolean isAddMode(final @NotNull Context context) {
        return Boolean.TRUE.equals(this.addMode.get(context));
    }

    private @NotNull String resolveModeLabel(final @NotNull Context context) {
        return this.isAddMode(context) ? "add" : "remove";
    }

    private double resolveCurrentOffer(final @NotNull Context context) {
        final Double selectedCurrentOffer = this.currentOffer.get(context);
        if (selectedCurrentOffer == null || !Double.isFinite(selectedCurrentOffer)) {
            return 0.0D;
        }
        return Math.max(0.0D, selectedCurrentOffer);
    }

    private double resolveDefaultInputAmount(final @NotNull Context context) {
        if (this.isAddMode(context)) {
            return 0.0D;
        }
        return this.resolveCurrentOffer(context);
    }
}
