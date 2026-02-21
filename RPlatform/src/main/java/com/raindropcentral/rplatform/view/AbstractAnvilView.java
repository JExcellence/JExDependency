package com.raindropcentral.rplatform.view;

import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.version.ServerEnvironment;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.AnvilInput;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.ViewType;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Template-method foundation for anvil-based workflows that render confirmation-style inputs using
 * Inventory Framework's {@link AnvilInput} support.
 *
 * <p>The view coordinates translation keys scoped to the supplied base key, interacts with
 * scheduler-aware head utilities for navigation, and transparently bridges Paper and Spigot title
 * rendering through {@link ServerEnvironment}.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public abstract class AbstractAnvilView extends View {

  /**
   * Shared anvil input modifier used to capture the text entry lifecycle.
   */
  private final AnvilInput anvilInput = AnvilInput.createAnvilInput();
  /**
   * Optional parent view reference that enables back navigation through Inventory Framework.
   */
  private final Class<? extends View> parentClass;
  /**
   * Base translation key cached from {@link #getKey()} for efficient message lookups.
   */
  private final String baseKey;

  /**
   * State container used to transport contextual data back to the parent view when the anvil closes.
   */
  protected final State<Object> data = initialState("data");
  /**
   * State backing the initial input text shown to the player when the view opens.
   */
  protected final MutableState<String> initialInput = mutableState("");

  /**
   * Cached title text used to avoid rebuilding translation output on sequential opens.
   */
  private String cachedTitle;
  
  /**
   * Creates an anvil view with optional parent navigation.
   *
   * @param parentClass the parent view class for back navigation, null to close inventory
   */
  protected AbstractAnvilView(
      @Nullable Class<? extends View> parentClass
  ) {
    this.parentClass = parentClass;
    this.baseKey = this.getKey();
  }
  
  /**
   * Creates an anvil view without parent navigation (will close on back).
   */
  protected AbstractAnvilView() {
    this(null);
  }
  
  
  /**
   * Obtains the translation namespace used for titles, prompts, and validation messages.
   *
   * @return the unique key for this view (for example {@code "item_rename"})
   */
  protected abstract String getKey();

  /**
   * Processes the user input when they click the result slot.
   *
   * @param input the user's input text
   * @param context the interaction context
   * @return the result to pass back to parent view, or null if processing failed
   */
  protected abstract Object processInput(
      final @NotNull String input,
      final @NotNull Context context
  );
  
  
  /**
   * Resolves the translation key used for the anvil title by appending {@code .title} to the base
   * key. Subclasses may override to point at bespoke translation buckets.
   *
   * @return the fully qualified translation key for the title component
   */
  protected String getTitleKey() {
    return this.baseKey + ".title";
  }

  /**
   * Supplies placeholder data used while building the translated title component.
   *
   * @param context the open context containing player and initial data
   * @return a map of placeholder keys and values for the title translation
   */
  protected Map<String, Object> getTitlePlaceholders(
      final @NotNull OpenContext context
  ) {
    return Map.of();
  }

  /**
   * Provides the initial input text shown in the anvil's result slot when the view opens.
   *
   * @param context the open context for the player
   * @return the initial text to display in the anvil input
   */
  protected String getInitialInputText(
      final @NotNull OpenContext context
  ) {
    return "";
  }

  /**
   * Validates user input before processing.
   *
   * @param input the user input
   * @param context the interaction context
   * @return true if valid, false otherwise
   */
  protected boolean isValidInput(
      final @NotNull String input,
      final @NotNull Context context
  ) {
    return !input.trim().isEmpty();
  }
  
  /**
   * @return Error message key for invalid input
   */
  protected String getValidationErrorKey() {
    return "error.invalid_input";
  }
  
  /**
   * Configures the first slot (input slot).
   * The item's display name becomes the initial editable text in the anvil.
   *
   * @param render the render context
   * @param player the player
   */
  protected void setupFirstSlot(
      final @NotNull RenderContext render,
      final @NotNull Player player
  ) {
    // Get the initial input text from state - this becomes the editable text in the anvil
    String initialText = null;
    try {
      initialText = this.initialInput.get(render);
    } catch (Exception ignored) {
      // State not set
    }
    
    if (initialText == null || initialText.isEmpty()) {
      initialText = " "; // Use space if empty to allow editing
    }
    
    ItemStack item = UnifiedBuilderFactory.item(Material.NAME_TAG)
        .setName(Component.text(initialText))
        .setLore(this.i18n("input.lore", player).build().children())
        .build();
    
    render.firstSlot(item);
  }
  
  /**
   * Configures the middle slot (optional decoration).
   *
   * @param render the render context
   * @param player the player
   */
  protected void setupMiddleSlot(
      final @NotNull RenderContext render,
      final @NotNull Player player
  ) {
  }
  
  /**
   * Called when input validation fails.
   *
   * @param input the invalid input
   * @param context the context
   */
  protected void onValidationFailed(
      final @Nullable String input,
      final @NotNull Context context
  ) {
    this.i18n(this.getValidationErrorKey(), context.getPlayer())
        .includePrefix()
        .withPlaceholder("input", input != null ? input : "")
        .build().sendMessage();
  }
  
  /**
   * Called when input processing fails.
   *
   * @param input the input that failed
   * @param context the context
   * @param exception the exception that occurred
   */
  protected void onProcessingFailed(
      final @NotNull String input,
      final @NotNull Context context,
      final @NotNull Exception exception
  ) {
    Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Failed to process anvil input: " + input, exception);
    
    this.i18n("error.processing_failed", context.getPlayer())
        .includePrefix()
        .withPlaceholder("input", input)
        .build().sendMessage();
  }
  
  /**
   * Prepares the result data to pass back to parent view.
   *
   * @param result the processing result
   * @param input the user input
   * @param context the context
   * @return map of data to pass to parent view
   */
  protected Map<String, Object> prepareResultData(
      final @Nullable Object result,
      final @NotNull String input,
      final @NotNull Context context
  ) {
    final Map<String, Object> resultData = new HashMap<>();
    
    if (
        result != null
    ) {
      if (
          result instanceof ItemStack
      ) {
        resultData.put("item", result);
      } else {
        resultData.put("result", result);
      }
    } else {
      resultData.put("input", input);
    }
    
    // Safely retrieve existing data - may be null if not provided during open
    try {
      Object existingData = this.data.get(context);
      if (
          existingData instanceof Map<?, ?>
      ) {
        resultData.putAll((Map<? extends String, ?>) existingData);
      }
    } catch (NullPointerException ignored) {
      // Data state was not initialized - this is fine, just skip merging
    }
    
    return resultData;
  }
  
  
  /**
   * Creates an i18n builder with the view's base key prefix.
   *
   * @param suffix the key suffix
   * @param player the player
   * @return i18n builder
   */
  protected I18n.Builder i18n(
      final @NotNull String suffix,
      final @NotNull Player player
  ) {
      return new I18n.Builder(this.baseKey + "." + suffix, player);
  }
  
  /**
   * Builds and caches the title to avoid redundant i18n calls.
   *
   * @param context the open context
   * @return the localized title
   */
  private Component buildTitle(
      final @NotNull OpenContext context
  ) {
    
    return this.i18n(this.getTitleKey(), context.getPlayer()).withPlaceholders(this.getTitlePlaceholders(context)).build().component();
  }
  
  
  /**
   * Configures the view type and wires the custom anvil input modifier ahead of rendering.
   *
   * @param config the view configuration builder supplied during initialization
   */
  @Override
  public void onInit(
      final @NotNull ViewConfigBuilder config
  ) {
    config.type(ViewType.ANVIL).use(this.anvilInput).title("");
  }

  /**
   * Applies localized titles using Adventure components on Paper and serialized strings on legacy
   * servers.
   *
   * @param open the open context describing the player and container configuration
   */
  @Override
  public void onOpen(
      final @NotNull OpenContext open
  ) {
    // Store the initial input text in state for use during render
    String initialText = this.getInitialInputText(open);
    if (initialText != null && !initialText.isEmpty()) {
      this.initialInput.set(initialText, open);
    }
    
    Component titleComponent = this.buildTitle(open);

    if (ServerEnvironment.getInstance().isPaper()) {
      open.modifyConfig().title(titleComponent);
    } else {
      String legacyTitle = LegacyComponentSerializer.legacySection().serialize(titleComponent);
      open.modifyConfig().title(legacyTitle);
    }
  }
  
  
  
  /**
   * Draws the base slot arrangement including initial, middle, and result slots before delegating
   * to user input handling.
   *
   * @param render the render context provided on first paint
   */
  @Override
  public void onFirstRender(
      final @NotNull RenderContext render
  ) {
    final Player player = render.getPlayer();
    
    this.setupFirstSlot(render, player);
    this.setupMiddleSlot(render, player);
    this.setupResultSlot(render);
  }
  
  /**
   * Sets up the result slot with click handling.
   *
   * @param render the render context
   */
  private void setupResultSlot(
      final @NotNull RenderContext render
  ) {
    render.resultSlot().onClick(clickContext -> {
      final String input = this.anvilInput.get(clickContext);
      
      if (
          ! this.isValidInput(input, render)
      ) {
        this.onValidationFailed(input, clickContext);
        return;
      }
      
      try {
        final Object result = this.processInput(input, clickContext);
        
        final Map<String, Object> resultData = this.prepareResultData(result, input, clickContext);
        clickContext.back(resultData);
        
      } catch (
          final Exception exception
      ) {
        this.onProcessingFailed(input, clickContext, exception);
      }
    });
  }
}