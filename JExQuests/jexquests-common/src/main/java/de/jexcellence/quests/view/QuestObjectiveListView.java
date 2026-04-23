package de.jexcellence.quests.view;

import de.jexcellence.jexplatform.view.PaginatedView;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.api.QuestObjective;
import de.jexcellence.quests.database.entity.PlayerTaskProgress;
import de.jexcellence.quests.database.entity.Quest;
import de.jexcellence.quests.database.entity.QuestTask;
import de.jexcellence.quests.service.QuestObjectiveCodec;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Per-task objective breakdown — opened from {@link QuestDetailView}
 * when the player wants to see <em>what</em> the quest asks them to
 * do, not just its top-line metadata. Each task renders as one tile
 * with:
 *
 * <ul>
 *   <li>icon matching the objective kind (sword for entity-kill,
 *       pickaxe for block-break, crafting-table for item-craft, etc.)</li>
 *   <li>the task's display name and identifier</li>
 *   <li>the objective target (e.g. "kill 15 zombies")</li>
 *   <li><b>live progress</b> from the player's
 *       {@link PlayerTaskProgress} row — {@code 7/15} with a percent
 *       — when the quest is active; otherwise "not started"</li>
 * </ul>
 *
 * <p>This is the view the user wanted when they said "requirements
 * not visible" — quest-level requirements are the gate to <em>accept</em>
 * the quest; task-level objectives are what the player needs to
 * <em>do</em> to finish it. Those were never surfaced before.
 */
public class QuestObjectiveListView extends PaginatedView<QuestObjectiveListView.Entry> {

    private final State<JExQuests> plugin = initialState("plugin");
    private final State<Quest> quest = initialState("quest");

    public QuestObjectiveListView() {
        super(QuestDetailView.class);
    }

    @Override
    protected String translationKey() {
        return "quest_objective_list_ui";
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
    protected @NotNull Map<String, Object> titlePlaceholders(@NotNull me.devnatan.inventoryframework.context.OpenContext open) {
        final Quest q = this.quest.get(open);
        return Map.of("quest", q.getDisplayName());
    }

    @Override
    protected @NotNull CompletableFuture<List<Entry>> loadData(@NotNull Context ctx) {
        final JExQuests quests = this.plugin.get(ctx);
        final Quest q = this.quest.get(ctx);
        final var playerUuid = ctx.getPlayer().getUniqueId();

        return quests.questService().tasks().findByQuestAsync(q)
                .thenCombine(
                        quests.questService().taskProgress().findByPlayerAndQuestAsync(playerUuid, q.getIdentifier()),
                        (tasks, progress) -> {
                            final Map<String, PlayerTaskProgress> progressById = new HashMap<>();
                            for (final PlayerTaskProgress p : progress) progressById.put(p.getTaskIdentifier(), p);
                            return tasks.stream()
                                    .sorted(Comparator.comparingInt(QuestTask::getOrderIndex))
                                    .map(task -> new Entry(task, progressById.get(task.getTaskIdentifier())))
                                    .toList();
                        })
                .exceptionally(ex -> List.of());
    }

    @Override
    protected void renderItem(
            @NotNull Context ctx,
            @NotNull BukkitItemComponentBuilder builder,
            int index,
            @NotNull Entry entry
    ) {
        final var player = ctx.getPlayer();
        final QuestTask task = entry.task();
        final PlayerTaskProgress progress = entry.progress();
        final QuestObjective objective = decode(task);
        final Material icon = iconFor(objective, progress != null && progress.isCompleted());
        final String kind = kindOf(objective);
        final String targetText = targetText(objective);

        builder.withItem(createItem(
                icon,
                i18n("entry.name", player)
                        .withPlaceholder("task", task.getDisplayName())
                        .withPlaceholder("state", stateTag(progress))
                        .build().component(),
                i18n("entry.lore", player)
                        .withPlaceholders(Map.of(
                                "identifier", task.getTaskIdentifier(),
                                "kind", kind,
                                "target", targetText,
                                "state", stateTag(progress),
                                "progress", progress != null ? String.valueOf(progress.getProgress()) : "0",
                                "maximum", progress != null
                                        ? String.valueOf(progress.getTarget())
                                        : (objective != null ? String.valueOf(objective.target()) : "?"),
                                "percent", progress != null ? String.valueOf(progress.percent()) : "0"
                        ))
                        .build().children()
        ));
    }

    // ── decoding + formatting ─────────────────────────────────────────────

    private static QuestObjective decode(@NotNull QuestTask task) {
        try {
            return QuestObjectiveCodec.decode(task.getObjectiveData());
        } catch (final RuntimeException ex) {
            return null;
        }
    }

    private static @NotNull Material iconFor(QuestObjective objective, boolean completed) {
        if (completed) return Material.LIME_STAINED_GLASS_PANE;
        if (objective == null) return Material.PAPER;
        return switch (objective) {
            case QuestObjective.EntityKill ignored -> Material.IRON_SWORD;
            case QuestObjective.BlockBreak ignored -> Material.IRON_PICKAXE;
            case QuestObjective.BlockPlace ignored -> Material.BRICKS;
            case QuestObjective.ItemCraft ignored -> Material.CRAFTING_TABLE;
            case QuestObjective.ItemPickup ignored -> Material.HOPPER;
            case QuestObjective.PlayerJoin ignored -> Material.COMPASS;
            case QuestObjective.Custom ignored -> Material.PAPER;
        };
    }

    private static @NotNull String kindOf(QuestObjective objective) {
        if (objective == null) return "—";
        return switch (objective) {
            case QuestObjective.EntityKill ignored -> "entity-kill";
            case QuestObjective.BlockBreak ignored -> "block-break";
            case QuestObjective.BlockPlace ignored -> "block-place";
            case QuestObjective.ItemCraft ignored -> "item-craft";
            case QuestObjective.ItemPickup ignored -> "item-pickup";
            case QuestObjective.PlayerJoin ignored -> "player-join";
            case QuestObjective.Custom c -> c.type();
        };
    }

    /** Short human-readable target — "15× ZOMBIE", "100× STONE", "1 player join". */
    private static @NotNull String targetText(QuestObjective objective) {
        if (objective == null) return "—";
        return switch (objective) {
            case QuestObjective.EntityKill k -> k.target() + "× " + k.entityType();
            case QuestObjective.BlockBreak b -> b.target() + "× " + b.material();
            case QuestObjective.BlockPlace p -> p.target() + "× " + p.material();
            case QuestObjective.ItemCraft c -> c.target() + "× " + c.material();
            case QuestObjective.ItemPickup p -> p.target() + "× " + p.material();
            case QuestObjective.PlayerJoin j -> j.target() + " login(s)";
            case QuestObjective.Custom c -> c.target() + " " + c.type();
        };
    }

    private static @NotNull String stateTag(PlayerTaskProgress progress) {
        if (progress == null) return "<gradient:#64748b:#334155>○ not started</gradient>";
        if (progress.isCompleted()) return "<gradient:#86efac:#16a34a>✔ complete</gradient>";
        return "<gradient:#fde047:#f59e0b>▸ " + progress.percent() + "%</gradient>";
    }

    public record Entry(@NotNull QuestTask task, PlayerTaskProgress progress) {
    }
}
