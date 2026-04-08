package com.raindropcentral.rdq.quest.sidebar;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.quest.QuestUser;
import de.jexcellence.jextranslate.i18n.I18n;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages a sidebar scoreboard displaying a player's active quest progress.
 *
 * <p>The service refreshes every {@value UPDATE_PERIOD_TICKS} ticks and shows
 * all active quest names. Players are automatically enrolled when they start a
 * quest and removed when they quit the server.</p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public final class QuestProgressSidebarService {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN_TEXT_SERIALIZER = PlainTextComponentSerializer.plainText();
    private static final long UPDATE_PERIOD_TICKS = 40L;
    private static final int MAX_SCOREBOARD_LINES = 15;
    private static final String OBJECTIVE_NAME = "rdqquests";
    private static final char COLOR_CHAR = '\u00A7';

    /** Unique colour-code entries used as scoreboard line placeholders (one per line slot). */
    private static final List<String> LINE_ENTRIES = List.of(
        "\u00A70", "\u00A71", "\u00A72", "\u00A73", "\u00A74",
        "\u00A75", "\u00A76", "\u00A77", "\u00A78", "\u00A79",
        "\u00A7a", "\u00A7b", "\u00A7c", "\u00A7d", "\u00A7e"
    );

    private final RDQ plugin;
    private final Map<UUID, SidebarState> activeSidebars = new ConcurrentHashMap<>();

    /**
     * Creates the quest progress sidebar service.
     *
     * @param plugin active RDQ plugin instance
     */
    public QuestProgressSidebarService(@NotNull final RDQ plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts periodic refreshes for every active quest sidebar.
     */
    public void start() {
        this.plugin.getPlatform().getScheduler().runRepeating(
            this::refreshActivePlayers,
            UPDATE_PERIOD_TICKS,
            UPDATE_PERIOD_TICKS
        );
    }

    /**
     * Disables all active quest sidebars and restores previous scoreboards.
     */
    public void shutdown() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            this.disable(player);
        }
        this.activeSidebars.clear();
    }

    /**
     * Enables the quest progress sidebar for a player.
     * If the player already has an active sidebar, it is refreshed immediately.
     *
     * @param player the player to show the sidebar to
     */
    public void enable(@NotNull final Player player) {
        final SidebarState existingState = this.activeSidebars.get(player.getUniqueId());
        if (existingState != null) {
            player.setScoreboard(existingState.sidebarScoreboard());
            this.refreshPlayer(player, existingState);
            return;
        }

        final Scoreboard sidebarScoreboard = this.createSidebarScoreboard();
        final SidebarState state = new SidebarState(player.getScoreboard(), sidebarScoreboard);
        this.activeSidebars.put(player.getUniqueId(), state);
        player.setScoreboard(sidebarScoreboard);
        this.refreshPlayer(player, state);
    }

    /**
     * Disables the quest progress sidebar for a player, restoring their previous scoreboard.
     *
     * @param player the player whose sidebar should be removed
     */
    public void disable(@NotNull final Player player) {
        final SidebarState state = this.activeSidebars.remove(player.getUniqueId());
        if (state == null) {
            return;
        }

        if (player.getScoreboard() == state.sidebarScoreboard()) {
            player.setScoreboard(state.previousScoreboard());
        }
    }

    /**
     * Forces an immediate refresh of the quest sidebar for a specific player.
     * Used when quest progress changes (e.g. task completed, quest started).
     *
     * @param player the player whose sidebar should be refreshed
     */
    public void refresh(@NotNull final Player player) {
        final SidebarState state = this.activeSidebars.get(player.getUniqueId());
        if (state != null) {
            this.refreshPlayer(player, state);
        }
    }

    /**
     * Returns whether the player currently has an active quest progress sidebar.
     *
     * @param player the player to inspect
     * @return {@code true} if the sidebar is active for this player
     */
    public boolean isActive(@NotNull final Player player) {
        return this.activeSidebars.containsKey(player.getUniqueId());
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private void refreshActivePlayers() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            final SidebarState state = this.activeSidebars.get(player.getUniqueId());
            if (state != null) {
                this.refreshPlayer(player, state);
            }
        }
    }

    private void refreshPlayer(@NotNull final Player player, @NotNull final SidebarState state) {
        if (!player.isOnline()) {
            this.activeSidebars.remove(player.getUniqueId());
            return;
        }

        if (player.getScoreboard() != state.sidebarScoreboard()) {
            player.setScoreboard(state.sidebarScoreboard());
        }

        this.renderSidebar(state.sidebarScoreboard(), player, this.buildLines(player));
    }

    @NotNull
    private List<String> buildLines(@NotNull final Player player) {
        final List<QuestUser> activeQuests = this.plugin.getQuestCacheManager()
            .getPlayerQuests(player.getUniqueId());

        final List<String> lines = new ArrayList<>();

        if (activeQuests.isEmpty()) {
            lines.add(this.i18nLine("scoreboard_sidebar.quests.no_active", player, Map.of()));
            return lines;
        }

        // Reserve 1 slot for potential "more..." line
        final int maxQuestLines = MAX_SCOREBOARD_LINES - 1;
        final boolean truncated = activeQuests.size() > maxQuestLines;
        final int listed = truncated ? maxQuestLines : activeQuests.size();

        for (int i = 0; i < listed; i++) {
            final QuestUser questUser = activeQuests.get(i);
            final String questName = this.resolveQuestName(player, questUser);
            lines.add(this.i18nLine(
                "scoreboard_sidebar.quests.quest_entry",
                player,
                Map.of("quest_name", questName)
            ));
        }

        if (truncated) {
            lines.add(this.i18nLine(
                "scoreboard_sidebar.quests.more_quests",
                player,
                Map.of("remaining", activeQuests.size() - listed)
            ));
        }

        return lines;
    }

    @NotNull
    private String resolveQuestName(@NotNull final Player player, @NotNull final QuestUser questUser) {
        try {
            final String nameKey = questUser.getQuest().getIcon().getDisplayNameKey();
            return PLAIN_TEXT_SERIALIZER.serialize(
                new I18n.Builder(nameKey, player).build().component()
            );
        } catch (final Exception e) {
            return questUser.getQuest().getIdentifier();
        }
    }

    @NotNull
    private Scoreboard createSidebarScoreboard() {
        final ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            throw new IllegalStateException("Bukkit scoreboard manager is unavailable");
        }

        final Scoreboard scoreboard = manager.getNewScoreboard();
        final Objective objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, Component.empty());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());

        for (int i = 0; i < LINE_ENTRIES.size(); i++) {
            final Team team = scoreboard.registerNewTeam(this.getTeamName(i));
            team.addEntry(LINE_ENTRIES.get(i));
        }

        return scoreboard;
    }

    private void renderSidebar(
        @NotNull final Scoreboard scoreboard,
        @NotNull final Player player,
        @NotNull final List<String> rawLines
    ) {
        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective == null) {
            objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, Component.empty());
        }

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());
        objective.displayName(
            new I18n.Builder("scoreboard_sidebar.quests.title", player).build().component()
        );

        final List<String> lines = rawLines.size() > MAX_SCOREBOARD_LINES
            ? rawLines.subList(0, MAX_SCOREBOARD_LINES)
            : rawLines;

        for (int index = 0; index < LINE_ENTRIES.size(); index++) {
            final String entry = LINE_ENTRIES.get(index);
            final Team team = this.getOrCreateTeam(scoreboard, index, entry);

            if (index < lines.size()) {
                this.applyLine(team, lines.get(index));
                final Score score = objective.getScore(entry);
                score.numberFormat(NumberFormat.blank());
                score.setScore(lines.size() - index);
            } else {
                team.prefix(Component.empty());
                team.suffix(Component.empty());
                scoreboard.resetScores(entry);
            }
        }
    }

    @NotNull
    private Team getOrCreateTeam(@NotNull final Scoreboard scoreboard, final int index, @NotNull final String entry) {
        final String teamName = this.getTeamName(index);
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        if (!team.hasEntry(entry)) {
            team.addEntry(entry);
        }
        return team;
    }

    private void applyLine(@NotNull final Team team, @NotNull final String line) {
        final String prefix = this.safeSubstring(line, 64);
        final String remainder = line.substring(prefix.length());
        if (remainder.isEmpty()) {
            team.prefix(this.fromLegacy(prefix));
            team.suffix(Component.empty());
            return;
        }
        final String suffix = this.safeSubstring(this.trailingColorCodes(prefix) + remainder, 64);
        team.prefix(this.fromLegacy(prefix));
        team.suffix(this.fromLegacy(suffix));
    }

    @NotNull
    private String safeSubstring(@NotNull final String text, final int max) {
        if (text.length() <= max) return text;
        int safe = max;
        if (safe > 0 && text.charAt(safe - 1) == COLOR_CHAR) safe--;
        return text.substring(0, safe);
    }

    @NotNull
    private String trailingColorCodes(@NotNull final String text) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) != COLOR_CHAR) continue;
            final char code = Character.toLowerCase(text.charAt(i + 1));
            if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                sb.setLength(0);
                sb.append(COLOR_CHAR).append(code);
            } else if (code == 'r') {
                sb.setLength(0);
            } else if (code >= 'k' && code <= 'o') {
                sb.append(COLOR_CHAR).append(code);
            }
        }
        return sb.toString();
    }

    @NotNull
    private String getTeamName(final int index) {
        return "rdqql" + index;
    }

    @NotNull
    private String i18nLine(
        @NotNull final String key,
        @NotNull final Player player,
        @NotNull final Map<String, Object> placeholders
    ) {
        return LEGACY_SERIALIZER.serialize(
            new I18n.Builder(key, player)
                .withPlaceholders(placeholders)
                .build()
                .component()
        );
    }

    @NotNull
    private Component fromLegacy(@NotNull final String text) {
        return LEGACY_SERIALIZER.deserialize(text);
    }

    private record SidebarState(
        @NotNull Scoreboard previousScoreboard,
        @NotNull Scoreboard sidebarScoreboard
    ) {}
}
