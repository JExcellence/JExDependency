package de.jexcellence.quests.view;

import de.jexcellence.jexplatform.view.BaseView;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.database.entity.PlayerRank;
import de.jexcellence.quests.database.entity.Rank;
import de.jexcellence.quests.database.entity.RankTree;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Vertical tree-grid view — ranks drawn top-down with root at row 0,
 * branching beams between parents and children, three glass-pane
 * connectors between each depth. Ported from RDQ's
 * {@code RankPathOverview} using the same {@code renderWith} / state-
 * watcher pattern, which is the <b>only</b> way to get IF to refresh
 * dynamic slot content as the viewport pans.
 *
 * <p>Each of the 28 grid slots is registered once with a
 * {@link java.util.function.Supplier Supplier} that computes the slot's
 * ItemStack against the current viewport offset at render time. The
 * supplier closure is re-invoked by IF whenever one of the declared
 * watched states changes — here, {@link #viewOffsetX} and
 * {@link #viewOffsetY}. A static {@code render.slot(N, item)} snapshot
 * wouldn't recompute on pan; RDQ found that out the hard way too, which
 * is why {@code bindSlotToDynamicContent} uses {@code renderWith}.
 *
 * <p>Depth spacing: 4 rows. Parent at row {@code py}, three panes at
 * {@code py+1/+2/+3}, child at {@code py+4}. For branches the middle
 * row hosts a horizontal beam spanning parent.x → child.x; straight
 * chains collapse to a single vertical line.
 *
 * <p>X-layout: Reingold–Tilford post-order — leaves claim consecutive
 * columns, parents centre over their children. Primary parent
 * (first {@code previousRanks}) wins for placement; cross-edges still
 * get drawn as connection panes, just not for layout.
 */
public class RankPathGridView extends BaseView {

    private static final int VIEWPORT_WIDTH = 7;
    private static final int VIEWPORT_HEIGHT = 4;
    private static final int GRID_START_ROW = 1;
    private static final int GRID_START_COL = 1;

    private static final int LEAF_SPACING = 2;
    private static final int DEPTH_SPACING = 4;

    private static final int SLOT_BACK = 0;
    private static final int SLOT_UP = 2;
    private static final int SLOT_MODE_TOGGLE = 4;
    private static final int SLOT_RECENTER = 6;
    private static final int SLOT_HELP = 8;
    private static final int SLOT_LEFT = 45;
    private static final int SLOT_DOWN = 47;
    private static final int SLOT_PREVIEW_BADGE = 51;
    private static final int SLOT_RIGHT = 53;

    private final State<JExQuests> plugin = initialState("plugin");
    private final State<RankTree> tree = initialState("tree");
    private final State<Boolean> previewMode = initialState("previewMode");

    /** Viewport offsets — pan buttons mutate these; grid slots watch them. */
    private final MutableState<Integer> viewOffsetX = mutableState(0);
    private final MutableState<Integer> viewOffsetY = mutableState(0);
    /** First-open sentinel so auto-centre runs once, not on every re-render. */
    private final MutableState<Boolean> centred = mutableState(false);

    public RankPathGridView() {
        super(RankTreeOverviewView.class);
    }

    @Override
    protected String translationKey() {
        return "rank_path_grid_ui";
    }

    @Override
    protected int size() {
        return 6;
    }

    @Override
    protected String[] layout() {
        return null;
    }

    @Override
    protected boolean autoFill() {
        return false;
    }

    @Override
    protected @NotNull Map<String, Object> titlePlaceholders(@NotNull me.devnatan.inventoryframework.context.OpenContext open) {
        final RankTree t = this.tree.get(open);
        final boolean preview = Boolean.TRUE.equals(this.previewMode.get(open));
        return Map.of(
                "tree", t.getDisplayName(),
                "mode", preview
                        ? "<gradient:#a5f3fc:#06b6d4>[preview]</gradient>"
                        : "<gradient:#86efac:#16a34a>[active]</gradient>"
        );
    }

    @Override
    protected void onRender(@NotNull RenderContext render, @NotNull Player player) {
        final JExQuests quests = this.plugin.get(render);
        final RankTree t = this.tree.get(render);
        final boolean preview = Boolean.TRUE.equals(this.previewMode.get(render));
        final GridModel model = buildModel(quests, t, player);

        // Auto-centre the first time the view opens: prefer the player's
        // active rank; fall back to the tree's root so the very first rank
        // always appears in the middle of the top row for an unenrolled
        // viewer. Negative offsets are allowed because narrow trees would
        // otherwise sit flush against the left edge of the viewport.
        if (!Boolean.TRUE.equals(this.centred.get(render))) {
            final GridNode focus = model.focusNode() != null ? model.focusNode() : model.rootNode();
            if (focus != null) {
                this.viewOffsetX.set(focus.x() - VIEWPORT_WIDTH / 2, render);
                this.viewOffsetY.set(focus.y(), render);
            }
            this.centred.set(true, render);
        }

        // Bind every viewport slot to a dynamic supplier — IF will call
        // the supplier fresh whenever viewOffsetX/Y change, so panning
        // actually moves the visible content. Static snapshots (via
        // `render.slot(N, stack)`) don't work here — RDQ hit the same
        // wall and switched to renderWith.
        for (int row = 0; row < VIEWPORT_HEIGHT; row++) {
            for (int col = 0; col < VIEWPORT_WIDTH; col++) {
                final int slot = toSlot(col, row);
                final int displayCol = col;
                final int displayRow = row;
                render.slot(slot)
                        .renderWith(() -> computeSlotItem(render, player, model, displayCol, displayRow, preview))
                        .updateOnStateChange(this.viewOffsetX, this.viewOffsetY)
                        .onClick(click -> {
                            final int gridX = displayCol + this.viewOffsetX.get(click);
                            final int gridY = displayRow + this.viewOffsetY.get(click);
                            final GridNode node = model.nodeAt(gridX, gridY);
                            if (node == null) return;
                            // Open the detail view for every rank click — players
                            // need the requirements / rewards breakdown before
                            // they can decide whether to promote. The detail view
                            // itself fires the actual promotion request.
                            click.openForPlayer(RankDetailView.class, Map.of(
                                    "plugin", quests,
                                    "tree", t,
                                    "rank", node.rank(),
                                    "previewMode", preview
                            ));
                        });
            }
        }

        // Fill the rest of the chrome bezel (rows 0 and 5, + unused gaps) with background.
        paintBezel(render, player);
        renderChrome(render, player, quests, t, model, preview);
    }

    // ── slot supplier — runs on every re-render ───────────────────────────

    private @NotNull ItemStack computeSlotItem(@NotNull RenderContext render, @NotNull Player player,
                                                @NotNull GridModel model, int displayCol, int displayRow,
                                                boolean preview) {
        final int offsetX = this.viewOffsetX.get(render);
        final int offsetY = this.viewOffsetY.get(render);
        final int gridX = displayCol + offsetX;
        final int gridY = displayRow + offsetY;

        // Is there a rank at this world position?
        final GridNode node = model.nodeAt(gridX, gridY);
        if (node != null) {
            return createRankItem(player, node.rank(), classify(node.rank(), model));
        }

        // Is this cell on any edge's pane path?
        final EdgePane edgePane = model.edgePaneAt(gridX, gridY);
        if (edgePane != null) {
            return createItem(
                    edgePane.reached() ? Material.LIME_STAINED_GLASS_PANE : Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                    net.kyori.adventure.text.Component.text(" ")
            );
        }

        // Empty cell — transparent filler.
        return createItem(Material.GRAY_STAINED_GLASS_PANE, net.kyori.adventure.text.Component.text(" "));
    }

    private void paintBezel(@NotNull RenderContext render, @NotNull Player player) {
        final var fill = createItem(Material.GRAY_STAINED_GLASS_PANE, net.kyori.adventure.text.Component.text(" "));
        // Row 0 (navigation row) — everything not a specific chrome slot.
        for (int slot = 0; slot < 9; slot++) {
            if (slot == SLOT_BACK || slot == SLOT_UP || slot == SLOT_MODE_TOGGLE
                    || slot == SLOT_RECENTER || slot == SLOT_HELP) continue;
            render.slot(slot, fill);
        }
        // Row 5 — ditto.
        for (int slot = 45; slot < 54; slot++) {
            if (slot == SLOT_LEFT || slot == SLOT_DOWN || slot == SLOT_PREVIEW_BADGE || slot == SLOT_RIGHT) continue;
            render.slot(slot, fill);
        }
        // Column 0 and column 8 of rows 1-4 — viewport bezel.
        for (int row = GRID_START_ROW; row < GRID_START_ROW + VIEWPORT_HEIGHT; row++) {
            render.slot(row * 9, fill);
            render.slot(row * 9 + 8, fill);
        }
    }

    // ── chrome ────────────────────────────────────────────────────────────

    private void renderChrome(@NotNull RenderContext render, @NotNull Player player,
                               @NotNull JExQuests quests, @NotNull RankTree tree,
                               @NotNull GridModel model, boolean preview) {
        render.slot(SLOT_BACK, createItem(
                Material.ARROW,
                i18nWithDefault("back", player).build().component()
        )).onClick(this::handleBack);

        renderPanButton(render, player, SLOT_UP, "nav.up", () ->
                this.viewOffsetY.set(this.viewOffsetY.get(render) - 1, render));
        renderPanButton(render, player, SLOT_DOWN, "nav.down", () ->
                this.viewOffsetY.set(this.viewOffsetY.get(render) + 1, render));
        renderPanButton(render, player, SLOT_LEFT, "nav.left", () ->
                this.viewOffsetX.set(this.viewOffsetX.get(render) - 1, render));
        renderPanButton(render, player, SLOT_RIGHT, "nav.right", () ->
                this.viewOffsetX.set(this.viewOffsetX.get(render) + 1, render));

        render.slot(SLOT_RECENTER, createItem(
                Material.COMPASS,
                i18n("nav.recenter.name", player).build().component(),
                i18n("nav.recenter.lore", player).build().children()
        )).onClick(click -> {
            final GridNode focus = model.focusNode() != null ? model.focusNode() : model.rootNode();
            if (focus != null) {
                this.viewOffsetX.set(focus.x() - VIEWPORT_WIDTH / 2, click);
                this.viewOffsetY.set(focus.y(), click);
            } else {
                this.viewOffsetX.set(0, click);
                this.viewOffsetY.set(0, click);
            }
        });

        render.slot(SLOT_MODE_TOGGLE, createItem(
                Material.KNOWLEDGE_BOOK,
                i18n("toggle.name", player).build().component(),
                i18n("toggle.lore", player).build().children()
        )).onClick(click -> click.openForPlayer(
                RankPathOverview.class,
                Map.of("plugin", quests, "tree", tree, "previewMode", preview)
        ));

        render.slot(SLOT_HELP, createItem(
                Material.PAPER,
                i18n("legend.name", player).build().component(),
                i18n("legend.lore", player).build().children()
        ));

        if (preview) {
            render.slot(SLOT_PREVIEW_BADGE, createItem(
                    Material.SPYGLASS,
                    i18n("preview_badge.name", player).build().component(),
                    i18n("preview_badge.lore", player).build().children()
            ));
        }
    }

    private void renderPanButton(@NotNull RenderContext render, @NotNull Player player,
                                  int slot, @NotNull String i18nSuffix, @NotNull Runnable action) {
        render.slot(slot, createItem(
                Material.SPECTRAL_ARROW,
                i18n(i18nSuffix + ".name", player).build().component(),
                i18n(i18nSuffix + ".lore", player).build().children()
        )).onClick(click -> action.run());
    }

    // ── rank-icon assembly ────────────────────────────────────────────────

    private @NotNull ItemStack createRankItem(@NotNull Player player, @NotNull Rank rank, @NotNull RankStatus status) {
        return createItem(
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
        );
    }

    // ── data-load + classification ────────────────────────────────────────

    private @NotNull GridModel buildModel(@NotNull JExQuests quests, @NotNull RankTree t, @NotNull Player player) {
        try {
            final var ranks = quests.rankService().ranks().findByTreeAsync(t)
                    .get(2, TimeUnit.SECONDS);
            final Optional<PlayerRank> playerRank = quests.rankService().playerRankRepository()
                    .findAsync(player.getUniqueId(), t.getIdentifier())
                    .get(2, TimeUnit.SECONDS);
            return GridModel.build(ranks, playerRank.orElse(null));
        } catch (final TimeoutException | java.util.concurrent.ExecutionException | InterruptedException ex) {
            return GridModel.EMPTY;
        }
    }

    private @NotNull RankStatus classify(@NotNull Rank rank, @NotNull GridModel model) {
        if (model.currentRankIdentifier() == null) {
            return rank.isInitialRank() ? RankStatus.NEXT : RankStatus.LOCKED;
        }
        if (rank.getIdentifier().equals(model.currentRankIdentifier())) return RankStatus.ACTIVE;
        if (model.isOwned(rank.getIdentifier())) return RankStatus.OWNED;
        if (model.isNextFrontier(rank.getIdentifier())) return RankStatus.NEXT;
        return RankStatus.LOCKED;
    }

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

    /**
     * Route a {@link de.jexcellence.quests.service.RankService.PromotionResult}
     * back to the player — success, missing requirements, tree completed,
     * or hard error each get a distinct translated line. Without this the
     * promote click looked silent and players thought nothing happened.
     */
    static void handlePromotionResult(
            @NotNull de.jexcellence.quests.JExQuests quests,
            @NotNull Player player,
            @NotNull de.jexcellence.quests.service.RankService.PromotionResult result
    ) {
        final var r18n = de.jexcellence.jextranslate.R18nManager.getInstance();
        org.bukkit.Bukkit.getScheduler().runTask(quests.getPlugin(), () -> {
            switch (result.status()) {
                case PROMOTED -> r18n.msg("rank.promoted").prefix()
                        .with("rank", result.rank() != null ? result.rank() : "—")
                        .with("rewards", "—")
                        .send(player);
                case REQUIREMENTS_NOT_MET -> r18n.msg("rank.requirements-missing").prefix()
                        .with("requirements", "see /rank info").send(player);
                case TREE_COMPLETED -> r18n.msg("rank.max-reached").prefix().send(player);
                case NOT_ENROLLED, NOT_FOUND -> r18n.msg("rank.path-not-found").prefix()
                        .with("path", "(current)").send(player);
                case ERROR -> r18n.msg("error.unknown").prefix()
                        .with("error", result.error() != null ? result.error() : "unknown")
                        .send(player);
            }
        });
    }

    private static @NotNull String hintFor(@NotNull RankStatus status) {
        return switch (status) {
            case NEXT -> "<gradient:#a5f3fc:#06b6d4>▸ Click to attempt promotion</gradient>";
            case ACTIVE -> "<gray>This is your current rank.";
            case OWNED -> "<gray>You have already earned this rank.";
            case LOCKED -> "<gray>Progress further to unlock.";
        };
    }

    // ── slot maths ────────────────────────────────────────────────────────

    private static int toSlot(int displayCol, int displayRow) {
        return (GRID_START_ROW + displayRow) * 9 + (GRID_START_COL + displayCol);
    }

    public enum RankStatus { ACTIVE, OWNED, NEXT, LOCKED }

    // ── grid model ────────────────────────────────────────────────────────

    /**
     * Placement computed once per open. Reverse-index lookup at slot
     * render time is {@code O(1)} via precomputed maps for nodes and
     * edge panes — critical because the renderWith supplier can fire
     * many times per second during rapid panning.
     */
    private record GridModel(
            @NotNull Map<String, GridNode> nodes,
            @NotNull Map<Long, String> rankByPosition,
            @NotNull Map<Long, EdgePane> edgePaneByPosition,
            @Nullable String currentRankIdentifier,
            @NotNull Set<String> ownedIdentifiers,
            @NotNull Set<String> nextFrontier,
            @Nullable GridNode focusNode
    ) {
        static final GridModel EMPTY = new GridModel(
                Map.of(), Map.of(), Map.of(), null, Set.of(), Set.of(), null);

        boolean isOwned(@NotNull String id) { return this.ownedIdentifiers.contains(id); }
        boolean isNextFrontier(@NotNull String id) { return this.nextFrontier.contains(id); }

        /** The first root node (depth 0, leftmost) — used as a viewport anchor when no player progression exists. */
        @Nullable GridNode rootNode() {
            return this.nodes.values().stream()
                    .filter(n -> n.y() == 0)
                    .min(java.util.Comparator.comparingInt(GridNode::x))
                    .orElse(null);
        }

        @Nullable GridNode nodeAt(int x, int y) {
            final String id = this.rankByPosition.get(pack(x, y));
            return id != null ? this.nodes.get(id) : null;
        }

        @Nullable EdgePane edgePaneAt(int x, int y) {
            return this.edgePaneByPosition.get(pack(x, y));
        }

        static @NotNull GridModel build(@NotNull List<Rank> ranks, @Nullable PlayerRank playerRank) {
            if (ranks.isEmpty()) return EMPTY;

            final Map<String, Rank> byId = new HashMap<>();
            for (final Rank r : ranks) byId.put(r.getIdentifier(), r);

            // Identify roots: isInitialRank=true OR not referenced by any nextRanks edge.
            final Set<String> referenced = new HashSet<>();
            for (final Rank r : ranks) referenced.addAll(r.nextRankList());
            final List<Rank> roots = ranks.stream()
                    .filter(r -> r.isInitialRank() || !referenced.contains(r.getIdentifier()))
                    .sorted(java.util.Comparator.comparingInt(Rank::getOrderIndex))
                    .toList();
            final List<Rank> seeds = roots.isEmpty()
                    ? List.of(ranks.stream().min(java.util.Comparator.comparingInt(Rank::getOrderIndex))
                            .orElseThrow())
                    : roots;

            // Depth via BFS — longest path wins, so a multi-parent node lands at the deepest tier.
            final Map<String, Integer> depth = new HashMap<>();
            final java.util.ArrayDeque<String> q = new java.util.ArrayDeque<>();
            for (final Rank root : seeds) { depth.put(root.getIdentifier(), 0); q.add(root.getIdentifier()); }
            while (!q.isEmpty()) {
                final String id = q.poll();
                final int d = depth.get(id);
                final Rank r = byId.get(id);
                if (r == null) continue;
                for (final String childId : r.nextRankList()) {
                    if (!byId.containsKey(childId)) continue;
                    final Integer existing = depth.get(childId);
                    if (existing == null || existing < d + 1) {
                        depth.put(childId, d + 1);
                        q.add(childId);
                    }
                }
            }
            for (final Rank r : ranks) depth.putIfAbsent(r.getIdentifier(), 0);

            // Primary-parent resolution for R-T: first previousRanks entry wins
            // (falls back to first rank whose nextRanks contains the child when no
            // previousRanks are declared).
            final Map<String, String> primaryParent = new HashMap<>();
            for (final Rank r : ranks) {
                if (!r.previousRankList().isEmpty()) {
                    primaryParent.put(r.getIdentifier(), r.previousRankList().get(0));
                }
            }
            for (final Rank r : ranks) {
                for (final String childId : r.nextRankList()) {
                    primaryParent.putIfAbsent(childId, r.getIdentifier());
                }
            }
            final Map<String, List<String>> primaryChildren = new HashMap<>();
            for (final Rank r : ranks) {
                final List<String> kept = new ArrayList<>();
                for (final String childId : r.nextRankList()) {
                    if (r.getIdentifier().equals(primaryParent.get(childId))) kept.add(childId);
                }
                primaryChildren.put(r.getIdentifier(), kept);
            }

            // Reingold–Tilford post-order: leaves first, each inner node at the
            // midpoint of its children's x range. Produces correctly-centred
            // parents out of the box for tree-shaped hierarchies.
            final Map<String, Integer> xByNode = new HashMap<>();
            int nextX = 0;
            for (final Rank root : seeds) {
                nextX = layoutX(root.getIdentifier(), nextX, primaryChildren, xByNode);
                nextX += LEAF_SPACING;
            }

            // Multi-parent fix: champion in the Warrior tree has parents
            // [berserker, guardian, gladiator] and under R-T it ends up under
            // berserker alone. Shift such nodes (and their R-T descendants) to the
            // mean-x of ALL their parents so they look centred under the fanout.
            // Walk depth-ascending so a shifted parent's delta cascades into its
            // children when we process the next layer.
            final List<String> multiParentOrder = ranks.stream()
                    .filter(r -> r.previousRankList().size() > 1)
                    .sorted(java.util.Comparator.comparingInt(r -> depth.getOrDefault(r.getIdentifier(), 0)))
                    .map(Rank::getIdentifier)
                    .toList();
            for (final String multiParentId : multiParentOrder) {
                final Rank r = byId.get(multiParentId);
                if (r == null) continue;
                double sum = 0;
                int count = 0;
                for (final String parentId : r.previousRankList()) {
                    final Integer px = xByNode.get(parentId);
                    if (px != null) { sum += px; count++; }
                }
                if (count <= 1) continue;
                final int idealX = (int) Math.round(sum / count);
                final int currentX = xByNode.getOrDefault(multiParentId, idealX);
                final int delta = idealX - currentX;
                if (delta == 0) continue;
                shiftSubtree(multiParentId, delta, primaryChildren, xByNode);
            }

            // Shift so the minimum x is 0 — prevents negative columns in storage
            // (the viewport offset can still go negative for centring).
            int minX = xByNode.values().stream().mapToInt(Integer::intValue).min().orElse(0);
            if (minX < 0) {
                final int shift = -minX;
                xByNode.replaceAll((k, v) -> v + shift);
            }

            final Map<String, GridNode> nodes = new HashMap<>();
            final Map<Long, String> rankByPosition = new HashMap<>();
            for (final Rank r : ranks) {
                final int x = xByNode.getOrDefault(r.getIdentifier(), 0);
                final int y = depth.getOrDefault(r.getIdentifier(), 0) * DEPTH_SPACING;
                final GridNode node = new GridNode(r, x, y);
                nodes.put(r.getIdentifier(), node);
                rankByPosition.put(pack(x, y), r.getIdentifier());
            }

            // Ownership walk.
            final Set<String> owned = new HashSet<>();
            final Set<String> next = new HashSet<>();
            final String currentId = playerRank != null ? playerRank.getCurrentRankIdentifier() : null;
            if (currentId != null) {
                final java.util.ArrayDeque<String> back = new java.util.ArrayDeque<>();
                back.add(currentId);
                while (!back.isEmpty()) {
                    final String id = back.poll();
                    if (!owned.add(id)) continue;
                    final Rank r = byId.get(id);
                    if (r != null) back.addAll(r.previousRankList());
                }
                owned.remove(currentId);
                final Rank current = byId.get(currentId);
                if (current != null) next.addAll(current.nextRankList());
            }

            // Precompute every edge-pane cell → "reached" flag, with
            // rank-cell avoidance baked in. Reverse-lookup during slot
            // supplier evaluation is O(1).
            final Map<Long, EdgePane> edgePanes = new HashMap<>();
            for (final Rank r : ranks) {
                final GridNode from = nodes.get(r.getIdentifier());
                if (from == null) continue;
                final boolean reached = owned.contains(r.getIdentifier())
                        || r.getIdentifier().equals(currentId);
                for (final String nextId : r.nextRankList()) {
                    final GridNode to = nodes.get(nextId);
                    if (to == null) continue;
                    plotEdge(edgePanes, rankByPosition, from, to, reached);
                }
            }

            final GridNode focus = currentId != null ? nodes.get(currentId) : null;
            return new GridModel(
                    Collections.unmodifiableMap(nodes),
                    Collections.unmodifiableMap(rankByPosition),
                    Collections.unmodifiableMap(edgePanes),
                    currentId,
                    Collections.unmodifiableSet(owned),
                    Collections.unmodifiableSet(next),
                    focus
            );
        }

        /**
         * Plot the canonical parent→child connector: drop below parent,
         * horizontal beam at mid-row for branches, drop above child.
         * Never paints on a rank cell (rankByPosition lookup).
         */
        private static void plotEdge(@NotNull Map<Long, EdgePane> out,
                                      @NotNull Map<Long, String> rankByPosition,
                                      @NotNull GridNode from, @NotNull GridNode to, boolean reached) {
            final int px = from.x(), py = from.y();
            final int cx = to.x(), cy = to.y();
            placePane(out, rankByPosition, px, py + 1, reached);
            placePane(out, rankByPosition, cx, cy - 1, reached);

            if (px == cx) {
                for (int y = py + 2; y <= cy - 2; y++) placePane(out, rankByPosition, px, y, reached);
                return;
            }
            final int beamY = (py + cy) / 2;
            for (int x = Math.min(px, cx); x <= Math.max(px, cx); x++) {
                placePane(out, rankByPosition, x, beamY, reached);
            }
            for (int y = py + 2; y < beamY; y++) placePane(out, rankByPosition, px, y, reached);
            for (int y = beamY + 1; y <= cy - 2; y++) placePane(out, rankByPosition, cx, y, reached);
        }

        private static void placePane(@NotNull Map<Long, EdgePane> out,
                                       @NotNull Map<Long, String> rankByPosition,
                                       int x, int y, boolean reached) {
            final long key = pack(x, y);
            if (rankByPosition.containsKey(key)) return;
            // OR-merge reached flags: once any edge reaches this cell, it stays lime.
            final EdgePane existing = out.get(key);
            out.put(key, new EdgePane(reached || (existing != null && existing.reached())));
        }

        /**
         * Classic post-order Reingold–Tilford: each leaf takes one
         * {@value #LEAF_SPACING}-wide column, each inner node centres over
         * the midpoint of its first and last primary child. Returns the
         * next free x-slot after the subtree rooted at {@code nodeId}.
         */
        private static int layoutX(@NotNull String nodeId, int startX,
                                    @NotNull Map<String, List<String>> primaryChildren,
                                    @NotNull Map<String, Integer> xByNode) {
            final List<String> children = primaryChildren.getOrDefault(nodeId, List.of());
            if (children.isEmpty()) {
                xByNode.put(nodeId, startX);
                return startX + LEAF_SPACING;
            }
            int cursor = startX;
            for (final String childId : children) {
                cursor = layoutX(childId, cursor, primaryChildren, xByNode);
            }
            final int firstChildX = xByNode.get(children.get(0));
            final int lastChildX = xByNode.get(children.get(children.size() - 1));
            xByNode.put(nodeId, (firstChildX + lastChildX) / 2);
            return cursor;
        }

        /**
         * Shift a node and every R-T descendant (via {@link #primaryChildren})
         * by {@code delta} columns. Used by the multi-parent fixup: when a
         * node like {@code champion} is relocated to the barycentre of its
         * parents, anything it roots in the primary tree (e.g. {@code warlord})
         * moves with it so the chain stays straight.
         */
        private static void shiftSubtree(@NotNull String nodeId, int delta,
                                          @NotNull Map<String, List<String>> primaryChildren,
                                          @NotNull Map<String, Integer> xByNode) {
            final java.util.ArrayDeque<String> stack = new java.util.ArrayDeque<>();
            stack.push(nodeId);
            while (!stack.isEmpty()) {
                final String id = stack.pop();
                final Integer x = xByNode.get(id);
                if (x == null) continue;
                xByNode.put(id, x + delta);
                for (final String childId : primaryChildren.getOrDefault(id, List.of())) {
                    stack.push(childId);
                }
            }
        }

        private static long pack(int x, int y) {
            return (((long) x) << 32) | (y & 0xffffffffL);
        }
    }

    private record GridNode(@NotNull Rank rank, int x, int y) {
    }

    private record EdgePane(boolean reached) {
    }
}
