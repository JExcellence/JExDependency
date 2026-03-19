package de.jexcellence.home.view;

import com.raindropcentral.rplatform.integration.geyser.GeyserService;
import de.jexcellence.home.JExHome;
import de.jexcellence.home.config.HomeSystemConfig;
import de.jexcellence.home.database.entity.Home;
import de.jexcellence.home.factory.HomeFactory;
import de.jexcellence.home.view.bedrock.BedrockDeleteHomeForm;
import de.jexcellence.home.view.bedrock.BedrockHomeOverviewForm;
import de.jexcellence.home.view.bedrock.BedrockSetHomeForm;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Routes players to the appropriate view based on their client type.
 * <p>
 * This class determines whether a player is using Bedrock Edition (via Floodgate)
 * and routes them to the appropriate UI - either native Bedrock forms or
 * traditional chest-based GUIs for Java Edition players.
 * </p>
 * <p>
 * The routing behavior can be configured via {@link HomeSystemConfig}:
 * <ul>
 *   <li>{@code bedrock.enabled} - Enable/disable Bedrock form support</li>
 *   <li>{@code bedrock.forceChestGui} - Force all players to use chest GUI</li>
 * </ul>
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class ViewRouter {

    private final JExHome plugin;
    private final ViewFrame viewFrame;

    /**
     * Creates a new ViewRouter instance.
     *
     * @param plugin    the JExHome plugin instance
     * @param viewFrame the ViewFrame for Java Edition GUIs
     */
    public ViewRouter(@NotNull JExHome plugin, @NotNull ViewFrame viewFrame) {
        this.plugin = plugin;
        this.viewFrame = viewFrame;
    }

    /**
     * Determines if a player should use Bedrock forms.
     * <p>
     * Returns true only if:
     * <ul>
     *   <li>Bedrock support is enabled in configuration</li>
     *   <li>Force chest GUI is not enabled</li>
     *   <li>The player is detected as a Bedrock player</li>
     * </ul>
     * </p>
     *
     * @param player the player to check
     * @return true if the player should use Bedrock forms
     */
    private boolean shouldUseBedrockForms(@NotNull Player player) {
        HomeSystemConfig config = plugin.getHomeConfig();
        
        // Check if Bedrock support is disabled
        if (!config.isBedrockEnabled()) {
            return false;
        }
        
        // Check if chest GUI is forced for all players
        if (config.isForceChestGui()) {
            return false;
        }
        
        // Check if player is actually a Bedrock player using GeyserService
        GeyserService geyserService = plugin.getPlatform().getGeyserService();
        if (geyserService == null) {
            return false;
        }
        return geyserService.isBedrockPlayer(player);
    }

    /**
     * Opens the home overview for a player.
     * <p>
     * Bedrock players will see a SimpleForm with home buttons,
     * while Java players will see the traditional chest GUI.
     * </p>
     *
     * @param player the player to show the overview to
     */
    public void openHomeOverview(@NotNull Player player) {
        if (shouldUseBedrockForms(player)) {
            BedrockHomeOverviewForm.show(player, plugin);
        } else {
            viewFrame.open(HomeOverviewView.class, player, Map.of("plugin", plugin));
        }
    }

    /**
     * Opens the set home interface for a player.
     * <p>
     * Bedrock players will see a CustomForm with text input,
     * while Java players will see the anvil-based input GUI.
     * </p>
     *
     * @param player the player to show the set home interface to
     */
    public void openSetHome(@NotNull Player player) {
        if (shouldUseBedrockForms(player)) {
            BedrockSetHomeForm.show(player, plugin);
        } else {
            viewFrame.open(SetHomeAnvilView.class, player, Map.of("plugin", plugin));
        }
    }

    /**
     * Opens the delete home confirmation for a player.
     * <p>
     * Bedrock players will see a ModalForm with confirm/cancel buttons,
     * while Java players will see a confirmation dialog and then delete the home.
     * </p>
     *
     * @param player the player to show the confirmation to
     * @param home   the home to be deleted
     */
    public void openDeleteConfirmation(@NotNull Player player, @NotNull Home home) {
        if (shouldUseBedrockForms(player)) {
            BedrockDeleteHomeForm.show(player, plugin, home);
        } else {
            // For Java players, directly delete with confirmation message
            // since we don't have a context to open ConfirmationView from command
            try {
                var factory = HomeFactory.getInstance();
                factory.deleteHome(player, home.getHomeName())
                    .thenAccept(deleted -> {
                        if (deleted) {
                            new I18n.Builder("delhome.deleted", player)
                                .withPlaceholder("home_name", home.getHomeName())
                                .includePrefix()
                                .build()
                                .sendMessage();
                        } else {
                            new I18n.Builder("delhome.does_not_exist", player)
                                .withPlaceholder("home_name", home.getHomeName())
                                .includePrefix()
                                .build()
                                .sendMessage();
                        }
                    });
            } catch (Exception e) {
                new I18n.Builder("home.error.internal", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            }
        }
    }

    /**
     * Opens the delete home selection view for a player.
     * <p>
     * Shows a list of homes that the player can click to delete.
     * </p>
     *
     * @param player the player to show the delete selection to
     */
    public void openDeleteHomeSelection(@NotNull Player player) {
        if (shouldUseBedrockForms(player)) {
            // For Bedrock, show the home overview (they can delete from there)
            BedrockHomeOverviewForm.show(player, plugin);
        } else {
            viewFrame.open(DeleteHomeView.class, player, Map.of("plugin", plugin));
        }
    }

    /**
     * Gets the JExHome plugin instance.
     *
     * @return the plugin instance
     */
    @NotNull
    public JExHome getPlugin() {
        return plugin;
    }

    /**
     * Gets the ViewFrame for Java Edition GUIs.
     *
     * @return the ViewFrame instance
     */
    @NotNull
    public ViewFrame getViewFrame() {
        return viewFrame;
    }
}
