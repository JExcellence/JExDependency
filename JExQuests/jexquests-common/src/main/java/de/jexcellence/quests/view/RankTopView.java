package de.jexcellence.quests.view;

import de.jexcellence.jexplatform.view.PaginatedView;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.api.RankLeaderboardEntry;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.state.State;
import me.devnatan.inventoryframework.state.StateValueHost;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated rank leaderboard. Opened from {@link RankTreeDetailView}
 * (or the {@code /rank top <tree>} command) with the tree identifier
 * passed through {@link State#get(StateValueHost)}.
 */
public class RankTopView extends PaginatedView<RankLeaderboardEntry> {

    private final State<JExQuests> plugin = initialState("plugin");
    private final State<String> tree = initialState("tree");

    public RankTopView() {
        super();
    }

    @Override
    protected String translationKey() {
        return "rank_top_ui";
    }

    @Override
    protected String[] layout() {
        return new String[]{
                "         ",
                "OOOOOOOOO",
                "OOOOOOOOO",
                "OOOOOOOOO",
                "   <p>   "
        };
    }

    @Override
    protected @NotNull CompletableFuture<List<RankLeaderboardEntry>> loadData(@NotNull Context ctx) {
        final String treeId = this.tree.get(ctx);
        final int hardLimit = 100;
        return this.plugin.get(ctx)
                .rankService()
                .topAsync(treeId, hardLimit)
                .exceptionally(ex -> List.of());
    }

    @Override
    protected void renderItem(
            @NotNull Context ctx,
            @NotNull BukkitItemComponentBuilder builder,
            int index,
            @NotNull RankLeaderboardEntry entry
    ) {
        final var player = ctx.getPlayer();
        final Material icon = iconFor(entry.position());

        builder.withItem(createItem(
                icon,
                i18n("entry.name", player)
                        .withPlaceholder("position", entry.position())
                        .withPlaceholder("player", entry.playerName())
                        .build().component(),
                i18n("entry.lore", player)
                        .withPlaceholders(Map.of(
                                "rank", entry.rankIdentifier(),
                                "tree", entry.treeIdentifier(),
                                "ordinal", String.valueOf(entry.rankOrderIndex()),
                                "promoted", entry.promotedAt().toString(),
                                "tree_completed", entry.treeCompleted() ? "★" : "—",
                                "index", entry.position()
                        ))
                        .build().children()
        ));
    }

    private static @NotNull Material iconFor(int position) {
        return switch (position) {
            case 1 -> Material.GOLD_BLOCK;
            case 2 -> Material.IRON_BLOCK;
            case 3 -> Material.COPPER_BLOCK;
            default -> Material.STONE;
        };
    }
}
