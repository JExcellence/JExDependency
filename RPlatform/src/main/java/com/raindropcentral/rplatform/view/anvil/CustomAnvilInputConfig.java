package com.raindropcentral.rplatform.view.anvil;

import org.jetbrains.annotations.Contract;

import java.util.function.UnaryOperator;

public class CustomAnvilInputConfig {
    
    String initialInput = "";
    boolean closeOnSelect;
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
