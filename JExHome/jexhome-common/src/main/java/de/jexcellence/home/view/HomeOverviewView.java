package de.jexcellence.home.view;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import de.jexcellence.home.JExHome;
import de.jexcellence.home.database.entity.Home;
import de.jexcellence.home.factory.HomeFactory;
import de.jexcellence.home.utility.heads.House;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated view for displaying player homes.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class HomeOverviewView extends APaginatedView<Home> {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private final State<JExHome> jexHome = initialState("plugin");

    private int cachedHomeCount = 0;
    private int cachedMaxHomes = 0;

    @Override
    protected @NotNull String getKey() {
        return "home_overview_ui";
    }

    @Override
    protected @NotNull String[] getLayout() {
        return new String[]{
            "XXXXXXXXX",
            "XOOOOOOOX",
            "XOOOOOOOX",
            "XOOOOOOOX",
            "XXXXXXXXX",
            "   <p>  C"
        };
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(@NotNull OpenContext context) {
        return Map.of(
            "home_count", cachedHomeCount,
            "max_homes", cachedMaxHomes
        );
    }

    @Override
    protected @NotNull CompletableFuture<List<Home>> getAsyncPaginationSource(@NotNull Context context) {
        var player = context.getPlayer();
        var plugin = jexHome.get(context);

        // Update max homes
        cachedMaxHomes = plugin.getHomeService().getMaxHomesForPlayer(player);

        return plugin.getHomeService().getPlayerHomes(player.getUniqueId())
            .thenApply(homes -> {
                // Update home count
                cachedHomeCount = homes.size();

                // Sort by name by default
                return homes.stream()
                    .sorted(Comparator.comparing(Home::getHomeName))
                    .toList();
            });
    }

    @Override
    protected void renderEntry(
        @NotNull Context context,
        @NotNull BukkitItemComponentBuilder itemBuilder,
        int index,
        @NotNull Home home
    ) {
        var player = context.getPlayer();

        // Use House head for home items
        var houseHead = new House().getHead(player);

        var itemBuilder2 = UnifiedBuilderFactory.item(houseHead)
            .setName(this.i18n("home.name", player)
                .withPlaceholder("home_name", home.getHomeName())
                .build().component())
            .setLore(this.i18n("home.lore", player)
                .withPlaceholders(Map.of(
                    "world", home.getWorldName(),
                    "location", home.getFormattedLocation(),
                    "visit_count", String.valueOf(home.getVisitCount()),
                    "last_visited", formatLastVisited(home.getLastVisited())
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        if (home.isFavorite()) {
            itemBuilder2.setGlowing(true);
        }

        itemBuilder
            .withItem(itemBuilder2.build())
            .onClick(clickContext -> {
                if (clickContext.isLeftClick()) {
                    teleportToHome(clickContext, home);
                    clickContext.closeForPlayer();
                }
                // Right-click edit disabled for now - no action taken
            });
    }

    @Override
    protected void onPaginatedRender(@NotNull RenderContext render, @NotNull Player player) {
        renderCreateButton(render, player);
    }

    private void renderCreateButton(RenderContext render, Player player) {
        render.layoutSlot('C', UnifiedBuilderFactory.item(new House().getHead(player))
            .setName(this.i18n("create.title", player).build().component())
            .setLore(this.i18n("create.lore", player).build().children())
            .setGlowing(true)
            .build()
        ).onClick(ctx -> {
            ctx.openForPlayer(
                SetHomeAnvilView.class,
                Map.of("plugin", this.jexHome.get(ctx))
            );
        });
    }

    private void teleportToHome(@NotNull Context context, @NotNull Home home) {
        var player = context.getPlayer();
        var plugin = jexHome.get(context);

        if (home.getLocation() == null || home.getLocation().getWorld() == null) {
            this.i18n("home.world_not_loaded", player)
                .withPlaceholder("world", home.getWorldName())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        try {
            var factory = HomeFactory.getInstance();
            factory.teleportToHome(player, home.getHomeName(), () -> {
                this.i18n("home.teleported", player)
                    .withPlaceholder("home_name", home.getHomeName())
                    .includePrefix()
                    .build()
                    .sendMessage();
            });
        } catch (Exception e) {
            // Fallback to direct teleport
            plugin.getPlatform().getScheduler().runSync(() -> {
                player.teleport(home.getLocation());
                home.recordVisit();
                plugin.getHomeService().updateHome(home);
                this.i18n("home.teleported", player)
                    .withPlaceholder("home_name", home.getHomeName())
                    .includePrefix()
                    .build()
                    .sendMessage();
            });
        }
    }

    private String formatLastVisited(LocalDateTime lastVisited) {
        if (lastVisited == null) return "Never";
        return lastVisited.format(DATE_FORMAT);
    }

    @Override
    public void onResume(
        final @NotNull Context originContext,
        final @NotNull Context targetContext
    ) {
        // Re-open the view to refresh the pagination with fresh data
        var data = new HashMap<String, Object>();
        data.put("plugin", jexHome.get(targetContext));
        targetContext.openForPlayer(HomeOverviewView.class, data);
    }
}
