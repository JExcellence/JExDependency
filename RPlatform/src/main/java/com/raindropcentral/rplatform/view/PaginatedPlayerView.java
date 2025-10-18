package com.raindropcentral.rplatform.view;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.component.Pagination;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PaginatedPlayerView extends APaginatedView<OfflinePlayer> {
    
    @Override
    protected String getKey() {
        
        return "paginated_player_ui";
    }
    
    @Override
    protected void handleBackButtonClick(final @NotNull SlotClickContext clickContext) {
        clickContext.back(clickContext.getInitialData());
    }
    
    @Override
    protected CompletableFuture<List<OfflinePlayer>> getAsyncPaginationSource(
        final @NotNull Context context
    ) {
        return CompletableFuture.supplyAsync(() -> Arrays.stream(Bukkit.getOfflinePlayers()).toList());
    }
    
    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull OfflinePlayer offlinePlayer
    ) {
        builder.withItem(
                   UnifiedBuilderFactory
                       .head()
                       .setPlayerHead(offlinePlayer)
                       .setName(
                           this.i18n("player_entry.name", context.getPlayer()
                               ).with(
                                   "player_name",
                                   offlinePlayer.getName()
                               )
                               .build().component()
                       )
                       .build()
               )
               .onClick(clickContext -> {
                            Map<String, Object> initialData = new HashMap<>((Map<String, Object>) clickContext.getInitialData());
                            initialData.put(
                                "target",
                                Optional.of(offlinePlayer)
                            );
                            
                            clickContext.back(
                                initialData
                            );
                        }
               );
    }
    
    @Override
    protected void onPaginatedRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        final Pagination pagination = this.getPagination(render);
    }
}