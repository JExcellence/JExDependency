package de.jexcellence.economy.command;

import com.raindropcentral.commands.v2.argument.ArgumentType;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.service.EconomyService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * JExCommand 2.0 argument type resolving raw tokens into {@link Currency} entities
 * via the in-memory {@link EconomyService} cache.
 *
 * <p>YAML schema uses the id {@code currency}:
 * <pre>
 * argumentSchema:
 *   - { name: currency, type: currency, required: true }
 * </pre>
 *
 * <p>On failure, emits the i18n key {@code economy.error.currency-not-found} with
 * the placeholder {@code {currency}} equal to the raw token.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public final class CurrencyArgumentType {

    private CurrencyArgumentType() {}

    /**
     * Builds an {@link ArgumentType ArgumentType&lt;Currency&gt;} backed by the given
     * service.
     */
    public static @NotNull ArgumentType<Currency> of(@NotNull EconomyService service) {
        return ArgumentType.custom(
                "currency",
                Currency.class,
                (sender, raw) -> service.findCurrency(raw)
                        .map(ArgumentType.ParseResult::ok)
                        .orElseGet(() -> ArgumentType.ParseResult.err(
                                "economy.error.currency-not-found",
                                Map.of("currency", raw))),
                (sender, partial) -> {
                    var lower = partial.toLowerCase(Locale.ROOT);
                    return service.getAllCurrencies().values().stream()
                            .map(Currency::getIdentifier)
                            .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(lower))
                            .sorted()
                            .toList();
                });
    }

    /**
     * Convenience overload whose tab completion excludes the given identifier — useful
     * for edit/delete flows where repeating the currently-edited currency is redundant.
     */
    public static @NotNull ArgumentType<Currency> excluding(@NotNull EconomyService service,
                                                             @NotNull String excludedIdentifier) {
        return ArgumentType.custom(
                "currency",
                Currency.class,
                (sender, raw) -> service.findCurrency(raw)
                        .map(ArgumentType.ParseResult::ok)
                        .orElseGet(() -> ArgumentType.ParseResult.err(
                                "economy.error.currency-not-found",
                                Map.of("currency", raw))),
                (sender, partial) -> {
                    var lower = partial.toLowerCase(Locale.ROOT);
                    var skip = excludedIdentifier.toLowerCase(Locale.ROOT);
                    return service.getAllCurrencies().values().stream()
                            .map(Currency::getIdentifier)
                            .filter(id -> !id.toLowerCase(Locale.ROOT).equals(skip))
                            .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(lower))
                            .sorted()
                            .toList();
                });
    }

    /**
     * Returns the list of known currency identifiers — useful for builder APIs that
     * need the raw completion list (e.g. reload commands).
     */
    public static @NotNull List<String> knownIdentifiers(@NotNull EconomyService service) {
        return service.getAllCurrencies().values().stream()
                .map(Currency::getIdentifier)
                .sorted()
                .toList();
    }
}
