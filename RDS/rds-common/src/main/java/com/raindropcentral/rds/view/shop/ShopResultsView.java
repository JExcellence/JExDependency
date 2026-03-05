package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.component.Pagination;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Renders a paginated directory of shops offering a searched material.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopResultsView extends AbstractShopBrowserView {

    private final State<Material> searchMaterial = initialState("searchMaterial");

    /**
     * Creates the paginated filtered shop results view.
     */
    public ShopResultsView() {
        super(ShopSearchView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "shop_results_ui";
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(
            final @NotNull OpenContext context
    ) {
        return Map.of(
                "material", this.getMaterialLabel(this.searchMaterial.get(context))
        );
    }

    @Override
    protected @NotNull CompletableFuture<List<ShopBrowserSupport.ShopBrowserEntry>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        final Material material = this.searchMaterial.get(context);
        if (material == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        final var plugin = this.getPlugin(context);
        return CompletableFuture.supplyAsync(
                () -> ShopBrowserSupport.loadEntries(plugin, material),
                plugin.getExecutor()
        );
    }

    @Override
    protected @NotNull ItemStack createEntryItem(
            final @NotNull Context context,
            final @NotNull Player player,
            final @NotNull ShopBrowserSupport.ShopBrowserEntry entry
    ) {
        return UnifiedBuilderFactory.item(Material.CHEST)
                .setName(this.i18n("entry.name", player)
                        .withPlaceholders(Map.of(
                                "owner", ShopBrowserSupport.getOwnerName(entry.ownerId())
                        ))
                        .build()
                        .component())
                .setLore(this.i18n("entry.lore", player)
                        .withPlaceholders(Map.of(
                                "owner", ShopBrowserSupport.getOwnerName(entry.ownerId()),
                                "location", ShopBrowserSupport.formatLocation(entry.shopLocation()),
                                "material", this.getMaterialLabel(this.searchMaterial.get(context)),
                                "matching_count", entry.matchingOfferCount(),
                                "available_count", entry.availableItemCount()
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    @Override
    protected @NotNull ItemStack createHeaderItem(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final Pagination pagination = this.getPagination(render);
        final List<ShopBrowserSupport.ShopBrowserEntry> entries = this.getEntries(pagination);
        final Material material = this.searchMaterial.get(render);

        if (pagination.source() != null && entries.isEmpty()) {
            return UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(this.i18n("feedback.empty.name", player)
                            .withPlaceholders(Map.of(
                                    "material", this.getMaterialLabel(material)
                            ))
                            .build()
                            .component())
                    .setLore(this.i18n("feedback.empty.lore", player)
                            .withPlaceholders(Map.of(
                                    "material", this.getMaterialLabel(material)
                            ))
                            .build()
                            .children())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        }

        int matchingOfferCount = 0;
        for (final ShopBrowserSupport.ShopBrowserEntry entry : entries) {
            matchingOfferCount += entry.matchingOfferCount();
        }

        final Material summaryIcon = material == null ? Material.COMPASS : material;
        return UnifiedBuilderFactory.item(summaryIcon)
                .setName(this.i18n("summary.name", player)
                        .withPlaceholders(Map.of(
                                "material", this.getMaterialLabel(material)
                        ))
                        .build()
                        .component())
                .setLore(this.i18n("summary.lore", player)
                        .withPlaceholders(Map.of(
                                "material", this.getMaterialLabel(material),
                                "shop_count", entries.size(),
                                "matching_count", matchingOfferCount
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    @Override
    protected @NotNull Map<String, Object> createViewData(
            final @NotNull Context context
    ) {
        final Map<String, Object> viewData = new LinkedHashMap<>();
        viewData.put("plugin", this.getPlugin(context));

        final Material material;
        try {
            material = this.searchMaterial.get(context);
        } catch (RuntimeException ignored) {
            return viewData;
        }

        if (material != null) {
            viewData.put("searchMaterial", material);
        }

        return viewData;
    }

    @Override
    protected @NotNull Class<? extends View> getCurrentViewClass() {
        return ShopResultsView.class;
    }

    @Override
    protected void handleBackButtonClick(
            final @NotNull SlotClickContext clickContext
    ) {
        clickContext.openForPlayer(
                ShopSearchView.class,
                Map.of("plugin", this.getPlugin(clickContext))
        );
    }

    private @NotNull String getMaterialLabel(
            final @Nullable Material material
    ) {
        return material == null
                ? "Unknown Material"
                : ShopBrowserSupport.formatMaterialName(material);
    }
}
