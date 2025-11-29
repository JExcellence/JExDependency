package com.raindropcentral.rdq.rank.view;

import com.raindropcentral.rdq.api.RankService;
import com.raindropcentral.rdq.rank.RankTree;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.api.Placeholder;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class RankMainView extends BaseView {

    private final RankService rankService;
    private final State<List<RankTree>> treesState;

    public RankMainView(@NotNull RankService rankService) {
        super(null); // No parent view
        this.rankService = rankService;
        this.treesState = lazyState(() -> rankService.getAvailableRankTrees().join());
    }

    @Override
    protected String getKey() {
        return "rank_main_ui";
    }

    @Override
    protected int getSize() {
        return 3;
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "XXXXXXXXX",
            "XOOOOOOOX",
            "XXXXXXXXX"
        };
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render) {
        renderDecorations(render);
        renderTreeItems(render);
        renderCloseButton(render);
    }

    /**
     * Renders decorative glass panes.
     */
    private void renderDecorations(@NotNull RenderContext render) {
        render.layoutSlot('X', UnifiedBuilderFactory
                .item(Material.GRAY_STAINED_GLASS_PANE)
                .setName(net.kyori.adventure.text.Component.empty())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build());
    }

    /**
     * Renders rank tree items.
     */
    private void renderTreeItems(@NotNull RenderContext render) {
        var trees = treesState.get(render);
        var slots = new int[]{10, 11, 12, 13, 14, 15, 16};

        for (int i = 0; i < Math.min(trees.size(), slots.length); i++) {
            var tree = trees.get(i);
            var slot = slots[i];

            var item = createTreeItem(render, tree);
            render.slot(slot, item)
                .onClick(ctx -> {
                    ctx.openForPlayer(RankTreeView.class, rankService, tree.id());
                });
        }
    }

    /**
     * Renders close button.
     */
    private void renderCloseButton(@NotNull RenderContext render) {
        render.slot(22, createCloseItem(render))
            .onClick(ctx -> ctx.closeForPlayer());
    }

    private ItemStack createTreeItem(@NotNull RenderContext render, @NotNull RankTree tree) {
        var player = render.getPlayer();
        var material = parseMaterial(tree.iconMaterial());
        
        return UnifiedBuilderFactory
                .item(material)
                .setName(this.i18n("tree_item.name", player)
                        .with(Placeholder.of("tree_name", tree.displayNameKey()))
                        .build().component())
                .setLore(this.i18n("tree_item.lore", player)
                        .with(Placeholder.of("description", tree.descriptionKey()))
                        .with(Placeholder.of("rank_count", tree.rankCount()))
                        .with(Placeholder.of("max_tier", tree.maxTier()))
                        .build().splitLines())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private ItemStack createCloseItem(@NotNull RenderContext render) {
        var player = render.getPlayer();
        
        return UnifiedBuilderFactory
                .item(Material.BARRIER)
                .setName(this.i18n("close_button.name", player).build().component())
                .setLore(this.i18n("close_button.lore", player).build().splitLines())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private Material parseMaterial(@NotNull String name) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }
}
