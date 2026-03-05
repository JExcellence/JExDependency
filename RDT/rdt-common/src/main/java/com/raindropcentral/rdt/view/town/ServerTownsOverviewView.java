package com.raindropcentral.rdt.view.town;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ServerTownsOverviewView extends APaginatedView<RTown> {

    private final State<RDT> rdt = initialState("plugin");

    @Override
    protected String getKey() {
        return "server_town_overview";
    }

    @Override
    protected int getSize() {
        return 54;
    }

    @Override
    protected CompletableFuture<List<RTown>> getAsyncPaginationSource(final @NotNull Context context) {
        return this.rdt.get(context).getTownRepository().findAllByAttributesAsync(
                Map.of("active", true)
        );
    }

    @Override
    protected void renderEntry(@NotNull Context context, @NotNull BukkitItemComponentBuilder builder, int index, @NonNull RTown entry) {
        var player = context.getPlayer();
        var townName = entry.getTownName();

        builder.withItem(UnifiedBuilderFactory.item(Material.EMERALD)
                .setName(this.i18n(townName, player)
                        .build().component())
                .setLore(this.i18n("town.view_town.lore", player)
                        .build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build()
        ).onClick(ctx -> ctx.openForPlayer(
                TownOverviewView.class
        ));
    }

    @Override
    protected void onPaginatedRender(@NotNull RenderContext render, @NotNull Player player) {

    }

}
