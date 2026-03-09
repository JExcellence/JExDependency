package com.raindropcentral.rdr.view;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.database.entity.RServerBank;
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
import org.jetbrains.annotations.Nullable;

/**
 * Admin-only paginated view for server trade-tax bank balances and withdrawal actions.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class TradeAdminBankView extends APaginatedView<TradeService.ServerBankCurrencySnapshot> {

    private static final String ADMIN_COMMAND_PERMISSION = "raindroprdr.command.admin";
    private static final double EPSILON = 1.0E-6D;

    private final State<RDR> rdr = initialState("plugin");

    /**
     * Creates the trade admin bank view.
     */
    public TradeAdminBankView() {
        super(StorageAdminView.class);
    }

    /**
     * Returns the translation namespace used by this view.
     *
     * @return trade admin-bank translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "trade_admin_bank_ui";
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
     * Resolves server-bank entries for pagination.
     *
     * @param context active menu context
     * @return async list of server-bank entries
     */
    @Override
    protected @NotNull CompletableFuture<List<TradeService.ServerBankCurrencySnapshot>> getAsyncPaginationSource(
        final @NotNull Context context
    ) {
        final RDR plugin = this.rdr.get(context);
        final TradeService tradeService = plugin.getTradeService();
        if (!this.hasAdminAccess(context.getPlayer()) || tradeService == null) {
            return CompletableFuture.completedFuture(List.of());
        }
        return CompletableFuture.completedFuture(tradeService.findServerBankEntries());
    }

    /**
     * Renders one server-bank currency entry.
     *
     * @param context menu context
     * @param builder item component builder
     * @param index rendered index
     * @param entry server-bank entry
     */
    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull TradeService.ServerBankCurrencySnapshot entry
    ) {
        final RDR plugin = this.rdr.get(context);
        final TradeService tradeService = plugin.getTradeService();
        builder.withItem(this.createEntryItem(context.getPlayer(), tradeService, entry))
            .onClick(clickContext -> this.handleWithdrawClick(clickContext, plugin, entry));
    }

    /**
     * Renders static controls for the admin bank view.
     *
     * @param render render context
     * @param player player viewing the menu
     */
    @Override
    protected void onPaginatedRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        if (!this.hasAdminAccess(player)) {
            render.slot(22).renderWith(() -> this.createLockedItem(player));
            return;
        }

        final int currencyCount = this.getPagination(render).source() == null
            ? 0
            : this.getPagination(render).source().size();
        render.layoutSlot('s', this.createSummaryItem(player, currencyCount));
        if (currencyCount < 1) {
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

    private void handleWithdrawClick(
        final @NotNull SlotClickContext clickContext,
        final @NotNull RDR plugin,
        final @NotNull TradeService.ServerBankCurrencySnapshot entry
    ) {
        clickContext.setCancelled(true);
        if (!this.hasAdminAccess(clickContext.getPlayer())) {
            this.i18n("feedback.locked_message", clickContext.getPlayer()).build().sendMessage();
            return;
        }

        final TradeService tradeService = plugin.getTradeService();
        if (tradeService == null) {
            this.i18n("feedback.unavailable", clickContext.getPlayer()).build().sendMessage();
            return;
        }
        if (entry.amount() <= EPSILON) {
            this.i18n("feedback.empty_balance", clickContext.getPlayer())
                .withPlaceholder("currency", tradeService.getCurrencyDisplayName(entry.currencyId()))
                .build()
                .sendMessage();
            clickContext.openForPlayer(TradeAdminBankView.class, Map.of("plugin", plugin));
            return;
        }

        tradeService.withdrawServerBankToPlayer(clickContext.getPlayer(), entry.currencyId())
            .thenAccept(withdrawnAmount -> this.runOnMainThread(plugin, () -> {
                final String currencyId = entry.currencyId();
                final String currencyDisplay = tradeService.getCurrencyDisplayName(currencyId);
                if (withdrawnAmount <= EPSILON) {
                    this.i18n("feedback.withdraw_failed", clickContext.getPlayer())
                        .withPlaceholder("currency", currencyDisplay)
                        .build()
                        .sendMessage();
                } else {
                    this.i18n("feedback.withdraw_success", clickContext.getPlayer())
                        .withPlaceholders(Map.of(
                            "currency", currencyDisplay,
                            "amount", tradeService.formatCurrency(currencyId, withdrawnAmount)
                        ))
                        .build()
                        .sendMessage();
                }
                if (clickContext.getPlayer().isOnline()) {
                    clickContext.openForPlayer(TradeAdminBankView.class, Map.of("plugin", plugin));
                }
            }));
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final int currencyCount
    ) {
        return UnifiedBuilderFactory.item(Material.GOLD_BLOCK)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholder("currency_count", currencyCount)
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createEntryItem(
        final @NotNull Player player,
        final @Nullable TradeService tradeService,
        final @NotNull TradeService.ServerBankCurrencySnapshot entry
    ) {
        final String currencyId = entry.currencyId();
        final String currencyDisplay = this.resolveCurrencyDisplayName(tradeService, currencyId);
        final RServerBank.LedgerEntry latestEntry = entry.latestEntry();
        final String latestType = this.resolveLatestTypeLabel(player, latestEntry);
        final String latestAmount = latestEntry == null
            ? this.i18n("entry.latest.none", player).build().getI18nVersionWrapper().asPlaceholder()
            : this.formatCurrencyAmount(tradeService, currencyId, latestEntry.amount());
        final String latestNote = latestEntry == null
            ? this.i18n("entry.latest.none", player).build().getI18nVersionWrapper().asPlaceholder()
            : latestEntry.note();

        return UnifiedBuilderFactory.item(this.resolveCurrencyMaterial(currencyId))
            .setName(this.i18n("entry.name", player)
                .withPlaceholders(Map.of(
                    "currency", currencyDisplay,
                    "currency_id", currencyId
                ))
                .build()
                .component())
            .setLore(this.i18n("entry.lore", player)
                .withPlaceholders(Map.of(
                    "currency", currencyDisplay,
                    "currency_id", currencyId,
                    "amount", this.formatCurrencyAmount(tradeService, currencyId, entry.amount()),
                    "ledger_count", entry.ledgerCount(),
                    "latest_type", latestType,
                    "latest_amount", latestAmount,
                    "latest_note", latestNote
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

    private @NotNull String resolveLatestTypeLabel(
        final @NotNull Player player,
        final @Nullable RServerBank.LedgerEntry latestEntry
    ) {
        if (latestEntry == null) {
            return this.i18n("entry.latest.none", player).build().getI18nVersionWrapper().asPlaceholder();
        }
        return this.i18n(
            latestEntry.transactionType() == RServerBank.TransactionType.DEPOSIT
                ? "entry.latest.deposit"
                : "entry.latest.withdraw",
            player
        ).build().getI18nVersionWrapper().asPlaceholder();
    }

    private @NotNull String resolveCurrencyDisplayName(
        final @Nullable TradeService tradeService,
        final @NotNull String currencyId
    ) {
        if (tradeService == null) {
            return currencyId;
        }
        return tradeService.getCurrencyDisplayName(currencyId);
    }

    private @NotNull String formatCurrencyAmount(
        final @Nullable TradeService tradeService,
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
        return Material.GOLD_INGOT;
    }

    private boolean hasAdminAccess(final @NotNull Player player) {
        return player.isOp() || player.hasPermission(ADMIN_COMMAND_PERMISSION);
    }

    private void runOnMainThread(final @NotNull RDR plugin, final @NotNull Runnable runnable) {
        if (plugin.getScheduler() == null) {
            runnable.run();
            return;
        }
        plugin.getScheduler().runSync(runnable);
    }
}
