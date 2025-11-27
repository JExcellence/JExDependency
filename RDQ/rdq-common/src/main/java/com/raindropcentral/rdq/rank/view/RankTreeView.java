package com.raindropcentral.rdq.rank.view;

import com.raindropcentral.rdq.api.RankService;
import com.raindropcentral.rdq.rank.PlayerRankData;
import com.raindropcentral.rdq.rank.Rank;
import com.raindropcentral.rdq.rank.RankTree;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public final class RankTreeView extends View {

    private final RankService rankService;
    private final String treeId;
    private State<List<Rank>> ranksState;

    public RankTreeView(@NotNull RankService rankService, @NotNull String treeId) {
        this.rankService = rankService;
        this.treeId = treeId;
    }

    @Override
    public void onInit(@NotNull ViewConfigBuilder config) {
        config.cancelOnClick();
        config.size(6);
        config.title("Rank Tree");
        config.layout(
            "XXXXXXXXX",
            "OOOOOOOOO",
            "OOOOOOOOO",
            "OOOOOOOOO",
            "OOOOOOOOO",
            "B        "
        );

        ranksState = lazyState(() -> rankService.getRankTree(treeId).join()
            .map(RankTree::ranks)
            .orElse(List.of()));
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render) {
        var tree = rankService.getRankTree(treeId).join();
        if (tree.isPresent()) {
            render.updateTitleForPlayer("<gold>" + tree.get().displayNameKey());
        }

        var ranks = ranksState.get(render);
        var playerId = render.getPlayer().getUniqueId();
        var playerData = rankService.getPlayerRanks(playerId).join();
        var slots = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

        for (int i = 0; i < Math.min(ranks.size(), slots.length); i++) {
            var rank = ranks.get(i);
            var item = createRankItem(rank, playerData);
            render.slot(slots[i], item);
        }

        render.layoutSlot('B', createBackItem())
            .onClick(ctx -> ctx.closeForPlayer());
    }


    private ItemStack createRankItem(@NotNull Rank rank, @NotNull Optional<PlayerRankData> playerData) {
        var material = parseMaterial(rank.iconMaterial());
        var item = new ItemStack(material);
        var meta = item.getItemMeta();
        if (meta != null) {
            var unlocked = playerData.map(d -> d.hasUnlockedRank(rank.id())).orElse(false);
            var prefix = unlocked ? "<green>✓ " : "<red>✗ ";
            meta.setDisplayName(prefix + "<gold>" + rank.displayNameKey());
            meta.setLore(List.of(
                "<gray>" + rank.descriptionKey(),
                "",
                "<yellow>Tier: <white>" + rank.tier(),
                "<yellow>Requirements: <white>" + rank.requirements().size(),
                "",
                unlocked ? "<green>Unlocked!" : "<yellow>Click to view details"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBackItem() {
        var item = new ItemStack(Material.ARROW);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("<red>Back");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPreviousItem() {
        var item = new ItemStack(Material.ARROW);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("<yellow>Previous Page");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNextItem() {
        var item = new ItemStack(Material.ARROW);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("<yellow>Next Page");
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
