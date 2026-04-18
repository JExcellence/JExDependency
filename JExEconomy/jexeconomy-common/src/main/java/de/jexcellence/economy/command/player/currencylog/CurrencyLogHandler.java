package de.jexcellence.economy.command.player.currencylog;

import com.raindropcentral.commands.v2.CommandContext;
import com.raindropcentral.commands.v2.CommandHandler;
import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.database.entity.TransactionLog;
import de.jexcellence.economy.database.repository.TransactionLogRepository;
import de.jexcellence.jextranslate.R18nManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * JExCommand 2.0 handler collection for {@code /currencylog}.
 *
 * <p>Supports six paths:
 * <ul>
 *   <li>{@code /currencylog} — opens page 1 of the invoker's log.</li>
 *   <li>{@code /currencylog view [page]} — explicit pagination.</li>
 *   <li>{@code /currencylog details &lt;id&gt;} — full detail view for one entry.</li>
 *   <li>{@code /currencylog player &lt;name&gt; [page]} — inspect another player's log
 *       (requires {@code currencylog.command.player}).</li>
 *   <li>{@code /currencylog clear [days]} — prune entries older than N days
 *       (requires {@code currencylog.command.clear}).</li>
 *   <li>{@code /currencylog help} — permission-gated usage printout.</li>
 * </ul>
 *
 * @author JExcellence
 * @since 3.0.0
 */
public final class CurrencyLogHandler {

    private static final int LOGS_PER_PAGE = 10;
    private static final int MAX_LOGS = 200;
    private static final int MIN_CLEAR_DAYS = 7;
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String PERM_PLAYER = "currencylog.command.player";
    private static final String PERM_CLEAR  = "currencylog.command.clear";

    private final TransactionLogRepository txLogRepo;

    public CurrencyLogHandler(@NotNull JExEconomy economy) {
        this.txLogRepo = economy.transactionLogRepo();
    }

