/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.configs.ConfigSection;
import com.raindropcentral.rds.service.shop.ShopAdminPlayerSettingsService;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated selector for player-specific shop override editing.
 *
 * <p>The view combines online players with previously configured override targets so admins can
 * quickly open player override editors.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopAdminPlayerSelectView extends APaginatedView<ShopAdminPlayerSelectView.PlayerTargetEntry> {

    private static final String ADMIN_COMMAND_PERMISSION = "raindropshops.command.admin";

    private final State<RDS> rds = initialState("plugin");

    /**
     * Creates the paginated player selector.
     */
    public ShopAdminPlayerSelectView() {
        super(ShopAdminPlayerView.class);
    }

    /**
     * Returns the i18n key used by this view.
     *
     * @return view i18n key
     */
    @Override
    protected @NotNull String getKey() {
        return "shop_admin_player_select_ui";
    }

    /**
     * Returns the menu layout for this view.
     *
     * @return rendered menu layout
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
                "    s    ",
                " OOOOOOO ",
                " OOOOOOO ",
                " OOOOOOO ",
                " OOOOOOO ",
                "  < p >  "
        };
    }

    /**
     * Loads online and configured players for pagination.
     *
     * @param context current context
     * @return async player target list
     */
    @Override
    protected @NotNull CompletableFuture<List<PlayerTargetEntry>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        if (!this.hasAdminAccess(context.getPlayer())) {
            return CompletableFuture.completedFuture(List.of());
        }

        final RDS plugin = this.rds.get(context);
        final ShopAdminPlayerSettingsService settingsService = plugin.getShopAdminPlayerSettingsService();
        final Map<UUID, PlayerTargetEntry> targetsById = new HashMap<>();

        if (settingsService != null) {
            for (final Map.Entry<UUID, ShopAdminPlayerSettingsService.PlayerOverride> entry
                    : settingsService.getPlayerOverrides().entrySet()) {
                final UUID playerId = entry.getKey();
                final ShopAdminPlayerSettingsService.PlayerOverride override = entry.getValue();
                final String fallbackName = override.playerName() == null
                        ? playerId.toString()
                        : override.playerName();
                targetsById.put(playerId, new PlayerTargetEntry(playerId, fallbackName, false));
            }
        }

        for (final Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            targetsById.put(
                    onlinePlayer.getUniqueId(),
                    new PlayerTargetEntry(
                            onlinePlayer.getUniqueId(),
                            onlinePlayer.getName(),
                            true
                    )
            );
        }

        final List<PlayerTargetEntry> entries = new ArrayList<>(targetsById.values());
        entries.sort(Comparator.comparing(PlayerTargetEntry::displayName, String.CASE_INSENSITIVE_ORDER));
        return CompletableFuture.completedFuture(entries);
    }

    /**
     * Renders one player target card.
     *
     * @param context current context
     * @param builder slot builder
     * @param index zero-based entry index
     * @param entry current player entry
     */
    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull PlayerTargetEntry entry
    ) {
        builder.withItem(this.createPlayerEntryItem(context, entry))
                .onClick(clickContext -> clickContext.openForPlayer(
                        ShopAdminPlayerEditView.class,
                        Map.of(
                                "plugin", this.rds.get(clickContext),
                                "targetUuid", entry.playerId(),
                                "targetName", entry.displayName()
                        )
                ));
    }

    /**
     * Renders summary/empty cards.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    protected void onPaginatedRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final RDS plugin = this.rds.get(render);
        final int configuredOverrides = plugin.getShopAdminPlayerSettingsService() == null
                ? 0
                : plugin.getShopAdminPlayerSettingsService().getPlayerOverrides().size();
        final int trackedPlayers = this.getPagination(render).source() == null
                ? 0
                : this.getPagination(render).source().size();

        if (!this.hasAdminAccess(player)) {
            render.slot(22).renderWith(() -> this.createLockedItem(player));
            return;
        }

        render.layoutSlot('s', this.createSummaryItem(player, trackedPlayers, configuredOverrides));
        if (trackedPlayers < 1) {
            render.slot(22).renderWith(() -> this.createEmptyItem(player));
        }
    }

    /**
     * Cancels vanilla click handling so the menu behaves as an action UI.
     *
     * @param click click context
     */
    @Override
    public void onClick(
            final @NotNull SlotClickContext click
    ) {
        click.setCancelled(true);
    }

    private @NotNull ItemStack createPlayerEntryItem(
            final @NotNull Context context,
            final @NotNull PlayerTargetEntry entry
    ) {
        final RDS plugin = this.rds.get(context);
        final ShopAdminPlayerSettingsService settingsService = plugin.getShopAdminPlayerSettingsService();
        final ShopAdminPlayerSettingsService.PlayerOverride override = settingsService == null
                ? null
                : settingsService.getPlayerOverride(entry.playerId());
        final ConfigSection config = plugin.getDefaultConfig();
        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.playerId());
        final Player onlinePlayer = offlinePlayer.getPlayer();

        final int resolvedMaxShops = onlinePlayer == null
                ? plugin.getMaximumShops(entry.playerId(), config)
                : plugin.getMaximumShops(onlinePlayer, config);
        final String resolvedMaxShopsDisplay = resolvedMaxShops > 0
                ? Integer.toString(resolvedMaxShops)
                : this.i18n("entry.unlimited", context.getPlayer())
                        .build()
                        .getI18nVersionWrapper()
                        .asPlaceholder();
        final String overrideMaxDisplay = override != null && override.maximumShops() != null
                ? (override.maximumShops() > 0
                ? Integer.toString(override.maximumShops())
                : this.i18n("entry.unlimited", context.getPlayer()).build().getI18nVersionWrapper().asPlaceholder())
                : this.i18n("entry.inherit", context.getPlayer()).build().getI18nVersionWrapper().asPlaceholder();

        final double resolvedDiscount = onlinePlayer == null
                ? (override != null && override.discountPercent() != null ? override.discountPercent() : 0.0D)
                : plugin.getShopDiscountPercent(onlinePlayer);
        final String overrideDiscountDisplay = override != null && override.discountPercent() != null
                ? formatPercent(override.discountPercent())
                : this.i18n("entry.inherit", context.getPlayer()).build().getI18nVersionWrapper().asPlaceholder();

        return UnifiedBuilderFactory.item(entry.online() ? Material.PLAYER_HEAD : Material.SKELETON_SKULL)
                .setName(this.i18n("entry.name", context.getPlayer())
                        .withPlaceholders(Map.of(
                                "player_name", entry.displayName(),
                                "status", this.resolveOnlineStatusPlaceholder(context.getPlayer(), entry.online())
                        ))
                        .build()
                        .component())
                .setLore(this.i18n("entry.lore", context.getPlayer())
                        .withPlaceholders(Map.of(
                                "player_name", entry.displayName(),
                                "player_uuid", entry.playerId().toString(),
                                "override_max_shops", overrideMaxDisplay,
                                "effective_max_shops", resolvedMaxShopsDisplay,
                                "override_discount", overrideDiscountDisplay,
                                "effective_discount", formatPercent(resolvedDiscount)
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player,
            final int trackedPlayers,
            final int configuredOverrides
    ) {
        return UnifiedBuilderFactory.item(Material.BOOK)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n("summary.lore", player)
                        .withPlaceholders(Map.of(
                                "tracked_players", trackedPlayers,
                                "configured_overrides", configuredOverrides
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createEmptyItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("feedback.empty.name", player).build().component())
                .setLore(this.i18n("feedback.empty.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createLockedItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("feedback.access_denied.name", player).build().component())
                .setLore(this.i18n("feedback.access_denied.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull String resolveOnlineStatusPlaceholder(
            final @NotNull Player viewer,
            final boolean online
    ) {
        final String key = online ? "entry.status.online" : "entry.status.offline";
        return this.i18n(key, viewer)
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
    }

    private static @NotNull String formatPercent(
            final double value
    ) {
        return String.format(Locale.US, "%.2f%%", value);
    }

    private boolean hasAdminAccess(
            final @NotNull Player player
    ) {
        return player.isOp() || player.hasPermission(ADMIN_COMMAND_PERMISSION);
    }

    /**
     * Player target entry for the override editor list.
     *
     * @param playerId player UUID
     * @param displayName display name shown in the UI
     * @param online whether the player is currently online
     */
    protected record PlayerTargetEntry(
            @NotNull UUID playerId,
            @NotNull String displayName,
            boolean online
    ) {
    }
}
