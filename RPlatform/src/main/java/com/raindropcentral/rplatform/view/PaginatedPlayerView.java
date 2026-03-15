package com.raindropcentral.rplatform.view;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import me.devnatan.inventoryframework.RootView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.component.Pagination;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Concrete {@link APaginatedView} implementation that presents a searchable roster of offline.
 * players using player head avatars for selection.
 *
 * <p>The view relies on translation keys rooted at {@code paginated_player_ui} for row labelling
 * and leverages the shared head utilities to render individual player identities.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class PaginatedPlayerView extends APaginatedView<OfflinePlayer> {

    final State<Object> initialData = initialState("initialData");
    final State<Class<? extends RootView>> parentClazz = initialState("parentClazz");

    /**
     * Provides the base translation key namespace for player selection strings.
     *
     * @return the translation key prefix for this view
     */
    @Override
    protected String getKey() {

        return "paginated_player_ui";
    }

    /**
     * Overrides the default back handling to pop the Inventory Framework context stack while.
     * preserving initial data such as previously selected players.
     *
     * @param clickContext the click context supplied when the return head is activated
     */
    @Override
    protected void handleBackButtonClick(final @NotNull SlotClickContext clickContext) {
        clickContext.back(clickContext.getInitialData());
    }

    /**
     * Streams Bukkit's offline player cache asynchronously to populate the pagination source.
     *
     * @param context the inventory framework context associated with the fetch
     * @return a future supplying the full offline player list
     */
    @Override
    protected CompletableFuture<List<OfflinePlayer>> getAsyncPaginationSource(
        final @NotNull Context context
    ) {
        return CompletableFuture.supplyAsync(() -> Arrays.stream(Bukkit.getOfflinePlayers()).toList());
    }

    /**
     * Renders each offline player as a localized head item with click-through behaviour that stores.
     * the selected player back into the view's initial data payload.
     *
     * @param context the render context providing player information
     * @param builder the item builder used to define slot visuals and actions
     * @param index the zero-based index of the entry within the pagination
     * @param offlinePlayer the offline player represented by the entry
     */
    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull OfflinePlayer offlinePlayer
    ) {
        builder.withItem(
                   UnifiedBuilderFactory
                       .unifiedHead()
                       .setPlayerHead(offlinePlayer)
                       .setDisplayName(
                           (Component) this.i18n("player_entry.name", context.getPlayer()
                               ).withPlaceholder(
                                   "player_name",
                                   offlinePlayer.getName()
                               )
                               .build().component()
                       )
                       .build()
               )
               .onClick(clickContext -> {
                   Map<String, Object> initialData = new HashMap<>();
                   initialData.putAll(((Map<String, Object>) this.initialData.get(context)));
                   initialData.put("target", Optional.of(offlinePlayer));
                   clickContext.openForPlayer(this.parentClazz.get(context), initialData);
               });
    }

    /**
     * Invoked after pagination chrome renders; currently a no-op hook reserved for future.
     * embellishments such as filtering controls.
     *
     * @param render the render context for slot management
     * @param player the viewer interacting with the roster
     */
    @Override
    protected void onPaginatedRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        final Pagination pagination = this.getPagination(render);
    }
}
