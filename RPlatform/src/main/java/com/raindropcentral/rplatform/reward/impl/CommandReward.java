package com.raindropcentral.rplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reward that executes a command.
 * <p>
 * This reward supports placeholder replacement and can execute commands
 * as either the player or console. It includes error handling for
 * command execution failures.
 * </p>
 *
 * @author RaindropCentral
 * @version 2.0.0
 * @since TBD
 */
@JsonTypeName("COMMAND")
public final class CommandReward extends AbstractReward {

    private static final Logger LOGGER = Logger.getLogger(CommandReward.class.getName());

    private final String command;
    private final boolean executeAsPlayer;
    private final long delayTicks;

    @JsonCreator
    public CommandReward(
        @JsonProperty("command") @NotNull String command,
        @JsonProperty("executeAsPlayer") boolean executeAsPlayer,
        @JsonProperty("delayTicks") long delayTicks
    ) {
        this.command = command;
        this.executeAsPlayer = executeAsPlayer;
        this.delayTicks = Math.max(0, delayTicks);
    }

    @Override
    public @NotNull String getTypeId() {
        return "COMMAND";
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        final String processedCommand = replacePlaceholders(command, player);
        
        // Get any plugin for scheduling (use first available)
        final Plugin plugin = Bukkit.getPluginManager().getPlugins()[0];

        final Runnable task = () -> {
            try {
                if (executeAsPlayer) {
                    player.performCommand(processedCommand);
                    LOGGER.fine("Executed command as player " + player.getName() + ": " + processedCommand);
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
                    LOGGER.fine("Executed command as console for " + player.getName() + ": " + processedCommand);
                }
                future.complete(true);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to execute command reward: " + processedCommand, e);
                future.complete(false);
            }
        };
        
        if (delayTicks > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
        
        return future;
    }

    /**
     * Replaces placeholders in the command string.
     *
     * @param input  the command string
     * @param player the player
     * @return the processed command
     */
    private String replacePlaceholders(@NotNull String input, @NotNull Player player) {
        return input
            .replace("{player}", player.getName())
            .replace("{uuid}", player.getUniqueId().toString())
            .replace("{world}", player.getWorld().getName())
            .replace("{x}", String.valueOf(player.getLocation().getBlockX()))
            .replace("{y}", String.valueOf(player.getLocation().getBlockY()))
            .replace("{z}", String.valueOf(player.getLocation().getBlockZ()));
    }

    @Override
    public double getEstimatedValue() {
        return 0.0;
    }

    public String getCommand() {
        return command;
    }

    public boolean isExecuteAsPlayer() {
        return executeAsPlayer;
    }

    public long getDelayTicks() {
        return delayTicks;
    }

    @Override
    public void validate() {
        if (command == null || command.trim().isEmpty()) {
            throw new IllegalArgumentException("Command cannot be empty");
        }
    }
}
