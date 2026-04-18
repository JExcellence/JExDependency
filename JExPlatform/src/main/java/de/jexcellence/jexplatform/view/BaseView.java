package de.jexcellence.jexplatform.view;

import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Template-method base for Inventory Framework views providing layout configuration,
 * translated titles, automatic back navigation, and empty-slot filling.
 *
 * <p>Subclasses define their translation key and rendering logic while the base
 * class handles chrome and navigation:
 *
 * <pre>{@code
 * public class RankView extends BaseView {
 *     public RankView() { super(MainMenuView.class); }
 *     protected String translationKey() { return "rank_ui"; }
 *     protected void onRender(RenderContext render, Player player) {
 *         // render rank entries
 *     }
 * }
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public abstract class BaseView extends View {

    private static final Logger LOG = Logger.getLogger(BaseView.class.getName());

    private final @Nullable Class<? extends View> parentView;
    private final String baseKey;

    /**
     * Creates a view with optional parent navigation.
     *
     * @param parentView the parent view class for back navigation, or null to close
     */
    protected BaseView(@Nullable Class<? extends View> parentView) {
        this.parentView = parentView;
        this.baseKey = translationKey();
    }

    /**
     * Creates a view without parent navigation (closes on back).
     */
    protected BaseView() {
        this(null);
    }

    // ── Abstract hooks ──────────────────────────────────────────────────────────

    /**
     * Returns the translation namespace for this view (e.g. {@code "rank_ui"}).
     *
     * @return the base i18n key
     */
    protected abstract String translationKey();

    /**
     * Renders the view content after navigation chrome is placed.
     *
     * @param render the render context for slot registration
     * @param player the player viewing the inventory
     */
    protected abstract void onRender(@NotNull RenderContext render, @NotNull Player player);

    // ── Configuration ───────────────────────────────────────────────────────────

    /**
     * Returns the inventory layout rows, each string representing nine slots.
     *
     * @return the layout pattern, or a blank six-row default
     */
    protected String[] layout() {
        return new String[]{
                "         ", "         ", "         ",
                "         ", "         ", "         "
        };
    }

    /**
     * Returns the inventory row count when no meaningful layout is defined.
     *
     * @return the number of rows (default 6)
     */
    protected int size() {
        return 6;
    }

    /**
     * Returns the material used to fill empty slots.
     *
     * @return the filler material (default gray stained glass)
     */
    protected Material fillerMaterial() {
        return Material.GRAY_STAINED_GLASS_PANE;
    }

    /**
     * Returns whether empty slots should be auto-filled with the filler material.
     *
     * @return {@code true} to enable auto-fill (default)
     */
    protected boolean autoFill() {
        return true;
    }

    /**
     * Returns the update interval in ticks, or 0 for no scheduled updates.
     *
     * @return the tick interval between scheduled updates
     */
    protected int updateInterval() {
        return 0;
    }

    /**
     * Returns placeholder values for the translated title.
     *
     * @param open the open context
     * @return placeholder map applied to the title translation
     */
    protected Map<String, Object> titlePlaceholders(@NotNull OpenContext open) {
        return Map.of();
    }

    // ── Lifecycle ───────────────────────────────────────────────────────────────

    /**
     * Configures layout or size, and optional scheduled updates.
     *
     * @param config the view configuration builder
     */
    @Override
    public void onInit(@NotNull ViewConfigBuilder config) {
        var layout = layout();
        if (hasLayoutContent(layout)) {
            config.layout(layout);
        } else {
            config.size(size());
        }
        config.cancelOnClick();
        if (updateInterval() > 0) {
            config.scheduleUpdate(updateInterval());
        }
        config.build();
    }

    /**
     * Applies the translated title from the view's translation key.
     *
     * @param open the open context
     */
    @Override
    public void onOpen(@NotNull OpenContext open) {
        var title = (Component) i18n("title", open.getPlayer())
                .withPlaceholders(titlePlaceholders(open))
                .build().component();
        open.modifyConfig().title(
                LegacyComponentSerializer.legacySection().serialize(title)
        );
    }

    /**
     * Places the back button, delegates to subclass rendering, then fills empty slots.
     *
     * @param render the render context
     */
    @Override
    public void onFirstRender(@NotNull RenderContext render) {
        var player = render.getPlayer();
        placeBackButton(render, player);
        onRender(render, player);
    }

    // ── Navigation ──────────────────────────────────────────────────────────────

    /**
     * Handles back button activation by navigating to the parent view or closing.
     *
     * @param ctx the click context
     */
    protected void handleBack(@NotNull SlotClickContext ctx) {
        if (parentView == null) {
            ctx.closeForPlayer();
            return;
        }
        try {
            ctx.openForPlayer(parentView, ctx.getInitialData());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to navigate to parent view", e);
            ctx.closeForPlayer();
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    /**
     * Creates an {@link I18n.Builder} scoped to this view's translation namespace.
     *
     * @param suffix the key suffix (e.g. {@code "button.confirm"})
     * @param player the player for locale resolution
     * @return the translation builder
     */
    @SuppressWarnings("deprecation")
    protected I18n.Builder i18n(@NotNull String suffix, @NotNull Player player) {
        return new I18n.Builder(baseKey + "." + suffix, player);
    }

    /**
     * Creates an item with a display name.
     *
     * @param material the item material
     * @param name     the Adventure component display name
     * @return the item stack
     */
    protected static ItemStack createItem(@NotNull Material material, @NotNull Component name) {
        var item = new ItemStack(material);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates an item with a display name and lore.
     *
     * @param material the item material
     * @param name     the Adventure component display name
     * @param lore     the Adventure component lore lines
     * @return the item stack
     */
    protected static ItemStack createItem(@NotNull Material material, @NotNull Component name,
                                          @NotNull List<Component> lore) {
        var item = new ItemStack(material);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void placeBackButton(@NotNull RenderContext render, @NotNull Player player) {
        var layout = layout();
        var rows = layout != null ? layout.length : size();
        if (rows <= 1) return;

        var slot = (rows - 1) * 9;

        // Skip if the layout character at bottom-left is not a space
        if (layout != null && layout.length > 0) {
            var lastRow = layout[layout.length - 1];
            if (lastRow != null && !lastRow.isEmpty() && lastRow.charAt(0) != ' ') {
                return;
            }
        }

        var backItem = createItem(Material.ARROW,
                i18n("back", player).build().component());
        render.slot(slot, backItem).onClick(this::handleBack);
    }

    private static boolean hasLayoutContent(@Nullable String[] layout) {
        if (layout == null) return false;
        for (var row : layout) {
            if (row != null) {
                for (var c : row.toCharArray()) {
                    if (c != ' ') return true;
                }
            }
        }
        return false;
    }
}
