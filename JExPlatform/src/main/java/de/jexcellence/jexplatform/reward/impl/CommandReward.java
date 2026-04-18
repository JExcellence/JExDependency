package de.jexcellence.jexplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.jexplatform.reward.AbstractReward;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Executes a console command as a reward with player-specific placeholders.
 *
 * <p>Supported placeholders: {@code {player}}, {@code {uuid}}, {@code {world}},
 * {@code {x}}, {@code {y}}, {@code {z}}.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class CommandReward extends AbstractReward {

    @JsonProperty("command") private final String command;

    public CommandReward(@JsonProperty("command") @NotNull String command) {
        super("COMMAND");
        this.command = command;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        var loc = player.getLocation();
        var resolved = command
                .replace("{player}", player.getName())
                .replace("{uuid}", player.getUniqueId().toString())
                .replace("{world}", loc.getWorld() != null ? loc.getWorld().getName() : "world")
                .replace("{x}", String.valueOf(loc.getBlockX()))
                .replace("{y}", String.valueOf(loc.getBlockY()))
                .replace("{z}", String.valueOf(loc.getBlockZ()));

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
        return CompletableFuture.completedFuture(true);
    }

    @Override public @NotNull String descriptionKey() { return "reward.command"; }
}
