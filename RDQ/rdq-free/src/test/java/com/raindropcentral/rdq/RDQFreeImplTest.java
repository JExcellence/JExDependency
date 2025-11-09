package com.raindropcentral.rdq;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.raindropcentral.rdq.manager.RDQFreeManager;
import com.raindropcentral.rdq.manager.RDQManager;
import com.raindropcentral.rdq.service.bounty.BountyService;
import com.raindropcentral.rdq.service.bounty.BountyServiceProvider;
import com.raindropcentral.rdq.service.FreeBountyService;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.annotation.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class RDQFreeImplTest {

    private ServerMock server;
    private TestRDQFreePlugin plugin;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.plugin = MockBukkit.load(TestRDQFreePlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void onLoadRegistersServicesAndInitializesManager() throws Exception {
        AtomicBoolean managerInitialized = new AtomicBoolean(false);

        try (MockedStatic<BountyServiceProvider> providerStatic = mockStatic(BountyServiceProvider.class);
             MockedConstruction<FreeBountyService> bountyConstruction = mockConstruction(FreeBountyService.class);
             MockedConstruction<RDQFreeManager> managerConstruction = mockConstruction(RDQFreeManager.class, (mock, context) ->
                     doAnswer(invocation -> {
                         managerInitialized.set(true);
                         return null;
                     }).when(mock).initialize()
             )) {

            RDQFreeImpl impl = new RDQFreeImpl(this.plugin);

            impl.onLoad();

            RDQ rdq = impl.getRDQ();
            assertNotNull(rdq, "RDQ instance should be created during onLoad");

            RDQManager manager = invokeInitializeManager(rdq);
            assertNotNull(manager, "Manager should be returned when initialized");

            List<RDQFreeManager> constructedManagers = managerConstruction.constructed();
            assertEquals(1, constructedManagers.size(), "RDQFreeManager should be constructed once");
            RDQFreeManager constructedManager = constructedManagers.get(0);
            assertSame(constructedManager, manager, "Returned manager should match the constructed instance");
            verify(constructedManager).initialize();
            assertTrue(managerInitialized.get(), "Manager.initialize() should be invoked");

            List<FreeBountyService> constructedServices = bountyConstruction.constructed();
            assertEquals(1, constructedServices.size(), "FreeBountyService should be constructed once");
            FreeBountyService bountyService = constructedServices.get(0);

            providerStatic.verify(() -> BountyServiceProvider.setInstance(bountyService));

            RegisteredServiceProvider<BountyService> registration =
                    this.server.getServicesManager().getRegistration(BountyService.class);
            assertNotNull(registration, "BountyService should be registered with the ServicesManager");
            assertSame(this.plugin, registration.getPlugin(), "Registration should belong to the RDQ plugin");
            assertSame(bountyService, registration.getProvider(), "Registered provider should match the constructed service");
            assertEquals(ServicePriority.Normal, registration.getPriority(), "Priority should be set to NORMAL");

            impl.onDisable();

            assertNull(this.server.getServicesManager().getRegistration(BountyService.class),
                    "BountyService registration should be removed on disable");
            providerStatic.verify(BountyServiceProvider::reset);
        }
    }

    @Test
    void onEnableDelegatesToRdqWhenPresent() throws Exception {
        RDQFreeImpl impl = new RDQFreeImpl(this.plugin);
        RDQ rdqMock = mock(RDQ.class);

        setRdqField(impl, rdqMock);

        impl.onEnable();

        verify(rdqMock).onEnable();
        assertTrue(this.plugin.isEnabled(), "Plugin should remain enabled when RDQ instance exists");
    }

    @Test
    void onEnableDisablesPluginWhenRdqMissing() {
        RDQFreeImpl impl = new RDQFreeImpl(this.plugin);

        assertTrue(this.plugin.isEnabled(), "MockBukkit should report the plugin as enabled before the call");

        impl.onEnable();

        assertFalse(this.plugin.isEnabled(), "Plugin must be disabled when RDQ failed to load");
    }

    @Test
    @SuppressWarnings("unchecked")
    void onLoadFailureLogsAndPropagatesRuntimeException() throws Exception {
        Logger logger = Logger.getLogger(RDQFreeImpl.class.getName());
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(final LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        logger.addHandler(handler);

        Class<?> platformClass = Class.forName("com.raindropcentral.rplatform.RPlatform");

        try (MockedConstruction<?> platformConstruction = mockConstruction((Class) platformClass, (mock, context) -> {
            throw new IllegalStateException("platform failure");
        })) {
            RDQFreeImpl impl = new RDQFreeImpl(this.plugin);

            RuntimeException runtimeException = assertThrows(RuntimeException.class, impl::onLoad,
                    "onLoad should rethrow failures as RuntimeException");
            assertInstanceOf(IllegalStateException.class, runtimeException.getCause(),
                    "Underlying cause should be preserved");
        } finally {
            logger.removeHandler(handler);
        }

        LogRecord severeRecord = records.stream()
                .filter(record -> record.getLevel() == Level.SEVERE)
                .findFirst()
                .orElse(null);
        assertNotNull(severeRecord, "Failure should log a SEVERE record");
        assertTrue(severeRecord.getMessage().contains("Failed to load RDQ"),
                "Log message should indicate load failure");
    }

    private RDQManager invokeInitializeManager(final RDQ rdq) throws Exception {
        Method method = RDQ.class.getDeclaredMethod("initializeManager");
        method.setAccessible(true);
        return (RDQManager) method.invoke(rdq);
    }

    private void setRdqField(final RDQFreeImpl impl, final RDQ value) throws Exception {
        Field field = RDQFreeImpl.class.getDeclaredField("rdq");
        field.setAccessible(true);
        field.set(impl, value);
    }

    @Plugin(name = "TestRDQFree")
    public static class TestRDQFreePlugin extends RDQFree {
        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }
}
