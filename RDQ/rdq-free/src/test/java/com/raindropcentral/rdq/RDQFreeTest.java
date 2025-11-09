package com.raindropcentral.rdq;

import be.seeseemelk.mockbukkit.MockBukkit;
import de.jexcellence.dependency.JEDependency;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RDQFreeTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void lifecycleDelegatesToImplementationWhenLoadSucceeds() throws Exception {
        try (MockedStatic<JEDependency> dependencyMock = Mockito.mockStatic(JEDependency.class);
             MockedConstruction<RDQFreeImpl> implConstruction = Mockito.mockConstruction(RDQFreeImpl.class, (mock, context) -> {
                 doNothing().when(mock).onLoad();
                 doNothing().when(mock).onEnable();
                 doNothing().when(mock).onDisable();
             })) {

            RDQFree plugin = MockBukkit.load(RDQFree.class);

            dependencyMock.verify(() -> JEDependency.initializeWithRemapping(plugin, RDQFree.class));

            List<RDQFreeImpl> constructed = implConstruction.constructed();
            assertEquals(1, constructed.size(), "Plugin should construct a single delegate instance");
            RDQFreeImpl delegate = constructed.getFirst();

            verify(delegate).onLoad();
            verify(delegate).onEnable();
            assertSame(delegate, plugin.getImpl(), "getImpl should expose the constructed delegate");

            reset(delegate);

            plugin.onEnable();
            plugin.onDisable();

            verify(delegate).onEnable();
            verify(delegate).onDisable();

            reset(delegate);
            clearDelegate(plugin);

            plugin.onEnable();
            plugin.onDisable();

            verifyNoInteractions(delegate);
        }
    }

    @Test
    void loadFailureClearsDelegateAndSkipsLifecycle() throws Exception {
        try (MockedStatic<JEDependency> dependencyMock = Mockito.mockStatic(JEDependency.class);
             MockedConstruction<RDQFreeImpl> implConstruction = Mockito.mockConstruction(RDQFreeImpl.class, (mock, context) ->
                     doThrow(new IllegalStateException("boom")).when(mock).onLoad())) {

            RDQFree plugin = MockBukkit.load(RDQFree.class);

            dependencyMock.verify(() -> JEDependency.initializeWithRemapping(plugin, RDQFree.class));

            List<RDQFreeImpl> constructed = implConstruction.constructed();
            assertEquals(1, constructed.size(), "Constructor should still run prior to failure");
            RDQFreeImpl delegate = constructed.getFirst();

            assertNull(plugin.getImpl(), "Delegate should be cleared when load fails");

            reset(delegate);

            plugin.onEnable();
            plugin.onDisable();

            verifyNoInteractions(delegate);
        }
    }

    private void clearDelegate(final RDQFree plugin) throws Exception {
        Field field = RDQFree.class.getDeclaredField("rdqImpl");
        field.setAccessible(true);
        field.set(plugin, null);
    }
}
