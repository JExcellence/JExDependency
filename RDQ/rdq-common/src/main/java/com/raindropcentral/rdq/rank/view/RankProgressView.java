package com.raindropcentral.rdq.rank.view;

import com.raindropcentral.rdq.api.RankService;
import com.raindropcentral.rdq.rank.PlayerRankData;
import com.raindropcentral.rdq.rank.Rank;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.api.Placeholder;
import me.devnatan.inventoryframework.context.RenderContext;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public final class RankProgressView extends BaseView {

    private final RankService rankService;

    public RankProgressView(@NotNull RankService rankService) {
        super(RankMainView.class);
        this.rankService = rankService;
    }

    @Override
    protected String getKey() {
        return "rank_progress_ui";
    }

    @Override
    protected int getSize() {
        return 4;
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
                "XXXXXXXXX",
                "XOOOOOOOX",
                "XOOOOOOOX",
                "        X"
        };
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        renderDecorations(render);

        var playerId = player.getUniqueId();
        var playerDataOpt = rankService.getPlayerRanks(playerId).join();

        if (playerDataOpt.isEmpty()) {
            renderNoProgress(render, player);
        } else {
            renderProgressItems(render, player, playerDataOpt.get());
        }
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
     * Renders progress items for each active rank path.
     */
    private void renderProgressItems(@NotNull RenderContext render, @NotNull Player player, @NotNull PlayerRankData playerData) {
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

                render.slot(slots[slotIndex], createProgressItem(player, tree.displayNameKey(), currentRank, nextRank))
                        .onClick(ctx -> {
                            ctx.openForPlayer(RankTreeView.class, );
                        });
                slotIndex++;
            }
        }
    }

    /**
     * Renders no progress indicator.
     */
    private void renderNoProgress(@NotNull RenderContext render, @NotNull Player player) {
        render.slot(13, createNoProgressItem(player));
    }

    private Optional<Rank> findNextRank(@NotNull List<Rank> ranks, @NotNull Rank current) {
        return ranks.stream()
            .filter(r -> r.tier() > current.tier() && r.enabled())
            .findFirst();
    }

    private ItemStack createProgressItem(@NotNull Player player, @NotNull String treeName, @NotNull Rank current, @NotNull Optional<Rank> next) {
        var material = parseMaterial(current.iconMaterial());

        if (next.isPresent()) {
            return UnifiedBuilderFactory
                    .item(material)
                    .setName(this.i18n("progress_item.name", player)
                            .with(Placeholder.of("tree_name", treeName))
                            .build().component())
                    .setLore(this.i18n("progress_item.lore", player)
                            .with(Placeholder.of("current_rank", current.displayNameKey()))
                            .with(Placeholder.of("tier", current.tier()))
                            .with(Placeholder.of("next_rank", next.get().displayNameKey()))
                            .with(Placeholder.of("requirement_count", next.get().requirements().size()))
                            .build().splitLines())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        } else {
            return UnifiedBuilderFactory
                    .item(material)
                    .setName(this.i18n("progress_item.name", player)
                            .with(Placeholder.of("tree_name", treeName))
                            .build().component())
                    .setLore(this.i18n("progress_item.max_rank.lore", player)
                            .with(Placeholder.of("current_rank", current.displayNameKey()))
                            .with(Placeholder.of("tier", current.tier()))
                            .build().splitLines())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        }
    }

    private ItemStack createNoProgressItem(@NotNull Player player) {
        return UnifiedBuilderFactory
                .item(Material.BARRIER)
                .setName(this.i18n("no_progress.name", player).build().component())
                .setLore(this.i18n("no_progress.lore", player).build().splitLines())
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
