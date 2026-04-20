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
 * Edit form for an existing currency, pre-populated with current values.
 *
 * <p>Each field slot opens {@link CurrencyFieldInputView} so the player can type
 * a new value via the anvil. On return the updated draft is re-rendered here.
 * The save button persists all changes via the economy service.
 *
 * <p>Required initial-data keys:
 * <ul>
 *   <li>{@code "plugin"}               — {@link JExEconomy} instance</li>
 *   <li>{@code "draft_currency"}       — {@link Currency} being edited (carries current edits)</li>
 *   <li>{@code "original_identifier"}  — identifier that was used to open this view; used by
 *       {@code updateCurrency} to locate the cache entry even if the identifier was changed</li>
 * </ul>
 *
 * @author JExcellence
 * @since 3.0.0
 */
public class CurrencyEditView extends BaseView {

    private final State<JExEconomy> economy            = initialState("plugin");
    private final State<Currency>   draft              = initialState("draft_currency");
    private final State<String>     originalIdentifier = initialState("original_identifier");

    public CurrencyEditView() {
        super(CurrencyDetailView.class);
    }

    @Override
    protected String translationKey() {
        return "currency_edit_ui";
    }

    @Override
    protected String[] layout() {
        return new String[]{
                "         ",
                " n s i   ",
                " p f   w ",
                "         "
        };
    }

    @Override
    protected void onRender(@NotNull RenderContext render, @NotNull Player player) {
        var cur    = draft.get(render);
        var eco    = economy.get(render);
        var origId = originalIdentifier.get(render);

        renderIdentifier(render, player, cur, eco, origId);
        renderSymbol(render, player, cur, eco, origId);
        renderIcon(render, player, cur, eco, origId);
        renderPrefix(render, player, cur, eco, origId);
        renderSuffix(render, player, cur, eco, origId);
        renderSaveButton(render, player, cur, eco, origId);
    }

    // ── Field slots ──────────────────────────────────────────────────────────────

    private void renderIdentifier(RenderContext render, Player player,
                                  Currency cur, JExEconomy eco, String origId) {
        render.layoutSlot('n', createItem(
                Material.NAME_TAG,
                i18n("identifier.name", player)
                        .withPlaceholder("value", cur.getIdentifier())
                        .build().component(),
                i18n("identifier.lore", player)
                        .withPlaceholder("value", cur.getIdentifier())
                        .build().children()
        )).onClick(click -> openField(click, cur, "identifier", origId));
    }

    private void renderSymbol(RenderContext render, Player player,
                               Currency cur, JExEconomy eco, String origId) {
        render.layoutSlot('s', createItem(
                Material.GOLD_NUGGET,
                i18n("symbol.name", player)
                        .withPlaceholder("value", cur.getSymbol())
                        .build().component(),
                i18n("symbol.lore", player)
                        .withPlaceholders(Map.of(
                                "value", cur.getSymbol(),
                                "currency_identifier", cur.getIdentifier()
                        ))
                        .build().children()
        )).onClick(click -> openField(click, cur, "symbol", origId));
    }

    private void renderIcon(RenderContext render, Player player,
                             Currency cur, JExEconomy eco, String origId) {
        render.layoutSlot('i', createItem(
                CurrencyDetailView.parseMaterial(cur.getIcon()),
                i18n("icon.name", player)
                        .withPlaceholder("value", cur.getIcon())
                        .build().component(),
                i18n("icon.lore", player)
                        .withPlaceholder("value", cur.getIcon())
                        .build().children()
        )).onClick(click -> openField(click, cur, "icon", origId));
    }

    private void renderPrefix(RenderContext render, Player player,
                               Currency cur, JExEconomy eco, String origId) {
        var display = cur.getPrefix().isEmpty() ? "—" : cur.getPrefix();
        render.layoutSlot('p', createItem(
                Material.WRITABLE_BOOK,
                i18n("prefix.name", player)
                        .withPlaceholder("value", display)
                        .build().component(),
                i18n("prefix.lore", player)
                        .withPlaceholders(Map.of(
                                "value", display,
                                "currency_identifier", cur.getIdentifier()
                        ))
                        .build().children()
        )).onClick(click -> openField(click, cur, "prefix", origId));
    }

    private void renderSuffix(RenderContext render, Player player,
                               Currency cur, JExEconomy eco, String origId) {
        var display = cur.getSuffix().isEmpty() ? "—" : cur.getSuffix();
        render.layoutSlot('f', createItem(
                Material.PAPER,
                i18n("suffix.name", player)
                        .withPlaceholder("value", display)
                        .build().component(),
                i18n("suffix.lore", player)
                        .withPlaceholders(Map.of(
                                "value", display,
                                "currency_identifier", cur.getIdentifier()
                        ))
                        .build().children()
        )).onClick(click -> openField(click, cur, "suffix", origId));
    }

    // ── Save button ──────────────────────────────────────────────────────────────

    private void renderSaveButton(RenderContext render, Player player,
                                  Currency cur, JExEconomy eco, String origId) {
        render.layoutSlot('w', createItem(
                Material.EMERALD,
                i18n("save.name", player).build().component(),
                i18n("save.lore", player)
                        .withPlaceholder("currency_identifier", cur.getIdentifier())
                        .build().children()
        )).onClick(click -> {
            var p = click.getPlayer();

            if (cur.getIdentifier().isEmpty()) {
                R18nManager.getInstance()
                        .msg("currency_edit_ui.save.missing_identifier")
                        .prefix()
                        .send(p);
                return;
            }

            if (cur.getSymbol().isEmpty()) {
                R18nManager.getInstance()
                        .msg("currency_edit_ui.save.missing_symbol")
                        .prefix()
                        .send(p);
                return;
            }

            eco.economyService().updateCurrency(origId, cur, p).thenAccept(success -> {
                if (success) {
                    R18nManager.getInstance()
                            .msg("currency_edit_ui.save.success")
                            .prefix()
                            .with("currency_identifier", cur.getIdentifier())
                            .send(p);
                } else {
                    R18nManager.getInstance()
                            .msg("currency_edit_ui.save.failed")
                            .prefix()
                            .with("currency_identifier", origId)
                            .send(p);
                }
            });
            click.closeForPlayer();
        });
    }

    // ── Navigation helper ────────────────────────────────────────────────────────

    /**
     * Opens the anvil field input view using the proper {@code ctx.openForPlayer()}
     * navigation so InventoryFramework handles the inventory transition correctly.
     */
    private void openField(@NotNull SlotClickContext click,
                           @NotNull Currency cur,
                           @NotNull String fieldName,
                           @NotNull String origId) {
        click.openForPlayer(CurrencyFieldInputView.class, Map.of(
                "plugin",               economy.get(click),
                "field",                fieldName,
                "draft_currency",       cur,
                "mode",                 CurrencyFieldInputView.MODE_EDIT,
                "original_identifier",  origId
        ));
    }
}
