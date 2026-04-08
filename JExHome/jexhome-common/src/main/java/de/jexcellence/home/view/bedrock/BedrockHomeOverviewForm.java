package de.jexcellence.home.view.bedrock;

import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.home.JExHome;
import de.jexcellence.home.database.entity.Home;
import de.jexcellence.home.factory.HomeFactory;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bedrock SimpleForm for displaying the home overview.
 * <p>
 * This form displays a list of player homes as buttons, allowing Bedrock players
 * to teleport to their homes or create new ones using native Bedrock UI elements.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public final class BedrockHomeOverviewForm {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("JExHome");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private BedrockHomeOverviewForm() {
        // Utility class - prevent instantiation
    }

    /**
     * Shows the home overview form to a Bedrock player.
     *
     * @param player the Bedrock player to show the form to
     * @param plugin the JExHome plugin instance
     */
    public static void show(@NotNull Player player, @NotNull JExHome plugin) {
        plugin.getHomeService().getPlayerHomes(player.getUniqueId())
            .thenAccept(homes -> {
                var sortedHomes = homes.stream()
                    .sorted(Comparator.comparing(Home::getHomeName))
                    .toList();

                var form = buildForm(player, plugin, sortedHomes);
                sendForm(player, form);
            })
            .exceptionally(throwable -> {
                LOGGER.log(Level.WARNING, "Failed to load homes for Bedrock form", throwable);
                sendErrorMessage(player);
                return null;
            });
    }

    private static SimpleForm buildForm(
        @NotNull Player player,
        @NotNull JExHome plugin,
        @NotNull List<Home> homes
    ) {
        var maxHomes = plugin.getHomeService().getMaxHomesForPlayer(player);
        var r18n = plugin.getPlatform().getTranslationManager().getR18n();
        
        // Get translated strings - use toPlainString for Bedrock forms (strips formatting)
        String title;
        String content;
        String createButton;
        
        try {
            title = r18n.message("bedrock.home_overview.title")
                .placeholder("count", String.valueOf(homes.size()))
                .placeholder("max", String.valueOf(maxHomes))
                .toPlainString(player);
            content = r18n.message("bedrock.home_overview.content")
                .toPlainString(player);
            createButton = r18n.message("bedrock.home_overview.create_button")
                .toPlainString(player);
        } catch (Exception e) {
            // Fallback to hardcoded English if translation fails
            title = "Your Homes (" + homes.size() + "/" + maxHomes + ")";
            content = "Select a home to teleport to, or create a new one.";
            createButton = "+ Create New Home";
        }
        
        var builder = SimpleForm.builder()
            .title(title)
            .content(content);

        // Add home buttons
        for (Home home : homes) {
            var description = formatHomeDescription(home);
            builder.button(home.getHomeName() + "\n" + description);
        }

        // Add create new home button
        builder.button(createButton);

        // Handle button clicks
        builder.validResultHandler(response -> {
            int clickedIndex = response.clickedButtonId();

            if (clickedIndex < homes.size()) {
                // Teleport to selected home
                var selectedHome = homes.get(clickedIndex);
                teleportToHome(player, selectedHome, plugin);
            } else {
                // Create new home button clicked
                BedrockSetHomeForm.show(player, plugin);
            }
        });

        builder.closedOrInvalidResultHandler(response -> {
            // Form was closed or invalid - no action needed
        });

        return builder.build();
    }

    private static String formatHomeDescription(@NotNull Home home) {
        var worldName = home.getWorldName();
        var location = home.getFormattedLocation();
        var visitCount = home.getVisitCount();
        var lastVisited = formatLastVisited(home.getLastVisited());

        return String.format(
            "%s | %s\nVisits: %d | Last: %s",
            worldName,
            location,
            visitCount,
            lastVisited
        );
    }

    private static String formatLastVisited(LocalDateTime lastVisited) {
        if (lastVisited == null) {
            return "Never";
        }
        return lastVisited.format(DATE_FORMAT);
    }

    private static void teleportToHome(
        @NotNull Player player,
        @NotNull Home home,
        @NotNull JExHome plugin
    ) {
        if (home.getLocation() == null || home.getLocation().getWorld() == null) {
            new de.jexcellence.jextranslate.i18n.I18n.Builder("home.world_not_loaded", player)
                .includePrefix()
                .withPlaceholder("world", home.getWorldName())
                .build()
                .sendMessage();
            return;
        }

        try {
            var factory = HomeFactory.getInstance();
            factory.teleportToHome(player, home.getHomeName(), () -> {
                new de.jexcellence.jextranslate.i18n.I18n.Builder("home.teleported", player)
                    .includePrefix()
                    .withPlaceholder("home_name", home.getHomeName())
                    .build()
                    .sendMessage();
            });
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to teleport player via HomeFactory", e);
            sendErrorMessage(player);
        }
    }

    private static void sendForm(@NotNull Player player, @NotNull SimpleForm form) {
        try {
            FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to send Bedrock form to player", e);
            sendErrorMessage(player);
        }
    }

    private static void sendErrorMessage(@NotNull Player player) {
        new de.jexcellence.jextranslate.i18n.I18n.Builder("home.error.internal", player)
            .includePrefix()
            .build()
            .sendMessage();
    }
}
