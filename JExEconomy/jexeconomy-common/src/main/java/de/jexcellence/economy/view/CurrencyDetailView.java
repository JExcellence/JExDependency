package de.jexcellence.economy.view;

import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.jexplatform.view.BaseView;
import de.jexcellence.jextranslate.R18nManager;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Detail view for a single currency showing properties and admin actions.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public class CurrencyDetailView extends BaseView {

    private final State<JExEconomy> economy = initialState("plugin");
    private final State<Currency> currency = initialState("currency");

    public CurrencyDetailView() {
        super(CurrencyOverviewView.class);
    }

    @Override
    protected String translationKey() {
        return "currency_detail_ui";
    }

    @Override
    protected String[] layout() {
        return new String[]{
                "         ",
                " is l pf ",
                "         ",
                "  e      ",
                "        r"
        };
    }

    @Override
    protected void onRender(@NotNull RenderContext render, @NotNull Player player) {
        var cur = currency.get(render);
        var eco = economy.get(render);

        renderCurrencyIcon(render, player, cur);
        renderSymbol(render, player, cur);
        renderPrefix(render, player, cur);
        renderSuffix(render, player, cur);
        renderLeaderboard(render, player, cur);
        renderEdit(render, player, cur, eco);
        renderReset(render, player, cur);
    }

    private void renderCurrencyIcon(RenderContext render, Player player, Currency cur) {
        render.layoutSlot('i', createItem(
                parseMaterial(cur.getIcon()),
                i18n("currency_icon.name", player)
                        .withPlaceholder("currency_identifier", cur.getIdentifier())
                        .build().component(),
                i18n("currency_icon.lore", player)
                        .withPlaceholders(Map.of(
                                "material_name", cur.getIcon(),
                                "currency_identifier", cur.getIdentifier()
                        ))
                        .build().children()
        ));
    }

    private void renderSymbol(RenderContext render, Player player, Currency cur) {
        render.layoutSlot('s', createItem(
                Material.GOLD_NUGGET,
                i18n("currency_symbol.name", player)
                        .withPlaceholder("currency_symbol", cur.getSymbol())
                        .build().component(),
                i18n("currency_symbol.lore", player)
                        .withPlaceholders(Map.of(
                                "currency_symbol", cur.getSymbol(),
                                "currency_identifier", cur.getIdentifier()
                        ))
                        .build().children()
        ));
    }

    private void renderPrefix(RenderContext render, Player player, Currency cur) {
        var prefix    = cur.getPrefix();
        var hasPrefix = prefix != null && !prefix.isEmpty();
        var display   = hasPrefix ? prefix : "—";

        render.layoutSlot('p', createItem(
                Material.WRITABLE_BOOK,
                i18n("currency_prefix.name", player)
                        .withPlaceholder("currency_prefix", display)
                        .build().component(),
                i18n("currency_prefix.lore", player)
                        .withPlaceholders(Map.of(
                                "currency_prefix", display,
                                "has_prefix", hasPrefix ? "true" : "false"
                        ))
                        .build().children()
        ));
    }

    private void renderSuffix(RenderContext render, Player player, Currency cur) {
        var suffix    = cur.getSuffix();
        var hasSuffix = suffix != null && !suffix.isEmpty();
        var display   = hasSuffix ? suffix : "—";

        render.layoutSlot('f', createItem(
                Material.PAPER,
                i18n("currency_suffix.name", player)
                        .withPlaceholder("currency_suffix", display)
                        .build().component(),
                i18n("currency_suffix.lore", player)
                        .withPlaceholders(Map.of(
                                "currency_suffix", display,
                                "has_suffix", hasSuffix ? "true" : "false"
                        ))
                        .build().children()
        ));
    }

    private void renderLeaderboard(RenderContext render, Player player, Currency cur) {
        render.layoutSlot('l', createItem(
                Material.DIAMOND,
                i18n("leaderboard.name", player)
                        .withPlaceholder("currency_identifier", cur.getIdentifier())
                        .build().component(),
                i18n("leaderboard.lore", player)
                        .withPlaceholders(Map.of(
                                "currency_identifier", cur.getIdentifier(),
                                "currency_symbol", cur.getSymbol()
                        ))
                        .build().children()
        )).onClick(click -> click.openForPlayer(
                LeaderboardView.class,
                Map.of(
                        "plugin", economy.get(click),
                        "currency", currency.get(click),
                        "initialData", click.getInitialData()
                )
        ));
    }

    private void renderEdit(RenderContext render, Player player, Currency cur, JExEconomy eco) {
        render.layoutSlot('e', createItem(
                Material.ANVIL,
                i18n("edit.name", player)
                        .withPlaceholder("currency_identifier", cur.getIdentifier())
                        .build().component(),
                i18n("edit.lore", player)
                        .withPlaceholder("currency_identifier", cur.getIdentifier())
                        .build().children()
        )).displayIf(ctx -> {
            var p = ctx.getPlayer();
            return p.hasPermission("jexeconomy.admin.edit")
                    || p.hasPermission("jexeconomy.admin.*")
                    || p.isOp();
        }).onClick(click -> {
            // Pass a draft copy of the currency (without DB id) so the edit view can build
            // on it. The original identifier is threaded through for save lookup.
            var editDraft = new Currency(
                    cur.getIdentifier(), cur.getSymbol(),
                    cur.getPrefix(), cur.getSuffix(), cur.getIcon());
            var data = new HashMap<String, Object>();
            data.put("plugin",              eco);
            data.put("draft_currency",      editDraft);
            data.put("original_identifier", cur.getIdentifier());
            click.openForPlayer(CurrencyEditView.class, data);
        });
    }

    private void renderReset(RenderContext render, Player player, Currency cur) {
        render.layoutSlot('r', createItem(
                Material.BARRIER,
                i18n("reset_all.name", player)
                        .withPlaceholder("currency_identifier", cur.getIdentifier())
                        .build().component(),
                i18n("reset_all.lore", player)
                        .withPlaceholders(Map.of(
                                "currency_identifier", cur.getIdentifier(),
                                "currency_symbol", cur.getSymbol()
                        ))
                        .build().children()
        )).displayIf(ctx -> {
            var p = ctx.getPlayer();
            return p.hasPermission("jexeconomy.admin.reset")
                    || p.hasPermission("jexeconomy.admin.*")
                    || p.isOp();
        }).onClick(click -> {
            var p = click.getPlayer();
            R18nManager.getInstance().msg("currency_detail_ui.reset_all.confirm_chat")
                    .prefix()
                    .with("currency_identifier", cur.getIdentifier())
                    .send(p);
            click.closeForPlayer();
        });
    }

    static Material parseMaterial(String name) {
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Material.GOLD_INGOT;
        }
    }
}
