package de.jexcellence.jexplatform.view;

import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.AnvilInput;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.ViewType;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Template for anvil-based text input workflows with validation and result passing.
 *
 * <p><strong>Prerequisite:</strong> The {@code AnvilInputFeature} must be installed
 * on the {@code ViewFrame} before any anvil view can be opened:
 *
 * <pre>{@code
 * ViewFrame.create(plugin)
 *     .install(AnvilInputFeature.AnvilInput)
 *     .with(RenameView.class)
 *     .register();
 * }</pre>
 *
 * <p>Subclasses provide a translation key, input processing, and optional validation:
 *
 * <pre>{@code
 * public class RenameView extends AnvilView {
 *     protected String translationKey() { return "rename_ui"; }
 *     protected Object processInput(String input, Context ctx) {
 *         return input.trim();
 *     }
 * }
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public abstract class AnvilView extends View {

    private static final Logger LOG = Logger.getLogger(AnvilView.class.getName());

    private final AnvilInput anvilInput = AnvilInput.createAnvilInput();
    private final @Nullable Class<? extends View> parentView;
    private final String baseKey;

    /** State container for passing data back to the parent view. */
    protected final State<Object> data = initialState("data");

    /** Mutable state backing the initial text in the anvil input. */
    protected final MutableState<String> initialInput = mutableState("");

    /**
     * Creates an anvil view with optional parent navigation.
     *
     * @param parentView the parent view class, or null to close on back
     */
    protected AnvilView(@Nullable Class<? extends View> parentView) {
        this.parentView = parentView;
        this.baseKey = translationKey();
    }

    /**
     * Creates an anvil view without parent navigation.
     */
    protected AnvilView() {
        this(null);
    }

    // ── Abstract hooks ──────────────────────────────────────────────────────────

    /**
     * Returns the translation namespace for this anvil view.
     *
     * @return the base i18n key
     */
    protected abstract String translationKey();

    /**
     * Processes the validated input and returns a result to pass back.
     *
     * @param input the user's text input
     * @param ctx   the interaction context
     * @return result object to pass to the parent view, or null
     */
    protected abstract @Nullable Object processInput(@NotNull String input, @NotNull Context ctx);

    // ── Configuration hooks ─────────────────────────────────────────────────────

    /**
     * Validates user input before processing.
     *
     * @param input the user's text input
     * @param ctx   the interaction context
     * @return {@code true} if the input is valid
     */
    protected boolean isValidInput(@NotNull String input, @NotNull Context ctx) {
        return !input.trim().isEmpty();
    }

    /**
     * Returns the initial text shown in the anvil.
     *
     * @param ctx the open context
     * @return the initial editable text
     */
    protected String initialText(@NotNull OpenContext ctx) {
        return "";
    }

    /**
     * Returns placeholder values for the translated title.
     *
     * @param ctx the open context
     * @return placeholder map for title translation
     */
    protected Map<String, Object> titlePlaceholders(@NotNull OpenContext ctx) {
        return Map.of();
    }

    // ── Lifecycle ───────────────────────────────────────────────────────────────

    /**
     * Configures the view as an anvil type with the input modifier.
     *
     * @param config the view configuration builder
     */
    @Override
    public void onInit(@NotNull ViewConfigBuilder config) {
        config.type(ViewType.ANVIL).cancelOnClick().use(anvilInput).title("");
    }

    /**
     * Applies the translated title and stores the initial input text.
     *
     * @param open the open context
     */
    @Override
    public void onOpen(@NotNull OpenContext open) {
        var text = initialText(open);
        if (text != null && !text.isEmpty()) {
            initialInput.set(text, open);
        }

        var title = (Component) i18n("title", open.getPlayer())
                .withPlaceholders(titlePlaceholders(open))
                .build().component();
        open.modifyConfig().title(
                LegacyComponentSerializer.legacySection().serialize(title)
        );
    }

    /**
     * Sets up the input and result slots.
     *
     * @param render the render context
     */
    @Override
    public void onFirstRender(@NotNull RenderContext render) {
        var player = render.getPlayer();
        setupInputSlot(render, player);
        setupResultSlot(render);
    }

    // ── Internal ────────────────────────────────────────────────────────────────

    private void setupInputSlot(@NotNull RenderContext render, @NotNull Player player) {
        var text = " ";
        try {
            var stored = initialInput.get(render);
            if (stored != null && !stored.isEmpty()) text = stored;
        } catch (Exception ignored) {
            // State not yet initialized
        }

        var item = new ItemStack(Material.NAME_TAG);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(text));
            item.setItemMeta(meta);
        }
        render.firstSlot(item);
    }

    private void setupResultSlot(@NotNull RenderContext render) {
        render.resultSlot().onClick(ctx -> {
            var input = anvilInput.get(ctx);

            if (!isValidInput(input, ctx)) {
                i18n("error.invalid_input", ctx.getPlayer())
                        .withPlaceholder("input", input != null ? input : "")
                        .build().sendMessage();
                return;
            }

            try {
                var result = processInput(input, ctx);
                ctx.back(prepareResult(result, input, ctx));
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to process anvil input: " + input, e);
                i18n("error.processing_failed", ctx.getPlayer())
                        .withPlaceholder("input", input)
                        .build().sendMessage();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> prepareResult(@Nullable Object result, @NotNull String input,
                                              @NotNull Context ctx) {
        var map = new HashMap<String, Object>();
        if (result != null) {
            map.put("result", result);
        } else {
            map.put("input", input);
        }

        try {
            var existing = data.get(ctx);
            if (existing instanceof Map<?, ?> m) {
                map.putAll((Map<? extends String, ?>) m);
            }
        } catch (Exception ignored) {
            // Data state may not be initialized
        }
        return map;
    }

    /**
     * Creates an {@link I18n.Builder} scoped to this view's translation namespace.
     *
     * @param suffix the key suffix
     * @param player the player for locale resolution
     * @return the translation builder
     */
    @SuppressWarnings("deprecation")
    protected I18n.Builder i18n(@NotNull String suffix, @NotNull Player player) {
        return new I18n.Builder(baseKey + "." + suffix, player);
    }
}
