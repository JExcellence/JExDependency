package com.raindropcentral.rdq;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import com.raindropcentral.rdq.service.bounty.BountyService;
import com.raindropcentral.rdq.service.bounty.BountyServiceProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.annotation.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class RDQPremiumImplTest {

    private ServerMock server;
    private TestRDQPremiumPlugin plugin;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.plugin = MockBukkit.load(TestRDQPremiumPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void onEnableDelegatesToRdqWhenAvailable() throws Exception {
        RDQPremiumImpl impl = new RDQPremiumImpl(this.plugin);
        RDQ rdqMock = mock(RDQ.class);

        setField(impl, "rdq", rdqMock);

        impl.onEnable();

        verify(rdqMock).onEnable();
        assertTrue(this.plugin.isEnabled(), "Plugin should remain enabled when RDQ exists");
    }

    @Test
    void onDisableDelegatesToRdqWhenAvailable() throws Exception {
        RDQPremiumImpl impl = new RDQPremiumImpl(this.plugin);
        RDQ rdqMock = mock(RDQ.class);

        setField(impl, "rdq", rdqMock);

        try (MockedStatic<Bukkit> bukkitStatic = mockStatic(Bukkit.class);
             MockedStatic<BountyServiceProvider> providerStatic = mockStatic(BountyServiceProvider.class)) {
            bukkitStatic.when(Bukkit::getServer).thenReturn(this.server);

            impl.onDisable();

            verify(rdqMock).onDisable();
            providerStatic.verify(BountyServiceProvider::reset);
        }
    }

    @Test
    void onEnableDisablesPluginWhenRdqMissing() {
        RDQPremiumImpl impl = new RDQPremiumImpl(this.plugin);

        assertTrue(this.plugin.isEnabled(), "MockBukkit should enable the plugin before invocation");

        impl.onEnable();

        assertTrue(this.server.getPluginManager().isPluginDisabled(this.plugin),
                "Plugin manager should disable the plugin when RDQ failed to load");
    }

    @Test
    void registerServicesRegistersBountyProviderWhenPresent() throws Exception {
        RDQPremiumImpl impl = new RDQPremiumImpl(this.plugin);
        BountyService bountyService = mock(BountyService.class);

        setField(impl, "bountyService", bountyService);

        try (MockedStatic<Bukkit> bukkitStatic = mockStatic(Bukkit.class);
             MockedStatic<BountyServiceProvider> providerStatic = mockStatic(BountyServiceProvider.class)) {
            bukkitStatic.when(Bukkit::getServer).thenReturn(this.server);

            invokeRegisterServices(impl);

            RegisteredServiceProvider<BountyService> registration =
                    this.server.getServicesManager().getRegistration(BountyService.class);
            assertNotNull(registration, "BountyService should be registered when a provider exists");
            assertSame(this.plugin, registration.getPlugin(), "Registration should belong to the plugin");
            assertSame(bountyService, registration.getProvider(),
                    "Registered provider should match the bounty service");
            assertEquals(ServicePriority.High, registration.getPriority(),
                    "Service should be registered with HIGH priority");
            providerStatic.verifyNoInteractions();
        }
    }

    @Test
    void registerServicesSkipsRegistrationWhenProviderMissing() throws Exception {
        RDQPremiumImpl impl = new RDQPremiumImpl(this.plugin);

        setField(impl, "bountyService", null);

        try (MockedStatic<Bukkit> bukkitStatic = mockStatic(Bukkit.class)) {
            bukkitStatic.when(Bukkit::getServer).thenReturn(this.server);

            invokeRegisterServices(impl);
        }

        assertNull(this.server.getServicesManager().getRegistration(BountyService.class),
                "No registration should be present when bountyService is null");
    }

    @Test
    void unregisterServicesRemovesRegistrationAndResetsProvider() throws Exception {
        RDQPremiumImpl impl = new RDQPremiumImpl(this.plugin);
        BountyService bountyService = mock(BountyService.class);

        setField(impl, "bountyService", bountyService);
        this.server.getServicesManager().register(BountyService.class, bountyService, this.plugin, ServicePriority.High);

        try (MockedStatic<Bukkit> bukkitStatic = mockStatic(Bukkit.class);
             MockedStatic<BountyServiceProvider> providerStatic = mockStatic(BountyServiceProvider.class)) {
            bukkitStatic.when(Bukkit::getServer).thenReturn(this.server);

            invokeUnregisterServices(impl);

            assertNull(this.server.getServicesManager().getRegistration(BountyService.class),
                    "Registration should be removed during unregister");
            providerStatic.verify(BountyServiceProvider::reset);
        }
    }

    private void setField(final RDQPremiumImpl impl, final String fieldName, final Object value) throws Exception {
        Field field = RDQPremiumImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(impl, value);
    }

    private void invokeRegisterServices(final RDQPremiumImpl impl) throws Exception {
        Method method = RDQPremiumImpl.class.getDeclaredMethod("registerServices");
        method.setAccessible(true);
        method.invoke(impl);
    }

    private void invokeUnregisterServices(final RDQPremiumImpl impl) throws Exception {
        Method method = RDQPremiumImpl.class.getDeclaredMethod("unregisterServices");
        method.setAccessible(true);
        method.invoke(impl);
    }

    @Plugin(name = "TestRDQPremium")
    public static class TestRDQPremiumPlugin extends RDQPremium {
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
