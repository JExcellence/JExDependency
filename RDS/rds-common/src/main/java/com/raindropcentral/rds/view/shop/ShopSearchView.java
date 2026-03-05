package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.view.shop.anvil.ShopMaterialSearchAnvilView;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Renders the shop search landing page with actions for browsing every shop or searching by material.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopSearchView extends BaseView {

    private final State<RDS> rds = initialState("plugin");

    /**
     * Creates the shop search landing view.
     */
    public ShopSearchView() {
        super();
    }

    @Override
    protected @NotNull String getKey() {
        return "shop_search_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
                "         ",
                "   l a   ",
                "    s    ",
                "         ",
                "         ",
                "         "
        };
    }

    /**
     * Re-opens the material search results after the player submits a valid anvil query.
     *
     * @param origin context the player returned from
     * @param target active context for this landing view
     */
    @Override
    public void onResume(
            final @NotNull Context origin,
            final @NotNull Context target
    ) {
        final SearchRequest request = this.extractSearchRequest(target) != null
                ? this.extractSearchRequest(target)
                : this.extractSearchRequest(origin);
        if (request == null) {
            target.update();
            return;
        }

        target.openForPlayer(
                ShopResultsView.class,
                Map.of(
                        "plugin", this.rds.get(target),
                        "searchMaterial", request.material()
                )
        );
    }

    /**
     * Draws the landing-page action buttons for browsing and material search.
     *
     * @param render render context for the current inventory
     * @param player player viewing the landing page
     */
    @Override
    public void onFirstRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        render.layoutSlot('s', this.createSummaryItem(player));

        render.layoutSlot('l', this.createBrowseButton(player))
                .onClick(clickContext -> clickContext.openForPlayer(
                        ShopListView.class,
                        Map.of("plugin", this.rds.get(clickContext))
                ));

        render.layoutSlot('a', this.createSearchButton(player))
                .onClick(clickContext -> clickContext.openForPlayer(
                        ShopMaterialSearchAnvilView.class,
                        Map.of("plugin", this.rds.get(clickContext))
                ));
    }

    /**
     * Cancels vanilla inventory interaction so the landing page behaves like a menu.
     *
     * @param click click context for the active inventory interaction
     */
    @Override
    public void onClick(
            final @NotNull SlotClickContext click
    ) {
        click.setCancelled(true);
    }

    private @Nullable SearchRequest extractSearchRequest(
            final @NotNull Context context
    ) {
        final Object initialData = context.getInitialData();
        if (!(initialData instanceof Map<?, ?> data)) {
            return null;
        }

        final Object materialObject = data.get("searchMaterial");
        if (!(materialObject instanceof Material material)) {
            return null;
        }

        return new SearchRequest(material);
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.COMPASS)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n("summary.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createBrowseButton(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.CHEST)
                .setName(this.i18n("browse.name", player).build().component())
                .setLore(this.i18n("browse.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createSearchButton(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.ANVIL)
                .setName(this.i18n("search.name", player).build().component())
                .setLore(this.i18n("search.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private record SearchRequest(
            @NotNull Material material
    ) {
    }
}
