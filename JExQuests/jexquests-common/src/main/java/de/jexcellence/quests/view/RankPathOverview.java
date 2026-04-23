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
 * Compact rank view — a paginated, flat list of every rank in the tree
 * ordered by {@code orderIndex}. Each page shows up to 27 ranks
 * (3×9 grid); the framework's built-in arrows advance automatically
 * when the rank count exceeds what fits on one page.
 *
 * <p>This is the linear alternative to the scroll-pan
 * {@link RankPathGridView} — cheaper to read at a glance and always
 * shows every rank regardless of tree shape. Players who want to see
 * branch structure hit the Tree View toggle at slot 49.
 *
 * <p>Per-rank classification mirrors the grid view:
 * <ul>
 *   <li><b>ACTIVE</b> — player's current rank (emerald)</li>
 *   <li><b>OWNED</b> — earlier rank on the same branch (tier ingot)</li>
 *   <li><b>NEXT</b> — direct next promotion (ender eye, clickable)</li>
 *   <li><b>LOCKED</b> — not yet reachable (barrier)</li>
 * </ul>
 */
public class RankPathOverview extends PaginatedView<RankPathOverview.Entry> {

    private final State<JExQuests> plugin = initialState("plugin");
    private final State<RankTree> tree = initialState("tree");
    private final State<Boolean> previewMode = initialState("previewMode");

    public RankPathOverview() {
        super(RankTreeOverviewView.class);
    }

    @Override
    protected String translationKey() {
        return "rank_path_overview_ui";
    }

    @Override
    protected String[] layout() {
        // 3 rows of rank slots (27 entries/page), paginator at mid-bottom,
        // back button bottom-left, mode toggle bottom-right.
        return new String[]{
                "         ",
                "OOOOOOOOO",
                "OOOOOOOOO",
                "OOOOOOOOO",
                "   <p>  m"
        };
    }

    @Override
    protected @NotNull CompletableFuture<List<Entry>> loadData(@NotNull Context ctx) {
        final JExQuests quests = this.plugin.get(ctx);
        final RankTree t = this.tree.get(ctx);
        final var playerUuid = ctx.getPlayer().getUniqueId();

        final var ranksFuture = quests.rankService().ranks().findByTreeAsync(t);
        final var playerRankFuture = quests.rankService().playerRankRepository()
                .findAsync(playerUuid, t.getIdentifier());

        return ranksFuture.thenCombine(playerRankFuture, (ranks, optPlayerRank) -> {
            final List<Rank> ordered = ranks.stream()
                    .sorted(Comparator.comparingInt(Rank::getTier)
                            .thenComparingInt(Rank::getOrderIndex)
                            .thenComparing(Rank::getIdentifier))
                    .toList();
            final Classifier classifier = Classifier.build(ordered, optPlayerRank.orElse(null));
            return ordered.stream()
                    .map(rank -> new Entry(rank, classifier.classify(rank)))
                    .toList();
        }).exceptionally(ex -> List.of());
    }

    @Override
    protected void onPaginatedRender(@NotNull me.devnatan.inventoryframework.context.RenderContext render,
                                      @NotNull org.bukkit.entity.Player player) {
        final JExQuests quests = this.plugin.get(render);
        final RankTree t = this.tree.get(render);
        final boolean preview = Boolean.TRUE.equals(this.previewMode.get(render));

        // Mode toggle at the 'm' layout char — jumps to the tree view
        // on the same tree, carrying the preview/active mode through.
        render.layoutSlot('m', createItem(
                Material.KNOWLEDGE_BOOK,
                i18n("toggle.name", player).build().component(),
                i18n("toggle.lore", player).build().children()
        )).onClick(click -> click.openForPlayer(
                RankPathGridView.class,
                Map.of("plugin", quests, "tree", t, "previewMode", preview)
        ));
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
        final boolean preview = Boolean.TRUE.equals(this.previewMode.get(ctx));
        final Rank rank = entry.rank();
        final RankStatus status = entry.status();

        builder.withItem(createItem(
                iconFor(rank, status),
                i18n("rank." + status.name().toLowerCase() + ".name", player)
                        .withPlaceholder("rank", rank.getDisplayName())
                        .withPlaceholder("tier", String.valueOf(rank.getTier()))
                        .build().component(),
                i18n("rank.lore", player)
                        .withPlaceholders(Map.of(
                                "identifier", rank.getIdentifier(),
                                "description", rank.getDescription() != null ? rank.getDescription() : "—",
                                "tier", String.valueOf(rank.getTier()),
                                "state", stateTag(status),
                                "requirements", RequirementDescriber.describe(rank.getRequirementData()),
                                "rewards", RewardDescriber.describe(rank.getRewardData()),
                                "action_hint", hintFor(status)
                        ))
                        .build().children()
        )).onClick(click -> click.openForPlayer(RankDetailView.class, Map.of(
                "plugin", quests,
                "tree", t,
                "rank", rank,
                "previewMode", preview
        )));
    }

