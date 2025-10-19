package com.raindropcentral.rplatform.view.anvil;

import org.jetbrains.annotations.Contract;

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
 * @version 1.0.1
 */
public class CustomAnvilInputConfig {

    /**
     * Default string shown when the anvil view opens.
     */
    String initialInput = "";
    /**
     * Flag indicating whether the view should close immediately when the result slot is selected.
     */
    boolean closeOnSelect;
    /**
     * Optional unary operator used to normalize or validate input prior to persistence.
     */
    UnaryOperator<String> inputChangeHandler;

    CustomAnvilInputConfig() {}
    
    /**
     * Sets the initial anvil text input value.
     *
     * @param initialInput Initial anvil text input value.
     * @return This anvil input config.
     */
    @Contract("_ -> this")
    public CustomAnvilInputConfig initialInput(String initialInput) {
        this.initialInput = initialInput;
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
        this.closeOnSelect = !this.closeOnSelect;
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
        return this;
    }
}
