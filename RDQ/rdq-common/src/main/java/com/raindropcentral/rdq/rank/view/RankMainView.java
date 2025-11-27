package com.raindropcentral.rdq.rank.view;

import com.raindropcentral.rdq.api.RankService;
import com.raindropcentral.rdq.rank.RankTree;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class RankMainView extends View {

    private final RankService rankService;
    private final State<List<RankTree>> treesState;

    public RankMainView(@NotNull RankService rankService) {
        this.rankService = rankService;
        this.treesState = lazyState(() -> rankService.getAvailableRankTrees().join());
    }

    @Override
    public void onInit(@NotNull ViewConfigBuilder config) {
        config.cancelOnClick();
        config.size(3);
        config.title("Rank Trees");
        config.layout(
            "XXXXXXXXX",
            "XOOOOOOOX",
            "XXXXXXXXX"
        );
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render) {
        var trees = treesState.get(render);
        var slots = new int[]{10, 11, 12, 13, 14, 15, 16};

        for (int i = 0; i < Math.min(trees.size(), slots.length); i++) {
            var tree = trees.get(i);
            var slot = slots[i];

            var item = createTreeItem(tree);
            render.slot(slot, item)
                .onClick(ctx -> {
                    ctx.openForPlayer(RankTreeView.class, tree.id());
                });
        }

        render.slot(22, createCloseItem())
            .onClick(ctx -> ctx.closeForPlayer());
    }

    private ItemStack createTreeItem(@NotNull RankTree tree) {
        var material = parseMaterial(tree.iconMaterial());
        var item = new ItemStack(material);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("<gold>" + tree.displayNameKey());
            meta.setLore(List.of(
                "<gray>" + tree.descriptionKey(),
                "",
                "<yellow>Ranks: <white>" + tree.rankCount(),
                "<yellow>Max Tier: <white>" + tree.maxTier(),
                "",
                "<green>Click to view ranks"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }


    private ItemStack createCloseItem() {
        var item = new ItemStack(Material.BARRIER);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("<red>Close");
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material parseMaterial(@NotNull String name) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }
}
