package de.jexcellence.economy.command.player.currency;

import com.raindropcentral.commands.v2.CommandContext;
import com.raindropcentral.commands.v2.CommandHandler;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.service.EconomyService;
import de.jexcellence.jextranslate.R18nManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * JExCommand 2.0 handler collection for {@code /balance}.
 *
 * <p>Supports five paths:
 * <ul>
 *   <li>{@code /balance} — show own balance for an optional currency (or primary if omitted).</li>
 *   <li>{@code /balance all [player]} — show every balance for self or target.</li>
 *   <li>{@code /balance top [currency] [limit]} — leaderboard for a currency.</li>
 *   <li>{@code /balance help} — permission-gated usage printout.</li>
 * </ul>
 *
 * <p>The root path accepts {@code [currency] [player]} — when an extra token
 * follows the currency the viewer must hold
 * {@code currency.command.other}.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public final class BalanceHandler {

    private static final long DEFAULT_TOP_LIMIT = 10L;
    private static final long MAX_TOP_LIMIT = 50L;
    private static final String PERM_OTHER = "currency.command.other";

    private final EconomyService economyService;

    public BalanceHandler(@NotNull EconomyService economyService) {
        this.economyService = economyService;
    }

    /** Returns the path → handler map for registration. */
    public @NotNull Map<String, CommandHandler> handlerMap() {
        return Map.of(
                "balance",      this::onRoot,
                "balance.all",  this::onAll,
                "balance.top",  this::onTop,
                "balance.help", this::onHelp
        );
    }

    // ── Root: /balance [currency] [player] ───────────────────────────────────

    private void onRoot(@NotNull CommandContext ctx) {
        var player = ctx.asPlayer().orElseThrow();
        var currencyOpt = ctx.get("currency", Currency.class);
        var targetOpt = ctx.get("player", OfflinePlayer.class);

        OfflinePlayer viewerTarget = targetOpt.orElse(player);
        if (targetOpt.isPresent() && !player.hasPermission(PERM_OTHER)) {
            r18n().msg("jexcommand.error.no-permission").prefix().with("permission", PERM_OTHER).send(player);
            return;
        }
        if (targetOpt.isPresent() && !viewerTarget.hasPlayedBefore() && !viewerTarget.isOnline()) {
            r18n().msg("error.player-not-found").prefix()
                    .with("player", viewerTarget.getName() != null ? viewerTarget.getName() : "?")
                    .send(player);
            return;
        }

        if (currencyOpt.isPresent()) {
            displaySingle(player, viewerTarget, currencyOpt.get());
        } else {
            // No currency — fall back to "all".
            displayAll(player, viewerTarget);
        }
    }

    // ── /balance all [player] ────────────────────────────────────────────────

    private void onAll(@NotNull CommandContext ctx) {
        var player = ctx.asPlayer().orElseThrow();
        var targetOpt = ctx.get("player", OfflinePlayer.class);

        OfflinePlayer target = targetOpt.orElse(player);
        if (targetOpt.isPresent() && !player.hasPermission(PERM_OTHER)) {
            r18n().msg("jexcommand.error.no-permission").prefix().with("permission", PERM_OTHER).send(player);
            return;
        }
        if (targetOpt.isPresent() && !target.hasPlayedBefore() && !target.isOnline()) {
            r18n().msg("error.player-not-found").prefix()
                    .with("player", target.getName() != null ? target.getName() : "?")
                    .send(player);
            return;
        }
        displayAll(player, target);
    }

    // ── /balance top [currency] [limit] ──────────────────────────────────────

    private void onTop(@NotNull CommandContext ctx) {
        var player = ctx.asPlayer().orElseThrow();

        if (economyService.getAllCurrencies().isEmpty()) {
            r18n().msg("error.no-currencies").prefix().send(player);
            return;
        }

        var currency = ctx.get("currency", Currency.class)
                .orElse(economyService.getAllCurrencies().values().iterator().next());
        long limit = ctx.get("limit", Long.class).orElse(DEFAULT_TOP_LIMIT);
        limit = Math.max(1L, Math.min(MAX_TOP_LIMIT, limit));
        final int resolvedLimit = (int) limit;
        final Currency resolvedCurrency = currency;

        economyService.getTopAccounts(resolvedCurrency, resolvedLimit).thenAccept(accounts -> {
            if (accounts.isEmpty()) {
                r18n().msg("balance.top.empty").prefix()
                        .with("currency", resolvedCurrency.getIdentifier())
                        .send(player);
                return;
            }

            r18n().msg("balance.top.header").prefix()
                    .with("currency", resolvedCurrency.getIdentifier())
                    .with("limit", String.valueOf(resolvedLimit))
                    .send(player);

            int rank = 1;
            for (var account : accounts) {
                var ownerName = account.getPlayer() != null && account.getPlayer().getPlayerName() != null
                        ? account.getPlayer().getPlayerName()
                        : "Unknown";
                r18n().msg("balance.top.entry").prefix()
                        .with("rank", String.valueOf(rank++))
                        .with("player", ownerName)
                        .with("balance", resolvedCurrency.format(account.getBalance()))
                        .with("symbol", resolvedCurrency.getSymbol())
                        .with("currency", resolvedCurrency.getIdentifier())
                        .send(player);
            }

            r18n().msg("balance.top.footer").prefix()
                    .with("currency", resolvedCurrency.getIdentifier())
                    .send(player);
        });
    }

    // ── /balance help ────────────────────────────────────────────────────────

    private void onHelp(@NotNull CommandContext ctx) {
        var player = ctx.asPlayer().orElseThrow();
        var alias = ctx.alias();

        r18n().msg("balance.help-header").send(player);
        r18n().msg("balance.help-self").with("alias", alias).send(player);
        r18n().msg("balance.help-single").with("alias", alias).send(player);
        r18n().msg("balance.help-all").with("alias", alias).send(player);
        if (player.hasPermission(PERM_OTHER)) {
            r18n().msg("balance.help-other").with("alias", alias).send(player);
        }
        if (player.hasPermission("currency.command.top")) {
            r18n().msg("balance.help-top").with("alias", alias).send(player);
        }
        r18n().msg("balance.help-footer").send(player);
    }

    // ── Display helpers ──────────────────────────────────────────────────────

    private void displaySingle(@NotNull Player viewer,
                                @NotNull OfflinePlayer target,
                                @NotNull Currency currency) {
        economyService.getBalance(target, currency).thenAccept(balance -> {
            var self = viewer.getUniqueId().equals(target.getUniqueId());
            var key = self ? "balance.self" : "balance.other";
            var msg = r18n().msg(key).prefix()
                    .with("currency", currency.getIdentifier())
                    .with("symbol", currency.getSymbol())
                    .with("balance", currency.format(balance));
            if (!self) msg = msg.with("player", target.getName() != null ? target.getName() : "?");
            msg.send(viewer);
        });
    }

    private void displayAll(@NotNull Player viewer, @NotNull OfflinePlayer target) {
        if (economyService.getAllCurrencies().isEmpty()) {
            r18n().msg("error.no-currencies").prefix().send(viewer);
            return;
        }

        economyService.getAccounts(target).thenAccept(accounts -> {
            var self = viewer.getUniqueId().equals(target.getUniqueId());

            if (accounts.isEmpty()) {
                var key = self ? "balance.no-accounts-self" : "balance.no-accounts-other";
                var msg = r18n().msg(key).prefix();
                if (!self) msg = msg.with("player", target.getName() != null ? target.getName() : "?");
                msg.send(viewer);
                return;
            }

            var headerKey = self ? "balance.all-header-self" : "balance.all-header-other";
            var header = r18n().msg(headerKey).prefix();
            if (!self) header = header.with("player", target.getName() != null ? target.getName() : "?");
            header.send(viewer);

            for (var account : accounts) {
                var c = account.getCurrency();
                r18n().msg("balance.entry").prefix()
                        .with("currency", c.getIdentifier())
                        .with("symbol", c.getSymbol())
                        .with("balance", c.format(account.getBalance()))
                        .send(viewer);
            }

            r18n().msg("balance.all-footer").prefix().send(viewer);
        });
    }

    private static R18nManager r18n() {
        return R18nManager.getInstance();
    }
}
