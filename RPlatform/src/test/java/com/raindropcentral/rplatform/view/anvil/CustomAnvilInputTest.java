package com.raindropcentral.rplatform.view.anvil;

import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.ViewType;
import me.devnatan.inventoryframework.context.IFContext;
import me.devnatan.inventoryframework.state.MutableValue;
import me.devnatan.inventoryframework.state.State;
import me.devnatan.inventoryframework.state.StateValueFactory;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class CustomAnvilInputTest {

    @Test
    void applyForcesAnvilViewType() {
        final StateValueFactory factory = Mockito.mock(StateValueFactory.class);
        final CustomAnvilInput input = new CustomAnvilInput(42L, factory);
        final ViewConfigBuilder config = Mockito.mock(ViewConfigBuilder.class, Mockito.RETURNS_SELF);
        final IFContext context = Mockito.mock(IFContext.class);

        input.apply(config, context);

        Mockito.verify(config).type(ViewType.ANVIL);
        Mockito.verifyNoMoreInteractions(config);
    }

    @Test
    void createAnvilInputAppliesInitialValueAndChangeHandler() throws Exception {
        final CustomAnvilInputConfig config = new CustomAnvilInputConfig()
            .initialInput("  Initial Value  ")
            .onInputChange(String::trim);

        final CustomAnvilInput input = CustomAnvilInput.createAnvilInput(config);
        final MutableValue value = instantiateStateValue(input);

        assertEquals("  Initial Value  ", value.get());

        value.set("   Updated Value   ");

        assertEquals("Updated Value", value.get());
    }

    @Test
    void stateUpdateEnforcesStringConstraints() throws Exception {
        final AtomicInteger invocationCount = new AtomicInteger();
        final UnaryOperator<String> validator = raw -> {
            invocationCount.incrementAndGet();
            if (raw == null || raw.isBlank()) {
                throw new IllegalArgumentException("Input must not be blank");
            }
            return raw.strip();
        };

        final CustomAnvilInputConfig config = new CustomAnvilInputConfig()
            .initialInput("Seed")
            .onInputChange(validator);

        final CustomAnvilInput input = CustomAnvilInput.createAnvilInput(config);
        final MutableValue value = instantiateStateValue(input);

        assertEquals("Seed", value.get());

        value.set("Next");
        assertEquals("Next", value.get());
        assertEquals(1, invocationCount.get());

        assertThrows(IllegalArgumentException.class, () -> value.set("   "));
        assertEquals(2, invocationCount.get());
    }

    private MutableValue instantiateStateValue(CustomAnvilInput input) throws Exception {
        final StateValueFactory factory = extractFactory(input);
        final State<?> state = Mockito.mock(State.class);
        return (MutableValue) factory.create(new Object(), state);
    }

    private StateValueFactory extractFactory(CustomAnvilInput input) throws Exception {
        final Field field = CustomAnvilInput.class.getSuperclass().getDeclaredField("valueFactory");
        field.setAccessible(true);
        return (StateValueFactory) field.get(input);
    }
}
