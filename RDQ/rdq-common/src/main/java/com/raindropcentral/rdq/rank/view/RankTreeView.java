package com.raindropcentral.rdq.rank.view;

import com.raindropcentral.rdq.RDQCore;
import com.raindropcentral.rdq.api.RankService;
import com.raindropcentral.rdq.rank.PlayerRankData;
import com.raindropcentral.rdq.rank.Rank;
import com.raindropcentral.rdq.rank.RankTree;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.api.Placeholder;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RankTreeView extends BaseView {

    private final State<RDQCore> rdq = initialState("plugin");
    private final State<String> treeId = initialState("tree_id");

    private final List<Rank> ranks = new ArrayList<>();

    private final RankService rankService;
    private final String treeId;
    private State<List<Rank>> ranksState;

    public RankTreeView(@NotNull RankService rankService, @NotNull String treeId) {
        super(RankMainView.class);
        this.rankService = rankService;
        this.treeId = treeId;
        this.ranksState = lazyState(() -> rankService.getRankTree(treeId).join()
                .map(RankTree::ranks)
                .orElse(List.of()));
    }

    @Override
    protected String getKey() {
        return "rank_tree_ui";
    }

    @Override
    protected int getSize() {
        return 6;
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
                "XXXXXXXXX",
                "OOOOOOOOO",
                "OOOOOOOOO",
                "OOOOOOOOO",
                "OOOOOOOOO",
                "        X"
        };
    }

    @Override
    protected Map<String, Object> getTitlePlaceholders(@NotNull OpenContext open) {
        var tree = rankService.getRankTree(treeId).join();
        return Map.of(
                "tree_name", tree.map(RankTree::displayNameKey).orElse("Unknown")
        );
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        renderDecorations(render);
        renderRankItems(render, player);
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
     * Renders rank items with click handlers to open detail view.
     */
    private void renderRankItems(@NotNull RenderContext render, @NotNull Player player) {
        var ranks = ranksState.get(render);
        var playerId = player.getUniqueId();
        var playerData = rankService.getPlayerRanks(playerId).join();
        var slots = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

        for (int i = 0; i < Math.min(ranks.size(), slots.length); i++) {
            var rank = ranks.get(i);
            var item = createRankItem(render, rank, playerData);
            render.slot(slots[i], item)
                    .onClick(ctx -> {
                        ctx.openForPlayer(RankDetailView.class, rankService, rank.id());
                    });
        }
    }

    private ItemStack createRankItem(@NotNull RenderContext render, @NotNull Rank rank, @NotNull Optional<PlayerRankData> playerData) {
        var player = render.getPlayer();
        var material = parseMaterial(rank.iconMaterial());
        var unlocked = playerData.map(d -> d.hasUnlockedRank(rank.id())).orElse(false);
        var unlockedStatus = unlocked ? "<green>✓" : "<red>✗";
        var unlockStatus = unlocked ? "<green>Unlocked!" : "<yellow>Click to view details";

        return UnifiedBuilderFactory
                .item(material)
                .setName(this.i18n("rank_item.name", player)
                        .with(Placeholder.of("unlocked_status", unlockedStatus))
                        .with(Placeholder.of("rank_name", rank.displayNameKey()))
                        .build().component())
                .setLore(this.i18n("rank_item.lore", player)
                        .with(Placeholder.of("description", rank.descriptionKey()))
                        .with(Placeholder.of("tier", rank.tier()))
                        .with(Placeholder.of("requirement_count", rank.requirements().size()))
                        .with(Placeholder.of("unlock_status", unlockStatus))
                        .build().splitLines())
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
