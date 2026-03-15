package de.jexcellence.economy.command.player.currencylog;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.CurrencyLog;
import de.jexcellence.economy.type.EChangeType;
import de.jexcellence.economy.type.ELogLevel;
import de.jexcellence.economy.type.ELogType;
import de.jexcellence.jextranslate.i18n.I18n;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Enhanced player command implementation for viewing currency logs with advanced filtering and modern interactive pagination.
 *
 * <p>This command provides players and administrators with comprehensive access to currency
 * transaction logs, featuring beautiful MiniMessage formatting, advanced filtering capabilities,
 * and intuitive pagination controls matching the modern RDQImpl design standards.
 *
 * <p><strong>Enhanced Features:</strong>
 * <ul>
 *   <li>Modern MiniMessage gradients and formatting</li>
 *   <li>Interactive filtering with hover effects</li>
 *   <li>Enhanced pagination with visual navigation</li>
 *   <li>Improved error handling and user feedback</li>
 *   <li>Consistent design patterns matching RDQImpl styling</li>
 *   <li>Fixed operation filtering for DEPOSIT, WITHDRAW, etc.</li>
 * </ul>
 *
 * @author JExcellence
 * @version 2.1.0
 * @since 1.0.0
 * @see PlayerCommand
 * @see CurrencyLog
 */
@Command
@SuppressWarnings("unused")
public class PCurrencyLog extends PlayerCommand {

    /**
     * Reference to the main JExEconomy plugin instance.
     */
    private final JExEconomy jexEconomyImpl;

    private final static Logger LOGGER = CentralLogger.getLoggerByName("JExEconomy");

    /**
     * Number of log entries to display per page.
     */
    private static final int LOGS_PER_PAGE = 12;

    /**
     * Date formatter for displaying log timestamps.
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Available subcommands for tab completion and validation.
     */
    private static final List<String> SUBCOMMANDS = List.of("view", "filter", "clear", "stats", "export");

    /**
     * Available filter types for tab completion and validation.
     */
    private static final List<String> FILTER_TYPES = List.of("player", "currency", "type", "level", "operation");

    /**
     * Available log types for filtering.
     */
    private static final List<String> LOG_TYPES = Arrays.stream(ELogType.values())
            .map(Enum::name)
            .map(String::toLowerCase)
            .collect(Collectors.toList());

    /**
     * Available log levels for filtering.
     */
    private static final List<String> LOG_LEVELS = Arrays.stream(ELogLevel.values())
            .map(Enum::name)
            .map(String::toLowerCase)
            .collect(Collectors.toList());

    /**
     * Available operation types for filtering.
     */
    private static final List<String> OPERATION_TYPES = Arrays.stream(EChangeType.values())
            .map(Enum::name)
            .map(String::toLowerCase)
            .collect(Collectors.toList());

    /**
     * Store active filters per player to maintain state between commands.
     */
    private final Map<UUID, LogFilter> activeFilters = new HashMap<>();

    /**
     * MiniMessage instance for parsing formatted text.
     */
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /**
     * Constructs a new currency log command handler.
     *
     * @param commandSection the command section configuration
     * @param jexEconomy the JExEconomy plugin instance
     */
    public PCurrencyLog(
            final @NotNull PCurrencyLogSection commandSection,
            final @NotNull JExEconomy jexEconomy
    ) {
        super(commandSection);
        this.jexEconomyImpl = jexEconomy;
    }

    /**
     * Handles player command invocation with enhanced routing and modern styling.
     */
    @Override
    protected void onPlayerInvocation(
            final @NotNull Player player,
            final @NotNull String alias,
            final @NotNull String[] args
    ) {
        // Handle pagination routing for view command
        if (args.length == 2 && "view".equalsIgnoreCase(args[0])) {
            this.handlePaginationRouting(player, args[1]);
            return;
        }

        if (args.length == 0) {
            this.sendEnhancedUsageMessage(player);
            return;
        }

        final ECurrencyLogAction action = this.enumParameterOrElse(args, 0, ECurrencyLogAction.class, ECurrencyLogAction.HELP);

        switch (action) {
            case VIEW -> this.handleViewAction(player, args);
            case FILTER -> this.handleFilterAction(player, args);
            case CLEAR -> this.handleClearAction(player);
            case STATS -> this.handleStatsAction(player);
            case EXPORT -> this.handleExportAction(player);
            case DETAILS -> this.handleDetailsAction(player, args);
            default -> this.sendEnhancedUsageMessage(player);
        }
    }

