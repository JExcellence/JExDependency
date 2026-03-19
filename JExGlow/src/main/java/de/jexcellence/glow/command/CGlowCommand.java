package de.jexcellence.glow.command;

import com.raindropcentral.commands.ServerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.glow.JExGlow;
import de.jexcellence.glow.factory.GlowFactory;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Console command handler for managing player glow effects.
 * <p>
 * Allows console to enable or disable the glowing effect for any player.
 * </p>
 * <p>
 * Usage:
 * - /glow on &lt;player&gt; - Enable glow effect for a player
 * - /glow off &lt;player&gt; - Disable glow effect for a player
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Command
@SuppressWarnings("unused")
public class CGlowCommand extends ServerCommand {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("CGlowCommand");
    private final JExGlow plugin;

    public CGlowCommand(@NotNull CGlowCommandSection commandSection, @NotNull JExGlow plugin) {
        super(commandSection);
        this.plugin = plugin;
    }

    @Override
    protected void onPlayerInvocation(@NotNull ConsoleCommandSender console, @NotNull String label, @NotNull String[] args) {
        // Validate arguments
        if (args.length < 2) {
            sendUsage(console);
            return;
        }

        String action = args[0].toLowerCase();
        String targetName = args[1];

        // Validate action
        if (!action.equals("on") && !action.equals("off")) {
            sendUsage(console);
            return;
        }

        // Resolve target player (offline or online)
        OfflinePlayer targetPlayer = this.offlinePlayerParameter(args, 1, true);
        
        if (targetPlayer == null || !targetPlayer.hasPlayedBefore()) {
            console.sendMessage("§c[JExGlow] Player not found: " + targetName);
            return;
        }

        // Execute action
        try {
            var glowService = GlowFactory.getService();
            
            if (action.equals("on")) {
                handleEnableGlow(console, targetPlayer);
            } else {
                handleDisableGlow(console, targetPlayer);
            }
        } catch (IllegalStateException e) {
            console.sendMessage("§c[JExGlow] An error occurred while processing the command.");
            LOGGER.log(Level.SEVERE, "GlowFactory not initialized", e);
        }
    }

    private void handleEnableGlow(@NotNull ConsoleCommandSender console, @NotNull OfflinePlayer target) {
        var glowService = GlowFactory.getService();
        
        glowService.enableGlow(target.getUniqueId(), null)
            .thenAccept(success -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        console.sendMessage("§a[JExGlow] Glow effect enabled for " + target.getName());
                        LOGGER.log(Level.INFO, "Console enabled glow for player: " + target.getName());
                    } else {
                        console.sendMessage("§c[JExGlow] Failed to enable glow effect.");
                    }
                });
            })
            .exceptionally(throwable -> {
                LOGGER.log(Level.SEVERE, "Failed to enable glow for " + target.getName(), throwable);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    console.sendMessage("§c[JExGlow] Database error occurred.");
                });
                return null;
            });
    }

    private void handleDisableGlow(@NotNull ConsoleCommandSender console, @NotNull OfflinePlayer target) {
        var glowService = GlowFactory.getService();
        
        glowService.disableGlow(target.getUniqueId())
            .thenAccept(success -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        console.sendMessage("§a[JExGlow] Glow effect disabled for " + target.getName());
                        LOGGER.log(Level.INFO, "Console disabled glow for player: " + target.getName());
                    } else {
                        console.sendMessage("§c[JExGlow] Failed to disable glow effect.");
                    }
                });
            })
            .exceptionally(throwable -> {
                LOGGER.log(Level.SEVERE, "Failed to disable glow for " + target.getName(), throwable);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    console.sendMessage("§c[JExGlow] Database error occurred.");
                });
                return null;
            });
    }

    private void sendUsage(@NotNull ConsoleCommandSender console) {
        console.sendMessage("§c[JExGlow] Usage: /glow <on|off> <player>");
    }
}
