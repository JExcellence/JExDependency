package de.jexcellence.oneblock.view.island;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.database.entity.oneblock.EOneblockIslandRole;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIslandBan;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BannedPlayersView extends APaginatedView<OneblockIslandBan> {
    
    private final State<JExOneblock> plugin = initialState("plugin");
    private final State<OneblockIsland> island = initialState("island");
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd");
    
    @Override
    protected String getKey() {
        return "banned_players";
    }
    
    @Override
    protected CompletableFuture<List<OneblockIslandBan>> getAsyncPaginationSource(@NotNull Context context) {
        var islandData = island.get(context);
        var banService = plugin.get(context).getIslandBanService();
        
        return banService.getActiveBans(islandData).thenApply(activeBans -> {
            activeBans.sort(Comparator
                .comparing((OneblockIslandBan ban) -> ban.getExpiresAt() == null ? 0 : 1)
                .thenComparing(OneblockIslandBan::getBannedAt, Comparator.reverseOrder()));
            return activeBans;
        });
    }
    
    @Override
    protected void renderEntry(@NotNull Context context, @NotNull BukkitItemComponentBuilder builder, 
                              int index, @NotNull OneblockIslandBan ban) {
        var player = context.getPlayer();
        var islandData = island.get(context);
        var offlinePlayer = Bukkit.getOfflinePlayer(ban.getBannedPlayerUuid());
        var playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : i18n("common.unknown_player", player).build().component().toString();
        
        var isPermanent = ban.getExpiresAt() == null;
        var canUnban = canUnbanPlayer(player, islandData);
        
        var material = isPermanent ? Material.BARRIER : Material.CLOCK;
        var status = isPermanent ? 
            i18n("banned.players.entry.permanent_status", player).build().component().toString() : 
            formatTimeRemaining(ban.getExpiresAt(), player);
        
        var itemStack = new ItemStack(material);
        
        var displayName = isPermanent ? 
            "<red>" + playerName + " <gray>(" + status + ")" :
            "<gold>" + playerName + " <gray>(" + status + ")";
            
        var meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(displayName));
            meta.lore(buildBanLore(player, ban, canUnban));
            itemStack.setItemMeta(meta);
        }
        
        builder.withItem(itemStack)
            .onClick(ctx -> {
                if (canUnban) {
                    showUnbanConfirmation((RenderContext) context, player, islandData, ban);
                } else {
                    showBanDetails(player, ban);
                }
            });
    }
    
    @Override
    protected void onPaginatedRender(@NotNull RenderContext render, @NotNull Player player) {
        renderActionButtons(render, player);
        renderBorder(render);
    }
    
    private void renderActionButtons(@NotNull RenderContext render, @NotNull Player player) {
        var islandData = island.get(render);
        var canUnban = canUnbanPlayer(player, islandData);
        
        if (canUnban) {
            render.layoutSlot('C', UnifiedBuilderFactory
                .item(Material.TNT)
                .setName(i18n("banned.players.clear_all.name", player).build().component())
                .setLore(i18n("banned.players.clear_all.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build()
            ).onClick(ctx -> {
                var pagination = getPagination(render);
                var banCount = pagination.source() != null ? pagination.source().size() : 0;
                showUnbanAllConfirmation(player, islandData, banCount);
            });
        }
        
        if (canUnban) {
            render.layoutSlot('U', UnifiedBuilderFactory
                .item(Material.HOPPER)
                .setName(i18n("banned.players.unban_expired.name", player).build().component())
                .setLore(i18n("banned.players.unban_expired.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build()
            ).onClick(ctx -> {
                unbanExpiredPlayers(render, player, islandData);
            });
        }
        
        if (canUnban) {
            render.layoutSlot('A', UnifiedBuilderFactory
                .item(Material.IRON_BARS)
                .setName(i18n("banned.players.add_ban.name", player).build().component())
                .setLore(i18n("banned.players.add_ban.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build()
            ).onClick(ctx -> {
                i18n("banned.players.add_ban.instructions", player).includePrefix().build().sendMessage();
                player.closeInventory();
            });
        }
        
        render.layoutSlot('B', UnifiedBuilderFactory
            .item(Material.BOOK)
            .setName(i18n("banned.players.history.name", player).build().component())
            .setLore(i18n("banned.players.history.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            i18n("banned.players.history.coming_soon", player).includePrefix().build().sendMessage();
        });
    }
    
    private List<Component> buildBanLore(@NotNull Player player, @NotNull OneblockIslandBan ban, boolean canUnban) {
        var offlinePlayer = Bukkit.getOfflinePlayer(ban.getBannedPlayerUuid());
        var playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : i18n("common.unknown_player", player).build().component().toString();
        var bannerName = i18n("common.console", player).build().component().toString();
        if (ban.getBannedBy() != null) {
            var bannerUuid = ban.getBannedBy().getUuid();
            var banner = Bukkit.getOfflinePlayer(bannerUuid);
            bannerName = banner.getName() != null ? banner.getName() : i18n("common.unknown_player", player).build().component().toString();
        }
        
        var lore = new ArrayList<Component>();
        lore.add(i18n("banned.players.entry.player", player).withPlaceholder("player", playerName).build().component());
        lore.add(i18n("banned.players.entry.reason", player).withPlaceholder("reason", ban.getReason() != null ? ban.getReason() : i18n("common.no_reason", player).build().component().toString()).build().component());
        lore.add(i18n("banned.players.entry.banned_by", player).withPlaceholder("banner", bannerName).build().component());
        lore.add(i18n("banned.players.entry.banned_at", player).withPlaceholder("date", ban.getBannedAt().format(DATE_FORMAT)).build().component());
        
        if (ban.getExpiresAt() != null) {
            var timeRemaining = formatTimeRemaining(ban.getExpiresAt(), player);
            lore.add(i18n("banned.players.entry.expires", player).withPlaceholder("date", ban.getExpiresAt().format(DATE_FORMAT)).build().component());
            lore.add(i18n("banned.players.entry.time_remaining", player).withPlaceholder("time", timeRemaining).build().component());
        } else {
            lore.add(i18n("banned.players.entry.permanent", player).build().component());
        }
        
        lore.add(Component.text(""));
        if (canUnban) {
            lore.add(i18n("banned.players.entry.click_unban", player).build().component());
        } else {
            lore.add(i18n("banned.players.entry.click_details", player).build().component());
        }
        
        return lore;
    }
    
    private void renderBorder(@NotNull RenderContext render) {
        render.layoutSlot('X', UnifiedBuilderFactory
            .item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(Component.text(""))
            .build()
        );
    }
    private boolean canViewBans(@NotNull Player player, @NotNull OneblockIsland island) {
        if (island.getOwnerUuid().equals(player.getUniqueId())) {
            return true;
        }
        
        return island.isMember(island.getOwner()) || island.isOwner(island.getOwner());
    }
    
    private boolean canUnbanPlayer(@NotNull Player player, @NotNull OneblockIsland island) {
        if (island.getOwnerUuid().equals(player.getUniqueId())) {
            return true;
        }
        
        return false;
    }
    
    private String formatTimeRemaining(@NotNull LocalDateTime expiresAt, @NotNull Player player) {
        var now = LocalDateTime.now();
        if (expiresAt.isBefore(now)) {
            return i18n("common.expired", player).build().component().toString();
        }
        
        var days = ChronoUnit.DAYS.between(now, expiresAt);
        var hours = ChronoUnit.HOURS.between(now, expiresAt) % 24;
        var minutes = ChronoUnit.MINUTES.between(now, expiresAt) % 60;
        
        if (days > 0) {
            return days + "d " + hours + "h";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
    
    private void showBanDetails(@NotNull Player player, @NotNull OneblockIslandBan ban) {
        var offlinePlayer = Bukkit.getOfflinePlayer(ban.getBannedPlayerUuid());
        var playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : i18n("common.unknown_player", player).build().component().toString();
        
        i18n("banned.players.details.header", player)
            .withPlaceholder("player", playerName)
            .includePrefix()
            .build().sendMessage();
        
        i18n("banned.players.details.info", player)
            .withPlaceholder("reason", ban.getReason() != null ? ban.getReason() : i18n("common.no_reason", player).build().component().toString())
            .withPlaceholder("banned_at", ban.getBannedAt().format(DATE_FORMAT))
            .build().sendMessage();
    }
    
    private void showUnbanConfirmation(@NotNull RenderContext render, @NotNull Player player, @NotNull OneblockIsland island, @NotNull OneblockIslandBan ban) {
        var offlinePlayer = Bukkit.getOfflinePlayer(ban.getBannedPlayerUuid());
        var playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : i18n("common.unknown_player", player).build().component().toString();
        
        i18n("banned.players.unban.confirm.header", player)
            .withPlaceholder("player", playerName)
            .includePrefix()
            .build().sendMessage();
        
        i18n("banned.players.unban.confirm.details", player)
            .withPlaceholder("reason", ban.getReason() != null ? ban.getReason() : i18n("common.no_reason", player).build().component().toString())
            .withPlaceholder("banned_at", ban.getBannedAt().format(DATE_FORMAT))
            .build().sendMessage();
        
        i18n("banned.players.unban.confirm.prompt", player).build().sendMessage();
        
        performUnban(render, player, island, ban);
    }
    
    private void showUnbanAllConfirmation(@NotNull Player player, @NotNull OneblockIsland island, int banCount) {
        i18n("banned.players.unban_all.confirm.header", player)
            .withPlaceholder("count", banCount)
            .includePrefix()
            .build().sendMessage();
        
        i18n("banned.players.unban_all.confirm.warning", player).build().sendMessage();
        i18n("banned.players.unban_all.confirm.prompt", player).build().sendMessage();
    }
    
    private void unbanExpiredPlayers(@NotNull RenderContext render, @NotNull Player player, @NotNull OneblockIsland island) {
        var banService = plugin.get(render).getIslandBanService();
        
        CompletableFuture.supplyAsync(() -> {
            banService.cleanupExpiredBans();
            return 0;
        }).thenAccept(count -> {
            Bukkit.getScheduler().runTask((org.bukkit.plugin.Plugin) plugin.get(render), () -> {
                i18n("banned.players.unban_expired.success", player)
                    .withPlaceholder("count", count)
                    .includePrefix()
                    .build().sendMessage();
                
                render.update();
            });
        });
    }
    
    private void performUnban(@NotNull RenderContext render, @NotNull Player player, @NotNull OneblockIsland island, @NotNull OneblockIslandBan ban) {
        var banService = plugin.get(render).getIslandBanService();
        var offlinePlayer = Bukkit.getOfflinePlayer(ban.getBannedPlayerUuid());
        var playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : i18n("common.unknown_player", player).build().component().toString();
        
        try {
            var bannedPlayer = ban.getBannedPlayer();
            var unbannedByPlayer = new de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer(player);
            
            banService.unbanPlayer(island, bannedPlayer, unbannedByPlayer);
            
            i18n("banned.players.unban.success", player)
                .withPlaceholder("player", playerName)
                .includePrefix()
                .build().sendMessage();
            
            var targetPlayer = Bukkit.getPlayer(ban.getBannedPlayerUuid());
            if (targetPlayer != null) {
                i18n("banned.players.unban.notify_player", targetPlayer)
                    .withPlaceholder("island", island.getIslandName())
                    .includePrefix()
                    .build().sendMessage();
            }
            
            render.update();
            
        } catch (Exception e) {
            i18n("banned.players.unban.error", player)
                .withPlaceholder("error", e.getMessage())
                .includePrefix()
                .build().sendMessage();
        }
    }
}
