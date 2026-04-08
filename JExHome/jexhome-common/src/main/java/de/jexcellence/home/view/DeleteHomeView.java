package de.jexcellence.home.view;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import com.raindropcentral.rplatform.view.ConfirmationView;
import de.jexcellence.home.JExHome;
import de.jexcellence.home.database.entity.Home;
import de.jexcellence.home.factory.HomeFactory;
import de.jexcellence.home.utility.heads.House;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * View for selecting a home to delete.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class DeleteHomeView extends APaginatedView<Home> {

    private final State<JExHome> jexHome = initialState("plugin");

    public DeleteHomeView() {
        super();
    }

    @Override
    protected @NotNull String getKey() {
        return "delete_home_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "         ",
            " iiiiiii ",
            " iiiiiii ",
            " iiiiiii ",
            "b   p   n"
        };
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(@NotNull OpenContext context) {
        return Map.of();
    }

    @Override
    protected @NotNull CompletableFuture<List<Home>> getAsyncPaginationSource(@NotNull Context context) {
        var player = context.getPlayer();
        try {
            var factory = HomeFactory.getInstance();
            return factory.getPlayerHomes(player.getUniqueId());
        } catch (Exception e) {
            return CompletableFuture.completedFuture(List.of());
        }
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
        
        var builtItem = UnifiedBuilderFactory.item(houseHead)
            .setName(this.i18n("home.name", player)
                .withPlaceholder("home_name", home.getHomeName())
                .build().component())
            .setLore(this.i18n("home.lore", player)
                .withPlaceholder("world", home.getWorldName())
                .withPlaceholder("location", home.getFormattedLocation())
                .build().children())
            .build();
        
        itemBuilder
            .withItem(builtItem)
            .onClick(clickContext -> {
                openDeleteConfirmation(clickContext, home);
            });
    }

    private void openDeleteConfirmation(@NotNull Context context, @NotNull Home home) {
        var player = context.getPlayer();
        var plugin = jexHome.get(context);
        
        Map<String, Object> initialData = new HashMap<>();
        initialData.put("home", home);
        initialData.put("home_name", home.getHomeName());
        initialData.put("world", home.getWorldName());
        initialData.put("location", home.getFormattedLocation());
        initialData.put("plugin", plugin);
        
        new ConfirmationView.Builder()
            .withKey("delete_home_confirm")
            .withInitialData(initialData)
            .withCallback(confirmed -> {
                if (confirmed) {
                    deleteHome(player, home, plugin);
                }
            })
            .withParentView(DeleteHomeView.class)
            .openFor(context, player);
    }

    private void deleteHome(@NotNull Player player, @NotNull Home home, @NotNull JExHome plugin) {
        try {
            var factory = HomeFactory.getInstance();
            factory.deleteHome(player, home.getHomeName())
                .thenAccept(deleted -> {
                    if (deleted) {
                        new I18n.Builder("delhome.deleted", player)
                            .withPlaceholder("home_name", home.getHomeName())
                            .includePrefix()
                            .build()
                            .sendMessage();
                    } else {
                        new I18n.Builder("delhome.does_not_exist", player)
                            .withPlaceholder("home_name", home.getHomeName())
                            .includePrefix()
                            .build()
                            .sendMessage();
                    }
                });
        } catch (Exception e) {
            new I18n.Builder("home.error.internal", player)
                .includePrefix()
                .build()
                .sendMessage();
        }
    }

    @Override
    protected void onPaginatedRender(@NotNull RenderContext render, @NotNull Player player) {
        // No additional rendering needed
    }
}
