/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

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

/**
 * Registry of services discovered either through Bukkit's {@link org.bukkit.plugin.ServicesManager}.
 * or loaded reflectively from another plugin's class loader.
 *
 * <p>Instances are thread-safe courtesy of the underlying {@link ConcurrentHashMap} and the
 * asynchronous loading workflow employed by {@link ServiceRegistrationBuilder#load()}.
 * Logging occurs through {@link Logger} at {@code INFO} or {@code WARNING} levels to report
 * registration events and missing required services. Retry attempts performed during discovery
 * are configurable, defaulting to ten attempts spaced 500&nbsp;ms apart, and can be tuned via the
 * builder API.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class ServiceRegistry {

    /**
     * Logger used for reporting registration outcomes and retry failures.
     */
    private static final Logger LOGGER = Logger.getLogger(ServiceRegistry.class.getName());

    /**
     * Default number of attempts performed by {@link ServiceRegistrationBuilder#load()}.
     */
    private static final int DEFAULT_MAX_ATTEMPTS = 10;

    /**
     * Default delay, in milliseconds, between retry attempts during asynchronous discovery.
     */
    private static final long DEFAULT_RETRY_DELAY_MS = 500;

    /**
     * Thread-safe map of registered services keyed by their fully qualified class name.
     */
    private final Map<String, Object> services = new ConcurrentHashMap<>();

    /**
     * Begin constructing a registration for a service represented by the provided type.
     *
     * @param serviceClass service interface or implementation advertised through Bukkit
     * @param <T>          service type
     * @return builder capable of loading and caching the service instance
     */
    public <T> @NotNull ServiceRegistrationBuilder<T> register(final @NotNull Class<T> serviceClass) {
        return new ServiceRegistrationBuilder<>(this, serviceClass);
    }

    /**
     * Begin constructing a registration for a service using its fully qualified class name.
     *
     * @param serviceClass fully qualified service class name
     * @param <T> service type
     * @return builder capable of loading and caching the resolved service instance
     * @throws RuntimeException when the class cannot be loaded
     */
    public <T> @NotNull ServiceRegistrationBuilder<T> register(final @NotNull String serviceClass) {
        try {
            return new ServiceRegistrationBuilder<>(this, castServiceClass(Class.forName(serviceClass)));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Begin constructing a registration for a service using its fully qualified class name.
     * This variant supports loading implementations from a specific plugin's class loader.
     *
     * @param serviceFqcn fully qualified name of the service
     * @param pluginName  optional plugin providing the service implementation
     * @param <T>         service type
     * @return builder capable of loading and caching the service instance
     */
    public <T> @NotNull ServiceRegistrationBuilder<T> register(
            final @NotNull String serviceFqcn,
            final @Nullable String pluginName
    ) {
        return new ServiceRegistrationBuilder<>(this, serviceFqcn, pluginName);
    }

    /**
     * Retrieve a previously registered service, if present.
     *
     * @param serviceClass service type
     * @param <T>          service type
     * @return optional containing the service when known to the registry
     */
    public <T> @NotNull Optional<T> get(final @NotNull Class<T> serviceClass) {
        final Object service = services.get(serviceClass.getName());
        if (serviceClass.isInstance(service)) {
            return Optional.of(serviceClass.cast(service));
        }
        return Optional.empty();
    }

    /**
     * Retrieve a required service, throwing when unavailable.
     *
     * @param serviceClass service type
     * @param <T>          service type
     * @return service instance
     * @throws ServiceNotFoundException when the service has not been registered
     */
    public <T> @NotNull T getRequired(final @NotNull Class<T> serviceClass) {
        return get(serviceClass).orElseThrow(() ->
                new ServiceNotFoundException("Required service not available: " + serviceClass.getSimpleName())
        );
    }

    /**
     * Determine whether the registry already contains the provided service type.
     *
     * @param serviceClass service type
     * @return {@code true} when registered, {@code false} otherwise
     */
    public boolean has(final @NotNull Class<?> serviceClass) {
        return services.containsKey(serviceClass.getName());
    }

    /**
     * Binds a concrete service instance directly into the registry cache.
     *
     * <p>This is used for services created inside the current plugin runtime that should be
     * discoverable through the shared platform registry without going through Bukkit's global
     * service manager.</p>
     *
     * @param serviceClass service type used as the cache key
     * @param service service instance to expose
     * @param <T> service type
     * @return the bound service instance
     */
    public <T> @NotNull T bind(final @NotNull Class<T> serviceClass, final @NotNull T service) {
        registerService(serviceClass.getName(), service);
        return service;
    }

    /**
     * Store a resolved service within the registry cache.
     *
     * @param key     fully qualified class name used to identify the service
     * @param service resolved service implementation
     * @param <T>     service type
     */
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

    @SuppressWarnings("unchecked")
    private static <T> @NotNull Class<T> castServiceClass(final @NotNull Class<?> serviceClass) {
        return (Class<T>) serviceClass;
    }

    /**
     * Builder that orchestrates asynchronous service discovery with retry semantics and.
     * optional success or failure callbacks.
 *
 * <p>Instances are not thread-safe; share a builder per service registration sequence and
     * use the produced {@link CompletableFuture} for synchronization.
     *
     * @param <T> service type resolved by the builder
     *
     * @author JExcellence
     * @since 1.0.0
     * @version 1.0.1
     */
    public static final class ServiceRegistrationBuilder<T> {

        /**
         * Owning registry that caches resolved service instances.
         */
        private final ServiceRegistry registry;
        /**
         * Direct service type to request from Bukkit when known at compile time.
         */
        private final Class<T> serviceClass;
        /**
         * Fully qualified name used when loading the service reflectively.
         */
        private final String serviceFqcn;
        /**
         * Optional plugin expected to contribute the service implementation.
         */
        private final String pluginName;

        /**
         * Indicates whether the service is required, impacting failure logging and callback semantics.
         */
        private boolean required;
        /**
         * Maximum attempts performed before the builder gives up on discovery.
         */
        private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
        /**
         * Delay between retry attempts in milliseconds.
         */
        private long retryDelayMs = DEFAULT_RETRY_DELAY_MS;
        /**
         * Callback executed when discovery succeeds.
         */
        private Consumer<T> onSuccess;
        /**
         * Callback executed when discovery fails after exhausting retries.
         */
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

        /**
         * Mark the registration as required, emitting a warning when discovery ultimately fails.
         *
         * @return this builder for chaining
         */
        public @NotNull ServiceRegistrationBuilder<T> required() {
            this.required = true;
            return this;
        }

        /**
         * Mark the registration as optional, logging at {@code INFO} level when discovery fails.
         *
         * @return this builder for chaining
         */
        public @NotNull ServiceRegistrationBuilder<T> optional() {
            this.required = false;
            return this;
        }

        /**
         * Customize the number of retry attempts performed during discovery.
         *
         * @param attempts total attempts; values below one are coerced to one
         * @return this builder for chaining
         */
        public @NotNull ServiceRegistrationBuilder<T> maxAttempts(final int attempts) {
            this.maxAttempts = Math.max(1, attempts);
            return this;
        }

        /**
         * Customize the delay between retry attempts.
         *
         * @param delayMs delay in milliseconds; values below 100 are coerced to 100
         * @return this builder for chaining
         */
        public @NotNull ServiceRegistrationBuilder<T> retryDelay(final long delayMs) {
            this.retryDelayMs = Math.max(100, delayMs);
            return this;
        }

        /**
         * Provide a callback to execute when discovery succeeds.
         *
         * @param handler consumer invoked with the resolved service instance
         * @return this builder for chaining
         */
        public @NotNull ServiceRegistrationBuilder<T> onSuccess(final @NotNull Consumer<T> handler) {
            this.onSuccess = handler;
            return this;
        }

        /**
         * Provide a callback to execute when discovery fails after exhausting retries.
         *
         * @param handler runnable invoked when discovery fails
         * @return this builder for chaining
         */
        public @NotNull ServiceRegistrationBuilder<T> onFailure(final @NotNull Runnable handler) {
            this.onFailure = handler;
            return this;
        }

        /**
         * Attempt to discover the service asynchronously according to the configured retry policy.
         * When successful, the service is cached within the parent registry and any success callback
         * is invoked. Failures trigger the optional failure callback and emit logging as noted above.
         *
         * @return future supplying the discovered service when available; empty when discovery fails
         */
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

    /**
     * Exception thrown when a required service cannot be resolved.
     */
    public static final class ServiceNotFoundException extends RuntimeException {
        /**
         * Create a new exception describing the missing service.
         *
         * @param message human-readable description of the missing service
         */
        public ServiceNotFoundException(final @NotNull String message) {
            super(message);
        }
    }
}
