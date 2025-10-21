package com.raindropcentral.rplatform.view.anvil;

import me.devnatan.inventoryframework.state.State;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class AnvilInputStateValueTest {

    @Test
    void usesDefaultInitialInputWhenConfigurationNotCustomized() {
        final State<?> state = Mockito.mock(State.class);
        final CustomAnvilInputConfig config = new CustomAnvilInputConfig();

        final AnvilInputStateValue value = new AnvilInputStateValue(state, config);

        assertEquals("", value.get(), "Default configuration should expose an empty initial input");
    }

    @Test
    void allowsNullInitialInput() {
        final State<?> state = Mockito.mock(State.class);
        final CustomAnvilInputConfig config = new CustomAnvilInputConfig().initialInput(null);

        final AnvilInputStateValue value = new AnvilInputStateValue(state, config);

        assertNull(value.get(), "Null initial input should be preserved");
    }

    @Test
    void setUpdatesMutableValueWhenHandlerMissing() {
        final State<?> state = Mockito.mock(State.class);
        final CustomAnvilInputConfig config = new CustomAnvilInputConfig().initialInput("start");
        final AnvilInputStateValue value = new AnvilInputStateValue(state, config);

        value.set("updated");

        assertEquals("updated", value.get(), "Raw assignments must flow directly to the underlying mutable value");
    }

    @Test
    void setAppliesChangeHandlerBeforePersisting() {
        final State<?> state = Mockito.mock(State.class);
        final UnaryOperator<String> handler = input -> {
            if (input == null || input.isBlank()) {
                return "fallback";
            }
            return input.trim().toUpperCase();
        };
        final CustomAnvilInputConfig config = new CustomAnvilInputConfig()
                .initialInput("   default   ")
                .onInputChange(handler);
        final AnvilInputStateValue value = new AnvilInputStateValue(state, config);

        value.set("  next  ");
        assertEquals("NEXT", value.get(), "Change handler should sanitize non-null input");

        value.set("   ");
        assertEquals("fallback", value.get(), "Change handler should replace blank input with fallback value");

        value.set(null);
        assertEquals("fallback", value.get(), "Change handler should process null input through validation path");
    }

    @Test
    void equalityReflectsUnderlyingStateAssociation() {
        final State<?> sharedState = Mockito.mock(State.class);
        final CustomAnvilInputConfig sharedConfig = new CustomAnvilInputConfig().initialInput("value");

        final AnvilInputStateValue first = new AnvilInputStateValue(sharedState, sharedConfig);
        final AnvilInputStateValue second = new AnvilInputStateValue(sharedState, sharedConfig);
        first.set("result");
        second.set("result");

        assertEquals(first, second, "Instances sharing the same state should compare equal once synchronized");
        assertEquals(first.hashCode(), second.hashCode(), "Equal instances must provide matching hash codes");

        final State<?> distinctState = Mockito.mock(State.class);
        final AnvilInputStateValue different = new AnvilInputStateValue(distinctState, new CustomAnvilInputConfig().initialInput("value"));
        different.set("result");

        assertNotEquals(first, different, "Distinct state references must break equality to honour parent contracts");
    }
}
