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

public class RCoreBukkitServiceRegistrar {

    private static final Logger LOGGER = CentralLogger.getLogger(RCoreBukkitServiceRegistrar.class);

    private RCoreBukkitServiceRegistrar() {}

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

    public static void unregister(final @NotNull JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        Bukkit.getServicesManager().unregisterAll(plugin);
        LOGGER.info("[%s] Unregistered services".formatted(plugin.getName()));
    }
}