    // ── icons + text ──────────────────────────────────────────────────────

    private static @NotNull Material iconFor(@NotNull Rank rank, @NotNull RankStatus status) {
        return switch (status) {
            case ACTIVE -> Material.EMERALD_BLOCK;
            case OWNED -> materialByTier(rank.getTier());
            case NEXT -> Material.ENDER_EYE;
            case LOCKED -> Material.BARRIER;
        };
    }

    private static @NotNull Material materialByTier(int tier) {
        return switch (tier) {
            case 1 -> Material.IRON_INGOT;
            case 2 -> Material.GOLD_INGOT;
            case 3 -> Material.DIAMOND;
            case 4 -> Material.EMERALD;
            default -> Material.NETHERITE_INGOT;
        };
    }

    private static @NotNull String stateTag(@NotNull RankStatus status) {
        return switch (status) {
            case ACTIVE -> "<gradient:#86efac:#16a34a>● you are here</gradient>";
            case OWNED -> "<gradient:#86efac:#16a34a>✔ earned</gradient>";
            case NEXT -> "<gradient:#fde047:#f59e0b>▸ next promotion</gradient>";
            case LOCKED -> "<gradient:#fca5a5:#dc2626>✘ locked</gradient>";
        };
    }

    private static @NotNull String hintFor(@NotNull RankStatus status) {
        return switch (status) {
            case NEXT -> "<gradient:#a5f3fc:#06b6d4>▸ Click to attempt promotion</gradient>";
            case ACTIVE -> "<gray>This is your current rank.";
            case OWNED -> "<gray>You have already earned this rank.";
            case LOCKED -> "<gray>Progress further to unlock.";
        };
    }

    public record Entry(@NotNull Rank rank, @NotNull RankStatus status) {
    }

    public enum RankStatus { ACTIVE, OWNED, NEXT, LOCKED }

    /**
     * Status classification shared with the grid view — a rank is
     * OWNED if it's on the ancestor path of the player's current rank
     * (walk backwards via {@code previousRanks}), ACTIVE if it <em>is</em>
     * the current rank, NEXT if it's a direct child of the current,
     * and LOCKED otherwise.
     */
    private record Classifier(
            @org.jetbrains.annotations.Nullable String currentId,
            @NotNull java.util.Set<String> owned,
            @NotNull java.util.Set<String> next
    ) {
        RankStatus classify(@NotNull Rank rank) {
            if (this.currentId == null) {
                return rank.isInitialRank() ? RankStatus.NEXT : RankStatus.LOCKED;
            }
            if (rank.getIdentifier().equals(this.currentId)) return RankStatus.ACTIVE;
            if (this.owned.contains(rank.getIdentifier())) return RankStatus.OWNED;
            if (this.next.contains(rank.getIdentifier())) return RankStatus.NEXT;
            return RankStatus.LOCKED;
        }

        static @NotNull Classifier build(@NotNull List<Rank> ranks, @org.jetbrains.annotations.Nullable PlayerRank playerRank) {
            final String currentId = playerRank != null ? playerRank.getCurrentRankIdentifier() : null;
            if (currentId == null) return new Classifier(null, java.util.Set.of(), java.util.Set.of());

            final Map<String, Rank> byId = new java.util.HashMap<>();
            for (final Rank r : ranks) byId.put(r.getIdentifier(), r);

            final java.util.Set<String> owned = new java.util.HashSet<>();
            final java.util.ArrayDeque<String> back = new java.util.ArrayDeque<>();
            back.add(currentId);
            while (!back.isEmpty()) {
                final String id = back.poll();
                if (!owned.add(id)) continue;
                final Rank r = byId.get(id);
                if (r != null) back.addAll(r.previousRankList());
            }
            owned.remove(currentId);

            final java.util.Set<String> next = new java.util.HashSet<>();
            final Rank current = byId.get(currentId);
            if (current != null) next.addAll(current.nextRankList());

            return new Classifier(currentId, java.util.Set.copyOf(owned), java.util.Set.copyOf(next));
        }
    }
}
