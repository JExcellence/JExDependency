package de.jexcellence.quests.view;

import de.jexcellence.jexplatform.view.BaseView;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.database.entity.PlayerRank;
import de.jexcellence.quests.database.entity.RankTree;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Rank dashboard — the landing view opened by {@code /rank}. Ported
 * from RDQ's {@code RankMainView} as a 3-row hub with three primary
 * actions: your active path, browse available trees, and leaderboard.
 *
 * <p>All dependent data is fetched <b>synchronously</b> inside
 * {@code onRender} — the IF render context only accepts slot writes
 * while the frame is alive, so any {@code thenAccept} continuation
 * would miss the window and paint empty slots. Queries are indexed
 * on {@code (player_uuid, tree_identifier)} and finish in under a
 * millisecond on a warm connection; a 2-second timeout guards
 * against DB stalls with graceful fallback rendering.
 */
public class RankMainView extends BaseView {

    private static final int SLOT_SUMMARY = 4;
    private static final int SLOT_PATH = 11;
    private static final int SLOT_BROWSE = 13;
    private static final int SLOT_LEADERBOARD = 15;

    private final State<JExQuests> plugin = initialState("plugin");

    public RankMainView() {
        super();
    }

    @Override
    protected String translationKey() {
        return "rank_main_ui";
    }

    @Override
    protected int size() {
        return 3;
    }

    @Override
    protected String[] layout() {
        return new String[]{
                "    S    ",
                "  P B L  ",
                "         "
        };
    }

    @Override
    protected void onRender(@NotNull RenderContext render, @NotNull Player player) {
        final JExQuests quests = this.plugin.get(render);

        // Resolve everything synchronously: profile row (for the active
        // tree id) and full rank-row set (for enrolled / completed
        // counts). The `activeRankTree` in QuestsPlayer is the canonical
        // active path — prefer it over the PlayerRank.active flag when
        // both are populated (normal case).
        final String activeTreeId;
        final List<PlayerRank> allRows;
        try {
            activeTreeId = quests.questsPlayerService().findAsync(player.getUniqueId())
                    .get(2, TimeUnit.SECONDS)
                    .map(q -> q.getActiveRankTree())
                    .orElse(null);
            allRows = quests.rankService().playerRanks(player.getUniqueId())
                    .get(2, TimeUnit.SECONDS);
        } catch (final TimeoutException | java.util.concurrent.ExecutionException | InterruptedException ex) {
            // Fall-through render — empty counts, no active path.
            renderSummary(render, player, null, null, 0, 0);
            renderPathButton(render, player, quests, null);
            renderBrowseButton(render, player);
            renderLeaderboardButton(render, player, quests, null);
            return;
        }

        final PlayerRank activeRow = resolveActiveRow(allRows, activeTreeId);
        final long completed = allRows.stream().filter(PlayerRank::isTreeCompleted).count();
        renderSummary(render, player, activeTreeId, activeRow, allRows.size(), (int) completed);
        renderPathButton(render, player, quests, activeTreeId);
        renderBrowseButton(render, player);
        renderLeaderboardButton(render, player, quests, activeTreeId);
    }

    private void renderSummary(@NotNull RenderContext render, @NotNull Player player,
                                String activeTreeId, PlayerRank activeRow,
                                int enrolled, int completed) {
        render.slot(SLOT_SUMMARY, createItem(
                Material.PLAYER_HEAD,
                i18n("summary.name", player)
                        .withPlaceholder("player", player.getName())
                        .build().component(),
                i18n("summary.lore", player)
                        .withPlaceholders(Map.of(
                                "active_tree", activeTreeId != null ? activeTreeId : "—",
                                "active_rank", activeRow != null ? activeRow.getCurrentRankIdentifier() : "—",
                                "enrolled", String.valueOf(enrolled),
                                "completed", String.valueOf(completed)
                        ))
                        .build().children()
        ));
    }

    private void renderPathButton(@NotNull RenderContext render, @NotNull Player player,
                                   @NotNull JExQuests quests, String activeTreeId) {
        final boolean hasActive = activeTreeId != null && !activeTreeId.isBlank();
        render.slot(SLOT_PATH, createItem(
                hasActive ? Material.ENCHANTED_BOOK : Material.BOOK,
                i18n(hasActive ? "path.name" : "path.name_none", player)
                        .withPlaceholder("tree", hasActive ? activeTreeId : "—")
                        .build().component(),
                i18n(hasActive ? "path.lore" : "path.lore_none", player)
                        .withPlaceholder("tree", hasActive ? activeTreeId : "—")
                        .build().children()
        )).onClick(click -> {
            if (!hasActive) {
                click.openForPlayer(RankTreeOverviewView.class,
                        Map.of("plugin", this.plugin.get(click)));
                return;
            }
            final Optional<RankTree> tOpt;
            try {
                tOpt = quests.rankService().trees().findByIdentifierAsync(activeTreeId)
                        .get(2, TimeUnit.SECONDS);
            } catch (final Exception ex) {
                click.closeForPlayer();
                return;
            }
            tOpt.ifPresent(tree -> click.openForPlayer(
                    RankPathOverview.class,
                    Map.of("plugin", this.plugin.get(click), "tree", tree, "previewMode", false)
            ));
        });
    }

    private void renderBrowseButton(@NotNull RenderContext render, @NotNull Player player) {
        render.slot(SLOT_BROWSE, createItem(
                Material.BOOKSHELF,
                i18n("browse.name", player).build().component(),
                i18n("browse.lore", player).build().children()
        )).onClick(click -> click.openForPlayer(
                RankTreeOverviewView.class,
                Map.of("plugin", this.plugin.get(click))
        ));
    }

    private void renderLeaderboardButton(@NotNull RenderContext render, @NotNull Player player,
                                          @NotNull JExQuests quests, String activeTreeId) {
        render.slot(SLOT_LEADERBOARD, createItem(
                Material.GOLDEN_HELMET,
                i18n("leaderboard.name", player).build().component(),
                i18n("leaderboard.lore", player).build().children()
        )).onClick(click -> {
            String tree = activeTreeId;
            if (tree == null || tree.isBlank()) {
                try {
                    final var trees = quests.rankService().trees().findEnabledAsync()
                            .get(2, TimeUnit.SECONDS);
                    if (trees.isEmpty()) { click.closeForPlayer(); return; }
                    tree = trees.get(0).getIdentifier();
                } catch (final Exception ex) {
                    click.closeForPlayer();
                    return;
                }
            }
            click.openForPlayer(RankTopView.class,
                    Map.of("plugin", this.plugin.get(click), "tree", tree));
        });
    }

    /** Prefer the QuestsPlayer.activeRankTree pointer; fall back to any row flagged active. */
    private static PlayerRank resolveActiveRow(@NotNull List<PlayerRank> rows, String activeTreeId) {
        if (activeTreeId != null && !activeTreeId.isBlank()) {
            for (final PlayerRank r : rows) if (r.getTreeIdentifier().equals(activeTreeId)) return r;
        }
        for (final PlayerRank r : rows) if (r.isActive()) return r;
        return null;
    }
}
