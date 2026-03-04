/*
 * PerkSidebarScoreboardService.java
 *
 * @author RaindropCentral
 * @version 6.0.0
 */

package com.raindropcentral.rdq.service.scoreboard;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.perk.Perk;
import com.raindropcentral.rdq.database.entity.perk.PlayerPerk;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.jextranslate.i18n.I18n;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains the opt-in RDQ perks sidebar scoreboard exposed through {@code /rq scoreboard perks}.
 *
 * <p>The service keeps a single active perks sidebar per player, refreshes the displayed perk
 * summary on a repeating interval, and restores the player's previous scoreboard when disabled.</p>
 *
 * @author RaindropCentral
 * @since 6.0.0
 * @version 6.0.0
 */
public final class PerkSidebarScoreboardService {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN_TEXT_SERIALIZER = PlainTextComponentSerializer.plainText();
    private static final long UPDATE_PERIOD_TICKS = 40L;
    private static final int MAX_SCOREBOARD_LINES = 15;
    private static final int SUMMARY_LINE_COUNT = 2;
    private static final int TEAM_TEXT_LIMIT = 64;
    private static final String OBJECTIVE_NAME = "rdqperks";
    private static final char COLOR_CHAR = '\u00A7';
    private static final List<String> LINE_ENTRIES = List.of(
        "\u00A70",
        "\u00A71",
        "\u00A72",
        "\u00A73",
        "\u00A74",
        "\u00A75",
        "\u00A76",
        "\u00A77",
        "\u00A78",
        "\u00A79",
        "\u00A7a",
        "\u00A7b",
        "\u00A7c",
        "\u00A7d",
        "\u00A7e"
    );

    private final RDQ plugin;
    private final Map<UUID, SidebarState> activeSidebars = new ConcurrentHashMap<>();

    /**
     * Creates the perks sidebar service for the owning plugin.
     *
     * @param plugin active RDQ plugin instance
     */
    public PerkSidebarScoreboardService(
        final @NotNull RDQ plugin
    ) {
        this.plugin = plugin;
    }

    /**
     * Starts periodic refreshes for every active perks sidebar.
     */
    public void start() {
        this.plugin.getPlatform().getScheduler().runRepeating(
            this::refreshActivePlayers,
            UPDATE_PERIOD_TICKS,
            UPDATE_PERIOD_TICKS
        );
    }

