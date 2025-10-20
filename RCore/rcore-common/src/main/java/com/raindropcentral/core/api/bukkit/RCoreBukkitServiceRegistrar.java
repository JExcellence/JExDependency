package com.raindropcentral.core.api.bukkit;

import com.raindropcentral.core.api.RCoreAdapter;
import com.raindropcentral.core.api.RCoreBackend;
import com.raindropcentral.core.service.RCoreService;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Utility for registering and unregistering {@link RCoreService} instances
 * within Bukkit's {@link ServicesManager}.
 *
 * <p>All registration calls construct a new {@link RCoreAdapter} around the
 * provided {@link RCoreBackend}, preserving the lifecycle guarantees described
 * in {@code com.raindropcentral.core.api}.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class RCoreBukkitServiceRegistrar {

    /**
     * Shared logger used to announce registration events and assist operators
     * when diagnosing priority conflicts or lifecycle issues.
     */
    private static final Logger LOGGER = CentralLogger.getLogger(RCoreBukkitServiceRegistrar.class);

    /**
     * Constructs the registrar.
     *
     * <p>This constructor remains private to enforce the non-instantiable
     * utility pattern; all functionality is exposed via the class's static
     * helpers and no state should ever be retained.</p>
     */
    private RCoreBukkitServiceRegistrar() {
    }

    /**
     * Registers an {@link RCoreService} implementation backed by the supplied
     * backend with Bukkit, returning the constructed adapter while logging the
     * outcome.
     *
     * <p>The provided {@link ServicePriority} controls how Bukkit resolves
     * competing implementations; ensure it matches the consuming plugin's
     * expectations to avoid priority conflicts. Successful registrations are
     * announced through {@link CentralLogger} for operational visibility.</p>
     *
     * @param plugin plugin owning the service registration
     * @param backend initialized backend providing persistence and executors
     * @param priority Bukkit registration priority to use when publishing the service
     * @return registered adapter instance
     * @throws NullPointerException if any argument is {@code null}
     */
    public static @NotNull RCoreService register(
        final @NotNull JavaPlugin plugin,
        final @NotNull RCoreBackend backend,
        final @NotNull ServicePriority priority
    ) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        Objects.requireNonNull(backend, "backend cannot be null");
        Objects.requireNonNull(priority, "priority cannot be null");

        final RCoreService service = new RCoreAdapter(backend);
        final ServicesManager services = Bukkit.getServicesManager();
        services.register(RCoreService.class, service, plugin, priority);

        LOGGER.info("[%s] Registered RCoreService v%s at priority %s"
            .formatted(plugin.getName(), service.getApiVersion(), priority));
        return service;
    }

    /**
     * Removes all services registered by the given plugin from Bukkit's service
     * registry and records the outcome in the shared logger.
     *
     * <p>Call this during plugin shutdown to ensure adapters are not left
     * dangling in the {@link ServicesManager}. A matching log entry is emitted
     * to aid operators verifying the deregistration sequence.</p>
     *
     * @param plugin plugin whose services should be unregistered
     * @throws NullPointerException if {@code plugin} is {@code null}
     */
    public static void unregister(final @NotNull JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        Bukkit.getServicesManager().unregisterAll(plugin);
        LOGGER.info("[%s] Unregistered services".formatted(plugin.getName()));
    }
}