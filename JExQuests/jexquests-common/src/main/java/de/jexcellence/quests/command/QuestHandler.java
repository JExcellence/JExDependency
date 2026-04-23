package de.jexcellence.quests.command;

import com.raindropcentral.commands.v2.CommandContext;
import com.raindropcentral.commands.v2.CommandHandler;
import de.jexcellence.jextranslate.R18nManager;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.database.entity.PlayerQuestProgress;
import de.jexcellence.quests.database.entity.PlayerTaskProgress;
import de.jexcellence.quests.database.entity.QuestsPlayer;
import de.jexcellence.quests.service.QuestService;
import de.jexcellence.quests.service.QuestsPlayerService;
import de.jexcellence.quests.view.QuestOverviewView;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * JExCommand 2.0 handlers for {@code /quest}. Thin — every substantive
 * operation delegates to {@link QuestService}. Error cases branch on
 * the sealed result enum so messages always match state.
 */
public final class QuestHandler {

    private final JExQuests quests;
    private final QuestService questService;
    private final QuestsPlayerService questsPlayers;

    public QuestHandler(@NotNull JExQuests quests) {
        this.quests = quests;
        this.questService = quests.questService();
        this.questsPlayers = quests.questsPlayerService();
    }

    public @NotNull Map<String, CommandHandler> handlerMap() {
        return Map.ofEntries(
                // Bare `/quest` now lands on the dashboard hub (same as /quest list).
                // Previously it had no handler and just printed the usage line.
                Map.entry("quest", this::onList),
                Map.entry("quest.list", this::onList),
                Map.entry("quest.info", this::onInfo),
                Map.entry("quest.accept", this::onAccept),
                Map.entry("quest.abandon", this::onAbandon),
                Map.entry("quest.track", this::onTrack),
                Map.entry("quest.progress", this::onProgress),
                Map.entry("quest.sidebar", this::onSidebar)
        );
    }

    private void onSidebar(@NotNull CommandContext ctx) {
        final Player player = ctx.asPlayer().orElseThrow();
        final String state = ctx.get("state", String.class).orElse("toggle").toLowerCase(java.util.Locale.ROOT);
        final boolean enable = switch (state) {
            case "on", "true", "enable" -> true;
            case "off", "false", "disable" -> false;
            default -> {
                // Toggle: flip whatever the persisted value is.
                yield this.quests.questsPlayerService().findAsync(player.getUniqueId())
                        .join()
                        .map(row -> !row.isQuestSidebarEnabled())
                        .orElse(true);
            }
        };
        this.quests.questSidebarService().toggle(player, enable);
        r18n().msg(enable ? "quest.sidebar.enabled" : "quest.sidebar.disabled").prefix().send(player);
    }

    private void onList(@NotNull CommandContext ctx) {
        final Player player = ctx.asPlayer().orElseThrow();
        // Route to the dashboard hub; from there the player can drill into
        // active / browse-all / completed filters. The old direct-to-overview
        // path skipped the dashboard and always showed every quest unfiltered,
        // which users reported as "miserably designed".
        this.quests.viewFrame().open(
                de.jexcellence.quests.view.QuestMainView.class,
                player,
                Map.of("plugin", this.quests)
        );
    }

    /** Legacy chat-based active quest listing, reachable via {@code /quest progress} paths. */
    @SuppressWarnings("unused")
    private void listActiveInChat(@NotNull Player player) {
        this.questService.activeForPlayerAsync(player.getUniqueId()).thenAccept(active -> {
            if (active.isEmpty()) {
                r18n().msg("quest.none-active").prefix().send(player);
                return;
            }
            for (final PlayerQuestProgress row : active) {
                r18n().msg("quest.list.entry").prefix()
                        .with("quest", row.getQuestIdentifier())
                        .with("status", statusTag(row.getStatus().name()))
                        .with("category", "—")
                        .send(player);
            }
        });
    }

