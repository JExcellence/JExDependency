package de.jexcellence.quests.view;

import de.jexcellence.jexplatform.view.PaginatedView;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.database.entity.PlayerRank;
import de.jexcellence.quests.database.entity.Rank;
import de.jexcellence.quests.database.entity.RankTree;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Ranks within one {@link RankTree}, rendered with real unlock state
 * resolved from the viewing player's {@link PlayerRank} row. Each rank
 * is one of three visual statuses (see {@link RankStatus}):
 *
 * <ul>
 *   <li><b>PROMOTED</b> — the rank is at or below the player's current
 *       index; enchanted icon, tier-coloured material</li>
 *   <li><b>NEXT</b> — the rank directly after the player's current one;
 *       highlighted as the promotion target, click attempts promotion</li>
 *   <li><b>LOCKED</b> — beyond NEXT; a barrier icon and locked label</li>
 * </ul>
 *
 * <p>The heavy lifting — checking requirement gates, granting rewards
 * — stays in {@link de.jexcellence.quests.service.RankService#promoteAsync};
 * this view is purely presentational plus the click dispatch.
 */
public class RankTreeDetailView extends PaginatedView<RankTreeDetailView.Entry> {

    private final State<JExQuests> plugin = initialState("plugin");
    private final State<RankTree> tree = initialState("tree");

    public RankTreeDetailView() {
        super(RankTreeOverviewView.class);
    }

    @Override
    protected String translationKey() {
        return "rank_tree_detail_ui";
    }

    @Override
    protected String[] layout() {
        return new String[]{
                "         ",
                "OOOOOOOOO",
                "OOOOOOOOO",
                "OOOOOOOOO",
                "   <p>  r"
        };
    }

    @Override
    protected @NotNull CompletableFuture<List<Entry>> loadData(@NotNull Context ctx) {
        final JExQuests quests = this.plugin.get(ctx);
        final RankTree t = this.tree.get(ctx);
        final var playerUuid = ctx.getPlayer().getUniqueId();

        // Resolve the two queries in parallel: full rank list + player's current row.
        final var ranksFuture = quests.rankService().ranks().findByTreeAsync(t);
        final var playerRankFuture = quests.rankService().playerRankRepository()
                .findAsync(playerUuid, t.getIdentifier());

        return ranksFuture.thenCombine(playerRankFuture, (ranks, optPlayerRank) -> {
            final List<Rank> ordered = ranks.stream()
                    .sorted(Comparator.comparingInt(Rank::getOrderIndex))
                    .toList();
            final int currentIdx = resolveCurrentIndex(ordered, optPlayerRank);
            return ordered.stream()
                    .map(rank -> new Entry(rank, classify(rank, ordered, currentIdx)))
                    .toList();
        }).exceptionally(ex -> List.of());
    }

    @Override
    protected void renderItem(
            @NotNull Context ctx,
            @NotNull BukkitItemComponentBuilder builder,
            int index,
            @NotNull Entry entry
    ) {
        final var player = ctx.getPlayer();
        final JExQuests quests = this.plugin.get(ctx);
        final RankTree t = this.tree.get(ctx);
        final Rank rank = entry.rank();
        final RankStatus status = entry.status();

        builder.withItem(createItem(
                iconFor(rank, status),
                i18n("entry.name", player)
                        .withPlaceholder("rank", rank.getDisplayName())
                        .withPlaceholder("state", status.tag())
                        .build().component(),
                i18n("entry.lore", player)
                        .withPlaceholders(Map.of(
                                "identifier", rank.getIdentifier(),
                                "order", String.valueOf(rank.getOrderIndex()),
                                "description", rank.getDescription() != null ? rank.getDescription() : "—",
                                "state", status.tag(),
                                "requirements", RequirementDescriber.describe(rank.getRequirementData()),
                                "rewards", RewardDescriber.describe(rank.getRewardData())
                        ))
                        .build().children()
        )).onClick(click -> {
            if (status == RankStatus.NEXT) {
                quests.rankService().promoteAsync(player.getUniqueId(), t.getIdentifier());
            }
            click.closeForPlayer();
        });
    }

    // ── classification ─────────────────────────────────────────────────────

    /** Find the player's current rank in the ordered list. {@code -1} when not enrolled. */
    private static int resolveCurrentIndex(@NotNull List<Rank> ordered, @NotNull Optional<PlayerRank> playerRank) {
        if (playerRank.isEmpty()) return -1;
        final String currentId = playerRank.get().getCurrentRankIdentifier();
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getIdentifier().equals(currentId)) return i;
        }
        return -1;
    }

    private static @NotNull RankStatus classify(@NotNull Rank rank, @NotNull List<Rank> ordered, int currentIdx) {
        final int myIdx = ordered.indexOf(rank);
        if (currentIdx < 0) {
            // Player hasn't enrolled yet — the very first rank is their NEXT target.
            return myIdx == 0 ? RankStatus.NEXT : RankStatus.LOCKED;
        }
        if (myIdx <= currentIdx) return RankStatus.PROMOTED;
        if (myIdx == currentIdx + 1) return RankStatus.NEXT;
        return RankStatus.LOCKED;
    }

    private static @NotNull Material iconFor(@NotNull Rank rank, @NotNull RankStatus status) {
        if (status == RankStatus.LOCKED) return Material.BARRIER;
        if (status == RankStatus.NEXT) return Material.ENDER_EYE;
        // Tier progression keyed off orderIndex so a rank set can use any
        // depth without the view needing to know about specific trees.
        final int order = rank.getOrderIndex();
        if (order == 0) return Material.IRON_INGOT;
        if (order <= 2) return Material.GOLD_INGOT;
        if (order <= 4) return Material.DIAMOND;
        return Material.NETHERITE_INGOT;
    }

    /** Carries the tuple (rank, visual status) into the paginated data set. */
    public record Entry(@NotNull Rank rank, @NotNull RankStatus status) {
    }

    /** Three-way visual state displayed in the lore + icon choice. */
    public enum RankStatus {
        PROMOTED("<gradient:#86efac:#16a34a>✔ promoted</gradient>"),
        NEXT("<gradient:#fde047:#f59e0b>▸ next</gradient>"),
        LOCKED("<gradient:#fca5a5:#dc2626>✘ locked</gradient>");

        private final String tag;
        RankStatus(@NotNull String tag) { this.tag = tag; }
        public @NotNull String tag() { return this.tag; }
    }
}
