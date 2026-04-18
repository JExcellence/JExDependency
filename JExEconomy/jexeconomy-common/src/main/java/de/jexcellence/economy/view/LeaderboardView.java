package de.jexcellence.economy.view;

import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.database.entity.Account;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.jexplatform.view.PaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated leaderboard showing top players for a currency.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public class LeaderboardView extends PaginatedView<Account> {

    private static final DecimalFormat BALANCE_FORMAT = new DecimalFormat("#,###.##");

    private final State<JExEconomy> economy = initialState("plugin");
    private final State<Currency> currency = initialState("currency");

    public LeaderboardView() {
        super(CurrencyDetailView.class);
    }

    @Override
    protected String translationKey() {
        return "currency_leaderboard_ui";
    }

    @Override
    protected String[] layout() {
        return new String[]{
                "         ",
                "         ",
                "  OOOOO  ",
                "  OOOOO  ",
                "         ",
                "   <p>   "
        };
    }

    @Override
    protected @NotNull CompletableFuture<List<Account>> loadData(@NotNull Context ctx) {
        var cur = currency.get(ctx);
        var eco = economy.get(ctx);
        return eco.economyService().getTopAccounts(cur, 25);
    }

    @Override
    protected void renderItem(@NotNull Context ctx,
                              @NotNull BukkitItemComponentBuilder builder,
                              int index,
                              @NotNull Account entry) {
        var player = ctx.getPlayer();
        var cur = currency.get(ctx);
        var rank = index + 1;
        var rankMaterial = rankMaterial(rank);
        var rankColor = rankColor(rank);

        builder.withItem(createItem(
                rankMaterial,
                i18n("player_entry.name", player)
                        .withPlaceholders(Map.of(
                                "rank", rank,
                                "rank_color", rankColor,
                                "player_name", entry.getPlayer().getPlayerName(),
                                "currency_symbol", cur.getSymbol()
                        ))
                        .build().component(),
                i18n("player_entry.lore", player)
                        .withPlaceholders(Map.of(
                                "rank", rank,
                                "player_name", entry.getPlayer().getPlayerName(),
                                "balance", BALANCE_FORMAT.format(entry.getBalance()),
                                "currency_symbol", cur.getSymbol(),
                                "currency_identifier", cur.getIdentifier(),
                                "formatted_balance", cur.format(entry.getBalance())
                        ))
                        .build().children()
        ));
    }

    @Override
    protected void onPaginatedRender(@NotNull RenderContext render, @NotNull Player player) {
        var cur = currency.get(render);

        render.slot(0, 4, createItem(
                CurrencyDetailView.parseMaterial(cur.getIcon()),
                i18n("currency_info.name", player)
                        .withPlaceholders(Map.of(
                                "currency_identifier", cur.getIdentifier(),
                                "currency_symbol", cur.getSymbol()
                        ))
                        .build().component(),
                i18n("currency_info.lore", player)
                        .withPlaceholders(Map.of(
                                "currency_identifier", cur.getIdentifier(),
                                "currency_symbol", cur.getSymbol()
                        ))
                        .build().children()
        ));
    }

    private static Material rankMaterial(int rank) {
        return switch (rank) {
            case 1 -> Material.DIAMOND_BLOCK;
            case 2 -> Material.GOLD_BLOCK;
            case 3 -> Material.IRON_BLOCK;
            default -> Material.PLAYER_HEAD;
        };
    }

    private static String rankColor(int rank) {
        return switch (rank) {
            case 1 -> "<gradient:#FFD700:#FFA500>";
            case 2 -> "<gradient:#C0C0C0:#A8A8A8>";
            case 3 -> "<gradient:#CD7F32:#B87333>";
            default -> "<white>";
        };
    }
}
