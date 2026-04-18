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

import java.util.Map;

/**
 * Deletion confirmation view for a currency.
 *
 * <p>Displays currency information with warning styling and provides
 * confirm/cancel buttons. Confirming deletes via the economy service.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public class CurrencyDeleteView extends BaseView {

    private final State<JExEconomy> economy = initialState("plugin");
    private final State<Currency> currency = initialState("currency");

    public CurrencyDeleteView() {
        super(CurrencyDetailView.class);
    }

    @Override
    protected String translationKey() {
        return "currency_delete_ui";
    }

    @Override
    protected String[] layout() {
        return new String[]{
                "         ",
                "    w    ",
                "         ",
                " y     n ",
                "         "
        };
    }

    @Override
    protected void onRender(@NotNull RenderContext render, @NotNull Player player) {
        var cur = currency.get(render);

        renderWarning(render, player, cur);
        renderConfirm(render, player, cur);
        renderCancel(render, player);
    }

    private void renderWarning(RenderContext render, Player player, Currency cur) {
        render.layoutSlot('w', createItem(
                Material.BARRIER,
                i18n("warning.name", player)
                        .withPlaceholder("currency_identifier", cur.getIdentifier())
                        .build().component(),
                i18n("warning.lore", player)
                        .withPlaceholders(Map.of(
                                "currency_identifier", cur.getIdentifier(),
                                "currency_symbol", cur.getSymbol()
                        ))
                        .build().children()
        ));
    }

    private void renderConfirm(RenderContext render, Player player, Currency cur) {
        render.layoutSlot('y', createItem(
                Material.LIME_WOOL,
                i18n("confirm.name", player).build().component(),
                i18n("confirm.lore", player)
                        .withPlaceholder("currency_identifier", cur.getIdentifier())
                        .build().children()
        )).onClick(click -> {
            var p = click.getPlayer();
            var eco = economy.get(click);

            eco.economyService().deleteCurrency(cur.getIdentifier(), p).thenAccept(success -> {
                if (success) {
                    R18nManager.getInstance().msg("currency_delete_ui.success")
                            .prefix()
                            .with("currency_identifier", cur.getIdentifier())
                            .send(p);
                } else {
                    R18nManager.getInstance().msg("currency_delete_ui.failed")
                            .prefix()
                            .with("currency_identifier", cur.getIdentifier())
                            .send(p);
                }
            }).exceptionally(ex -> {
                R18nManager.getInstance().msg("currency_delete_ui.error")
                        .prefix()
                        .with("currency_identifier", cur.getIdentifier())
                        .with("error", ex.getMessage())
                        .send(p);
                return null;
            });
            click.closeForPlayer();
        });
    }

    private void renderCancel(RenderContext render, Player player) {
        render.layoutSlot('n', createItem(
                Material.RED_WOOL,
                i18n("cancel.name", player).build().component(),
                i18n("cancel.lore", player).build().children()
        )).onClick(this::handleBack);
    }
}
