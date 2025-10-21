package com.raindropcentral.rplatform.view.anvil;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class CustomAnvilInputConfigTest {

    @Test
    void defaultsExposeExpectedValues() {
        final CustomAnvilInputConfig config = new CustomAnvilInputConfig();

        assertEquals("", config.getInitialInput());
        assertFalse(config.isCloseOnSelect());
        assertNull(config.getInputChangeHandler());
    }

    @Test
    void settersUpdateStateAndHandleNulls() {
        final CustomAnvilInputConfig config = new CustomAnvilInputConfig();

        config.initialInput("value");
        assertEquals("value", config.getInitialInput());

        config.initialInput(null);
        assertEquals("", config.getInitialInput());

        config.closeOnSelect();
        assertTrue(config.isCloseOnSelect());

        config.closeOnSelect(false);
        assertFalse(config.isCloseOnSelect());
    }

    @Test
    void optionalInputHandlerCanBeCleared() {
        final CustomAnvilInputConfig config = new CustomAnvilInputConfig();
        final UnaryOperator<String> handler = String::toUpperCase;

        config.onInputChange(handler);
        assertSame(handler, config.getInputChangeHandler());

        config.onInputChange(null);
        assertNull(config.getInputChangeHandler());
    }

    @Test
    void serializeAndDeserializeRoundTripSimpleFields() {
        final CustomAnvilInputConfig config = new CustomAnvilInputConfig()
            .initialInput("hello")
            .closeOnSelect(true);

        final Map<String, Object> serialized = config.serialize();
        assertEquals("hello", serialized.get("initialInput"));
        assertEquals(true, serialized.get("closeOnSelect"));
        assertFalse(serialized.containsKey("inputChangeHandler"));

        final CustomAnvilInputConfig deserialized = CustomAnvilInputConfig.deserialize(serialized);
        assertEquals("hello", deserialized.getInitialInput());
        assertTrue(deserialized.isCloseOnSelect());
        assertNull(deserialized.getInputChangeHandler());
    }

    @Test
    void deserializeHandlesStringValues() {
        final Map<String, Object> serialized = new HashMap<>();
        serialized.put("initialInput", "preset");
        serialized.put("closeOnSelect", "true");

        final CustomAnvilInputConfig config = CustomAnvilInputConfig.deserialize(serialized);
        assertEquals("preset", config.getInitialInput());
        assertTrue(config.isCloseOnSelect());
    }

    @Test
    void mergeRetainsDefaultsWhenOverridesMissing() {
        final UnaryOperator<String> handler = value -> value.trim();
        final CustomAnvilInputConfig defaults = new CustomAnvilInputConfig()
            .initialInput("base")
            .closeOnSelect(true)
            .onInputChange(handler);
        final CustomAnvilInputConfig overrides = new CustomAnvilInputConfig();

        final CustomAnvilInputConfig merged = defaults.merge(overrides);
        assertNotSame(defaults, merged);
        assertEquals("base", merged.getInitialInput());
        assertTrue(merged.isCloseOnSelect());
        assertSame(handler, merged.getInputChangeHandler());
    }

    @Test
    void mergeOverridesConfiguredValues() {
        final UnaryOperator<String> defaultsHandler = value -> value;
        final UnaryOperator<String> overridesHandler = value -> value + "!";

        final CustomAnvilInputConfig defaults = new CustomAnvilInputConfig()
            .initialInput("base")
            .closeOnSelect(false)
            .onInputChange(defaultsHandler);
        final CustomAnvilInputConfig overrides = new CustomAnvilInputConfig()
            .initialInput("override")
            .closeOnSelect(true)
            .onInputChange(overridesHandler);

        final CustomAnvilInputConfig merged = defaults.merge(overrides);
        assertEquals("override", merged.getInitialInput());
        assertTrue(merged.isCloseOnSelect());
        assertSame(overridesHandler, merged.getInputChangeHandler());
    }

    @Test
    void mergeClearsOptionalHandlerWhenExplicitlyNull() {
        final UnaryOperator<String> handler = value -> value;
        final CustomAnvilInputConfig defaults = new CustomAnvilInputConfig().onInputChange(handler);
        final CustomAnvilInputConfig overrides = new CustomAnvilInputConfig().onInputChange(null);

        final CustomAnvilInputConfig merged = defaults.merge(overrides);
        assertNull(merged.getInputChangeHandler());
    }

    @Test
    void defaultConfigProvidesFreshInstance() {
        final CustomAnvilInputConfig mutated = CustomAnvilInput.defaultConfig().initialInput("changed");
        assertEquals("changed", mutated.getInitialInput());

        final CustomAnvilInputConfig fresh = CustomAnvilInput.defaultConfig();
        assertEquals("", fresh.getInitialInput());
        assertFalse(fresh.isCloseOnSelect());
        assertNull(fresh.getInputChangeHandler());
    }
}
