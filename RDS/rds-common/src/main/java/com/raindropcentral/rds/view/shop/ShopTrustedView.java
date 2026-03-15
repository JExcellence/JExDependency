package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.database.entity.ShopTrustStatus;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Renders the shop trusted inventory view.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopTrustedView extends APaginatedView<ShopTrustedView.TrustedPlayerEntry> {

    private final State<RDS> rds = initialState("plugin");
    private final State<Location> shopLocation = initialState("shopLocation");

    /**
     * Creates a new shop trusted view.
     */
    public ShopTrustedView() {
        super(ShopOverviewView.class);
    }

    @Override
    protected String getKey() {
        return "shop_trusted_ui";
    }

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

    @Override
    protected Map<String, Object> getTitlePlaceholders(
            final @NotNull OpenContext context
    ) {
        final Shop shop = this.getCurrentShop(context);
        final Location location = this.shopLocation.get(context);

        return Map.of(
                "location", this.formatLocation(location),
                "owner", shop == null ? "Unknown" : this.getOwnerName(shop)
        );
    }

    @Override
    protected CompletableFuture<List<TrustedPlayerEntry>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        final Shop shop = this.getCurrentShop(context);
        if (shop == null || !shop.isOwner(context.getPlayer().getUniqueId())) {
            return CompletableFuture.completedFuture(List.of());
        }

        final List<TrustedPlayerEntry> entries = new ArrayList<>();
        for (final OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer == null || offlinePlayer.getUniqueId() == null) {
                continue;
            }

            if (shop.isOwner(offlinePlayer.getUniqueId())) {
                continue;
            }

            entries.add(new TrustedPlayerEntry(
                    offlinePlayer.getUniqueId(),
                    this.getPlayerName(offlinePlayer),
                    shop.getTrustStatus(offlinePlayer.getUniqueId())
            ));
        }

        entries.sort(Comparator.comparing(TrustedPlayerEntry::playerName, String.CASE_INSENSITIVE_ORDER));
        return CompletableFuture.completedFuture(entries);
    }

    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull TrustedPlayerEntry entry
    ) {
        builder.withItem(this.createEntryItem(context.getPlayer(), entry))
                .onClick(clickContext -> this.handleEntryClick(clickContext, entry));
    }

    @Override
    protected void onPaginatedRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final Shop shop = this.getCurrentShop(render);

        if (shop == null) {
            render.slot(4).renderWith(() -> this.createMissingShopItem(player));
            return;
        }

        if (!shop.isOwner(player.getUniqueId())) {
            render.slot(4).renderWith(() -> this.createLockedItem(player));
            return;
        }

        render.layoutSlot(
                's',
                this.createSummaryItem(player, shop)
        );

        if (Bukkit.getOfflinePlayers().length <= 1) {
            render.slot(4).renderWith(() -> this.createEmptyItem(player));
        }
    }

    /**
     * Executes onClick.
     */
    @Override
    public void onClick(
            final @NotNull SlotClickContext click
    ) {
        click.setCancelled(true);
    }

    private void handleEntryClick(
            final @NotNull SlotClickContext clickContext,
            final @NotNull TrustedPlayerEntry entry
    ) {
        final Shop shop = this.getCurrentShop(clickContext);
        if (shop == null) {
            this.i18n("feedback.shop_missing.message", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        if (!shop.isOwner(clickContext.getPlayer().getUniqueId())) {
            this.i18n("feedback.not_owner", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        final ShopTrustStatus updatedStatus = shop.getTrustStatus(entry.playerId()).next();
        shop.setTrustStatus(entry.playerId(), updatedStatus);
        this.rds.get(clickContext).getShopRepository().update(shop);

        this.i18n("feedback.updated." + updatedStatus.name().toLowerCase(Locale.ROOT), clickContext.getPlayer())
                .withPlaceholder("player", entry.playerName())
                .includePrefix()
                .build()
                .sendMessage();

        this.openFreshView(clickContext);
    }

    private void openFreshView(
            final @NotNull Context context
    ) {
        context.openForPlayer(
                ShopTrustedView.class,
                Map.of(
                        "plugin", this.rds.get(context),
                        "shopLocation", this.shopLocation.get(context)
                )
        );
    }

    private Shop getCurrentShop(
            final @NotNull Context context
    ) {
        return this.rds.get(context).getShopRepository().findByLocation(this.shopLocation.get(context));
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player,
            final @NotNull Shop shop
    ) {
        return UnifiedBuilderFactory.item(Material.BOOKSHELF)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n("summary.lore", player)
                        .withPlaceholders(Map.of(
                                "owner", this.getOwnerName(shop),
                                "location", this.formatLocation(shop.getShopLocation()),
                                "associate_count", shop.getTrustedPlayerCount(ShopTrustStatus.ASSOCIATE),
                                "trusted_count", shop.getTrustedPlayerCount(ShopTrustStatus.TRUSTED)
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createEntryItem(
            final @NotNull Player player,
            final @NotNull TrustedPlayerEntry entry
    ) {
        final String statusKey = entry.status().name().toLowerCase(Locale.ROOT);
        return UnifiedBuilderFactory.item(this.resolveMaterial(entry.status()))
                .setName(this.i18n("entry." + statusKey + ".name", player)
                        .withPlaceholder("player", entry.playerName())
                        .build()
                        .component())
                .setLore(this.i18n("entry." + statusKey + ".lore", player)
                        .withPlaceholder("player", entry.playerName())
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull Material resolveMaterial(
            final @NotNull ShopTrustStatus status
    ) {
        return switch (status) {
            case PUBLIC -> Material.GRAY_DYE;
            case ASSOCIATE -> Material.YELLOW_DYE;
            case TRUSTED -> Material.LIME_DYE;
        };
    }

    private @NotNull ItemStack createLockedItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("feedback.locked.name", player).build().component())
                .setLore(this.i18n("feedback.locked.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createMissingShopItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("feedback.shop_missing.name", player).build().component())
                .setLore(this.i18n("feedback.shop_missing.lore", player).build().children())
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

    private @NotNull String getPlayerName(
            final @NotNull OfflinePlayer player
    ) {
        return player.getName() == null || player.getName().isBlank()
                ? player.getUniqueId().toString()
                : player.getName();
    }

    private @NotNull String formatLocation(
            final @NotNull Location location
    ) {
        return "("
                + location.getBlockX() + ", "
                + location.getBlockY() + ", "
                + location.getBlockZ() + ")";
    }

    private @NotNull String getOwnerName(
            final @NotNull Shop shop
    ) {
        final String ownerName = Bukkit.getOfflinePlayer(shop.getOwner()).getName();
        return ownerName == null ? shop.getOwner().toString() : ownerName;
    }

    /**
     * Represents trusted player entry.
     *
     * @param playerId player identifier to evaluate
     * @param playerName player name
     * @param status status
     */
    public record TrustedPlayerEntry(
            @NotNull UUID playerId,
            @NotNull String playerName,
            @NotNull ShopTrustStatus status
    ) {
    }
}
