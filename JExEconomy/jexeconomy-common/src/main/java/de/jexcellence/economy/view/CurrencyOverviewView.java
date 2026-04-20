package de.jexcellence.economy.view;

import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.jexplatform.view.PaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated overview listing all registered currencies.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public class CurrencyOverviewView extends PaginatedView<Currency> {

    private final State<JExEconomy> economy = initialState("plugin");

    public CurrencyOverviewView() {
        super();
    }

    @Override
    protected String translationKey() {
        return "currency_overview_ui";
    }

    @Override
    protected String[] layout() {
        return new String[]{
                "         ",
                "OOOOOOOOO",
                "   <p>   ",
                "         "
        };
    }

    @Override
    protected @NotNull CompletableFuture<List<Currency>> loadData(@NotNull Context ctx) {
        var currencies = economy.get(ctx).economyService().getAllCurrencies();
        return CompletableFuture.completedFuture(new ArrayList<>(currencies.values()));
    }

    @Override
    protected void renderItem(@NotNull Context ctx,
                              @NotNull BukkitItemComponentBuilder builder,
                              int index,
                              @NotNull Currency entry) {
        var player = ctx.getPlayer();
        var icon   = CurrencyDetailView.parseMaterial(entry.getIcon());

        var prefix = entry.getPrefix().isEmpty() ? "—" : entry.getPrefix();
        var suffix = entry.getSuffix().isEmpty() ? "—" : entry.getSuffix();

        builder.withItem(createItem(
                icon,
                i18n("currency.name", player)
                        .withPlaceholder("currency_identifier", entry.getIdentifier())
                        .withPlaceholder("currency_symbol", entry.getSymbol())
                        .build().component(),
                i18n("currency.lore", player)
                        .withPlaceholders(Map.of(
                                "currency_identifier", entry.getIdentifier(),
                                "currency_symbol", entry.getSymbol(),
                                "currency_prefix", prefix,
                                "currency_suffix", suffix,
                                "index", index + 1
                        ))
                        .build().children()
        )).onClick(click -> click.openForPlayer(
                CurrencyDetailView.class,
                Map.of(
                        "plugin", economy.get(click),
                        "currency", entry,
                        "initialData", click.getInitialData()
                )
        ));
    }
}
