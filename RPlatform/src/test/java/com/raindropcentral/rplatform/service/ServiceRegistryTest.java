package com.raindropcentral.rplatform.service;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ServiceRegistryTest {

    private ServiceRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ServiceRegistry();
    }

    @AfterEach
    void tearDown() {
        registry = null;
    }

    @Test
    void registerAndRetrieveServiceFromBukkit() {
        ServicesManager servicesManager = mock(ServicesManager.class);
        RegisteredServiceProvider<TestService> provider = mock(RegisteredServiceProvider.class);
        TestService implementation = new TestServiceImpl("primary");

        when(servicesManager.getRegistration(TestService.class)).thenReturn(provider);
        when(provider.getProvider()).thenReturn(implementation);

        AtomicReference<TestService> successCallback = new AtomicReference<>();

        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
            mockedBukkit.when(Bukkit::getServicesManager).thenReturn(servicesManager);

            Optional<TestService> discovered = registry.register(TestService.class)
                    .required()
                    .onSuccess(successCallback::set)
                    .load()
                    .join();

            assertTrue(discovered.isPresent(), "Service should resolve from Bukkit");
            assertSame(implementation, discovered.orElseThrow());
            assertSame(implementation, successCallback.get(), "Success callback should receive service instance");
            assertTrue(registry.has(TestService.class));
            assertSame(implementation, registry.getRequired(TestService.class));
        }
    }

    @Test
    void duplicateRegistrationReplacesExistingService() {
        TestService first = new TestServiceImpl("first");
        TestService second = new TestServiceImpl("second");

        registry.registerService(TestService.class.getName(), first);
        assertSame(first, registry.getRequired(TestService.class));

        registry.registerService(TestService.class.getName(), second);
        assertSame(second, registry.getRequired(TestService.class), "Latest registration should replace earlier instance");
    }

    @Test
    void removalClearsServiceFromRegistry() throws Exception {
        TestService service = new TestServiceImpl("to-remove");
        registry.registerService(TestService.class.getName(), service);
        assertTrue(registry.has(TestService.class));

        Map<String, Object> services = servicesMap(registry);
        Object removed = services.remove(TestService.class.getName());

        assertSame(service, removed);
        assertFalse(registry.has(TestService.class));
        assertTrue(registry.get(TestService.class).isEmpty());
    }

    @Test
    void iterationReflectsCurrentRegistryState() throws Exception {
        registry.registerService(TestService.class.getName(), new TestServiceImpl("primary"));
        registry.registerService(SecondaryService.class.getName(), new SecondaryService("secondary"));
        registry.registerService(TestService.class.getName(), new TestServiceImpl("replacement"));

        Map<String, Object> services = servicesMap(registry);
        Set<String> keys = services.entrySet().stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        assertEquals(Set.of(TestService.class.getName(), SecondaryService.class.getName()), keys);
        assertEquals(2, keys.size(), "Duplicate registration should not introduce additional entries");
    }

    @Test
    void concurrentRegistrationsRemainThreadSafe() throws Exception {
        int registrations = 100;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < registrations; i++) {
            final int index = i;
            tasks.add(() -> {
                startLatch.await(2, TimeUnit.SECONDS);
                registry.registerService("service-" + index, new TestServiceImpl("service-" + index));
                return null;
            });
        }

        List<Future<Void>> futures = new ArrayList<>();
        try {
            for (Callable<Void> task : tasks) {
                futures.add(executor.submit(task));
            }
            startLatch.countDown();
            for (Future<Void> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        Map<String, Object> services = servicesMap(registry);
        assertEquals(registrations, services.size());
        assertTrue(services.keySet().stream().allMatch(key -> key.startsWith("service-")));
    }

    @Test
    void getRequiredThrowsWhenMissing() {
        assertThrows(ServiceRegistry.ServiceNotFoundException.class, () -> registry.getRequired(Plugin.class));
    }

    private static Map<String, Object> servicesMap(ServiceRegistry registry) throws Exception {
        Field field = ServiceRegistry.class.getDeclaredField("services");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> services = (Map<String, Object>) field.get(registry);
        return services;
    }

    private interface TestService {
        String name();
    }

    private static final class TestServiceImpl implements TestService {
        private final String name;

        private TestServiceImpl(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }
    }

    private static final class SecondaryService {
        private final String name;

        private SecondaryService(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
