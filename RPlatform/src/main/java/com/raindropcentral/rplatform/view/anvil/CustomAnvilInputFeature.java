package com.raindropcentral.rplatform.view.anvil;

import me.devnatan.inventoryframework.*;
import me.devnatan.inventoryframework.context.*;
import me.devnatan.inventoryframework.feature.Feature;
import me.devnatan.inventoryframework.pipeline.PipelineInterceptor;
import me.devnatan.inventoryframework.pipeline.StandardPipelinePhases;
import me.devnatan.inventoryframework.state.State;
import me.devnatan.inventoryframework.state.StateValue;
import me.devnatan.inventoryframework.state.StateValueHost;
import me.devnatan.inventoryframework.state.StateWatcher;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

import static com.raindropcentral.rplatform.view.anvil.CustomAnvilInput.defaultConfig;
import static java.util.Objects.requireNonNull;
import static me.devnatan.inventoryframework.IFViewFrame.FRAME_REGISTERED;

/**
 * Inventory Framework feature that injects {@link CustomAnvilInput} support with improved result.
 * synchronization and translation-aware input handling.
 *
 * <p>The feature coordinates reflection hooks to patch the upstream NMS integration, mirroring the
 * UI patterns used across {@code view} classes so title translations and head utilities remain
 * consistent.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.2
 */
@SuppressWarnings({"rawtypes", "unchecked", "unused"})
public final class CustomAnvilInputFeature implements Feature<CustomAnvilInputConfig, Void, ViewFrame> {

    private static final int INGREDIENT_SLOT = 0;

    /**
     * Instance of the Anvil Input feature.
     *
     * @see <a href="https://github.com/DevNatan/inventory-framework/wiki/anvil-input">Anvil Input on Wiki</a>
     */
    public static final Feature<CustomAnvilInputConfig, Void, ViewFrame> AnvilInput = new CustomAnvilInputFeature();
    
    /**
     * Configuration applied when the feature is installed, reused for each view intercept.
     */
    private CustomAnvilInputConfig config;
    /**
     * Interceptor registered on the framework pipeline for lifecycle callbacks.
     */
    private PipelineInterceptor frameInterceptor;

    private static final Method GET_REGISTERED_VIEWS_METHOD;
    
    static {
        try {
            GET_REGISTERED_VIEWS_METHOD = IFViewFrame.class.getDeclaredMethod("getRegisteredViews");
            GET_REGISTERED_VIEWS_METHOD.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Cannot access getRegisteredViews method", e);
        }
    }
    
    private CustomAnvilInputFeature() {}
    
    /**
     * Executes name.
     */
    @Override
    public @NotNull String name() {
        return "Anvil Input";
    }

    @Override
    /**
     * Installs the feature with the provided configuration and registers pipeline interceptors for.
     * open, close, and click phases.
     *
     * @param framework the view frame being configured
     * @param configure unary operator used to customize the feature configuration
     * @return {@code null} as this feature does not expose a runtime handle
     */
    public @NotNull Void install(ViewFrame framework, UnaryOperator<CustomAnvilInputConfig> configure) {
        final CustomAnvilInputConfig defaults = defaultConfig();
        final CustomAnvilInputConfig base = defaults.copy();
        final CustomAnvilInputConfig overrides = configure.apply(base);
        config = defaults.merge(overrides == null ? base : overrides);
        framework.getPipeline().intercept(FRAME_REGISTERED, (frameInterceptor = createFrameworkInterceptor()));
        return null;
    }

    @Override
    /**
     * Removes previously registered interceptors to avoid memory leaks when the feature is.
     * uninstalled.
     *
     * @param framework the view frame from which interceptors should be removed
     */
    public void uninstall(ViewFrame framework) {
        framework.getPipeline().removeInterceptor(FRAME_REGISTERED, frameInterceptor);
    }
    
    private PipelineInterceptor createFrameworkInterceptor() {
        return (PipelineInterceptor<IFViewFrame>) (pipeline, subject) -> {
	        final Map<UUID, PlatformView> views;
	        try {
		        views = new HashMap<>((Map<? extends UUID, ? extends PlatformView>) GET_REGISTERED_VIEWS_METHOD.invoke(subject));
	        } catch (IllegalAccessException |
	                 InvocationTargetException e) {
		        throw new RuntimeException(e);
	        }
	        
	        for (final PlatformView view : views.values()) {
                handleOpen(view);
                handleClose(view);
                handleClick(view);
            }
        };
    }
    
    private CustomAnvilInput getAnvilInput(IFContext context) {
        if (context.getConfig().getType() != ViewType.ANVIL) return null;
        
        final Optional<ViewConfig.Modifier> optional = context.getConfig().getModifiers().stream()
                                                              .filter(CustomAnvilInput.class::isInstance)
                                                              .findFirst();
        
        //noinspection OptionalIsPresent
        if (!optional.isPresent()) return null;
        
        return (CustomAnvilInput) optional.get();
    }
    
