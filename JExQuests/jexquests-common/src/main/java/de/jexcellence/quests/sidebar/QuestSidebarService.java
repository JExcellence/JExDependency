package de.jexcellence.quests.sidebar;

import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.api.event.QuestAcceptedEvent;
import de.jexcellence.quests.api.event.QuestCompletedEvent;
import de.jexcellence.quests.database.entity.PlayerTaskProgress;
import de.jexcellence.quests.database.entity.QuestsPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Per-player sidebar showing the currently tracked quest and its task
 * progress. Install/uninstall is keyed on
 * {@link QuestsPlayer#isQuestSidebarEnabled()} — the toggle persists
 * across sessions and is flipped by {@code /quest sidebar}.
 *
 * <p>Hooks into {@link QuestAcceptedEvent} + {@link QuestCompletedEvent}
 * for reactive refresh and runs a 4-second fallback tick for
 * mid-progress task updates. Uses Bukkit's scoreboard API with
 * per-player {@link Scoreboard} instances to avoid stomping on
 * plugin-provided sidebars.
 */
public final class QuestSidebarService implements Listener {

    private static final long REFRESH_PERIOD_TICKS = 80L;
    private static final int MAX_ENTRIES = 13;
    private static final String OBJECTIVE_KEY = "jexquests_sb";

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final JavaPlugin plugin;
    private final JExQuests quests;
    private final JExLogger logger;
    private final ConcurrentMap<UUID, Scoreboard> active = new ConcurrentHashMap<>();

    private BukkitTask refreshTask;

    public QuestSidebarService(@NotNull JExQuests quests) {
        this.plugin = quests.getPlugin();
        this.quests = quests;
        this.logger = quests.logger();
    }

    /**
     * Starts the sidebar service.
     */
    public void start() {
        if (this.refreshTask != null) return;
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
        this.refreshTask = Bukkit.getScheduler().runTaskTimer(
                this.plugin, this::refreshAll, REFRESH_PERIOD_TICKS, REFRESH_PERIOD_TICKS);
        // Install for already-online players (covers /reload scenarios).
        for (final Player online : Bukkit.getOnlinePlayers()) ensureInstalled(online);
        this.logger.info("Quest sidebar service online");
    }

    /**
     * Stops the sidebar service.
     */
    public void stop() {
        if (this.refreshTask != null) {
            this.refreshTask.cancel();
            this.refreshTask = null;
        }
        for (final UUID uuid : Set.copyOf(this.active.keySet())) removeFor(uuid);
        this.logger.info("Quest sidebar service stopped");
    }

    /** Toggle the sidebar on/off for one player. Persists to {@link QuestsPlayer}. */
    public void toggle(@NotNull Player player, boolean enable) {
        this.quests.questsPlayerService().trackAsync(player.getUniqueId()).thenAccept(row -> {
            if (row == null) return;
            row.setQuestSidebarEnabled(enable);
            this.quests.questsPlayerService().repository().update(row);
            if (enable) Bukkit.getScheduler().runTask(this.plugin, () -> ensureInstalled(player));
            else Bukkit.getScheduler().runTask(this.plugin, () -> removeFor(player.getUniqueId()));
        });
    }

    /**
     * Handles player join to install sidebar if enabled.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        ensureInstalled(event.getPlayer());
    }

    /**
     * Handles player quit to remove sidebar.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        removeFor(event.getPlayer().getUniqueId());
    }

    /**
     * Handles quest accepted event to refresh sidebar.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAccepted(@NotNull QuestAcceptedEvent event) {
        scheduleRefresh(event.playerUuid());
    }

    /**
     * Handles quest completed event to refresh sidebar.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onCompleted(@NotNull QuestCompletedEvent event) {
        scheduleRefresh(event.playerUuid());
    }

    private void ensureInstalled(@NotNull Player player) {
        this.quests.questsPlayerService().findAsync(player.getUniqueId()).thenAccept(opt -> {
            if (opt.isEmpty() || !opt.get().isQuestSidebarEnabled()) return;
            Bukkit.getScheduler().runTask(this.plugin, () -> install(player, opt.get()));
        });
    }

    private void install(@NotNull Player player, @NotNull QuestsPlayer row) {
        final ScoreboardManager mgr = Bukkit.getScoreboardManager();
        if (mgr == null) return;
        final Scoreboard board = mgr.getNewScoreboard();
        final Objective objective = board.registerNewObjective(
                OBJECTIVE_KEY,
                org.bukkit.scoreboard.Criteria.DUMMY,
                title());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(board);
        this.active.put(player.getUniqueId(), board);
        populate(player, board, objective, row);
    }

    private void scheduleRefresh(@NotNull UUID uuid) {
        Bukkit.getScheduler().runTask(this.plugin, () -> refreshFor(uuid));
    }

    private void refreshAll() {
        for (final UUID uuid : this.active.keySet()) refreshFor(uuid);
    }

    private void refreshFor(@NotNull UUID uuid) {
        final Scoreboard board = this.active.get(uuid);
        if (board == null) return;
        final Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            this.active.remove(uuid);
            return;
        }
        this.quests.questsPlayerService().findAsync(uuid).thenAccept(opt -> {
            if (opt.isEmpty() || !opt.get().isQuestSidebarEnabled()) {
                Bukkit.getScheduler().runTask(this.plugin, () -> removeFor(uuid));
                return;
            }
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                final Objective objective = board.getObjective(OBJECTIVE_KEY);
                if (objective == null) return;
                populate(player, board, objective, opt.get());
            });
        });
    }

    private void populate(
            @NotNull Player player,
            @NotNull Scoreboard board,
            @NotNull Objective objective,
            @NotNull QuestsPlayer row
    ) {
        final String tracked = row.getTrackedQuestIdentifier();
        if (tracked == null || tracked.isBlank()) {
            clearEntries(board, objective);
            setLine(board, objective, 1, mini("sidebar.no-tracking"));
            return;
        }
        // Don't paint a partial "header-only" frame here — that's what
        // caused the flicker-to-empty-state. Fetch first, then render the
        // full snapshot in one atomic main-thread pass via renderProgress.
        this.quests.questService().taskProgressAsync(player.getUniqueId(), tracked).thenAccept(rows ->
                Bukkit.getScheduler().runTask(this.plugin, () -> renderProgress(board, objective, tracked, rows)));
    }

    private void renderProgress(
            @NotNull Scoreboard board,
            @NotNull Objective objective,
            @NotNull String questIdentifier,
            @NotNull List<PlayerTaskProgress> rows
    ) {
        clearEntries(board, objective);
        setLine(board, objective, 99, miniWith("sidebar.tracked-header", "quest", questIdentifier));
        if (rows.isEmpty()) {
            setLine(board, objective, 1, mini("sidebar.no-tracking"));
            return;
        }
        int score = 98;
        int count = 0;
        for (final PlayerTaskProgress task : rows) {
            if (count++ >= MAX_ENTRIES) break;
            final String line = renderTaskLine(task);
            setLine(board, objective, score--, line);
        }
    }

    private @NotNull String renderTaskLine(@NotNull PlayerTaskProgress task) {
        final String status = task.isCompleted()
                ? "<gradient:#86efac:#16a34a>✔</gradient>"
                : "<gray>" + task.percent() + "%";
        final String mini = "<gray>" + task.getTaskIdentifier()
                + " <dark_gray>»</dark_gray> " + status
                + " <dark_gray>(</dark_gray><white>" + task.getProgress()
                + "<dark_gray>/</dark_gray><white>" + task.getTarget() + "</white><dark_gray>)</dark_gray>";
        return LEGACY.serialize(MINI.deserialize(mini));
    }

    private void removeFor(@NotNull UUID uuid) {
        final Scoreboard removed = this.active.remove(uuid);
        if (removed == null) return;
        final Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            final ScoreboardManager mgr = Bukkit.getScoreboardManager();
            if (mgr != null) player.setScoreboard(mgr.getMainScoreboard());
        }
        final Objective objective = removed.getObjective(OBJECTIVE_KEY);
        if (objective != null) objective.unregister();
    }

    private void clearEntries(@NotNull Scoreboard board, @NotNull Objective objective) {
        for (final String entry : board.getEntries()) board.resetScores(entry);
        // Teams linger between refreshes — unregister the managed ones.
        for (final Team team : board.getTeams()) {
            if (team.getName().startsWith("jq_line_")) team.unregister();
        }
    }

    /** Adds one line at {@code score} using a unique invisible-prefix trick so duplicates are allowed. */
    private void setLine(
            @NotNull Scoreboard board,
            @NotNull Objective objective,
            int score,
            @NotNull String legacyText
    ) {
        final String teamName = "jq_line_" + score;
        Team team = board.getTeam(teamName);
        if (team == null) team = board.registerNewTeam(teamName);
        final String anchor = invisible(score);

        // Use Adventure's Component-aware prefix setter — the legacy
        // String-based setPrefix caps at 40 chars which was causing
        // `0/15` to be rendered as `/108` (the split landed inside a
        // §X escape and the serialiser ate the 1). Component-API has
        // no length limit on modern Paper.
        final Component component = LEGACY.deserialize(legacyText);
        team.prefix(component);
        team.suffix(Component.empty());

        if (!team.hasEntry(anchor)) team.addEntry(anchor);
        objective.getScore(anchor).setScore(score);
    }

    private static @NotNull String invisible(int score) {
        // Each score gets a unique zero-width entry so sidebar rows don't collide.
        return "§" + Integer.toHexString(score & 0xf) + "§r§" + Integer.toHexString((score >> 4) & 0xf);
    }

    private @NotNull Component title() {
        return MINI.deserialize("<gradient:#fde047:#f59e0b>JExQuests</gradient>");
    }

    private @NotNull String mini(@NotNull String key) {
        return LEGACY.serialize(MINI.deserialize(resolve(key)));
    }

    private @NotNull String miniWith(@NotNull String key, @NotNull String k, @NotNull String v) {
        return LEGACY.serialize(MINI.deserialize(resolve(key).replace("{" + k + "}", v)));
    }

    private @NotNull String resolve(@NotNull String key) {
        return switch (key) {
            case "sidebar.no-tracking" -> "<gray>(nothing tracked)";
            case "sidebar.tracked-header" -> "<dark_gray>» <gradient:#fde047:#f59e0b>{quest}</gradient>";
            default -> key;
        };
    }

    /** Exposed for tests — package-private in intent. */
    int activeCount() {
        return this.active.size();
    }

    private static final class Set {
        private Set() {
        }
        static <T> java.util.Set<T> copyOf(@NotNull java.util.Set<T> src) {
            return java.util.Set.copyOf(src);
        }
    }
}
