package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.bounty.dto.HunterStats;
import com.raindropcentral.rdq.bounty.type.HunterSortOrder;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.api.Placeholder;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * View for displaying the bounty hunter leaderboard.
 * Shows top hunters ranked by bounties claimed and total reward value.
 *
 * Requirements: 8.1, 15.1, 15.2
 * 
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class BountyLeaderboardView extends BaseView {

    private static final int TOP_HUNTERS_LIMIT = 10;

    private final State<RDQ> rdq = initialState("plugin");

    public BountyLeaderboardView() {
        super(BountyMainView.class);
    }

    @Override
    protected String getKey() {
        return "bounty_leaderboard_ui";
    }

    @Override
    protected int getSize() {
        return 6;
    }

    @Override
    protected String[] getLayout() {
        return new String[] {
                "XXXXXXXXX",
                "XhhhhhhhX",
                "XhhhhhhhX",
                "XhhhhhhhX",
                "XhhhhhhhX",
                "XXXXXXXXX"
        };
    }

    @Override
    protected int getUpdateSchedule() {
        return 20 * 30; // Update every 30 seconds
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        // Render decorations
        renderDecorations(render);
        
        // Show loading indicator
        render.layoutSlot('h', UnifiedBuilderFactory
                .item(Material.HOPPER)
                .setName(this.i18n("loading.name", player).build().component())
                .setLore(this.i18n("loading.lore", player).build().splitLines())
                .build());
        
        // Load and render leaderboard
        loadAndRenderLeaderboard(render, player);
    }

    /**
     * Renders decorative glass panes.
     */
    private void renderDecorations(@NotNull RenderContext render) {
        render.layoutSlot('X', UnifiedBuilderFactory
                .item(Material.GRAY_STAINED_GLASS_PANE)
                .setName(Component.empty())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build());
    }

    /**
     * Loads and renders the leaderboard.
     * Requirements: 8.1, 8.2
     */
    private void loadAndRenderLeaderboard(@NotNull RenderContext render, @NotNull Player player) {
        var rdq = this.rdq.get(render);
        var statsRepository = rdq.getBountyHunterStatsRepository();
        
        // Fetch top hunters asynchronously
        statsRepository.findTopByBountiesClaimedAsync(TOP_HUNTERS_LIMIT).thenAccept(topHunters -> {
            // Convert to DTOs
            var hunterStatsList = new ArrayList<HunterStats>();
            int rank = 1;
            for (var entity : topHunters) {
                var hunterPlayer = org.bukkit.Bukkit.getOfflinePlayer(entity.getPlayerUniqueId());
                hunterStatsList.add(new HunterStats(
                        entity.getPlayerUniqueId(),
                        hunterPlayer.getName() != null ? hunterPlayer.getName() : "Unknown",
                        entity.getBountiesClaimed(),
                        entity.getTotalRewardValue(),
                        entity.getHighestBountyValue(),
                        entity.getLastClaimTimestamp().map(timestamp -> 
                                java.time.LocalDateTime.ofInstant(
                                        java.time.Instant.ofEpochMilli(timestamp),
                                        java.time.ZoneId.systemDefault())),
                        rank++
                ));
            }
            
            // Render on main thread
            rdq.getPlatform().getScheduler().runSync(() -> {
                renderLeaderboard(render, player, hunterStatsList);
            });
        }).exceptionally(throwable -> {
            rdq.getPlatform().getScheduler().runSync(() -> {
                this.i18n("load_error", player)
                        .withPrefix()
                        .with(Placeholder.of("error", throwable.getMessage()))
                        .send();
            });
            return null;
        });
    }

    /**
     * Renders the leaderboard entries.
     * Requirements: 8.2, 8.3, 8.5
     */
    private void renderLeaderboard(@NotNull RenderContext render, @NotNull Player player, @NotNull List<HunterStats> hunters) {
        if (hunters.isEmpty()) {
            // Show empty message
            render.layoutSlot('h', UnifiedBuilderFactory
                    .item(Material.BARRIER)
                    .setName(this.i18n("empty.name", player).build().component())
                    .setLore(this.i18n("empty.lore", player).build().splitLines())
                    .build());
            return;
        }
        
        // Render each hunter in the leaderboard
        int slotIndex = 0;
        for (var hunter : hunters) {
            if (slotIndex >= 28) break; // Max 28 slots (4 rows of 7)
            
            int row = 1 + (slotIndex / 7);
            int col = 1 + (slotIndex % 7);
            int actualSlot = (row * 9) + col;
            
            var hunterPlayer = org.bukkit.Bukkit.getOfflinePlayer(hunter.playerUuid());
            boolean isViewer = hunter.playerUuid().equals(player.getUniqueId());
            
            // Use different material for viewer's own entry
            Material headMaterial = isViewer ? Material.PLAYER_HEAD : Material.PLAYER_HEAD;
            
            render.slot(actualSlot, UnifiedBuilderFactory.unifiedHead(hunterPlayer)
                    .setDisplayName(this.i18n(isViewer ? "hunter_entry_self.name" : "hunter_entry.name", player)
                            .with(Placeholder.of("rank", hunter.rank()))
                            .with(Placeholder.of("hunter_name", hunter.playerName()))
                            .build().component())
                    .setLore(this.i18n("hunter_entry.lore", player)
                            .with(Placeholder.of("bounties_claimed", hunter.bountiesClaimed()))
                            .with(Placeholder.of("total_reward", String.format("%.2f", hunter.totalRewardValue())))
                            .with(Placeholder.of("highest_bounty", String.format("%.2f", hunter.highestBountyValue())))
                            .build().splitLines())
                    .build());
            
            slotIndex++;
        }
    }
}
