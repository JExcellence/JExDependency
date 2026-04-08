package de.jexcellence.multiverse.command.spawn;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.jextranslate.i18n.I18n;
import de.jexcellence.multiverse.JExMultiverse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command handler for the /spawn command.
 * <p>
 * Teleports the player to the appropriate spawn location using
 * the MultiverseAdapter's spawn resolution logic.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Command
@SuppressWarnings("unused")
public class PSpawn extends PlayerCommand {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("PSpawn");

    private final JExMultiverse multiverse;

    public PSpawn(
            @NotNull PSpawnSection commandSection,
            @NotNull JExMultiverse multiverse
    ) {
        super(commandSection);
        this.multiverse = multiverse;
    }

    @Override
    protected void onPlayerInvocation(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        if (hasNoPermission(player, ESpawnPermission.SPAWN)) return;

        // Send teleporting message
        new I18n.Builder("spawn.teleporting_to_spawn", player)
                .includePrefix()
                .build()
                .sendMessage();

        // Use the adapter to handle spawn teleportation
        multiverse.getMultiverseAdapter().spawn(player, "spawn.teleported")
                .thenAccept(success -> {
                    if (!success) {
                        Bukkit.getScheduler().runTask(multiverse.getPlugin(), () ->
                                new I18n.Builder("spawn.spawn_not_found", player)
                                        .includePrefix()
                                        .build()
                                        .sendMessage()
                        );
                    }
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to teleport player to spawn: " + player.getName(), throwable);
                    Bukkit.getScheduler().runTask(multiverse.getPlugin(), () ->
                            new I18n.Builder("spawn.teleport_failed", player)
                                    .includePrefix()
                                    .build()
                                    .sendMessage()
                    );
                    return null;
                });
    }

    @Override
    protected List<String> onPlayerTabCompletion(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        // No tab completion for spawn command
        return new ArrayList<>();
    }
}
