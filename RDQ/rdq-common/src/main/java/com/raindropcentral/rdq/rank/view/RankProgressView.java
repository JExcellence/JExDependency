package com.raindropcentral.rdq.rank.view;

import com.raindropcentral.rdq.api.RankService;
import com.raindropcentral.rdq.rank.PlayerRankData;
import com.raindropcentral.rdq.rank.Rank;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.context.RenderContext;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public final class RankProgressView extends View {

    private final RankService rankService;

    public RankProgressView(@NotNull RankService rankService) {
        this.rankService = rankService;
    }

    @Override
    public void onInit(@NotNull ViewConfigBuilder config) {
        config.cancelOnClick();
        config.size(4);
        config.title("Your Rank Progress");
        config.layout(
            "XXXXXXXXX",
            "XOOOOOOOX",
            "XOOOOOOOX",
            "B       X"
        );
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render) {
        var playerId = render.getPlayer().getUniqueId();
        var playerDataOpt = rankService.getPlayerRanks(playerId).join();

        if (playerDataOpt.isEmpty()) {
            render.slot(13, createNoProgressItem());
            render.layoutSlot('B', createBackItem())
                .onClick(ctx -> ctx.closeForPlayer());
            return;
        }

        var playerData = playerDataOpt.get();
        var activePaths = playerData.activePaths();
        var slots = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

        int slotIndex = 0;
        for (var path : activePaths) {
            if (slotIndex >= slots.length) break;

            var treeOpt = rankService.getRankTree(path.treeId()).join();
            var currentRankOpt = rankService.getRank(path.currentRankId()).join();

            if (treeOpt.isPresent() && currentRankOpt.isPresent()) {
                var tree = treeOpt.get();
                var currentRank = currentRankOpt.get();
                var nextRank = findNextRank(tree.ranks(), currentRank);

                render.slot(slots[slotIndex], createProgressItem(tree.displayNameKey(), currentRank, nextRank));
                slotIndex++;
            }
        }

        render.layoutSlot('B', createBackItem())
            .onClick(ctx -> ctx.closeForPlayer());
    }


    private Optional<Rank> findNextRank(@NotNull List<Rank> ranks, @NotNull Rank current) {
        return ranks.stream()
            .filter(r -> r.tier() > current.tier() && r.enabled())
            .findFirst();
    }

    private ItemStack createProgressItem(@NotNull String treeName, @NotNull Rank current, @NotNull Optional<Rank> next) {
        var material = parseMaterial(current.iconMaterial());
        var item = new ItemStack(material);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("<gold>" + treeName);
            if (next.isPresent()) {
                meta.setLore(List.of(
                    "<gray>Current: <white>" + current.displayNameKey(),
                    "<gray>Tier: <white>" + current.tier(),
                    "",
                    "<yellow>Next Rank: <white>" + next.get().displayNameKey(),
                    "<yellow>Requirements: <white>" + next.get().requirements().size(),
                    "",
                    "<green>Click to view tree"
                ));
            } else {
                meta.setLore(List.of(
                    "<gray>Current: <white>" + current.displayNameKey(),
                    "<gray>Tier: <white>" + current.tier(),
                    "",
                    "<green>✓ Max rank reached!",
                    "",
                    "<green>Click to view tree"
                ));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNoProgressItem() {
        var item = new ItemStack(Material.BARRIER);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("<red>No Active Rank Paths");
            meta.setLore(List.of(
                "<gray>You haven't started any rank paths yet.",
                "",
                "<yellow>Click 'Back' to browse rank trees"
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

    private Material parseMaterial(@NotNull String name) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }
}
