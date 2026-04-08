package de.jexcellence.home.view.bedrock;

import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.home.JExHome;
import de.jexcellence.home.database.entity.Home;
import de.jexcellence.home.factory.HomeFactory;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bedrock ModalForm for confirming home deletion.
 * <p>
 * This form displays a confirmation dialog with home details, allowing Bedrock players
 * to confirm or cancel home deletion using native Bedrock UI elements.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public final class BedrockDeleteHomeForm {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("JExHome");

    private BedrockDeleteHomeForm() {
        // Utility class - prevent instantiation
    }

    /**
     * Shows the delete home confirmation form to a Bedrock player.
     *
     * @param player the Bedrock player to show the form to
     * @param plugin the JExHome plugin instance
     * @param home   the home to be deleted
     */
    public static void show(@NotNull Player player, @NotNull JExHome plugin, @NotNull Home home) {
        var form = buildForm(player, plugin, home);
        sendForm(player, form);
    }

    private static ModalForm buildForm(
        @NotNull Player player,
        @NotNull JExHome plugin,
        @NotNull Home home
    ) {
        var r18n = plugin.getPlatform().getTranslationManager().getR18n();
        
        String title;
        String content;
        String confirmButton;
        String cancelButton;
        
        try {
            title = r18n.message("bedrock.delete_home.title").toPlainString(player);
            content = r18n.message("bedrock.delete_home.content")
                .placeholder("home_name", home.getHomeName())
                .toPlainString(player);
            confirmButton = r18n.message("bedrock.delete_home.confirm_button").toPlainString(player);
            cancelButton = r18n.message("bedrock.delete_home.cancel_button").toPlainString(player);
        } catch (Exception e) {
            title = "Delete Home";
            content = buildFallbackContent(home);
            confirmButton = "Delete";
            cancelButton = "Cancel";
        }

        return ModalForm.builder()
            .title(title)
            .content(content)
            .button1(confirmButton)
            .button2(cancelButton)
            .validResultHandler(response -> {
                if (response.clickedFirst()) {
                    // Delete button clicked
                    deleteHome(player, plugin, home);
                } else {
                    // Cancel button clicked - return to overview
                    BedrockHomeOverviewForm.show(player, plugin);
                }
            })
            .closedOrInvalidResultHandler(response -> {
                // Form was closed - return to overview
                BedrockHomeOverviewForm.show(player, plugin);
            })
            .build();
    }

    private static String buildFallbackContent(@NotNull Home home) {
        return String.format(
            "Are you sure you want to delete '%s'?\n\n" +
            "Location: %s\n" +
            "World: %s\n" +
            "Visits: %d\n\n" +
            "This action cannot be undone.",
            home.getHomeName(),
            home.getFormattedLocation(),
            home.getWorldName(),
            home.getVisitCount()
        );
    }

    private static void deleteHome(
        @NotNull Player player,
        @NotNull JExHome plugin,
        @NotNull Home home
    ) {
        try {
            var factory = HomeFactory.getInstance();
            factory.deleteHome(player, home.getHomeName())
                .thenAccept(deleted -> {
                    if (deleted) {
                        // Success - send message
                        new de.jexcellence.jextranslate.i18n.I18n.Builder("delhome.deleted", player)
                            .includePrefix()
                            .withPlaceholder("home_name", home.getHomeName())
                            .build()
                            .sendMessage();
                    } else {
                        // Home not found or already deleted
                        new de.jexcellence.jextranslate.i18n.I18n.Builder("home.does_not_exist", player)
                            .includePrefix()
                            .withPlaceholder("home_name", home.getHomeName())
                            .build()
                            .sendMessage();
                    }

                    // Return to home overview
                    BedrockHomeOverviewForm.show(player, plugin);
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, "Failed to delete home for Bedrock player", throwable);
                    
                    // Send error message
                    new de.jexcellence.jextranslate.i18n.I18n.Builder("delhome.error", player)
                        .includePrefix()
                        .withPlaceholder("home_name", home.getHomeName())
                        .build()
                        .sendMessage();

                    // Return to overview
                    BedrockHomeOverviewForm.show(player, plugin);
                    return null;
                });
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to delete home via HomeFactory", e);
            sendErrorMessage(player);
            BedrockHomeOverviewForm.show(player, plugin);
        }
    }

    private static void sendForm(@NotNull Player player, @NotNull ModalForm form) {
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
