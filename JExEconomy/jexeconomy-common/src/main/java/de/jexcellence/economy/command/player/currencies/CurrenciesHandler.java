package de.jexcellence.economy.command.player.currencies;

import com.raindropcentral.commands.v2.CommandContext;
import com.raindropcentral.commands.v2.CommandHandler;
import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.service.EconomyService;
import de.jexcellence.economy.view.CurrencyCreateView;
import de.jexcellence.economy.view.CurrencyFieldInputView;
import de.jexcellence.economy.view.CurrencyOverviewView;
import de.jexcellence.jextranslate.R18nManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * JExCommand 2.0 handler collection for {@code /currencies}.
 *
 * <p>Covers seven paths:
 * <ul>
 *   <li>{@code /currencies} root — opens the GUI overview (requires {@code currencies.command.overview}).</li>
 *   <li>{@code /currencies overview} — same GUI, explicit.</li>
 *   <li>{@code /currencies list} — compact chat listing.</li>
 *   <li>{@code /currencies info &lt;currency&gt;} — inspect one currency.</li>
 *   <li>{@code /currencies create [id symbol prefix suffix]} — inline or GUI create.</li>
 *   <li>{@code /currencies edit &lt;currency&gt; &lt;field&gt; &lt;value&gt;} — update one field.</li>
 *   <li>{@code /currencies delete &lt;currency&gt;} — delete.</li>
 *   <li>{@code /currencies help} — permission-gated usage list.</li>
 * </ul>
 *
 * @author JExcellence
 * @since 3.0.0
 */
public final class CurrenciesHandler {

    private static final List<String> ALLOWED_FIELDS =
            List.of("identifier", "symbol", "prefix", "suffix", "icon");
    private static final String PERM_OVERVIEW = "currencies.command.overview";

    private final JExEconomy economy;
    private final EconomyService economyService;

    public CurrenciesHandler(@NotNull JExEconomy economy) {
        this.economy = economy;
        this.economyService = economy.economyService();
    }

