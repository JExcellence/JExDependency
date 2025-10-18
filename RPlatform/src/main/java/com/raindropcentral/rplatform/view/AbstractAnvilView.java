package com.raindropcentral.rplatform.view;

import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.version.ServerEnvironment;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.AnvilInput;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.ViewType;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
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

/**
 * Clean and flexible abstract base class for anvil-based views.
 *
 * <p>Provides streamlined anvil functionality with customizable slots,
 * automatic back navigation, and simplified i18n integration.</p>
 * 
 * <p>Includes fallback input reading for Spigot servers when NMS is not available.</p>
 */
public abstract class AbstractAnvilView extends View {
  
  private final AnvilInput anvilInput = AnvilInput.createAnvilInput();
  private final Class<? extends View> parentClass;
  private final String baseKey;
  
  protected final State<Object> data = initialState("data");
  protected final State<String> initialInput = initialState("initialInput");
  
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
   * @return The unique key for this view (e.g., "item_rename", "player_search")
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
   * @return The title key (defaults to "{viewKey}.title")
   */
  protected String getTitleKey() {
    return this.baseKey + ".title";
  }
  
  /**
   * @return Title placeholders for i18n (override for dynamic titles)
   */
  protected Map<String, Object> getTitlePlaceholders(
      final @NotNull OpenContext context
  ) {
    return Map.of();
  }
  
  /**
   * @return The initial input text (override for dynamic input)
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
   *
   * @param render the render context
   * @param player the player
   */
  protected void setupFirstSlot(
      final @NotNull RenderContext render,
      final @NotNull Player player
  ) {
    ItemStack item = UnifiedBuilderFactory.item(
            Material.NAME_TAG)
                                          .setName(this.i18n("input.name", player).build().component())
                                          .setLore(this.i18n("input.lore", player).build().splitLines())
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
    this.i18n(this.getValidationErrorKey(), context.getPlayer()).withPrefix().with("input", input != null ? input : "").send();
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
    CentralLogger.getLogger(this.getClass()).log(Level.WARNING, "Failed to process anvil input: " + input, exception);
    
    this.i18n("error.processing_failed", context.getPlayer()).withPrefix().with("input", input).send();
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
    
    Object existingData = this.data.get(context);
    if (
        existingData instanceof Map<?, ?>
    ) {
      resultData.putAll((Map<? extends String, ?>) existingData);
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
  protected TranslationService i18n(
      final @NotNull String suffix,
      final @NotNull Player player
  ) {
      return TranslationService.create(TranslationKey.of(this.baseKey, suffix), player);
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
    
    return this.i18n(this.getTitleKey(), context.getPlayer()).withAll(this.getTitlePlaceholders(context)).build().component();
  }
  
  
  @Override
  public void onInit(
      final @NotNull ViewConfigBuilder config
  ) {
    config.type(ViewType.ANVIL).use(this.anvilInput).title("");
  }

  @Override
  public void onOpen(
      final @NotNull OpenContext open
  ) {
    Component titleComponent = this.buildTitle(open);

    if (ServerEnvironment.getInstance().isPaper()) {
      open.modifyConfig().title(titleComponent);
    } else {
      String legacyTitle = LegacyComponentSerializer.legacySection().serialize(titleComponent);
      open.modifyConfig().title(legacyTitle);
    }
  }
  
  
  
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