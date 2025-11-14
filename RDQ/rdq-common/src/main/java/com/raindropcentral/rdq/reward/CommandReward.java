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

/**
 * Represents a reward that executes a server command when granted to a player.
 * <p>
 * The command may include placeholders such as {@code %player%}, {@code %uuid%}, and
 * coordinate tokens. These placeholders are replaced with the target player's data when the
 * reward executes.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
public final class CommandReward extends AbstractReward {

    private static final Logger LOGGER = Logger.getLogger(CommandReward.class.getName());
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([a-zA-Z_]+)%");

    @JsonProperty("command")
    private final String command;

    @JsonProperty("executeAsPlayer")
    private final boolean executeAsPlayer;

    @JsonProperty("delay")
    private final long delayTicks;

    /**
     * Constructs a new {@code CommandReward} with the specified command.
     *
     * @param command the command to execute when the reward is applied
     */
    public CommandReward(final @NotNull String command) {
        this(command, false, 0L);
    }

    /**
     * Constructs a new {@code CommandReward} with full configuration.
     *
     * @param command         the command to execute
     * @param executeAsPlayer whether to execute as the player (true) or console (false)
     * @param delayTicks      delay in ticks before executing the command
     */
    @JsonCreator
    public CommandReward(
            @JsonProperty("command") final @NotNull String command,
            @JsonProperty("executeAsPlayer") final boolean executeAsPlayer,
            @JsonProperty("delay") final long delayTicks
    ) {
        super(Type.COMMAND);
        
        if (command == null || command.trim().isEmpty()) {
            throw new IllegalArgumentException("Command cannot be null or empty");
        }
        
        this.command = command;
        this.executeAsPlayer = executeAsPlayer;
        this.delayTicks = Math.max(0, delayTicks);
    }

    /**
     * Executes the configured command for the supplied player, optionally after a delay.
     *
     * @param player the player receiving the reward
     */
    @Override
    public void apply(final @NotNull Player player) {
        final String processedCommand = this.replacePlaceholders(this.command, player);

        if (this.delayTicks > 0) {
            Bukkit.getScheduler().runTaskLater(
                    Bukkit.getPluginManager().getPlugin("RaindropQuests"),
                    () -> this.executeCommand(player, processedCommand),
                    this.delayTicks
            );
        } else {
            this.executeCommand(player, processedCommand);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getDescriptionKey() {
        return "reward.command";
    }

    /**
     * Retrieves the raw command configured for this reward.
     *
     * @return the command string
     */
    @NotNull
    public String getCommand() {
        return this.command;
    }

    /**
     * Indicates whether the command should be executed with the player's permissions.
     *
     * @return {@code true} if the player executes the command, {@code false} for console execution
     */
    public boolean isExecuteAsPlayer() {
        return this.executeAsPlayer;
    }

    /**
     * Provides the configured delay before the command is executed.
     *
     * @return the delay in ticks
     */
    public long getDelayTicks() {
        return this.delayTicks;
    }

    /**
     * Performs the actual command execution, handling both console and player contexts.
     *
     * @param player  the player receiving the reward
     * @param command the fully processed command to execute
     */
    private void executeCommand(final @NotNull Player player, final @NotNull String command) {
        try {
            if (this.executeAsPlayer) {
                player.performCommand(command);
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to execute command reward: " + command, exception);
        }
    }

    /**
     * Replaces supported placeholder tokens within the supplied command string with data from
     * the provided player.
     *
     * @param input  the command string containing placeholders
     * @param player the player supplying placeholder values
     * @return the command string with placeholders resolved
     */
    @NotNull
    private String replacePlaceholders(final @NotNull String input, final @NotNull Player player) {
        final Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("player_name", player.getName());
        placeholders.put("uniqueId", player.getUniqueId().toString());
        placeholders.put("world", player.getWorld().getName());
        placeholders.put("x", String.valueOf(player.getLocation().getBlockX()));
        placeholders.put("y", String.valueOf(player.getLocation().getBlockY()));
        placeholders.put("z", String.valueOf(player.getLocation().getBlockZ()));
        placeholders.put("level", String.valueOf(player.getLevel()));
        placeholders.put("health", String.valueOf(player.getHealth()));

        final Matcher matcher = PLACEHOLDER_PATTERN.matcher(input);
        final StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            final String placeholder = matcher.group(1).toLowerCase();
            final String replacement = placeholders.getOrDefault(placeholder, matcher.group(0));
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}