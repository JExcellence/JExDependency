package de.jexcellence.quests.view;

import de.jexcellence.jexplatform.view.BaseView;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.database.entity.PlayerQuestProgress;
import de.jexcellence.quests.database.entity.QuestStatus;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Quest dashboard — the landing view opened by {@code /quest}. Three
 * primary actions mirror {@link RankMainView}'s shape so the player's
 * UX is consistent across the two systems:
 *
 * <ul>
 *   <li><b>Active</b> — drills into the quest browser pre-filtered
 *       to in-progress quests</li>
 *   <li><b>Browse</b> — every quest the player can see, regardless
 *       of status</li>
 *   <li><b>Completed</b> — a quick archive of finished quests</li>
 * </ul>
 *
 * <p>A summary head at slot 4 shows running counts (active / completed
 * / available) so the player gets an at-a-glance progress digest without
 * clicking anything.
 */
public class QuestMainView extends BaseView {

    private static final int SLOT_SUMMARY = 4;
    private static final int SLOT_ACTIVE = 11;
    private static final int SLOT_BROWSE = 13;
    private static final int SLOT_COMPLETED = 15;

    private final State<JExQuests> plugin = initialState("plugin");

    public QuestMainView() {
        super();
    }

    @Override
    protected String translationKey() {
        return "quest_main_ui";
    }

    @Override
    protected int size() {
        return 3;
    }

    @Override
    protected String[] layout() {
        return new String[]{
                "    S    ",
                "  A B C  ",
                "         "
        };
    }

    @Override
    protected void onRender(@NotNull RenderContext render, @NotNull Player player) {
        final JExQuests quests = this.plugin.get(render);

        final List<PlayerQuestProgress> rows = loadProgressRows(quests, player);
        final long active = rows.stream().filter(r -> r.getStatus() == QuestStatus.ACTIVE).count();
        final long completed = rows.stream().filter(r -> r.getStatus() == QuestStatus.COMPLETED).count();
        final long abandoned = rows.stream().filter(r -> r.getStatus() == QuestStatus.ABANDONED).count();

        render.slot(SLOT_SUMMARY, createItem(
                Material.PLAYER_HEAD,
                i18n("summary.name", player)
                        .withPlaceholder("player", player.getName())
                        .build().component(),
                i18n("summary.lore", player)
                        .withPlaceholders(Map.of(
                                "active", String.valueOf(active),
                                "completed", String.valueOf(completed),
                                "abandoned", String.valueOf(abandoned)
                        ))
                        .build().children()
        ));

        render.slot(SLOT_ACTIVE, createItem(
                Material.WRITABLE_BOOK,
                i18n("active.name", player).build().component(),
                i18n("active.lore", player)
                        .withPlaceholder("count", String.valueOf(active))
                        .build().children()
        )).onClick(click -> click.openForPlayer(
                QuestOverviewView.class,
                Map.of("plugin", this.plugin.get(click), "filter", "active")
        ));

        render.slot(SLOT_BROWSE, createItem(
                Material.BOOKSHELF,
                i18n("browse.name", player).build().component(),
                i18n("browse.lore", player).build().children()
        )).onClick(click -> click.openForPlayer(
                QuestOverviewView.class,
                Map.of("plugin", this.plugin.get(click), "filter", "all")
        ));

        render.slot(SLOT_COMPLETED, createItem(
                Material.KNOWLEDGE_BOOK,
                i18n("completed.name", player).build().component(),
                i18n("completed.lore", player)
                        .withPlaceholder("count", String.valueOf(completed))
                        .build().children()
        )).onClick(click -> click.openForPlayer(
                QuestOverviewView.class,
                Map.of("plugin", this.plugin.get(click), "filter", "completed")
        ));
    }

    /** Synchronous fetch with a 2s timeout — matches the pattern used in RankMainView. */
    private @NotNull List<PlayerQuestProgress> loadProgressRows(@NotNull JExQuests quests, @NotNull Player player) {
        try {
            return quests.questService().questProgress()
                    .findByPlayerAsync(player.getUniqueId())
                    .get(2, TimeUnit.SECONDS);
        } catch (final TimeoutException | java.util.concurrent.ExecutionException | InterruptedException ex) {
            return List.of();
        }
    }
}
