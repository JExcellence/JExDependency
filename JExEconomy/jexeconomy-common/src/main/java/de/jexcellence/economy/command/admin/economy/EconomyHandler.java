package de.jexcellence.economy.command.admin.economy;

import com.raindropcentral.commands.v2.CommandContext;
import com.raindropcentral.commands.v2.CommandHandler;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.service.EconomyService;
import de.jexcellence.economy.api.TransactionResult;
import de.jexcellence.jextranslate.R18nManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * JExCommand 2.0 handler collection for {@code /economy}.
 *
 * <p>Supports seven paths (all dual-sender: console + player):
 * <ul>
 *   <li>{@code /economy give &lt;target&gt; &lt;amount&gt; [currency]} — credit an account.</li>
 *   <li>{@code /economy take &lt;target&gt; &lt;amount&gt; [currency]} — debit an account.</li>
 *   <li>{@code /economy set  &lt;target&gt; &lt;amount&gt; [currency]} — set absolute balance.</li>
 *   <li>{@code /economy reset &lt;target&gt; [currency]} — reset balance to zero.</li>
 *   <li>{@code /economy migrate &lt;start|status|supported|info&gt;} — migration tooling.</li>
 *   <li>{@code /economy reload} — reload currencies + translations.</li>
 *   <li>{@code /economy help} — usage printout.</li>
 * </ul>
 *
 * <p>Console always bypasses runtime permission checks; players are gated at
 * tree-load time by the YAML {@code permission:} fields.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public final class EconomyHandler {

    private final EconomyService economyService;

    public EconomyHandler(@NotNull EconomyService economyService) {
        this.economyService = economyService;
    }

    /** Returns the path → handler map for registration. */
    public @NotNull Map<String, CommandHandler> handlerMap() {
        return Map.of(
                "economy",          this::onRoot,
                "economy.give",     ctx -> mutate(ctx, Action.GIVE),
                "economy.take",     ctx -> mutate(ctx, Action.TAKE),
                "economy.set",      ctx -> mutate(ctx, Action.SET),
                "economy.reset",    this::onReset,
                "economy.migrate",  this::onMigrate,
                "economy.reload",   this::onReload,
                "economy.help",     this::onHelp
        );
    }

    // ── Root ─────────────────────────────────────────────────────────────────

    private void onRoot(@NotNull CommandContext ctx) {
        onHelp(ctx);
    }

    // ── Mutations (give / take / set) ────────────────────────────────────────

    private void mutate(@NotNull CommandContext ctx, @NotNull Action action) {
        var sender = ctx.sender();
        var target = ctx.require("target", OfflinePlayer.class);
        var amount = ctx.require("amount", Double.class);

        if (action != Action.SET && amount <= 0) {
            r18n().msg("error.invalid-amount").prefix().send(sender);
            return;
        }

        var targetName = target.getName();
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            r18n().msg("error.player-not-found").prefix()
                    .with("player", targetName != null ? targetName : "?")
                    .send(sender);
            return;
        }

        var currencyOpt = resolveCurrency(ctx);
        if (currencyOpt.isEmpty()) return;
        var currency = currencyOpt.get();

        Player initiator = sender instanceof Player p ? p : null;
        String reason = action.key + " by " + sender.getName();
        String displayName = targetName != null ? targetName : target.getUniqueId().toString();

        CompletableFuture<TransactionResult> future = switch (action) {
            case GIVE -> economyService.deposit(target, currency, amount, initiator, reason);
            case TAKE -> economyService.withdraw(target, currency, amount, initiator, reason);
            case SET  -> economyService.setBalance(target, currency, amount, initiator, reason);
        };

        future.thenAccept(result -> {
            if (result.isSuccess()) {
                r18n().msg("economy." + action.key + "-success").prefix()
                        .with("amount", currency.format(amount))
                        .with("player", displayName)
                        .with("currency", currency.getIdentifier())
                        .with("symbol", currency.getSymbol())
                        .with("balance", currency.format(result.balance()))
                        .send(sender);
            } else {
                r18n().msg("economy." + action.key + "-failed").prefix()
                        .with("player", displayName)
                        .with("currency", currency.getIdentifier())
                        .with("error", result.error() != null ? result.error() : "Unknown")
                        .send(sender);
            }
        }).exceptionally(ex -> {
            r18n().msg("economy." + action.key + "-failed").prefix()
                    .with("player", displayName)
                    .with("currency", currency.getIdentifier())
                    .with("error", ex.getMessage() != null ? ex.getMessage() : "Exception")
                    .send(sender);
            return null;
        });
    }

    // ── Reset ────────────────────────────────────────────────────────────────

    private void onReset(@NotNull CommandContext ctx) {
        var sender = ctx.sender();
        var target = ctx.require("target", OfflinePlayer.class);

        var targetName = target.getName();
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            r18n().msg("error.player-not-found").prefix()
                    .with("player", targetName != null ? targetName : "?")
                    .send(sender);
            return;
        }

        var currencyOpt = resolveCurrency(ctx);
        if (currencyOpt.isEmpty()) return;
        var currency = currencyOpt.get();

        Player initiator = sender instanceof Player p ? p : null;
        String displayName = targetName != null ? targetName : target.getUniqueId().toString();

        economyService.resetBalance(target, currency, initiator).thenAccept(result -> {
            if (result.isSuccess()) {
                r18n().msg("economy.reset-success").prefix()
                        .with("player", displayName)
                        .with("currency", currency.getIdentifier())
                        .with("symbol", currency.getSymbol())
                        .send(sender);
            } else {
                r18n().msg("economy.reset-failed").prefix()
                        .with("player", displayName)
                        .with("currency", currency.getIdentifier())
                        .with("error", result.error() != null ? result.error() : "Unknown")
                        .send(sender);
            }
        }).exceptionally(ex -> {
            r18n().msg("economy.reset-failed").prefix()
                    .with("player", displayName)
                    .with("currency", currency.getIdentifier())
                    .with("error", ex.getMessage() != null ? ex.getMessage() : "Exception")
                    .send(sender);
            return null;
        });
    }

    // ── Migrate ──────────────────────────────────────────────────────────────

    private void onMigrate(@NotNull CommandContext ctx) {
        var sender = ctx.sender();
        var action = ctx.require("action", String.class).toLowerCase(Locale.ROOT);

        switch (action) {
            case "start"     -> r18n().msg("migrate.not-implemented").prefix().send(sender);
            case "status"    -> r18n().msg("migrate.status").prefix().send(sender);
            case "supported" -> sendMigrateSupported(sender);
            case "info"      -> sendMigrateInfo(sender);
            default          -> sendMigrateUsage(sender);
        }
    }

    private void sendMigrateUsage(@NotNull CommandSender sender) {
        r18n().msg("migrate.usage-header").send(sender);
        r18n().msg("migrate.usage-start").send(sender);
        r18n().msg("migrate.usage-status").send(sender);
        r18n().msg("migrate.usage-supported").send(sender);
        r18n().msg("migrate.usage-info").send(sender);
    }

    private void sendMigrateSupported(@NotNull CommandSender sender) {
        r18n().msg("migrate.supported-header").send(sender);
        r18n().msg("migrate.supported-entry").with("provider", "Essentials Economy").send(sender);
        r18n().msg("migrate.supported-entry").with("provider", "iConomy").send(sender);
        r18n().msg("migrate.supported-entry").with("provider", "CMI Economy").send(sender);
        r18n().msg("migrate.supported-entry").with("provider", "TheNewEconomy").send(sender);
        r18n().msg("migrate.supported-vault").send(sender);
    }

    private void sendMigrateInfo(@NotNull CommandSender sender) {
        r18n().msg("migrate.info-header").send(sender);
        r18n().msg("migrate.info-description").send(sender);
        r18n().msg("migrate.info-features-header").send(sender);
        r18n().msg("migrate.info-feature").with("feature", "Automatic provider detection").send(sender);
        r18n().msg("migrate.info-feature").with("feature", "Backup before migration").send(sender);
        r18n().msg("migrate.info-feature").with("feature", "Seamless Vault provider replacement").send(sender);
        r18n().msg("migrate.info-footer").send(sender);
    }

    // ── Reload ───────────────────────────────────────────────────────────────

    private void onReload(@NotNull CommandContext ctx) {
        var sender = ctx.sender();
        r18n().msg("economy.reload-start").prefix().send(sender);

        economyService.reload()
                .thenCompose(v -> r18n().reload())
                .thenAccept(v -> r18n().msg("economy.reload-success").prefix()
                        .with("count", String.valueOf(economyService.getAllCurrencies().size()))
                        .send(sender))
                .exceptionally(ex -> {
                    r18n().msg("economy.reload-failed").prefix()
                            .with("error", ex.getMessage() != null ? ex.getMessage() : "Exception")
                            .send(sender);
                    return null;
                });
    }

    // ── Help ─────────────────────────────────────────────────────────────────

    private void onHelp(@NotNull CommandContext ctx) {
        var sender = ctx.sender();
        var alias = ctx.alias();
        var isConsole = sender instanceof ConsoleCommandSender;

        r18n().msg("economy.help-header").send(sender);
        if (isConsole || hasPerm(sender, "economy.command.give")) {
            r18n().msg("economy.help-give").with("alias", alias).send(sender);
        }
        if (isConsole || hasPerm(sender, "economy.command.take")) {
            r18n().msg("economy.help-take").with("alias", alias).send(sender);
        }
        if (isConsole || hasPerm(sender, "economy.command.set")) {
            r18n().msg("economy.help-set").with("alias", alias).send(sender);
        }
        if (isConsole || hasPerm(sender, "economy.command.reset")) {
            r18n().msg("economy.help-reset").with("alias", alias).send(sender);
        }
        if (isConsole || hasPerm(sender, "economy.command.migrate")) {
            r18n().msg("economy.help-migrate").with("alias", alias).send(sender);
        }
        if (isConsole || hasPerm(sender, "economy.command.reload")) {
            r18n().msg("economy.help-reload").with("alias", alias).send(sender);
        }
        r18n().msg("economy.help-footer").send(sender);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private @NotNull Optional<Currency> resolveCurrency(@NotNull CommandContext ctx) {
        var sender = ctx.sender();
        var explicit = ctx.get("currency", Currency.class);
        if (explicit.isPresent()) return explicit;

        var defaultOpt = economyService.getAllCurrencies().values().stream().findFirst();
        if (defaultOpt.isEmpty()) {
            var available = economyService.getAllCurrencies().keySet().stream()
                    .collect(Collectors.joining(", "));
            r18n().msg("admin.no-default-currency").prefix()
                    .with("available", available.isEmpty() ? "-" : available)
                    .send(sender);
        }
        return defaultOpt;
    }

    private static boolean hasPerm(@NotNull CommandSender sender, @NotNull String node) {
        return sender instanceof ConsoleCommandSender
                || (sender instanceof Player p && (p.isOp() || p.hasPermission(node)));
    }

    private static R18nManager r18n() {
        return R18nManager.getInstance();
    }

    private enum Action {
        GIVE("give"),
        TAKE("take"),
        SET("set");

        final String key;

        Action(String key) {
            this.key = key;
        }
    }
}
