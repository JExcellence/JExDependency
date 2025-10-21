package com.raindropcentral.rplatform.view.anvil;

import org.jetbrains.annotations.Contract;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Configuration record for {@link CustomAnvilInput} describing initial text, lifecycle behaviour,
 * and transformation hooks for player input.
 *
 * <p>The config aligns with translation-driven workflows by allowing callers to inject default
 * values and on-change handlers prior to handing control back to Inventory Framework.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.2
 */
public class CustomAnvilInputConfig {

    /**
     * Default string shown when the anvil view opens.
     */
    private String initialInput = "";
    /**
     * Flag indicating whether the view should close immediately when the result slot is selected.
     */
    private boolean closeOnSelect;
    /**
     * Optional unary operator used to normalize or validate input prior to persistence.
     */
    private UnaryOperator<String> inputChangeHandler;

    private boolean initialInputConfigured;
    private boolean closeOnSelectConfigured;
    private boolean inputChangeHandlerConfigured;

    CustomAnvilInputConfig() {}

    /**
     * Returns the initial input configured for the anvil view.
     *
     * @return Initial anvil input value.
     */
    public String getInitialInput() {
        return initialInput;
    }

    /**
     * Indicates whether the view should close immediately after a result selection.
     *
     * @return {@code true} if the view should close on select, otherwise {@code false}.
     */
    public boolean isCloseOnSelect() {
        return closeOnSelect;
    }

    /**
     * Returns the optional input change handler.
     *
     * @return Configured input change handler or {@code null} when none has been set.
     */
    public UnaryOperator<String> getInputChangeHandler() {
        return inputChangeHandler;
    }

    /**
     * Sets the initial anvil text input value.
     *
     * @param initialInput Initial anvil text input value.
     * @return This anvil input config.
     */
    @Contract("_ -> this")
    public CustomAnvilInputConfig initialInput(String initialInput) {
        this.initialInput = initialInput == null ? "" : initialInput;
        this.initialInputConfigured = true;
        return this;
    }

    /**
     * Configures the view to close immediately when the player interacts with the item placed
     * at container's {@link me.devnatan.inventoryframework.ViewType#getResultSlots() first result slot}.
     *
     * @return This anvil input feature config.
     */
    @Contract("-> this")
    public CustomAnvilInputConfig closeOnSelect() {
        return closeOnSelect(true);
    }

    /**
     * Explicitly configures whether the view should close immediately when the result slot is used.
     *
     * @param closeOnSelect Flag indicating if the view should close.
     * @return This anvil input feature config.
     */
    @Contract("_ -> this")
    public CustomAnvilInputConfig closeOnSelect(boolean closeOnSelect) {
        this.closeOnSelect = closeOnSelect;
        this.closeOnSelectConfigured = true;
        return this;
    }

    /**
     * Setups a handler that can be used to transform the input provided by the player.
     * <p>
     * Note that it's not called immediately, only when view is closed or the player interacts with
     * the item placed at container's {@link me.devnatan.inventoryframework.ViewType#getResultSlots() first result slot}.
     *
     * @param inputChangeHandler The input change handler.
     * @return This anvil input feature config.
     */
    @Contract("_ -> this")
    public CustomAnvilInputConfig onInputChange(UnaryOperator<String> inputChangeHandler) {
        this.inputChangeHandler = inputChangeHandler;
        this.inputChangeHandlerConfigured = true;
        return this;
    }

    /**
     * Serializes this configuration into a map representation.
     *
     * @return Map representation of the configuration.
     */
    public Map<String, Object> serialize() {
        final Map<String, Object> serialized = new LinkedHashMap<>(3);
        serialized.put("initialInput", initialInput);
        serialized.put("closeOnSelect", closeOnSelect);
        return serialized;
    }

    /**
     * Creates a configuration instance from its serialized representation.
     *
     * @param serialized Serialized configuration map.
     * @return Deserialized configuration.
     */
    public static CustomAnvilInputConfig deserialize(Map<String, Object> serialized) {
        final CustomAnvilInputConfig config = new CustomAnvilInputConfig();
        if (serialized == null) {
            return config;
        }

        final Object initial = serialized.get("initialInput");
        if (initial instanceof String) {
            config.initialInput((String) initial);
        }

        final Object close = serialized.get("closeOnSelect");
        if (close instanceof Boolean) {
            config.closeOnSelect((Boolean) close);
        } else if (close instanceof String) {
            config.closeOnSelect(Boolean.parseBoolean((String) close));
        }

        return config;
    }

    /**
     * Creates a deep copy of this configuration.
     *
     * @return Copied configuration instance.
     */
    public CustomAnvilInputConfig copy() {
        final CustomAnvilInputConfig copy = new CustomAnvilInputConfig();
        copy.initialInput = this.initialInput;
        copy.closeOnSelect = this.closeOnSelect;
        copy.inputChangeHandler = this.inputChangeHandler;
        copy.initialInputConfigured = this.initialInputConfigured;
        copy.closeOnSelectConfigured = this.closeOnSelectConfigured;
        copy.inputChangeHandlerConfigured = this.inputChangeHandlerConfigured;
        return copy;
    }

    /**
     * Merges this configuration with the provided overrides.
     *
     * @param overrides Overrides to apply on top of this configuration.
     * @return Resulting merged configuration.
     */
    public CustomAnvilInputConfig merge(CustomAnvilInputConfig overrides) {
        final CustomAnvilInputConfig merged = copy();
        if (overrides == null) {
            return merged;
        }

        if (overrides.initialInputConfigured) {
            merged.initialInput = overrides.initialInput;
            merged.initialInputConfigured = true;
        }

        if (overrides.closeOnSelectConfigured) {
            merged.closeOnSelect = overrides.closeOnSelect;
            merged.closeOnSelectConfigured = true;
        }

        if (overrides.inputChangeHandlerConfigured) {
            merged.inputChangeHandler = overrides.inputChangeHandler;
            merged.inputChangeHandlerConfigured = true;
        }

        return merged;
    }
}