    /** Returns the path → handler map for registration. */
    public @NotNull Map<String, CommandHandler> handlerMap() {
        return Map.of(
                "currencylog",         this::onRoot,
                "currencylog.view",    this::onView,
                "currencylog.details", this::onDetails,
                "currencylog.player",  this::onPlayerLookup,
                "currencylog.clear",   this::onClear,
                "currencylog.help",    this::onHelp
        );
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    private void onRoot(@NotNull CommandContext ctx) {
        var player = ctx.asPlayer().orElseThrow();
        displayLogs(player, player.getUniqueId(), player.getName(), 1);
    }

    private void onView(@NotNull CommandContext ctx) {
        var player = ctx.asPlayer().orElseThrow();
        int page = ctx.get("page", Long.class).orElse(1L).intValue();
        displayLogs(player, player.getUniqueId(), player.getName(), Math.max(1, page));
    }

    private void onDetails(@NotNull CommandContext ctx) {
        var player = ctx.asPlayer().orElseThrow();
        long logId = ctx.require("id", Long.class);

        CompletableFuture.supplyAsync(() -> txLogRepo.findById(logId).orElse(null))
                .thenAccept(log -> {
                    if (log == null) {
                        r18n().msg("log.not-found").prefix()
                                .with("id", String.valueOf(logId))
                                .send(player);
                        return;
                    }
                    sendDetailView(player, log);
                })
                .exceptionally(ex -> {
                    r18n().msg("log.error").prefix().send(player);
                    return null;
                });
    }

    private void onPlayerLookup(@NotNull CommandContext ctx) {
        var viewer = ctx.asPlayer().orElseThrow();
        var target = ctx.require("player", OfflinePlayer.class);
        int page = ctx.get("page", Long.class).orElse(1L).intValue();

        var targetName = target.getName();
        if (target.hasPlayedBefore() || target.isOnline()) {
            displayLogs(viewer, target.getUniqueId(),
                    targetName != null ? targetName : target.getUniqueId().toString(),
                    Math.max(1, page));
            return;
        }

        // Fallback: query by cached player_name column.
        final int resolvedPage = Math.max(1, page);
        final String nameKey = targetName != null ? targetName : target.getUniqueId().toString();
        txLogRepo.findByPlayerNameAsync(nameKey, MAX_LOGS)
                .thenAccept(logs -> {
                    if (logs.isEmpty()) {
                        r18n().msg("log.empty-other").prefix()
                                .with("player", nameKey)
                                .send(viewer);
                        return;
                    }
                    int totalPages = (int) Math.ceil((double) logs.size() / LOGS_PER_PAGE);
                    if (resolvedPage > totalPages) {
                        r18n().msg("log.page-not-found").prefix()
                                .with("page", String.valueOf(resolvedPage))
                                .with("total", String.valueOf(totalPages))
                                .send(viewer);
                        return;
                    }
                    r18n().msg("log.viewing-other").prefix()
                            .with("player", nameKey)
                            .send(viewer);
                    int start = (resolvedPage - 1) * LOGS_PER_PAGE;
                    int end = Math.min(start + LOGS_PER_PAGE, logs.size());
                    sendLogPage(viewer, logs.subList(start, end), resolvedPage, totalPages, logs.size());
                })
                .exceptionally(ex -> {
                    r18n().msg("log.error").prefix().send(viewer);
                    return null;
                });
    }

    private void onClear(@NotNull CommandContext ctx) {
        var viewer = ctx.asPlayer().orElseThrow();
        int days = ctx.get("days", Long.class).orElse(90L).intValue();

        if (days < MIN_CLEAR_DAYS) {
            r18n().msg("log.clear-days-too-small").prefix()
                    .with("min", String.valueOf(MIN_CLEAR_DAYS))
                    .with("given", String.valueOf(days))
                    .send(viewer);
            return;
        }

        final int cutoffDays = days;
        var cutoff = Instant.now().minus(cutoffDays, ChronoUnit.DAYS);

        r18n().msg("log.clear-start").prefix()
                .with("days", String.valueOf(cutoffDays))
                .send(viewer);

        txLogRepo.deleteOlderThanAsync(cutoff)
                .thenAccept(count -> r18n().msg("log.clear-success").prefix()
                        .with("count", String.valueOf(count))
                        .with("days", String.valueOf(cutoffDays))
                        .send(viewer))
                .exceptionally(ex -> {
                    r18n().msg("log.clear-failed").prefix()
                            .with("error", ex.getMessage() != null ? ex.getMessage() : "Exception")
                            .send(viewer);
                    return null;
                });
    }

    private void onHelp(@NotNull CommandContext ctx) {
        var player = ctx.asPlayer().orElseThrow();
        var alias = ctx.alias();

        player.sendMessage(Component.empty());
        r18n().msg("log.usage-header").send(player);
        r18n().msg("log.usage-view").with("alias", alias).send(player);
        r18n().msg("log.usage-details").with("alias", alias).send(player);
        if (player.hasPermission(PERM_PLAYER)) {
            r18n().msg("log.usage-player").with("alias", alias).send(player);
        }
        if (player.hasPermission(PERM_CLEAR)) {
            r18n().msg("log.usage-clear").with("alias", alias).send(player);
        }
        player.sendMessage(Component.empty());
    }

    // ── Display helpers ──────────────────────────────────────────────────────

    private void displayLogs(@NotNull Player viewer,
                              @NotNull UUID targetUuid,
                              String targetName,
                              int page) {
        txLogRepo.findByPlayerUuidAsync(targetUuid, MAX_LOGS)
                .thenAccept(logs -> {
                    if (logs.isEmpty()) {
                        var self = viewer.getUniqueId().equals(targetUuid);
                        var key = self ? "log.empty" : "log.empty-other";
                        var msg = r18n().msg(key).prefix();
                        if (!self) {
                            msg = msg.with("player",
                                    targetName != null ? targetName : targetUuid.toString());
                        }
                        msg.send(viewer);
                        return;
                    }

                    int totalPages = (int) Math.ceil((double) logs.size() / LOGS_PER_PAGE);
                    if (page > totalPages) {
                        r18n().msg("log.page-not-found").prefix()
                                .with("page", String.valueOf(page))
                                .with("total", String.valueOf(totalPages))
                                .send(viewer);
                        return;
                    }

                    int start = (page - 1) * LOGS_PER_PAGE;
                    int end = Math.min(start + LOGS_PER_PAGE, logs.size());
                    if (!viewer.getUniqueId().equals(targetUuid)) {
                        r18n().msg("log.viewing-other").prefix()
                                .with("player",
                                        targetName != null ? targetName : targetUuid.toString())
                                .send(viewer);
                    }
                    sendLogPage(viewer, logs.subList(start, end), page, totalPages, logs.size());
                })
                .exceptionally(ex -> {
                    r18n().msg("log.error").prefix().send(viewer);
                    return null;
                });
    }

    private void sendLogPage(@NotNull Player player,
                             @NotNull List<TransactionLog> logs,
                             int page,
                             int totalPages,
                             int total) {
        player.sendMessage(Component.empty());
        r18n().msg("log.header")
                .with("page", page)
                .with("total_pages", totalPages)
                .with("total_entries", total)
                .send(player);
        player.sendMessage(Component.empty());

        for (int i = 0; i < logs.size(); i++) {
            sendEntry(player, logs.get(i), (page - 1) * LOGS_PER_PAGE + i + 1);
        }

        sendNavigation(player, page, totalPages);
    }

    private void sendEntry(@NotNull Player player, @NotNull TransactionLog log, int index) {
        var statusKey = log.isSuccess() ? "log.status-success" : "log.status-failed";
        var timestamp = log.getTimestamp().atZone(ZoneId.systemDefault()).format(DATE_FMT);
        var currencyName = log.getCurrency() != null ? log.getCurrency().getIdentifier() : "?";

        r18n().msg("log.entry")
                .with("index", "%2d".formatted(index))
                .with("id", log.getId())
                .with("status", r18n().msg(statusKey).text(null))
                .with("type", log.getChangeType().name())
                .with("amount", "%.2f".formatted(log.getAmount()))
                .with("currency", currencyName)
                .with("timestamp", timestamp)
                .with("old_balance", "%.2f".formatted(log.getOldBalance()))
                .with("new_balance", "%.2f".formatted(log.getNewBalance()))
                .send(player);
    }

    private void sendNavigation(@NotNull Player player, int page, int totalPages) {
        player.sendMessage(Component.empty());
        var prevKey = page > 1 ? "log.nav-prev-active" : "log.nav-prev-disabled";
        var nextKey = page < totalPages ? "log.nav-next-active" : "log.nav-next-disabled";

        var prev = r18n().msg(prevKey).with("target_page", page - 1).text(null);
        var next = r18n().msg(nextKey).with("target_page", page + 1).text(null);

        r18n().msg("log.nav-line")
                .with("prev", prev)
                .with("next", next)
                .with("page", page)
                .with("total_pages", totalPages)
                .send(player);
        player.sendMessage(Component.empty());
    }

    private void sendDetailView(@NotNull Player player, @NotNull TransactionLog log) {
        var timestamp = log.getTimestamp().atZone(ZoneId.systemDefault()).format(DATE_FMT);
        var statusKey = log.isSuccess() ? "log.status-success" : "log.status-failed";
        var currencyName = log.getCurrency() != null ? log.getCurrency().getIdentifier() : "N/A";
        var change = log.getChangeAmount();
        var netKey = change >= 0 ? "log.details-net-positive" : "log.details-net-negative";
        var netAmount = "%.2f".formatted(Math.abs(change));

        player.sendMessage(Component.empty());
        r18n().msg("log.details-header").with("id", log.getId()).send(player);
        r18n().msg("log.details-time").with("time", timestamp).send(player);
        r18n().msg("log.details-status")
                .with("status", r18n().msg(statusKey).text(null))
                .send(player);
        r18n().msg("log.details-type").with("type", log.getChangeType().name()).send(player);
        r18n().msg("log.details-currency").with("currency", currencyName).send(player);
        r18n().msg("log.details-amount")
                .with("amount", "%.2f".formatted(log.getAmount()))
                .send(player);
        r18n().msg("log.details-balance")
                .with("old", "%.2f".formatted(log.getOldBalance()))
                .with("new", "%.2f".formatted(log.getNewBalance()))
                .send(player);
        r18n().msg(netKey).with("amount", netAmount).send(player);

        if (log.getPlayerUuid() != null) {
            r18n().msg("log.details-player")
                    .with("player", playerName(log.getPlayerUuid()))
                    .send(player);
        }
        if (log.getInitiatorUuid() != null
                && !log.getInitiatorUuid().equals(log.getPlayerUuid())) {
            r18n().msg("log.details-initiator")
                    .with("initiator", playerName(log.getInitiatorUuid()))
                    .send(player);
        }
        if (log.getReason() != null && !log.getReason().isEmpty()) {
            r18n().msg("log.details-reason").with("reason", log.getReason()).send(player);
        }
        if (!log.isSuccess() && log.getErrorMessage() != null) {
            r18n().msg("log.details-error").with("error", log.getErrorMessage()).send(player);
        }

        r18n().msg("log.details-back").send(player);
        player.sendMessage(Component.empty());
    }

    private static @NotNull String playerName(@NotNull UUID uuid) {
        var p = Bukkit.getOfflinePlayer(uuid);
        return p.getName() != null ? p.getName() : uuid.toString();
    }

    private static R18nManager r18n() {
        return R18nManager.getInstance();
    }
}
