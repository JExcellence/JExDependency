package com.raindropcentral.rplatform.service;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class ServiceRegistry {

    private static final Logger LOGGER = Logger.getLogger(ServiceRegistry.class.getName());
    private static final int DEFAULT_MAX_ATTEMPTS = 10;
    private static final long DEFAULT_RETRY_DELAY_MS = 500;

    private final Map<String, Object> services = new ConcurrentHashMap<>();

    public <T> @NotNull ServiceRegistrationBuilder<T> register(final @NotNull Class<T> serviceClass) {
        return new ServiceRegistrationBuilder<>(this, serviceClass);
    }

    public <T> @NotNull ServiceRegistrationBuilder<T> register(
            final @NotNull String serviceFqcn,
            final @Nullable String pluginName
    ) {
        return new ServiceRegistrationBuilder<>(this, serviceFqcn, pluginName);
    }

    public <T> @NotNull Optional<T> get(final @NotNull Class<T> serviceClass) {
        final Object service = services.get(serviceClass.getName());
        if (service != null && serviceClass.isInstance(service)) {
            return Optional.of(serviceClass.cast(service));
        }
        return Optional.empty();
    }

    public <T> @NotNull T getRequired(final @NotNull Class<T> serviceClass) {
        return get(serviceClass).orElseThrow(() ->
                new ServiceNotFoundException("Required service not available: " + serviceClass.getSimpleName())
        );
    }

    public boolean has(final @NotNull Class<?> serviceClass) {
        return services.containsKey(serviceClass.getName());
    }

    <T> void registerService(final @NotNull String key, final @NotNull T service) {
        services.put(key, service);
        LOGGER.info("Registered service: " + key);
    }

    @Nullable
    <T> T loadFromBukkit(final @NotNull Class<T> serviceClass) {
        final RegisteredServiceProvider<T> provider = Bukkit.getServicesManager().getRegistration(serviceClass);
        return provider != null ? provider.getProvider() : null;
    }

    @Nullable
    Class<?> loadClass(final @NotNull String fqcn, final @Nullable String pluginName) {
        if (pluginName != null && !pluginName.isBlank()) {
            final Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
            if (plugin != null) {
                try {
                    return Class.forName(fqcn, false, plugin.getClass().getClassLoader());
                } catch (final ClassNotFoundException e) {
                    LOGGER.fine("Class " + fqcn + " not found in plugin " + pluginName);
                }
            }
        }

        try {
            return Class.forName(fqcn);
        } catch (final ClassNotFoundException e) {
            return null;
        }
    }

    public static final class ServiceRegistrationBuilder<T> {

        private final ServiceRegistry registry;
        private final Class<T> serviceClass;
        private final String serviceFqcn;
        private final String pluginName;

        private boolean required;
        private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
        private long retryDelayMs = DEFAULT_RETRY_DELAY_MS;
        private Consumer<T> onSuccess;
        private Runnable onFailure;

        private ServiceRegistrationBuilder(
                final @NotNull ServiceRegistry registry,
                final @NotNull Class<T> serviceClass
        ) {
            this.registry = registry;
            this.serviceClass = serviceClass;
            this.serviceFqcn = serviceClass.getName();
            this.pluginName = null;
        }

        private ServiceRegistrationBuilder(
                final @NotNull ServiceRegistry registry,
                final @NotNull String serviceFqcn,
                final @Nullable String pluginName
        ) {
            this.registry = registry;
            this.serviceClass = null;
            this.serviceFqcn = serviceFqcn;
            this.pluginName = pluginName;
        }

        public @NotNull ServiceRegistrationBuilder<T> required() {
            this.required = true;
            return this;
        }

        public @NotNull ServiceRegistrationBuilder<T> optional() {
            this.required = false;
            return this;
        }

        public @NotNull ServiceRegistrationBuilder<T> maxAttempts(final int attempts) {
            this.maxAttempts = Math.max(1, attempts);
            return this;
        }

        public @NotNull ServiceRegistrationBuilder<T> retryDelay(final long delayMs) {
            this.retryDelayMs = Math.max(100, delayMs);
            return this;
        }

        public @NotNull ServiceRegistrationBuilder<T> onSuccess(final @NotNull Consumer<T> handler) {
            this.onSuccess = handler;
            return this;
        }

        public @NotNull ServiceRegistrationBuilder<T> onFailure(final @NotNull Runnable handler) {
            this.onFailure = handler;
            return this;
        }

        public @NotNull CompletableFuture<Optional<T>> load() {
            return CompletableFuture.supplyAsync(() -> {
                for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                    final Optional<T> result = tryLoad();
                    
                    if (result.isPresent()) {
                        final T service = result.get();
                        registry.registerService(serviceFqcn, service);
                        
                        if (onSuccess != null) {
                            onSuccess.accept(service);
                        }
                        
                        return result;
                    }

                    if (attempt < maxAttempts) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(retryDelayMs);
                        } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }

                if (required) {
                    LOGGER.warning("Required service not available after " + maxAttempts + " attempts: " + serviceFqcn);
                } else {
                    LOGGER.info("Optional service not available: " + serviceFqcn);
                }

                if (onFailure != null) {
                    onFailure.run();
                }

                return Optional.empty();
            });
        }

        @SuppressWarnings("unchecked")
        private @NotNull Optional<T> tryLoad() {
            if (serviceClass != null) {
                final T service = registry.loadFromBukkit(serviceClass);
                if (service != null) {
                    return Optional.of(service);
                }
            } else {
                final Class<?> loadedClass = registry.loadClass(serviceFqcn, pluginName);
                if (loadedClass != null) {
                    final Object service = registry.loadFromBukkit(loadedClass);
                    if (service != null) {
                        return Optional.of((T) service);
                    }
                }
            }
            return Optional.empty();
        }
    }

    public static final class ServiceNotFoundException extends RuntimeException {
        public ServiceNotFoundException(final @NotNull String message) {
            super(message);
        }
    }
}
