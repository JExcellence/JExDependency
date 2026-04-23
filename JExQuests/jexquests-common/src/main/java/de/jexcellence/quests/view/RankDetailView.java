package de.jexcellence.quests.view;

import de.jexcellence.jexplatform.view.BaseView;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.database.entity.PlayerRank;
import de.jexcellence.quests.database.entity.Rank;
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
 * Detail view for a single {@link Rank} — shows the rank's icon with
 * the status banner, a requirement breakdown, a reward breakdown, and
 * an action button that claims the promotion when the rank is the
 * player's direct next step.
 *
 * <p>Opened from both {@link RankPathGridView} and
 * {@link RankPathOverview} when a rank is clicked. Ports the
 * three-view design from RDQ's {@code RankRequirementDetailView} +
 * {@code RankRewardsDetailView} into a single screen so players don't
 * have to click through three menus to understand what they're trying
 * to do.
 *
 * <p>Opens in one of four display modes keyed off the rank's status
 * relative to the viewer:
 * <ul>
 *   <li><b>ACTIVE</b> — "you are here" banner, no action button</li>
 *   <li><b>OWNED</b> — "earned" banner, no action button</li>
 *   <li><b>NEXT</b> — the promotion target; action button offers to
 *       attempt the branching-aware {@code promoteToAsync}</li>
 *   <li><b>LOCKED</b> — "locked" banner, action button disabled</li>
 * </ul>
 */
public class RankDetailView extends BaseView {

    private static final int SLOT_ICON = 13;
    private static final int SLOT_REQUIREMENTS = 20;
    private static final int SLOT_REWARDS = 24;
    private static final int SLOT_ACTION = 40;

    private final State<JExQuests> plugin = initialState("plugin");
    private final State<RankTree> tree = initialState("tree");
    private final State<Rank> rank = initialState("rank");
    private final State<Boolean> previewMode = initialState("previewMode");

    public RankDetailView() {
        super(RankPathGridView.class);
    }

    @Override
    protected String translationKey() {
        return "rank_detail_ui";
    }

    @Override
    protected int size() {
        return 5;
    }

    @Override
    protected String[] layout() {
        return new String[]{
                "         ",
                "    I    ",
                "   R W   ",
                "    A    ",
                "         "
        };
    }

    @Override
    protected @NotNull Map<String, Object> titlePlaceholders(@NotNull me.devnatan.inventoryframework.context.OpenContext open) {
        final Rank r = this.rank.get(open);
        return Map.of("rank", r.getDisplayName());
    }

    @Override
    protected void onRender(@NotNull RenderContext render, @NotNull Player player) {
        final JExQuests quests = this.plugin.get(render);
        final RankTree t = this.tree.get(render);
        final Rank r = this.rank.get(render);
        final boolean preview = Boolean.TRUE.equals(this.previewMode.get(render));

        final Status status = resolveStatus(quests, t, r, player);

        // Rank icon + banner — the top-centre anchor that shows status.
        render.slot(SLOT_ICON, createItem(
                iconFor(status),
                i18n("icon.name." + status.name().toLowerCase(), player)
                        .withPlaceholder("rank", r.getDisplayName())
                        .withPlaceholder("tier", String.valueOf(r.getTier()))
                        .build().component(),
                i18n("icon.lore", player)
                        .withPlaceholders(Map.of(
                                "identifier", r.getIdentifier(),
                                "tier", String.valueOf(r.getTier()),
                                "description", r.getDescription() != null ? r.getDescription() : "—",
                                "state", stateTag(status)
                        ))
                        .build().children()
        ));

        // Requirements breakdown — click to drill into per-predicate icons.
        render.slot(SLOT_REQUIREMENTS, createItem(
                Material.BOOK,
                i18n("requirements.name", player).build().component(),
                i18n("requirements.lore", player)
                        .withPlaceholder("requirements", RequirementDescriber.describe(r.getRequirementData()))
                        .build().children()
        )).onClick(click -> {
            if (r.getRequirementData() == null || r.getRequirementData().isBlank()) return;
            click.openForPlayer(RequirementListView.class, Map.of(
                    "plugin", quests,
                    "requirementData", r.getRequirementData(),
                    "titleContext", r.getDisplayName()
            ));
        });

        // Rewards breakdown — click to drill into per-grant icons.
        render.slot(SLOT_REWARDS, createItem(
                Material.CHEST,
                i18n("rewards.name", player).build().component(),
                i18n("rewards.lore", player)
                        .withPlaceholder("rewards", RewardDescriber.describe(r.getRewardData()))
                        .build().children()
        )).onClick(click -> {
            if (r.getRewardData() == null || r.getRewardData().isBlank()) return;
            click.openForPlayer(RewardListView.class, Map.of(
                    "plugin", quests,
                    "rewardData", r.getRewardData(),
                    "titleContext", r.getDisplayName()
            ));
        });

        // Action button — only live when the rank is actually reachable
        // from the player's current position and we're not previewing.
        final boolean canPromote = !preview && status == Status.NEXT;
        render.slot(SLOT_ACTION, createItem(
                canPromote ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK,
                i18n(canPromote ? "action.promote.name" : "action.locked.name", player)
                        .withPlaceholder("rank", r.getDisplayName())
                        .build().component(),
                i18n(canPromote ? "action.promote.lore" : "action.locked.lore", player)
                        .withPlaceholders(Map.of(
                                "rank", r.getDisplayName(),
                                "state", stateTag(status)
                        ))
                        .build().children()
        )).onClick(click -> {
            if (!canPromote) return;
            quests.rankService()
                    .promoteToAsync(player.getUniqueId(), t.getIdentifier(), r.getIdentifier())
                    .thenAccept(result ->
                            RankPathGridView.handlePromotionResult(quests, player, result));
            click.closeForPlayer();
        });
    }

