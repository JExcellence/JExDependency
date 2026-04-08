package de.jexcellence.home.view.bedrock;

import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.home.JExHome;
import de.jexcellence.home.factory.HomeFactory;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bedrock CustomForm for creating a new home.
 * <p>
 * This form displays a text input field for the home name, allowing Bedrock players
 * to create new homes using native Bedrock UI elements.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public final class BedrockSetHomeForm {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("JExHome");

    /**
     * Home name validation pattern.
     * - Alphanumeric characters (including Unicode letters like ä, ü, ö)
     * - Underscores and hyphens allowed
     * - No spaces or special characters
     * - Length between 1 and 32 characters
     */
    private static final String HOME_NAME_PATTERN = "^[\\p{L}\\p{N}_-]{1,32}$";

    private BedrockSetHomeForm() {
        // Utility class - prevent instantiation
    }

    /**
     * Shows the set home form to a Bedrock player.
     *
     * @param player the Bedrock player to show the form to
     * @param plugin the JExHome plugin instance
     */
    public static void show(@NotNull Player player, @NotNull JExHome plugin) {
        var form = buildForm(player, plugin);
        sendForm(player, form);
    }

    private static CustomForm buildForm(@NotNull Player player, @NotNull JExHome plugin) {
        var r18n = plugin.getPlatform().getTranslationManager().getR18n();
        
        String title;
        String inputLabel;
        String inputPlaceholder;
        
        try {
            title = r18n.message("bedrock.set_home.title").toPlainString(player);
            inputLabel = r18n.message("bedrock.set_home.input_label").toPlainString(player);
            inputPlaceholder = r18n.message("bedrock.set_home.input_placeholder").toPlainString(player);
        } catch (Exception e) {
            title = "Create New Home";
            inputLabel = "Home Name";
            inputPlaceholder = "my_home";
        }
        
        return CustomForm.builder()
            .title(title)
            .input(inputLabel, inputPlaceholder, "home")
            .validResultHandler(response -> {
                var homeName = response.asInput();
                handleHomeCreation(player, plugin, homeName);
            })
            .closedOrInvalidResultHandler(response -> {
                // Form was closed or invalid - return to overview
                BedrockHomeOverviewForm.show(player, plugin);
            })
            .build();
    }

    private static void handleHomeCreation(
        @NotNull Player player,
        @NotNull JExHome plugin,
        String homeName
    ) {
        if (homeName == null || homeName.trim().isEmpty()) {
            sendValidationError(player, "empty");
            BedrockSetHomeForm.show(player, plugin);
            return;
        }

        var trimmedName = homeName.trim();

        if (!isValidHomeName(trimmedName)) {
            sendValidationError(player, "invalid_name");
            BedrockSetHomeForm.show(player, plugin);
            return;
        }

        createHome(player, plugin, trimmedName);
    }

    private static boolean isValidHomeName(String name) {
        return name != null && name.matches(HOME_NAME_PATTERN);
    }

    private static void createHome(
        @NotNull Player player,
        @NotNull JExHome plugin,
        @NotNull String homeName
    ) {
        try {
            var factory = HomeFactory.getInstance();
            factory.createHome(player, homeName)
                .thenAccept(home -> {
                    // Success - send message and return to overview
                    new de.jexcellence.jextranslate.i18n.I18n.Builder("sethome_anvil_ui.success", player)
                        .includePrefix()
                        .withPlaceholder("home_name", homeName)
                        .build()
                        .sendMessage();

                    // Return to home overview
                    BedrockHomeOverviewForm.show(player, plugin);
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, "Failed to create home for Bedrock player", throwable);
                    
                    // Send error message
                    new de.jexcellence.jextranslate.i18n.I18n.Builder("sethome_anvil_ui.error", player)
                        .includePrefix()
                        .withPlaceholder("home_name", homeName)
                        .build()
                        .sendMessage();

                    // Return to overview
                    BedrockHomeOverviewForm.show(player, plugin);
                    return null;
                });
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to create home via HomeFactory", e);
            sendErrorMessage(player);
            BedrockHomeOverviewForm.show(player, plugin);
        }
    }

    private static void sendValidationError(@NotNull Player player, @NotNull String errorType) {
        new de.jexcellence.jextranslate.i18n.I18n.Builder("sethome_anvil_ui.validation_error." + errorType, player)
            .includePrefix()
            .build()
            .sendMessage();
    }

    private static void sendForm(@NotNull Player player, @NotNull CustomForm form) {
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
