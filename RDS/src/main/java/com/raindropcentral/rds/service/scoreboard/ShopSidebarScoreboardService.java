/*
 * ShopSidebarScoreboardService.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rds.service.scoreboard;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.Bank;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.database.entity.ShopLedgerEntry;
import com.raindropcentral.rds.database.entity.ShopLedgerType;
import com.raindropcentral.rds.items.AbstractItem;
import com.raindropcentral.rds.items.ShopItem;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import de.jexcellence.jextranslate.i18n.I18n;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains the opt-in RDS sidebar scoreboards exposed through {@code /rs scoreboard <type>}.
 *
 * <p>The service keeps at most one active sidebar per player, periodically refreshes the rendered
 * values while it remains enabled, and restores the player's previous scoreboard when the sidebar
 * is disabled.</p>
 *
 * @author RaindropCentral
 * @since 5.0.0
 * @version 5.0.0
 */
public class ShopSidebarScoreboardService {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final long UPDATE_PERIOD_TICKS = 40L;
    private static final int MAX_LEDGER_BANK_LINES = 3;
    private static final int MAX_STOCK_SHOPS = 5;
    private static final int MAX_SCOREBOARD_LINES = 15;
    private static final int TEAM_TEXT_LIMIT = 64;
    private static final String OBJECTIVE_NAME = "rdsboard";
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

    private final RDS plugin;
    private final Map<UUID, SidebarState> activeSidebars = new ConcurrentHashMap<>();

    /**
     * Creates the sidebar scoreboard service for the owning plugin.
     *
     * @param plugin active RDS plugin instance
     */
    public ShopSidebarScoreboardService(
            final @NotNull RDS plugin
    ) {
        this.plugin = plugin;
    }

    /**
     * Starts periodic refreshes for every active sidebar scoreboard.
     */
    public void start() {
        this.plugin.getScheduler().runRepeating(
                this::refreshActivePlayers,
                UPDATE_PERIOD_TICKS,
                UPDATE_PERIOD_TICKS
        );
    }

