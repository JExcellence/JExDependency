/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.core.service.statistics.vanilla.command;

import com.raindropcentral.core.service.statistics.vanilla.CollectionResult;
import com.raindropcentral.core.service.statistics.vanilla.CollectionStatistics;
import com.raindropcentral.core.service.statistics.vanilla.VanillaStatisticCollectionService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Command handler for vanilla statistics management and diagnostics.
 * <p>
 * Provides subcommands for:
 * <ul>
 *   <li>{@code /rcstats vanilla status} - Display collection status</li>
 *   <li>{@code /rcstats vanilla collect [player]} - Trigger immediate collection</li>
 *   <li>{@code /rcstats vanilla cache clear [player]} - Clear cache</li>
 *   <li>{@code /rcstats vanilla metrics} - Display collection metrics</li>
 * </ul>
 */
public class VanillaStatisticsCommand implements CommandExecutor, TabCompleter {
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault());
    
    private final VanillaStatisticCollectionService collectionService;
    private final JavaPlugin plugin;
    
    /**
     * Creates a new vanilla statistics command handler.
     *
     * @param collectionService the vanilla statistic collection service
     * @param plugin the plugin instance for scheduling
     */
    public VanillaStatisticsCommand(VanillaStatisticCollectionService collectionService, JavaPlugin plugin) {
        this.collectionService = collectionService;
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2 || !args[0].equalsIgnoreCase("vanilla")) {
            return false;
        }
        
        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "status" -> handleStatus(sender);
            case "collect" -> handleCollect(sender, args);
            case "cache" -> handleCache(sender, args);
            case "metrics" -> handleMetrics(sender);
            default -> {
                sender.sendMessage(Component.text("Unknown subcommand: " + subCommand, NamedTextColor.RED));
                return false;
            }
        }
        
        return true;
    }
    
    private void handleStatus(CommandSender sender) {
        if (!sender.hasPermission("rcore.stats.vanilla.status")) {
            sender.sendMessage(Component.text("No permission", NamedTextColor.RED));
            return;
        }
        
        CollectionStatistics stats = collectionService.getStatistics();
        
        sender.sendMessage(Component.text("=== Vanilla Statistics Status ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("Total Collections: ", NamedTextColor.GRAY)
            .append(Component.text(stats.totalCollections(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Total Statistics: ", NamedTextColor.GRAY)
            .append(Component.text(stats.totalStatistics(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Average Duration: ", NamedTextColor.GRAY)
            .append(Component.text(stats.averageDuration() + "ms", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Cache Size: ", NamedTextColor.GRAY)
            .append(Component.text(stats.cacheSize() + " players", NamedTextColor.WHITE)));
    }
    
    private void handleCollect(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rcore.stats.vanilla.collect")) {
            sender.sendMessage(Component.text("No permission", NamedTextColor.RED));
            return;
        }
        
        if (args.length >= 3) {
            // Collect for specific player
            String playerName = args[2];
            Player target = Bukkit.getPlayer(playerName);
            
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + playerName, NamedTextColor.RED));
                return;
            }
            
            sender.sendMessage(Component.text("Collecting statistics for " + target.getName() + "...", NamedTextColor.YELLOW));
            
            long startTime = System.currentTimeMillis();
            collectionService.collectForPlayer(target.getUniqueId()).thenAccept(result -> {
                long duration = System.currentTimeMillis() - startTime;
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(Component.text("Collected ", NamedTextColor.GREEN)
                        .append(Component.text(result.statistics().size(), NamedTextColor.WHITE))
                        .append(Component.text(" statistics in ", NamedTextColor.GREEN))
                        .append(Component.text(duration + "ms", NamedTextColor.WHITE)));
                });
            }).exceptionally(error -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(Component.text("Collection failed: " + error.getMessage(), NamedTextColor.RED));
                });
                return null;
            });
        } else {
            // Collect for all players
            sender.sendMessage(Component.text("Collecting statistics for all online players...", NamedTextColor.YELLOW));
            
            long startTime = System.currentTimeMillis();
            collectionService.collectAll().thenAccept(result -> {
                long duration = System.currentTimeMillis() - startTime;
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(Component.text("Collected ", NamedTextColor.GREEN)
                        .append(Component.text(result.statistics().size(), NamedTextColor.WHITE))
                        .append(Component.text(" statistics from ", NamedTextColor.GREEN))
                        .append(Component.text(result.playerCount(), NamedTextColor.WHITE))
                        .append(Component.text(" players in ", NamedTextColor.GREEN))
                        .append(Component.text(duration + "ms", NamedTextColor.WHITE)));
                });
            }).exceptionally(error -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(Component.text("Collection failed: " + error.getMessage(), NamedTextColor.RED));
                });
                return null;
            });
        }
    }
    
    private void handleCache(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rcore.stats.vanilla.cache")) {
            sender.sendMessage(Component.text("No permission", NamedTextColor.RED));
            return;
        }
        
        if (args.length < 3 || !args[2].equalsIgnoreCase("clear")) {
            sender.sendMessage(Component.text("Usage: /rcstats vanilla cache clear [player]", NamedTextColor.RED));
            return;
        }
        
        if (args.length >= 4) {
            // Clear cache for specific player
            String playerName = args[3];
            Player target = Bukkit.getPlayer(playerName);
            
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + playerName, NamedTextColor.RED));
                return;
            }
            
            collectionService.clearPlayerCache(target.getUniqueId());
            sender.sendMessage(Component.text("Cleared cache for " + target.getName(), NamedTextColor.GREEN));
        } else {
            // Clear all cache
            collectionService.clearAllCache();
            sender.sendMessage(Component.text("Cleared all vanilla statistics cache", NamedTextColor.GREEN));
        }
    }
    
    private void handleMetrics(CommandSender sender) {
        if (!sender.hasPermission("rcore.stats.vanilla.metrics")) {
            sender.sendMessage(Component.text("No permission", NamedTextColor.RED));
            return;
        }
        
        CollectionStatistics stats = collectionService.getStatistics();
        
        sender.sendMessage(Component.text("=== Vanilla Statistics Metrics ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.empty());
        
        sender.sendMessage(Component.text("Collection Statistics:", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  Total Collections: ", NamedTextColor.GRAY)
            .append(Component.text(stats.totalCollections(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Total Statistics: ", NamedTextColor.GRAY)
            .append(Component.text(stats.totalStatistics(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Average Duration: ", NamedTextColor.GRAY)
            .append(Component.text(stats.averageDuration() + "ms", NamedTextColor.WHITE)));
        
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Cache Statistics:", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  Cache Size: ", NamedTextColor.GRAY)
            .append(Component.text(stats.cacheSize() + " players", NamedTextColor.WHITE)));
        
        // Performance indicators
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Performance:", NamedTextColor.YELLOW));
        
        long avgDuration = stats.averageDuration();
        Component perfIndicator;
        if (avgDuration < 50) {
            perfIndicator = Component.text("Excellent", NamedTextColor.GREEN);
        } else if (avgDuration < 100) {
            perfIndicator = Component.text("Good", NamedTextColor.YELLOW);
        } else {
            perfIndicator = Component.text("Slow", NamedTextColor.RED);
        }
        
        sender.sendMessage(Component.text("  Status: ", NamedTextColor.GRAY)
            .append(perfIndicator));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("vanilla");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("vanilla")) {
            completions.add("status");
            completions.add("collect");
            completions.add("cache");
            completions.add("metrics");
        } else if (args.length == 3) {
            if (args[1].equalsIgnoreCase("collect")) {
                // Add online player names
                Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            } else if (args[1].equalsIgnoreCase("cache")) {
                completions.add("clear");
            }
        } else if (args.length == 4 && args[1].equalsIgnoreCase("cache") && args[2].equalsIgnoreCase("clear")) {
            // Add online player names for cache clear
            Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
        }
        
        return completions;
    }
}