    @Override
    protected List<String> onPlayerTabCompletion(
            final @NotNull Player player,
            final @NotNull String alias,
            final @NotNull String[] args
    ) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(subcommand -> subcommand.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && "filter".equalsIgnoreCase(args[0])) {
            return FILTER_TYPES.stream()
                    .filter(type -> type.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && "filter".equalsIgnoreCase(args[0])) {
            String filterType = args[1].toLowerCase();
            return switch (filterType) {
                case "type" -> LOG_TYPES.stream()
                        .filter(type -> type.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
                case "level" -> LOG_LEVELS.stream()
                        .filter(level -> level.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
                case "operation" -> OPERATION_TYPES.stream()
                        .filter(op -> op.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
                case "currency" -> this.jexEconomyImpl.getCurrencies().values().stream()
                        .map(Currency::getIdentifier)
                        .filter(id -> id.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
                default -> List.of();
            };
        }

        if (args.length == 2 && "view".equalsIgnoreCase(args[0])) {
            return List.of("1", "2", "3", "4", "5");
        }

        return List.of();
    }

    /**
     * Handles pagination routing for view command.
     */
    private void handlePaginationRouting(final @NotNull Player player, final @NotNull String pageArg) {
        try {
            int page = Integer.parseInt(pageArg);
            this.displayCurrencyLogs(player, page);
        } catch (NumberFormatException e) {
            new I18n.Builder("currency_log.invalid_page_format", player).includePrefix().withPlaceholder("input", pageArg).build().sendMessage();
        }
    }

    /**
     * Handles the view action with enhanced feedback.
     */
    private void handleViewAction(final @NotNull Player player, final @NotNull String[] args) {
        int page = 1;

        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
                if (page < 1) {
                    new I18n.Builder("currency_log.invalid_page_number", player).includePrefix().withPlaceholder("page", args[1]).build().sendMessage();
                }
            } catch (NumberFormatException e) {
                new I18n.Builder("currency_log.invalid_page_format", player).includePrefix().withPlaceholder("input", args[1]).build().sendMessage();
            }
        }

        this.displayCurrencyLogs(player, page);
    }

    /**
     * Handles the filter action with enhanced interface.
     */
    private void handleFilterAction(final @NotNull Player player, final @NotNull String[] args) {
        if (args.length < 3) {
            this.sendEnhancedFilterMenu(player);
            return;
        }

        final String filterType = args[1].toLowerCase();
        final String filterValue = args[2];

        LogFilter filter = this.activeFilters.getOrDefault(player.getUniqueId(), new LogFilter());

        switch (filterType) {
            case "player" -> this.applyPlayerFilter(player, filter, filterValue);
            case "currency" -> this.applyCurrencyFilter(player, filter, filterValue);
            case "type" -> this.applyTypeFilter(player, filter, filterValue);
            case "level" -> this.applyLevelFilter(player, filter, filterValue);
            case "operation" -> this.applyOperationFilter(player, filter, filterValue);
            default -> this.sendEnhancedFilterMenu(player);
        }
    }

    /**
     * Handles the clear action with enhanced feedback.
     */
    private void handleClearAction(final @NotNull Player player) {
        this.activeFilters.remove(player.getUniqueId());

        Component successMessage = MINI_MESSAGE.deserialize(
                "<gradient:#2ecc71:#27ae60>✓ Filters Cleared</gradient>\n" +
                        "<gray>All active filters have been removed successfully!</gray>"
        );

        player.sendMessage(successMessage);
    }

    /**
     * Handles the stats action with enhanced display.
     */
    private void handleStatsAction(final @NotNull Player player) {
        this.displayEnhancedLogStatistics(player);
    }

    /**
     * Handles the export action with enhanced feedback.
     */
    private void handleExportAction(final @NotNull Player player) {
        if (!player.hasPermission("jexeconomy.admin.export")) {
            Component noPermissionMessage = MINI_MESSAGE.deserialize(
                    "<gradient:#e74c3c:#c0392b>✗ Access Denied</gradient>\n" +
                            "<gray>You don't have permission to export currency logs.</gray>"
            );
            player.sendMessage(noPermissionMessage);
            return;
        }

        Component startMessage = MINI_MESSAGE.deserialize(
                "<gradient:#f39c12:#e67e22>🔄 Export Started</gradient>\n" +
                        "<gray>Preparing currency logs for export...</gray>"
        );
        player.sendMessage(startMessage);

        this.exportLogsToFile(player).thenAccept(success -> {
            Component resultMessage = success ?
                    MINI_MESSAGE.deserialize(
                            "<gradient:#2ecc71:#27ae60>✓ Export Complete</gradient>\n" +
                                    "<gray>Currency logs have been exported successfully!</gray>"
                    ) :
                    MINI_MESSAGE.deserialize(
                            "<gradient:#e74c3c:#c0392b>✗ Export Failed</gradient>\n" +
                                    "<gray>An error occurred during the export process.</gray>"
                    );
            player.sendMessage(resultMessage);
        });
    }

    /**
     * Displays enhanced filter menu with modern styling.
     */
    private void sendEnhancedFilterMenu(final @NotNull Player player) {
        Component headerLine = MINI_MESSAGE.deserialize(
                "<gradient:#3498db:#2980b9>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬</gradient>"
        );

        Component title = MINI_MESSAGE.deserialize(
                "<gradient:#3498db:#2980b9>🔍 Currency Log Filters</gradient>"
        );

        Component subtitle = MINI_MESSAGE.deserialize(
                "<gray>Select a filter type to narrow down your log search</gray>"
        );

        Component instructions = MINI_MESSAGE.deserialize(
                "<dark_gray>▪ <yellow>Click</yellow> <dark_gray>»</dark_gray> <gray>Apply filter type</gray>\n" +
                        "<dark_gray>▪ <yellow>Hover</yellow> <dark_gray>»</dark_gray> <gray>See filter description</gray>"
        );

        player.sendMessage(Component.empty());
        player.sendMessage(headerLine);
        player.sendMessage(Component.empty());
        player.sendMessage(title);
        player.sendMessage(subtitle);
        player.sendMessage(Component.empty());
        player.sendMessage(instructions);
        player.sendMessage(Component.empty());

        // Send filter buttons in grid
        this.sendFilterButtonsGrid(player);

        player.sendMessage(Component.empty());
        player.sendMessage(headerLine);
        player.sendMessage(Component.empty());
    }

    /**
     * Sends filter buttons in an organized grid layout.
     */
    private void sendFilterButtonsGrid(final @NotNull Player player) {
        List<FilterOption> filterOptions = List.of(
                new FilterOption("player", "👤", "Filter by player name", "#e74c3c"),
                new FilterOption("currency", "💰", "Filter by currency type", "#f39c12"),
                new FilterOption("type", "📋", "Filter by log type", "#9b59b6"),
                new FilterOption("level", "⚠️", "Filter by severity level", "#e67e22"),
                new FilterOption("operation", "🔄", "Filter by operation type (deposit/withdraw)", "#2ecc71")
        );

        int buttonsPerRow = 3;
        for (int i = 0; i < filterOptions.size(); i += buttonsPerRow) {
            Component row = Component.text("  ");

            for (int j = 0; j < buttonsPerRow && (i + j) < filterOptions.size(); j++) {
                FilterOption option = filterOptions.get(i + j);

                if (j > 0) {
                    row = row.append(Component.text("  "));
                }

                Component filterButton = this.createEnhancedFilterButton(option);
                row = row.append(filterButton);
            }

            player.sendMessage(row);
        }
    }

    /**
     * Creates an enhanced filter button with hover effects.
     */
    private Component createEnhancedFilterButton(final @NotNull FilterOption option) {
        Component button = MINI_MESSAGE.deserialize(
                "<gradient:" + option.color + ":" + this.getDarkerShade(option.color) + ">[" +
                        option.icon + " " + option.name.toUpperCase() + "]</gradient>"
        );

        Component hoverText = MINI_MESSAGE.deserialize(
                "<gradient:#3498db:#2980b9>Filter Information</gradient>\n" +
                        "\n" +
                        "<dark_gray>▪ <gray>Type: <white>" + option.name.toUpperCase() + "</white></gray>\n" +
                        "<dark_gray>▪ <gray>Description: <white>" + option.description + "</white></gray>\n" +
                        "\n" +
                        "<gradient:#f1c40f:#f39c12>Click</gradient> <dark_gray>»</dark_gray> <gray>Apply this filter</gray>"
        );

        return button.hoverEvent(HoverEvent.showText(hoverText))
                .clickEvent(ClickEvent.suggestCommand("/pcurrencylog filter " + option.name + " "));
    }

    /**
     * Sends enhanced header for logs display.
     */
    private void sendEnhancedLogsHeader(
            final @NotNull Player player,
            final int currentPage,
            final int totalPages,
            final long totalLogs,
            final @Nullable LogFilter filter
    ) {
        Component headerLine = MINI_MESSAGE.deserialize(
                "<gradient:#9b59b6:#8e44ad>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬</gradient>"
        );

        Component title = MINI_MESSAGE.deserialize(
                "<gradient:#9b59b6:#8e44ad>📋 Currency Transaction Logs</gradient>"
        );

        Component pageInfo = MINI_MESSAGE.deserialize(
                "<gray>Page <gradient:#f39c12:#e67e22>" + currentPage + "</gradient><gray>/<gradient:#f39c12:#e67e22>" +
                        totalPages + "</gradient> <dark_gray>•</dark_gray> <gradient:#9b59b6:#8e44ad>" + totalLogs + "</gradient> <gray>total entries</gray>"
        );

        Component instructions = MINI_MESSAGE.deserialize(
                "<dark_gray>▪ <yellow>Click</yellow> <dark_gray>»</dark_gray> <gray>View detailed log information</gray>\n" +
                        "<dark_gray>▪ <yellow>Hover</yellow> <dark_gray>»</dark_gray> <gray>See transaction details and metadata</gray>"
        );

        player.sendMessage(Component.empty());
        player.sendMessage(headerLine);
        player.sendMessage(Component.empty());
        player.sendMessage(title);
        player.sendMessage(pageInfo);

        // Show active filters if any
        if (filter != null && filter.hasActiveFilters()) {
            Component filterInfo = MINI_MESSAGE.deserialize(
                    "<gradient:#f39c12:#e67e22>🔍 Active Filters:</gradient> <gray>" + filter.getFilterDescription() + "</gray>"
            );
            player.sendMessage(filterInfo);
        }

        player.sendMessage(Component.empty());
        player.sendMessage(instructions);
        player.sendMessage(Component.empty());
    }

    /**
     * Creates an enhanced log entry component with hover details.
     */
    private @NotNull Component createEnhancedLogEntryComponent(
            final @NotNull CurrencyLog log,
            final @NotNull Player player,
            final int index
    ) {
        String logLevelColor = this.getLogLevelColor(log.getLogLevel());
        String logTypeIcon = this.getLogTypeIcon(log.getLogType());
        String statusIcon = log.isSuccess() ? "✓" : "✗";

        Component logEntry = Component.text(" ")
                .append(MINI_MESSAGE.deserialize("<gradient:#95a5a6:#7f8c8d>" + String.format("%2d", index) + ".</gradient>"))
                .append(Component.text(" "))
                .append(MINI_MESSAGE.deserialize(logLevelColor + statusIcon + "</gradient>"))
                .append(Component.text(" "))
                .append(MINI_MESSAGE.deserialize("<gradient:#ecf0f1:#bdc3c7>" + logTypeIcon + "</gradient>"))
                .append(Component.text(" "))
                .append(MINI_MESSAGE.deserialize("<gradient:#ecf0f1:#95a5a6>" + log.getDescription() + "</gradient>"))
                .append(Component.text(" "))
                .append(MINI_MESSAGE.deserialize("<dark_gray>" + log.getTimestamp().format(DATE_FORMATTER) + "</dark_gray>"));

        Component hoverText = this.createEnhancedLogHoverEvent(log);

        return logEntry
                .hoverEvent(HoverEvent.showText(hoverText))
                .clickEvent(ClickEvent.runCommand("/pcurrencylog details " + log.getId()));
    }

    /**
     * Creates enhanced hover event for log entries.
     */
    private Component createEnhancedLogHoverEvent(final @NotNull CurrencyLog log) {
        Component hoverText = MINI_MESSAGE.deserialize(
                "<gradient:#9b59b6:#8e44ad>Transaction Log Details</gradient>\n" +
                        "\n" +
                        "<dark_gray>▪ <gray>Log ID: <gradient:#ecf0f1:#bdc3c7>" + log.getId() + "</gradient></gray>\n" +
                        "<dark_gray>▪ <gray>Type: <gradient:#f39c12:#e67e22>" + log.getLogType().name() + "</gradient></gray>\n" +
                        "<dark_gray>▪ <gray>Level: " + this.getLogLevelColor(log.getLogLevel()) + log.getLogLevel().name() + "</gradient></gray>\n" +
                        "<dark_gray>▪ <gray>Status: " + (log.isSuccess() ? "<gradient:#2ecc71:#27ae60>Success</gradient>" : "<gradient:#e74c3c:#c0392b>Failed</gradient>") + "</gray>\n"
        );

        if (log.getPlayerUuid() != null) {
            String playerName = this.getPlayerName(log.getPlayerUuid());
            hoverText = hoverText.append(MINI_MESSAGE.deserialize(
                    "<dark_gray>▪ <gray>Player: <gradient:#3498db:#2980b9>" + playerName + "</gradient></gray>\n"
            ));
        }

        if (log.getCurrency() != null) {
            hoverText = hoverText.append(MINI_MESSAGE.deserialize(
                    "<dark_gray>▪ <gray>Currency: <gradient:#f1c40f:#f39c12>" + log.getCurrency().getIdentifier() + "</gradient></gray>\n"
            ));
        }

        if (log.getOperationType() != null) {
            hoverText = hoverText.append(MINI_MESSAGE.deserialize(
                    "<dark_gray>▪ <gray>Operation: <gradient:#2ecc71:#27ae60>" + log.getOperationType().name() + "</gradient></gray>\n"
            ));
        }

        if (log.getAmount() != null) {
            hoverText = hoverText.append(MINI_MESSAGE.deserialize(
                    "<dark_gray>▪ <gray>Amount: <gradient:#2ecc71:#27ae60>" + String.format("%.2f", log.getAmount()) + "</gradient></gray>\n"
            ));
        }

        hoverText = hoverText.append(MINI_MESSAGE.deserialize(
                "<dark_gray>▪ <gray>Timestamp: <gradient:#95a5a6:#7f8c8d>" + log.getTimestamp().format(DATE_FORMATTER) + "</gradient></gray>\n" +
                        "\n" +
                        "<gradient:#f1c40f:#f39c12>Click</gradient> <dark_gray>»</dark_gray> <gray>View full details</gray>"
        ));

        return hoverText;
    }

    /**
     * Sends enhanced navigation bar with modern styling.
     */
    private void sendEnhancedNavigationBar(
            final @NotNull Player player,
            final int currentPage,
            final int totalPages
    ) {
        player.sendMessage(Component.empty());

        Component navigation = Component.text("  ");

        // Previous button
        if (currentPage > 1) {
            Component prevButton = MINI_MESSAGE.deserialize(
                    "<gradient:#2ecc71:#27ae60>[← Previous]</gradient>"
            ).hoverEvent(HoverEvent.showText(MINI_MESSAGE.deserialize(
                    "<gradient:#2ecc71:#27ae60>Go to page " + (currentPage - 1) + "</gradient>"
            ))).clickEvent(ClickEvent.runCommand("/pcurrencylog view " + (currentPage - 1)));

            navigation = navigation.append(prevButton);
        } else {
            navigation = navigation.append(MINI_MESSAGE.deserialize("<dark_gray>[← Previous]</dark_gray>"));
        }

        navigation = navigation.append(Component.text("  "));

        // Page indicator
        navigation = navigation.append(MINI_MESSAGE.deserialize(
                "<gradient:#9b59b6:#8e44ad>[Page " + currentPage + "/" + totalPages + "]</gradient>"
        ));

        navigation = navigation.append(Component.text("  "));

        // Next button
        if (currentPage < totalPages) {
            Component nextButton = MINI_MESSAGE.deserialize(
                    "<gradient:#2ecc71:#27ae60>[Next →]</gradient>"
            ).hoverEvent(HoverEvent.showText(MINI_MESSAGE.deserialize(
                    "<gradient:#2ecc71:#27ae60>Go to page " + (currentPage + 1) + "</gradient>"
            ))).clickEvent(ClickEvent.runCommand("/pcurrencylog view " + (currentPage + 1)));

            navigation = navigation.append(nextButton);
        } else {
            navigation = navigation.append(MINI_MESSAGE.deserialize("<dark_gray>[Next →]</dark_gray>"));
        }

        navigation = navigation.append(Component.text("  "));

        // Filter button
        Component filterButton = MINI_MESSAGE.deserialize(
                "<gradient:#3498db:#2980b9>[🔍 Filters]</gradient>"
        ).hoverEvent(HoverEvent.showText(MINI_MESSAGE.deserialize(
                "<gradient:#3498db:#2980b9>Open filter menu</gradient>"
        ))).clickEvent(ClickEvent.runCommand("/pcurrencylog filter"));

        navigation = navigation.append(filterButton);

        player.sendMessage(navigation);

        Component footerLine = MINI_MESSAGE.deserialize(
                "<gradient:#9b59b6:#8e44ad>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬</gradient>"
        );

        player.sendMessage(Component.empty());
        player.sendMessage(footerLine);
        player.sendMessage(Component.empty());
    }

    /**
     * Sends enhanced "no logs found" message.
     */
    private void sendNoLogsFoundMessage(final @NotNull Player player) {
        Component headerLine = MINI_MESSAGE.deserialize(
                "<gradient:#95a5a6:#7f8c8d>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬</gradient>"
        );

        Component message = MINI_MESSAGE.deserialize(
                "<gradient:#95a5a6:#7f8c8d>📋 No Currency Logs Found</gradient>\n" +
                        "\n" +
                        "<gray>No transaction logs match your current criteria.</gray>\n" +
                        "<gray>Try adjusting your filters or check back later!</gray>"
        );

        Component filterButton = MINI_MESSAGE.deserialize(
                "<gradient:#3498db:#2980b9>[🔍 Adjust Filters]</gradient>"
        ).hoverEvent(HoverEvent.showText(MINI_MESSAGE.deserialize(
                "<gradient:#3498db:#2980b9>Open filter menu</gradient>"
        ))).clickEvent(ClickEvent.runCommand("/pcurrencylog filter"));

        player.sendMessage(Component.empty());
        player.sendMessage(headerLine);
        player.sendMessage(Component.empty());
        player.sendMessage(message);
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  ").append(filterButton));
        player.sendMessage(Component.empty());
        player.sendMessage(headerLine);
        player.sendMessage(Component.empty());
    }

    /**
     * Sends enhanced usage message with modern styling.
     */
    private void sendEnhancedUsageMessage(final @NotNull Player player) {
        Component headerLine = MINI_MESSAGE.deserialize(
                "<gradient:#9b59b6:#8e44ad>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬</gradient>"
        );

        Component title = MINI_MESSAGE.deserialize(
                "<gradient:#9b59b6:#8e44ad>📋 Currency Log Commands</gradient>"
        );

        Component viewCommand = MINI_MESSAGE.deserialize(
                "<dark_gray>▪ <gradient:#2ecc71:#27ae60>/pcurrencylog view [page]</gradient> <dark_gray>»</dark_gray> <gray>View paginated transaction logs</gray>"
        );

        Component filterCommand = MINI_MESSAGE.deserialize(
                "<dark_gray>▪ <gradient:#3498db:#2980b9>/pcurrencylog filter</gradient> <dark_gray>»</dark_gray> <gray>Apply filters to log display</gray>"
        );

        Component clearCommand = MINI_MESSAGE.deserialize(
                "<dark_gray>▪ <gradient:#f39c12:#e67e22>/pcurrencylog clear</gradient> <dark_gray>»</dark_gray> <gray>Clear all active filters</gray>"
        );

        Component statsCommand = MINI_MESSAGE.deserialize(
                "<dark_gray>▪ <gradient:#9b59b6:#8e44ad>/pcurrencylog stats</gradient> <dark_gray>»</dark_gray> <gray>View log statistics and analytics</gray>"
        );

        Component exportCommand = MINI_MESSAGE.deserialize(
                "<dark_gray>▪ <gradient:#e74c3c:#c0392b>/pcurrencylog export</gradient> <dark_gray>»</dark_gray> <gray>Export logs to file (admin only)</gray>"
        );

        player.sendMessage(Component.empty());
        player.sendMessage(headerLine);
        player.sendMessage(Component.empty());
        player.sendMessage(title);
        player.sendMessage(Component.empty());
        player.sendMessage(viewCommand);
        player.sendMessage(filterCommand);
        player.sendMessage(clearCommand);
        player.sendMessage(statsCommand);
        player.sendMessage(exportCommand);
        player.sendMessage(Component.empty());
        player.sendMessage(headerLine);
        player.sendMessage(Component.empty());
    }

    /**
     * Gets the color gradient for a log level.
     */
    private @NotNull String getLogLevelColor(final @NotNull ELogLevel level) {
        return switch (level) {
            case INFO -> "<gradient:#2ecc71:#27ae60>";
            case WARNING -> "<gradient:#f39c12:#e67e22>";
            case ERROR -> "<gradient:#e74c3c:#c0392b>";
            case DEBUG -> "<gradient:#3498db:#2980b9>";
            case CRITICAL -> "<gradient:#8e44ad:#9b59b6>";
        };
    }

    /**
     * Gets the icon for a log type.
     */
    private @NotNull String getLogTypeIcon(final @NotNull ELogType type) {
        return switch (type) {
            case TRANSACTION -> "💰";
            case MANAGEMENT -> "⚙️";
            case SYSTEM -> "🔧";
            case ERROR -> "❌";
            case AUDIT -> "📋";
            case DEBUG -> "🐛";
        };
    }

    /**
     * Gets a darker shade of a hex color for gradients.
     */
    private String getDarkerShade(final @NotNull String hexColor) {
        return switch (hexColor) {
            case "#2ecc71" -> "#27ae60";
            case "#f39c12" -> "#e67e22";
            case "#e74c3c" -> "#c0392b";
            case "#3498db" -> "#2980b9";
            case "#9b59b6" -> "#8e44ad";
            default -> "#7f8c8d";
        };
    }

    /**
     * Gets the display name for a player UUID.
     */
    private @NotNull String getPlayerName(final @NotNull UUID playerUuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUuid);
        return player.getName() != null ? player.getName() : playerUuid.toString();
    }

    /**
     * Inner class to represent filter options.
     */
    private static class FilterOption {
        final String name;
        final String icon;
        final String description;
        final String color;

        FilterOption(String name, String icon, String description, String color) {
            this.name = name;
            this.icon = icon;
            this.description = description;
            this.color = color;
        }
    }

    /**
     * Inner class to represent log filters.
     */
    private static class LogFilter {
        private UUID playerUuid;
        private Long currencyId;
        private ELogType logType;
        private ELogLevel logLevel;
        private EChangeType operationType;

        /**
         * Returns whether activeFilters.
         */
        public boolean hasActiveFilters() {
            return playerUuid != null || currencyId != null || logType != null ||
                    logLevel != null || operationType != null;
        }

        /**
         * Gets filterDescription.
         */
        public String getFilterDescription() {
            StringBuilder description = new StringBuilder();

            if (playerUuid != null) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerUuid);
                description.append("Player: ").append(player.getName()).append(" ");
            }

            if (logType != null) {
                description.append("Type: ").append(logType.name()).append(" ");
            }

            if (logLevel != null) {
                description.append("Level: ").append(logLevel.name()).append(" ");
            }

            if (operationType != null) {
                description.append("Operation: ").append(operationType.name()).append(" ");
            }

            if (currencyId != null) {
                description.append("Currency ID: ").append(currencyId).append(" ");
            }

            return description.toString().trim();
        }
    }

    /**
     * Enhanced method to get currency logs with proper filtering support.
     * This method now handles operation type filtering at the application level
     * since the repository doesn't support it directly.
     */
    private CompletableFuture<List<CurrencyLog>> getCurrencyLogs(LogFilter filter, int page) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get logs from repository with supported filters
                List<CurrencyLog> allMatchingLogs;

                if (filter != null && (filter.logType != null || filter.logLevel != null ||
                        filter.playerUuid != null || filter.currencyId != null)) {
                    // Apply repository-level filters
                    allMatchingLogs = this.jexEconomyImpl.getCurrencyLogRepository().findByCriteria(
                            filter.logType,
                            filter.logLevel,
                            filter.playerUuid,
                            filter.currencyId,
                            null, // startTime
                            null, // endTime
                            Integer.MAX_VALUE // Get all matching records first
                    );
                } else {
                    // Get all logs if no repository-level filter is applied
                    allMatchingLogs = this.jexEconomyImpl.getCurrencyLogRepository().findAll(1, Integer.MAX_VALUE);
                }

                // Apply operation type filter at application level (since repository doesn't support it)
                if (filter != null && filter.operationType != null) {
                    allMatchingLogs = allMatchingLogs.stream()
                            .filter(log -> filter.operationType.equals(log.getOperationType()))
                            .collect(Collectors.toList());
                }

                // Debug logging
                LOGGER.info("Found " + allMatchingLogs.size() + " total matching logs after filtering");
                LOGGER.info("Requesting page " + page + " with " + LOGS_PER_PAGE + " logs per page");

                // Apply manual pagination
                if (allMatchingLogs.isEmpty()) {
                    return List.of();
                }

                // Calculate the actual slice for this page
                int offset = (page - 1) * LOGS_PER_PAGE;
                int startIndex = Math.min(offset, allMatchingLogs.size());
                int endIndex = Math.min(offset + LOGS_PER_PAGE, allMatchingLogs.size());

                if (startIndex >= allMatchingLogs.size()) {
                    // Page is beyond available data
                    return List.of();
                }

                List<CurrencyLog> pageResults = allMatchingLogs.subList(startIndex, endIndex);

                // Debug logging
                LOGGER.info("Returning " + pageResults.size() + " logs for page " + page);

                return pageResults;

            } catch (Exception e) {
                LOGGER.severe("Failed to retrieve currency logs: " + e.getMessage());
                e.printStackTrace();
                return List.of();
            }
        }, this.jexEconomyImpl.getExecutor());
    }

    /**
     * Enhanced method to get total log count with better error handling and operation filtering.
     */
    private CompletableFuture<Long> getTotalLogCount(LogFilter filter) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<CurrencyLog> allLogs;

                if (filter != null && (filter.logType != null || filter.logLevel != null ||
                        filter.playerUuid != null || filter.currencyId != null)) {
                    // Count with repository-level filters
                    allLogs = this.jexEconomyImpl.getCurrencyLogRepository().findByCriteria(
                            filter.logType,
                            filter.logLevel,
                            filter.playerUuid,
                            filter.currencyId,
                            null,
                            null,
                            Integer.MAX_VALUE // Get all matching logs to count them
                    );
                } else {
                    // Count all logs
                    allLogs = this.jexEconomyImpl.getCurrencyLogRepository().findAll(1, Integer.MAX_VALUE);
                }

                // Apply operation type filter at application level
                if (filter != null && filter.operationType != null) {
                    allLogs = allLogs.stream()
                            .filter(log -> filter.operationType.equals(log.getOperationType()))
                            .collect(Collectors.toList());
                }

                long count = allLogs.size();
                LOGGER.info("Total log count after filtering: " + count);

                return count;

            } catch (Exception e) {
                LOGGER.severe("Failed to count currency logs: " + e.getMessage());
                e.printStackTrace();
                return 0L;
            }
        }, this.jexEconomyImpl.getExecutor());
    }

    /**
     * Enhanced method to display currency logs with better error handling and debugging.
     */
    private void displayCurrencyLogs(final @NotNull Player player, final int page) {
        LogFilter filter = this.activeFilters.get(player.getUniqueId());

        // Debug message to player
        Component debugMessage = MINI_MESSAGE.deserialize(
                "<gradient:#f39c12:#e67e22>🔍 Searching for logs...</gradient>"
        );
        player.sendMessage(debugMessage);

        this.getCurrencyLogs(filter, page).thenAcceptAsync(logs -> {
            LOGGER.info("Retrieved " + logs.size() + " logs for display");

            if (logs.isEmpty()) {
                this.getTotalLogCount(filter).thenAcceptAsync(totalCount -> {
                    if (totalCount == 0) {
                        this.sendNoLogsFoundMessage(player);
                    } else {
                        // There are logs, but this page is empty (probably page number too high)
                        Component pageErrorMessage = MINI_MESSAGE.deserialize(
                                "<gradient:#e74c3c:#c0392b>✗ Page Not Found</gradient>\n" +
                                        "<gray>Page " + page + " is empty. There are " + totalCount + " total logs.</gray>\n" +
                                        "<gray>Try page 1 or use a lower page number.</gray>"
                        );
                        player.sendMessage(pageErrorMessage);
                    }
                }, this.jexEconomyImpl.getExecutor());
                return;
            }

            this.getTotalLogCount(filter).thenAcceptAsync(totalCount -> {
                int totalPages = (int) Math.ceil((double) totalCount / LOGS_PER_PAGE);

                this.sendEnhancedLogsHeader(player, page, totalPages, totalCount, filter);
                this.sendEnhancedLogEntries(player, logs);
                this.sendEnhancedNavigationBar(player, page, totalPages);
            }, this.jexEconomyImpl.getExecutor());
        }, this.jexEconomyImpl.getExecutor()).exceptionally(throwable -> {
            LOGGER.severe("Error displaying currency logs: " + throwable.getMessage());
            throwable.printStackTrace();

            Component errorMessage = MINI_MESSAGE.deserialize(
                    "<gradient:#e74c3c:#c0392b>✗ Error Loading Logs</gradient>\n" +
                            "<gray>An error occurred while loading currency logs.</gray>\n" +
                            "<gray>Please check the console for details.</gray>"
            );
            player.sendMessage(errorMessage);
            return null;
        });
    }

    /**
     * Handles the details action to show comprehensive information about a specific log entry.
     */
    private void handleDetailsAction(final @NotNull Player player, final @NotNull String[] args) {
        if (args.length < 2) {
            Component errorMessage = MINI_MESSAGE.deserialize(
                    "<gradient:#e74c3c:#c0392b>✗ Missing Log ID</gradient>\n" +
                            "<gray>Please specify a log ID to view details.</gray>\n" +
                            "<dark_gray>Usage: <yellow>/pcurrencylog details <log_id></yellow></dark_gray>"
            );
            player.sendMessage(errorMessage);
            return;
        }

        try {
            long logId = Long.parseLong(args[1]);
            this.displayLogDetails(player, logId);
        } catch (NumberFormatException e) {
            Component errorMessage = MINI_MESSAGE.deserialize(
                    "<gradient:#e74c3c:#c0392b>✗ Invalid Log ID</gradient>\n" +
                            "<gray>Log ID must be a valid number.</gray>\n" +
                            "<dark_gray>Provided: <white>" + args[1] + "</white></dark_gray>"
            );
            player.sendMessage(errorMessage);
        }
    }

    /**
     * Displays detailed information about a specific log entry.
     */
    private void displayLogDetails(final @NotNull Player player, final long logId) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return this.jexEconomyImpl.getCurrencyLogRepository().findById(logId).orElse(null);
            } catch (Exception e) {
                LOGGER.warning("Failed to retrieve log details for ID " + logId + ": " + e.getMessage());
                return null;
            }
        }, this.jexEconomyImpl.getExecutor()).thenAcceptAsync(log -> {
            if (log == null) {
                Component notFoundMessage = MINI_MESSAGE.deserialize(
                        "<gradient:#e74c3c:#c0392b>✗ Log Not Found</gradient>\n" +
                                "<gray>No log entry found with ID: <white>" + logId + "</white></gray>\n" +
                                "<dark_gray>The log may have been deleted or the ID is incorrect.</dark_gray>"
                );
                player.sendMessage(notFoundMessage);
                return;
            }

            this.sendEnhancedLogDetails(player, log);
        }, this.jexEconomyImpl.getExecutor()).exceptionally(throwable -> {
            LOGGER.severe("Error retrieving log details: " + throwable.getMessage());
            throwable.printStackTrace();

            Component errorMessage = MINI_MESSAGE.deserialize(
                    "<gradient:#e74c3c:#c0392b>✗ Error Loading Details</gradient>\n" +
                            "<gray>An error occurred while loading log details.</gray>\n" +
                            "<gray>Please check the console for more information.</gray>"
            );
            player.sendMessage(errorMessage);
            return null;
        });
    }

    /**
     * Sends enhanced detailed view of a specific log entry.
     */
    private void sendEnhancedLogDetails(final @NotNull Player player, final @NotNull CurrencyLog log) {
        Component headerLine = MINI_MESSAGE.deserialize(
                "<gradient:#9b59b6:#8e44ad>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬</gradient>"
        );

        String logLevelColor = this.getLogLevelColor(log.getLogLevel());
        String logTypeIcon = this.getLogTypeIcon(log.getLogType());
        String statusIcon = log.isSuccess() ? "✓" : "✗";
        String statusColor = log.isSuccess() ? "<gradient:#2ecc71:#27ae60>" : "<gradient:#e74c3c:#c0392b>";

        Component title = MINI_MESSAGE.deserialize(
                "<gradient:#9b59b6:#8e44ad>📋 Transaction Log Details</gradient>"
        );

        Component logIdInfo = MINI_MESSAGE.deserialize(
                "<gradient:#3498db:#2980b9>Log ID:</gradient> <gradient:#ecf0f1:#bdc3c7>" + log.getId() + "</gradient>"
        );

        // Basic Information Section
        Component basicInfoTitle = MINI_MESSAGE.deserialize(
                "<gradient:#f39c12:#e67e22>📊 Basic Information</gradient>"
        );

        Component basicInfo = MINI_MESSAGE.deserialize(
                "<dark_gray>▪ <gray>Timestamp: <gradient:#95a5a6:#7f8c8d>" + log.getTimestamp().format(DATE_FORMATTER) + "</gradient></gray>\n" +
                        "<dark_gray>▪ <gray>Type: " + logTypeIcon + " <gradient:#f39c12:#e67e22>" + log.getLogType().name() + "</gradient></gray>\n" +
                        "<dark_gray>▪ <gray>Level: " + logLevelColor + log.getLogLevel().name() + "</gradient></gray>\n" +
                        "<dark_gray>▪ <gray>Status: " + statusColor + statusIcon + " " + (log.isSuccess() ? "Success" : "Failed") + "</gradient></gray>\n" +
                        "<dark_gray>▪ <gray>Description: <gradient:#ecf0f1:#bdc3c7>" + log.getDescription() + "</gradient></gray>"
        );

        player.sendMessage(Component.empty());
        player.sendMessage(headerLine);
        player.sendMessage(Component.empty());
        player.sendMessage(title);
        player.sendMessage(logIdInfo);
        player.sendMessage(Component.empty());
        player.sendMessage(basicInfoTitle);
        player.sendMessage(basicInfo);

        // Player Information Section (if applicable)
        if (log.getPlayerUuid() != null) {
            Component playerInfoTitle = MINI_MESSAGE.deserialize(
                    "<gradient:#3498db:#2980b9>👤 Player Information</gradient>"
            );

            String playerName = this.getPlayerName(log.getPlayerUuid());
            StringBuilder playerInfoBuilder = new StringBuilder();
            playerInfoBuilder.append("<dark_gray>▪ <gray>Player: <gradient:#3498db:#2980b9>")
                    .append(playerName)
                    .append("</gradient></gray>\n");
            playerInfoBuilder.append("<dark_gray>▪ <gray>UUID: <gradient:#95a5a6:#7f8c8d>")
                    .append(log.getPlayerUuid().toString())
                    .append("</gradient></gray>\n");

            if (log.getInitiatorUuid() != null && !log.getInitiatorUuid().equals(log.getPlayerUuid())) {
                String initiatorName = this.getPlayerName(log.getInitiatorUuid());
                playerInfoBuilder.append("<dark_gray>▪ <gray>Initiated by: <gradient:#e67e22:#d35400>")
                        .append(initiatorName)
                        .append("</gradient></gray>\n");
            }

            if (log.getIpAddress() != null && !log.getIpAddress().isEmpty()) {
                playerInfoBuilder.append("<dark_gray>▪ <gray>IP Address: <gradient:#95a5a6:#7f8c8d>")
                        .append(log.getIpAddress())
                        .append("</gradient></gray>\n");
            }

            Component playerInfo = MINI_MESSAGE.deserialize(playerInfoBuilder.toString());

            player.sendMessage(Component.empty());
            player.sendMessage(playerInfoTitle);
            player.sendMessage(playerInfo);
        }

        // Currency & Transaction Information Section (if applicable)
        if (log.getCurrency() != null || log.getOperationType() != null) {
            Component transactionInfoTitle = MINI_MESSAGE.deserialize(
                    "<gradient:#2ecc71:#27ae60>💰 Transaction Information</gradient>"
            );

            StringBuilder transactionInfoBuilder = new StringBuilder();

            if (log.getCurrency() != null) {
                transactionInfoBuilder.append("<dark_gray>▪ <gray>Currency: <gradient:#f1c40f:#f39c12>")
                        .append(log.getCurrency().getIdentifier())
                        .append("</gradient></gray>\n");
                transactionInfoBuilder.append("<dark_gray>▪ <gray>Currency Name: <gradient:#f1c40f:#f39c12>")
                        .append(log.getCurrency().getIdentifier())
                        .append("</gradient></gray>\n");
            }

            if (log.getOperationType() != null) {
                transactionInfoBuilder.append("<dark_gray>▪ <gray>Operation: <gradient:#2ecc71:#27ae60>")
                        .append(log.getOperationType().name())
                        .append("</gradient></gray>\n");
            }

            if (log.getAmount() != null) {
                transactionInfoBuilder.append("<dark_gray>▪ <gray>Amount: <gradient:#2ecc71:#27ae60>")
                        .append(String.format("%.2f", log.getAmount()))
                        .append("</gradient></gray>\n");
            }

            if (log.getOldBalance() != null && log.getNewBalance() != null) {
                transactionInfoBuilder.append("<dark_gray>▪ <gray>Balance Change: <gradient:#95a5a6:#7f8c8d>")
                        .append(String.format("%.2f", log.getOldBalance()))
                        .append("</gradient> <dark_gray>→</dark_gray> <gradient:#2ecc71:#27ae60>")
                        .append(String.format("%.2f", log.getNewBalance()))
                        .append("</gradient></gray>\n");

                Double changeAmount = log.getChangeAmount();
                if (changeAmount != null) {
                    String changeColor = changeAmount >= 0 ? "<gradient:#2ecc71:#27ae60>" : "<gradient:#e74c3c:#c0392b>";
                    String changeSign = changeAmount >= 0 ? "+" : "";
                    transactionInfoBuilder.append("<dark_gray>▪ <gray>Net Change: ")
                            .append(changeColor)
                            .append(changeSign)
                            .append(String.format("%.2f", changeAmount))
                            .append("</gradient></gray>\n");
                }
            }

            if (log.getReason() != null && !log.getReason().isEmpty()) {
                transactionInfoBuilder.append("<dark_gray>▪ <gray>Reason: <gradient:#ecf0f1:#bdc3c7>")
                        .append(log.getReason())
                        .append("</gradient></gray>\n");
            }

            Component transactionInfo = MINI_MESSAGE.deserialize(transactionInfoBuilder.toString());

            player.sendMessage(Component.empty());
            player.sendMessage(transactionInfoTitle);
            player.sendMessage(transactionInfo);
        }

        // Error Information Section (if applicable)
        if (!log.isSuccess() && log.getErrorMessage() != null && !log.getErrorMessage().isEmpty()) {
            Component errorInfoTitle = MINI_MESSAGE.deserialize(
                    "<gradient:#e74c3c:#c0392b>❌ Error Information</gradient>"
            );

            Component errorInfo = MINI_MESSAGE.deserialize(
                    "<dark_gray>▪ <gray>Error Message: <gradient:#e74c3c:#c0392b>" + log.getErrorMessage() + "</gradient></gray>"
            );

            player.sendMessage(Component.empty());
            player.sendMessage(errorInfoTitle);
            player.sendMessage(errorInfo);
        }

        // Additional Details Section (if applicable)
        if (log.getDetails() != null && !log.getDetails().isEmpty()) {
            Component detailsInfoTitle = MINI_MESSAGE.deserialize(
                    "<gradient:#95a5a6:#7f8c8d>📝 Additional Details</gradient>"
            );

            // Split details into lines for better readability
            String[] detailLines = log.getDetails().split("\n");
            StringBuilder detailsBuilder = new StringBuilder();

            for (int i = 0; i < Math.min(detailLines.length, 10); i++) { // Limit to 10 lines
                detailsBuilder.append("<dark_gray>▪ <gray>")
                        .append(detailLines[i].trim())
                        .append("</gray>");
                if (i < Math.min(detailLines.length, 10) - 1) {
                    detailsBuilder.append("\n");
                }
            }

            if (detailLines.length > 10) {
                detailsBuilder.append("\n<dark_gray>▪ <gray>... and ")
                        .append(detailLines.length - 10)
                        .append(" more lines</gray>");
            }

            Component detailsInfo = MINI_MESSAGE.deserialize(detailsBuilder.toString());

            player.sendMessage(Component.empty());
            player.sendMessage(detailsInfoTitle);
            player.sendMessage(detailsInfo);
        }

        // Metadata Section (if applicable)
        if (log.getMetadata() != null && !log.getMetadata().isEmpty()) {
            Component metadataInfoTitle = MINI_MESSAGE.deserialize(
                    "<gradient:#9b59b6:#8e44ad>🔧 Metadata</gradient>"
            );

            Component metadataInfo = MINI_MESSAGE.deserialize(
                    "<dark_gray>▪ <gray>Raw Metadata: <gradient:#95a5a6:#7f8c8d>" + log.getMetadata() + "</gradient></gray>"
            );

            player.sendMessage(Component.empty());
            player.sendMessage(metadataInfoTitle);
            player.sendMessage(metadataInfo);
        }

        // Navigation Section
        Component navigationTitle = MINI_MESSAGE.deserialize(
                "<gradient:#3498db:#2980b9>🧭 Navigation</gradient>"
        );

        Component backButton = MINI_MESSAGE.deserialize(
                "<gradient:#3498db:#2980b9>[← Back to Logs]</gradient>"
        ).hoverEvent(HoverEvent.showText(MINI_MESSAGE.deserialize(
                "<gradient:#3498db:#2980b9>Return to log list</gradient>"
        ))).clickEvent(ClickEvent.runCommand("/pcurrencylog view"));

        Component filterButton = MINI_MESSAGE.deserialize(
                "<gradient:#f39c12:#e67e22>[🔍 Filters]</gradient>"
        ).hoverEvent(HoverEvent.showText(MINI_MESSAGE.deserialize(
                "<gradient:#f39c12:#e67e22>Open filter menu</gradient>"
        ))).clickEvent(ClickEvent.runCommand("/pcurrencylog filter"));

        Component navigation = Component.text("  ")
                .append(backButton)
                .append(Component.text("  "))
                .append(filterButton);

        player.sendMessage(Component.empty());
        player.sendMessage(navigationTitle);
        player.sendMessage(navigation);
        player.sendMessage(Component.empty());
        player.sendMessage(headerLine);
        player.sendMessage(Component.empty());
    }

    /**
     * Enhanced method to send log entries with better formatting and debugging.
     */
    private void sendEnhancedLogEntries(final @NotNull Player player, final @NotNull List<CurrencyLog> logs) {
        LOGGER.info("Sending " + logs.size() + " log entries to player " + player.getName());

        if (logs.isEmpty()) {
            Component emptyMessage = MINI_MESSAGE.deserialize(
                    "<gradient:#95a5a6:#7f8c8d>No log entries to display</gradient>"
            );
            player.sendMessage(emptyMessage);
            return;
        }

        for (int i = 0; i < logs.size(); i++) {
            CurrencyLog log = logs.get(i);

            // Debug log each entry
            LOGGER.info("Processing log entry " + (i + 1) + ": ID=" + log.getId() +
                    ", Description=" + log.getDescription() +
                    ", Type=" + log.getLogType() +
                    ", Operation=" + log.getOperationType() +
                    ", Success=" + log.isSuccess());

            Component logComponent = this.createEnhancedLogEntryComponent(log, player, i + 1);
            player.sendMessage(logComponent);
        }
    }

    private CompletableFuture<Boolean> exportLogsToFile(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create logs directory if it doesn't exist
                File logsDir = new File(this.jexEconomyImpl.getPlugin().getDataFolder(), "logs");
                if (!logsDir.exists()) {
                    logsDir.mkdirs();
                }

                // Generate filename with timestamp
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
                String filename = String.format("currency-logs-export_%s_%s.txt", player.getName(), timestamp);
                File exportFile = new File(logsDir, filename);

                // Get all logs for export
                List<CurrencyLog> allLogs = this.jexEconomyImpl.getCurrencyLogRepository().findAll(1, 512);

                // Write logs to file
                try (FileWriter writer = new FileWriter(exportFile, StandardCharsets.UTF_8)) {
                    writer.write("=".repeat(80) + "\n");
                    writer.write("JECURRENCY TRANSACTION LOGS EXPORT\n");
                    writer.write("=".repeat(80) + "\n");
                    writer.write("Export Date: " + LocalDateTime.now().format(DATE_FORMATTER) + "\n");
                    writer.write("Exported by: " + player.getName() + " (" + player.getUniqueId() + ")\n");
                    writer.write("Total Logs: " + allLogs.size() + "\n");
                    writer.write("=".repeat(80) + "\n\n");

                    for (CurrencyLog log : allLogs) {
                        writer.write("-".repeat(60) + "\n");
                        writer.write("Log ID: " + log.getId() + "\n");
                        writer.write("Timestamp: " + log.getTimestamp().format(DATE_FORMATTER) + "\n");
                        writer.write("Type: " + log.getLogType().name() + "\n");
                        writer.write("Level: " + log.getLogLevel().name() + "\n");
                        writer.write("Success: " + (log.isSuccess() ? "YES" : "NO") + "\n");

                        if (log.getPlayerUuid() != null) {
                            String playerName = this.getPlayerName(log.getPlayerUuid());
                            writer.write("Player: " + playerName + " (" + log.getPlayerUuid() + ")\n");
                        }

                        if (log.getCurrency() != null) {
                            writer.write("Currency: " + log.getCurrency().getIdentifier() + "\n");
                        }

                        if (log.getOperationType() != null) {
                            writer.write("Operation: " + log.getOperationType().name() + "\n");
                        }

                        if (log.getAmount() != null) {
                            writer.write("Amount: " + String.format("%.2f", log.getAmount()) + "\n");
                        }

                        if (log.getOldBalance() != null && log.getNewBalance() != null) {
                            writer.write("Balance Change: " + String.format("%.2f", log.getOldBalance()) +
                                    " → " + String.format("%.2f", log.getNewBalance()) + "\n");
                        }

                        writer.write("Description: " + log.getDescription() + "\n");

                        if (log.getDetails() != null && !log.getDetails().isEmpty()) {
                            writer.write("Details:\n" + log.getDetails() + "\n");
                        }

                        if (!log.isSuccess() && log.getErrorMessage() != null) {
                            writer.write("Error: " + log.getErrorMessage() + "\n");
                        }

                        writer.write("\n");
                    }

                    writer.write("=".repeat(80) + "\n");
                    writer.write("END OF EXPORT\n");
                    writer.write("=".repeat(80) + "\n");
                }

                // Send success message with file location
                Component successMessage = MINI_MESSAGE.deserialize(
                        "<gradient:#2ecc71:#27ae60>✓ Export Complete</gradient>\n" +
                                "<gray>Logs exported to: <white>" + filename + "</white></gray>\n" +
                                "<gray>Location: <white>plugins/JExEconomy/logs/</white></gray>"
                );
                player.sendMessage(successMessage);

                return true;

            } catch (Exception e) {
                LOGGER.severe("Failed to export currency logs: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, this.jexEconomyImpl.getExecutor());
    }

    private void displayEnhancedLogStatistics(Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                // Gather statistics
                List<CurrencyLog> allLogs = this.jexEconomyImpl.getCurrencyLogRepository().findAll(1, 512);

                // Calculate various statistics
                long totalLogs = allLogs.size();
                long successfulLogs = allLogs.stream().mapToLong(log -> log.isSuccess() ? 1 : 0).sum();
                long failedLogs = totalLogs - successfulLogs;

                Map<ELogType, Long> logTypeStats = allLogs.stream()
                        .collect(Collectors.groupingBy(CurrencyLog::getLogType, Collectors.counting()));

                Map<ELogLevel, Long> logLevelStats = allLogs.stream()
                        .collect(Collectors.groupingBy(CurrencyLog::getLogLevel, Collectors.counting()));

                // Get operation type statistics
                Map<EChangeType, Long> operationStats = allLogs.stream()
                        .filter(log -> log.getOperationType() != null)
                        .collect(Collectors.groupingBy(CurrencyLog::getOperationType, Collectors.counting()));

                // Get recent activity (last 24 hours)
                LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
                long recentLogs = allLogs.stream()
                        .mapToLong(log -> log.getTimestamp().isAfter(yesterday) ? 1 : 0)
                        .sum();

                // Get unique players count
                long uniquePlayers = allLogs.stream()
                        .filter(log -> log.getPlayerUuid() != null)
                        .map(CurrencyLog::getPlayerUuid)
                        .distinct()
                        .count();

                // Calculate total transaction volume
                double totalVolume = allLogs.stream()
                        .filter(log -> log.getAmount() != null && log.isSuccess())
                        .mapToDouble(CurrencyLog::getAmount)
                        .sum();

                // Send enhanced statistics display
                Bukkit.getScheduler().runTask(this.jexEconomyImpl.getPlugin(), () -> {
                    this.sendEnhancedStatisticsDisplay(player, totalLogs, successfulLogs, failedLogs,
                            logTypeStats, logLevelStats, operationStats, recentLogs, uniquePlayers, totalVolume);
                });

            } catch (Exception e) {
                LOGGER.warning("Failed to generate log statistics: " + e.getMessage());

                Bukkit.getScheduler().runTask(this.jexEconomyImpl.getPlugin(), () -> {
                    Component errorMessage = MINI_MESSAGE.deserialize(
                            "<gradient:#e74c3c:#c0392b>✗ Statistics Error</gradient>\n" +
                                    "<gray>Failed to generate log statistics. Please try again later.</gray>"
                    );
                    player.sendMessage(errorMessage);
                });
            }
        }, this.jexEconomyImpl.getExecutor());
    }

    private void sendEnhancedStatisticsDisplay(
            Player player, long totalLogs, long successfulLogs, long failedLogs,
            Map<ELogType, Long> logTypeStats, Map<ELogLevel, Long> logLevelStats,
            Map<EChangeType, Long> operationStats, long recentLogs, long uniquePlayers, double totalVolume
    ) {
        Component headerLine = MINI_MESSAGE.deserialize(
                "<gradient:#9b59b6:#8e44ad>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬</gradient>"
        );

        Component title = MINI_MESSAGE.deserialize(
                "<gradient:#9b59b6:#8e44ad>📊 Currency Log Statistics</gradient>"
        );

        Component overallStats = MINI_MESSAGE.deserialize(
                "<gradient:#3498db:#2980b9>📈 Overall Statistics</gradient>\n" +
                        "<dark_gray>▪ <gray>Total Logs: <gradient:#f1c40f:#f39c12>" + totalLogs + "</gradient></gray>\n" +
                        "<dark_gray>▪ <gray>Successful: <gradient:#2ecc71:#27ae60>" + successfulLogs + "</gradient> <dark_gray>(" +
                        String.format("%.1f", (double) successfulLogs / totalLogs * 100) + "%)</dark_gray></gray>\n" +
                        "<dark_gray>▪ <gray>Failed: <gradient:#e74c3c:#c0392b>" + failedLogs + "</gradient> <dark_gray>(" +
                        String.format("%.1f", (double) failedLogs / totalLogs * 100) + "%)</dark_gray></gray>\n" +
                        "<dark_gray>▪ <gray>Recent Activity (24h): <gradient:#f39c12:#e67e22>" + recentLogs + "</gradient></gray>\n" +
                        "<dark_gray>▪ <gray>Unique Players: <gradient:#9b59b6:#8e44ad>" + uniquePlayers + "</gradient></gray>\n" +
                        "<dark_gray>▪ <gray>Total Volume: <gradient:#2ecc71:#27ae60>" + String.format("%.2f", totalVolume) + "</gradient></gray>"
        );

        // Log Type Statistics
        Component typeStatsTitle = MINI_MESSAGE.deserialize(
                "<gradient:#f39c12:#e67e22>📋 Log Type Distribution</gradient>"
        );

        StringBuilder typeStatsBuilder = new StringBuilder();
        for (Map.Entry<ELogType, Long> entry : logTypeStats.entrySet()) {
            String typeIcon = this.getLogTypeIcon(entry.getKey());
            double percentage = (double) entry.getValue() / totalLogs * 100;
            typeStatsBuilder.append("<dark_gray>▪ <gray>")
                    .append(typeIcon).append(" ")
                    .append(entry.getKey().name())
                    .append(": <gradient:#ecf0f1:#bdc3c7>")
                    .append(entry.getValue())
                    .append("</gradient> <dark_gray>(")
                    .append(String.format("%.1f", percentage))
                    .append("%)</dark_gray></gray>\n");
        }

        Component typeStats = MINI_MESSAGE.deserialize(typeStatsBuilder.toString());

        // Log Level Statistics
        Component levelStatsTitle = MINI_MESSAGE.deserialize(
                "<gradient:#e74c3c:#c0392b>⚠️ Log Level Distribution</gradient>"
        );

        StringBuilder levelStatsBuilder = new StringBuilder();
        for (Map.Entry<ELogLevel, Long> entry : logLevelStats.entrySet()) {
            String levelColor = this.getLogLevelColor(entry.getKey());
            double percentage = (double) entry.getValue() / totalLogs * 100;
            levelStatsBuilder.append("<dark_gray>▪ <gray>")
                    .append(levelColor)
                    .append(entry.getKey().name())
                    .append("</gradient>: <gradient:#ecf0f1:#bdc3c7>")
                    .append(entry.getValue())
                    .append("</gradient> <dark_gray>(")
                    .append(String.format("%.1f", percentage))
                    .append("%)</dark_gray></gray>\n");
        }

        Component levelStats = MINI_MESSAGE.deserialize(levelStatsBuilder.toString());

        // Operation Type Statistics
        Component operationStatsTitle = MINI_MESSAGE.deserialize(
                "<gradient:#2ecc71:#27ae60>🔄 Operation Type Distribution</gradient>"
        );

        StringBuilder operationStatsBuilder = new StringBuilder();
        for (Map.Entry<EChangeType, Long> entry : operationStats.entrySet()) {
            double percentage = (double) entry.getValue() / totalLogs * 100;
            operationStatsBuilder.append("<dark_gray>▪ <gray>")
                    .append(entry.getKey().name())
                    .append(": <gradient:#ecf0f1:#bdc3c7>")
                    .append(entry.getValue())
                    .append("</gradient> <dark_gray>(")
                    .append(String.format("%.1f", percentage))
                    .append("%)</dark_gray></gray>\n");
        }

        Component operationStats2 = MINI_MESSAGE.deserialize(operationStatsBuilder.toString());

        // Send all components
        player.sendMessage(Component.empty());
        player.sendMessage(headerLine);
        player.sendMessage(Component.empty());
        player.sendMessage(title);
        player.sendMessage(Component.empty());
        player.sendMessage(overallStats);
        player.sendMessage(Component.empty());
        player.sendMessage(typeStatsTitle);
        player.sendMessage(typeStats);
        player.sendMessage(levelStatsTitle);
        player.sendMessage(levelStats);
        player.sendMessage(operationStatsTitle);
        player.sendMessage(operationStats2);
        player.sendMessage(headerLine);
        player.sendMessage(Component.empty());
    }

    // Enhanced filter application methods
    private void applyPlayerFilter(Player player, LogFilter filter, String playerName) {
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerName);

        if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
            Component errorMessage = MINI_MESSAGE.deserialize(
                    "<gradient:#e74c3c:#c0392b>✗ Player Not Found</gradient>\n" +
                            "<gray>Player <white>" + playerName + "</white> has never joined this server.</gray>"
            );
            player.sendMessage(errorMessage);
            return;
        }

        filter.playerUuid = targetPlayer.getUniqueId();
        this.activeFilters.put(player.getUniqueId(), filter);

        Component successMessage = MINI_MESSAGE.deserialize(
                "<gradient:#2ecc71:#27ae60>✓ Player Filter Applied</gradient>\n" +
                        "<gray>Now showing logs for player: <gradient:#3498db:#2980b9>" + targetPlayer.getName() + "</gradient></gray>\n" +
                        "<dark_gray>Use <yellow>/pcurrencylog view</yellow> to see filtered results</dark_gray>"
        );
        player.sendMessage(successMessage);
    }

    private void applyCurrencyFilter(Player player, LogFilter filter, String currencyIdentifier) {
        // Find currency by identifier
        Currency currency = this.jexEconomyImpl.getCurrencies().values().stream()
                .filter(c -> c.getIdentifier().equalsIgnoreCase(currencyIdentifier))
                .findFirst()
                .orElse(null);

        if (currency == null) {
            Component errorMessage = MINI_MESSAGE.deserialize(
                    "<gradient:#e74c3c:#c0392b>✗ Currency Not Found</gradient>\n" +
                            "<gray>Currency <white>" + currencyIdentifier + "</white> does not exist.</gray>\n" +
                            "<dark_gray>Available currencies: " +
                            this.jexEconomyImpl.getCurrencies().values().stream()
                                    .map(Currency::getIdentifier)
                                    .collect(Collectors.joining(", ")) + "</dark_gray>"
            );
            player.sendMessage(errorMessage);
            return;
        }

        filter.currencyId = currency.getId();
        this.activeFilters.put(player.getUniqueId(), filter);

        Component successMessage = MINI_MESSAGE.deserialize(
                "<gradient:#2ecc71:#27ae60>✓ Currency Filter Applied</gradient>\n" +
                        "<gray>Now showing logs for currency: <gradient:#f1c40f:#f39c12>" + currency.getIdentifier() + "</gradient></gray>\n" +
                        "<dark_gray>Use <yellow>/pcurrencylog view</yellow> to see filtered results</dark_gray>"
        );
        player.sendMessage(successMessage);
    }

    private void applyTypeFilter(Player player, LogFilter filter, String type) {
        try {
            ELogType logType = ELogType.valueOf(type.toUpperCase());
            filter.logType = logType;
            this.activeFilters.put(player.getUniqueId(), filter);

            String typeIcon = this.getLogTypeIcon(logType);
            Component successMessage = MINI_MESSAGE.deserialize(
                    "<gradient:#2ecc71:#27ae60>✓ Type Filter Applied</gradient>\n" +
                            "<gray>Now showing logs of type: " + typeIcon + " <gradient:#9b59b6:#8e44ad>" + logType.name() + "</gradient></gray>\n" +
                            "<dark_gray>Use <yellow>/pcurrencylog view</yellow> to see filtered results</dark_gray>"
            );
            player.sendMessage(successMessage);

        } catch (IllegalArgumentException e) {
            Component errorMessage = MINI_MESSAGE.deserialize(
                    "<gradient:#e74c3c:#c0392b>✗ Invalid Log Type</gradient>\n" +
                            "<gray>Unknown log type: <white>" + type + "</white></gray>\n" +
                            "<dark_gray>Valid types: " + String.join(", ", LOG_TYPES) + "</dark_gray>"
            );
            player.sendMessage(errorMessage);
        }
    }

    private void applyLevelFilter(Player player, LogFilter filter, String level) {
        try {
            ELogLevel logLevel = ELogLevel.valueOf(level.toUpperCase());
            filter.logLevel = logLevel;
            this.activeFilters.put(player.getUniqueId(), filter);

            String levelColor = this.getLogLevelColor(logLevel);
            Component successMessage = MINI_MESSAGE.deserialize(
                    "<gradient:#2ecc71:#27ae60>✓ Level Filter Applied</gradient>\n" +
                            "<gray>Now showing logs with level: " + levelColor + logLevel.name() + "</gradient></gray>\n" +
                            "<dark_gray>Use <yellow>/pcurrencylog view</yellow> to see filtered results</dark_gray>"
            );
            player.sendMessage(successMessage);

        } catch (IllegalArgumentException exception) {
            Component errorMessage = MINI_MESSAGE.deserialize(
                    "<gradient:#e74c3c:#c0392b>✗ Invalid Log Level</gradient>\n" +
                            "<gray>Unknown log level: <white>" + level + "</white></gray>\n" +
                            "<dark_gray>Valid levels: " + String.join(", ", LOG_LEVELS) + "</dark_gray>"
            );
            player.sendMessage(errorMessage);
        }
    }

    private void applyOperationFilter(Player player, LogFilter filter, String operation) {
        try {
            EChangeType operationType = EChangeType.valueOf(operation.toUpperCase());
            filter.operationType = operationType;
            this.activeFilters.put(player.getUniqueId(), filter);

            Component successMessage = MINI_MESSAGE.deserialize(
                    "<gradient:#2ecc71:#27ae60>✓ Operation Filter Applied</gradient>\n" +
                            "<gray>Now showing logs for operation: <gradient:#f39c12:#e67e22>" + operationType.name() + "</gradient></gray>\n" +
                            "<dark_gray>Use <yellow>/pcurrencylog view</yellow> to see filtered results</dark_gray>\n" +
                            "<dark_gray>Note: Operation filtering is applied at the application level</dark_gray>"
            );
            player.sendMessage(successMessage);

        } catch (IllegalArgumentException e) {
            Component errorMessage = MINI_MESSAGE.deserialize(
                    "<gradient:#e74c3c:#c0392b>✗ Invalid Operation Type</gradient>\n" +
                            "<gray>Unknown operation: <white>" + operation + "</white></gray>\n" +
                            "<dark_gray>Valid operations: " + String.join(", ", OPERATION_TYPES) + "</dark_gray>"
            );
            player.sendMessage(errorMessage);
        }
    }
}
