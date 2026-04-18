package de.jexcellence.jexplatform;

import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.jexplatform.scheduler.PlatformScheduler;
import de.jexcellence.jexplatform.server.PlatformApi;
import de.jexcellence.jexplatform.server.ServerType;
import de.jexcellence.jexplatform.service.ServiceRegistry;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable snapshot of the core platform components.
 *
 * <p>Constructed once during {@link JExPlatform.Builder#build()} and held for the
 * lifetime of the plugin. Every field is non-null and thread-safe.
 *
 * @param plugin     the owning Bukkit plugin
 * @param serverType detected server implementation
 * @param api        platform-agnostic messaging and item API
 * @param scheduler  cross-platform task scheduler
 * @param logger     SLF4J-style logger for the plugin
 * @param services   thread-safe service registry
 *
 * @author JExcellence
 * @since 1.0.0
 */
public record PlatformContext(
        @NotNull JavaPlugin plugin,
        @NotNull ServerType serverType,
        @NotNull PlatformApi api,
        @NotNull PlatformScheduler scheduler,
        @NotNull JExLogger logger,
        @NotNull ServiceRegistry services
) { }
