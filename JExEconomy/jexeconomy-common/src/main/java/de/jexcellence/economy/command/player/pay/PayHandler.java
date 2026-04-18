package de.jexcellence.economy.command.player.pay;

import com.raindropcentral.commands.v2.CommandContext;
import com.raindropcentral.commands.v2.CommandHandler;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.service.EconomyService;
import de.jexcellence.jextranslate.R18nManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

/**
 * JExCommand 2.0 handler for {@code /pay}.
 *
 * <p>Transfers {@code amount} of the specified (or default) currency from the
 * invoking player to the target player, emitting success messages to both
 * participants and insufficient-funds / generic failure branches otherwise.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public final class PayHandler {

    private final EconomyService economyService;

    public PayHandler(@NotNull EconomyService economyService) {
        this.economyService = economyService;
    }

    /** Returns the path → handler map for {@link com.raindropcentral.commands.CommandFactory#registerTree}. */
    public @NotNull Map<String, CommandHandler> handlerMap() {
        return Map.of("pay", this::onPay);
    }

    // ── Handler ──────────────────────────────────────────────────────────────

    private void onPay(@NotNull CommandContext ctx) {
        var player = ctx.asPlayer().orElseThrow();

        var target = ctx.require("target", OfflinePlayer.class);
        var amount = ctx.require("amount", Double.class);
        var currency = ctx.get("currency", Currency.class)
                .orElseGet(() -> resolveDefaultCurrency().orElse(null));

        if (currency == null) {
            r18n().msg("pay.no-default-currency").prefix().send(player);
            return;
        }

        if (amount <= 0) {
            r18n().msg("error.invalid-amount").prefix().send(player);
            return;
        }

        var targetName = target.getName();
        if (targetName != null && targetName.equalsIgnoreCase(player.getName())) {
            r18n().msg("error.self-payment").prefix().send(player);
            return;
        }

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            r18n().msg("error.player-not-found").prefix()
                    .with("player", targetName != null ? targetName : "?")
                    .send(player);
            return;
        }

        economyService.transfer(player, target, currency, amount)
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        r18n().msg("pay.sent").prefix()
                                .with("amount", currency.format(amount))
                                .with("target", targetName != null ? targetName : "?")
                                .with("currency", currency.getIdentifier())
                                .with("balance", currency.format(result.balance()))
                                .send(player);

                        var onlineTarget = Bukkit.getPlayer(target.getUniqueId());
                        if (onlineTarget != null) {
                            r18n().msg("pay.received").prefix()
                                    .with("amount", currency.format(amount))
                                    .with("sender", player.getName())
                                    .with("currency", currency.getIdentifier())
                                    .send(onlineTarget);
                        }
                    } else if (isInsufficientFunds(result.error())) {
                        r18n().msg("error.insufficient-funds").prefix()
                                .with("required", currency.format(amount))
                                .with("balance", currency.format(result.balance()))
                                .with("currency", currency.getIdentifier())
                                .send(player);
                    } else {
                        r18n().msg("pay.failed").prefix()
                                .with("currency", currency.getIdentifier())
                                .with("error", result.error() != null ? result.error() : "Unknown")
                                .send(player);
                    }
                });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private @NotNull Optional<Currency> resolveDefaultCurrency() {
        return economyService.getAllCurrencies().values().stream().findFirst();
    }

    private static boolean isInsufficientFunds(String error) {
        return error != null && error.toLowerCase().contains("insufficient");
    }

    private static R18nManager r18n() {
        return R18nManager.getInstance();
    }

    /** Acts as a {@link CommandHandler} for the root path. */
    public static CommandHandler asHandler(@NotNull PayHandler instance) {
        return instance::onPay;
    }
}