    private void updatePhysicalResult(String newText, ViewContainer container) {
        final Inventory inventory = ((BukkitViewContainer) container).getInventory();
        final ItemStack ingredientItem = requireNonNull(inventory.getItem(INGREDIENT_SLOT));
        final ItemMeta ingredientMeta = requireNonNull(ingredientItem.getItemMeta());
        ingredientMeta.setDisplayName(newText);
        ingredientItem.setItemMeta(ingredientMeta);
    }
    
    private void handleClick(PlatformView view) {
        view.getPipeline().intercept(StandardPipelinePhases.CLICK, (pipeline, subject) -> {
            if (!(subject instanceof IFSlotClickContext)) return;
            
            final SlotClickContext context = (SlotClickContext) subject;
            final CustomAnvilInput anvilInput = getAnvilInput(context);
            if (anvilInput == null) return;
            
            final int resultSlot = context.getContainer().getType().getResultSlots()[0];
            if (context.getClickedSlot() != resultSlot) return;
            
            final ItemStack resultItem = context.getItem();
            if (resultItem == null || resultItem.getType() == Material.AIR) return;
            
            final ItemMeta resultMeta = requireNonNull(resultItem.getItemMeta());
            final String text = resultMeta.getDisplayName();
            final Inventory clickedInventory =
                requireNonNull(context.getClickOrigin().getClickedInventory(), "Clicked inventory cannot be null");
            final ItemStack ingredientItem = requireNonNull(clickedInventory.getItem(INGREDIENT_SLOT));
            final ItemMeta ingredientMeta = requireNonNull(ingredientItem.getItemMeta());
            ingredientMeta.setDisplayName(text);
            context.updateState(anvilInput, text);
            ingredientItem.setItemMeta(ingredientMeta);
            
            if (config.isCloseOnSelect()) {
                context.closeForPlayer();
            }
        });
    }
    
    private void handleOpen(PlatformView view) {
        view.getPipeline().intercept(StandardPipelinePhases.OPEN, (pipeline, subject) -> {
            if (!(subject instanceof IFOpenContext)) return;
            
            final OpenContext context = (OpenContext) subject;
            final CustomAnvilInput anvilInput = getAnvilInput(context);
            if (anvilInput == null) return;
            
            // Forces internal state initialization
            context.getInternalStateValue(anvilInput);
            context.watchState(anvilInput.internalId(), new StateWatcher() {
                /**
                 * Executes stateRegistered.
                 */
                @Override
                public void stateRegistered(@NotNull State<?> state, Object caller) {}
                
                /**
                 * Executes stateUnregistered.
                 */
                @Override
                public void stateUnregistered(@NotNull State<?> state, Object caller) {}
                
                /**
                 * Executes stateValueGet.
                 */
                @Override
                public void stateValueGet(
                    @NotNull State<?> state,
                    @NotNull StateValueHost host,
                    @NotNull StateValue internalValue,
                    Object rawValue) {}
                
                /**
                 * Executes stateValueSet.
                 */
                @Override
                public void stateValueSet(
                    @NotNull StateValueHost host,
                    @NotNull StateValue value,
                    Object rawOldValue,
                    Object rawNewValue) {
                    updatePhysicalResult((String) rawNewValue, ((IFRenderContext) host).getContainer());
                }
            });
            
            final String globalInitialInput = config.getInitialInput();
            final String scopedInitialInput = anvilInput.get(context);
            
            final Inventory inventory = CustomAnvilInputNMS.open(
                context.getPlayer(),
                context.getConfig().getTitle(),
                scopedInitialInput.isEmpty() ? globalInitialInput : scopedInitialInput);
            final ViewContainer container =
                new BukkitViewContainer(inventory, context.isShared(), ViewType.ANVIL, true);
            
            context.setContainer(container);
        });
    }
    
    private void handleClose(PlatformView view) {
        view.getPipeline().intercept(StandardPipelinePhases.CLOSE, (pipeline, subject) -> {
            if (!(subject instanceof IFCloseContext)) return;
            
            final CloseContext context = (CloseContext) subject;
            final CustomAnvilInput anvilInput = getAnvilInput(context);
            if (anvilInput == null) return;
            
            final BukkitViewContainer container = (BukkitViewContainer) context.getContainer();
            final int slot = container.getType().getResultSlots()[0];
            final ItemStack item = container.getInventory().getItem(slot);
            
            if (item == null || item.getType() == Material.AIR) return;
            
            final String input = requireNonNull(item.getItemMeta()).getDisplayName();
            context.updateState(anvilInput, input);
        });
    }
}
