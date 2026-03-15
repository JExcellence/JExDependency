package com.raindropcentral.rplatform.view.anvil;

import me.devnatan.inventoryframework.ViewConfig;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.ViewType;
import me.devnatan.inventoryframework.context.IFContext;
import me.devnatan.inventoryframework.state.BaseMutableState;
import me.devnatan.inventoryframework.state.State;
import me.devnatan.inventoryframework.state.StateValueFactory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

/**
 * Inventory Framework state wrapper that exposes a translation-friendly anvil input modifier while.
 * retaining control over lifecycle behaviour.
 *
 * <p>The implementation mirrors the UI conventions established in {@code view} classes, allowing
 * localized prompts and head utilities to co-exist with custom NMS handling.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.2
 */
public final class CustomAnvilInput extends BaseMutableState<String> implements ViewConfig.Modifier {

    /**
     * Shared configuration instance used when callers rely on default behaviour.
     */
    private static final CustomAnvilInputConfig DEFAULT_CONFIG = new CustomAnvilInputConfig();

    CustomAnvilInput(long id, StateValueFactory valueFactory) {
        super(id, valueFactory);
    }
    
    /**
     * Forces the view type to {@link ViewType#ANVIL} ensuring the modifier pairs with anvil.
     * containers.
     *
     * @param config the view configuration builder being modified
     * @param context the context associated with the configuration phase
     */
    @Override
    public void apply(@NotNull ViewConfigBuilder config, @NotNull IFContext context) {
        config.type(ViewType.ANVIL);
    }
    
    /**
     * Returns the default configuration of the anvil input feature.
     *
     * @return Default configuration of the anvil input feature.
     */
    public static CustomAnvilInputConfig defaultConfig() {
        return DEFAULT_CONFIG.copy();
    }
    
    /**
     * Creates a new AnvilInput instance.
 *
 * <p><b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @see <a href="https://github.com/DevNatan/inventory-framework/wiki/anvil-input">Anvil Input on Wiki</a>
     */
    @ApiStatus.Experimental
    public static CustomAnvilInput createAnvilInput() {
        return createAnvilInput("");
    }
    
    /**
     * Creates a new AnvilInput instance with an initial input.
 *
 * <p><b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @param initialInput Initial text input value.
     * @see <a href="https://github.com/DevNatan/inventory-framework/wiki/anvil-input">Anvil Input on Wiki</a>
     */
    @ApiStatus.Experimental
    public static CustomAnvilInput createAnvilInput(@NotNull String initialInput) {
        return createAnvilInput(initialInput, UnaryOperator.identity());
    }
    
    /**
     * Creates a new AnvilInput instance with an input change handler.
 *
 * <p><code>onInputChange</code> parameter can be used to transform the input provided by the player.
     * Note that it's not called immediately, only when view is closed or the player interacts with
     * the item placed at container's {@link ViewType#getResultSlots() first result slot}.
 *
 * <p><b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @param onInputChange Input change handler, current input will be set to the result of it.
     * @see <a href="https://github.com/DevNatan/inventory-framework/wiki/anvil-input">Anvil Input on Wiki</a>
     */
    @ApiStatus.Experimental
    public static CustomAnvilInput createAnvilInput(@NotNull UnaryOperator<String> onInputChange) {
        return createAnvilInput("", onInputChange);
    }
    
    /**
     * Creates a new AnvilInput instance with an initial input and an input change handler.
 *
 * <p><code>onInputChange</code> parameter can be used to transform the input provided by the player.
     * Note that it's not called immediately, only when view is closed or the player interacts with
     * the item placed at container's {@link ViewType#getResultSlots() first result slot}.
 *
 * <p><b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @param initialInput Initial text input value.
     * @param onInputChange Input change handler, current input will be set to the result of it.
     * @see <a href="https://github.com/DevNatan/inventory-framework/wiki/anvil-input">Anvil Input on Wiki</a>
     */
    @ApiStatus.Experimental
    public static CustomAnvilInput createAnvilInput(
        @NotNull String initialInput, @NotNull UnaryOperator<String> onInputChange) {
        return createAnvilInput(defaultConfig().initialInput(initialInput).onInputChange(onInputChange));
    }
    
    /**
     * Creates a new AnvilInput instance.
 *
 * <p><b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @param config Anvil input feature configuration.
     * @see <a href="https://github.com/DevNatan/inventory-framework/wiki/anvil-input">Anvil Input on Wiki</a>
     */
    @ApiStatus.Experimental
    public static CustomAnvilInput createAnvilInput(@NotNull CustomAnvilInputConfig config) {
        final long id = State.next();
        final StateValueFactory factory = (host, state) -> new AnvilInputStateValue(state, config);
        
        return new CustomAnvilInput(id, factory);
    }
}