    /**
     * Disables every active perks sidebar and restores prior player scoreboards.
     */
    public void shutdown() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            this.disable(player);
        }
        this.activeSidebars.clear();
    }

    /**
     * Enables the perks sidebar for the supplied player.
     *
     * @param player player who should receive the perks sidebar
     */
    public void enable(
        final @NotNull Player player
    ) {
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
     * Disables the perks sidebar for the supplied player.
     *
     * @param player player whose perks sidebar should be removed
     */
    public void disable(
        final @NotNull Player player
    ) {
        final SidebarState state = this.activeSidebars.remove(player.getUniqueId());
        if (state == null) {
            return;
        }

        if (player.getScoreboard() == state.sidebarScoreboard()) {
            player.setScoreboard(state.previousScoreboard());
        }
    }

    /**
     * Returns whether the supplied player currently has the RDQ perks sidebar enabled.
     *
     * @param player player to inspect
     * @return {@code true} when the player currently has an active perks sidebar
     */
    public boolean isActive(
        final @NotNull Player player
    ) {
        return this.activeSidebars.containsKey(player.getUniqueId());
    }

    private void refreshActivePlayers() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            final SidebarState state = this.activeSidebars.get(player.getUniqueId());
            if (state == null) {
                continue;
            }

            this.refreshPlayer(player, state);
        }
    }

    private void refreshPlayer(
        final @NotNull Player player,
        final @NotNull SidebarState state
    ) {
        if (!player.isOnline()) {
            this.activeSidebars.remove(player.getUniqueId());
            return;
        }

        if (player.getScoreboard() != state.sidebarScoreboard()) {
            player.setScoreboard(state.sidebarScoreboard());
        }

        this.renderSidebar(
            state.sidebarScoreboard(),
            player,
            this.buildLines(player)
        );
    }

    private @NotNull List<String> buildLines(
        final @NotNull Player player
    ) {
        final RDQPlayer rdqPlayer = this.getPlayerData(player);
        if (rdqPlayer == null) {
            return this.buildEmptyLines(
                player,
                this.plugin.getPerkManagementService().getMaxEnabledPerks(),
                this.resolveTotalPerkCount()
            );
        }

        final List<PlayerPerk> acquiredPerks = this.plugin.getPerkManagementService().getUnlockedPerks(rdqPlayer);
        final List<PlayerPerk> activePerks = this.plugin.getPerkManagementService().getEnabledPerks(rdqPlayer).stream()
            .filter(PlayerPerk::isUnlocked)
            .sorted(Comparator
                .comparingInt((final PlayerPerk playerPerk) -> this.getDisplayOrder(playerPerk.getPerk()))
                .thenComparing(playerPerk -> playerPerk.getPerk().getIdentifier(), String.CASE_INSENSITIVE_ORDER))
            .toList();
        final int maxActivePerks = this.plugin.getPerkManagementService().getMaxEnabledPerks();
        final int acquiredPerkCount = acquiredPerks.size();
        final int totalPerks = this.resolveTotalPerkCount();

        final List<String> lines = new ArrayList<>();
        lines.add(this.i18nLine(
            "scoreboard_sidebar.perks.active_summary",
            player,
            Map.of(
                "active_perks",
                activePerks.size(),
                "max_active_perks",
                maxActivePerks
            )
        ));
        lines.add(this.i18nLine(
            "scoreboard_sidebar.perks.owned_summary",
            player,
            Map.of(
                "acquired_perks", acquiredPerkCount,
                "total_perks", totalPerks
            )
        ));

        if (activePerks.isEmpty()) {
            lines.add(this.i18nLine("scoreboard_sidebar.perks.no_active", player, Map.of()));
            return lines;
        }

        final int availablePerkLines = MAX_SCOREBOARD_LINES - SUMMARY_LINE_COUNT;
        final boolean truncated = activePerks.size() > availablePerkLines - 1;
        final int listedPerks = truncated ? availablePerkLines - 1 : activePerks.size();
        for (int index = 0; index < listedPerks; index++) {
            lines.add(this.i18nLine(
                "scoreboard_sidebar.perks.active_entry",
                player,
                Map.of(
                    "perk_name",
                    this.getPerkDisplayName(player, activePerks.get(index).getPerk()),
                    "usage_count",
                    activePerks.get(index).getActivationCount()
                )
            ));
        }

        if (truncated) {
            lines.add(this.i18nLine(
                "scoreboard_sidebar.perks.more_active",
                player,
                Map.of("remaining_perks", activePerks.size() - listedPerks)
            ));
        }

        return lines;
    }

    private @NotNull List<String> buildEmptyLines(
        final @NotNull Player player,
        final int maxActivePerks,
        final int totalPerks
    ) {
        return List.of(
            this.i18nLine(
                "scoreboard_sidebar.perks.active_summary",
                player,
                Map.of(
                    "active_perks", 0,
                    "max_active_perks", maxActivePerks
                )
            ),
            this.i18nLine(
                "scoreboard_sidebar.perks.owned_summary",
                player,
                Map.of(
                    "acquired_perks", 0,
                    "total_perks", totalPerks
                )
            ),
            this.i18nLine("scoreboard_sidebar.perks.no_active", player, Map.of())
        );
    }

    private int resolveTotalPerkCount() {
        final Map<String, Perk> cachedPerks = this.plugin.getPerkRepository().getCachedByKey();
        if (!cachedPerks.isEmpty()) {
            return cachedPerks.size();
        }

        return this.plugin.getPerkManagementService().getAvailablePerks(null).size();
    }

    private @Nullable RDQPlayer getPlayerData(
        final @NotNull Player player
    ) {
        final RDQPlayer cachedPlayer = this.plugin.getPlayerRepository().getCachedByKey().get(player.getUniqueId());
        if (cachedPlayer != null) {
            return cachedPlayer;
        }

        return this.plugin.getPlayerRepository().findByAttributes(
            Map.of("uniqueId", player.getUniqueId())
        ).orElse(null);
    }

    private int getDisplayOrder(
        final @Nullable Perk perk
    ) {
        return perk == null ? Integer.MAX_VALUE : perk.getDisplayOrder();
    }

    private @NotNull String getPerkDisplayName(
        final @NotNull Player player,
        final @NotNull Perk perk
    ) {
        return PLAIN_TEXT_SERIALIZER.serialize(
            new I18n.Builder("perk." + perk.getIdentifier() + ".name", player)
                .build()
                .component()
        );
    }

    private @NotNull Scoreboard createSidebarScoreboard() {
        final ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager == null) {
            throw new IllegalStateException("Bukkit scoreboard manager is unavailable");
        }

        final Scoreboard scoreboard = scoreboardManager.getNewScoreboard();
        final Objective objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, Component.empty());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());

        for (int index = 0; index < LINE_ENTRIES.size(); index++) {
            final Team team = scoreboard.registerNewTeam(this.getTeamName(index));
            team.addEntry(LINE_ENTRIES.get(index));
        }

        return scoreboard;
    }

    private void renderSidebar(
        final @NotNull Scoreboard scoreboard,
        final @NotNull Player player,
        final @NotNull List<String> rawLines
    ) {
        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective == null) {
            objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, Component.empty());
        }

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());
        objective.displayName(
            new I18n.Builder("scoreboard_sidebar.perks.title", player)
                .build()
                .component()
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
                continue;
            }

            team.prefix(Component.empty());
            team.suffix(Component.empty());
            scoreboard.resetScores(entry);
        }
    }

    private @NotNull Team getOrCreateTeam(
        final @NotNull Scoreboard scoreboard,
        final int index,
        final @NotNull String entry
    ) {
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

    private void applyLine(
        final @NotNull Team team,
        final @NotNull String line
    ) {
        final String prefix = this.safeSubstring(line, TEAM_TEXT_LIMIT);
        final String remainder = line.substring(prefix.length());
        if (remainder.isEmpty()) {
            team.prefix(this.fromLegacyText(prefix));
            team.suffix(Component.empty());
            return;
        }

        final String suffix = this.safeSubstring(this.getTrailingColorCodes(prefix) + remainder, TEAM_TEXT_LIMIT);
        team.prefix(this.fromLegacyText(prefix));
        team.suffix(this.fromLegacyText(suffix));
    }

    private @NotNull String safeSubstring(
        final @NotNull String text,
        final int maxLength
    ) {
        if (text.length() <= maxLength) {
            return text;
        }

        int safeLength = Math.max(0, maxLength);
        if (safeLength > 0 && text.charAt(safeLength - 1) == COLOR_CHAR) {
            safeLength--;
        }
        return text.substring(0, safeLength);
    }

    private @NotNull String getTrailingColorCodes(
        final @NotNull String text
    ) {
        final StringBuilder activeCodes = new StringBuilder();
        for (int index = 0; index < text.length() - 1; index++) {
            if (text.charAt(index) != COLOR_CHAR) {
                continue;
            }

            final char code = Character.toLowerCase(text.charAt(index + 1));
            if (this.isColorCode(code)) {
                activeCodes.setLength(0);
                activeCodes.append(COLOR_CHAR).append(code);
                continue;
            }
            if (code == 'r') {
                activeCodes.setLength(0);
                continue;
            }
            if (this.isFormatCode(code)) {
                activeCodes.append(COLOR_CHAR).append(code);
            }
        }

        return activeCodes.toString();
    }

    private boolean isColorCode(
        final char code
    ) {
        return (code >= '0' && code <= '9') || (code >= 'a' && code <= 'f');
    }

    private boolean isFormatCode(
        final char code
    ) {
        return code >= 'k' && code <= 'o';
    }

    private @NotNull String getTeamName(
        final int index
    ) {
        return "rdql" + index;
    }

    private @NotNull String i18nLine(
        final @NotNull String key,
        final @NotNull Player player,
        final @NotNull Map<String, Object> placeholders
    ) {
        return this.toLegacyText(
            new I18n.Builder(key, player)
                .withPlaceholders(placeholders)
                .build()
                .component()
        );
    }

    private @NotNull String toLegacyText(
        final @NotNull Component component
    ) {
        return LEGACY_SERIALIZER.serialize(component);
    }

    private @NotNull Component fromLegacyText(
        final @NotNull String text
    ) {
        return LEGACY_SERIALIZER.deserialize(text);
    }

    private record SidebarState(
        @NotNull Scoreboard previousScoreboard,
        @NotNull Scoreboard sidebarScoreboard
    ) {
    }
}
