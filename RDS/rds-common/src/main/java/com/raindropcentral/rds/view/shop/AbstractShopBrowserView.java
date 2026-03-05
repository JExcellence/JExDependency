package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.component.Pagination;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shared paginated shop-browser implementation for the directory and filtered search results.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
abstract class AbstractShopBrowserView extends APaginatedView<ShopBrowserSupport.ShopBrowserEntry> {

    protected final State<RDS> rds = initialState("plugin");

    protected AbstractShopBrowserView(
            final @Nullable Class<? extends View> parentClass
    ) {
        super(parentClass);
    }

    @Override
    protected final String[] getLayout() {
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
    protected final void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull ShopBrowserSupport.ShopBrowserEntry entry
    ) {
        builder.withItem(this.createEntryItem(context, context.getPlayer(), entry))
                .onClick(clickContext -> this.handleShopClick(clickContext, entry));
    }

    @Override
    protected final void onPaginatedRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final State<Pagination> paginationState = this.findPaginationState();
        if (paginationState == null) {
            render.slot(4).renderWith(() -> this.createHeaderItem(render, player));
            return;
        }

        render.slot(4)
                .renderWith(() -> this.createHeaderItem(render, player))
                .updateOnStateChange(paginationState);
    }

    /**
     * Cancels vanilla inventory interaction so view clicks are handled exclusively by the GUI.
     *
     * @param click click context for the active inventory interaction
     */
    @Override
    public void onClick(
            final @NotNull SlotClickContext click
    ) {
        click.setCancelled(true);
    }

    protected abstract @NotNull ItemStack createEntryItem(
            final @NotNull Context context,
            final @NotNull Player player,
            final @NotNull ShopBrowserSupport.ShopBrowserEntry entry
    );

    protected abstract @NotNull ItemStack createHeaderItem(
            final @NotNull RenderContext render,
            final @NotNull Player player
    );

    protected abstract @NotNull Map<String, Object> createViewData(
            final @NotNull Context context
    );

    protected abstract @NotNull Class<? extends View> getCurrentViewClass();

    protected final @NotNull List<ShopBrowserSupport.ShopBrowserEntry> getEntries(
            final @NotNull Pagination pagination
    ) {
        final List<ShopBrowserSupport.ShopBrowserEntry> entries = new ArrayList<>();
        if (pagination.source() == null) {
            return entries;
        }

        for (final Object sourceEntry : pagination.source()) {
            if (sourceEntry instanceof ShopBrowserSupport.ShopBrowserEntry browserEntry) {
                entries.add(browserEntry);
            }
        }

        return entries;
    }

    protected final @NotNull ItemStack createMissingShopItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("feedback.shop_missing.name", player).build().component())
                .setLore(this.i18n("feedback.shop_missing.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    protected final @NotNull ItemStack createEmptyItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("feedback.empty.name", player).build().component())
                .setLore(this.i18n("feedback.empty.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    protected final @NotNull RDS getPlugin(
            final @NotNull Context context
    ) {
        return this.rds.get(context);
    }

    @SuppressWarnings("unchecked")
    private @Nullable State<Pagination> findPaginationState() {
        try {
            final Field paginationField = APaginatedView.class.getDeclaredField("pagination");
            paginationField.setAccessible(true);

            final Object paginationState = paginationField.get(this);
            return paginationState instanceof State<?> state
                    ? (State<Pagination>) state
                    : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private void handleShopClick(
            final @NotNull SlotClickContext clickContext,
            final @NotNull ShopBrowserSupport.ShopBrowserEntry entry
    ) {
        final Shop shop = this.getPlugin(clickContext).getShopRepository().findByLocation(entry.shopLocation());
        if (shop == null) {
            this.i18n("feedback.shop_missing.message", clickContext.getPlayer())
                    .build()
                    .sendMessage();
            clickContext.openForPlayer(
                    this.getCurrentViewClass(),
                    this.createViewData(clickContext)
            );
            return;
        }

        final Location shopLocation = shop.getShopLocation();
        if (shopLocation == null) {
            this.i18n("feedback.shop_missing.message", clickContext.getPlayer())
                    .build()
                    .sendMessage();
            clickContext.openForPlayer(
                    this.getCurrentViewClass(),
                    this.createViewData(clickContext)
            );
            return;
        }

        clickContext.openForPlayer(
                ShopBrowserSupport.getTargetView(shop, clickContext.getPlayer()),
                Map.of(
                        "plugin", this.getPlugin(clickContext),
                        "shopLocation", shopLocation
                )
        );
    }
}
