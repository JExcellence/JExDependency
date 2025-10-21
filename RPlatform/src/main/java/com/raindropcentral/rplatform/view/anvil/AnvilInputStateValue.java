package com.raindropcentral.rplatform.view.anvil;

import me.devnatan.inventoryframework.state.MutableValue;
import me.devnatan.inventoryframework.state.State;

import java.util.function.UnaryOperator;

/**
 * Mutable state wrapper that applies optional transformation logic to anvil input updates.
 *
 * <p>The state hooks into {@link CustomAnvilInputConfig#onInputChange(UnaryOperator)} so callers can
 * normalize user input prior to downstream consumption.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.2
 */
class AnvilInputStateValue extends MutableValue {

    /**
     * Configuration describing initial input and change handlers for the anvil state.
     */
    private final CustomAnvilInputConfig config;

    /**
     * Creates a new mutable state value bound to the supplied configuration.
     *
     * @param state the backing state managed by Inventory Framework
     * @param config the configuration guiding default input and transformations
     */
    public AnvilInputStateValue(State<?> state, CustomAnvilInputConfig config) {
        super(state, config.getInitialInput());
        this.config = config;
    }

    /**
     * Applies the configured input change handler (if any) before delegating to the base setter.
     *
     * @param value the new raw value provided by the framework
     */
    @Override
    public void set(Object value) {
        final Object newValue;
        if (config.getInputChangeHandler() == null) {
            newValue = value;
        } else {
            newValue = config.getInputChangeHandler().apply((String) value);
        }

        super.set(newValue);
    }
}
