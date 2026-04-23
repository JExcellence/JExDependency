package de.jexcellence.jexplatform.view;

import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.component.Pagination;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Generic paginated grid with async data loading, navigation arrows, and
 * a page indicator.
 *
 * <p>Layout characters:
 * <ul>
 *   <li>{@code O} — pagination content slots</li>
 *   <li>{@code <} — previous page button</li>
 *   <li>{@code >} — next page button</li>
 *   <li>{@code p} — page indicator</li>
 *   <li>{@code X} — decoration/filler slots</li>
 * </ul>
 *
 * <pre>{@code
 * public class PlayerListView extends PaginatedView<OfflinePlayer> {
 *     protected String translationKey() { return "player_list_ui"; }
 *     protected CompletableFuture<List<OfflinePlayer>> loadData(Context ctx) {
 *         return CompletableFuture.supplyAsync(() -> List.of(Bukkit.getOfflinePlayers()));
 *     }
 *     protected void renderItem(Context ctx, BukkitItemComponentBuilder b, int i, OfflinePlayer p) {
 *         b.withItem(createPlayerHead(p));
 *     }
 * }
 * }</pre>
 *
 * @param <T> the type of elements in the paginated grid
 *
 * @author JExcellence
 * @since 1.0.0
 */
public abstract class PaginatedView<T> extends BaseView {

    private final State<Pagination> paginationState =
            this.buildLazyAsyncPaginationState(this::loadData)
                    .layoutTarget('O')
                    .elementFactory(this::renderItem)
                    .build();

    /**
     * Creates a paginated view with optional parent navigation.
     *
     * @param parentView the parent view class, or null to close on back
     */
    protected PaginatedView(@Nullable Class<? extends View> parentView) {
        super(parentView);
    }

    /**
     * Creates a paginated view without parent navigation.
     */
    protected PaginatedView() {
        this(null);
    }

    // ── Abstract hooks ──────────────────────────────────────────────────────────

    /**
     * Loads paginated data asynchronously.
     *
     * @param ctx the inventory context
     * @return future containing the list of items to paginate
     */
    protected abstract CompletableFuture<List<T>> loadData(@NotNull Context ctx);

    /**
     * Renders a single item in the paginated grid.
     *
     * @param context the inventory context
     * @param builder the item component builder
     * @param index   the item's index in the current page
     * @param entry   the data entry to render
     */
    protected abstract void renderItem(@NotNull Context context,
                                       @NotNull BukkitItemComponentBuilder builder,
                                       int index,
                                       @NotNull T entry);

    // ── Configuration ───────────────────────────────────────────────────────────

    /**
     * Returns the default pagination layout with bordered content area.
     *
     * @return the layout rows
     */
    @Override
    protected String[] layout() {
        return new String[]{
                "XXXXXXXXX",
                "XOOOOOOOX",
                "XOOOOOOOX",
                "XOOOOOOOX",
                "XXXXXXXXX",
                "   <p>   "
        };
    }

    // ── Rendering ───────────────────────────────────────────────────────────────

    /**
     * Renders border decoration, pagination navigation buttons, and the page indicator.
     *
     * @param render the render context
     * @param player the player viewing the inventory
     */
    @Override
    protected void onRender(@NotNull RenderContext render, @NotNull Player player) {
        var pagination = paginationState.get(render);
        renderBorder(render);
        renderNavigationButtons(render, player, pagination);
        renderPageIndicator(render, player, pagination);
        onPaginatedRender(render, player);
    }

    /**
     * Hook for additional rendering after pagination chrome is placed.
     *
     * @param render the render context
     * @param player the player viewing the inventory
     */
    protected void onPaginatedRender(@NotNull RenderContext render, @NotNull Player player) {
        // Optional hook for subclasses
    }

    /**
     * Returns the pagination instance for the current context.
     *
     * @param ctx the render context
     * @return the pagination state
     */
    protected final Pagination pagination(@NotNull RenderContext ctx) {
        return paginationState.get(ctx);
    }

    // ── Internal ────────────────────────────────────────────────────────────────

    private void renderBorder(@NotNull RenderContext render) {
        var hasBorder = render.getLayoutSlots().stream()
                .anyMatch(s -> s.getCharacter() == 'X');
        if (!hasBorder) return;

        render.layoutSlot('X', createItem(fillerMaterial(), Component.empty()));
    }

    private void renderNavigationButtons(@NotNull RenderContext render,
                                         @NotNull Player player,
                                         @NotNull Pagination pagination) {
        var prevItem = createItem(Material.ARROW,
                i18nWithDefault("previous", player).build().component());
        render.layoutSlot('<', prevItem)
                .updateOnStateChange(paginationState)
                .displayIf(() -> !pagination.isLoading() && pagination.canBack())
                .onClick(pagination::back);

        var nextItem = createItem(Material.ARROW,
                i18nWithDefault("next", player).build().component());
        render.layoutSlot('>', nextItem)
                .updateOnStateChange(paginationState)
                .displayIf(() -> !pagination.isLoading() && pagination.canAdvance())
                .onClick(pagination::advance);
    }

    private void renderPageIndicator(@NotNull RenderContext render,
                                     @NotNull Player player,
                                     @NotNull Pagination pagination) {
        var hasSlot = render.getLayoutSlots().stream()
                .anyMatch(s -> s.getCharacter() == 'p');
        if (!hasSlot) return;

        if (pagination.isLoading()) {
            var loadingItem = createItem(Material.CLOCK,
                    i18nWithDefault("loading", player).build().component());
            render.layoutSlot('p', loadingItem)
                    .updateOnStateChange(paginationState);
            return;
        }

        var currentPage = pagination.currentPageIndex() + 1;
        var totalPages = pagination.lastPageIndex() + 1;

        var placeholders = Map.<String, Object>of(
                "page", currentPage,
                "total_pages", totalPages,
                "current_page", currentPage,
                "max_page", totalPages
        );

        var pageItem = createItem(Material.PAPER,
                i18nWithDefault("page.name", player).withPlaceholders(placeholders).build().component(),
                i18nWithDefault("page.lore", player).withPlaceholders(placeholders).build().children());
        render.layoutSlot('p', pageItem)
                .updateOnStateChange(paginationState);
    }
}
