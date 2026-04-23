package de.jexcellence.quests.view;

import de.jexcellence.jexplatform.view.PaginatedView;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.database.entity.RankTree;
import de.jexcellence.quests.service.RankPathService;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Rank tree catalogue — every enabled tree rendered with its
 * {@link RankPathService.Availability} classification. Ports RDQ's
 * dual-click UX:
 * <ul>
 *   <li><b>Left click</b> — open {@link RankPathOverview} in preview
 *       mode (no promotion actions, just the grid)</li>
 *   <li><b>Right click</b> — select the tree as the active path (via
 *       {@link RankPathService#selectRankPath}); on success reopens
 *       the grid in interactive mode</li>
 * </ul>
 */
public class RankTreeOverviewView extends PaginatedView<RankTreeOverviewView.Entry> {

    private final State<JExQuests> plugin = initialState("plugin");

    public RankTreeOverviewView() {
        super(RankMainView.class);
    }

    @Override
    protected String translationKey() {
        return "rank_tree_overview_ui";
    }

    @Override
    protected String[] layout() {
        return new String[]{
                "         ",
                "  OOOOO  ",
                "  OOOOO  ",
                "         ",
                "   <p>   "
        };
    }

    @Override
    protected @NotNull CompletableFuture<List<Entry>> loadData(@NotNull Context ctx) {
        final JExQuests quests = this.plugin.get(ctx);
        final var player = ctx.getPlayer();

        return quests.rankService().trees().findEnabledAsync().thenCompose(trees -> {
            final List<RankTree> sorted = trees.stream()
                    .sorted(Comparator.comparingInt(RankTree::getDisplayOrder))
                    .toList();
            final List<CompletableFuture<RankPathService.Availability>> futures = sorted.stream()
                    .map(tree -> quests.rankPathService().availabilityAsync(
                            player.getUniqueId(), tree.getIdentifier()))
                    .toList();
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(v -> {
                final List<Entry> entries = new java.util.ArrayList<>(sorted.size());
                for (int i = 0; i < sorted.size(); i++) {
                    entries.add(new Entry(sorted.get(i),
                            futures.get(i).getNow(RankPathService.Availability.AVAILABLE)));
                }
                return entries;
            });
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
        final RankTree tree = entry.tree();
        final RankPathService.Availability availability = entry.availability();

        builder.withItem(createItem(
                iconFor(tree, availability),
                i18n("entry.name", player)
                        .withPlaceholder("tree", tree.getDisplayName())
                        .withPlaceholder("state", tagFor(availability))
                        .build().component(),
                i18n("entry.lore", player)
                        .withPlaceholders(Map.of(
                                "identifier", tree.getIdentifier(),
                                "description", tree.getDescription() != null ? tree.getDescription() : "—",
                                "state", tagFor(availability),
                                "action_hint", hintFor(availability)
                        ))
                        .build().children()
        )).onClick(click -> {
            final ClickType type = click.getClickOrigin().getClick();
            switch (availability) {
                case AVAILABLE -> {
                    if (type == ClickType.RIGHT) {
                        click.openForPlayer(RankPathOverview.class,
                                Map.of("plugin", quests, "tree", tree, "previewMode", true));
                    } else {
                        quests.rankPathService().selectRankPath(player.getUniqueId(), tree.getIdentifier())
                                .thenAccept(result -> handleSelectResult(click, quests, tree, player, result));
                    }
                }
                case CURRENTLY_ACTIVE -> click.openForPlayer(RankPathOverview.class,
                        Map.of("plugin", quests, "tree", tree, "previewMode", false));
                case PREVIOUSLY_SELECTED -> {
                    if (type == ClickType.RIGHT) {
                        click.openForPlayer(RankPathOverview.class,
                                Map.of("plugin", quests, "tree", tree, "previewMode", true));
                    } else {
                        quests.rankPathService().switchRankPath(player.getUniqueId(), tree.getIdentifier())
                                .thenAccept(result -> handleSelectResult(click, quests, tree, player, result));
                    }
                }
                case COMPLETED, LOCKED, DISABLED, NOT_FOUND -> click.openForPlayer(RankPathOverview.class,
                        Map.of("plugin", quests, "tree", tree, "previewMode", true));
            }
        });
    }

    /**
     * Route every status of a {@link RankPathService.SelectResult} back to
     * the player. On {@code SELECTED} we bounce them into the interactive
     * grid; every other status sends a translated error line so the click
     * never looks silently ignored. Runs on the main Bukkit thread because
     * it may open another view, which has to happen there.
     */
    private static void handleSelectResult(
            @NotNull me.devnatan.inventoryframework.context.SlotClickContext click,
            @NotNull de.jexcellence.quests.JExQuests quests,
            @NotNull RankTree tree,
            @NotNull org.bukkit.entity.Player player,
            @NotNull RankPathService.SelectResult result
    ) {
        final var r18n = de.jexcellence.jextranslate.R18nManager.getInstance();
        org.bukkit.Bukkit.getScheduler().runTask(quests.getPlugin(), () -> {
            switch (result.status()) {
                case SELECTED -> {
                    r18n.msg("rank.path-selected").prefix()
                            .with("path", tree.getIdentifier())
                            .with("rank", result.rankIdentifier() != null ? result.rankIdentifier() : "—")
                            .send(player);
                    click.openForPlayer(RankPathOverview.class,
                            Map.of("plugin", quests, "tree", tree, "previewMode", false));
                }
                case ALREADY_ACTIVE -> r18n.msg("rank.path-already-active").prefix()
                        .with("path", tree.getIdentifier()).send(player);
                case LOCKED_MINIMUM, LOCKED_PREREQUISITE -> r18n.msg("rank.path-locked").prefix()
                        .with("path", tree.getIdentifier())
                        .with("reason", result.detail() != null ? result.detail() : "requirements not met")
                        .send(player);
                case ON_COOLDOWN -> r18n.msg("rank.path-cooldown").prefix()
                        .with("seconds", String.valueOf(result.cooldownSecondsRemaining())).send(player);
                case SWITCHING_LOCKED -> r18n.msg("rank.path-switch-locked").prefix()
                        .with("path", tree.getIdentifier()).send(player);
                case NO_INITIAL_RANK, NOT_FOUND, DISABLED -> r18n.msg("rank.path-not-found").prefix()
                        .with("path", tree.getIdentifier()).send(player);
                case ERROR -> r18n.msg("error.unknown").prefix()
                        .with("error", result.detail() != null ? result.detail() : "unknown").send(player);
            }
        });
    }

    private static @NotNull Material iconFor(@NotNull RankTree tree, @NotNull RankPathService.Availability availability) {
        if (availability == RankPathService.Availability.LOCKED) return Material.IRON_BARS;
        if (availability == RankPathService.Availability.DISABLED) return Material.BARRIER;
        if (availability == RankPathService.Availability.COMPLETED) return Material.NETHER_STAR;
        if (availability == RankPathService.Availability.CURRENTLY_ACTIVE) return Material.EMERALD;
        if (availability == RankPathService.Availability.PREVIOUSLY_SELECTED) return Material.GOLD_INGOT;
        if (tree.getIconMaterial() != null) {
            try { return Material.valueOf(tree.getIconMaterial().toUpperCase()); }
            catch (final IllegalArgumentException ignored) { /* fall through */ }
        }
        return Material.ENCHANTED_BOOK;
    }

    private static @NotNull String tagFor(@NotNull RankPathService.Availability availability) {
        return switch (availability) {
            case CURRENTLY_ACTIVE -> "<gradient:#86efac:#16a34a>★ active</gradient>";
            case PREVIOUSLY_SELECTED -> "<gradient:#fde047:#f59e0b>▸ paused</gradient>";
            case COMPLETED -> "<gradient:#d8b4fe:#9333ea>✔ completed</gradient>";
            case AVAILABLE -> "<gradient:#a5f3fc:#06b6d4>○ available</gradient>";
            case LOCKED -> "<gradient:#fca5a5:#dc2626>🔒 locked</gradient>";
            case DISABLED -> "<gradient:#64748b:#334155>✘ disabled</gradient>";
            case NOT_FOUND -> "<gradient:#64748b:#334155>? unknown</gradient>";
        };
    }

    private static @NotNull String hintFor(@NotNull RankPathService.Availability availability) {
        return switch (availability) {
            case AVAILABLE -> "<gradient:#a5f3fc:#06b6d4>▸ Left-click to select · Right-click to preview</gradient>";
            case CURRENTLY_ACTIVE -> "<gradient:#a5f3fc:#06b6d4>▸ Click to open your path</gradient>";
            case PREVIOUSLY_SELECTED -> "<gradient:#a5f3fc:#06b6d4>▸ Left-click to reactivate · Right-click to preview</gradient>";
            case COMPLETED -> "<gradient:#d8b4fe:#9333ea>▸ Click to review your achievement</gradient>";
            case LOCKED -> "<gradient:#fca5a5:#dc2626>▸ Complete prerequisites to unlock</gradient>";
            case DISABLED, NOT_FOUND -> "<gray>—";
        };
    }

    /** Tuple of tree + per-viewer availability classification. */
    public record Entry(@NotNull RankTree tree, @NotNull RankPathService.Availability availability) {
    }
}