    /**
     * Disables every active sidebar and restores the players' previous scoreboards.
     */
    public void shutdown() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            this.disable(player);
        }
        this.activeSidebars.clear();
    }

    /**
     * Enables the ledger sidebar scoreboard for the supplied player.
     *
     * @param player player who should receive the sidebar
     */
    public void enableLedger(
            final @NotNull Player player
    ) {
        this.enable(player, SidebarType.LEDGER);
    }

    /**
     * Enables the stock sidebar scoreboard for the supplied player.
     *
     * @param player player who should receive the sidebar
     */
    public void enableStock(
            final @NotNull Player player
    ) {
        this.enable(player, SidebarType.STOCK);
    }

    /**
     * Disables the currently active sidebar scoreboard for the supplied player.
     *
     * @param player player whose sidebar should be removed
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
     * Returns the currently active sidebar type name for the player.
     *
     * @param player player whose sidebar state should be inspected
     * @return {@code ledger}, {@code stock}, or {@code null} when no RDS sidebar is active
     */
    public @Nullable String getActiveTypeName(
            final @NotNull Player player
    ) {
        final SidebarState state = this.activeSidebars.get(player.getUniqueId());
        return state == null ? null : state.type().id();
    }

    private void enable(
            final @NotNull Player player,
            final @NotNull SidebarType type
    ) {
        final SidebarState existingState = this.activeSidebars.get(player.getUniqueId());
        if (existingState == null) {
            final Scoreboard sidebarScoreboard = this.createSidebarScoreboard();
            final SidebarState state = new SidebarState(type, player.getScoreboard(), sidebarScoreboard);
            this.activeSidebars.put(player.getUniqueId(), state);
            player.setScoreboard(sidebarScoreboard);
            this.refreshPlayer(player, state);
            return;
        }

        final SidebarState updatedState = new SidebarState(type, existingState.previousScoreboard(), existingState.sidebarScoreboard());
        this.activeSidebars.put(player.getUniqueId(), updatedState);
        player.setScoreboard(updatedState.sidebarScoreboard());
        this.refreshPlayer(player, updatedState);
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

        final List<String> lines = switch (state.type()) {
            case LEDGER -> this.buildLedgerLines(player);
            case STOCK -> this.buildStockLines(player);
        };
        this.renderSidebar(
                state.sidebarScoreboard(),
                player,
                state.type().titleKey(),
                lines
        );
    }

    private @NotNull List<String> buildLedgerLines(
            final @NotNull Player player
    ) {
        final List<Shop> ownedShops = this.getOwnedShops(player.getUniqueId(), false);
        final Map<String, Double> bankTotals = new HashMap<>();
        int transactionCount = 0;

        for (final Shop shop : ownedShops) {
            transactionCount += shop.getLedgerEntryCount();
            for (final Bank bankEntry : shop.getBankEntries()) {
                bankTotals.merge(
                        bankEntry.getCurrencyType(),
                        bankEntry.getAmount(),
                        Double::sum
                );
            }
        }

        final List<String> lines = new ArrayList<>();
        lines.add(this.i18nLine(
                "scoreboard_sidebar.ledger.transactions",
                player,
                Map.of("transaction_count", transactionCount)
        ));

        if (bankTotals.isEmpty()) {
            lines.add(this.i18nLine("scoreboard_sidebar.ledger.bank_empty", player, Map.of()));
            return lines;
        }

        lines.add(this.i18nLine("scoreboard_sidebar.ledger.bank_header", player, Map.of()));
        bankTotals.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)))
                .limit(MAX_LEDGER_BANK_LINES)
                .forEach(entry -> lines.add(
                        this.i18nLine(
                                "scoreboard_sidebar.ledger.bank_entry",
                                player,
                                Map.of(
                                        "currency_name", this.getCurrencyDisplayName(entry.getKey()),
                                        "amount", this.formatBankAmount(entry.getKey(), entry.getValue())
                                )
                        )
                ));

        return lines;
    }

    private @NotNull List<String> buildStockLines(
            final @NotNull Player player
    ) {
        final List<ShopStockEntry> stockEntries = new ArrayList<>();
        for (final Shop shop : this.getOwnedShops(player.getUniqueId(), true)) {
            final Location location = shop.getShopLocation();
            if (location == null) {
                continue;
            }

            final int currentStock = this.countCurrentStock(shop);
            final int totalStock = currentStock + this.countSoldStock(shop);
            stockEntries.add(new ShopStockEntry(
                    location,
                    currentStock,
                    totalStock
            ));
        }

        stockEntries.sort(Comparator
                .comparingInt(ShopStockEntry::missingStockAmount)
                .reversed()
                .thenComparingInt(ShopStockEntry::currentStockAmount)
                .thenComparing(entry -> this.formatLocation(entry.location()), String.CASE_INSENSITIVE_ORDER));

        final List<String> lines = new ArrayList<>();
        if (stockEntries.isEmpty()) {
            lines.add(this.i18nLine("scoreboard_sidebar.stock.empty", player, Map.of()));
            return lines;
        }

        for (int index = 0; index < stockEntries.size() && index < MAX_STOCK_SHOPS; index++) {
            final ShopStockEntry entry = stockEntries.get(index);
            lines.add(this.i18nLine(
                    "scoreboard_sidebar.stock.location",
                    player,
                    Map.of(
                            "location", this.formatLocation(entry.location())
                    )
            ));
            lines.add(this.i18nLine(
                    "scoreboard_sidebar.stock.amount",
                    player,
                    Map.of(
                            "current_stock", entry.currentStockAmount(),
                            "total_stock", entry.totalStockAmount()
                    )
            ));
        }

        return lines;
    }

    private @NotNull List<Shop> getOwnedShops(
            final @NotNull UUID playerId,
            final boolean excludeAdminShops
    ) {
        final List<Shop> ownedShops = new ArrayList<>();
        for (final Shop shop : this.plugin.getShopRepository().findAllShops()) {
            if (!shop.isOwner(playerId)) {
                continue;
            }
            if (excludeAdminShops && shop.isAdminShop()) {
                continue;
            }

            ownedShops.add(shop);
        }
        return ownedShops;
    }

    private int countCurrentStock(
            final @NotNull Shop shop
    ) {
        int totalStock = 0;
        for (final AbstractItem item : shop.getItems()) {
            if (item instanceof ShopItem shopItem && shopItem.getAmount() > 0) {
                totalStock += shopItem.getAmount();
            }
        }
        return totalStock;
    }

    private int countSoldStock(
            final @NotNull Shop shop
    ) {
        int soldStock = 0;
        for (final ShopLedgerEntry ledgerEntry : shop.getLedgerEntries()) {
            if (ledgerEntry.getEntryType() != ShopLedgerType.PURCHASE) {
                continue;
            }

            final Integer itemAmount = ledgerEntry.getItemAmount();
            if (itemAmount == null || itemAmount <= 0) {
                continue;
            }

            soldStock += itemAmount;
        }
        return soldStock;
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
            final @NotNull String titleKey,
            final @NotNull List<String> rawLines
    ) {
        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective == null) {
            objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, Component.empty());
        }

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());
        objective.displayName(
                new I18n.Builder(titleKey, player)
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

    private @NotNull String getTeamName(
            final int index
    ) {
        return "rdsl" + index;
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

    private @NotNull String formatLocation(
            final @NotNull Location location
    ) {
        final String worldName = location.getWorld() == null
                ? "unknown"
                : location.getWorld().getName();
        return worldName
                + " ("
                + location.getBlockX()
                + ","
                + location.getBlockY()
                + ","
                + location.getBlockZ()
                + ")";
    }

    private @NotNull String formatBankAmount(
            final @NotNull String currencyType,
            final double amount
    ) {
        if ("vault".equalsIgnoreCase(currencyType)) {
            return this.plugin.formatVaultCurrency(amount);
        }

        return String.format(Locale.US, "%.2f", amount);
    }

    private @NotNull String getCurrencyDisplayName(
            final @NotNull String currencyType
    ) {
        if ("vault".equalsIgnoreCase(currencyType)) {
            return "Vault";
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        return bridge == null ? currencyType : bridge.getCurrencyDisplayName(currencyType);
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

    private record SidebarState(
            @NotNull SidebarType type,
            @NotNull Scoreboard previousScoreboard,
            @NotNull Scoreboard sidebarScoreboard
    ) {
    }

    private record ShopStockEntry(
            @NotNull Location location,
            int currentStockAmount,
            int totalStockAmount
    ) {
        private int missingStockAmount() {
            return Math.max(0, this.totalStockAmount - this.currentStockAmount);
        }
    }

    private enum SidebarType {
        LEDGER("ledger", "scoreboard_sidebar.ledger.title"),
        STOCK("stock", "scoreboard_sidebar.stock.title");

        private final String id;
        private final String titleKey;

        SidebarType(
                final @NotNull String id,
                final @NotNull String titleKey
        ) {
            this.id = id;
            this.titleKey = titleKey;
        }

        private @NotNull String id() {
            return this.id;
        }

        private @NotNull String titleKey() {
            return this.titleKey;
        }
    }
}
