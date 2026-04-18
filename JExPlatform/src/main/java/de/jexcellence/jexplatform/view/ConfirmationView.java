package de.jexcellence.jexplatform.view;

import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Modal accept/decline dialog with translated titles, confirm/cancel panes,
 * and an optional callback.
 *
 * <p>Open with the fluent builder:
 *
 * <pre>{@code
 * new ConfirmationView.Builder()
 *     .withKey("delete_currency")
 *     .withInitialData(Map.of("currency", name))
 *     .withCallback(confirmed -> {
 *         if (confirmed) adapter.delete(name);
 *     })
 *     .openFor(context);
 * }</pre>
 *
 * <p>Layout: green glass ({@code c}) for confirm, red glass ({@code x}) for
 * cancel, arrow ({@code b}) for back navigation.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class ConfirmationView extends BaseView {

    private static final Logger LOG = Logger.getLogger(ConfirmationView.class.getName());

    private final State<String> customKey = initialState("key");
    private final State<Map<String, Object>> initialData = initialState("initialData");
    private final State<Consumer<Boolean>> callback = initialState("callback");

    /**
     * Creates a confirmation view (no-arg constructor for IF registration).
     */
    public ConfirmationView() {
        super();
    }

    @Override
    protected String translationKey() {
        return "";
    }

    /**
     * Returns the two-column confirmation layout.
     *
     * @return the dialog layout rows
     */
    @Override
    protected String[] layout() {
        return new String[]{
                "         ",
                " ccc xxx ",
                " ccc xxx ",
                " ccc xxx ",
                "         ",
                "b        "
        };
    }

    /**
     * Applies the dynamic translation key for the title.
     *
     * @param open the open context
     */
    @Override
    public void onOpen(@NotNull OpenContext open) {
        var key = customKey.get(open);
        if (key == null || key.isEmpty()) return;

        @SuppressWarnings("deprecation")
        var title = (Component) new I18n.Builder(key + ".title", open.getPlayer())
                .build().component();
        open.modifyConfig().title(
                LegacyComponentSerializer.legacySection().serialize(title)
        );
    }

    /**
     * Renders the confirm and cancel glass panes with translated names and lore.
     *
     * @param render the render context
     * @param player the player viewing the dialog
     */
    @Override
    @SuppressWarnings("deprecation")
    protected void onRender(@NotNull RenderContext render, @NotNull Player player) {
        var key = customKey.get(render);
        var data = safeGetInitialData(render);

        // Confirm pane (green)
        var confirmItem = createItem(
                Material.LIME_STAINED_GLASS_PANE,
                new I18n.Builder(key + ".confirm.name", player).build().component(),
                new I18n.Builder(key + ".confirm.lore", player)
                        .withPlaceholders(data).build().children()
        );
        render.layoutSlot('c', confirmItem).onClick(this::handleConfirm);

        // Cancel pane (red)
        var cancelItem = createItem(
                Material.RED_STAINED_GLASS_PANE,
                new I18n.Builder(key + ".cancel.name", player).build().component(),
                new I18n.Builder(key + ".cancel.lore", player)
                        .withPlaceholders(data).build().children()
        );
        render.layoutSlot('x', cancelItem).onClick(this::handleCancel);
    }

    /**
     * Overrides back to return {@code confirmed=false} and trigger the callback.
     *
     * @param ctx the click context
     */
    @Override
    protected void handleBack(@NotNull SlotClickContext ctx) {
        notifyCallback(false, ctx);
        ctx.back(mergeResult(false, ctx));
    }

    // ── Internal ────────────────────────────────────────────────────────────────

    private void handleConfirm(@NotNull Context ctx) {
        notifyCallback(true, ctx);
        ctx.back(mergeResult(true, ctx));
    }

    private void handleCancel(@NotNull Context ctx) {
        notifyCallback(false, ctx);
        ctx.back(mergeResult(false, ctx));
    }

    private void notifyCallback(boolean confirmed, @NotNull Context ctx) {
        try {
            var cb = callback.get(ctx);
            if (cb != null) cb.accept(confirmed);
        } catch (Exception ignored) {
            // Callback state may not be provided
        }
    }

    private Map<String, Object> mergeResult(boolean confirmed, @NotNull Context ctx) {
        var data = safeGetInitialData(ctx);
        var merged = new HashMap<>(data);
        merged.put("confirmed", confirmed);
        return merged;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeGetInitialData(@NotNull Context ctx) {
        try {
            var data = initialData.get(ctx);
            return data != null ? data : Map.of();
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    // ── Builder ─────────────────────────────────────────────────────────────────

    /**
     * Fluent builder for opening a confirmation dialog.
     */
    public static class Builder {

        private String key;
        private Map<String, Object> initialData;
        private Consumer<Boolean> callback;

        /**
         * Sets the translation key namespace for dialog strings.
         *
         * @param key the i18n key (e.g. {@code "delete_currency"})
         * @return this builder
         */
        public @NotNull Builder withKey(@NotNull String key) {
            this.key = key;
            return this;
        }

        /**
         * Sets initial data merged into the confirmation result.
         *
         * @param data the data payload
         * @return this builder
         */
        public @NotNull Builder withInitialData(@Nullable Map<String, Object> data) {
            this.initialData = data;
            return this;
        }

        /**
         * Sets the callback invoked with the user's decision.
         *
         * @param callback receives {@code true} for confirm, {@code false} for cancel
         * @return this builder
         */
        public @NotNull Builder withCallback(@Nullable Consumer<Boolean> callback) {
            this.callback = callback;
            return this;
        }

        /**
         * Opens the confirmation dialog for the player in the given context.
         *
         * @param context the Inventory Framework context
         */
        public void openFor(@NotNull Context context) {
            var data = new HashMap<String, Object>();
            if (key != null) data.put("key", key);
            if (initialData != null) data.put("initialData", initialData);
            if (callback != null) data.put("callback", callback);
            try {
                context.openForPlayer(ConfirmationView.class, data);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to open confirmation dialog", e);
                context.back(data);
            }
        }
    }
}