    // ── status resolution ─────────────────────────────────────────────────

    private @NotNull Status resolveStatus(@NotNull JExQuests quests, @NotNull RankTree tree,
                                           @NotNull Rank rank, @NotNull Player player) {
        try {
            final List<Rank> allRanks = quests.rankService().ranks().findByTreeAsync(tree)
                    .get(2, TimeUnit.SECONDS);
            final Optional<PlayerRank> optPlayer = quests.rankService().playerRankRepository()
                    .findAsync(player.getUniqueId(), tree.getIdentifier())
                    .get(2, TimeUnit.SECONDS);
            if (optPlayer.isEmpty()) {
                return rank.isInitialRank() ? Status.NEXT : Status.LOCKED;
            }
            final String currentId = optPlayer.get().getCurrentRankIdentifier();
            if (rank.getIdentifier().equals(currentId)) return Status.ACTIVE;
            if (rank.previousRankList().contains(currentId)) return Status.NEXT;
            // OWNED = on the ancestor path of the current rank.
            if (isAncestor(rank.getIdentifier(), currentId, allRanks)) return Status.OWNED;
            return Status.LOCKED;
        } catch (final TimeoutException | java.util.concurrent.ExecutionException | InterruptedException ex) {
            return Status.LOCKED;
        }
    }

    private static boolean isAncestor(@NotNull String candidate, @NotNull String descendantId,
                                       @NotNull List<Rank> allRanks) {
        // Walk backwards via previousRanks from descendant until we hit the candidate.
        final java.util.Map<String, Rank> byId = new java.util.HashMap<>();
        for (final Rank r : allRanks) byId.put(r.getIdentifier(), r);
        final java.util.Set<String> visited = new java.util.HashSet<>();
        final java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<>();
        queue.add(descendantId);
        while (!queue.isEmpty()) {
            final String id = queue.poll();
            if (!visited.add(id)) continue;
            final Rank r = byId.get(id);
            if (r == null) continue;
            for (final String prev : r.previousRankList()) {
                if (prev.equals(candidate)) return true;
                queue.add(prev);
            }
        }
        return false;
    }

    // ── icons + tags ──────────────────────────────────────────────────────

    private static @NotNull Material iconFor(@NotNull Status status) {
        return switch (status) {
            case ACTIVE -> Material.EMERALD_BLOCK;
            case OWNED -> Material.GOLD_INGOT;
            case NEXT -> Material.ENDER_EYE;
            case LOCKED -> Material.BARRIER;
        };
    }

    private static @NotNull String stateTag(@NotNull Status status) {
        return switch (status) {
            case ACTIVE -> "<gradient:#86efac:#16a34a>● you are here</gradient>";
            case OWNED -> "<gradient:#86efac:#16a34a>✔ earned</gradient>";
            case NEXT -> "<gradient:#fde047:#f59e0b>▸ next promotion</gradient>";
            case LOCKED -> "<gradient:#fca5a5:#dc2626>✘ locked</gradient>";
        };
    }

    public enum Status { ACTIVE, OWNED, NEXT, LOCKED }
}
