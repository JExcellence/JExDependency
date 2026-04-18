package de.jexcellence.jexplatform.service;

import de.jexcellence.jexplatform.logging.JExLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Thread-safe service registry for binding and discovering plugin services.
 *
 * <p>Direct binding for own services, async discovery with retry for external
 * services (Vault, PlaceholderAPI, etc.):
 *
 * <pre>{@code
 * services.bind(CurrencyAdapter.class, adapter);
 * var adapter = services.require(CurrencyAdapter.class);
 * var opt = services.get(CurrencyAdapter.class);
 *
 * services.discover(Economy.class)
 *     .maxAttempts(5)
 *     .retryDelay(500)
 *     .onSuccess(e -> log.info("Found Vault"))
 *     .load();
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class ServiceRegistry {

    private static final JExLogger LOG = JExLogger.of("Services");
    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();
    private final JavaPlugin plugin;

    /**
     * Creates a service registry bound to the given plugin.
     *
     * @param plugin owning plugin for Bukkit service discovery
     */
    public ServiceRegistry(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Direct binding ──────────────────────────────────────────────────────────

    /**
     * Binds a service instance to its type.
     *
     * @param type     the service interface or class
     * @param instance the implementation to bind
     * @param <T>      service type
     */
    public <T> void bind(@NotNull Class<T> type, @NotNull T instance) {
        services.put(type, instance);
        LOG.debug("Bound service: {}", type.getSimpleName());
    }

    /**
     * Retrieves a bound service, if present.
     *
     * @param type the service type to look up
     * @param <T>  service type
     * @return the service wrapped in an optional
     */
    @SuppressWarnings("unchecked")
    public <T> @NotNull Optional<T> get(@NotNull Class<T> type) {
        return Optional.ofNullable((T) services.get(type));
    }

    /**
     * Retrieves a bound service or throws if missing.
     *
     * @param type the service type to look up
     * @param <T>  service type
     * @return the service instance, never null
     * @throws IllegalStateException if the service is not bound
     */
    public <T> @NotNull T require(@NotNull Class<T> type) {
        return get(type).orElseThrow(() ->
                new IllegalStateException("Required service not found: " + type.getSimpleName()));
    }

    /**
     * Returns whether a service type is currently bound.
     *
     * @param type the service type to check
     * @return {@code true} if a binding exists
     */
    public boolean has(@NotNull Class<?> type) {
        return services.containsKey(type);
    }

    /**
     * Removes a service binding.
     *
     * @param type the service type to unbind
     */
    public void unbind(@NotNull Class<?> type) {
        services.remove(type);
        LOG.debug("Unbound service: {}", type.getSimpleName());
    }

    /**
     * Removes all service bindings.
     */
    public void clear() {
        services.clear();
    }

    // ── Async discovery ─────────────────────────────────────────────────────────

    /**
     * Starts building an async discovery for a Bukkit-registered service.
     *
     * @param type the service type to discover
     * @param <T>  service type
     * @return a fluent builder for configuring the discovery
     */
    public <T> @NotNull DiscoveryBuilder<T> discover(@NotNull Class<T> type) {
        return new DiscoveryBuilder<>(type);
    }

    /**
     * Fluent builder for configuring async service discovery with retries.
     *
     * @param <T> service type being discovered
     */
    public final class DiscoveryBuilder<T> {

        private final Class<T> type;
        private int maxAttempts = 10;
        private long retryDelayMs = 500;
        private Consumer<T> onSuccess = t -> { };
        private Runnable onFailure = () -> { };

        private DiscoveryBuilder(Class<T> type) {
            this.type = type;
        }

        /**
         * Sets the maximum number of discovery attempts.
         *
         * @param max maximum attempts (default 10)
         * @return this builder
         */
        public @NotNull DiscoveryBuilder<T> maxAttempts(int max) {
            this.maxAttempts = max;
            return this;
        }

        /**
         * Sets the delay between retry attempts in milliseconds.
         *
         * @param ms delay between retries (default 500)
         * @return this builder
         */
        public @NotNull DiscoveryBuilder<T> retryDelay(long ms) {
            this.retryDelayMs = ms;
            return this;
        }

        /**
         * Sets a callback invoked when the service is found.
         *
         * @param callback receives the discovered service instance
         * @return this builder
         */
        public @NotNull DiscoveryBuilder<T> onSuccess(@NotNull Consumer<T> callback) {
            this.onSuccess = callback;
            return this;
        }

        /**
         * Sets a callback invoked when discovery exhausts all attempts.
         *
         * @param callback runs on failure
         * @return this builder
         */
        public @NotNull DiscoveryBuilder<T> onFailure(@NotNull Runnable callback) {
            this.onFailure = callback;
            return this;
        }

        /**
         * Starts the async discovery process.
         *
         * @return future containing the discovered service, or empty if not found
         */
        public @NotNull CompletableFuture<Optional<T>> load() {
            return CompletableFuture.supplyAsync(() -> {
                for (var attempt = 1; attempt <= maxAttempts; attempt++) {
                    var provider = Bukkit.getServicesManager().getRegistration(type);
                    if (provider != null) {
                        var service = provider.getProvider();
                        bind(type, service);
                        LOG.info("Discovered {} (attempt {})", type.getSimpleName(), attempt);
                        onSuccess.accept(service);
                        return Optional.of(service);
                    }
                    if (attempt < maxAttempts) {
                        try {
                            Thread.sleep(retryDelayMs);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                LOG.warn("Failed to discover {} after {} attempts",
                        type.getSimpleName(), maxAttempts);
                onFailure.run();
                return Optional.empty();
            });
        }
    }
}
