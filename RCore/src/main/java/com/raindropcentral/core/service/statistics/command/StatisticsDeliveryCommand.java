package com.raindropcentral.core.service.statistics.command;

import com.raindropcentral.core.service.statistics.StatisticsDeliveryService;
import com.raindropcentral.core.service.statistics.delivery.StatisticsDeliveryEngine;
import com.raindropcentral.core.service.statistics.queue.DeliveryPriority;
import com.raindropcentral.core.service.statistics.queue.StatisticsQueueManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin command for statistics delivery management.
 * Provides status, metrics, flush, pause/resume, and diagnostic commands.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class StatisticsDeliveryCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "rcore.admin.statistics";

    private final StatisticsDeliveryService service;
    private boolean diagnosticMode = false;

    public StatisticsDeliveryCommand(final @NotNull StatisticsDeliveryService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "status" -> handleStatus(sender);
            case "metrics" -> handleMetrics(sender);
            case "flush" -> handleFlush(sender);
            case "pause" -> handlePause(sender);
            case "resume" -> handleResume(sender);
            case "diagnostic" -> handleDiagnostic(sender);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    private boolean handleStatus(final CommandSender sender) {
        StatisticsQueueManager queueManager = service.getQueueManager();
        var queueStats = queueManager.getStatistics();

        sender.sendMessage(Component.text("=== Statistics Delivery Status ===", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("Running: ", NamedTextColor.YELLOW).append(Component.text(String.valueOf(service.isRunning()), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Paused: ", NamedTextColor.YELLOW).append(Component.text(String.valueOf(service.isPaused()), NamedTextColor.WHITE)));
        sender.sendMessage(Component.empty());

        sender.sendMessage(Component.text("Queue Status:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  Total Size: ", NamedTextColor.YELLOW).append(Component.text(String.valueOf(queueStats.totalSize()), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Backpressure: ", NamedTextColor.YELLOW).append(Component.text(queueStats.backpressureLevel().name(), NamedTextColor.WHITE)));

        sender.sendMessage(Component.text("Queue by Priority:", NamedTextColor.GOLD));
        for (DeliveryPriority priority : DeliveryPriority.values()) {
            int size = queueStats.sizeByPriority().getOrDefault(priority, 0);
            sender.sendMessage(Component.text("  " + priority.name() + ": ", NamedTextColor.YELLOW).append(Component.text(String.valueOf(size), NamedTextColor.WHITE)));
        }

        if (queueStats.oldestEntryAgeMs() > 0) {
            sender.sendMessage(Component.text("  Oldest Entry: ", NamedTextColor.YELLOW)
                .append(Component.text(formatDuration(queueStats.oldestEntryAgeMs()), NamedTextColor.WHITE)));
        }

        return true;
    }

    private boolean handleMetrics(final CommandSender sender) {
        StatisticsDeliveryEngine engine = service.getDeliveryEngine();
        var metrics = engine.getMetrics();

        sender.sendMessage(Component.text("=== Delivery Metrics ===", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("Total Deliveries: ", NamedTextColor.YELLOW).append(Component.text(String.valueOf(metrics.totalDeliveries()), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Successful: ", NamedTextColor.YELLOW).append(Component.text(String.valueOf(metrics.successfulDeliveries()), NamedTextColor.GREEN)));
        sender.sendMessage(Component.text("Failed: ", NamedTextColor.YELLOW).append(Component.text(String.valueOf(metrics.failedDeliveries()), NamedTextColor.RED)));
        sender.sendMessage(Component.text("Success Rate: ", NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", metrics.successRate() * 100), NamedTextColor.WHITE)));
        sender.sendMessage(Component.empty());

        sender.sendMessage(Component.text("Statistics Delivered: ", NamedTextColor.YELLOW)
            .append(Component.text(String.valueOf(metrics.totalStatisticsDelivered()), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Bytes Transmitted: ", NamedTextColor.YELLOW)
            .append(Component.text(formatBytes(metrics.totalBytesTransmitted()), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Average Latency: ", NamedTextColor.YELLOW)
            .append(Component.text(metrics.averageLatencyMs() + "ms", NamedTextColor.WHITE)));

        return true;
    }

    private boolean handleFlush(final CommandSender sender) {
        if (service.isPaused()) {
            sender.sendMessage(Component.text("Cannot flush while delivery is paused.", NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("Flushing statistics queue...", NamedTextColor.YELLOW));
        service.flushQueue();
        sender.sendMessage(Component.text("Queue flush initiated.", NamedTextColor.GREEN));

        return true;
    }

    private boolean handlePause(final CommandSender sender) {
        if (service.isPaused()) {
            sender.sendMessage(Component.text("Delivery is already paused.", NamedTextColor.YELLOW));
            return true;
        }

        service.pauseDelivery();
        sender.sendMessage(Component.text("Statistics delivery paused.", NamedTextColor.GREEN));

        return true;
    }

    private boolean handleResume(final CommandSender sender) {
        if (!service.isPaused()) {
            sender.sendMessage(Component.text("Delivery is not paused.", NamedTextColor.YELLOW));
            return true;
        }

        service.resumeDelivery();
        sender.sendMessage(Component.text("Statistics delivery resumed.", NamedTextColor.GREEN));

        return true;
    }

    private boolean handleDiagnostic(final CommandSender sender) {
        diagnosticMode = !diagnosticMode;

        if (diagnosticMode) {
            sender.sendMessage(Component.text("Diagnostic mode enabled. Detailed logging active.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Diagnostic mode disabled.", NamedTextColor.YELLOW));
        }

        return true;
    }

    private void sendHelp(final CommandSender sender) {
        sender.sendMessage(Component.text("=== Statistics Delivery Commands ===", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("/rcstats status", NamedTextColor.YELLOW).append(Component.text(" - Show delivery status", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/rcstats metrics", NamedTextColor.YELLOW).append(Component.text(" - Show performance metrics", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/rcstats flush", NamedTextColor.YELLOW).append(Component.text(" - Flush queue immediately", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/rcstats pause", NamedTextColor.YELLOW).append(Component.text(" - Pause delivery", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/rcstats resume", NamedTextColor.YELLOW).append(Component.text(" - Resume delivery", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/rcstats diagnostic", NamedTextColor.YELLOW).append(Component.text(" - Toggle diagnostic mode", NamedTextColor.WHITE)));
    }

    private String formatDuration(final long ms) {
        if (ms < 1000) return ms + "ms";
        if (ms < 60_000) return (ms / 1000) + "s";
        if (ms < 3600_000) return (ms / 60_000) + "m";
        return (ms / 3600_000) + "h";
    }

    private String formatBytes(final long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    @Override
    public @Nullable List<String> onTabComplete(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String alias,
        @NotNull String[] args
    ) {
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("status", "metrics", "flush", "pause", "resume", "diagnostic");
            return subCommands.stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