    /**
     * {@code /quest info <quest>} — prints quest definition + the
     * viewer's live progress on every task. The old implementation
     * was a stub that just echoed the quest id with dashes. This one
     * actually queries the {@code jexquests_quest} and
     * {@code jexquests_player_task_progress} tables so players can
     * see what they need to do and how close they are.
     */
    private void onInfo(@NotNull CommandContext ctx) {
        final String questId = ctx.require("quest", String.class);
        final var sender = ctx.sender();
        final java.util.UUID viewerUuid = ctx.asPlayer()
                .map(org.bukkit.entity.Player::getUniqueId)
                .orElse(null);

        this.questService.quests().findByIdentifierAsync(questId).thenAccept(optQuest -> {
            if (optQuest.isEmpty()) {
                r18n().msg("quest.not-found").prefix().with("quest", questId).send(sender);
                return;
            }
            final var quest = optQuest.get();
            r18n().msg("quest.header").send(sender);
            r18n().msg("quest.info.summary").prefix()
                    .with("quest", quest.getIdentifier())
                    .with("category", quest.getCategory())
                    .with("difficulty", quest.getDifficulty().name())
                    .with("repeatable", Boolean.toString(quest.isRepeatable()))
                    .with("rewards", de.jexcellence.quests.view.RewardDescriber.describe(quest.getRewardData()))
                    .with("requirements",
                            de.jexcellence.quests.view.RequirementDescriber.describe(quest.getRequirementData()))
                    .send(sender);

            // Per-task breakdown with live progress when the sender is a player.
            this.questService.tasks().findByQuestAsync(quest).thenAccept(tasks -> {
                if (tasks.isEmpty()) {
                    r18n().msg("quest.info.no-tasks").prefix().send(sender);
                    return;
                }
                if (viewerUuid == null) {
                    // Console listing — objective only, no progress.
                    for (final var task : tasks) {
                        r18n().msg("quest.info.task").prefix()
                                .with("task", task.getTaskIdentifier())
                                .with("progress", "—")
                                .with("target", resolveTargetText(task))
                                .with("percent", "—")
                                .send(sender);
                    }
                    return;
                }
                this.questService.taskProgress()
                        .findByPlayerAndQuestAsync(viewerUuid, quest.getIdentifier())
                        .thenAccept(progressRows -> {
                            final java.util.Map<String, de.jexcellence.quests.database.entity.PlayerTaskProgress> byId =
                                    new java.util.HashMap<>();
                            for (final var row : progressRows) byId.put(row.getTaskIdentifier(), row);
                            for (final var task : tasks) {
                                final var p = byId.get(task.getTaskIdentifier());
                                r18n().msg("quest.info.task").prefix()
                                        .with("task", task.getTaskIdentifier())
                                        .with("progress", p != null ? String.valueOf(p.getProgress()) : "0")
                                        .with("target", resolveTargetText(task))
                                        .with("percent", p != null ? String.valueOf(p.percent()) : "0")
                                        .send(sender);
                            }
                        });
            });
        });
    }

    /** Short objective target label — "15× ZOMBIE", falls back to the raw number for custom objectives. */
    private static @NotNull String resolveTargetText(@NotNull de.jexcellence.quests.database.entity.QuestTask task) {
        final var objective = de.jexcellence.quests.service.QuestObjectiveCodec.decode(task.getObjectiveData());
        if (objective == null) return "—";
        return switch (objective) {
            case de.jexcellence.quests.api.QuestObjective.EntityKill k -> k.target() + "× " + k.entityType();
            case de.jexcellence.quests.api.QuestObjective.BlockBreak b -> b.target() + "× " + b.material();
            case de.jexcellence.quests.api.QuestObjective.BlockPlace p -> p.target() + "× " + p.material();
            case de.jexcellence.quests.api.QuestObjective.ItemCraft c -> c.target() + "× " + c.material();
            case de.jexcellence.quests.api.QuestObjective.ItemPickup p -> p.target() + "× " + p.material();
            case de.jexcellence.quests.api.QuestObjective.PlayerJoin j -> j.target() + " login(s)";
            case de.jexcellence.quests.api.QuestObjective.Custom c -> c.target() + " " + c.type();
        };
    }

