package de.jexcellence.economy.view;

import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.jexplatform.view.BaseView;
import de.jexcellence.jextranslate.R18nManager;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Visual preview and creation form for a new currency.
 *
 * <p>Each slot shows the current draft value. Clicking a field slot opens
 * {@link CurrencyFieldInputView} via the anvil input; when the player confirms
 * their text the updated draft is passed back here and the view re-renders.
 * Clicking the create button persists the currency via the economy service.
 *
 * <p>Required initial-data keys:
 * <ul>
 *   <li>{@code "plugin"}         — {@link JExEconomy} instance</li>
 *   <li>{@code "draft_currency"} — {@link Currency} being built (may be empty on first open)</li>
 * </ul>
 *
 * @author JExcellence
 * @since 3.0.0
 */
public class CurrencyCreateView extends BaseView {

    private final State<JExEconomy> economy = initialState("plugin");
    private final State<Currency>   draft   = initialState("draft_currency");

    public CurrencyCreateView() {
        super(CurrencyOverviewView.class);
    }

    @Override
    protected String translationKey() {
        return "currency_create_ui";
    }

    @Override
    protected String[] layout() {
        return new String[]{
                "         ",
                " n s i   ",
                " p f   c ",
                "         "
        };
    }

    @Override
    protected void onRender(@NotNull RenderContext render, @NotNull Player player) {
        var cur = draft.get(render);
        var eco = economy.get(render);

        renderIdentifier(render, player, cur, eco);
        renderSymbol(render, player, cur, eco);
        renderIcon(render, player, cur, eco);
        renderPrefix(render, player, cur, eco);
        renderSuffix(render, player, cur, eco);
        renderCreateButton(render, player, cur, eco);
    }

    // ── Field slots ──────────────────────────────────────────────────────────────

    private void renderIdentifier(RenderContext render, Player player,
                                  Currency cur, JExEconomy eco) {
        var value = cur.getIdentifier().isEmpty() ? "—" : cur.getIdentifier();
        render.layoutSlot('n', createItem(
                Material.NAME_TAG,
                i18n("identifier.name", player)
                        .withPlaceholder("value", value)
                        .build().component(),
                i18n("identifier.lore", player)
                        .withPlaceholder("value", value)
                        .build().children()
        )).onClick(click -> openField(click, cur, "identifier"));
    }

    private void renderSymbol(RenderContext render, Player player,
                               Currency cur, JExEconomy eco) {
        var value = cur.getSymbol().isEmpty() ? "—" : cur.getSymbol();
        render.layoutSlot('s', createItem(
                Material.GOLD_NUGGET,
                i18n("symbol.name", player)
                        .withPlaceholder("value", value)
                        .build().component(),
                i18n("symbol.lore", player)
                        .withPlaceholder("value", value)
                        .build().children()
        )).onClick(click -> openField(click, cur, "symbol"));
    }

    private void renderIcon(RenderContext render, Player player,
                             Currency cur, JExEconomy eco) {
        render.layoutSlot('i', createItem(
                CurrencyDetailView.parseMaterial(cur.getIcon()),
                i18n("icon.name", player)
                        .withPlaceholder("value", cur.getIcon())
                        .build().component(),
                i18n("icon.lore", player)
                        .withPlaceholder("value", cur.getIcon())
                        .build().children()
        )).onClick(click -> openField(click, cur, "icon"));
    }

    private void renderPrefix(RenderContext render, Player player,
                               Currency cur, JExEconomy eco) {
        var display = cur.getPrefix().isEmpty() ? "—" : cur.getPrefix();
        render.layoutSlot('p', createItem(
                Material.WRITABLE_BOOK,
                i18n("prefix.name", player)
                        .withPlaceholder("value", display)
                        .build().component(),
                i18n("prefix.lore", player)
                        .withPlaceholder("value", display)
                        .build().children()
        )).onClick(click -> openField(click, cur, "prefix"));
    }

    private void renderSuffix(RenderContext render, Player player,
                               Currency cur, JExEconomy eco) {
        var display = cur.getSuffix().isEmpty() ? "—" : cur.getSuffix();
        render.layoutSlot('f', createItem(
                Material.PAPER,
                i18n("suffix.name", player)
                        .withPlaceholder("value", display)
                        .build().component(),
                i18n("suffix.lore", player)
                        .withPlaceholder("value", display)
                        .build().children()
        )).onClick(click -> openField(click, cur, "suffix"));
    }

    /**
     * Opens the anvil field input view using the proper {@code ctx.openForPlayer()}
     * navigation so InventoryFramework handles the inventory transition correctly.
     */
    private void openField(@NotNull SlotClickContext click,
                           @NotNull Currency cur,
                           @NotNull String fieldName) {
        click.openForPlayer(CurrencyFieldInputView.class, Map.of(
                "plugin",          economy.get(click),
                "field",           fieldName,
                "draft_currency",  cur,
                "mode",            CurrencyFieldInputView.MODE_CREATE
        ));
    }

    // ── Create button ────────────────────────────────────────────────────────────

    private void renderCreateButton(RenderContext render, Player player,
                                    Currency cur, JExEconomy eco) {
        render.layoutSlot('c', createItem(
                Material.EMERALD,
                i18n("create.name", player).build().component(),
                i18n("create.lore", player)
                        .withPlaceholder("currency_identifier",
                                cur.getIdentifier().isEmpty() ? "—" : cur.getIdentifier())
                        .build().children()
        )).onClick(click -> {
            var p = click.getPlayer();

            if (cur.getIdentifier().isEmpty()) {
                R18nManager.getInstance()
                        .msg("currency_create_ui.create.missing_identifier")
                        .prefix()
                        .send(p);
                return;
            }

            if (cur.getSymbol().isEmpty()) {
                R18nManager.getInstance()
                        .msg("currency_create_ui.create.missing_symbol")
                        .prefix()
                        .send(p);
                return;
            }

            eco.economyService().createCurrency(cur, p).thenAccept(success -> {
                if (success) {
                    R18nManager.getInstance()
                            .msg("currency_create_ui.create.success")
                            .prefix()
                            .with("currency_identifier", cur.getIdentifier())
                            .send(p);
                } else {
                    R18nManager.getInstance()
                            .msg("currency_create_ui.create.failed")
                            .prefix()
                            .with("currency_identifier", cur.getIdentifier())
                            .send(p);
                }
            });
            click.closeForPlayer();
        });
    }
}
