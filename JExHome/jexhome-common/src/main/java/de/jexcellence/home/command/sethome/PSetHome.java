package de.jexcellence.home.command.sethome;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.home.JExHome;
import de.jexcellence.home.exception.HomeLimitReachedException;
import de.jexcellence.home.factory.HomeFactory;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command handler for the /sethome command.
 * <p>
 * Creates a new home at the player's current location or overwrites
 * an existing home with the same name using HomeFactory.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Command
@SuppressWarnings("unused")
public class PSetHome extends PlayerCommand {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("PSetHome");
    private final JExHome jexHome;

    public PSetHome(@NotNull PSetHomeSection commandSection, @NotNull JExHome jexHome) {
        super(commandSection);
        this.jexHome = jexHome;
    }

    @Override
    protected void onPlayerInvocation(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        if (hasNoPermission(player, EPSetHomePermission.SETHOME)) return;

        var homeName = args.length == 1 ? stringParameter(args, 0) : jexHome.getHomeConfig().getDefaultHomeName();

        try {
            var factory = HomeFactory.getInstance();
            factory.createHome(player, homeName)
                .thenAccept(home -> {
                    // Check if it was an update or create
                    factory.getHomeService().findHome(player.getUniqueId(), homeName)
                        .thenAccept(existing -> {
                            var messageKey = existing.isPresent() && existing.get().getVisitCount() > 0 
                                ? "sethome.home_overwritten" 
                                : "sethome.created";
                            new I18n.Builder(messageKey, player)
                                .withPlaceholder("home_name", homeName)
                                .includePrefix()
                                .build()
                                .sendMessage();
                        });
                })
                .exceptionally(throwable -> {
                    handleError(player, throwable);
                    return null;
                });
        } catch (IllegalStateException e) {
            new I18n.Builder("home.error.internal", player).includePrefix().build().sendMessage();
            LOGGER.log(Level.SEVERE, "HomeFactory not initialized", e);
        }
    }

    private void handleError(Player player, Throwable throwable) {
        var cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        
        if (cause instanceof HomeLimitReachedException ex) {
            new I18n.Builder("sethome.home_limit_reached", player)
                .withPlaceholders(Map.of("current", String.valueOf(ex.getCurrentCount()), "max", String.valueOf(ex.getMaxLimit())))
                .includePrefix()
                .build()
                .sendMessage();
        } else {
            LOGGER.log(Level.SEVERE, "Failed to create home for " + player.getName(), throwable);
            new I18n.Builder("home.error.internal", player).includePrefix().build().sendMessage();
        }
    }

    @Override
    protected List<String> onPlayerTabCompletion(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        if (hasNoPermission(player, EPSetHomePermission.SETHOME)) return new ArrayList<>();

        if (args.length == 1) {
            try {
                var factory = HomeFactory.getInstance();
                var homes = factory.getPlayerHomes(player.getUniqueId()).join();
                return homes.stream()
                    .map(home -> home.getHomeName().toLowerCase())
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .toList();
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }
}
