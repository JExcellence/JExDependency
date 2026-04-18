package de.jexcellence.economy.placeholder;

import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.service.EconomyService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * PlaceholderAPI expansion for JExEconomy currency and balance placeholders.
 *
 * <p>Supported patterns:
 * <ul>
 *   <li>{@code %jexeconomy_currency_<id>_name|symbol|prefix|suffix%}</li>
 *   <li>{@code %jexeconomy_balance_<id>%} — raw two-decimal balance</li>
 *   <li>{@code %jexeconomy_balance_<id>_rounded%} — integer balance</li>
 *   <li>{@code %jexeconomy_balance_<id>_formatted%} — prefix + amount + symbol + suffix</li>
 * </ul>
 *
 * @author JExcellence
 * @since 3.0.0
 */
public class EconomyExpansion extends PlaceholderExpansion {

    private final JExEconomy economy;
    private final NumberFormat decimalFmt;
    private final NumberFormat integerFmt;

    public EconomyExpansion(@NotNull JExEconomy economy) {
        this.economy = economy;
        this.decimalFmt = NumberFormat.getInstance(Locale.US);
        this.decimalFmt.setMinimumFractionDigits(2);
        this.decimalFmt.setMaximumFractionDigits(2);
        this.integerFmt = NumberFormat.getIntegerInstance(Locale.US);
    }

    @Override
    public @NotNull String getIdentifier() {
        return "jexeconomy";
    }

    @Override
    public @NotNull String getAuthor() {
        return "JExcellence";
    }

    @Override
    public @NotNull String getVersion() {
        return "3.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {
        var service = economy.economyService();
        var parts = params.split("_");

        // currency_<id>_<field>
        if (params.startsWith("currency_") && parts.length >= 3) {
            return handleCurrencyInfo(service, parts[1], parts[2]);
        }

        // balance_<id>[_format]
        if (params.startsWith("balance_") && parts.length >= 2 && player != null) {
            var currencyId = parts[1];
            var format = parts.length >= 3 ? parts[2] : "amount";
            return handleBalance(service, player, currencyId, format);
        }

        return null;
    }

    private @Nullable String handleCurrencyInfo(@NotNull EconomyService service,
                                                 @NotNull String id,
                                                 @NotNull String field) {
        var opt = service.findCurrency(id);
        if (opt.isEmpty()) return null;
        var c = opt.get();
        return switch (field) {
            case "name" -> c.getIdentifier();
            case "symbol" -> c.getSymbol();
            case "prefix" -> c.getPrefix();
            case "suffix" -> c.getSuffix();
            default -> null;
        };
    }

    private @Nullable String handleBalance(@NotNull EconomyService service,
                                            @NotNull OfflinePlayer player,
                                            @NotNull String currencyId,
                                            @NotNull String format) {
        var opt = service.findCurrency(currencyId);
        if (opt.isEmpty()) return "N/A";
        var currency = opt.get();
        try {
            var balance = service.getBalance(player, currency).join();
            return switch (format) {
                case "rounded" -> integerFmt.format(balance);
                case "formatted" -> currency.format(balance);
                default -> decimalFmt.format(balance);
            };
        } catch (Exception e) {
            return "N/A";
        }
    }
}
