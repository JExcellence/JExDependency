package com.raindropcentral.rdq.bounty.view;

import com.raindropcentral.rdq.RDQCore;
import com.raindropcentral.rdq.bounty.HunterStats;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.api.Placeholder;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class BountyLeaderboardView extends BaseView {

    private static final int LEADERBOARD_SIZE = 10;

    private final State<RDQCore> rdqCore = initialState("rdqCore");

    public BountyLeaderboardView() {
        super(BountyMainView.class);
    }

    @Override
    protected String getKey() {
        return "bounty_leaderboard_ui";
    }

    @Override
    protected int getSize() {
        return 4;
    }

    @Override
    protected String[] getLayout() {
        return new String[] {
            "XXXXXXXXX",
            "XlllllllX",
            "XlllXXXXX",
            "XXXXXXXXX"
        };
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        renderDecorations(render);
        renderLoadingIndicator(render, player);
        loadAndRenderLeaderboard(render, player);
    }

    private void renderDecorations(@NotNull RenderContext render) {
        render.layoutSlot('X', UnifiedBuilderFactory
            .item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(Component.empty())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build());
    }

    private void renderLoadingIndicator(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('l', UnifiedBuilderFactory
            .item(Material.HOPPER)
            .setName(this.i18n("loading.name", player).build().component())
            .setLore(this.i18n("loading.lore", player).build().splitLines())
            .build());
    }

    private void loadAndRenderLeaderboard(@NotNull RenderContext render, @NotNull Player player) {
        var rdq = this.rdqCore.get(render);
        if (rdq == null) {
            renderError(render, player);
            return;
        }

        var bountyService = rdq.getBountyService();
        bountyService.getLeaderboard(LEADERBOARD_SIZE).thenAccept(hunters -> {
            rdq.getPlatform().getScheduler().runSync(() -> {
                renderLeaderboard(render, player, hunters);
            });
        }).exceptionally(throwable -> {
            rdq.getPlatform().getScheduler().runSync(() -> {
                this.i18n("load_error", player)
                    .withPrefix()
                    .with(Placeholder.of("error", throwable.getMessage()))
                    .send();
                renderError(render, player);
            });
            return null;
        });
    }

    private void renderLeaderboard(@NotNull RenderContext render, @NotNull Player player, @NotNull List<HunterStats> hunters) {
        render.slot(4, UnifiedBuilderFactory
            .item(Material.DIAMOND)
            .setName(this.i18n("header.name", player).build().component())
            .setLore(this.i18n("header.lore", player)
                .with(Placeholder.of("count", hunters.size()))
                .build().splitLines())
            .build());

        if (hunters.isEmpty()) {
            render.slot(13, UnifiedBuilderFactory
                .item(Material.BARRIER)
                .setName(this.i18n("empty.name", player).build().component())
                .setLore(this.i18n("empty.lore", player).build().splitLines())
                .build());
            return;
        }

        int slotIndex = 0;
        for (var stats : hunters) {
            if (slotIndex >= 10) break;

            int row = 1 + (slotIndex / 7);
            int col = 1 + (slotIndex % 7);
            int actualSlot = (row * 9) + col;

            int rank = slotIndex + 1;
            boolean isViewer = stats.playerId().equals(player.getUniqueId());

            render.slot(actualSlot, createHunterItem(stats, rank, isViewer, player));
            slotIndex++;
        }
    }

    private org.bukkit.inventory.ItemStack createHunterItem(
        @NotNull HunterStats stats,
        int rank,
        boolean isViewer,
        @NotNull Player player
    ) {
        var hunterPlayer = Bukkit.getOfflinePlayer(stats.playerId());
        var rankMaterial = getRankMaterial(rank);

        return UnifiedBuilderFactory.unifiedHead(hunterPlayer)
            .setDisplayName(this.i18n(isViewer ? "entry_self.name" : "entry.name", player)
                .with(Placeholder.of("rank", rank))
                .with(Placeholder.of("player_name", stats.playerName()))
                .build().component())
            .setLore(this.i18n("entry.lore", player)
                .with(Placeholder.of("bounties_claimed", stats.bountiesClaimed()))
                .with(Placeholder.of("bounties_placed", stats.bountiesPlaced()))
                .with(Placeholder.of("total_earned", stats.totalEarned().toPlainString()))
                .with(Placeholder.of("total_spent", stats.totalSpent().toPlainString()))
                .with(Placeholder.of("kd_ratio", String.format("%.2f", stats.getKDRatio())))
                .build().splitLines())
            .build();
    }

    private Material getRankMaterial(int rank) {
        return switch (rank) {
            case 1 -> Material.GOLD_BLOCK;
            case 2 -> Material.IRON_BLOCK;
            case 3 -> Material.COPPER_BLOCK;
            default -> Material.PLAYER_HEAD;
        };
    }

    private void renderError(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('l', UnifiedBuilderFactory
            .item(Material.REDSTONE_BLOCK)
            .setName(this.i18n("error.name", player).build().component())
            .setLore(this.i18n("error.lore", player).build().splitLines())
            .build());
    }
}
