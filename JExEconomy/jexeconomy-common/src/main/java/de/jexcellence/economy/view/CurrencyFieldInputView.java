package de.jexcellence.economy.view;

import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.jextranslate.R18nManager;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.AnvilInput;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.ViewType;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;

/**
 * Anvil-based text input for a single currency field (identifier, symbol, prefix, suffix, icon).
 *
 * <p>Opened by {@link CurrencyCreateView} and {@link CurrencyEditView}. On confirmation the
 * view applies the new value to the draft currency and navigates back to the parent view,
 * preserving the full draft state.
 *
 * <p>Required initial data keys:
 * <ul>
 *   <li>{@code "plugin"}        — {@link JExEconomy} instance</li>
 *   <li>{@code "field"}         — field name: {@code identifier|symbol|prefix|suffix|icon}</li>
 *   <li>{@code "draft_currency"} — {@link Currency} being built/edited</li>
 *   <li>{@code "mode"}          — {@code "create"} or {@code "edit"}</li>
 * </ul>
 *
 * @author JExcellence
 * @since 3.0.0
 */
public class CurrencyFieldInputView extends View {

    /** Field-name constant used to distinguish view modes. */
    public static final String MODE_CREATE = "create";
    public static final String MODE_EDIT   = "edit";

    private final AnvilInput anvilInput = AnvilInput.createAnvilInput();

    private final State<JExEconomy> economy            = initialState("plugin");
    private final State<String>     field              = initialState("field");
    private final State<Currency>   draft              = initialState("draft_currency");
    private final State<String>     mode               = initialState("mode");

    @Override
    public void onInit(@NotNull ViewConfigBuilder config) {
        config.type(ViewType.ANVIL)
                .cancelOnClick()
                .use(anvilInput)
                .title("");
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onOpen(@NotNull OpenContext open) {
        var f = field.get(open);

        // Set the view title via modifyConfig so InventoryFramework updates the inventory name.
        var titleComp = (Component) new I18n.Builder("currency_field_input_ui.title", open.getPlayer())
                .withPlaceholder("field", prettyField(f))
                .build()
                .component();
        open.modifyConfig().title(
                LegacyComponentSerializer.legacySection().serialize(titleComp)
        );
        // NOTE: anvilInput.set(...) must NOT be called here — AnvilInputFeature casts
        // the context to IFRenderContext internally, which OpenContext is not. Pre-populate
        // the text inside onFirstRender instead (see setupInputSlot).
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render) {
        setupInputSlot(render);
        setupResultSlot(render);

        // Pre-populate the anvil input text with the current field value.
        var cur     = draft.get(render);
        var f       = field.get(render);
        var current = fieldValue(cur, f);
        if (current != null && !current.isEmpty()) {
            anvilInput.set(current, render);
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    private void setupInputSlot(@NotNull RenderContext render) {
        var cur     = draft.get(render);
        var f       = field.get(render);
        var current = fieldValue(cur, f);

        var item = new ItemStack(Material.NAME_TAG);
        var meta = item.getItemMeta();
        if (meta != null) {
            var text = (current != null && !current.isEmpty()) ? current : " ";
            meta.displayName(Component.text(text));
            item.setItemMeta(meta);
        }
        render.firstSlot(item);
    }

    private void setupResultSlot(@NotNull RenderContext render) {
        render.resultSlot().onClick(ctx -> {
            var input = anvilInput.get(ctx);
            if (input == null || input.isBlank()) {
                R18nManager.getInstance()
                        .msg("currency_field_input_ui.error.empty")
                        .prefix()
                        .send(ctx.getPlayer());
                return;
            }

            var f       = field.get(ctx);
            var cur     = draft.get(ctx);
            var eco     = economy.get(ctx);
            var m       = mode.get(ctx);

            // Validate icon field (must be a valid Material name)
            if ("icon".equals(f) && !isValidMaterial(input)) {
                R18nManager.getInstance()
                        .msg("currency_field_input_ui.error.invalid_material")
                        .prefix()
                        .with("value", input)
                        .send(ctx.getPlayer());
                return;
            }

            var updated = applyField(cur, f, input.trim());

            if (MODE_CREATE.equals(m)) {
                ctx.openForPlayer(CurrencyCreateView.class, Map.of(
                        "plugin",          eco,
                        "draft_currency",  updated
                ));
            } else {
                // Retrieve the original identifier that was threaded through from CurrencyEditView
                // so that EconomyService can locate the correct cache entry even if the identifier
                // field itself was just changed.
                var origId = cur.getIdentifier();
                var rawData = ctx.getInitialData();
                if (rawData instanceof Map<?, ?> dataMap) {
                    var threaded = dataMap.get("original_identifier");
                    if (threaded != null) origId = threaded.toString();
                }
                ctx.openForPlayer(CurrencyEditView.class, Map.of(
                        "plugin",               eco,
                        "draft_currency",       updated,
                        "original_identifier",  origId
                ));
            }
        });
    }

    /**
     * Returns the current value of {@code field} on {@code cur}, or an empty string.
     */
    public static String fieldValue(@NotNull Currency cur, @NotNull String field) {
        return switch (field) {
            case "identifier" -> cur.getIdentifier();
            case "symbol"     -> cur.getSymbol();
            case "prefix"     -> cur.getPrefix();
            case "suffix"     -> cur.getSuffix();
            case "icon"       -> cur.getIcon();
            default           -> "";
        };
    }

    /**
     * Returns a new {@link Currency} identical to {@code cur} but with
     * {@code field} set to {@code value}.
     *
     * <p>The returned object is always a fresh instance so that the draft passed
     * to InventoryFramework states is never mutated in-place.
     */
    public static Currency applyField(@NotNull Currency cur, @NotNull String field, @NotNull String value) {
        var id     = cur.getIdentifier();
        var sym    = cur.getSymbol();
        var pre    = cur.getPrefix();
        var suf    = cur.getSuffix();
        var icon   = cur.getIcon();

        return switch (field) {
            case "identifier" -> new Currency(value, sym, pre, suf, icon);
            case "symbol"     -> new Currency(id, value, pre, suf, icon);
            case "prefix"     -> new Currency(id, sym, value, suf, icon);
            case "suffix"     -> new Currency(id, sym, pre, value, icon);
            case "icon"       -> new Currency(id, sym, pre, suf, value);
            default           -> new Currency(id, sym, pre, suf, icon);
        };
    }

    /** Human-readable field label for the title. */
    private static String prettyField(@NotNull String field) {
        return switch (field) {
            case "identifier" -> "Identifier";
            case "symbol"     -> "Symbol";
            case "prefix"     -> "Prefix";
            case "suffix"     -> "Suffix";
            case "icon"       -> "Icon";
            default           -> field;
        };
    }

    private static boolean isValidMaterial(@NotNull String name) {
        try {
            Material.valueOf(name.toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Opens this view for the given player with the required initial data.
     *
     * @param player calling player
     * @param eco    JExEconomy instance
     * @param f      field name to edit
     * @param cur    draft currency being built or edited
     * @param m      {@link #MODE_CREATE} or {@link #MODE_EDIT}
     */
    public static void open(@NotNull Player player, @NotNull JExEconomy eco,
                            @NotNull String f, @NotNull Currency cur, @NotNull String m) {
        eco.viewFrame().open(CurrencyFieldInputView.class, player, Map.of(
                "plugin",          eco,
                "field",           f,
                "draft_currency",  cur,
                "mode",            m
        ));
    }
}
