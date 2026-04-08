package de.jexcellence.home.command.delhome;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.home.JExHome;
import de.jexcellence.home.factory.HomeFactory;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command handler for the /delhome command using HomeFactory.
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Command
@SuppressWarnings("unused")
public class PDelHome extends PlayerCommand {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("PDelHome");
    private final JExHome jexHome;

    public PDelHome(@NotNull PDelHomeSection commandSection, @NotNull JExHome jexHome) {
        super(commandSection);
        this.jexHome = jexHome;
    }

    @Override
    protected void onPlayerInvocation(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        if (hasNoPermission(player, EPDelHomePermission.DELHOME)) return;

        if (args.length == 0) {
            // No arguments - show usage message
            new I18n.Builder("delhome.usage", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        var homeName = stringParameter(args, 0);

        try {
            var factory = HomeFactory.getInstance();
            
            // Find the home first to show confirmation
            factory.getHomeService().findHome(player.getUniqueId(), homeName)
                .thenAccept(homeOpt -> {
                    if (homeOpt.isEmpty()) {
                        new I18n.Builder("delhome.does_not_exist", player)
                            .withPlaceholder("home_name", homeName)
                            .includePrefix()
                            .build()
                            .sendMessage();
                        return;
                    }
                    
                    // Show delete confirmation - MUST run on main thread
                    jexHome.getPlatform().getScheduler().runSync(() -> {
                        jexHome.getViewRouter().openDeleteConfirmation(player, homeOpt.get());
                    });
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to find home for " + player.getName(), throwable);
                    new I18n.Builder("home.error.internal", player).includePrefix().build().sendMessage();
                    return null;
                });
        } catch (IllegalStateException e) {
            new I18n.Builder("home.error.internal", player).includePrefix().build().sendMessage();
            LOGGER.log(Level.SEVERE, "HomeFactory not initialized", e);
        }
    }

    @Override
    protected List<String> onPlayerTabCompletion(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        if (hasNoPermission(player, EPDelHomePermission.DELHOME)) return new ArrayList<>();

        if (args.length == 1) {
            try {
                var factory = HomeFactory.getInstance();
                var homes = factory.getPlayerHomes(player.getUniqueId()).join();
                // Keep original case for home names
                var homeNames = homes.stream().map(h -> h.getHomeName()).toList();
                return StringUtil.copyPartialMatches(args[0], homeNames, new ArrayList<>());
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }
}
