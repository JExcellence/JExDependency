package com.raindropcentral.rplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the CommandReward API type.
 */
@JsonTypeName("COMMAND")
public final class CommandReward extends AbstractReward {

    private final String command;
    private final boolean executeAsPlayer;
    private final long delayTicks;

    /**
     * Executes CommandReward.
     */
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

    /**
     * Gets typeId.
     */
    @Override
    public @NotNull String getTypeId() {
        return "COMMAND";
    }

    /**
     * Executes grant.
     */
    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        String processedCommand = replacePlaceholders(command, player);
        
        Runnable task = () -> {
            try {
                if (executeAsPlayer) {
                    player.performCommand(processedCommand);
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
                }
                future.complete(true);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        };
        
        if (delayTicks > 0) {
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugins()[0],
                task,
                delayTicks
            );
        } else {
            Bukkit.getScheduler().runTask(
                Bukkit.getPluginManager().getPlugins()[0],
                task
            );
        }
        
        return future;
    }

    private String replacePlaceholders(@NotNull String input, @NotNull Player player) {
        return input
            .replace("{player}", player.getName())
            .replace("{uuid}", player.getUniqueId().toString())
            .replace("{world}", player.getWorld().getName())
            .replace("{x}", String.valueOf(player.getLocation().getBlockX()))
            .replace("{y}", String.valueOf(player.getLocation().getBlockY()))
            .replace("{z}", String.valueOf(player.getLocation().getBlockZ()));
    }

    /**
     * Gets estimatedValue.
     */
    @Override
    public double getEstimatedValue() {
        return 0.0;
    }

    /**
     * Gets command.
     */
    public String getCommand() {
        return command;
    }

    /**
     * Returns whether executeAsPlayer.
     */
    public boolean isExecuteAsPlayer() {
        return executeAsPlayer;
    }

    /**
     * Gets delayTicks.
     */
    public long getDelayTicks() {
        return delayTicks;
    }

    /**
     * Executes validate.
     */
    @Override
    public void validate() {
        if (command == null || command.trim().isEmpty()) {
            throw new IllegalArgumentException("Command cannot be empty");
        }
    }
}