    /** Returns the path → handler map for registration. */
    public @NotNull Map<String, CommandHandler> handlerMap() {
        return Map.ofEntries(
                Map.entry("currencies",          this::onRoot),
                Map.entry("currencies.overview", this::onOverview),
                Map.entry("currencies.list",     this::onList),
                Map.entry("currencies.info",     this::onInfo),
                Map.entry("currencies.create",   this::onCreate),
                Map.entry("currencies.edit",     this::onEdit),
                Map.entry("currencies.delete",   this::onDelete),
                Map.entry("currencies.help",     this::onHelp)
        );
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    private void onRoot(@NotNull CommandContext ctx) {
        var player = ctx.asPlayer().orElseThrow();
        if (!player.hasPermission(PERM_OVERVIEW)) {
            r18n().msg("jexcommand.error.no-permission").prefix().with("permission", PERM_OVERVIEW).send(player);
            return;
        }
        openOverview(player);
    }

    private void onOverview(@NotNull CommandContext ctx) {
        openOverview(ctx.asPlayer().orElseThrow());
    }

    private void onList(@NotNull CommandContext ctx) {
        var player = ctx.asPlayer().orElseThrow();
        var all = economyService.getAllCurrencies();
        if (all.isEmpty()) {
            r18n().msg("error.no-currencies").prefix().send(player);
            return;
        }

        r18n().msg("currencies.list.header").prefix()
                .with("count", String.valueOf(all.size())).send(player);
        for (var c : all.values()) {
            r18n().msg("currencies.list.entry").prefix()
                    .with("identifier", c.getIdentifier())
                    .with("symbol", c.getSymbol())
                    .with("prefix", c.getPrefix() == null ? "" : c.getPrefix())
                    .with("suffix", c.getSuffix() == null ? "" : c.getSuffix())
                    .send(player);
        }
        r18n().msg("currencies.list.footer").prefix().send(player);
    }

    private void onInfo(@NotNull CommandContext ctx) {
        var player = ctx.asPlayer().orElseThrow();
        var c = ctx.require("currency", Currency.class);
        r18n().msg("currencies.info.details").prefix()
                .with("identifier", c.getIdentifier())
                .with("symbol", c.getSymbol())
                .with("prefix", c.getPrefix() == null ? "" : c.getPrefix())
                .with("suffix", c.getSuffix() == null ? "" : c.getSuffix())
                .send(player);
    }

    private void onCreate(@NotNull CommandContext ctx) {
        var player = ctx.asPlayer().orElseThrow();
        var id = ctx.get("identifier", String.class).orElse(null);

        // No id → open GUI with blank draft.
        if (id == null || id.isBlank()) {
            economy.viewFrame().open(CurrencyCreateView.class, player, Map.of(
                    "plugin",         economy,
                    "draft_currency", new Currency("", "", "", "", "GOLD_INGOT")
            ));
            return;
        }

        var symbol = ctx.get("symbol", String.class).orElse(null);
        if (symbol == null || symbol.isBlank()) {
            r18n().msg("currencies.create.usage").prefix().send(player);
            return;
        }
        var prefix = ctx.get("prefix", String.class).orElse("");
        var suffix = ctx.get("suffix", String.class).orElse("");

        if (economyService.findCurrency(id).isPresent()) {
            r18n().msg("currencies.create.already-exists").prefix().with("currency", id).send(player);
            return;
        }

        var currency = new Currency(id, symbol, prefix, suffix, null);
        economyService.createCurrency(currency, player).thenAccept(success -> {
            if (success) {
                r18n().msg("currencies.create.success").prefix().with("currency", id).send(player);
            } else {
                r18n().msg("currencies.create.failed").prefix().send(player);
            }
        });
    }

    private void onEdit(@NotNull CommandContext ctx) {
        var player = ctx.asPlayer().orElseThrow();
        var cur = ctx.require("currency", Currency.class);
        var field = ctx.require("field", String.class).toLowerCase(Locale.ROOT);
        var value = ctx.require("value", String.class);

        if (!ALLOWED_FIELDS.contains(field)) {
            r18n().msg("currencies.edit.usage").prefix().send(player);
            return;
        }

        var updated = CurrencyFieldInputView.applyField(cur, field, value);
        economyService.updateCurrency(cur.getIdentifier(), updated, player).thenAccept(success -> {
            if (success) {
                r18n().msg("currencies.edit.success").prefix()
                        .with("currency", updated.getIdentifier())
                        .with("field", field)
                        .with("value", value)
                        .send(player);
            } else {
                r18n().msg("currencies.edit.failed").prefix()
                        .with("currency", cur.getIdentifier())
                        .send(player);
            }
        });
    }

    private void onDelete(@NotNull CommandContext ctx) {
        var player = ctx.asPlayer().orElseThrow();
        var cur = ctx.require("currency", Currency.class);
        var id = cur.getIdentifier();
        economyService.deleteCurrency(id, player).thenAccept(success -> {
            if (success) {
                r18n().msg("currencies.delete.success").prefix().with("currency", id).send(player);
            } else {
                r18n().msg("currencies.delete.failed").prefix().send(player);
            }
        });
    }

    private void onHelp(@NotNull CommandContext ctx) {
        var player = ctx.asPlayer().orElseThrow();
        var alias = ctx.alias();

        r18n().msg("currencies.help.header").send(player);
        if (player.hasPermission(PERM_OVERVIEW)) {
            r18n().msg("currencies.help.overview").with("alias", alias).send(player);
        }
        r18n().msg("currencies.help.list").with("alias", alias).send(player);
        r18n().msg("currencies.help.info").with("alias", alias).send(player);
        if (player.hasPermission("currencies.command.create")) {
            r18n().msg("currencies.help.create").with("alias", alias).send(player);
        }
        if (player.hasPermission("currencies.command.update")) {
            r18n().msg("currencies.help.edit").with("alias", alias).send(player);
        }
        if (player.hasPermission("currencies.command.delete")) {
            r18n().msg("currencies.help.delete").with("alias", alias).send(player);
        }
        r18n().msg("currencies.help.footer").send(player);
    }

    // ── Shared ──────────────────────────────────────────────────────────────

    private void openOverview(@NotNull Player player) {
        economy.viewFrame().open(CurrencyOverviewView.class, player, Map.of("plugin", economy));
    }

    private static R18nManager r18n() {
        return R18nManager.getInstance();
    }
}