    private void onAccept(@NotNull CommandContext ctx) {
        final Player player = ctx.asPlayer().orElseThrow();
        final String quest = ctx.require("quest", String.class);
        this.questService.acceptAsync(player.getUniqueId(), quest).thenAccept(result -> {
            switch (result) {
                case ACCEPTED -> r18n().msg("quest.accepted").prefix().with("quest", quest).send(player);
                case NOT_FOUND -> r18n().msg("quest.not-found").prefix().with("quest", quest).send(player);
                case DISABLED -> r18n().msg("error.system-disabled").prefix().with("system", "quest " + quest).send(player);
                case ALREADY_ACTIVE -> r18n().msg("quest.already-active").prefix().with("quest", quest).send(player);
                case ALREADY_COMPLETED -> r18n().msg("quest.already-completed").prefix().with("quest", quest).send(player);
                case REQUIREMENTS_NOT_MET -> r18n().msg("quest.locked").prefix()
                        .with("quest", quest).with("requirements", "see /quest info").send(player);
                case ERROR -> r18n().msg("error.unknown").prefix().with("error", "accept failed").send(player);
            }
        });
    }

    private void onAbandon(@NotNull CommandContext ctx) {
        final Player player = ctx.asPlayer().orElseThrow();
        final String quest = ctx.require("quest", String.class);
        this.questService.abandonAsync(player.getUniqueId(), quest).thenAccept(ok -> {
            if (ok) r18n().msg("quest.abandoned").prefix().with("quest", quest).send(player);
            else r18n().msg("quest.not-found").prefix().with("quest", quest).send(player);
        });
    }

    private void onTrack(@NotNull CommandContext ctx) {
        final Player player = ctx.asPlayer().orElseThrow();
        final String quest = ctx.get("quest", String.class).orElse(null);
        this.questsPlayers.trackAsync(player.getUniqueId()).thenAccept(row -> {
            if (row == null) return;
            row.setTrackedQuestIdentifier(quest);
            this.questsPlayers.repository().update(row);
            if (quest == null) r18n().msg("quest.tracking-cleared").prefix().send(player);
            else r18n().msg("quest.tracking-set").prefix().with("quest", quest).send(player);
        });
    }

    private void onProgress(@NotNull CommandContext ctx) {
        final Player player = ctx.asPlayer().orElseThrow();
        final String explicit = ctx.get("quest", String.class).orElse(null);
        if (explicit != null) {
            emitProgress(player, explicit);
            return;
        }
        this.questsPlayers.findAsync(player.getUniqueId()).thenAccept(opt -> {
            final QuestsPlayer row = opt.orElse(null);
            final String tracked = row != null ? row.getTrackedQuestIdentifier() : null;
            if (tracked == null) {
                r18n().msg("quest.none-active").prefix().send(player);
                return;
            }
            emitProgress(player, tracked);
        });
    }

    private void emitProgress(@NotNull Player player, @NotNull String quest) {
        this.questService.taskProgressAsync(player.getUniqueId(), quest).thenAccept(rows -> {
            r18n().msg("quest.progress-header").prefix().with("quest", quest).send(player);
            if (rows.isEmpty()) {
                r18n().msg("quest.none-active").prefix().send(player);
                return;
            }
            for (final PlayerTaskProgress row : rows) {
                r18n().msg("quest.progress-line").prefix()
                        .with("objective", row.getTaskIdentifier())
                        .with("current", String.valueOf(row.getProgress()))
                        .with("target", String.valueOf(row.getTarget()))
                        .with("percent", String.valueOf(row.percent()))
                        .send(player);
            }
        });
    }

    private static String statusTag(String status) {
        return switch (status) {
            case "ACTIVE" -> "<gradient:#86efac:#16a34a>active</gradient>";
            case "COMPLETED" -> "<gradient:#d8b4fe:#9333ea>done</gradient>";
            case "FAILED", "EXPIRED" -> "<gradient:#fca5a5:#dc2626>lost</gradient>";
            case "ABANDONED" -> "<gray>abandoned";
            default -> "<gradient:#a5f3fc:#06b6d4>available</gradient>";
        };
    }

    private static R18nManager r18n() { return R18nManager.getInstance(); }
}
