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
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.database.repository.RRTown;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated town-bank management view with one entry per currency.
 *
 * <p>Left-click deposits the player's full balance of the selected currency into the town bank,
 * while right-click withdraws the town's full balance of the selected currency to the player.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownBankView extends APaginatedView<TownBankView.BankCurrencyEntry> {

    private static final String VAULT_CURRENCY_ID = "vault";

    private final State<RDT> rdt = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");

    /**
     * Creates the town-bank view and enables return navigation to town overview.
     */
    public TownBankView() {
        super(TownOverviewView.class);
    }

    /**
     * Returns the i18n key namespace for this view.
     *
     * @return translation key
     */
    @Override
    protected @NotNull String getKey() {
        return "town_bank_ui";
    }

    /**
     * Resolves placeholders used in the inventory title.
     *
     * @param openContext open context
     * @return title placeholders
     */
    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext openContext) {
        final RTown town = this.resolveTown(openContext, openContext.getPlayer());
        return Map.of(
                "town_name", town == null ? "Unknown" : town.getTownName()
        );
    }

    /**
     * Cancels default inventory movement behavior.
     *
     * @param click click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    /**
     * Verifies access before rendering paginated entries.
     *
     * @param render render context
     * @param player viewer
     */
    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        if (!this.verifyViewerAccess(render, player)) {
            player.closeInventory();
            return;
        }
        super.onFirstRender(render, player);
    }

    /**
     * Loads all available currency entries for this town bank.
     *
     * @param context view context
     * @return future currency entry list
     */
    @Override
    protected @NotNull CompletableFuture<List<BankCurrencyEntry>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        final RDT plugin = this.resolvePlugin(context);
        final RTown town = this.resolveTown(context, context.getPlayer());
        if (plugin == null || town == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        final Map<String, String> availableCurrencies = this.resolveAvailableCurrencies(plugin, town);
        final List<BankCurrencyEntry> entries = new ArrayList<>();
        for (final Map.Entry<String, String> currencyEntry : availableCurrencies.entrySet()) {
            entries.add(new BankCurrencyEntry(currencyEntry.getKey(), currencyEntry.getValue()));
        }

        entries.sort(Comparator.comparing(BankCurrencyEntry::currencyId, String.CASE_INSENSITIVE_ORDER));
        return CompletableFuture.completedFuture(entries);
    }

    /**
     * Renders one currency bank entry.
     *
     * @param context context
     * @param builder item builder
     * @param index rendered index
     * @param entry currency entry
     */
    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull BankCurrencyEntry entry
    ) {
        final Player player = context.getPlayer();
        final RTown town = this.resolveTown(context, player);
        if (town == null) {
            builder.withItem(
                    UnifiedBuilderFactory.item(Material.BARRIER)
                            .setName(this.i18n("currency.unavailable.name", player).build().component())
                            .setLore(this.i18n("currency.unavailable.lore", player).build().children())
                            .build()
            );
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(context, player);
        final boolean canDeposit = town.hasTownPermission(townPlayer, TownPermissions.TOWN_DEPOSIT);
        final boolean canWithdraw = town.hasTownPermission(townPlayer, TownPermissions.TOWN_WITHDRAW);
        final double townBalance = town.getBankAmount(entry.currencyId());
        final double playerBalance = this.resolvePlayerCurrencyBalance(context, entry.currencyId());

        builder.withItem(
                UnifiedBuilderFactory.item(this.resolveCurrencyMaterial(entry.currencyId(), townBalance))
                        .setName(this.i18n("currency.name", player)
                                .withPlaceholders(Map.of(
                                        "currency_id", entry.currencyId(),
                                        "currency_name", entry.currencyDisplayName()
                                ))
                                .build()
                                .component())
                        .setLore(this.i18n("currency.lore", player)
                                .withPlaceholders(Map.of(
                                        "currency_id", entry.currencyId(),
                                        "currency_name", entry.currencyDisplayName(),
                                        "town_balance", this.formatAmount(townBalance),
                                        "player_balance", this.formatAmount(playerBalance),
                                        "deposit_permission", TownPermissions.TOWN_DEPOSIT.getPermissionKey(),
                                        "withdraw_permission", TownPermissions.TOWN_WITHDRAW.getPermissionKey(),
                                        "deposit_state", this.resolvePermissionState(player, canDeposit),
                                        "withdraw_state", this.resolvePermissionState(player, canWithdraw)
                                ))
                                .build()
                                .children())
                        .build()
        ).onClick(click -> this.handleCurrencyClick(click, entry));
    }

    /**
     * Renders static controls for the paginated bank view.
     *
     * @param render render context
     * @param player viewer
     */
    @Override
    protected void onPaginatedRender(final @NotNull RenderContext render, final @NotNull Player player) {
    }

    private void handleCurrencyClick(
            final @NotNull SlotClickContext click,
            final @NotNull BankCurrencyEntry entry
    ) {
        click.setCancelled(true);
        if (click.getClickOrigin().isRightClick()) {
            this.handleWithdrawClick(click, entry.currencyId());
            return;
        }
        this.handleDepositClick(click, entry.currencyId());
    }

    private void handleDepositClick(
            final @NotNull SlotClickContext click,
            final @NotNull String currencyId
    ) {
        final Player player = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null || plugin.getTownRepository() == null) {
            this.i18n("error.system_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final RTown town = this.resolveTown(click, player);
        if (town == null) {
            this.i18n("error.town_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(click, player);
        if (!town.hasTownPermission(townPlayer, TownPermissions.TOWN_DEPOSIT)) {
            this.i18n("error.no_permission_deposit", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.TOWN_DEPOSIT.getPermissionKey())
                    .build()
                    .sendMessage();
            return;
        }

        final double playerBalance = this.resolvePlayerCurrencyBalance(click, currencyId);
        if (playerBalance <= 0.0D) {
            this.i18n("error.player_funds_unavailable", player)
                    .includePrefix()
                    .withPlaceholders(Map.of(
                            "currency_id", currencyId,
                            "currency_name", this.resolveCurrencyDisplayName(currencyId)
                    ))
                    .build()
                    .sendMessage();
            return;
        }

        if (!this.withdrawFromPlayerWallet(plugin, player, currencyId, playerBalance)) {
            this.i18n("error.deposit_failed", player)
                    .includePrefix()
                    .withPlaceholders(Map.of(
                            "currency_id", currencyId,
                            "currency_name", this.resolveCurrencyDisplayName(currencyId),
                            "amount", this.formatAmount(playerBalance)
                    ))
                    .build()
                    .sendMessage();
            return;
        }

        final double updatedTownBalance = town.depositBank(currencyId, playerBalance);
        plugin.getTownRepository().update(town);

        this.i18n("message.deposited", player)
                .includePrefix()
                .withPlaceholders(Map.of(
                        "currency_id", currencyId,
                        "currency_name", this.resolveCurrencyDisplayName(currencyId),
                        "amount", this.formatAmount(playerBalance),
                        "town_balance", this.formatAmount(updatedTownBalance)
                ))
                .build()
                .sendMessage();

        this.reopen(click, plugin, town.getIdentifier());
    }

    private void handleWithdrawClick(
            final @NotNull SlotClickContext click,
            final @NotNull String currencyId
    ) {
        final Player player = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null || plugin.getTownRepository() == null) {
            this.i18n("error.system_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final RTown town = this.resolveTown(click, player);
        if (town == null) {
            this.i18n("error.town_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(click, player);
        if (!town.hasTownPermission(townPlayer, TownPermissions.TOWN_WITHDRAW)) {
            this.i18n("error.no_permission_withdraw", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.TOWN_WITHDRAW.getPermissionKey())
                    .build()
                    .sendMessage();
            return;
        }

        final double townBalance = town.getBankAmount(currencyId);
        if (townBalance <= 0.0D) {
            this.i18n("error.town_funds_unavailable", player)
                    .includePrefix()
                    .withPlaceholders(Map.of(
                            "currency_id", currencyId,
                            "currency_name", this.resolveCurrencyDisplayName(currencyId)
                    ))
                    .build()
                    .sendMessage();
            return;
        }

        if (!this.depositToPlayerWallet(plugin, player, currencyId, townBalance)) {
            this.i18n("error.withdraw_failed", player)
                    .includePrefix()
                    .withPlaceholders(Map.of(
                            "currency_id", currencyId,
                            "currency_name", this.resolveCurrencyDisplayName(currencyId),
                            "amount", this.formatAmount(townBalance)
                    ))
                    .build()
                    .sendMessage();
            return;
        }

        if (!town.withdrawBank(currencyId, townBalance)) {
            this.i18n("error.town_funds_unavailable", player)
                    .includePrefix()
                    .withPlaceholders(Map.of(
                            "currency_id", currencyId,
                            "currency_name", this.resolveCurrencyDisplayName(currencyId)
                    ))
                    .build()
                    .sendMessage();
            return;
        }

        plugin.getTownRepository().update(town);
        this.i18n("message.withdrawn", player)
                .includePrefix()
                .withPlaceholders(Map.of(
                        "currency_id", currencyId,
                        "currency_name", this.resolveCurrencyDisplayName(currencyId),
                        "amount", this.formatAmount(townBalance),
                        "town_balance", this.formatAmount(town.getBankAmount(currencyId))
                ))
                .build()
                .sendMessage();

        this.reopen(click, plugin, town.getIdentifier());
    }

    private void reopen(
            final @NotNull SlotClickContext click,
            final @NotNull RDT plugin,
            final @NotNull UUID townUuid
    ) {
        click.openForPlayer(
                TownBankView.class,
                Map.of(
                        "plugin", plugin,
                        "town_uuid", townUuid
                )
        );
    }

    private boolean verifyViewerAccess(
            final @NotNull Context context,
            final @NotNull Player player
    ) {
        final RTown town = this.resolveTown(context, player);
        if (town == null) {
            this.i18n("error.town_unavailable", player).includePrefix().build().sendMessage();
            return false;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(context, player);
        final boolean canDeposit = town.hasTownPermission(townPlayer, TownPermissions.TOWN_DEPOSIT);
        final boolean canWithdraw = town.hasTownPermission(townPlayer, TownPermissions.TOWN_WITHDRAW);
        if (!canDeposit && !canWithdraw) {
            this.i18n("error.no_permission_view", player)
                    .includePrefix()
                    .withPlaceholders(Map.of(
                            "deposit_permission", TownPermissions.TOWN_DEPOSIT.getPermissionKey(),
                            "withdraw_permission", TownPermissions.TOWN_WITHDRAW.getPermissionKey()
                    ))
                    .build()
                    .sendMessage();
            return false;
        }

        return true;
    }

    private @Nullable RTown resolveTown(
            final @NotNull Context context,
            final @NotNull Player player
    ) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null) {
            return null;
        }

        final RRTown townRepository = plugin.getTownRepository();
        if (townRepository == null) {
            return null;
        }

        final UUID resolvedTownUuid = this.resolveTownUuid(context, player, plugin);
        if (resolvedTownUuid == null) {
            return null;
        }

        return townRepository.findByTownUUID(resolvedTownUuid);
    }

    private @Nullable UUID resolveTownUuid(
            final @NotNull Context context,
            final @NotNull Player player,
            final @NotNull RDT plugin
    ) {
        try {
            final UUID explicitTownUuid = this.townUuid.get(context);
            if (explicitTownUuid != null) {
                return explicitTownUuid;
            }
        } catch (final Exception ignored) {
        }

        final RRDTPlayer playerRepository = plugin.getPlayerRepository();
        if (playerRepository == null) {
            return null;
        }

        final RDTPlayer rdtPlayer = playerRepository.findByPlayer(player.getUniqueId());
        return rdtPlayer == null ? null : rdtPlayer.getTownUUID();
    }

    private @Nullable RDTPlayer resolveTownPlayer(
            final @NotNull Context context,
            final @NotNull Player player
    ) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null || plugin.getPlayerRepository() == null) {
            return null;
        }
        return plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
    }

    private @Nullable RDT resolvePlugin(final @NotNull Context context) {
        try {
            return this.rdt.get(context);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private @NotNull Map<String, String> resolveAvailableCurrencies(
            final @NotNull RDT plugin,
            final @NotNull RTown town
    ) {
        final Map<String, String> currencies = new LinkedHashMap<>();
        if (plugin.getEco() != null) {
            currencies.put(VAULT_CURRENCY_ID, "Vault");
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        if (bridge != null) {
            for (final Map.Entry<String, String> entry : bridge.getAvailableCurrencies().entrySet()) {
                final String currencyId = this.normalizeCurrencyId(entry.getKey());
                if (currencyId.isBlank()) {
                    continue;
                }
                currencies.put(currencyId, entry.getValue());
            }
        }

        for (final RTown.RTownBank bankEntry : town.getBankEntries()) {
            final String currencyId = this.normalizeCurrencyId(bankEntry.getCurrencyId());
            if (currencyId.isBlank()) {
                continue;
            }
            currencies.putIfAbsent(currencyId, bankEntry.getCurrencyId());
        }

        return currencies;
    }

    private double resolvePlayerCurrencyBalance(
            final @NotNull Context context,
            final @NotNull String currencyId
    ) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null) {
            return 0.0D;
        }

        if (this.usesVaultCurrency(currencyId)) {
            if (plugin.getEco() == null) {
                return 0.0D;
            }
            return plugin.getEco().getBalance(context.getPlayer());
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        if (bridge == null || !bridge.hasCurrency(currencyId)) {
            return 0.0D;
        }
        return bridge.getBalance(context.getPlayer(), currencyId);
    }

    private boolean withdrawFromPlayerWallet(
            final @NotNull RDT plugin,
            final @NotNull Player player,
            final @NotNull String currencyId,
            final double amount
    ) {
        if (amount <= 0.0D) {
            return true;
        }

        if (this.usesVaultCurrency(currencyId)) {
            if (plugin.getEco() == null) {
                return false;
            }
            return plugin.getEco().withdrawPlayer(player, amount).transactionSuccess();
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        if (bridge == null || !bridge.hasCurrency(currencyId)) {
            return false;
        }
        return Boolean.TRUE.equals(bridge.withdraw(player, currencyId, amount).join());
    }

    private boolean depositToPlayerWallet(
            final @NotNull RDT plugin,
            final @NotNull Player player,
            final @NotNull String currencyId,
            final double amount
    ) {
        if (amount <= 0.0D) {
            return true;
        }

        if (this.usesVaultCurrency(currencyId)) {
            if (plugin.getEco() == null) {
                return false;
            }
            return plugin.getEco().depositPlayer(player, amount).transactionSuccess();
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        if (bridge == null || !bridge.hasCurrency(currencyId)) {
            return false;
        }
        return Boolean.TRUE.equals(bridge.deposit(player, currencyId, amount).join());
    }

    private @NotNull Material resolveCurrencyMaterial(
            final @NotNull String currencyId,
            final double townBalance
    ) {
        if (this.usesVaultCurrency(currencyId)) {
            return townBalance > 0.0D ? Material.GOLD_BLOCK : Material.GOLD_INGOT;
        }
        return townBalance > 0.0D ? Material.EMERALD_BLOCK : Material.EMERALD;
    }

    private @NotNull String resolveCurrencyDisplayName(final @NotNull String currencyId) {
        if (this.usesVaultCurrency(currencyId)) {
            return "Vault";
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        if (bridge == null || !bridge.hasCurrency(currencyId)) {
            return currencyId;
        }
        return bridge.getCurrencyDisplayName(currencyId);
    }

    private boolean usesVaultCurrency(final @NotNull String currencyId) {
        return VAULT_CURRENCY_ID.equals(this.normalizeCurrencyId(currencyId));
    }

    private @NotNull String resolvePermissionState(
            final @NotNull Player player,
            final boolean hasPermission
    ) {
        return this.toPlain(player, hasPermission ? "state.enabled" : "state.disabled");
    }

    private @NotNull String formatAmount(final double amount) {
        if (Math.abs(amount - Math.rint(amount)) < 0.0001D) {
            return String.format(Locale.ROOT, "%.0f", amount);
        }
        return String.format(Locale.ROOT, "%.2f", amount);
    }

    private @NotNull String toPlain(
            final @NotNull Player player,
            final @NotNull String key
    ) {
        return PlainTextComponentSerializer.plainText().serialize(
                this.i18n(key, player).build().component()
        );
    }

    private @NotNull String normalizeCurrencyId(final @NotNull String currencyId) {
        return currencyId.trim().toLowerCase(Locale.ROOT);
    }

    static record BankCurrencyEntry(
            @NotNull String currencyId,
            @NotNull String currencyDisplayName
    ) {
    }
}
