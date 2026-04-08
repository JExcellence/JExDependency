package de.jexcellence.home.command.home;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.home.JExHome;
import de.jexcellence.home.exception.HomeNotFoundException;
import de.jexcellence.home.exception.WorldNotLoadedException;
import de.jexcellence.home.factory.HomeFactory;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command handler for the /home command using HomeFactory.
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Command
@SuppressWarnings("unused")
public class PHome extends PlayerCommand {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("PHome");
    private final JExHome jexHome;

    public PHome(@NotNull PHomeSection commandSection, @NotNull JExHome jexHome) {
        super(commandSection);
        this.jexHome = jexHome;
    }

    @Override
    protected void onPlayerInvocation(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        if (hasNoPermission(player, EPHomePermission.HOME)) return;

        // No arguments - open home overview GUI
        if (args.length == 0) {
            jexHome.getViewRouter().openHomeOverview(player);
            return;
        }

        var homeName = stringParameter(args, 0);

        try {
            var factory = HomeFactory.getInstance();
            factory.teleportToHome(player, homeName, () -> {
                new I18n.Builder("home.teleported", player)
                    .withPlaceholder("home_name", homeName)
                    .includePrefix()
                    .build()
                    .sendMessage();
            });
        } catch (HomeNotFoundException e) {
            new I18n.Builder("home.does_not_exist", player)
                .withPlaceholder("home_name", homeName)
                .includePrefix()
                .build()
                .sendMessage();
        } catch (WorldNotLoadedException e) {
            new I18n.Builder("home.world_not_loaded", player)
                .withPlaceholder("world", e.getWorldName())
                .includePrefix()
                .build()
                .sendMessage();
        } catch (IllegalStateException e) {
            new I18n.Builder("home.error.internal", player).includePrefix().build().sendMessage();
            LOGGER.log(Level.SEVERE, "HomeFactory not initialized", e);
        }
    }

    @Override
    protected List<String> onPlayerTabCompletion(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        if (hasNoPermission(player, EPHomePermission.HOME)) return new ArrayList<>();

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
