package de.jexcellence.glow.command;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.glow.JExGlow;
import de.jexcellence.glow.factory.GlowFactory;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command handler for managing player glow effects.
 * <p>
 * Allows administrators to enable or disable the glowing effect for online players.
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
public class PGlowCommand extends PlayerCommand {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("GlowCommand");
    private final JExGlow plugin;

    public PGlowCommand(@NotNull PGlowCommandSection commandSection, @NotNull JExGlow plugin) {
        super(commandSection);
        this.plugin = plugin;
    }

    @Override
    protected void onPlayerInvocation(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        // Validate arguments
        if (args.length < 2) {
            sendUsage(player);
            return;
        }

        String action = args[0].toLowerCase();
        String targetName = args[1];

        // Validate action
        if (!action.equals("on") && !action.equals("off")) {
            sendUsage(player);
            return;
        }

        // Resolve target player
        Player targetPlayer = Bukkit.getPlayer(targetName);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            new I18n.Builder("glow.command.player-not-found", player)
                .withPlaceholder("player", targetName)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        // Execute action
        try {
            var glowService = GlowFactory.getService();
            
            if (action.equals("on")) {
                handleEnableGlow(player, targetPlayer);
            } else {
                handleDisableGlow(player, targetPlayer);
            }
        } catch (IllegalStateException e) {
            new I18n.Builder("glow.error.general", player)
                .includePrefix()
                .build()
                .sendMessage();
            LOGGER.log(Level.SEVERE, "GlowFactory not initialized", e);
        }
    }

    private void handleEnableGlow(@NotNull Player admin, @NotNull Player target) {
        var glowService = GlowFactory.getService();
        
        glowService.enableGlow(target.getUniqueId(), admin.getUniqueId())
            .thenAccept(success -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        new I18n.Builder("glow.command.enabled", admin)
                            .withPlaceholder("player", target.getName())
                            .includePrefix()
                            .build()
                            .sendMessage();
                    } else {
                        new I18n.Builder("glow.error.general", admin)
                            .includePrefix()
                            .build()
                            .sendMessage();
                    }
                });
            })
            .exceptionally(throwable -> {
                LOGGER.log(Level.SEVERE, "Failed to enable glow for " + target.getName(), throwable);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    new I18n.Builder("glow.error.database", admin)
                        .includePrefix()
                        .build()
                        .sendMessage();
                });
                return null;
            });
    }

    private void handleDisableGlow(@NotNull Player admin, @NotNull Player target) {
        var glowService = GlowFactory.getService();
        
        glowService.disableGlow(target.getUniqueId())
            .thenAccept(success -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        new I18n.Builder("glow.command.disabled", admin)
                            .withPlaceholder("player", target.getName())
                            .includePrefix()
                            .build()
                            .sendMessage();
                    } else {
                        new I18n.Builder("glow.error.general", admin)
                            .includePrefix()
                            .build()
                            .sendMessage();
                    }
                });
            })
            .exceptionally(throwable -> {
                LOGGER.log(Level.SEVERE, "Failed to disable glow for " + target.getName(), throwable);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    new I18n.Builder("glow.error.database", admin)
                        .includePrefix()
                        .build()
                        .sendMessage();
                });
                return null;
            });
    }

    private void sendUsage(@NotNull Player player) {
        new I18n.Builder("glow.command.usage", player)
            .includePrefix()
            .build()
            .sendMessage();
    }

    @Override
    protected @NotNull java.util.List<String> onPlayerTabCompletion(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            // First argument: suggest "on" or "off"
            String input = args[0].toLowerCase();
            return java.util.Arrays.asList("on", "off").stream()
                .filter(option -> option.startsWith(input))
                .toList();
        } else if (args.length == 2) {
            // Second argument: suggest online player names
            String input = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(input))
                .sorted()
                .toList();
        }
        
        return java.util.Collections.emptyList();
    }
}
