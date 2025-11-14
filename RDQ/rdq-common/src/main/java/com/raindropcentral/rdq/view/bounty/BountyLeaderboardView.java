package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.database.entity.bounty.BountyHunterStats;
import com.raindropcentral.rdq.service.bounty.BountyService;
import com.raindropcentral.rdq.service.bounty.BountyServiceProvider;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated view displaying the bounty hunter leaderboard with player statistics.
 * <p>
 * This view shows top bounty hunters ranked by their claimed bounties and total
 * reward values. Players can see detailed statistics including claim counts,
 * total rewards earned, and highest single bounty claimed.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 2.0.0
 */
public final class BountyLeaderboardView extends APaginatedView<BountyHunterStats> {

    private static final DateTimeFormatter TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    /**
     * Creates a new leaderboard view with navigation back to the main bounty menu.
     */
    public BountyLeaderboardView() {
        super(BountyMainView.class);
    }

    /**
     * Gets the translation key used for localization of this view.
     *
     * @return the i18n key for the leaderboard view
     */
    @Override
    protected String getKey() {
        return "bounty.leaderboard";
    }

    /**
     * Loads the top bounty hunters from the service layer asynchronously.
     *
     * @param context the render context containing player information
     * @return a future providing the ordered list of hunter statistics
     */
    @Override
    protected CompletableFuture<List<BountyHunterStats>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        final BountyService service = BountyServiceProvider.getInstance();
        return service.getTopHunters(100, "bounties_claimed");
    }

    /**
     * Renders a single leaderboard entry showing hunter statistics.
     *
     * @param context the render context providing access to the viewing player
     * @param builder the item component builder for configuring the display
     * @param index the zero-based position in the leaderboard
     * @param stats the hunter statistics to display
     */
    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull BountyHunterStats stats
    ) {
        final Player viewer = context.getPlayer();
        final OfflinePlayer hunter = Bukkit.getOfflinePlayer(stats.getPlayer().getUniqueId());
        final int rank = index + 1;

        final Material rankMaterial = switch (rank) {
            case 1 -> Material.DIAMOND_BLOCK;
            case 2 -> Material.GOLD_BLOCK;
            case 3 -> Material.IRON_BLOCK;
            default -> Material.PLAYER_HEAD;
        };

        final String lastClaimTime = stats.getLastClaimTimestamp() != null
                ? TIME_FORMATTER.format(Instant.ofEpochMilli(stats.getLastClaimTimestamp()))
                : this.i18n("entry.never", viewer).build().asPlainText();

        builder.withItem(
                UnifiedBuilderFactory
                        .head()
                        .setPlayerHead(hunter)
                        .setName(
                                this.i18n("entry.name", viewer)
                                        .with("rank", rank)
                                        .with("player_name", stats.getPlayer().getPlayerName())
                                        .build()
                                        .component()
                        )
                        .setLore(
                                this.i18n("entry.lore", viewer)
                                        .withAll(Map.of(
                                                "rank", rank,
                                                "player_name", stats.getPlayer().getPlayerName(),
                                                "bounties_claimed", stats.getBountiesClaimed(),
                                                "total_reward_value", String.format("%.2f", stats.getTotalRewardValue()),
                                                "highest_bounty_value", String.format("%.2f", stats.getHighestBountyValue()),
                                                "last_claim_time", lastClaimTime
                                        ))
                                        .build()
                                        .splitLines()
                        )
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .setGlowing(rank <= 3)
                        .build()
        );
    }

    /**
     * Hook for additional rendering after pagination layout completes.
     *
     * @param render the render context for slot management
     * @param player the player viewing the leaderboard
     */
    @Override
    protected void onPaginatedRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        // Optional: Add viewer's own stats in a special slot
        this.renderViewerStats(render, player);
    }

    /**
     * Renders the viewing player's own statistics in a dedicated slot.
     *
     * @param render the render context for slot placement
     * @param player the player viewing the leaderboard
     */
    private void renderViewerStats(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final BountyService service = BountyServiceProvider.getInstance();

        service.getHunterStats(player.getUniqueId()).thenAccept(statsOpt -> {
            if (statsOpt.isEmpty()) {
                return;
            }

            final BountyHunterStats stats = statsOpt.get();

            service.getHunterRank(player.getUniqueId()).thenAccept(rank -> {
                render.slot(
                        49, // Bottom center slot
                        UnifiedBuilderFactory
                                .head()
                                .setPlayerHead(player)
                                .setName(
                                        this.i18n("viewer_stats.name", player)
                                                .build()
                                                .component()
                                )
                                .setLore(
                                        this.i18n("viewer_stats.lore", player)
                                                .withAll(Map.of(
                                                        "rank", rank > 0 ? rank : "Unranked",
                                                        "bounties_claimed", stats.getBountiesClaimed(),
                                                        "total_reward_value", String.format("%.2f", stats.getTotalRewardValue()),
                                                        "highest_bounty_value", String.format("%.2f", stats.getHighestBountyValue())
                                                ))
                                                .build()
                                                .splitLines()
                                )
                                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                                .setGlowing(true)
                                .build()
                );
            });
        });
    }
}
