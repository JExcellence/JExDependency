package de.jexcellence.home.view;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.AbstractAnvilView;
import de.jexcellence.home.JExHome;
import de.jexcellence.home.factory.HomeFactory;
import de.jexcellence.home.utility.heads.House;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Anvil view for creating a new home.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class SetHomeAnvilView extends AbstractAnvilView {

    private final State<JExHome> jexHome = initialState("plugin");

    public SetHomeAnvilView() {
        super(HomeOverviewView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "sethome_anvil_ui";
    }

    @Override
    protected @NotNull Object processInput(
        final @NotNull String userInput,
        final @NotNull Context processingContext
    ) {
        var player = processingContext.getPlayer();
        var plugin = jexHome.get(processingContext);
        var homeName = userInput.trim();

        try {
            var factory = HomeFactory.getInstance();
            var home = factory.createHome(player, homeName).join();
            return home;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(
        final @NotNull OpenContext openContext
    ) {
        return Map.of();
    }

    @Override
    protected @NotNull String getInitialInputText(
        final @NotNull OpenContext openContext
    ) {
        // Get default home name from config
        try {
            var plugin = jexHome.get(openContext);
            if (plugin != null && plugin.getHomeConfig() != null) {
                String defaultName = plugin.getHomeConfig().getDefaultHomeName();
                if (defaultName != null && !defaultName.isEmpty()) {
                    return defaultName;
                }
            }
        } catch (Exception ignored) {
            // Fall back to default
        }
        return "home";
    }

    /**
     * Home name validation pattern.
     * - Alphanumeric characters (including Unicode letters like ä, ü, ö)
     * - Underscores and hyphens allowed
     * - No spaces or special characters
     * - Length between 1 and 32 characters
     */
    private static final String HOME_NAME_PATTERN = "^[\\p{L}\\p{N}_-]{1,32}$";

    @Override
    protected boolean isValidInput(
        final @NotNull String userInput,
        final @NotNull Context validationContext
    ) {
        if (userInput == null) {
            return false;
        }
        // Trim and validate: alphanumeric, underscores, and hyphens only, 1-32 chars
        var trimmed = userInput.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        return trimmed.matches(HOME_NAME_PATTERN);
    }

    @Override
    protected void setupFirstSlot(
        final @NotNull RenderContext renderContext,
        final @NotNull Player contextPlayer
    ) {
        // Get the initial input text from state - this becomes the editable text in the anvil
        String initialText = null;
        try {
            initialText = this.initialInput.get(renderContext);
        } catch (Exception ignored) {
            // State not set
        }
        
        if (initialText == null || initialText.isEmpty()) {
            initialText = "home";
        }
        
        // Create the house head with the initial text as the name
        var head = new House().getHead(contextPlayer);
        var meta = head.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(initialText);
            head.setItemMeta(meta);
        }
        
        renderContext.firstSlot(head);
    }

    @Override
    protected void onValidationFailed(
        final @Nullable String invalidInput,
        final @NotNull Context validationContext
    ) {
        String errorKey = this.getValidationErrorKey();

        if (invalidInput == null || invalidInput.trim().isEmpty()) {
            errorKey = errorKey + ".empty";
        } else {
            errorKey = errorKey + ".invalid_name";
        }

        this.i18n(errorKey, validationContext.getPlayer())
            .includePrefix()
            .withPlaceholder("input", invalidInput != null ? invalidInput : "")
            .build()
            .sendMessage();
    }

    @Override
    protected @NotNull Map<String, Object> prepareResultData(
        final @Nullable Object processingResult,
        final @NotNull String originalInput,
        final @NotNull Context resultContext
    ) {
        var player = resultContext.getPlayer();

        // Send success/error message
        if (processingResult != null) {
            this.i18n("success", player)
                .includePrefix()
                .withPlaceholder("home_name", originalInput.trim())
                .build()
                .sendMessage();
        } else {
            this.i18n("error", player)
                .includePrefix()
                .withPlaceholder("home_name", originalInput.trim())
                .build()
                .sendMessage();
        }

        var resultData = super.prepareResultData(processingResult, originalInput, resultContext);
        resultData.put("plugin", this.jexHome.get(resultContext));
        return resultData;
    }
}
