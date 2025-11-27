package com.raindropcentral.rdq.reward;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommandReward extends AbstractReward {

    private static final Logger LOGGER = Logger.getLogger(CommandReward.class.getName());
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([a-zA-Z_]+)%");

    @JsonProperty("command")
    private final String command;

    @JsonProperty("executeAsPlayer")
    private final boolean executeAsPlayer;

    @JsonProperty("delay")
    private final long delayTicks;

    public CommandReward(@NotNull String command) {
        this(command, false, 0L);
    }

    @JsonCreator
    public CommandReward(@JsonProperty("command") @NotNull String command,
                        @JsonProperty("executeAsPlayer") boolean executeAsPlayer,
                        @JsonProperty("delay") long delayTicks) {
        super(Type.COMMAND, "reward.command");
        
        if (command == null || command.trim().isEmpty()) {
            throw new IllegalArgumentException("Command cannot be null or empty");
        }
        
        this.command = command;
        this.executeAsPlayer = executeAsPlayer;
        this.delayTicks = Math.max(0, delayTicks);
    }

    @Override
    public @NotNull java.util.concurrent.CompletableFuture<Boolean> grant(@NotNull Player player) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            var processedCommand = replacePlaceholders(command, player);

            if (delayTicks > 0) {
                var plugin = Bukkit.getPluginManager().getPlugin("RaindropQuests");
                Bukkit.getScheduler().runTaskLater(plugin, () -> executeCommand(player, processedCommand), delayTicks);
            } else {
                executeCommand(player, processedCommand);
            }
            return true;
        });
    }

    @Override
    public double getEstimatedValue() { return 0.0; }

    @NotNull
    public String getCommand() {
        return command;
    }

    public boolean isExecuteAsPlayer() {
        return executeAsPlayer;
    }

    public long getDelayTicks() {
        return delayTicks;
    }

    private void executeCommand(@NotNull Player player, @NotNull String command) {
        try {
            if (executeAsPlayer) {
                player.performCommand(command);
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to execute command reward: " + command, exception);
        }
    }

    @NotNull
    private String replacePlaceholders(@NotNull String input, @NotNull Player player) {
        var placeholders = new HashMap<String, String>();
        var location = player.getLocation();
        
        placeholders.put("player", player.getName());
        placeholders.put("player_name", player.getName());
        placeholders.put("uniqueId", player.getUniqueId().toString());
        placeholders.put("world", player.getWorld().getName());
        placeholders.put("x", String.valueOf(location.getBlockX()));
        placeholders.put("y", String.valueOf(location.getBlockY()));
        placeholders.put("z", String.valueOf(location.getBlockZ()));
        placeholders.put("level", String.valueOf(player.getLevel()));
        placeholders.put("health", String.valueOf(player.getHealth()));

        var matcher = PLACEHOLDER_PATTERN.matcher(input);
        var result = new StringBuffer();

        while (matcher.find()) {
            var placeholder = matcher.group(1).toLowerCase();
            var replacement = placeholders.getOrDefault(placeholder, matcher.group(0));
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}